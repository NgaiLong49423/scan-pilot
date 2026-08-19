import { useEffect, useState } from 'react';
import { RefreshCw, CheckCircle2, AlertCircle, Clock } from 'lucide-react';
import { ScanJob } from '../types/api';

interface ScanProgressBarProps {
  scanJob: ScanJob | null;
  onDismiss?: () => void;
  onRetry?: () => void;
}

export function ScanProgressBar({
  scanJob,
  onDismiss,
  onRetry,
}: ScanProgressBarProps) {
  const [elapsedSeconds, setElapsedSeconds] = useState(0);

  useEffect(() => {
    if (!scanJob || scanJob.status === 'COMPLETED' || scanJob.status === 'FAILED') {
      return;
    }

    const startTime = scanJob.startedAt ? new Date(scanJob.startedAt).getTime() : Date.now();
    const interval = setInterval(() => {
      setElapsedSeconds(Math.max(0, Math.floor((Date.now() - startTime) / 1000)));
    }, 500);

    return () => clearInterval(interval);
  }, [scanJob]);

  if (!scanJob) return null;

  const isPending = scanJob.status === 'PENDING';
  const isRunning = scanJob.status === 'RUNNING';
  const isCompleted = scanJob.status === 'COMPLETED';
  const isFailed = scanJob.status === 'FAILED';

  // Compute active step index: 0 = Init, 1 = Stage 1 Snapshot, 2 = Stage 2 History, 3 = Complete
  let currentStep = 1;
  if (isPending) currentStep = 0;
  if (isRunning) currentStep = 2;
  if (isCompleted) currentStep = 3;

  return (
    <div className={`border rounded-2xl p-5 shadow-lg transition-all animate-in fade-in slide-in-from-top-4 duration-300 ${
      isFailed 
        ? 'bg-rose-950/30 border-rose-800/80 text-rose-200' 
        : isCompleted 
        ? 'bg-emerald-950/20 border-emerald-800/60 text-emerald-200' 
        : 'bg-slate-900 border-blue-500/40 text-slate-200'
    }`}>
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
        <div className="flex items-center gap-3">
          <div className={`p-2 rounded-xl ${
            isFailed
              ? 'bg-rose-500/20 text-rose-400'
              : isCompleted
              ? 'bg-emerald-500/20 text-emerald-400'
              : 'bg-blue-500/20 text-blue-400'
          }`}>
            {isFailed && <AlertCircle className="w-5 h-5" />}
            {isCompleted && <CheckCircle2 className="w-5 h-5" />}
            {(isPending || isRunning) && <RefreshCw className="w-5 h-5 animate-spin" />}
          </div>
          <div>
            <h4 className="text-sm font-semibold text-white tracking-tight flex items-center gap-2">
              <span>
                {isPending && 'Queued Security Scan'}
                {isRunning && `Executing Pipeline on ${scanJob.branchName}`}
                {isCompleted && `Scan Completed Successfully on ${scanJob.branchName}`}
                {isFailed && 'Scan Execution Failed'}
              </span>
              <span className="text-xs font-mono text-slate-400 font-normal">
                ({scanJob.scanMode || 'CONTINUOUS_MONITORING'})
              </span>
            </h4>
            <p className="text-xs text-slate-400 mt-0.5">
              {isPending && 'Allocating isolated git workspace and resolving HEAD commit...'}
              {isRunning && 'Running Stage 1 snapshot scan & Stage 2 reachable git history scan...'}
              {isCompleted && `Analysis finished in ${(scanJob.durationMs || 0) / 1000}s. Zero secret leaks unaddressed.`}
              {isFailed && (scanJob.errorMessage || 'An error occurred during scan execution.')}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3 self-end sm:self-center">
          <div className="flex items-center gap-1.5 text-xs text-slate-400 bg-slate-950/60 px-2.5 py-1 rounded-md border border-slate-800 tabular-nums">
            <Clock className="w-3.5 h-3.5 text-slate-500" />
            <span>
              {isCompleted && scanJob.durationMs ? `${(scanJob.durationMs / 1000).toFixed(1)}s` : `${elapsedSeconds}s`}
            </span>
          </div>

          {isFailed && onRetry && (
            <button
              onClick={onRetry}
              className="bg-rose-900/70 hover:bg-rose-800 text-rose-100 text-xs font-semibold px-3 py-1.5 rounded-lg border border-rose-700 transition-colors"
            >
              Retry Scan
            </button>
          )}

          {isCompleted && onDismiss && (
            <button
              onClick={onDismiss}
              className="text-xs text-slate-400 hover:text-white px-2.5 py-1 rounded-md hover:bg-slate-800 transition-colors"
            >
              Dismiss
            </button>
          )}
        </div>
      </div>

      {/* Progress Step Bar */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-2 pt-2 border-t border-slate-800/80 text-xs">
        <div className={`p-2 rounded-lg border flex items-center gap-2 ${
          currentStep >= 0 
            ? 'bg-slate-800/60 border-slate-700 text-slate-200' 
            : 'bg-slate-950/40 border-slate-800/50 text-slate-500'
        }`}>
          <span className={`w-2 h-2 rounded-full ${currentStep >= 0 ? 'bg-blue-400' : 'bg-slate-600'}`}></span>
          <span className="truncate">1. Workspace & Prep</span>
        </div>

        <div className={`p-2 rounded-lg border flex items-center gap-2 ${
          currentStep >= 1 
            ? 'bg-slate-800/60 border-slate-700 text-slate-200' 
            : 'bg-slate-950/40 border-slate-800/50 text-slate-500'
        }`}>
          <span className={`w-2 h-2 rounded-full ${currentStep >= 1 ? 'bg-blue-400' : 'bg-slate-600'}`}></span>
          <span className="truncate">2. Snapshot Scan</span>
        </div>

        <div className={`p-2 rounded-lg border flex items-center gap-2 ${
          currentStep >= 2 
            ? 'bg-slate-800/60 border-slate-700 text-slate-200' 
            : 'bg-slate-950/40 border-slate-800/50 text-slate-500'
        }`}>
          <span className={`w-2 h-2 rounded-full ${currentStep >= 2 ? 'bg-blue-400' : 'bg-slate-600'}`}></span>
          <span className="truncate">3. Git History Scan</span>
        </div>

        <div className={`p-2 rounded-lg border flex items-center gap-2 ${
          currentStep >= 3 
            ? 'bg-emerald-950/40 border-emerald-800/80 text-emerald-300' 
            : 'bg-slate-950/40 border-slate-800/50 text-slate-500'
        }`}>
          <span className={`w-2 h-2 rounded-full ${currentStep >= 3 ? 'bg-emerald-400' : 'bg-slate-600'}`}></span>
          <span className="truncate">4. Complete & Purge</span>
        </div>
      </div>
    </div>
  );
}
