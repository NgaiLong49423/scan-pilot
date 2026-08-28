import { FindingIssuePreviewDto, FindingIssueLinkDto } from '../types';

export type ModalState =
  | 'IDLE'
  | 'PREVIEW_LOADING'
  | 'PREVIEW_READY'
  | 'SUBMITTING'
  | 'SUCCESS'
  | 'ERROR'
  | 'CREATION_IN_PROGRESS';

export type ModalAction =
  | { type: 'OPEN_MODAL' }
  | { type: 'PREVIEW_LOADED'; preview: FindingIssuePreviewDto }
  | { type: 'PREVIEW_FAILED'; error: string }
  | { type: 'SUBMIT_START' }
  | { type: 'SUBMIT_SUCCESS'; link: FindingIssueLinkDto }
  | { type: 'SUBMIT_CONFLICT_PENDING' }
  | { type: 'SUBMIT_FAILED'; error: string }
  | { type: 'CLOSE_MODAL' };

export interface FindingIssueState {
  status: ModalState;
  preview: FindingIssuePreviewDto | null;
  link: FindingIssueLinkDto | null;
  errorMessage: string | null;
}

export const initialFindingIssueState: FindingIssueState = {
  status: 'IDLE',
  preview: null,
  link: null,
  errorMessage: null,
};

/**
 * Maps internal/network error codes to allow-listed, human-readable UI messages.
 * Never leaks raw server messages, stack traces, paths, or tokens.
 */
export function sanitizeUserErrorMessage(rawCode: string | null | undefined): string {
  if (!rawCode) {
    return 'An unexpected error occurred. Please try again.';
  }

  const code = rawCode.trim();

  if (code === 'CREATION_IN_PROGRESS') {
    return 'An issue creation request is currently in progress. Please wait a moment.';
  }
  if (code === 'PREVIEW_TOKEN_EXPIRED_OR_INVALID') {
    return 'The preview token has expired or is invalid. Please close and re-open to generate a fresh preview.';
  }
  if (code === 'AUTH_REQUIRED' || code === '401') {
    return 'Authentication is required to create a GitHub issue.';
  }
  if (code === 'GITHUB_APP_REQUIRED' || code === '403') {
    return 'GitHub App installation is required on this repository to create issues.';
  }
  if (code === 'NOT_FOUND' || code === '404') {
    return 'The requested finding or repository could not be found.';
  }
  if (code === 'NETWORK_ERROR') {
    return 'Network communication error with server. Please check your connection and retry.';
  }

  return 'Unable to create GitHub issue at this time. Please try again later.';
}

/**
 * Pure state transition reducer for the Create GitHub Issue modal.
 */
export function reduceFindingIssueState(
  state: FindingIssueState,
  action: ModalAction
): FindingIssueState {
  switch (action.type) {
    case 'OPEN_MODAL':
      return {
        ...state,
        status: 'PREVIEW_LOADING',
        preview: null,
        link: null,
        errorMessage: null,
      };
    case 'PREVIEW_LOADED':
      if (action.preview.alreadyLinked || action.preview.linkState === 'CREATED') {
        return {
          ...state,
          status: 'SUCCESS',
          preview: action.preview,
          link: {
            id: '',
            findingId: action.preview.findingId,
            repositoryId: '',
            state: 'CREATED',
            githubIssueNumber: action.preview.existingIssueNumber,
            githubIssueUrl: action.preview.existingIssueUrl,
            createdAt: '',
          },
          errorMessage: null,
        };
      }
      if (action.preview.linkState === 'PENDING') {
        return {
          ...state,
          status: 'CREATION_IN_PROGRESS',
          preview: action.preview,
          errorMessage: null,
        };
      }
      return {
        ...state,
        status: 'PREVIEW_READY',
        preview: action.preview,
        errorMessage: null,
      };
    case 'PREVIEW_FAILED':
      return {
        ...state,
        status: 'ERROR',
        errorMessage: sanitizeUserErrorMessage(action.error),
      };
    case 'SUBMIT_START':
      return {
        ...state,
        status: 'SUBMITTING',
        errorMessage: null,
      };
    case 'SUBMIT_SUCCESS':
      return {
        ...state,
        status: 'SUCCESS',
        link: action.link,
        errorMessage: null,
      };
    case 'SUBMIT_CONFLICT_PENDING':
      return {
        ...state,
        status: 'CREATION_IN_PROGRESS',
        errorMessage: null,
      };
    case 'SUBMIT_FAILED':
      return {
        ...state,
        status: 'ERROR',
        errorMessage: sanitizeUserErrorMessage(action.error),
      };
    case 'CLOSE_MODAL':
      return initialFindingIssueState;
    default:
      return state;
  }
}

/**
 * Builds the strict confirmation payload sending only the signed previewToken.
 */
export function buildCreateIssuePayload(previewToken: string): { previewToken: string } {
  return { previewToken: previewToken.trim() };
}
