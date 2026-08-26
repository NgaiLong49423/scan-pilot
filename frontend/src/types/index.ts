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
  healthScore?: number | null;
  attentionStatus: AttentionStatus;
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
}

export interface HealthMetrics {
  healthScore: number | null;
  grade: string;
  scannedFilesCount: number;
  totalFilesCount?: number;
  skippedFilesCount?: number;
  openLeaksCount: number;
  resolvedLeaksCount: number;
  aiFixReadyCount: number;
  mttrMinutes: number;
  trendData: number[];
  reasonCode?: string;
  limitHitValue?: number;
  isCoverageIncomplete?: boolean;
}

export type { ScanEvent, ScanEventsResponse } from './api';
