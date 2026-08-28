import { describe, it, expect } from 'vitest';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { CreateIssueModal } from './CreateIssueModal';
import { Finding } from '../types';

describe('CreateIssueModal Component', () => {
  const dummyFinding: Finding = {
    id: '3fa85f64-5717-4562-b3fc-2c963f66afa6',
    ruleId: 'SP-CONFIG-001',
    ruleName: 'Detected Secret Leak',
    severity: 'HIGH',
    status: 'OPEN',
    remediationQuality: 'ACTION_REQUIRED',
    filePath: 'src/main/resources/application.yml',
    lineNumber: 42,
    rawSecretMasked: 'AKIA************',
    detectedCommit: '4d4cadf',
    detectedAt: '12:00 PM',
    remediationDiff: {
      filePath: 'src/main/resources/application.yml',
      startLine: 42,
      originalSnippet: '42 secret = AKIA...',
      suggestedFixSnippet: '42 secret = process.env...',
      explanation: 'Externalize secret',
    },
  };

  it('renders nothing when isOpen is false', () => {
    const html = renderToStaticMarkup(
      <CreateIssueModal
        isOpen={false}
        finding={dummyFinding}
        onClose={() => {}}
      />
    );
    expect(html).toBe('');
  });

  it('renders nothing when finding is null', () => {
    const html = renderToStaticMarkup(
      <CreateIssueModal
        isOpen={true}
        finding={null}
        onClose={() => {}}
      />
    );
    expect(html).toBe('');
  });

  it('renders modal structure, header, security title, buttons, and focus styles when isOpen is true', () => {
    const html = renderToStaticMarkup(
      <CreateIssueModal
        isOpen={true}
        finding={dummyFinding}
        onClose={() => {}}
      />
    );

    expect(html).toContain('Create Secret-Safe GitHub Issue');
    expect(html).toContain('Preview canonical markdown before creating issue on repository');
    expect(html).toContain('Cancel');
    expect(html).toContain('Confirm &amp; Create GitHub Issue');
    expect(html).toContain('focus-visible:ring-2');
  });
});
