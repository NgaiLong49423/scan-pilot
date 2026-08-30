import { describe, it, expect } from 'vitest';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { RemediationPrModal } from './RemediationPrModal';
import { Finding } from '../types';

describe('RemediationPrModal Component', () => {
  const dummyFinding: Finding = {
    id: '3fa85f64-5717-4562-b3fc-2c963f66afa6',
    ruleId: 'SP-CONFIG-001',
    ruleName: 'Exposed Secret',
    severity: 'CRITICAL',
    status: 'OPEN',
    remediationQuality: 'ACTION_REQUIRED',
    filePath: 'src/main/resources/application.properties',
    lineNumber: 3,
    rawSecretMasked: 'superSecret123',
    detectedCommit: '4d4cadf',
    detectedAt: '12:00 PM',
    remediationDiff: {
      filePath: 'src/main/resources/application.properties',
      startLine: 3,
      originalSnippet: 'spring.datasource.password=***',
      suggestedFixSnippet: 'spring.datasource.password=${SPRING_DATASOURCE_PASSWORD}',
      explanation: 'Externalize secret',
    },
  };

  it('renders nothing when isOpen is false', () => {
    const html = renderToStaticMarkup(
      <RemediationPrModal
        isOpen={false}
        finding={dummyFinding}
        onClose={() => {}}
        onPrCreated={() => {}}
      />
    );
    expect(html).toBe('');
  });

  it('renders modal structure, mandatory revocation warning, header, and buttons when isOpen is true', () => {
    const html = renderToStaticMarkup(
      <RemediationPrModal
        isOpen={true}
        finding={dummyFinding}
        onClose={() => {}}
        onPrCreated={() => {}}
      />
    );

    expect(html).toContain('Spring Boot Safe Remediation PR');
    expect(html).toContain('MANDATORY REVOCATION &amp; ROTATION NOTICE');
    expect(html).toContain('DOES NOT revoke or invalidate the exposed credential');
    expect(html).toContain('Cancel');
    expect(html).toContain('Confirm &amp; Open Remediation PR');
    expect(html).toContain('focus-visible:ring-2');
  });
});