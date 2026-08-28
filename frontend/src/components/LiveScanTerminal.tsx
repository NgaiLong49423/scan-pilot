import React, { useState, useEffect, useRef } from 'react';
import { 
  Terminal as TerminalIcon, 
  X, 
  Maximize2, 
  Minimize2, 
  Copy, 
  Check, 
  ShieldAlert, 
  FileCode, 
  Activity,
  CheckCircle2
} from 'lucide-react';

export interface ScanLogEntry {
  id: string;
  timestamp: string;
  level: 'INIT' | 'WORKSPACE' | 'SCAN' | 'ALERT' | 'SUCCESS' | 'INFO';
  message: string;
  file?: string;
}

export function formatScanEventLog(event: {
  sequenceNumber: number;
  stage: string;
  eventType: string;
  messageCode: string;
  payloadJson: string | null;
  createdAt: string;
}): { level: 'INIT' | 'WORKSPACE' | 'SCAN' | 'ALERT' | 'SUCCESS' | 'INFO'; message: string } {
  let payload: any = {};
  if (event.payloadJson) {
    try {
      payload = JSON.parse(event.payloadJson);
    } catch {
      payload = {};
    }
  }

  switch (event.messageCode) {
    case 'STAGE_STARTED':
      if (event.stage === 'QUEUED') {
        return { level: 'INIT', message: `Scan job enqueued (seq: #${event.sequenceNumber}). Awaiting runner assignment...` };
      } else if (event.stage === 'FETCHING_SNAPSHOT') {
        return { level: 'INIT', message: `Stage: Fetching repository snapshot from GitHub archive...` };
      } else if (event.stage === 'CLASSIFYING_FILES') {
        return { level: 'WORKSPACE', message: `Stage: Classifying workspace files and evaluating eligibility...` };
      } else if (event.stage === 'SCANNING_SECRETS') {
        return { level: 'SCAN', message: `Stage: Executing Gitleaks secret detection across workspace...` };
      } else if (event.stage === 'RECORDING_EVIDENCE') {
        return { level: 'WORKSPACE', message: `Stage: Recording finding evidence and updating checkpoint...` };
      }
      return { level: 'INFO', message: `Stage transition: ${event.stage}` };

    case 'SNAPSHOT_FETCHED': {
      const mode = payload.mode;
      const wsMb = (Number(payload.workspaceBytes || 0) / (1024 * 1024)).toFixed(2);
      const entries = payload.entryCount || 0;
      if (mode === 'GIT_CLONE' || payload.archiveBytes === undefined || payload.archiveBytes === null) {
        return { level: 'WORKSPACE', message: `Shallow Git clone completed: ${wsMb} MB workspace populated (${entries} entries).` };
      }
      const archMb = (Number(payload.archiveBytes || 0) / (1024 * 1024)).toFixed(2);
      return { level: 'WORKSPACE', message: `Snapshot downloaded: ${archMb} MB archive extracted to ${wsMb} MB workspace (${entries} entries).` };
    }

    case 'FILES_CLASSIFIED': {
      const eligible = payload.eligibleFiles || 0;
      const skipped = payload.skippedFiles || 0;
      const total = payload.totalFiles || (eligible + skipped);
      return { level: 'INFO', message: `File eligibility: ${eligible}/${total} text files eligible for analysis (${skipped} non-text/binary files skipped).` };
    }

    case 'SCANNER_ACTIVE': {
      const engine = payload.engine || 'GITLEAKS_AST';
      const timeout = payload.timeoutSeconds ? ` (timeout: ${payload.timeoutSeconds}s)` : '';
      return { level: 'SCAN', message: `Scanner active: ${engine} detector running${timeout}...` };
    }

    case 'FINDING_ALERT': {
      const idx = payload.findingIndex;
      const ruleId = payload.ruleId;
      const severity = payload.severity;
      if (
        typeof idx === 'number' &&
        idx > 0 &&
        typeof ruleId === 'string' &&
        ruleId.trim().length > 0 &&
        typeof severity === 'string' &&
        severity.trim().length > 0
      ) {
        return { level: 'ALERT', message: `Finding #${idx}: ${ruleId} (${severity})` };
      }
      return { level: 'ALERT', message: 'Finding detected; event details unavailable.' };
    }

    case 'FINDINGS_TRUNCATED': {
      const total = payload.totalFindings || 0;
      const reported = payload.reportedFindings || 50;
      const omitted = Math.max(0, total - reported);
      return { level: 'ALERT', message: `+${omitted} additional finding alerts omitted from stream` };
    }

    case 'GUARDRAIL_LIMIT_HIT': {
      const reason = payload.reasonCode || 'GUARDRAIL_LIMIT';
      const limit = payload.limitHitValue || 0;
      return { level: 'ALERT', message: `Resource guardrail triggered: reason=${reason} limit=${limit}. Coverage marked INCOMPLETE.` };
    }

    case 'JOB_COMPLETED': {
      const dur = payload.durationMs ? `${(Number(payload.durationMs) / 1000).toFixed(1)}s` : 'N/A';
      const findings = payload.findingsCount !== undefined ? payload.findingsCount : 0;
      const coverage = payload.coverageImpact || 'COMPLETE';
      return { level: 'SUCCESS', message: `Scan completed in ${dur}: ${findings} findings recorded (Coverage: ${coverage}). Sandbox purged.` };
    }

    case 'JOB_FAILED': {
      const reason = payload.errorReason || 'Execution error';
      return { level: 'ALERT', message: `Scan job failed: ${reason}` };
    }

    default:
      return { level: 'INFO', message: `Event recorded: [${event.messageCode || event.eventType}]` };
  }
}

