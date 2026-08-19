import { apiClient } from './client';
import { GitHubRepository, InstallUrlResponse } from '../types/api';

export const githubApi = {
  /**
   * Lists repositories accessible to the user via GitHub App installation or OAuth.
   */
  async getAccessibleRepositories(): Promise<GitHubRepository[]> {
    return await apiClient.get<GitHubRepository[]>('/api/v1/github/repositories');
  },

  /**
   * Retrieves the GitHub App installation URL.
   */
  async getInstallUrl(): Promise<InstallUrlResponse> {
    return await apiClient.get<InstallUrlResponse>('/api/v1/github/install-url');
  },

  /**
   * Links a GitHub App installation to the authenticated user's session.
   */
  async linkInstallation(installationId: number): Promise<{ message: string; installationId: number }> {
    return await apiClient.post<{ message: string; installationId: number }>(
      '/api/v1/github/installations/link',
      { installationId }
    );
  },
};
