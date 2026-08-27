import { ApiResult, Finding, SecurityActionSummary, FindingSeverityCounts } from '../types';
import { CoverageSummaryDto, ScanJobPollResult } from './api';

/**
 * Pure, deterministic resolver that maps raw backend evidence into truthful security action summaries.
 * Strictly follows the locked 7-rule precedence hierarchy.
 * Zero dynamic diagnostic leakage (ScanJobDto.errorMessage, paths, stack traces).
 */
export function resolveRepositoryPosture(
  findingsResult: ApiResult<Finding[]>,
  coverageResult: ApiResult<CoverageSummaryDto>,
  scanJobResult?: ScanJobPollResult | null
): SecurityActionSummary {
  // 1. Evidence / Retrieval Errors (Rule 1)
  if (findingsResult.status === 'ERROR') {
    return createUnavailableSummary('Unable to retrieve verified security findings from backend. Check connection and retry.');
  }
  if (coverageResult.status === 'ERROR') {
    return createUnavailableSummary('Unable to retrieve verified coverage record from backend. Check connection and retry.');
  }

  // 2. Awaiting Initial Scan (Strict Criteria - Rule 7)
  if (coverageResult.status === 'NOT_FOUND' && findingsResult.status === 'SUCCESS' && findingsResult.data.length === 0) {
    return {
      status: 'AWAITING_INITIAL_SCAN',
      statusLabel: 'Awaiting Initial Scan',
      statusDescription: 'Repository connected. Trigger a scan to evaluate security posture.',
      severityCounts: { critical: 0, high: 0, medium: 0, low: 0, total: 0 },
      totalFilesScanned: null,
      totalFilesSkipped: null,
      coverageImpact: null,
      coverageRecordedAt: null,
      scanCompletedAt: null,
      actionPrompt: 'Trigger initial security audit to establish verified posture baseline.',
    };
  }

  if (coverageResult.status !== 'SUCCESS') {
    return createUnavailableSummary('Unable to verify repository coverage baseline.');
  }

  const coverage = coverageResult.data;
  if (!coverage.scanJobId) {
    return createUnavailableSummary('Coverage evidence is missing valid scan job identifier.');
  }

  if (!scanJobResult || !scanJobResult.success || !scanJobResult.job) {
    return createUnavailableSummary('Unable to verify scan job completion status from backend. Check connection and retry.');
  }

  const job = scanJobResult.job;
  const findings = findingsResult.status === 'SUCCESS' ? findingsResult.data : [];
  const openFindings = findings.filter((f) => f.status === 'OPEN');
  const severityCounts = computeSeverityCounts(openFindings);

  // 3. Failed Scan Dominance (Rule 2: Zero Raw Error Message Leakage)
  if (job.status === 'FAILED') {
    return createUnavailableSummary('Scan Failed — The last scan job could not complete. Inspect the live terminal and retry.');
  }

  // 4. Active Scan In Progress (Rule 3)
  if (job.status === 'QUEUED' || job.status === 'RUNNING') {
    const impact = coverage.coverageImpact === 'COMPLETE' || coverage.coverageImpact === 'INCOMPLETE' || coverage.coverageImpact === 'NONE'
      ? coverage.coverageImpact
      : null;

    return {
      status: 'SCAN_IN_PROGRESS',
      statusLabel: 'Scan In Progress',
      statusDescription: 'Live scan evidence is still being collected. Follow the live terminal for the current stage.',
      severityCounts,
      totalFilesScanned: coverage.scannedFiles ?? null,
      totalFilesSkipped: coverage.skippedFiles ?? null,
      coverageImpact: impact,
      coverageRecordedAt: coverage.createdAt ?? null,
      scanCompletedAt: null,
      actionPrompt: 'Scan actively executing in background. Awaiting final evidence recording.',
    };
  }

  // 5. Completed Scan with Incomplete Coverage (Rule 4)
  if (job.status === 'COMPLETED' && coverage.coverageImpact === 'INCOMPLETE') {
    const desc = openFindings.length > 0
      ? `Coverage Incomplete — ${openFindings.length} verified open security ${openFindings.length === 1 ? 'finding requires' : 'findings require'} attention. Resource limits or timeouts prevented scanning all files, so additional findings may remain.`
      : 'Coverage Incomplete — Resource limits or timeouts prevented scanning all files. No findings were detected in the scanned portion, but unscanned files remain.';
    
    return {
      status: 'COVERAGE_INCOMPLETE',
      statusLabel: 'Coverage Incomplete',
      statusDescription: desc,
      severityCounts,
      totalFilesScanned: coverage.scannedFiles ?? null,
      totalFilesSkipped: coverage.skippedFiles ?? null,
      coverageImpact: 'INCOMPLETE',
      coverageRecordedAt: coverage.createdAt ?? null,
      scanCompletedAt: job.completedAt ?? null,
      actionPrompt: openFindings.length > 0 ? 'Remediate detected open findings and adjust repository size limits for full audit.' : 'Adjust repository scope or increase guardrail limits to achieve complete audit coverage.',
    };
  }

  // 6. Completed Scan with Action Required (Rule 5 - Strictly requires COMPLETE coverage)
  if (job.status === 'COMPLETED' && openFindings.length > 0 && coverage.coverageImpact === 'COMPLETE') {
    return {
      status: 'ACTION_REQUIRED',
      statusLabel: 'Action Required',
      statusDescription: `Action Required — ${openFindings.length} open security ${openFindings.length === 1 ? 'finding requires' : 'findings require'} attention.`,
      severityCounts,
      totalFilesScanned: coverage.scannedFiles ?? null,
      totalFilesSkipped: coverage.skippedFiles ?? null,
      coverageImpact: 'COMPLETE',
      coverageRecordedAt: coverage.createdAt ?? null,
      scanCompletedAt: job.completedAt ?? null,
      actionPrompt: 'Revoke and rotate exposed credentials immediately, then apply guided remediation.',
    };
  }

  // 7. Completed Scan with No Open Findings (Rule 6)
  if (job.status === 'COMPLETED' && openFindings.length === 0 && coverage.coverageImpact === 'COMPLETE') {
    return {
      status: 'NO_OPEN_FINDINGS',
      statusLabel: 'No Open Findings',
      statusDescription: 'No open findings in this completed scan.',
      severityCounts: { critical: 0, high: 0, medium: 0, low: 0, total: 0 },
      totalFilesScanned: coverage.scannedFiles ?? null,
      totalFilesSkipped: coverage.skippedFiles ?? null,
      coverageImpact: 'COMPLETE',
      coverageRecordedAt: coverage.createdAt ?? null,
      scanCompletedAt: job.completedAt ?? null,
      actionPrompt: 'Maintain continuous monitoring. Posture verified clean across all scanned files in this completed audit.',
    };
  }

  return createUnavailableSummary('Scan posture indeterminate. Check backend status.');
}

function createUnavailableSummary(description: string): SecurityActionSummary {
  return {
    status: 'SCAN_UNAVAILABLE',
    statusLabel: 'Scan Unavailable',
    statusDescription: description,
    severityCounts: { critical: 0, high: 0, medium: 0, low: 0, total: 0 },
    totalFilesScanned: null,
    totalFilesSkipped: null,
    coverageImpact: null,
    coverageRecordedAt: null,
    scanCompletedAt: null,
    actionPrompt: 'Retry evidence retrieval or trigger a fresh scan.',
  };
}

function computeSeverityCounts(findings: Finding[]): FindingSeverityCounts {
  let critical = 0;
  let high = 0;
  let medium = 0;
  let low = 0;
  for (const f of findings) {
    const sev = (f.severity || '').toUpperCase();
    if (sev === 'CRITICAL') critical++;
    else if (sev === 'HIGH') high++;
    else if (sev === 'MEDIUM') medium++;
    else if (sev === 'LOW') low++;
  }
  return { critical, high, medium, low, total: findings.length };
}
