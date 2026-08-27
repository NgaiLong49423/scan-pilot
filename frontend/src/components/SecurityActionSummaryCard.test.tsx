import React from 'react';
import { describe, it, expect } from 'vitest';
import { renderToStaticMarkup } from 'react-dom/server';
import { SecurityActionSummaryCard } from './SecurityActionSummaryCard';
import { SecurityActionSummary } from '../types';

describe('SecurityActionSummaryCard', () => {
  it('renders loading skeleton when isLoading is true', () => {
    const mockSummary: SecurityActionSummary = {
      status: 'NO_OPEN_FINDINGS',
      statusLabel: 'No Open Findings',
      statusDescription: 'No open findings in this completed scan.',
      severityCounts: { critical: 0, high: 0, medium: 0, low: 0, total: 0 },
      totalFilesScanned: 10,
      totalFilesSkipped: 0,
      coverageImpact: 'COMPLETE',
      coverageRecordedAt: '2026-08-27T10:00:00Z',
      scanCompletedAt: '2026-08-27T10:05:00Z',
      actionPrompt: 'Maintain monitoring.',
    };

    const html = renderToStaticMarkup(
      <SecurityActionSummaryCard summary={mockSummary} isLoading={true} />
    );

    expect(html).toContain('animate-pulse');
    expect(html).not.toContain('No open findings in this completed scan');
  });

  it('renders ACTION_REQUIRED state with severity breakdown and recommended action', () => {
    const mockSummary: SecurityActionSummary = {
      status: 'ACTION_REQUIRED',
      statusLabel: 'Action Required',
      statusDescription: 'Action Required — 3 open security findings require attention.',
      severityCounts: { critical: 2, high: 1, medium: 0, low: 0, total: 3 },
      totalFilesScanned: 50,
      totalFilesSkipped: 2,
      coverageImpact: 'COMPLETE',
      coverageRecordedAt: '2026-08-27T10:00:00Z',
      scanCompletedAt: '2026-08-27T10:05:00Z',
      actionPrompt: 'Revoke and rotate exposed credentials immediately.',
    };

    const html = renderToStaticMarkup(
      <SecurityActionSummaryCard summary={mockSummary} />
    );

    expect(html).toContain('Action Required');
    expect(html).toContain('Action Required — 3 open security findings require attention.');
    expect(html).toContain('Revoke and rotate exposed credentials immediately.');
    expect(html).toContain('50');
    expect(html).toContain('CRITICAL');
    expect(html).not.toContain('/100');
    expect(html).not.toContain('Grade');
  });

  it('renders NO_OPEN_FINDINGS state truthfully without whole-history safety claims', () => {
    const mockSummary: SecurityActionSummary = {
      status: 'NO_OPEN_FINDINGS',
      statusLabel: 'No Open Findings',
      statusDescription: 'No open findings in this completed scan.',
      severityCounts: { critical: 0, high: 0, medium: 0, low: 0, total: 0 },
      totalFilesScanned: 42,
      totalFilesSkipped: 0,
      coverageImpact: 'COMPLETE',
      coverageRecordedAt: '2026-08-27T10:00:00Z',
      scanCompletedAt: '2026-08-27T10:05:00Z',
      actionPrompt: 'Maintain continuous monitoring.',
    };

    const html = renderToStaticMarkup(
      <SecurityActionSummaryCard summary={mockSummary} />
    );

    expect(html).toContain('No Open Findings');
    expect(html).toContain('No open findings in this completed scan.');
    expect(html).not.toContain('100% Safe');
    expect(html).not.toContain('Clean History');
  });

  it('renders COVERAGE_INCOMPLETE with open findings (Bifurcation A)', () => {
    const mockSummary: SecurityActionSummary = {
      status: 'COVERAGE_INCOMPLETE',
      statusLabel: 'Coverage Incomplete',
      statusDescription: 'Coverage Incomplete — 1 verified open security finding requires attention. Resource limits or timeouts prevented scanning all files, so additional findings may remain.',
      severityCounts: { critical: 1, high: 0, medium: 0, low: 0, total: 1 },
      totalFilesScanned: 100,
      totalFilesSkipped: 15,
      coverageImpact: 'INCOMPLETE',
      coverageRecordedAt: '2026-08-27T10:00:00Z',
      scanCompletedAt: '2026-08-27T10:05:00Z',
      actionPrompt: 'Remediate detected open findings and adjust repository size limits for full audit.',
    };

    const html = renderToStaticMarkup(
      <SecurityActionSummaryCard summary={mockSummary} />
    );

    expect(html).toContain('Coverage Incomplete');
    expect(html).toContain('1 verified open security finding requires attention');
    expect(html).toContain('Skipped:');
    expect(html).toContain('15');
  });

  it('renders COVERAGE_INCOMPLETE with 0 open findings (Bifurcation B)', () => {
    const mockSummary: SecurityActionSummary = {
      status: 'COVERAGE_INCOMPLETE',
      statusLabel: 'Coverage Incomplete',
      statusDescription: 'Coverage Incomplete — Resource limits or timeouts prevented scanning all files. No findings were detected in the scanned portion, but unscanned files remain.',
      severityCounts: { critical: 0, high: 0, medium: 0, low: 0, total: 0 },
      totalFilesScanned: 100,
      totalFilesSkipped: 20,
      coverageImpact: 'INCOMPLETE',
      coverageRecordedAt: '2026-08-27T10:00:00Z',
      scanCompletedAt: '2026-08-27T10:05:00Z',
      actionPrompt: 'Adjust repository scope or increase guardrail limits to achieve complete audit coverage.',
    };

    const html = renderToStaticMarkup(
      <SecurityActionSummaryCard summary={mockSummary} />
    );

    expect(html).toContain('Coverage Incomplete');
    expect(html).toContain('No findings were detected in the scanned portion');
  });

  it('renders AWAITING_INITIAL_SCAN state cleanly', () => {
    const mockSummary: SecurityActionSummary = {
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

    const html = renderToStaticMarkup(
      <SecurityActionSummaryCard summary={mockSummary} />
    );

    expect(html).toContain('Awaiting Initial Scan');
    expect(html).toContain('Repository connected. Trigger a scan to evaluate security posture.');
  });

  it('renders SCAN_IN_PROGRESS state pointing to live collection', () => {
    const mockSummary: SecurityActionSummary = {
      status: 'SCAN_IN_PROGRESS',
      statusLabel: 'Scan In Progress',
      statusDescription: 'Live scan evidence is still being collected. Follow the live terminal for the current stage.',
      severityCounts: { critical: 0, high: 0, medium: 0, low: 0, total: 0 },
      totalFilesScanned: 12,
      totalFilesSkipped: 0,
      coverageImpact: null,
      coverageRecordedAt: null,
      scanCompletedAt: null,
      actionPrompt: 'Scan actively executing in background. Awaiting final evidence recording.',
    };

    const html = renderToStaticMarkup(
      <SecurityActionSummaryCard summary={mockSummary} />
    );

    expect(html).toContain('Scan In Progress');
    expect(html).toContain('Live scan evidence is still being collected');
  });

  it('renders SCAN_UNAVAILABLE error state and includes Retry button when onRetry is passed', () => {
    const mockSummary: SecurityActionSummary = {
      status: 'SCAN_UNAVAILABLE',
      statusLabel: 'Scan Unavailable',
      statusDescription: 'Scan Failed — The last scan job could not complete. Inspect the live terminal and retry.',
      severityCounts: { critical: 0, high: 0, medium: 0, low: 0, total: 0 },
      totalFilesScanned: null,
      totalFilesSkipped: null,
      coverageImpact: null,
      coverageRecordedAt: null,
      scanCompletedAt: null,
      actionPrompt: 'Retry evidence retrieval or trigger a fresh scan.',
    };

    const htmlWithRetry = renderToStaticMarkup(
      <SecurityActionSummaryCard summary={mockSummary} onRetry={() => {}} />
    );

    expect(htmlWithRetry).toContain('Scan Unavailable');
    expect(htmlWithRetry).toContain('Retry Evidence');

    const htmlWithoutRetry = renderToStaticMarkup(
      <SecurityActionSummaryCard summary={mockSummary} />
    );

    expect(htmlWithoutRetry).not.toContain('Retry Evidence');
  });
});
