import { describe, it, expect } from 'vitest';
import { resolveRepositoryPosture } from './postureResolver';
import { ApiResult, Finding } from '../types';
import { CoverageSummaryDto, ScanJobPollResult } from './api';

describe('services/postureResolver', () => {
  const dummyFinding: Finding = {
    id: 'f-1',
    ruleId: 'SP-SECRET-001',
    ruleName: 'Hardcoded Secret',
    severity: 'CRITICAL',
    status: 'OPEN',
    remediationQuality: 'ACTION_REQUIRED',
    filePath: 'src/config.ts',
    lineNumber: 10,
    rawSecretMasked: 'AKIA************',
    detectedCommit: 'abcdef1',
    detectedAt: '12:00:00',
    remediationDiff: {
      filePath: 'src/config.ts',
      startLine: 10,
      originalSnippet: 'const key = "secret";',
      suggestedFixSnippet: 'const key = process.env.KEY;',
      explanation: 'Use env',
    },
  };

  const validCoverage: CoverageSummaryDto = {
    id: 'cov-1',
    scanJobId: 'job-1',
    repositoryId: '11111111-2222-3333-4444-555555555555',
    branchName: 'main',
    scannedFiles: 25,
    skippedFiles: 0,
    totalFiles: 25,
    coverageImpact: 'COMPLETE',
    createdAt: '2026-08-27T10:00:00Z',
  };

  const successfulJobPoll: ScanJobPollResult = {
    success: true,
    job: {
      id: 'job-1',
      repositoryId: '11111111-2222-3333-4444-555555555555',
      branchName: 'main',
      status: 'COMPLETED',
      completedAt: '2026-08-27T10:05:00Z',
    },
  };

  // Rule 1: Evidence / Retrieval Errors
  describe('Rule 1: Retrieval/Evidence Failure', () => {
    it('resolves to SCAN_UNAVAILABLE if findingsResult is ERROR', () => {
      const findingsResult: ApiResult<Finding[]> = { status: 'ERROR', error: 'Network timeout' };
      const coverageResult: ApiResult<CoverageSummaryDto> = { status: 'SUCCESS', data: validCoverage };

      const summary = resolveRepositoryPosture(findingsResult, coverageResult, successfulJobPoll);

      expect(summary.status).toBe('SCAN_UNAVAILABLE');
      expect(summary.statusDescription).toContain('Unable to retrieve verified security findings');
    });

    it('resolves to SCAN_UNAVAILABLE if coverageResult is ERROR', () => {
      const findingsResult: ApiResult<Finding[]> = { status: 'SUCCESS', data: [] };
      const coverageResult: ApiResult<CoverageSummaryDto> = { status: 'ERROR', error: 'Server 500' };

      const summary = resolveRepositoryPosture(findingsResult, coverageResult, successfulJobPoll);

      expect(summary.status).toBe('SCAN_UNAVAILABLE');
      expect(summary.statusDescription).toContain('Unable to retrieve verified coverage record');
    });

    it('resolves to SCAN_UNAVAILABLE if coverage is missing scanJobId', () => {
      const findingsResult: ApiResult<Finding[]> = { status: 'SUCCESS', data: [] };
      const coverageResult: ApiResult<CoverageSummaryDto> = {
        status: 'SUCCESS',
        data: { ...validCoverage, scanJobId: undefined },
      };

      const summary = resolveRepositoryPosture(findingsResult, coverageResult, successfulJobPoll);

      expect(summary.status).toBe('SCAN_UNAVAILABLE');
      expect(summary.statusDescription).toContain('Coverage evidence is missing valid scan job identifier');
    });

    it('resolves to SCAN_UNAVAILABLE if scanJobResult is missing or unsuccessful', () => {
      const findingsResult: ApiResult<Finding[]> = { status: 'SUCCESS', data: [] };
      const coverageResult: ApiResult<CoverageSummaryDto> = { status: 'SUCCESS', data: validCoverage };

      const summary = resolveRepositoryPosture(findingsResult, coverageResult, null);

      expect(summary.status).toBe('SCAN_UNAVAILABLE');
      expect(summary.statusDescription).toContain('Unable to verify scan job completion status');
    });
  });

  // Rule 2: Failed Scan Dominance
  describe('Rule 2: Failed Scan Dominance', () => {
    it('resolves to SCAN_UNAVAILABLE with safe static message and never leaks raw errorMessage or secrets', () => {
      const sensitiveLeak = 'DB Connection Failed: postgres://admin:supersecret@10.0.0.1:5432/db';
      const findingsResult: ApiResult<Finding[]> = { status: 'SUCCESS', data: [dummyFinding] };
      const coverageResult: ApiResult<CoverageSummaryDto> = { status: 'SUCCESS', data: validCoverage };
      const failedJobPoll: ScanJobPollResult = {
        success: true,
        job: {
          id: 'job-1',
          repositoryId: '11111111-2222-3333-4444-555555555555',
          status: 'FAILED',
          errorMessage: sensitiveLeak,
        },
      };

      const summary = resolveRepositoryPosture(findingsResult, coverageResult, failedJobPoll);

      expect(summary.status).toBe('SCAN_UNAVAILABLE');
      expect(summary.statusDescription).toBe('Scan Failed — The last scan job could not complete. Inspect the live terminal and retry.');
      expect(summary.statusDescription).not.toContain(sensitiveLeak);
      expect(summary.statusDescription).not.toContain('supersecret');
      expect(summary.actionPrompt).toBe('Retry evidence retrieval or trigger a fresh scan.');
    });
  });

  // Rule 3: Active Scan In Progress
  describe('Rule 3: Active Scan In Progress', () => {
    it('resolves to SCAN_IN_PROGRESS when job status is QUEUED', () => {
      const findingsResult: ApiResult<Finding[]> = { status: 'SUCCESS', data: [] };
      const coverageResult: ApiResult<CoverageSummaryDto> = { status: 'SUCCESS', data: validCoverage };
      const queuedJob: ScanJobPollResult = {
        success: true,
        job: {
          id: 'job-1',
          repositoryId: '11111111-2222-3333-4444-555555555555',
          status: 'QUEUED',
        },
      };

      const summary = resolveRepositoryPosture(findingsResult, coverageResult, queuedJob);

      expect(summary.status).toBe('SCAN_IN_PROGRESS');
      expect(summary.statusLabel).toBe('Scan In Progress');
      expect(summary.statusDescription).toContain('Live scan evidence is still being collected');
      expect(summary.scanCompletedAt).toBeNull();
    });

    it('resolves to SCAN_IN_PROGRESS when job status is RUNNING', () => {
      const findingsResult: ApiResult<Finding[]> = { status: 'SUCCESS', data: [] };
      const coverageResult: ApiResult<CoverageSummaryDto> = { status: 'SUCCESS', data: validCoverage };
      const runningJob: ScanJobPollResult = {
        success: true,
        job: {
          id: 'job-1',
          repositoryId: '11111111-2222-3333-4444-555555555555',
          status: 'RUNNING',
        },
      };

      const summary = resolveRepositoryPosture(findingsResult, coverageResult, runningJob);

      expect(summary.status).toBe('SCAN_IN_PROGRESS');
      expect(summary.statusLabel).toBe('Scan In Progress');
    });
  });

  // Rule 4: Incomplete Coverage on Completed Scan
  describe('Rule 4: Incomplete Coverage on Completed Scan', () => {
    it('bifurcation A: discloses open findings and warns about unscanned files when open findings > 0', () => {
      const findingsResult: ApiResult<Finding[]> = { status: 'SUCCESS', data: [dummyFinding] };
      const incompleteCoverage: CoverageSummaryDto = {
        ...validCoverage,
        coverageImpact: 'INCOMPLETE',
        scannedFiles: 10,
        skippedFiles: 5,
      };

      const summary = resolveRepositoryPosture(findingsResult, { status: 'SUCCESS', data: incompleteCoverage }, successfulJobPoll);

      expect(summary.status).toBe('COVERAGE_INCOMPLETE');
      expect(summary.statusLabel).toBe('Coverage Incomplete');
      expect(summary.statusDescription).toContain('1 verified open security finding requires attention');
      expect(summary.statusDescription).toContain('Resource limits or timeouts prevented scanning all files, so additional findings may remain');
      expect(summary.severityCounts.total).toBe(1);
      expect(summary.severityCounts.critical).toBe(1);
      expect(summary.actionPrompt).toContain('Remediate detected open findings');
    });

    it('bifurcation B: discloses 0 findings in scanned portion and warns about unscanned files when open findings == 0', () => {
      const findingsResult: ApiResult<Finding[]> = { status: 'SUCCESS', data: [] };
      const incompleteCoverage: CoverageSummaryDto = {
        ...validCoverage,
        coverageImpact: 'INCOMPLETE',
        scannedFiles: 10,
        skippedFiles: 5,
      };

      const summary = resolveRepositoryPosture(findingsResult, { status: 'SUCCESS', data: incompleteCoverage }, successfulJobPoll);

      expect(summary.status).toBe('COVERAGE_INCOMPLETE');
      expect(summary.statusLabel).toBe('Coverage Incomplete');
      expect(summary.statusDescription).toBe('Coverage Incomplete — Resource limits or timeouts prevented scanning all files. No findings were detected in the scanned portion, but unscanned files remain.');
      expect(summary.severityCounts.total).toBe(0);
      expect(summary.actionPrompt).toContain('Adjust repository scope or increase guardrail limits');
    });
  });

  // Rule 5: Completed Scan with Action Required
  describe('Rule 5: Action Required on Completed Scan', () => {
    it('resolves to ACTION_REQUIRED when coverage is COMPLETE and open findings exist', () => {
      const findingsResult: ApiResult<Finding[]> = {
        status: 'SUCCESS',
        data: [
          dummyFinding,
          { ...dummyFinding, id: 'f-2', severity: 'HIGH' },
          { ...dummyFinding, id: 'f-3', severity: 'MEDIUM' },
          { ...dummyFinding, id: 'f-4', severity: 'LOW' },
          { ...dummyFinding, id: 'f-5', status: 'RESOLVED' }, // resolved finding not counted in open
        ],
      };
      const coverageResult: ApiResult<CoverageSummaryDto> = { status: 'SUCCESS', data: validCoverage };

      const summary = resolveRepositoryPosture(findingsResult, coverageResult, successfulJobPoll);

      expect(summary.status).toBe('ACTION_REQUIRED');
      expect(summary.statusLabel).toBe('Action Required');
      expect(summary.statusDescription).toBe('Action Required — 4 open security findings require attention.');
      expect(summary.severityCounts.critical).toBe(1);
      expect(summary.severityCounts.high).toBe(1);
      expect(summary.severityCounts.medium).toBe(1);
      expect(summary.severityCounts.low).toBe(1);
      expect(summary.severityCounts.total).toBe(4);
      expect(summary.actionPrompt).toContain('Revoke and rotate exposed credentials immediately');
    });

    it('resolves to SCAN_UNAVAILABLE when completed scan has open findings but coverageImpact is NONE', () => {
      const findingsResult: ApiResult<Finding[]> = {
        status: 'SUCCESS',
        data: [dummyFinding],
      };
      const noneCoverage: CoverageSummaryDto = {
        ...validCoverage,
        coverageImpact: 'NONE',
      };
      const coverageResult: ApiResult<CoverageSummaryDto> = { status: 'SUCCESS', data: noneCoverage };

      const summary = resolveRepositoryPosture(findingsResult, coverageResult, successfulJobPoll);

      expect(summary.status).toBe('SCAN_UNAVAILABLE');
      expect(summary.statusLabel).toBe('Scan Unavailable');
      expect(summary.statusDescription).toBe('Scan posture indeterminate. Check backend status.');
    });
  });

  // Rule 6: Completed Scan with No Open Findings
  describe('Rule 6: No Open Findings on Completed Scan', () => {
    it('resolves to NO_OPEN_FINDINGS when coverage is COMPLETE and open findings is 0', () => {
      const findingsResult: ApiResult<Finding[]> = {
        status: 'SUCCESS',
        data: [{ ...dummyFinding, id: 'f-resolved', status: 'RESOLVED' }],
      };
      const coverageResult: ApiResult<CoverageSummaryDto> = { status: 'SUCCESS', data: validCoverage };

      const summary = resolveRepositoryPosture(findingsResult, coverageResult, successfulJobPoll);

      expect(summary.status).toBe('NO_OPEN_FINDINGS');
      expect(summary.statusLabel).toBe('No Open Findings');
      expect(summary.statusDescription).toBe('No open findings in this completed scan.');
      expect(summary.severityCounts.total).toBe(0);
      expect(summary.actionPrompt).toContain('Maintain continuous monitoring');
    });
  });

  // Rule 7: Awaiting Initial Scan
  describe('Rule 7: Awaiting Initial Scan', () => {
    it('resolves to AWAITING_INITIAL_SCAN when coverage is NOT_FOUND and findings is SUCCESS([])', () => {
      const findingsResult: ApiResult<Finding[]> = { status: 'SUCCESS', data: [] };
      const coverageResult: ApiResult<CoverageSummaryDto> = { status: 'NOT_FOUND' };

      const summary = resolveRepositoryPosture(findingsResult, coverageResult, null);

      expect(summary.status).toBe('AWAITING_INITIAL_SCAN');
      expect(summary.statusLabel).toBe('Awaiting Initial Scan');
      expect(summary.statusDescription).toBe('Repository connected. Trigger a scan to evaluate security posture.');
      expect(summary.totalFilesScanned).toBeNull();
      expect(summary.actionPrompt).toContain('Trigger initial security audit');
    });
  });
});
