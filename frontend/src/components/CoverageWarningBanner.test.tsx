import { describe, it, expect } from 'vitest';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { CoverageWarningBanner } from './CoverageWarningBanner';

describe('CoverageWarningBanner Component', () => {
  it('renders REPOSITORY_TOO_LARGE explanation dynamically from limitHitValue and totalBytes without hardcoded 1MB', () => {
    const html = renderToStaticMarkup(
      <CoverageWarningBanner
        reasonCode="REPOSITORY_TOO_LARGE"
        limitHitValue={157286400}
        totalBytes={180000000}
      />
    );

    expect(html).toContain('Repository Exceeded Size Safety Guardrail');
    expect(html).toContain('150 MiB uncompressed limit');
    expect(html).toContain('observed: 171.7 MiB');
    expect(html).toContain('INCOMPLETE COVERAGE');
    expect(html).toContain('Verified audit reflects analyzed portions only');
    expect(html).not.toContain('1MB');
    expect(html).not.toContain('Score');
    expect(html).not.toContain('Grade');
  });

  it('renders TOO_MANY_FILES explanation dynamically from limitHitValue and totalFiles', () => {
    const html = renderToStaticMarkup(
      <CoverageWarningBanner
        reasonCode="TOO_MANY_FILES"
        limitHitValue={10000}
        totalFiles={12500}
      />
    );

    expect(html).toContain('File Entry Ceiling Reached (Zip-Bomb Protection)');
    expect(html).toContain('10,000 files limit');
    expect(html).toContain('observed: 12,500 entries');
    expect(html).not.toContain('1MB');
  });

  it('renders SCAN_TIMEOUT explanation dynamically from limitHitValue', () => {
    const html = renderToStaticMarkup(
      <CoverageWarningBanner
        reasonCode="SCAN_TIMEOUT"
        limitHitValue={60}
      />
    );

    expect(html).toContain('Scan Execution Watchdog Timed Out');
    expect(html).toContain('60s safety threshold');
  });
});