/**
 * Pure helper function to determine if telemetry polling should continue.
 * Follows strict deterministic draining rules:
 * - Returns false if status is null or undefined.
 * - For active/non-terminal scan jobs (e.g. QUEUED, RUNNING), continues polling.
 * - For terminal scan jobs (COMPLETED, FAILED), continues polling if there are more pages (hasMore)
 *   or if the client sequence cursor has not caught up with the backend's lastSequence.
 * - Terminates cleanly when terminal AND hasMore is false AND cursor >= lastSequence.
 */
export function shouldContinueTelemetryPolling(
  status: string | null | undefined,
  hasMore: boolean,
  cursor: number,
  lastSequence: number
): boolean {
  if (!status) return false;
  const isTerminal = status === 'COMPLETED' || status === 'FAILED';
  if (!isTerminal) {
    return true;
  }
  // Terminal reached: continue draining until all pages are fetched and cursor reached lastSequence
  return hasMore || cursor < lastSequence;
}
