import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { fetchFindingsForRepo, fetchCoverageForRepo, createFindingIssue } from './api';

describe('API Service - ApiResult Error Mapping', () => {
  const validUuid = '11111111-2222-3333-4444-555555555555';
  const invalidUuid = 'invalid-uuid-123';

  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('fetchFindingsForRepo', () => {
    it('returns ERROR on invalid UUID without making network request', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch');
      const result = await fetchFindingsForRepo(invalidUuid);

      expect(fetchSpy).not.toHaveBeenCalled();
      expect(result.status).toBe('ERROR');
      if (result.status === 'ERROR') {
        expect(result.error).toContain('Invalid repository UUID');
      }
    });

    it('maps HTTP 200 array response to SUCCESS with mapped findings', async () => {
      const mockRawFindings = [
        {
          id: 'find-1',
          ruleId: 'SP-SECRET-001',
          title: 'AWS Access Key',
          severity: 'CRITICAL',
          lifecycle: 'OPEN',
          remediationQuality: 'ACTION_REQUIRED',
          fingerprint: 'AKIAIOSFODNN7EXAMPLE',
          firstSeenAt: '2026-08-27T10:00:00Z',
          locations: [{ filePath: 'src/aws.ts', startLine: 12, commitSha: 'abcdef123456' }],
          description: 'Hardcoded AWS secret key detected',
        },
      ];

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockRawFindings,
      } as Response);

      const result = await fetchFindingsForRepo(validUuid);

      expect(result.status).toBe('SUCCESS');
      if (result.status === 'SUCCESS') {
        expect(result.data).toHaveLength(1);
        expect(result.data[0].id).toBe('find-1');
        expect(result.data[0].severity).toBe('CRITICAL');
        expect(result.data[0].status).toBe('OPEN');
        expect(result.data[0].filePath).toBe('src/aws.ts');
        expect(result.data[0].lineNumber).toBe(12);
        expect(result.data[0].rawSecretMasked).toContain('************');
      }
    });

    it('maps HTTP 404 to ERROR with status code 404 (never NOT_FOUND)', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 404,
        json: async () => ({ message: 'Findings resource not found' }),
      } as Response);

      const result = await fetchFindingsForRepo(validUuid);

      expect(result.status).toBe('ERROR');
      if (result.status === 'ERROR') {
        expect(result.statusCode).toBe(404);
        expect(result.error).toContain('Findings resource not found');
      }
    });

    it('maps HTTP 500 server error to ERROR without swallowing', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        json: async () => ({ message: 'Internal server error in findings store' }),
      } as Response);

      const result = await fetchFindingsForRepo(validUuid);

      expect(result.status).toBe('ERROR');
      if (result.status === 'ERROR') {
        expect(result.statusCode).toBe(500);
        expect(result.error).toContain('Internal server error in findings store');
      }
    });

    it('handles network disconnection gracefully with ERROR', async () => {
      vi.spyOn(global, 'fetch').mockRejectedValueOnce(new Error('Failed to fetch / Connection refused'));

      const result = await fetchFindingsForRepo(validUuid);

      expect(result.status).toBe('ERROR');
      if (result.status === 'ERROR') {
        expect(result.error).toContain('Connection refused');
      }
    });

    it('handles malformed JSON body gracefully with ERROR', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => {
          throw new SyntaxError('Unexpected token < in JSON at position 0');
        },
      } as unknown as Response);

      const result = await fetchFindingsForRepo(validUuid);

      expect(result.status).toBe('ERROR');
      if (result.status === 'ERROR') {
        expect(result.error).toContain('Malformed JSON');
      }
    });
  });

  describe('fetchCoverageForRepo', () => {
    it('returns ERROR on invalid UUID without making network request', async () => {
      const fetchSpy = vi.spyOn(global, 'fetch');
      const result = await fetchCoverageForRepo(invalidUuid);

      expect(fetchSpy).not.toHaveBeenCalled();
      expect(result.status).toBe('ERROR');
      if (result.status === 'ERROR') {
        expect(result.error).toContain('Invalid repository UUID');
      }
    });

    it('maps HTTP 200 object response to SUCCESS with CoverageSummaryDto', async () => {
      const mockCoverage = {
        id: 'cov-1',
        scanJobId: 'job-1',
        repositoryId: validUuid,
        branchName: 'main',
        scannedFiles: 42,
        skippedFiles: 0,
        totalFiles: 42,
        coverageImpact: 'COMPLETE',
        createdAt: '2026-08-27T10:00:00Z',
      };

      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => mockCoverage,
      } as Response);

      const result = await fetchCoverageForRepo(validUuid);

      expect(result.status).toBe('SUCCESS');
      if (result.status === 'SUCCESS') {
        expect(result.data.scanJobId).toBe('job-1');
        expect(result.data.coverageImpact).toBe('COMPLETE');
        expect(result.data.scannedFiles).toBe(42);
      }
    });

    it('strictly maps HTTP 404 to NOT_FOUND', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 404,
        json: async () => ({ message: 'Not found' }),
      } as Response);

      const result = await fetchCoverageForRepo(validUuid);

      expect(result.status).toBe('NOT_FOUND');
    });

    it('maps HTTP 500 server error to ERROR without swallowing', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 500,
        json: async () => ({ message: 'Database query timeout' }),
      } as Response);

      const result = await fetchCoverageForRepo(validUuid);

      expect(result.status).toBe('ERROR');
      if (result.status === 'ERROR') {
        expect(result.statusCode).toBe(500);
        expect(result.error).toContain('Database query timeout');
      }
    });

    it('handles network failure with ERROR', async () => {
      vi.spyOn(global, 'fetch').mockRejectedValueOnce(new Error('Network offline'));

      const result = await fetchCoverageForRepo(validUuid);

      expect(result.status).toBe('ERROR');
      if (result.status === 'ERROR') {
        expect(result.error).toContain('Network offline');
      }
    });

    it('handles malformed JSON body with ERROR', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: true,
        status: 200,
        json: async () => {
          throw new SyntaxError('Malformed response');
        },
      } as unknown as Response);

      const result = await fetchCoverageForRepo(validUuid);

      expect(result.status).toBe('ERROR');
      if (result.status === 'ERROR') {
        expect(result.error).toContain('Malformed JSON');
      }
    });
  });

  describe('createFindingIssue 409 allow-list and secret-safe isolation', () => {
    it('maps exact HTTP 409 CREATION_IN_PROGRESS to CREATION_IN_PROGRESS', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 409,
        json: async () => ({ message: 'CREATION_IN_PROGRESS' }),
      } as Response);

      const result = await createFindingIssue(validUuid, { previewToken: 'tok-123' });
      expect(result.status).toBe('ERROR');
      if (result.status === 'ERROR') {
        expect(result.error).toBe('CREATION_IN_PROGRESS');
        expect(result.statusCode).toBe(409);
      }
    });

    it('maps exact HTTP 409 PREVIEW_TOKEN_EXPIRED_OR_INVALID to PREVIEW_TOKEN_EXPIRED_OR_INVALID', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 409,
        json: async () => ({ message: 'PREVIEW_TOKEN_EXPIRED_OR_INVALID' }),
      } as Response);

      const result = await createFindingIssue(validUuid, { previewToken: 'tok-123' });
      expect(result.status).toBe('ERROR');
      if (result.status === 'ERROR') {
        expect(result.error).toBe('PREVIEW_TOKEN_EXPIRED_OR_INVALID');
        expect(result.statusCode).toBe(409);
      }
    });

    it('rejects injected substring "CREATION_IN_PROGRESS injected-text" and maps to ISSUE_CREATION_FAILED', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 409,
        json: async () => ({ message: 'CREATION_IN_PROGRESS injected-text' }),
      } as Response);

      const result = await createFindingIssue(validUuid, { previewToken: 'tok-123' });
      expect(result.status).toBe('ERROR');
      if (result.status === 'ERROR') {
        expect(result.error).toBe('ISSUE_CREATION_FAILED');
        expect(result.statusCode).toBe(409);
      }
    });

    it('rejects injected substring "PREVIEW_TOKEN_EXPIRED_OR_INVALID injected-text" and maps to ISSUE_CREATION_FAILED', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 409,
        json: async () => ({ message: 'PREVIEW_TOKEN_EXPIRED_OR_INVALID injected-text' }),
      } as Response);

      const result = await createFindingIssue(validUuid, { previewToken: 'tok-123' });
      expect(result.status).toBe('ERROR');
      if (result.status === 'ERROR') {
        expect(result.error).toBe('ISSUE_CREATION_FAILED');
        expect(result.statusCode).toBe(409);
      }
    });

    it('maps HTTP 409 with malicious/unknown content to generic ISSUE_CREATION_FAILED without leaking text', async () => {
      vi.spyOn(global, 'fetch').mockResolvedValueOnce({
        ok: false,
        status: 409,
        json: async () => ({ message: 'Fatal exception at /var/app/secrets.env with token gho_secret999' }),
      } as Response);

      const result = await createFindingIssue(validUuid, { previewToken: 'tok-123' });
      expect(result.status).toBe('ERROR');
      if (result.status === 'ERROR') {
        expect(result.error).toBe('ISSUE_CREATION_FAILED');
        expect(result.error).not.toContain('/var/app');
        expect(result.error).not.toContain('gho_secret999');
        expect(result.statusCode).toBe(409);
      }
    });
  });
});
