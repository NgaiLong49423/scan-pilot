import { describe, it, expect } from 'vitest';
import { shouldContinueTelemetryPolling } from './telemetryPolling';
import { formatScanEventLog } from '../components/LiveScanTerminal';

describe('telemetryPolling: shouldContinueTelemetryPolling', () => {
  it('returns false when status is null, undefined, or empty', () => {
    expect(shouldContinueTelemetryPolling(null, false, 0, 0)).toBe(false);
    expect(shouldContinueTelemetryPolling(undefined, false, 0, 0)).toBe(false);
    expect(shouldContinueTelemetryPolling('', false, 0, 0)).toBe(false);
  });

  it('returns true for non-terminal statuses regardless of hasMore or sequence numbers', () => {
    expect(shouldContinueTelemetryPolling('QUEUED', false, 0, 0)).toBe(true);
    expect(shouldContinueTelemetryPolling('RUNNING', false, 5, 5)).toBe(true);
    expect(shouldContinueTelemetryPolling('PENDING', true, 2, 10)).toBe(true);
    expect(shouldContinueTelemetryPolling('FETCHING_SNAPSHOT', false, 1, 1)).toBe(true);
  });

  it('returns true for terminal status when hasMore is true even if cursor matches lastSequence', () => {
    expect(shouldContinueTelemetryPolling('COMPLETED', true, 5, 5)).toBe(true);
    expect(shouldContinueTelemetryPolling('FAILED', true, 10, 10)).toBe(true);
  });

  it('returns true for terminal status when cursor is less than lastSequence (drain in progress)', () => {
    expect(shouldContinueTelemetryPolling('COMPLETED', false, 3, 5)).toBe(true);
    expect(shouldContinueTelemetryPolling('FAILED', false, 0, 1)).toBe(true);
    expect(shouldContinueTelemetryPolling('COMPLETED', false, 95, 100)).toBe(true);
  });

  it('returns false for terminal status when hasMore is false and cursor has caught up to lastSequence (drain complete)', () => {
    expect(shouldContinueTelemetryPolling('COMPLETED', false, 5, 5)).toBe(false);
    expect(shouldContinueTelemetryPolling('FAILED', false, 1, 1)).toBe(false);
    expect(shouldContinueTelemetryPolling('COMPLETED', false, 10, 5)).toBe(false);
    expect(shouldContinueTelemetryPolling('FAILED', false, 0, 0)).toBe(false);
  });
});

