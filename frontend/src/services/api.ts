import { Repository, Finding, HealthMetrics, UserProfile } from '../types';

const UUID_REGEX = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

export function isValidUuid(id?: string | null): boolean {
  return typeof id === 'string' && UUID_REGEX.test(id.trim());
}

/**
 * Resolves the Backend API base URL dynamically.
 */
export function getApiBaseUrl(): string {
  const envUrl = 
    (import.meta.env.VITE_API_URL as string | undefined) ||
    (import.meta.env.VITE_BACKEND_URL as string | undefined) ||
    (import.meta.env.VITE_API_BASE_URL as string | undefined);

  if (envUrl && envUrl.trim().length > 0) {
    return envUrl.trim().replace(/\/+$/, '');
  }

  return '';
}

/**
 * Initiates real GitHub OAuth login.
 */
export function loginWithGitHub(returnUrl?: string): void {
  const origin = returnUrl || (typeof window !== 'undefined' ? window.location.origin : '');
  const target = origin ? `?redirect_uri=${encodeURIComponent(origin)}` : '';
  const baseUrl = getApiBaseUrl();
  window.location.href = `${baseUrl}/api/v1/auth/github/login${target}`;
}

/**
 * Checks active user session from backend.
 */
export async function fetchCurrentUser(): Promise<UserProfile | null> {
  try {
    const baseUrl = getApiBaseUrl();
    const response = await fetch(`${baseUrl}/api/v1/auth/me`, {
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
    const baseUrl = getApiBaseUrl();
    await fetch(`${baseUrl}/api/v1/auth/logout`, {
      method: 'POST',
      credentials: 'include',
    });
  } catch (_e) {
    // Ignore error
  }
  window.location.href = '/';
}

/**
 * Fetches all available repositories from user's GitHub App installation.
 */
export async function fetchAvailableGitHubRepositories(): Promise<Repository[]> {
  try {
    const baseUrl = getApiBaseUrl();
    const response = await fetch(`${baseUrl}/api/v1/github/repositories`, {
      credentials: 'include',
    });
    if (response.ok) {
      const data = await response.json();
      if (Array.isArray(data)) {
        return data.map((item: any) => ({
          id: String(item.id || item.githubRepoId || item.fullName),
          githubRepoId: item.githubRepoId || item.id,
          name: item.fullName || item.name,
          branch: item.defaultBranch || 'main',
          isPrivate: Boolean(item.isPrivate || item.private),
          language: item.language || 'Java',
          lastScanned: null,
          isScanned: false,
          findingCount: 0,
          healthScore: 0,
          attentionStatus: 'NotScanned',
        }));
      }
    }
  } catch (_e) {
    // Fallback
  }
  return [];
}

/**
 * Fetches only the repositories explicitly monitored by the user from PostgreSQL.
 * Returns Repository[] on success (including empty array []).
 * Returns null if network or server error occurred.
 */
export async function fetchMonitoredProjects(): Promise<Repository[] | null> {
  try {
    const baseUrl = getApiBaseUrl();
    const response = await fetch(`${baseUrl}/api/v1/projects/monitored`, {
      credentials: 'include',
    });
    if (response.ok) {
      const data = await response.json();
      if (Array.isArray(data)) {
        return data.map((item: any) => {
          const repoId = String(item.id || item.repositoryId || '');
          return {
            id: repoId,
            dbRepositoryId: repoId,
            githubRepoId: item.githubRepoId,
            name: item.fullName || item.name,
            branch: item.primaryBranch || item.defaultBranch || 'main',
            isPrivate: Boolean(item.isPrivate),
            language: item.language || 'Java',
            lastScanned: null,
            isScanned: false,
            findingCount: 0,
            healthScore: 0,
            attentionStatus: 'NotScanned',
          };
        });
      }
      return [];
    }
    return null;
  } catch (_e) {
    return null;
  }
}

// Backward compatibility alias
export const fetchRepositories = fetchAvailableGitHubRepositories;

/**
 * Selects and imports a repository into PostgreSQL database for monitoring.
 * Returns the persisted UUID string on success, or null on failure.
 */
export async function selectRepositoryOnBackend(repo: Repository): Promise<string | null> {
  try {
    const baseUrl = getApiBaseUrl();
    const nameParts = repo.name.split('/');
    const owner = nameParts.length > 1 ? nameParts[0] : 'user';
    const repoName = nameParts.length > 1 ? nameParts[1] : repo.name;

    const response = await fetch(`${baseUrl}/api/v1/projects/select-repository`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({
        githubRepoId: Number(repo.githubRepoId || 0),
        owner,
        name: repoName,
        fullName: repo.name,
        defaultBranch: repo.branch || 'main',
        primaryBranch: repo.branch || 'main',
        isPrivate: repo.isPrivate,
      }),
    });

    if (response.ok) {
      const data = await response.json();
      const id = String(data.id || data.repositoryId || '');
      if (isValidUuid(id)) {
        return id;
      }
    }
  } catch (_e) {
    // Ignore error
  }
  return null;
}

/**
 * Fetches real findings from PostgreSQL database for the selected repository.
 */
export async function fetchFindingsForRepo(repositoryId: string): Promise<Finding[] | null> {
  if (!isValidUuid(repositoryId)) {
    return null;
  }
  try {
    const baseUrl = getApiBaseUrl();
    const response = await fetch(`${baseUrl}/api/v1/scans/repositories/${repositoryId}/findings`, {
      credentials: 'include',
    });
    if (response.ok) {
      const data = await response.json();
      if (Array.isArray(data)) {
        return data.map((item: any) => {
          const loc = item.locations && item.locations.length > 0 ? item.locations[0] : null;
          const masked = item.fingerprint 
            ? (item.fingerprint.length > 8 ? item.fingerprint.substring(0, 6) + '************' + item.fingerprint.substring(item.fingerprint.length - 2) : '************')
            : 'MASKED_SECRET';

          return {
            id: String(item.id),
            ruleId: item.ruleId || 'SP-CONFIG-001',
            ruleName: item.title || item.ruleId || 'Detected Secret Leak',
            severity: item.severity || 'HIGH',
            status: item.lifecycle === 'RESOLVED' ? 'RESOLVED' : 'OPEN',
            remediationQuality: item.remediationQuality || 'ACTION_REQUIRED',
            filePath: loc?.filePath || 'unknown/file',
            lineNumber: loc?.startLine || 1,
            rawSecretMasked: masked,
            detectedCommit: loc?.commitSha ? loc.commitSha.substring(0, 7) : 'HEAD',
            detectedAt: item.firstSeenAt ? new Date(item.firstSeenAt).toLocaleTimeString() : 'Just now',
            remediationDiff: {
              filePath: loc?.filePath || 'src/...',
              startLine: loc?.startLine || 1,
              originalSnippet: `${loc?.startLine || 1} // Exposed secret in ${loc?.filePath || 'file'}\n${loc?.startLine || 1} const secret = "${masked}";`,
              suggestedFixSnippet: `${loc?.startLine || 1} // Externalize into environment variable\n${loc?.startLine || 1} const secret = process.env.SAFE_CONFIG_KEY;`,
              explanation: item.description || 'Replace hardcoded secret with environment variable retrieval.',
            },
          };
        });
      }
      return [];
    }
  } catch (_e) {
    // Ignore error
  }
  return null;
}

/**
 * Fetches real coverage summary from PostgreSQL database for the repository.
 */
export async function fetchCoverageForRepo(repositoryId: string): Promise<any | null> {
  if (!isValidUuid(repositoryId)) {
    return null;
  }
  try {
    const baseUrl = getApiBaseUrl();
    const response = await fetch(`${baseUrl}/api/v1/scans/repositories/${repositoryId}/coverage`, {
      credentials: 'include',
    });
    if (response.ok) {
      return await response.json();
    }
  } catch (_e) {
    // Ignore error
  }
  return null;
}

/**
 * DTO representing scan job status and telemetry from backend.
 */
export interface ScanJobDto {
  id: string;
  repositoryId: string;
  branchName?: string;
  scanMode?: string;
  status: 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  stage?: string;
  commitSha?: string;
  durationMs?: number;
  errorMessage?: string;
  createdAt?: string;
  startedAt?: string;
  completedAt?: string;
}

export interface ScanJobPollResult {
  success: boolean;
  job?: ScanJobDto;
  isTerminal?: boolean;
  status?: number;
  message?: string;
}

/**
 * Fetches the real-time status and stage of a scan job from backend.
 * Distinguishes terminal HTTP errors (401/403/404) from transient server/network errors.
 */
export async function fetchScanJob(jobId: string): Promise<ScanJobPollResult> {
  if (!isValidUuid(jobId)) {
    return { success: false, isTerminal: true, message: 'Invalid scan job UUID' };
  }
  try {
    const baseUrl = getApiBaseUrl();
    const response = await fetch(`${baseUrl}/api/v1/scans/jobs/${jobId}`, {
      credentials: 'include',
    });
    if (response.ok) {
      const data = await response.json();
      return { success: true, job: data, status: response.status };
    }

    const err = await response.json().catch(() => ({}));
    const message = err.message || `Request failed with HTTP ${response.status}`;

    if (response.status === 401 || response.status === 403 || response.status === 404) {
      return { success: false, isTerminal: true, status: response.status, message };
    }

    return { success: false, isTerminal: false, status: response.status, message };
  } catch (e: any) {
    return { success: false, isTerminal: false, message: e?.message || 'Network error while fetching scan job' };
  }
}

/**
 * Triggers a real repository scan on the Spring Boot backend asynchronously (Issue #52).
 */
export async function triggerRealScan(
  repositoryId?: string,
  branchName?: string
): Promise<{ success: boolean; jobId?: string; status?: string; stage?: string; message?: string }> {
  if (!isValidUuid(repositoryId)) {
    return { success: false, message: 'Invalid or missing repository UUID (fail-closed)' };
  }
  try {
    const baseUrl = getApiBaseUrl();
    const response = await fetch(`${baseUrl}/api/v1/scans/trigger`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({
        repositoryId,
        branchName: branchName || 'main',
      }),
    });

    if (response.ok || response.status === 202) {
      const data = await response.json();
      return {
        success: true,
        jobId: data.jobId,
        status: data.status,
        stage: data.stage,
        message: data.message,
      };
    } else {
      const err = await response.json().catch(() => ({}));
      return { success: false, message: err.message || 'Scan trigger failed' };
    }
  } catch (e: any) {
    return { success: false, message: e.message || 'Network error' };
  }
}

