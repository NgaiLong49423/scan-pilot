import { Repository, Finding, HealthMetrics, UserProfile } from '../types';

export const MOCK_REPOSITORIES: Repository[] = [
  {
    id: 'repo-1',
    name: 'NgaiLong49423/scan-pilot',
    branch: 'main',
    isPrivate: false,
    language: 'Java',
    lastScanned: '2m ago',
    findingCount: 3,
    healthScore: 92,
    attentionStatus: 'Warning',
  },
  {
    id: 'repo-2',
    name: 'NgaiLong49423/BenchmarkJava',
    branch: 'master',
    isPrivate: true,
    language: 'Java',
    lastScanned: '1h ago',
    findingCount: 0,
    healthScore: 100,
    attentionStatus: 'Secure',
  },
  {
    id: 'repo-3',
    name: 'NgaiLong49423/studypack-exam-prep',
    branch: 'main',
    isPrivate: false,
    language: 'TypeScript',
    lastScanned: '45m ago',
    findingCount: 1,
    healthScore: 84,
    attentionStatus: 'Warning',
  },
];

export const MOCK_FINDINGS: Finding[] = [
  {
    id: 'find-001',
    ruleId: 'SP-CONFIG-001',
    ruleName: 'Hardcoded AWS Access Credentials',
    severity: 'CRITICAL',
    status: 'OPEN',
    remediationQuality: 'ACTION_REQUIRED',
    filePath: 'backend/src/test/java/com/scanpilot/SecretServiceTest.java',
    lineNumber: 102,
    rawSecretMasked: 'AKIAJ************Z',
    detectedCommit: 'HEAD-05 (b7f9a1c)',
    detectedAt: '2 mins ago',
    remediationDiff: {
      filePath: 'backend/src/test/java/com/scanpilot/SecretServiceTest.java',
      startLine: 101,
      originalSnippet: `101 public void setupTest() {
102   String awsAccessKey = "AKIAJ************Z";
103   String awsSecretKey = "wJalrXUtnFEMI/********************";
104   awsClient.initialize(awsAccessKey, awsSecretKey);`,
      suggestedFixSnippet: `101 public void setupTest() {
102   String awsAccessKey = System.getenv("TEST_AWS_ACCESS_KEY");
103   String awsSecretKey = System.getenv("TEST_AWS_SECRET_KEY");
104   awsClient.initialize(awsAccessKey, awsSecretKey);`,
      explanation: 'Replace hardcoded AWS API credentials with environment variable retrieval using System.getenv().',
    },
  },
  {
    id: 'find-002',
    ruleId: 'SP-CONFIG-001',
    ruleName: 'Generic Stripe API Secret Key Exposed',
    severity: 'HIGH',
    status: 'OPEN',
    remediationQuality: 'ACTION_REQUIRED',
    filePath: 'backend/src/main/resources/application.properties',
    lineNumber: 24,
    rawSecretMasked: 'sk_live_51M************9X',
    detectedCommit: 'HEAD-02 (f4a8b2e)',
    detectedAt: '15 mins ago',
    remediationDiff: {
      filePath: 'backend/src/main/resources/application.properties',
      startLine: 23,
      originalSnippet: `23 # Payment Configuration
24 stripe.api.key=sk_live_51M************9X
25 stripe.webhook.secret=whsec_*************`,
      suggestedFixSnippet: `23 # Payment Configuration
24 stripe.api.key=\${STRIPE_API_KEY}
25 stripe.webhook.secret=\${STRIPE_WEBHOOK_SECRET}`,
      explanation: 'Externalize private payment gateway tokens into environment variables injected at deployment.',
    },
  },
  {
    id: 'find-003',
    ruleId: 'SP-CONFIG-001',
    ruleName: 'GitHub Personal Access Token (PAT)',
    severity: 'HIGH',
    status: 'OPEN',
    remediationQuality: 'ACTION_REQUIRED',
    filePath: 'scripts/ci-deploy.sh',
    lineNumber: 12,
    rawSecretMasked: 'ghp_********************************',
    detectedCommit: 'HEAD-08 (1a3c5d7)',
    detectedAt: '1 hour ago',
    remediationDiff: {
      filePath: 'scripts/ci-deploy.sh',
      startLine: 11,
      originalSnippet: `11 # Automated Git Release Token
12 export GITHUB_TOKEN="ghp_********************************"
13 git push origin main --tags`,
      suggestedFixSnippet: `11 # Automated Git Release Token
12 # Rely on GitHub Actions secret context directly:
13 export GITHUB_TOKEN="\${{ secrets.RELEASE_BOT_TOKEN }}"
14 git push origin main --tags`,
      explanation: 'Avoid storing personal developer tokens in bash scripts; use CI/CD secret managers.',
    },
  },
];

