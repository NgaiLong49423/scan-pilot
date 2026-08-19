import { apiClient } from './client';
import { UserProfile } from '../types/api';

export const authApi = {
  /**
   * Retrieves the profile of the currently authenticated user.
   * Returns null if unauthenticated (401).
   */
  async getMe(): Promise<UserProfile | null> {
    try {
      return await apiClient.get<UserProfile>('/api/v1/auth/me');
    } catch {
      // Gracefully treat as guest/unauthenticated
      return null;
    }
  },

  /**
   * Returns the GitHub OAuth login URL.
   */
  getLoginUrl(): string {
    const base = apiClient.getBaseUrl();
    return `${base}/api/v1/auth/github/login`;
  },

  /**
   * Invalidates active session on backend and clears the HttpOnly cookie.
   */
  async logout(): Promise<{ message: string }> {
    return await apiClient.post<{ message: string }>('/api/v1/auth/logout');
  },
};
