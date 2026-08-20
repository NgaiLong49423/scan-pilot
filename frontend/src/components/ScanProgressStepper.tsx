import React from 'react';
import { CheckCircle2, Clock, ShieldCheck, RefreshCw } from 'lucide-react';

interface ScanProgressStepperProps {
  isScanning: boolean;
  branchName: string;
}

export const ScanProgressStepper: React.FC<ScanProgressStepperProps> = ({ isScanning, branchName }) => {
  return (
    <div className="w-full bg-[#161b22] border border-[#30363d] rounded-2xl p-4 sm:p-5 shadow-sm space-y-3.5">
      {/* Top status info */}
      <div className="flex flex-wrap items-center justify-between gap-3 text-xs">
        <div className="flex items-center gap-2.5">
          <div className="p-1.5 rounded-lg bg-[#238636]/15 border border-[#238636]/30 text-[#3fb950]">
            {isScanning ? (
              <RefreshCw className="w-4 h-4 animate-spin text-[#58a6ff]" />
            ) : (
              <CheckCircle2 className="w-4 h-4 text-[#3fb950]" />
            )}
          </div>
          <div>
            <div className="font-semibold text-[#f0f6fc] flex items-center gap-2">
              <span>{isScanning ? 'Dual-Stage Scan in Progress...' : `Scan Completed Successfully on ${branchName}`}</span>
              <span className="text-[10px] font-mono font-medium px-2 py-0.5 rounded bg-[#1f6feb]/15 text-[#58a6ff] border border-[#1f6feb]/30">
                DUAL-STAGE (SNAPSHOT & GIT HISTORY)
              </span>
            </div>
            <p className="text-[#8b949e] text-[11px] mt-0.5">
              Analysis finished in 4.31s. Both active working directory and full Git history tree verified.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 text-[#8b949e] text-xs">
          <Clock className="w-3.5 h-3.5 text-[#8b949e]" />
          <span className="font-mono text-[#c9d1d9]">4.31s</span>
          <span className="text-[#30363d]">•</span>
          <span className="text-[#3fb950] font-medium">Zero Leak Leaks Unaddressed</span>
        </div>
      </div>

      {/* 4-Stage Stepper Bar */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-2 pt-1 font-mono text-xs">
        {/* Stage 1 */}
        <div className="p-2.5 rounded-xl bg-[#0d1117] border border-[#30363d] flex items-center gap-2 text-[#c9d1d9]">
          <span className="w-2 h-2 rounded-full bg-[#3fb950] shrink-0" />
          <span className="truncate">1. Workspace & Prep</span>
        </div>

        {/* Stage 2: Snapshot Scan */}
        <div className="p-2.5 rounded-xl bg-[#1f6feb]/15 border border-[#1f6feb]/40 flex items-center justify-between text-[#58a6ff]">
          <div className="flex items-center gap-2 truncate">
            <span className="w-2 h-2 rounded-full bg-[#58a6ff] shrink-0 animate-pulse" />
            <span className="truncate font-semibold">2. Snapshot Scan (HEAD)</span>
          </div>
          <span className="text-[10px] text-[#58a6ff] bg-[#1f6feb]/30 px-1.5 py-0.5 rounded">
            Stage 1
          </span>
        </div>

        {/* Stage 3: Git History Scan */}
        <div className="p-2.5 rounded-xl bg-[#238636]/15 border border-[#238636]/40 flex items-center justify-between text-[#3fb950]">
          <div className="flex items-center gap-2 truncate">
            <span className="w-2 h-2 rounded-full bg-[#3fb950] shrink-0 animate-pulse" />
            <span className="truncate font-semibold">3. Git History Tree</span>
          </div>
          <span className="text-[10px] text-[#3fb950] bg-[#238636]/30 px-1.5 py-0.5 rounded">
            Stage 2
          </span>
        </div>

        {/* Stage 4: Purge */}
        <div className="p-2.5 rounded-xl bg-[#0d1117] border border-[#30363d] flex items-center gap-2 text-[#c9d1d9]">
          <span className="w-2 h-2 rounded-full bg-[#3fb950] shrink-0" />
          <span className="truncate">4. Complete & Verified</span>
        </div>
      </div>
    </div>
  );
};