/**
 * Fetches progressive telemetry events for a scan job from backend (Issue #69, AC-05).
 */
export async function fetchScanEvents(
  jobId: string,
  afterSeq: number = 0,
  limit: number = 50
): Promise<{ success: boolean; data?: import('../types/api').ScanEventsResponse; isTerminal?: boolean; status?: number; message?: string }> {
  if (!isValidUuid(jobId)) {
    return { success: false, isTerminal: true, message: 'Invalid or missing job UUID' };
  }
  try {
    const baseUrl = getApiBaseUrl();
    const response = await fetch(
      `${baseUrl}/api/v1/scans/jobs/${jobId}/events?afterSeq=${afterSeq}&limit=${limit}`,
      {
        credentials: 'include',
      }
    );

    if (response.ok) {
      const data: import('../types/api').ScanEventsResponse = await response.json();
      return { success: true, data };
    }

    const err = await response.json().catch(() => ({}));
    const message = err.message || `Request failed with HTTP ${response.status}`;

    if (response.status === 401 || response.status === 403 || response.status === 404) {
      return { success: false, isTerminal: true, status: response.status, message };
    }

    return { success: false, isTerminal: false, status: response.status, message };
  } catch (e: any) {
    return { success: false, isTerminal: false, message: e?.message || 'Network error while fetching scan events' };
  }
}
