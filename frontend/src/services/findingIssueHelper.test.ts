import { describe, it, expect } from 'vitest';
import {
  reduceFindingIssueState,
  initialFindingIssueState,
  buildCreateIssuePayload,
  FindingIssueState,
} from './findingIssueHelper';
import { FindingIssuePreviewDto, FindingIssueLinkDto } from '../types';

describe('findingIssueHelper: reduceFindingIssueState', () => {
  const dummyPreview: FindingIssuePreviewDto = {
    findingId: 'f-123',
    title: '[Security] SP-CONFIG-001: Secret Exposure',
    body: '<!-- scan-pilot-finding-id: f-123 -->\n### Security Finding',
    previewToken: 'tok-123',
    linkState: null,
    alreadyLinked: false,
  };

  const dummyLink: FindingIssueLinkDto = {
    id: 'link-1',
    findingId: 'f-123',
    repositoryId: 'repo-1',
    state: 'CREATED',
    githubIssueNumber: 42,
    githubIssueUrl: 'https://github.com/org/repo/issues/42',
    createdAt: '2026-08-28T20:00:00Z',
  };

  it('OPEN_MODAL transitions to PREVIEW_LOADING and clears errors', () => {
    const errorState: FindingIssueState = {
      status: 'ERROR',
      preview: null,
      link: null,
      errorMessage: 'Old error',
    };
    const next = reduceFindingIssueState(errorState, { type: 'OPEN_MODAL' });
    expect(next.status).toBe('PREVIEW_LOADING');
    expect(next.errorMessage).toBeNull();
  });

  it('PREVIEW_LOADED transitions to PREVIEW_READY when not already linked', () => {
    const next = reduceFindingIssueState(initialFindingIssueState, {
      type: 'PREVIEW_LOADED',
      preview: dummyPreview,
    });
    expect(next.status).toBe('PREVIEW_READY');
    expect(next.preview).toEqual(dummyPreview);
  });

  it('PREVIEW_LOADED transitions to SUCCESS if finding is alreadyLinked', () => {
    const linkedPreview: FindingIssuePreviewDto = {
      ...dummyPreview,
      alreadyLinked: true,
      existingIssueNumber: 77,
      existingIssueUrl: 'https://github.com/org/repo/issues/77',
    };
    const next = reduceFindingIssueState(initialFindingIssueState, {
      type: 'PREVIEW_LOADED',
      preview: linkedPreview,
    });
    expect(next.status).toBe('SUCCESS');
    expect(next.link?.githubIssueNumber).toBe(77);
  });

  it('PREVIEW_LOADED transitions to CREATION_IN_PROGRESS if linkState is PENDING', () => {
    const pendingPreview: FindingIssuePreviewDto = {
      ...dummyPreview,
      linkState: 'PENDING',
    };
    const next = reduceFindingIssueState(initialFindingIssueState, {
      type: 'PREVIEW_LOADED',
      preview: pendingPreview,
    });
    expect(next.status).toBe('CREATION_IN_PROGRESS');
  });

  it('PREVIEW_FAILED transitions to ERROR', () => {
    const next = reduceFindingIssueState(initialFindingIssueState, {
      type: 'PREVIEW_FAILED',
      error: 'NETWORK_ERROR',
    });
    expect(next.status).toBe('ERROR');
    expect(next.errorMessage).toBe('Network communication error with server. Please check your connection and retry.');
  });

  it('SUBMIT_START transitions to SUBMITTING', () => {
    const readyState: FindingIssueState = {
      status: 'PREVIEW_READY',
      preview: dummyPreview,
      link: null,
      errorMessage: null,
    };
    const next = reduceFindingIssueState(readyState, { type: 'SUBMIT_START' });
    expect(next.status).toBe('SUBMITTING');
  });

  it('SUBMIT_SUCCESS transitions to SUCCESS and stores link', () => {
    const submittingState: FindingIssueState = {
      status: 'SUBMITTING',
      preview: dummyPreview,
      link: null,
      errorMessage: null,
    };
    const next = reduceFindingIssueState(submittingState, {
      type: 'SUBMIT_SUCCESS',
      link: dummyLink,
    });
    expect(next.status).toBe('SUCCESS');
    expect(next.link).toEqual(dummyLink);
  });

  it('SUBMIT_CONFLICT_PENDING transitions to CREATION_IN_PROGRESS', () => {
    const next = reduceFindingIssueState(initialFindingIssueState, {
      type: 'SUBMIT_CONFLICT_PENDING',
    });
    expect(next.status).toBe('CREATION_IN_PROGRESS');
  });

  it('SUBMIT_FAILED transitions to ERROR with sanitized message without raw server leaks', () => {
    const maliciousLeak = 'Exception at /var/app/SecretService.java:42 with token gho_secret123456';
    const next = reduceFindingIssueState(initialFindingIssueState, {
      type: 'SUBMIT_FAILED',
      error: maliciousLeak,
    });
    expect(next.status).toBe('ERROR');
    expect(next.errorMessage).toBe('Unable to create GitHub issue at this time. Please try again later.');
    expect(next.errorMessage).not.toContain('/var/app');
    expect(next.errorMessage).not.toContain('gho_secret123456');
  });

  it('SUBMIT_FAILED with exact PREVIEW_TOKEN_EXPIRED_OR_INVALID instructs user to close/reopen modal', () => {
    const next = reduceFindingIssueState(initialFindingIssueState, {
      type: 'SUBMIT_FAILED',
      error: 'PREVIEW_TOKEN_EXPIRED_OR_INVALID',
    });
    expect(next.status).toBe('ERROR');
    expect(next.errorMessage).toBe('The preview token has expired or is invalid. Please close and re-open to generate a fresh preview.');
  });

  it('SUBMIT_FAILED with exact CREATION_IN_PROGRESS informs user creation is in progress', () => {
    const next = reduceFindingIssueState(initialFindingIssueState, {
      type: 'SUBMIT_FAILED',
      error: 'CREATION_IN_PROGRESS',
    });
    expect(next.status).toBe('ERROR');
    expect(next.errorMessage).toBe('An issue creation request is currently in progress. Please wait a moment.');
  });

  it('SUBMIT_FAILED with injected substrings rejects to generic safe message', () => {
    const next1 = reduceFindingIssueState(initialFindingIssueState, {
      type: 'SUBMIT_FAILED',
      error: 'CREATION_IN_PROGRESS injected-text',
    });
    expect(next1.errorMessage).toBe('Unable to create GitHub issue at this time. Please try again later.');

    const next2 = reduceFindingIssueState(initialFindingIssueState, {
      type: 'SUBMIT_FAILED',
      error: 'PREVIEW_TOKEN_EXPIRED_OR_INVALID injected-text',
    });
    expect(next2.errorMessage).toBe('Unable to create GitHub issue at this time. Please try again later.');
  });

  it('CLOSE_MODAL resets to initial state', () => {
    const dirtyState: FindingIssueState = {
      status: 'SUCCESS',
      preview: dummyPreview,
      link: dummyLink,
      errorMessage: null,
    };
    const next = reduceFindingIssueState(dirtyState, { type: 'CLOSE_MODAL' });
    expect(next).toEqual(initialFindingIssueState);
  });
});

describe('findingIssueHelper: buildCreateIssuePayload', () => {
  it('strictly builds payload containing only previewToken', () => {
    const payload = buildCreateIssuePayload('  eyJ2IjoidjEi...  ');
    expect(payload).toEqual({ previewToken: 'eyJ2IjoidjEi...' });
    expect(Object.keys(payload)).toEqual(['previewToken']);
  });
});
