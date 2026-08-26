import React from 'react';
import { AlertTriangle, ShieldAlert, ArrowRight } from 'lucide-react';

interface CoverageWarningBannerProps {
  reasonCode?: string;
  limitHitValue?: number;
  totalBytes?: number;
  totalFiles?: number;
  onViewCoverage?: () => void;
}

export const CoverageWarningBanner: React.FC<CoverageWarningBannerProps> = ({
  reasonCode,
  limitHitValue,
  totalBytes,
  totalFiles,
  onViewCoverage,
}) => {
  const getExplanation = () => {
    switch (reasonCode) {
      case 'REPOSITORY_TOO_LARGE': {
        const limitMb = limitHitValue ? Math.round(limitHitValue / (1024 * 1024)) : 150;
        const observedMb = totalBytes ? (totalBytes / (1024 * 1024)).toFixed(1) : undefined;
        return {
          title: 'Repository Exceeded Size Safety Guardrail',
          description: `Repository workspace exceeded the ${limitMb} MiB uncompressed limit${
            observedMb ? ` (observed: ${observedMb} MiB)` : ''
          }. Scan scope was truncated for container memory stability.`,
        };
      }
      case 'TOO_MANY_FILES': {
        const limitCount = limitHitValue || 10000;
        return {
          title: 'File Entry Ceiling Reached (Zip-Bomb Protection)',
          description: `Archive exceeded ${limitCount.toLocaleString()} files limit${
            totalFiles ? ` (observed: ${totalFiles.toLocaleString()} entries)` : ''
          }. Partial scan completed under resource guardrails.`,
        };
      }
      case 'SCAN_TIMEOUT': {
        const timeoutSec = limitHitValue || 60;
        return {
          title: 'Scan Execution Watchdog Timed Out',
          description: `Detection process exceeded the ${timeoutSec}s safety threshold. The process tree was forcibly terminated to prevent stalled worker threads.`,
        };
      }
      default:
        return {
          title: 'Partial Repository Coverage (Guardrails Active)',
          description: 'Scan coverage is incomplete due to safety guardrails. Results reflect analyzed files only.',
        };
    }
  };

  const { title, description } = getExplanation();

  return (
    <div
      role="alert"
      aria-live="polite"
      className="p-4 sm:p-5 bg-[#1f1606] border border-[#d29922]/40 rounded-2xl text-[#f0f6fc] shadow-sm animate-in fade-in duration-200"
    >
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="flex items-start gap-3.5">
          <div className="p-2 bg-[#d29922]/20 border border-[#d29922]/40 rounded-xl text-[#d29922] shrink-0 mt-0.5 sm:mt-0">
            <ShieldAlert className="w-5 h-5" />
          </div>
          <div className="space-y-1">
            <div className="flex items-center gap-2 flex-wrap">
              <h3 className="text-sm font-bold text-[#f0f6fc] tracking-tight">{title}</h3>
              <span className="px-2 py-0.5 text-[10px] font-mono font-semibold bg-[#d29922]/20 text-[#d29922] border border-[#d29922]/40 rounded-md uppercase">
                INCOMPLETE COVERAGE
              </span>
            </div>
            <p className="text-xs text-[#c9d1d9] leading-relaxed max-w-2xl">{description}</p>
            <p className="text-[11px] text-[#8b949e]">
              <span className="font-semibold text-[#d29922]">Important:</span> Security score and grade reflect audited portions only. Scan checkpoint advancement is held.
            </p>
          </div>
        </div>

        {onViewCoverage && (
          <button
            type="button"
            onClick={onViewCoverage}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-[#21262d] hover:bg-[#30363d] border border-[#30363d] text-xs font-semibold text-[#f0f6fc] transition-all duration-150 shrink-0 self-start sm:self-center active:scale-95"
          >
            <span>View Coverage Audit</span>
            <ArrowRight className="w-3.5 h-3.5 text-[#8b949e]" />
          </button>
        )}
      </div>
    </div>
  );
};
