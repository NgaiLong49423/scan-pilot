import { describe, it, expect } from 'vitest';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { CoverageAuditView } from './CoverageAuditView';
import { Repository } from '../types';
import { CoverageSummary } from '../types/api';

const mockRepo: Repository = {
  id: 'repo-1',
  dbRepositoryId: 'd0a1b2c3-4567-89ab-cdef-0123456789ab',
  name: 'acme/webapp',
  branch: 'main',
  isPrivate: false,
  language: 'TypeScript',
  findingCount: 0,
  attentionStatus: 'Secure',
  isScanned: true // Even if repo.isScanned is true, coverageImpact is the source of truth
};

describe('CoverageAuditView (Evidence-First Verification)', () => {
  it('when coverageImpact is INCOMPLETE: must render warning and NEVER render "Verified Complete" or remediation-ready claims', () => {
    const incompleteCoverage: CoverageSummary = {
      id: 'cov-1',
      repositoryId: 'repo-1',
      branchName: 'main',
      totalFiles: 50,
      scannedFiles: 20,
      skippedFiles: 5,
      textFiles: 20,
      binaryFiles: 5,
      undeterminedFiles: 25,
      totalBytes: 512000,
      coverageImpact: 'INCOMPLETE',
      createdAt: new Date().toISOString(),
      items: []
    };

    const html = renderToStaticMarkup(
      <CoverageAuditView repo={mockRepo} coverageData={incompleteCoverage} />
    );

    // 1. Must render incomplete warning banner & badge
    expect(html).toContain('Coverage Incomplete');
    expect(html).toContain('INCOMPLETE COVERAGE');
    expect(html).toContain('did not achieve full coverage');

    // 2. Must NOT claim "Verified Complete" on any stage
    expect(html).not.toContain('Verified Complete');

    // 3. Must NOT claim remediation ready or hardcoded engines
    expect(html).not.toContain('>Ready<');
    expect(html).not.toContain('Streamed GitHub Tarball');
    expect(html).not.toContain('Gitleaks AST Ruleset');
    expect(html).not.toContain('SP-CONFIG-001 Diff Engine');
    expect(html).not.toContain('Deterministic Patch');
  });

  it('when coverageImpact is COMPLETE: renders Stage 1 Verified Complete and displays "Not available in coverage evidence" for unevidenced fields', () => {
    const completeCoverage: CoverageSummary = {
      id: 'cov-2',
      repositoryId: 'repo-1',
      branchName: 'main',
      totalFiles: 30,
      scannedFiles: 28,
      skippedFiles: 2,
      textFiles: 28,
      binaryFiles: 2,
      undeterminedFiles: 0,
      totalBytes: 256000,
      coverageImpact: 'COMPLETE',
      createdAt: new Date().toISOString(),
      items: [
        {
          id: 'item-1',
          filePath: 'build/output.bin',
          classification: 'BINARY',
          sizeBytes: 10240,
          status: 'SKIPPED',
          reasonCode: 'UNSUPPORTED_BINARY_FILE',
          impact: 'SKIPPED',
          details: 'Excluded binary artifact'
        }
      ]
    };

    const html = renderToStaticMarkup(
      <CoverageAuditView repo={mockRepo} coverageData={completeCoverage} />
    );

    // 1. Must render Stage 1 Verified Complete
    expect(html).toContain('Verified Complete');

    // 2. Must render "Not available in coverage evidence" for unevidenced engines
    expect(html).toContain('Not available in coverage evidence');

    // 3. Must NOT render hardcoded speculative engines
    expect(html).not.toContain('Streamed GitHub Tarball');
    expect(html).not.toContain('Gitleaks AST Ruleset');
    expect(html).not.toContain('SP-CONFIG-001 Diff Engine');

    // 4. Must render real skipped item
    expect(html).toContain('build/output.bin');
  });

  it('when coverage data is missing (Awaiting): renders Awaiting Coverage and no unevidenced claims', () => {
    const unscannedRepo: Repository = {
      ...mockRepo,
      isScanned: false
    };

    const html = renderToStaticMarkup(
      <CoverageAuditView repo={unscannedRepo} coverageData={null} />
    );

    expect(html).toContain('Awaiting Coverage');
    expect(html).not.toContain('Verified Complete');
    expect(html).not.toContain('Coverage Incomplete');
  });
});
