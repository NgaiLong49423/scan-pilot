import { describe, it, expect } from 'vitest';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';
import { LiveScanTerminal, formatScanEventLog, ScanLogEntry } from './LiveScanTerminal';

describe('formatScanEventLog Function', () => {
  it('formats STAGE_STARTED events correctly for various stages', () => {
    expect(formatScanEventLog({
      sequenceNumber: 1,
      stage: 'QUEUED',
      eventType: 'SCAN_STAGE_CHANGED',
      messageCode: 'STAGE_STARTED',
      payloadJson: null,
      createdAt: '2026-08-30T07:00:00Z',
    })).toEqual({
      level: 'INIT',
      message: 'Scan job enqueued (seq: #1). Awaiting runner assignment...',
    });

    expect(formatScanEventLog({
      sequenceNumber: 2,
      stage: 'FETCHING_SNAPSHOT',
      eventType: 'SCAN_STAGE_CHANGED',
      messageCode: 'STAGE_STARTED',
      payloadJson: null,
      createdAt: '2026-08-30T07:00:01Z',
    })).toEqual({
      level: 'INIT',
      message: 'Stage: Fetching repository snapshot from GitHub archive...',
    });

    expect(formatScanEventLog({
      sequenceNumber: 3,
      stage: 'CLASSIFYING_FILES',
      eventType: 'SCAN_STAGE_CHANGED',
      messageCode: 'STAGE_STARTED',
      payloadJson: null,
      createdAt: '2026-08-30T07:00:02Z',
    })).toEqual({
      level: 'WORKSPACE',
      message: 'Stage: Classifying workspace files and evaluating eligibility...',
    });

    expect(formatScanEventLog({
      sequenceNumber: 4,
      stage: 'SCANNING_SECRETS',
      eventType: 'SCAN_STAGE_CHANGED',
      messageCode: 'STAGE_STARTED',
      payloadJson: null,
      createdAt: '2026-08-30T07:00:03Z',
    })).toEqual({
      level: 'SCAN',
      message: 'Stage: Executing Gitleaks secret detection across workspace...',
    });

    expect(formatScanEventLog({
      sequenceNumber: 5,
      stage: 'RECORDING_EVIDENCE',
      eventType: 'SCAN_STAGE_CHANGED',
      messageCode: 'STAGE_STARTED',
      payloadJson: null,
      createdAt: '2026-08-30T07:00:04Z',
    })).toEqual({
      level: 'WORKSPACE',
      message: 'Stage: Recording finding evidence and updating checkpoint...',
    });
  });

  it('formats SNAPSHOT_FETCHED events in git clone and archive modes', () => {
    const gitCloneLog = formatScanEventLog({
      sequenceNumber: 6,
      stage: 'FETCHING_SNAPSHOT',
      eventType: 'SNAPSHOT_FETCHED',
      messageCode: 'SNAPSHOT_FETCHED',
      payloadJson: JSON.stringify({ mode: 'GIT_CLONE', workspaceBytes: 5242880, entryCount: 120 }),
      createdAt: '2026-08-30T07:00:05Z',
    });
    expect(gitCloneLog.level).toBe('WORKSPACE');
    expect(gitCloneLog.message).toContain('Shallow Git clone completed: 5.00 MB workspace populated (120 entries).');

    const tarballLog = formatScanEventLog({
      sequenceNumber: 7,
      stage: 'FETCHING_SNAPSHOT',
      eventType: 'SNAPSHOT_FETCHED',
      messageCode: 'SNAPSHOT_FETCHED',
      payloadJson: JSON.stringify({ mode: 'TARBALL', archiveBytes: 2097152, workspaceBytes: 6291456, entryCount: 150 }),
      createdAt: '2026-08-30T07:00:06Z',
    });
    expect(tarballLog.level).toBe('WORKSPACE');
    expect(tarballLog.message).toContain('Snapshot downloaded: 2.00 MB archive extracted to 6.00 MB workspace (150 entries).');
  });

  it('formats FILES_CLASSIFIED event', () => {
    const log = formatScanEventLog({
      sequenceNumber: 8,
      stage: 'CLASSIFYING_FILES',
      eventType: 'FILES_CLASSIFIED',
      messageCode: 'FILES_CLASSIFIED',
      payloadJson: JSON.stringify({ eligibleFiles: 85, skippedFiles: 15, totalFiles: 100 }),
      createdAt: '2026-08-30T07:00:07Z',
    });
    expect(log.level).toBe('INFO');
    expect(log.message).toContain('File eligibility: 85/100 text files eligible for analysis (15 non-text/binary files skipped).');
  });

  it('formats SCANNER_ACTIVE, FINDING_ALERT, and FINDINGS_TRUNCATED', () => {
    const activeLog = formatScanEventLog({
      sequenceNumber: 9,
      stage: 'SCANNING_SECRETS',
      eventType: 'SCANNER_ACTIVE',
      messageCode: 'SCANNER_ACTIVE',
      payloadJson: JSON.stringify({ engine: 'GITLEAKS_AST', timeoutSeconds: 60 }),
      createdAt: '2026-08-30T07:00:08Z',
    });
    expect(activeLog.level).toBe('SCAN');
    expect(activeLog.message).toContain('Scanner active: GITLEAKS_AST detector running (timeout: 60s)...');

    const alertLog = formatScanEventLog({
      sequenceNumber: 10,
      stage: 'SCANNING_SECRETS',
      eventType: 'FINDING_ALERT',
      messageCode: 'FINDING_ALERT',
      payloadJson: JSON.stringify({ findingIndex: 1, ruleId: 'SP-CONFIG-001', severity: 'CRITICAL' }),
      createdAt: '2026-08-30T07:00:09Z',
    });
    expect(alertLog.level).toBe('ALERT');
    expect(alertLog.message).toBe('Finding #1: SP-CONFIG-001 (CRITICAL)');

    const truncatedLog = formatScanEventLog({
      sequenceNumber: 11,
      stage: 'SCANNING_SECRETS',
      eventType: 'FINDINGS_TRUNCATED',
      messageCode: 'FINDINGS_TRUNCATED',
      payloadJson: JSON.stringify({ totalFindings: 75, reportedFindings: 50 }),
      createdAt: '2026-08-30T07:00:10Z',
    });
    expect(truncatedLog.level).toBe('ALERT');
    expect(truncatedLog.message).toContain('+25 additional finding alerts omitted from stream');
  });

  it('formats GUARDRAIL_LIMIT_HIT, JOB_COMPLETED, and JOB_FAILED', () => {
    const guardrailLog = formatScanEventLog({
      sequenceNumber: 12,
      stage: 'SCANNING_SECRETS',
      eventType: 'GUARDRAIL_LIMIT_HIT',
      messageCode: 'GUARDRAIL_LIMIT_HIT',
      payloadJson: JSON.stringify({ reasonCode: 'REPOSITORY_TOO_LARGE', limitHitValue: 157286400 }),
      createdAt: '2026-08-30T07:00:11Z',
    });
    expect(guardrailLog.level).toBe('ALERT');
    expect(guardrailLog.message).toContain('Resource guardrail triggered: reason=REPOSITORY_TOO_LARGE limit=157286400. Coverage marked INCOMPLETE.');

    const completeLog = formatScanEventLog({
      sequenceNumber: 13,
      stage: 'COMPLETED',
      eventType: 'SCAN_COMPLETED',
      messageCode: 'JOB_COMPLETED',
      payloadJson: JSON.stringify({ durationMs: 2500, findingsCount: 2, coverageImpact: 'COMPLETE' }),
      createdAt: '2026-08-30T07:00:12Z',
    });
    expect(completeLog.level).toBe('SUCCESS');
    expect(completeLog.message).toContain('Scan completed in 2.5s: 2 findings recorded (Coverage: COMPLETE). Sandbox purged.');

    const failedLog = formatScanEventLog({
      sequenceNumber: 14,
      stage: 'FAILED',
      eventType: 'SCAN_FAILED',
      messageCode: 'JOB_FAILED',
      payloadJson: JSON.stringify({ errorReason: 'CONTAINER_OOM' }),
      createdAt: '2026-08-30T07:00:13Z',
    });
    expect(failedLog.level).toBe('ALERT');
    expect(failedLog.message).toBe('Scan job failed: CONTAINER_OOM');
  });
});

