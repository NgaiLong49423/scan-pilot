import { describe, it, expect, vi } from 'vitest';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { FleetDashboard } from './FleetDashboard';
import { Repository, UserProfile } from '../types';

describe('FleetDashboard Component', () => {
  const dummyUser: UserProfile = {
    githubUserId: 12345,
    login: 'testorg',
    name: 'Test Org',
    avatarUrl: 'https://avatars.githubusercontent.com/u/12345',
  };

  const mixedFleet: Repository[] = [
    {
      id: 'repo-1',
      githubRepoId: 101,
      name: 'acme/auth-service',
      branch: 'main',
      isPrivate: false,
      language: 'TypeScript',
      lastScanned: '2026-08-27T10:00:00Z',
      isScanned: true,
      findingCount: 2,
      attentionStatus: 'Warning',
      postureStatus: 'ACTION_REQUIRED',
      severityCounts: {
        critical: 1,
        high: 1,
        medium: 0,
        low: 0,
        total: 2,
      },
    },
    {
      id: 'repo-2',
      githubRepoId: 102,
      name: 'acme/payment-gateway',
      branch: 'main',
      isPrivate: true,
      language: 'Java',
      lastScanned: '2026-08-27T10:30:00Z',
      isScanned: true,
      findingCount: 1,
      attentionStatus: 'Warning',
      postureStatus: 'COVERAGE_INCOMPLETE', // Incomplete coverage with open finding
      severityCounts: {
        critical: 0,
        high: 1,
        medium: 0,
        low: 0,
        total: 1,
      },
    },
    {
      id: 'repo-3',
      githubRepoId: 103,
      name: 'acme/clean-service',
      branch: 'main',
      isPrivate: false,
      language: 'Go',
      lastScanned: '2026-08-27T11:00:00Z',
      isScanned: true,
      findingCount: 0,
      attentionStatus: 'Secure',
      postureStatus: 'NO_OPEN_FINDINGS',
      severityCounts: {
        critical: 0,
        high: 0,
        medium: 0,
        low: 0,
        total: 0,
      },
    },
    {
      id: 'repo-4',
      githubRepoId: 104,
      name: 'acme/unscanned-repo',
      branch: 'main',
      isPrivate: false,
      language: 'Python',
      lastScanned: null,
      isScanned: false,
      findingCount: 0,
      attentionStatus: 'NotScanned',
      postureStatus: 'AWAITING_INITIAL_SCAN',
    },
    {
      id: 'repo-5',
      githubRepoId: 105,
      name: 'acme/active-scan-repo',
      branch: 'main',
      isPrivate: false,
      language: 'Rust',
      lastScanned: null,
      isScanned: true, // Simulation of stale boolean flag during in-progress scan
      findingCount: 5, // Simulation of stale finding count during in-progress scan
      attentionStatus: 'Warning',
      postureStatus: 'SCAN_IN_PROGRESS',
      severityCounts: {
        critical: 2,
        high: 2,
        medium: 1,
        low: 0,
        total: 5,
      },
    },
    {
      id: 'repo-6',
      githubRepoId: 106,
      name: 'acme/broken-scan-repo',
      branch: 'main',
      isPrivate: false,
      language: 'C++',
      lastScanned: 'Failed',
      isScanned: false,
      findingCount: 0,
      attentionStatus: 'Warning',
      postureStatus: 'SCAN_UNAVAILABLE',
    },
  ];

  it('renders aggregate counts correctly with mixed fleet strictly excluding in-progress scans from audited and action-required aggregates', () => {
    const html = renderToStaticMarkup(
      <FleetDashboard
        monitoredRepositories={mixedFleet}
        currentUser={dummyUser}
        onSelectRepo={vi.fn()}
        onOpenImportModal={vi.fn()}
        onRetry={vi.fn()}
      />
    );

    // Monitored fleet total = 6
    expect(html).toContain('6');
    expect(html).toContain('Organization Fleet Overview');
    expect(html).toContain('testorg');

    // Card 1: Monitored Fleet breakdown separates Awaiting Scan and Evidence Unavailable
    // Strictly contains 3 Audited repos (repo-1, repo-2, repo-3); repo-5 (SCAN_IN_PROGRESS) is NOT audited
    expect(html).toContain('3 Audited • 1 Awaiting Scan • 1 Evidence Unavailable');

    // Card 2: Action Required tab includes ONLY terminal actionable repos (repo-1 and repo-2); repo-5 is excluded
    expect(html).toContain('Action Required (2)');

    // Card 3: Total Open Leaks = 3 across exactly 2 repositories (repo-5 leaks are excluded; no double counting)
    expect(html).toContain('3');
    expect(html).toContain('Across 2 Repositories');

    // Card 3 Severity Distribution breakdown (1 Crit, 2 High, 0 Med, 0 Low); repo-5 counts are excluded
    expect(html).toContain('1 Crit');
    expect(html).toContain('2 High');
    expect(html).toContain('0 Med');
    expect(html).toContain('0 Low');

    // Incomplete coverage tab includes repo-2 (total: 1)
    expect(html).toContain('Incomplete Coverage (1)');

    // No Open Findings tab includes repo-3 (total: 1)
    expect(html).toContain('No Open Findings (1)');

    // In Progress tab includes repo-5 (total: 1)
    expect(html).toContain('In Progress (1)');

    // Awaiting Scan tab includes repo-4 (total: 1)
    expect(html).toContain('Awaiting Scan (1)');

    // Table rows render truthful badges for all repositories
    expect(html).toContain('acme/auth-service');
    expect(html).toContain('2 Open Findings'); // Action Required repo badge

    expect(html).toContain('acme/payment-gateway');
    expect(html).toContain('Coverage Incomplete (1 open)'); // Incomplete coverage repo badge

    expect(html).toContain('acme/clean-service');
    expect(html).toContain('No Open Findings');

    expect(html).toContain('acme/unscanned-repo');
    expect(html).toContain('Awaiting Initial Scan');

    expect(html).toContain('acme/active-scan-repo');
    expect(html).toContain('Scan In Progress');

    expect(html).toContain('acme/broken-scan-repo');
    expect(html).toContain('Scan Unavailable');

    // Verify Refresh Fleet button is rendered with focus styling
    expect(html).toContain('Refresh Fleet');
    expect(html).toContain('focus:ring-[#58a6ff]');

    // Absolute absence of synthetic health scores or grades
    expect(html).not.toContain('/100');
    expect(html).not.toContain('Grade A');
    expect(html).not.toContain('Grade B');
  });
});
