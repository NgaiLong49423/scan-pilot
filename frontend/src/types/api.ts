/**
 * TypeScript API contracts matching Scan Pilot backend DTOs.
 * Strictly adheres to UC-001 through UC-006 and NFR-006 / NFR-007.
 */

export interface UserProfile {
  githubUserId: number;
  login: string;
  name: string | null;
  avatarUrl: string | null;
  email: string | null;
}

export interface GitHubRepository {
  id: number;
  name: string;
  fullName: string;
  owner: string;
  defaultBranch: string;
  isPrivate: boolean;
  htmlUrl: string;
  description: string | null;
  isSelected: boolean;
}

export interface MonitoredProject {
  id: string;
  githubRepoId: number;
  owner: string;
  name: string;
  fullName: string;
  defaultBranch: string;
  primaryBranch: string;
  secondaryBranches: string[];
  isPrivate: boolean;
  monitoredAt: string;
  status: string;
}

export interface ScanJob {
  id: string;
  repositoryId: string;
  branchName: string;
  scanMode: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  commitSha: string | null;
  durationMs: number | null;
  errorMessage: string | null;
  startedAt: string | null;
  completedAt: string | null;
}

export interface FindingLocation {
  id?: string;
  filePath: string;
  startLine?: number;
  endLine?: number;
  startColumn?: number;
  endColumn?: number;
  commitSha?: string;
  author?: string;
  isCurrentHead?: boolean;
  detectedAt?: string;
}

export interface EvidenceItem {
  id?: string;
  findingId?: string;
  evidenceType?: string;
  maskedSecret?: string;
  redactedSnippet?: string;
  verificationStatus?: string;
  sourceAttribution?: string;
  createdAt?: string;
}

export type FindingSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';
export type FindingLifecycle = 'OPEN' | 'RESOLVED' | 'REGRESSED';
export type RemediationQuality = 'ACTION_REQUIRED' | 'RISK_CONTAINED' | 'VERIFIED_COMPLETE';

export interface Finding {
  id: string;
  repositoryId: string;
  ruleId: string;
  fingerprint: string;
  severity: FindingSeverity;
  title: string;
  description: string;
  lifecycle: FindingLifecycle;
  remediationQuality: RemediationQuality;
  firstSeenAt: string;
  lastSeenAt: string;
  resolvedAt?: string | null;
  locations: FindingLocation[];
  evidenceItems?: EvidenceItem[];
}

export interface AiExplanation {
  summary: string;
  riskImpact: string;
  evidenceLimits: string;
  remediationSteps: string[];
  remediationDiff: string;
  revocationCommandHint: string;
  sourceAttribution: string;
}

export interface CoverageItem {
  id: string;
  filePath: string;
  classification: string;
  sizeBytes: number;
  status: string;
  reasonCode: string;
  impact: string;
  details: string;
}

export interface CoverageSummary {
  id: string;
  scanJobId?: string;
  repositoryId: string;
  branchName: string;
  totalFiles: number;
  scannedFiles: number;
  skippedFiles: number;
  textFiles: number;
  binaryFiles: number;
  undeterminedFiles: number;
  totalBytes: number;
  coverageImpact: string;
  reasonCode?: string;
  limitHitValue?: number;
  createdAt: string;
  skippedItems?: CoverageItem[];
  items?: CoverageItem[];
}

export interface ScanTriggerResponse {
  jobId: string;
  repositoryId: string;
  branchName: string;
  status: string;
  message: string;
}

export interface ScanTriggerRequest {
  repositoryId?: string;
  branchName?: string;
  sourcePath?: string;
}

export interface SelectRepositoryRequest {
  githubRepoId: number;
  fullName: string;
}

export interface BranchConfigRequest {
  repositoryId?: string;
  secondaryBranches: string[];
}

export interface InstallUrlResponse {
  installUrl: string;
}
