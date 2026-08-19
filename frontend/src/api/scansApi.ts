import { apiClient } from './client';
import { 
  ScanTriggerResponse, 
  ScanTriggerRequest, 
  ScanJob, 
  Finding, 
  CoverageSummary 
} from '../types/api';

export const scansApi = {
  /**
   * Triggers an on-demand snapshot and git history scan (FR-025, UC-003).
   */
  async triggerScan(
    branchName?: string, 
    repositoryId?: string, 
    sourcePath?: string
  ): Promise<ScanTriggerResponse> {
    const payload: ScanTriggerRequest = {};
    if (branchName) payload.branchName = branchName;
    if (repositoryId) payload.repositoryId = repositoryId;
    if (sourcePath) payload.sourcePath = sourcePath;

    return await apiClient.post<ScanTriggerResponse>('/api/v1/scans/trigger', payload);
  },

  /**
   * Retrieves scan job status, telemetry, and execution details.
   */
  async getScanJob(jobId: string): Promise<ScanJob> {
    return await apiClient.get<ScanJob>(`/api/v1/scans/jobs/${jobId}`);
  },

  /**
   * Retrieves all findings for a repository with severity and lifecycle states.
   */
  async getFindings(repositoryId: string): Promise<Finding[]> {
    return await apiClient.get<Finding[]>(`/api/v1/scans/repositories/${repositoryId}/findings`);
  },

  /**
   * Retrieves the latest coverage summary and skipped content report for a repository (UC-006).
   * Returns null if no coverage record exists yet (404).
   */
  async getCoverage(repositoryId: string): Promise<CoverageSummary | null> {
    try {
      return await apiClient.get<CoverageSummary>(`/api/v1/scans/repositories/${repositoryId}/coverage`);
    } catch (err: any) {
      if (err.status === 404) {
        return null;
      }
      throw err;
    }
  },
};
