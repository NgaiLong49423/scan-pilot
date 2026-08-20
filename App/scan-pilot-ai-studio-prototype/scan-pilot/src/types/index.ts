export type AttentionStatus = 'Critical' | 'Warning' | 'Secure';

export type FindingStatus = 'OPEN' | 'RESOLVED' | 'SCANNING...';

export type RemediationQuality = 'ACTION_REQUIRED' | 'RISK_CONTAINED' | 'VERIFIED_COMPLETE';

export type FindingSeverity = 'CRITICAL' | 'HIGH' | 'MEDIUM' | 'LOW' | 'INFO';

export interface Repository {
  id: string;
  name: string;
  branch: string;
  isPrivate: boolean;
  language: string;
  lastScanned: string;
  findingCount: number;
  healthScore: number;
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
  healthScore: number;
  grade: string;
  scannedFilesCount: number;
  openLeaksCount: number;
  resolvedLeaksCount: number;
  aiFixReadyCount: number;
  mttrMinutes: number;
  aiSuccessRate: number;
  trendData: number[];
}