describe('LiveScanTerminal Component Rendering', () => {
  const sampleLogs: ScanLogEntry[] = [
    { id: '1', timestamp: '07:00:00', level: 'INIT', message: 'Scan enqueued' },
    { id: '2', timestamp: '07:00:01', level: 'WORKSPACE', message: 'Workspace ready' },
    { id: '3', timestamp: '07:00:02', level: 'SCAN', message: 'Scanning files' },
    { id: '4', timestamp: '07:00:03', level: 'ALERT', message: 'Finding #1 detected' },
    { id: '5', timestamp: '07:00:04', level: 'SUCCESS', message: 'Scan complete' },
  ];

  it('renders nothing when isOpen is false', () => {
    const html = renderToStaticMarkup(
      <LiveScanTerminal
        isOpen={false}
        isScanning={false}
        logs={sampleLogs}
      />
    );
    expect(html).toBe('');
  });

  it('renders terminal top bar, telemetry meters, level badges, and footer when isOpen is true', () => {
    const html = renderToStaticMarkup(
      <LiveScanTerminal
        isOpen={true}
        isScanning={true}
        logs={sampleLogs}
        scannedCount={42}
        totalFiles={50}
        leaksFoundCount={1}
        activeStage="SCANNING_SECRETS"
        durationStr="3.2s"
      />
    );

    expect(html).toContain('SCAN PILOT RUNNER CLI • SCAN EXECUTION LOGS');
    expect(html).toContain('ENGINE ACTIVE');
    expect(html).toContain('Stage:');
    expect(html).toContain('SCANNING_SECRETS');
    expect(html).toContain('42/50');
    expect(html).toContain('1 Leaks detected');
    expect(html).toContain('3.2s');
    expect(html).toContain('[INIT]');
    expect(html).toContain('[WORKSPACE]');
    expect(html).toContain('[SCAN]');
    expect(html).toContain('[ALERT]');
    expect(html).toContain('[SUCCESS]');
    expect(html).toContain('Auto-scroll');
    expect(html).toContain('Zero Raw Secret Policy Active');
  });
});