interface LiveScanTerminalProps {
  isOpen: boolean;
  isScanning: boolean;
  logs: ScanLogEntry[];
  currentFile?: string;
  scannedCount?: number;
  totalFiles?: number;
  leaksFoundCount?: number;
  activeStage?: string | null;
  durationStr?: string | null;
  onClose?: () => void;
}

export const LiveScanTerminal: React.FC<LiveScanTerminalProps> = ({
  isOpen,
  isScanning,
  logs,
  scannedCount = 0,
  totalFiles = 0,
  leaksFoundCount = 0,
  activeStage = null,
  durationStr = null,
  onClose,
}) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const [copied, setCopied] = useState(false);
  const [autoScroll, setAutoScroll] = useState(true);
  const logContainerRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom
  useEffect(() => {
    if (autoScroll && logContainerRef.current) {
      logContainerRef.current.scrollTop = logContainerRef.current.scrollHeight;
    }
  }, [logs, autoScroll]);

  if (!isOpen) return null;

  const handleCopyLogs = () => {
    const text = logs.map((l) => `[${l.timestamp}] [${l.level}] ${l.message}`).join('\n');
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className={`w-full bg-[#0d1117] border border-[#30363d] rounded-2xl shadow-xl overflow-hidden flex flex-col font-mono text-xs transition-all duration-200 ${
      isExpanded ? 'fixed inset-4 z-50 max-h-[calc(100vh-2rem)]' : 'h-80 my-4'
    }`}>
      {/* Terminal Top Bar */}
      <div className="bg-[#161b22] px-4 py-2.5 border-b border-[#30363d] flex flex-wrap items-center justify-between gap-3 select-none">
        {/* Left: Engine Name & Pulsing Indicator */}
        <div className="flex items-center gap-2.5">
          <div className="flex items-center gap-1.5">
            <span className="w-3 h-3 rounded-full bg-[#da3633] inline-block" />
            <span className="w-3 h-3 rounded-full bg-[#d29922] inline-block" />
            <span className="w-3 h-3 rounded-full bg-[#238636] inline-block" />
          </div>

          <div className="flex items-center gap-2 ml-2 text-[#f0f6fc] font-bold text-[11px] tracking-wide">
            <TerminalIcon className="w-3.5 h-3.5 text-[#58a6ff]" />
            <span>SCAN PILOT RUNNER CLI • SCAN EXECUTION LOGS</span>
          </div>

          {isScanning ? (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] bg-[#1f6feb]/15 text-[#58a6ff] border border-[#1f6feb]/30 animate-pulse">
              <span className="w-1.5 h-1.5 rounded-full bg-[#58a6ff]" />
              ENGINE ACTIVE
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] bg-[#238636]/15 text-[#3fb950] border border-[#238636]/30">
              <CheckCircle2 className="w-3 h-3" />
              IDLE / COMPLETE
            </span>
          )}
        </div>

        {/* Right: Telemetry Meters & Controls */}
        <div className="flex items-center gap-3 ml-auto text-[11px]">
          {/* Stage Pill */}
          <div className="hidden sm:flex items-center gap-2 px-2.5 py-1 rounded-lg bg-[#0d1117] border border-[#30363d] text-[#c9d1d9]">
            <Activity className="w-3 h-3 text-[#58a6ff]" />
            <span>Stage: <strong className="text-[#58a6ff]">{activeStage || (isScanning ? 'RUNNING' : 'IDLE')}</strong></span>
          </div>

          {/* Files Audited Pill */}
          <div className="hidden md:flex items-center gap-2 px-2.5 py-1 rounded-lg bg-[#0d1117] border border-[#30363d] text-[#c9d1d9]">
            <FileCode className="w-3 h-3 text-[#3fb950]" />
            <span>Files: <strong>{scannedCount > 0 || totalFiles > 0 ? `${scannedCount}/${totalFiles}` : 'Not available'}</strong></span>
          </div>

          {/* Leaks Counter Pill */}
          <div className={`flex items-center gap-1.5 px-2.5 py-1 rounded-lg border ${
            leaksFoundCount > 0 
              ? 'bg-[#da3633]/15 text-[#f85149] border-[#da3633]/30' 
              : 'bg-[#0d1117] text-[#8b949e] border-[#30363d]'
          }`}>
            <ShieldAlert className="w-3 h-3" />
            <span>{isScanning ? `${leaksFoundCount} Leaks detected` : `${leaksFoundCount} Leaks`}</span>
          </div>

          {/* Timer */}
          <span className="text-[#8b949e] font-mono">
            {durationStr || (isScanning ? 'Running...' : '0.0s')}
          </span>

          {/* Buttons: Copy, Expand, Close */}
          <div className="flex items-center gap-1 border-l border-[#30363d] pl-2">
            <button
              type="button"
              onClick={handleCopyLogs}
              title="Copy Output Logs"
              className="p-1 rounded text-[#8b949e] hover:text-[#f0f6fc] hover:bg-[#21262d] transition-colors cursor-pointer"
            >
              {copied ? <Check className="w-3.5 h-3.5 text-[#3fb950]" /> : <Copy className="w-3.5 h-3.5 text-[#8b949e]" />}
            </button>

            <button
              type="button"
              onClick={() => setIsExpanded(!isExpanded)}
              title={isExpanded ? 'Minimize' : 'Expand Fullscreen'}
              className="p-1 rounded text-[#8b949e] hover:text-[#f0f6fc] hover:bg-[#21262d] transition-colors cursor-pointer"
            >
              {isExpanded ? <Minimize2 className="w-3.5 h-3.5" /> : <Maximize2 className="w-3.5 h-3.5" />}
            </button>

            {onClose && (
              <button
                type="button"
                onClick={onClose}
                title="Close Terminal"
                className="p-1 rounded text-[#8b949e] hover:text-[#f85149] hover:bg-[#da3633]/15 transition-colors cursor-pointer"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>
        </div>
      </div>

      {/* Stage Pulse Line */}
      <div className="w-full bg-[#21262d] h-0.5 overflow-hidden">
        {isScanning && (
          <div className="h-full bg-gradient-to-r from-[#1f6feb] via-[#58a6ff] to-[#238636] animate-pulse w-full" />
        )}
      </div>

      {/* Terminal Log Stream Area */}
      <div 
        ref={logContainerRef}
        className="flex-1 p-4 overflow-y-auto space-y-1.5 bg-[#010409] select-text selection:bg-[#1f6feb]/30"
      >
        {logs.length > 0 ? (
          logs.map((log) => {
            let levelBadge = (
              <span className="text-[#8b949e] bg-[#21262d] px-1.5 py-0.5 rounded text-[10px]">
                [INFO]
              </span>
            );

            if (log.level === 'INIT') {
              levelBadge = (
                <span className="text-[#58a6ff] bg-[#1f6feb]/15 border border-[#1f6feb]/30 px-1.5 py-0.5 rounded text-[10px]">
                  [INIT]
                </span>
              );
            } else if (log.level === 'WORKSPACE') {
              levelBadge = (
                <span className="text-[#bc8cff] bg-[#8957e5]/15 border border-[#8957e5]/30 px-1.5 py-0.5 rounded text-[10px]">
                  [WORKSPACE]
                </span>
              );
            } else if (log.level === 'SCAN') {
              levelBadge = (
                <span className="text-[#58a6ff] bg-[#1f6feb]/10 px-1.5 py-0.5 rounded text-[10px]">
                  [SCAN]
                </span>
              );
            } else if (log.level === 'ALERT') {
              levelBadge = (
                <span className="text-[#f85149] bg-[#da3633]/20 border border-[#da3633]/40 px-1.5 py-0.5 rounded text-[10px] font-bold">
                  [ALERT]
                </span>
              );
            } else if (log.level === 'SUCCESS') {
              levelBadge = (
                <span className="text-[#3fb950] bg-[#238636]/20 border border-[#238636]/40 px-1.5 py-0.5 rounded text-[10px] font-bold">
                  [SUCCESS]
                </span>
              );
            }

            return (
              <div key={log.id} className="flex items-start gap-2.5 leading-relaxed hover:bg-[#161b22]/40 px-1 rounded transition-colors">
                <span className="text-[#8b949e] shrink-0">{log.timestamp}</span>
                <span className="shrink-0">{levelBadge}</span>
                <span className={`break-all ${
                  log.level === 'ALERT' 
                    ? 'text-[#f85149] font-medium' 
                    : log.level === 'SUCCESS' 
                    ? 'text-[#3fb950] font-medium' 
                    : log.level === 'INIT' || log.level === 'WORKSPACE'
                    ? 'text-[#f0f6fc]'
                    : 'text-[#c9d1d9]'
                }`}>
                  {log.message}
                </span>
              </div>
            );
          })
        ) : (
          <div className="py-12 text-center text-[#8b949e]">
            <p>Scan Pilot Runner initialized. Awaiting scan trigger...</p>
          </div>
        )}

        {isScanning && (
          <div className="flex items-center gap-2 text-[#58a6ff] pt-1">
            <span className="inline-block w-2 h-4 bg-[#58a6ff] animate-pulse" />
            <span className="text-[#8b949e] text-[11px]">Runner executing isolated scan job (Stage: {activeStage || 'ACTIVE'})...</span>
          </div>
        )}
      </div>

      {/* Terminal Footer Bar */}
      <div className="px-4 py-1.5 bg-[#161b22] border-t border-[#30363d] flex items-center justify-between text-[10px] text-[#8b949e]">
        <div className="flex items-center gap-3">
          <label className="flex items-center gap-1.5 cursor-pointer hover:text-[#f0f6fc]">
            <input
              type="checkbox"
              checked={autoScroll}
              onChange={(e) => setAutoScroll(e.target.checked)}
              className="rounded bg-[#0d1117] border-[#30363d] text-[#1f6feb] focus:ring-0 w-3 h-3"
            />
            <span>Auto-scroll</span>
          </label>
          <span>•</span>
          <span>Zero Raw Secret Policy Active (SP_SECRET_FP_V1)</span>
        </div>

        <div className="font-mono text-[#8b949e]">
          Spring Boot 3 • Gitleaks AST Engine
        </div>
      </div>
    </div>
  );
};
