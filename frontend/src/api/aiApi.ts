import { apiClient } from './client';
import { AiExplanation } from '../types/api';

export const aiApi = {
  /**
   * Triggers or generates an AI explanation and remediation guide for a finding (FR-005, UC-004).
   */
  async explainFinding(findingId: string): Promise<AiExplanation> {
    return await apiClient.post<AiExplanation>(`/api/v1/ai/findings/${findingId}/explain`);
  },

  /**
   * Retrieves an existing cached AI explanation for a finding.
   * Returns null if not yet generated (404).
   */
  async getFindingExplanation(findingId: string): Promise<AiExplanation | null> {
    try {
      return await apiClient.get<AiExplanation>(`/api/v1/ai/findings/${findingId}/explanation`);
    } catch (err: any) {
      if (err.status === 404) {
        return null;
      }
      throw err;
    }
  },
};