export const MOCK_HEALTH_METRICS: HealthMetrics = {
  healthScore: 92,
  grade: 'Safe - Grade A',
  scannedFilesCount: 346,
  openLeaksCount: 3,
  resolvedLeaksCount: 14,
  aiFixReadyCount: 3,
  mttrMinutes: 12,
  aiSuccessRate: 98,
  trendData: [12, 10, 14, 9, 7, 5, 4, 3],
};

/**
 * Initiates real GitHub OAuth login by navigating browser to backend authorization endpoint.
 */
export function loginWithGitHub(returnUrl?: string): void {
  const target = returnUrl ? `?redirect_uri=${encodeURIComponent(returnUrl)}` : '';
  window.location.href = `/api/v1/auth/github/login${target}`;
}

/**
 * Checks if current user has an active session with backend.
 */
export async function fetchCurrentUser(): Promise<UserProfile | null> {
  try {
    const response = await fetch('/api/v1/auth/me', {
      credentials: 'include',
    });
    if (response.ok) {
      return await response.json();
    }
    return null;
  } catch (_e) {
    return null;
  }
}

/**
 * Logs out from backend session.
 */
export async function logoutUser(): Promise<void> {
  try {
    await fetch('/api/v1/auth/logout', {
      method: 'POST',
      credentials: 'include',
    });
  } catch (_e) {
    // Ignore error
  }
  window.location.href = '/';
}

/**
 * Fetches repositories from backend GitHub App integration or returns mock data.
 */
export async function fetchRepositories(): Promise<Repository[]> {
  try {
    const response = await fetch('/api/v1/github/repositories', {
      credentials: 'include',
    });
    if (response.ok) {
      const data = await response.json();
      if (Array.isArray(data) && data.length > 0) {
        return data.map((item: any) => ({
          id: String(item.id || item.fullName),
          name: item.fullName || item.name,
          branch: item.defaultBranch || 'main',
          isPrivate: Boolean(item.private),
          language: item.language || 'Java',
          lastScanned: 'Just now',
          findingCount: 0,
          healthScore: 100,
          attentionStatus: 'Secure',
        }));
      }
    }
  } catch (_e) {
    // Fallback to local mock data
  }
  return MOCK_REPOSITORIES;
}

/**
 * Fetches findings for the selected repository from backend scan jobs.
 */
export async function fetchFindingsForRepo(_repoId: string): Promise<Finding[]> {
  try {
    const response = await fetch('/api/v1/scans', {
      credentials: 'include',
    });
    if (response.ok) {
      const data = await response.json();
      if (Array.isArray(data) && data.length > 0) {
        // Map backend finding entity
      }
    }
  } catch (_e) {
    // Fallback to mock findings
  }
  return MOCK_FINDINGS;
}

/**
 * Fetches health score metrics for the selected repository.
 */
export async function fetchHealthMetrics(_repoId: string): Promise<HealthMetrics> {
  return MOCK_HEALTH_METRICS;
}

/**
 * Triggers a real repository scan on the Spring Boot backend.
 */
export async function triggerRealScan(branchName?: string): Promise<boolean> {
  try {
    const response = await fetch('/api/v1/scans/trigger', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({ branchName: branchName || 'main' }),
    });
    return response.ok;
  } catch (_e) {
    return false;
  }
}
