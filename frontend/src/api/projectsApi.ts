import { apiClient } from './client';
import { MonitoredProject, SelectRepositoryRequest, BranchConfigRequest } from '../types/api';

export const projectsApi = {
  /**
   * Retrieves the currently active monitored project for the authenticated user.
   * Returns null if none is selected yet (404).
   */
  async getCurrentProject(): Promise<MonitoredProject | null> {
    try {
      return await apiClient.get<MonitoredProject>('/api/v1/projects/current');
    } catch (err: any) {
      if (err.status === 404) {
        return null;
      }
      throw err;
    }
  },

  /**
   * Selects and onboards a repository for monitoring (DEC-046).
   */
  async selectRepository(githubRepoId: number, fullName: string): Promise<MonitoredProject> {
    const payload: SelectRepositoryRequest = { githubRepoId, fullName };
    return await apiClient.post<MonitoredProject>('/api/v1/projects/select-repository', payload);
  },

  /**
   * Configures secondary monitored branches (max 2 slots, FR-020, FR-023).
   */
  async updateBranches(secondaryBranches: string[]): Promise<MonitoredProject> {
    const payload: BranchConfigRequest = { secondaryBranches };
    return await apiClient.put<MonitoredProject>('/api/v1/projects/branches', payload);
  },
};
