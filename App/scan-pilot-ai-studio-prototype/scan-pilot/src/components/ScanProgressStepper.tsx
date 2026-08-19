import React from 'react';
import { CheckCircle2, Clock, ShieldCheck, RefreshCw } from 'lucide-react';

interface ScanProgressStepperProps {
  isScanning: boolean;
  branchName: string;
}

export const ScanProgressStepper: React.FC<ScanProgressStepperProps> = ({ isScanning, branchName }) => {
  return (
    <div className="w-full bg-slate-900/80 border border-slate-800 rounded-2xl p-4 sm:p-5 shadow-sm space-y-3.5">
      {/* Top status info */}
      <div className="flex flex-wrap items-center justify-between gap-3 text-xs">
        <div className="flex items-center gap-2.5">
          <div className="p-1.5 rounded-lg bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
            {isScanning ? (
              <RefreshCw className="w-4 h-4 animate-spin text-indigo-400" />
            ) : (
              <CheckCircle2 className="w-4 h-4 text-emerald-400" />
            )}
          </div>
          <div>
            <div className="font-semibold text-white flex items-center gap-2">
              <span>{isScanning ? 'Dual-Stage Scan in Progress...' : `Scan Completed Successfully on ${branchName}`}</span>
              <span className="text-[10px] font-mono font-medium px-2 py-0.5 rounded bg-indigo-950/80 text-indigo-300 border border-indigo-500/30">
                DUAL-STAGE (SNAPSHOT & GIT HISTORY)
              </span>
            </div>
            <p className="text-slate-400 text-[11px] mt-0.5">
              Analysis finished in 4.31s. Both active working directory and full Git history tree verified.
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 text-slate-400 text-xs">
          <Clock className="w-3.5 h-3.5 text-slate-500" />
          <span className="font-mono text-slate-300">4.31s</span>
          <span className="text-slate-600">•</span>
          <span className="text-emerald-400 font-medium">Zero Leak Leaks Unaddressed</span>
        </div>
      </div>

      {/* 4-Stage Stepper Bar */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-2 pt-1 font-mono text-xs">
        {/* Stage 1 */}
        <div className="p-2.5 rounded-xl bg-slate-950/80 border border-slate-800/90 flex items-center gap-2 text-slate-300">
          <span className="w-2 h-2 rounded-full bg-emerald-400 shrink-0" />
          <span className="truncate">1. Workspace & Prep</span>
        </div>

        {/* Stage 2: Snapshot Scan */}
        <div className="p-2.5 rounded-xl bg-indigo-950/40 border border-indigo-500/40 flex items-center justify-between text-indigo-200">
          <div className="flex items-center gap-2 truncate">
            <span className="w-2 h-2 rounded-full bg-indigo-400 shrink-0 animate-pulse" />
            <span className="truncate font-semibold">2. Snapshot Scan (HEAD)</span>
          </div>
          <span className="text-[10px] text-indigo-400 bg-indigo-900/60 px-1.5 py-0.5 rounded">
            Stage 1
          </span>
        </div>

        {/* Stage 3: Git History Scan */}
        <div className="p-2.5 rounded-xl bg-cyan-950/40 border border-cyan-500/40 flex items-center justify-between text-cyan-200">
          <div className="flex items-center gap-2 truncate">
            <span className="w-2 h-2 rounded-full bg-cyan-400 shrink-0 animate-pulse" />
            <span className="truncate font-semibold">3. Git History Tree</span>
          </div>
          <span className="text-[10px] text-cyan-400 bg-cyan-900/60 px-1.5 py-0.5 rounded">
            Stage 2
          </span>
        </div>

        {/* Stage 4: Purge */}
        <div className="p-2.5 rounded-xl bg-slate-950/80 border border-slate-800/90 flex items-center gap-2 text-slate-300">
          <span className="w-2 h-2 rounded-full bg-emerald-400 shrink-0" />
          <span className="truncate">4. Complete & Verified</span>
        </div>
      </div>
    </div>
  );
};
