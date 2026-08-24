import React from 'react';
import { CheckCircle2, Clock, RefreshCw, AlertCircle, Terminal } from 'lucide-react';

interface ScanProgressStepperProps {
  isScanning: boolean;
  branchName: string;
  stage?: string | null;
  isScanned?: boolean;
  findingCount?: number;
  scanDuration?: string | null;
  onToggleTerminal?: () => void;
  isTerminalOpen?: boolean;
  scanError?: string | null;
}

export const ScanProgressStepper: React.FC<ScanProgressStepperProps> = ({ 
  isScanning, 
  branchName,
  stage = null,
  isScanned = false,
  findingCount = 0,
  scanDuration = null,
  onToggleTerminal,
  isTerminalOpen = false,
  scanError = null,
}) => {
  const displayDuration = scanDuration || 'Not available';

  const getStageInfo = () => {
    if (scanError) {
      return {
        title: `Scan Execution Failed on ${branchName}`,
        badge: 'SCAN FAILED',
        description: `Scan pipeline error: ${scanError}`,
      };
    }
    if (isScanning) {
      switch (stage) {
        case 'QUEUED':
          return {
            title: `Scan Queued on ${branchName}...`,
            badge: 'QUEUED',
            description: 'Scan job is queued in worker executor...',
          };
        case 'FETCHING_SNAPSHOT':
          return {
            title: `Fetching Repository Snapshot on ${branchName}...`,
            badge: 'FETCHING SNAPSHOT',
            description: 'Downloading and verifying remote repository snapshot...',
          };
        case 'CLASSIFYING_FILES':
          return {
            title: `Classifying Files & Coverage on ${branchName}...`,
            badge: 'CLASSIFYING FILES',
            description: 'Evaluating file eligibility and calculating baseline coverage...',
          };
        case 'SCANNING_SECRETS':
          return {
            title: `Auditing Secret Rules on ${branchName}...`,
            badge: 'SCANNING SECRETS',
            description: 'Auditing repository snapshot and git history against SP-CONFIG-001 rules...',
          };
        case 'RECORDING_EVIDENCE':
          return {
            title: `Recording Findings & Evidence on ${branchName}...`,
            badge: 'RECORDING EVIDENCE',
            description: 'Recording findings, locations, coverage records, and checkpoints...',
          };
        default:
          return {
            title: `Security Scan in Progress on ${branchName}...`,
            badge: stage || 'SCANNING',
            description: 'Executing security analysis pipeline...',
          };
      }
    }
    if (isScanned) {
      return {
        title: `Scan Completed Successfully on ${branchName}`,
        badge: 'WORKING TREE SNAPSHOT',
        description: scanDuration
          ? `Analysis finished in ${scanDuration}. All active working tree files audited against SP-CONFIG-001 rules.`
          : 'Analysis finished. All active working tree files audited against SP-CONFIG-001 rules.',
      };
    }
    return {
      title: `Repository Awaiting Initial Scan on ${branchName}`,
      badge: 'ENGINE STANDBY',
      description: `Click 'Trigger Rescan' to download the latest ${branchName} snapshot and execute deep secret analysis.`,
    };
  };

  const stageInfo = getStageInfo();

  return (
    <div className="w-full bg-[#161b22] border border-[#30363d] rounded-2xl p-4 sm:p-5 shadow-sm space-y-3.5 animate-in fade-in duration-150">
      {/* Top status info */}
      <div className="flex flex-wrap items-center justify-between gap-3 text-xs">
        <div className="flex items-center gap-2.5">
          <div className={`p-1.5 rounded-lg border ${
            isScanning 
              ? 'bg-[#1f6feb]/15 border-[#1f6feb]/30 text-[#58a6ff]' 
              : scanError
              ? 'bg-[#da3633]/15 border-[#da3633]/30 text-[#f85149]'
              : isScanned 
              ? 'bg-[#238636]/15 border-[#238636]/30 text-[#3fb950]' 
              : 'bg-[#21262d] border-[#30363d] text-[#8b949e]'
          }`}>
            {isScanning ? (
              <RefreshCw className="w-4 h-4 animate-spin text-[#58a6ff]" />
            ) : scanError ? (
              <AlertCircle className="w-4 h-4 text-[#f85149]" />
            ) : isScanned ? (
              <CheckCircle2 className="w-4 h-4 text-[#3fb950]" />
            ) : (
              <AlertCircle className="w-4 h-4 text-[#8b949e]" />
            )}
          </div>
          <div>
            <div className="font-semibold text-[#f0f6fc] flex items-center gap-2">
              <span>{stageInfo.title}</span>
              <span className={`text-[10px] font-mono font-medium px-2 py-0.5 rounded border ${
                isScanning 
                  ? 'bg-[#1f6feb]/15 text-[#58a6ff] border-[#1f6feb]/30' 
                  : scanError
                  ? 'bg-[#da3633]/15 text-[#f85149] border-[#da3633]/30'
                  : isScanned 
                  ? 'bg-[#238636]/15 text-[#3fb950] border-[#238636]/30' 
                  : 'bg-[#21262d] text-[#8b949e] border-[#30363d]'
              }`}>
                {stageInfo.badge}
              </span>
            </div>
            <p className="text-[#8b949e] text-[11px] mt-0.5">
              {stageInfo.description}
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
            <span className={scanError ? 'text-[#f85149] font-medium' : isScanned ? (findingCount === 0 ? 'text-[#3fb950] font-medium' : 'text-[#f85149] font-medium') : 'text-[#8b949e]'}>
              {scanError ? 'Scan Failed' : isScanned ? `${findingCount} Leaks Detected` : 'Awaiting scan'}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
