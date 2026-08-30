export type AttentionStatus = 'Critical' | 'Warning' | 'Secure' | 'NotScanned';

export type FindingStatus = 'OPEN' | 'RESOLVED' | 'SCANNING...';

export type RemediationQuality = 'ACTION_REQUIRED' | 'RISK_CONTAINED' | 'VERIFIED_COMPLETE';

export type FindingSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';

export interface UserProfile {
  githubUserId: number;
  login: string;
  name: string;
  avatarUrl: string;
  email?: string;
}

export type ApiResult<T> =
  | { status: 'SUCCESS'; data: T }
  | { status: 'NOT_FOUND' }
  | { status: 'ERROR'; error: string; statusCode?: number };

export type RepositoryPostureStatus =
  | 'ACTION_REQUIRED'
  | 'NO_OPEN_FINDINGS'
  | 'COVERAGE_INCOMPLETE'
  | 'AWAITING_INITIAL_SCAN'
  | 'SCAN_IN_PROGRESS'
  | 'SCAN_UNAVAILABLE';

export interface FindingSeverityCounts {
  critical: number;
  high: number;
  medium: number;
  low: number;
  total: number;
}

export interface SecurityActionSummary {
  status: RepositoryPostureStatus;
  statusLabel: string;
  statusDescription: string;
  severityCounts: FindingSeverityCounts;
  totalFilesScanned: number | null;
  totalFilesSkipped: number | null;
  coverageImpact: 'COMPLETE' | 'INCOMPLETE' | 'NONE' | null;
  coverageRecordedAt: string | null;
  scanCompletedAt: string | null;
  actionPrompt: string;
}

export interface Repository {
  id: string;
  githubRepoId?: number;
  dbRepositoryId?: string;
  name: string;
  branch: string;
  isPrivate: boolean;
  language: string;
  lastScanned?: string | null;
  isScanned?: boolean;
  findingCount: number;
  attentionStatus: AttentionStatus;
  postureStatus?: RepositoryPostureStatus;
  severityCounts?: FindingSeverityCounts;
}

export interface CodeDiffSnippet {
  filePath: string;
  startLine: number;
  originalSnippet: string;
  suggestedFixSnippet: string;
  explanation: string;
}

export interface Finding {
  id: string;
  ruleId: string;
  ruleName: string;
  severity: FindingSeverity;
  status: FindingStatus;
  remediationQuality: RemediationQuality;
  filePath: string;
  lineNumber: number;
  rawSecretMasked: string;
  detectedCommit: string;
  detectedAt: string;
  remediationDiff: CodeDiffSnippet;
  githubIssueNumber?: number | null;
  githubIssueUrl?: string | null;
  issueLinkState?: 'PENDING' | 'CREATED' | 'UNKNOWN' | 'FAILED' | null;
  remediationPrNumber?: number | null;
  remediationPrUrl?: string | null;
  remediationPrState?: 'PENDING' | 'CREATED' | 'UNKNOWN' | 'FAILED' | null;
}

export interface FindingIssuePreviewDto {
  findingId: string;
  title: string;
  body: string;
  previewToken: string;
  linkState?: string | null;
  alreadyLinked: boolean;
  existingIssueNumber?: number | null;
  existingIssueUrl?: string | null;
}

export interface FindingIssueLinkDto {
  id: string;
  findingId: string;
  repositoryId: string;
  state: 'PENDING' | 'CREATED' | 'UNKNOWN' | 'FAILED';
  githubIssueNumber?: number | null;
  githubIssueUrl?: string | null;
  createdAt: string;
}

export interface FindingRemediationPrPreviewDto {
  findingId: string;
  repositoryId: string;
  filePath: string;
  lineNumber: number;
  targetCommitSha: string;
  targetBranch: string;
  remediationBranchName: string;
  originalLineMasked: string;
  patchedLine: string;
  envVariableName: string;
  previewToken: string;
  expiresAt: string;
  revocationWarning: string;
  alreadyLinked: boolean;
  existingPrNumber?: number | null;
  existingPrUrl?: string | null;
  linkState?: 'PENDING' | 'CREATED' | 'UNKNOWN' | 'FAILED' | null;
}

export interface FindingRemediationPrLinkDto {
  id: string;
  findingId: string;
  repositoryId: string;
  sourceRevisionCommit: string;
  targetBranch: string;
  headBranch: string;
  state: 'PENDING' | 'CREATED' | 'UNKNOWN' | 'FAILED';
  githubPrNumber?: number | null;
  githubPrUrl?: string | null;
  idempotencyMarker: string;
  failureReason?: string | null;
  createdAt: string;
  updatedAt: string;
}

export type { ScanEvent, ScanEventsResponse } from './api';