describe('LiveScanTerminal: formatScanEventLog safe formatting', () => {
  it('unknown message code never reveals raw payloadJson or secret content', () => {
    const rawSecretPayload = JSON.stringify({ secret: 'ghp_secret_token_12345', path: '/app/secrets.env' });
    const formatted = formatScanEventLog({
      sequenceNumber: 42,
      stage: 'CUSTOM_STAGE',
      eventType: 'CUSTOM_EVENT',
      messageCode: 'UNKNOWN_CODE',
      payloadJson: rawSecretPayload,
      createdAt: new Date().toISOString(),
    });

    expect(formatted.level).toBe('INFO');
    expect(formatted.message).toBe('Event recorded: [UNKNOWN_CODE]');
    expect(formatted.message).not.toContain('ghp_secret_token_12345');
    expect(formatted.message).not.toContain('/app/secrets.env');
  });

  it('renders FINDING_ALERT correctly with standardized fields', () => {
    const formatted = formatScanEventLog({
      sequenceNumber: 15,
      stage: 'SCANNING_SECRETS',
      eventType: 'FINDING_DISCOVERED',
      messageCode: 'FINDING_ALERT',
      payloadJson: JSON.stringify({ findingIndex: 3, ruleId: 'AWS-ACCESS-KEY', severity: 'CRITICAL' }),
      createdAt: new Date().toISOString(),
    });

    expect(formatted.level).toBe('ALERT');
    expect(formatted.message).toBe('Finding #3: AWS-ACCESS-KEY (CRITICAL)');
  });

  it('renders neutral message for FINDING_ALERT when required payload fields are missing or malformed', () => {
    const emptyPayload = formatScanEventLog({
      sequenceNumber: 16,
      stage: 'SCANNING_SECRETS',
      eventType: 'FINDING_DISCOVERED',
      messageCode: 'FINDING_ALERT',
      payloadJson: JSON.stringify({}),
      createdAt: new Date().toISOString(),
    });
    expect(emptyPayload.level).toBe('ALERT');
    expect(emptyPayload.message).toBe('Finding detected; event details unavailable.');

    const partialPayload = formatScanEventLog({
      sequenceNumber: 17,
      stage: 'SCANNING_SECRETS',
      eventType: 'FINDING_DISCOVERED',
      messageCode: 'FINDING_ALERT',
      payloadJson: JSON.stringify({ findingIndex: 2, ruleId: '' }),
      createdAt: new Date().toISOString(),
    });
    expect(partialPayload.level).toBe('ALERT');
    expect(partialPayload.message).toBe('Finding detected; event details unavailable.');

    const malformedPayload = formatScanEventLog({
      sequenceNumber: 18,
      stage: 'SCANNING_SECRETS',
      eventType: 'FINDING_DISCOVERED',
      messageCode: 'FINDING_ALERT',
      payloadJson: 'invalid-json',
      createdAt: new Date().toISOString(),
    });
    expect(malformedPayload.level).toBe('ALERT');
    expect(malformedPayload.message).toBe('Finding detected; event details unavailable.');
  });

  it('renders FINDINGS_TRUNCATED correctly calculating omitted count', () => {
    const formatted = formatScanEventLog({
      sequenceNumber: 51,
      stage: 'SCANNING_SECRETS',
      eventType: 'FINDING_DISCOVERED',
      messageCode: 'FINDINGS_TRUNCATED',
      payloadJson: JSON.stringify({ totalFindings: 120, reportedFindings: 50 }),
      createdAt: new Date().toISOString(),
    });

    expect(formatted.level).toBe('ALERT');
    expect(formatted.message).toBe('+70 additional finding alerts omitted from stream');
  });

  it('renders SNAPSHOT_FETCHED for GIT_CLONE correctly with no negative archive numbers and no claim of archive download', () => {
    const formatted = formatScanEventLog({
      sequenceNumber: 2,
      stage: 'FETCHING_SNAPSHOT',
      eventType: 'SNAPSHOT_ACQUIRED',
      messageCode: 'SNAPSHOT_FETCHED',
      payloadJson: JSON.stringify({ mode: 'GIT_CLONE', workspaceBytes: 5242880, entryCount: 42 }),
      createdAt: new Date().toISOString(),
    });

    expect(formatted.level).toBe('WORKSPACE');
    expect(formatted.message).toBe('Shallow Git clone completed: 5.00 MB workspace populated (42 entries).');
    expect(formatted.message).not.toContain('archive');
    expect(formatted.message).not.toContain('-');
  });

  it('renders SNAPSHOT_FETCHED for ZIP_DOWNLOAD correctly preserving download wording and metrics', () => {
    const formatted = formatScanEventLog({
      sequenceNumber: 2,
      stage: 'FETCHING_SNAPSHOT',
      eventType: 'SNAPSHOT_ACQUIRED',
      messageCode: 'SNAPSHOT_FETCHED',
      payloadJson: JSON.stringify({ mode: 'ZIP_DOWNLOAD', archiveBytes: 1048576, workspaceBytes: 3145728, entryCount: 20 }),
      createdAt: new Date().toISOString(),
    });

    expect(formatted.level).toBe('WORKSPACE');
    expect(formatted.message).toBe('Snapshot downloaded: 1.00 MB archive extracted to 3.00 MB workspace (20 entries).');
  });
});
