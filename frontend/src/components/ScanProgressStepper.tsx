import React from 'react';
import { CheckCircle2, Clock, RefreshCw, AlertCircle } from 'lucide-react';

interface ScanProgressStepperProps {
  isScanning: boolean;
  branchName: string;
  isScanned?: boolean;
  findingCount?: number;
  scanDuration?: string;
}

export const ScanProgressStepper: React.FC<ScanProgressStepperProps> = ({ 
  isScanning, 
  branchName,
  isScanned = false,
  findingCount = 0,
  scanDuration = '4.31s'
}) => {
  return (
    <div className="w-full bg-[#161b22] border border-[#30363d] rounded-2xl p-4 sm:p-5 shadow-sm space-y-3.5">
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
                  ? `Dual-Stage Scan in Progress on ${branchName}...` 
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
                  ? 'DUAL-STAGE (SNAPSHOT & GIT HISTORY)' 
                  : 'ENGINE STANDBY'}
              </span>
            </div>
            <p className="text-[#8b949e] text-[11px] mt-0.5">
              {isScanning 
                ? 'Downloading snapshot archive from GitHub and executing isolated worker scanner...'
                : isScanned
                ? `Analysis finished in ${scanDuration}. Both active working tree and reachable Git commit history verified.`
                : `Click 'Trigger Rescan' to download the latest ${branchName} snapshot and execute deep secret analysis.`}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 text-[#8b949e] text-xs">
          <Clock className="w-3.5 h-3.5 text-[#8b949e]" />
          <span className="font-mono text-[#c9d1d9]">{isScanned ? scanDuration : 'Pending Run'}</span>
          <span className="text-[#30363d]">•</span>
          <span className={isScanned ? (findingCount === 0 ? 'text-[#3fb950] font-medium' : 'text-[#f85149] font-medium') : 'text-[#8b949e]'}>
            {isScanned ? `${findingCount} Leaks Detected` : 'Zero Scan Data'}
          </span>
        </div>
      </div>

      {/* 4-Stage Stepper Bar */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-2 pt-1 font-mono text-xs">
        {/* Stage 1 */}
        <div className={`p-2.5 rounded-xl border flex items-center gap-2 ${
          isScanning || isScanned 
            ? 'bg-[#0d1117] border-[#30363d] text-[#c9d1d9]' 
            : 'bg-[#0d1117]/60 border-[#30363d]/60 text-[#8b949e]'
        }`}>
          <span className={`w-2 h-2 rounded-full shrink-0 ${isScanning || isScanned ? 'bg-[#3fb950]' : 'bg-[#8b949e]'}`} />
          <span className="truncate">1. Workspace & Prep</span>
        </div>

        {/* Stage 2: Snapshot Scan */}
        <div className={`p-2.5 rounded-xl border flex items-center justify-between ${
          isScanning 
            ? 'bg-[#1f6feb]/15 border-[#1f6feb]/40 text-[#58a6ff]' 
            : isScanned 
            ? 'bg-[#0d1117] border-[#30363d] text-[#c9d1d9]' 
            : 'bg-[#0d1117]/60 border-[#30363d]/60 text-[#8b949e]'
        }`}>
          <div className="flex items-center gap-2 truncate">
            <span className={`w-2 h-2 rounded-full shrink-0 ${isScanning ? 'bg-[#58a6ff] animate-pulse' : isScanned ? 'bg-[#3fb950]' : 'bg-[#8b949e]'}`} />
            <span className="truncate font-semibold">2. Snapshot Scan</span>
          </div>
          <span className="text-[10px] text-[#8b949e] bg-[#21262d] px-1.5 py-0.5 rounded">
            Stage 1
          </span>
        </div>

        {/* Stage 3: Git History Scan */}
        <div className={`p-2.5 rounded-xl border flex items-center justify-between ${
          isScanning 
            ? 'bg-[#238636]/15 border-[#238636]/40 text-[#3fb950]' 
            : isScanned 
            ? 'bg-[#0d1117] border-[#30363d] text-[#c9d1d9]' 
            : 'bg-[#0d1117]/60 border-[#30363d]/60 text-[#8b949e]'
        }`}>
          <div className="flex items-center gap-2 truncate">
            <span className={`w-2 h-2 rounded-full shrink-0 ${isScanning ? 'bg-[#3fb950] animate-pulse' : isScanned ? 'bg-[#3fb950]' : 'bg-[#8b949e]'}`} />
            <span className="truncate font-semibold">3. Git History Tree</span>
          </div>
          <span className="text-[10px] text-[#8b949e] bg-[#21262d] px-1.5 py-0.5 rounded">
            Stage 2
          </span>
        </div>

        {/* Stage 4: Purge / Complete */}
        <div className={`p-2.5 rounded-xl border flex items-center gap-2 ${
          isScanned 
            ? 'bg-[#0d1117] border-[#30363d] text-[#c9d1d9]' 
            : 'bg-[#0d1117]/60 border-[#30363d]/60 text-[#8b949e]'
        }`}>
          <span className={`w-2 h-2 rounded-full shrink-0 ${isScanned ? 'bg-[#3fb950]' : 'bg-[#8b949e]'}`} />
          <span className="truncate">4. Complete & Verified</span>
        </div>
      </div>
    </div>
  );
};
