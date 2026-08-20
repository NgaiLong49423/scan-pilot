import React from 'react';
import { CheckCircle2, Clock, RefreshCw, AlertCircle, Check, Terminal } from 'lucide-react';

interface ScanProgressStepperProps {
  isScanning: boolean;
  branchName: string;
  isScanned?: boolean;
  findingCount?: number;
  scanDuration?: string | null;
  currentStage?: number; // 1: Workspace Setup, 2: Working Tree Scan, 3: Finding & Evidence Sync, 4: Checkpoint Verified
  onToggleTerminal?: () => void;
  isTerminalOpen?: boolean;
}

export const ScanProgressStepper: React.FC<ScanProgressStepperProps> = ({ 
  isScanning, 
  branchName,
  isScanned = false,
  findingCount = 0,
  scanDuration = null,
  currentStage = 1,
  onToggleTerminal,
  isTerminalOpen = false,
}) => {
  const displayDuration = scanDuration || (isScanned ? 'Verified' : 'Pending Run');

  return (
    <div className="w-full bg-[#161b22] border border-[#30363d] rounded-2xl p-4 sm:p-5 shadow-sm space-y-3.5 animate-in fade-in duration-150">
      {/* Top status info */}
      <div className="flex flex-wrap items-center justify-between gap-3 text-xs">
        <div className="flex items-center gap-2.5">
          <div className={`p-1.5 rounded-lg border ${
            isScanning 
              ? 'bg-[#1f6feb]/15 border-[#1f6feb]/30 text-[#58a6ff]' 
              : isScanned 
              ? 'bg-[#238636]/15 border-[#238636]/30 text-[#3fb950]' 
              : 'bg-[#21262d] border-[#30363d] text-[#8b949e]'
          }`}>
            {isScanning ? (
              <RefreshCw className="w-4 h-4 animate-spin text-[#58a6ff]" />
            ) : isScanned ? (
              <CheckCircle2 className="w-4 h-4 text-[#3fb950]" />
            ) : (
              <AlertCircle className="w-4 h-4 text-[#8b949e]" />
            )}
          </div>
          <div>
            <div className="font-semibold text-[#f0f6fc] flex items-center gap-2">
              <span>
                {isScanning 
                  ? `Security Scan in Progress on ${branchName}...` 
                  : isScanned 
                  ? `Scan Completed Successfully on ${branchName}` 
                  : `Repository Awaiting Initial Scan on ${branchName}`}
              </span>
              <span className={`text-[10px] font-mono font-medium px-2 py-0.5 rounded border ${
                isScanning 
                  ? 'bg-[#1f6feb]/15 text-[#58a6ff] border-[#1f6feb]/30' 
                  : isScanned 
                  ? 'bg-[#238636]/15 text-[#3fb950] border-[#238636]/30' 
                  : 'bg-[#21262d] text-[#8b949e] border-[#30363d]'
              }`}>
                {isScanning 
                  ? 'ANALYZING PIPELINE' 
                  : isScanned 
                  ? 'WORKING TREE SNAPSHOT (VERIFIED)' 
                  : 'ENGINE STANDBY'}
              </span>
            </div>
            <p className="text-[#8b949e] text-[11px] mt-0.5">
              {isScanning 
                ? (
                  currentStage === 1
                    ? 'Stage 1/4: Preparing isolated workspace & downloading snapshot archive from GitHub...'
                    : currentStage === 2
                    ? 'Stage 2/4: Executing Gitleaks pattern matching & AST analysis across all HEAD text files...'
                    : currentStage === 3
                    ? 'Stage 3/4: Applying SP_SECRET_FP_V1 redaction and persisting findings to PostgreSQL...'
                    : 'Stage 4/4: Validating coverage record and advancing verified checkpoint...'
                )
                : isScanned
                ? `Analysis finished in ${displayDuration}. All active working tree files audited against SP-CONFIG-001 rules.`
                : `Click 'Trigger Rescan' to download the latest ${branchName} snapshot and execute deep secret analysis.`}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {/* Toggle Live Terminal Button */}
          {onToggleTerminal && (
            <button
              type="button"
              onClick={onToggleTerminal}
              className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg border text-xs font-medium transition-all ${
                isTerminalOpen
                  ? 'bg-[#1f6feb]/15 border-[#1f6feb]/40 text-[#58a6ff]'
                  : 'bg-[#0d1117] border-[#30363d] text-[#8b949e] hover:text-[#f0f6fc] hover:bg-[#21262d]'
              }`}
            >
              <Terminal className="w-3.5 h-3.5" />
              <span>{isTerminalOpen ? 'Hide Terminal' : 'Live Terminal'}</span>
            </button>
          )}

          <div className="flex items-center gap-2 text-[#8b949e] text-xs">
            <Clock className="w-3.5 h-3.5 text-[#8b949e]" />
            <span className="font-mono text-[#c9d1d9]">{displayDuration}</span>
            <span className="text-[#30363d]">•</span>
            <span className={isScanned ? (findingCount === 0 ? 'text-[#3fb950] font-medium' : 'text-[#f85149] font-medium') : 'text-[#8b949e]'}>
              {isScanned ? `${findingCount} Leaks Detected` : 'Zero Scan Data'}
            </span>
          </div>
        </div>
      </div>

      {/* 4-Stage Stepper Bar (Accurately mapping to real backend pipeline) */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-2 pt-1 font-mono text-xs">
        {/* Stage 1: Workspace Setup */}
        <div className={`p-2.5 rounded-xl border flex items-center justify-between transition-all duration-150 ${
          isScanning && currentStage === 1
            ? 'bg-[#1f6feb]/15 border-[#1f6feb]/50 text-[#58a6ff]'
            : (isScanning && currentStage > 1) || isScanned
            ? 'bg-[#238636]/15 border-[#238636]/30 text-[#3fb950]'
            : 'bg-[#0d1117]/60 border-[#30363d]/60 text-[#8b949e]'
        }`}>
          <div className="flex items-center gap-2 truncate">
            {isScanning && currentStage === 1 ? (
              <RefreshCw className="w-3 h-3 animate-spin text-[#58a6ff] shrink-0" />
            ) : (isScanning && currentStage > 1) || isScanned ? (
              <Check className="w-3 h-3 text-[#3fb950] shrink-0" />
            ) : (
              <span className="w-2 h-2 rounded-full bg-[#8b949e] shrink-0" />
            )}
            <span className="truncate font-semibold">1. Workspace Setup</span>
          </div>
          <span className="text-[10px] text-[#8b949e] bg-[#21262d] px-1.5 py-0.5 rounded shrink-0">
            Init
          </span>
        </div>

        {/* Stage 2: Working Tree Scan */}
        <div className={`p-2.5 rounded-xl border flex items-center justify-between transition-all duration-150 ${
          isScanning && currentStage === 2
            ? 'bg-[#1f6feb]/15 border-[#1f6feb]/50 text-[#58a6ff]'
            : (isScanning && currentStage > 2) || isScanned
            ? 'bg-[#238636]/15 border-[#238636]/30 text-[#3fb950]'
            : 'bg-[#0d1117]/60 border-[#30363d]/60 text-[#8b949e]'
        }`}>
          <div className="flex items-center gap-2 truncate">
            {isScanning && currentStage === 2 ? (
              <RefreshCw className="w-3 h-3 animate-spin text-[#58a6ff] shrink-0" />
            ) : (isScanning && currentStage > 2) || isScanned ? (
              <Check className="w-3 h-3 text-[#3fb950] shrink-0" />
            ) : (
              <span className="w-2 h-2 rounded-full bg-[#8b949e] shrink-0" />
            )}
            <span className="truncate font-semibold">2. Working Tree Scan</span>
          </div>
          <span className="text-[10px] text-[#8b949e] bg-[#21262d] px-1.5 py-0.5 rounded shrink-0">
            Stage 1
          </span>
        </div>

        {/* Stage 3: Evidence & Sync */}
        <div className={`p-2.5 rounded-xl border flex items-center justify-between transition-all duration-150 ${
          isScanning && currentStage === 3
            ? 'bg-[#1f6feb]/15 border-[#1f6feb]/50 text-[#58a6ff]'
            : (isScanning && currentStage > 3) || isScanned
            ? 'bg-[#238636]/15 border-[#238636]/30 text-[#3fb950]'
            : 'bg-[#0d1117]/60 border-[#30363d]/60 text-[#8b949e]'
        }`}>
          <div className="flex items-center gap-2 truncate">
            {isScanning && currentStage === 3 ? (
              <RefreshCw className="w-3 h-3 animate-spin text-[#58a6ff] shrink-0" />
            ) : (isScanning && currentStage > 3) || isScanned ? (
              <Check className="w-3 h-3 text-[#3fb950] shrink-0" />
            ) : (
              <span className="w-2 h-2 rounded-full bg-[#8b949e] shrink-0" />
            )}
            <span className="truncate font-semibold">3. Evidence & Sync</span>
          </div>
          <span className="text-[10px] text-[#8b949e] bg-[#21262d] px-1.5 py-0.5 rounded shrink-0">
            Stage 2
          </span>
        </div>

        {/* Stage 4: Checkpoint Verified */}
        <div className={`p-2.5 rounded-xl border flex items-center justify-between transition-all duration-150 ${
          isScanning && currentStage === 4
            ? 'bg-[#1f6feb]/15 border-[#1f6feb]/50 text-[#58a6ff]'
            : isScanned
            ? 'bg-[#238636]/15 border-[#238636]/30 text-[#3fb950]'
            : 'bg-[#0d1117]/60 border-[#30363d]/60 text-[#8b949e]'
        }`}>
          <div className="flex items-center gap-2 truncate">
            {isScanned ? (
              <Check className="w-3 h-3 text-[#3fb950] shrink-0" />
            ) : (
              <span className="w-2 h-2 rounded-full bg-[#8b949e] shrink-0" />
            )}
            <span className="truncate font-semibold">4. Checkpoint Verified</span>
          </div>
          <span className="text-[10px] text-[#8b949e] bg-[#21262d] px-1.5 py-0.5 rounded shrink-0">
            Audit
          </span>
        </div>
      </div>
    </div>
  );
};
