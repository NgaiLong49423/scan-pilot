import React from 'react';
import { 
  ShieldCheck, 
  AlertTriangle, 
  AlertCircle, 
  Clock, 
  RefreshCw, 
  FileCheck2, 
  FileX2, 
  Calendar,
  RotateCcw,
  Sparkles
} from 'lucide-react';
import { SecurityActionSummary, RepositoryPostureStatus } from '../types';

interface SecurityActionSummaryCardProps {
  summary: SecurityActionSummary;
  isLoading?: boolean;
  onRetry?: () => void;
  onViewCoverage?: () => void;
}

export const SecurityActionSummaryCard: React.FC<SecurityActionSummaryCardProps> = ({
  summary,
  isLoading = false,
  onRetry,
  onViewCoverage,
}) => {
  if (isLoading) {
    return (
      <div className="p-6 bg-[#161b22] border border-[#30363d] rounded-2xl animate-pulse space-y-4 shadow-sm">
        <div className="flex items-center justify-between">
          <div className="h-6 w-48 bg-[#21262d] rounded-md" />
          <div className="h-6 w-28 bg-[#21262d] rounded-full" />
        </div>
        <div className="h-4 w-3/4 bg-[#21262d] rounded-md" />
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-2">
          <div className="h-16 bg-[#21262d] rounded-xl" />
          <div className="h-16 bg-[#21262d] rounded-xl" />
          <div className="h-16 bg-[#21262d] rounded-xl" />
          <div className="h-16 bg-[#21262d] rounded-xl" />
        </div>
      </div>
    );
  }

  const getStatusBadgeConfig = (status: RepositoryPostureStatus) => {
    switch (status) {
      case 'ACTION_REQUIRED':
        return {
          icon: <AlertTriangle className="w-4 h-4 text-[#f85149]" />,
          bgClass: 'bg-[#da3633]/15 text-[#f85149] border-[#da3633]/30',
        };
      case 'NO_OPEN_FINDINGS':
        return {
          icon: <ShieldCheck className="w-4 h-4 text-[#3fb950]" />,
          bgClass: 'bg-[#238636]/15 text-[#3fb950] border-[#238636]/30',
        };
      case 'COVERAGE_INCOMPLETE':
        return {
          icon: <AlertCircle className="w-4 h-4 text-[#d29922]" />,
          bgClass: 'bg-[#d29922]/15 text-[#d29922] border-[#d29922]/30',
        };
      case 'SCAN_IN_PROGRESS':
        return {
          icon: <RefreshCw className="w-4 h-4 text-[#58a6ff] animate-spin motion-reduce:animate-none" />,
          bgClass: 'bg-[#1f6feb]/15 text-[#58a6ff] border-[#1f6feb]/30',
        };
      case 'AWAITING_INITIAL_SCAN':
        return {
          icon: <Clock className="w-4 h-4 text-[#8b949e]" />,
          bgClass: 'bg-[#21262d] text-[#8b949e] border-[#30363d]',
        };
      case 'SCAN_UNAVAILABLE':
      default:
        return {
          icon: <AlertTriangle className="w-4 h-4 text-[#f85149]" />,
          bgClass: 'bg-[#da3633]/15 text-[#f85149] border-[#da3633]/30',
        };
    }
  };

  const badgeConfig = getStatusBadgeConfig(summary.status);
  const isUnavailable = summary.status === 'SCAN_UNAVAILABLE';
  const isAwaiting = summary.status === 'AWAITING_INITIAL_SCAN';

  return (
    <div className="p-6 bg-[#161b22] border border-[#30363d] rounded-2xl shadow-sm space-y-6 text-[#f0f6fc]">
      {/* Header Row: Title & Posture Badge */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-[#30363d]/60 pb-4">
        <div>
          <span className="text-[11px] font-mono uppercase tracking-wider text-[#8b949e] block mb-0.5">
            Verified Security Posture Summary
          </span>
          <h2 className="text-xl font-bold tracking-tight text-[#f0f6fc]">
            {summary.statusLabel}
          </h2>
        </div>

        <div className="flex items-center gap-2">
          <span className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold border ${badgeConfig.bgClass}`}>
            {badgeConfig.icon}
            <span>{summary.statusLabel}</span>
          </span>

          {isUnavailable && onRetry && (
            <button
              type="button"
              onClick={onRetry}
              className="inline-flex items-center gap-1.5 px-3 py-1 rounded-lg bg-[#21262d] hover:bg-[#30363d] text-[#58a6ff] border border-[#30363d] hover:border-[#58a6ff]/40 text-xs font-medium transition-all active:scale-95 focus:outline-none focus:ring-2 focus:ring-[#58a6ff] focus:ring-offset-2 focus:ring-offset-[#161b22]"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Retry Evidence</span>
            </button>
          )}
        </div>
      </div>

      {/* Description & Action Prompt */}
      <div className="space-y-2">
        <p className="text-sm text-[#c9d1d9] leading-relaxed">
          {summary.statusDescription}
        </p>
        <div className="flex items-start gap-2 text-xs text-[#8b949e] bg-[#0d1117] border border-[#30363d] rounded-xl p-3">
          <Sparkles className="w-4 h-4 text-[#58a6ff] shrink-0 mt-0.5" />
          <div>
            <span className="font-semibold text-[#f0f6fc]">Recommended Action: </span>
            <span>{summary.actionPrompt}</span>
          </div>
        </div>
      </div>

      {/* Metrics Breakdown Grid */}
      {!isUnavailable && !isAwaiting && (
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 pt-1">
          {/* Critical Pills */}
          <div className="p-3 bg-[#0d1117] border border-[#30363d] rounded-xl">
            <span className="text-[11px] font-mono text-[#8b949e] block">CRITICAL</span>
            <span className={`text-2xl font-bold font-mono tabular-nums ${summary.severityCounts.critical > 0 ? 'text-[#f85149]' : 'text-[#f0f6fc]'}`}>
              {summary.severityCounts.critical}
            </span>
          </div>

          {/* High Pills */}
          <div className="p-3 bg-[#0d1117] border border-[#30363d] rounded-xl">
            <span className="text-[11px] font-mono text-[#8b949e] block">HIGH</span>
            <span className={`text-2xl font-bold font-mono tabular-nums ${summary.severityCounts.high > 0 ? 'text-[#d29922]' : 'text-[#f0f6fc]'}`}>
              {summary.severityCounts.high}
            </span>
          </div>

          {/* Medium Pills */}
          <div className="p-3 bg-[#0d1117] border border-[#30363d] rounded-xl">
            <span className="text-[11px] font-mono text-[#8b949e] block">MEDIUM</span>
            <span className={`text-2xl font-bold font-mono tabular-nums ${summary.severityCounts.medium > 0 ? 'text-[#58a6ff]' : 'text-[#f0f6fc]'}`}>
              {summary.severityCounts.medium}
            </span>
          </div>

          {/* Low Pills */}
          <div className="p-3 bg-[#0d1117] border border-[#30363d] rounded-xl">
            <span className="text-[11px] font-mono text-[#8b949e] block">LOW</span>
            <span className="text-2xl font-bold font-mono tabular-nums text-[#8b949e]">
              {summary.severityCounts.low}
            </span>
          </div>
        </div>
      )}

      {/* Audit Footprint & Timestamps Footer */}
      {!isUnavailable && !isAwaiting && (
        <div className="flex flex-wrap items-center justify-between gap-4 pt-3 border-t border-[#30363d]/60 text-xs font-mono text-[#8b949e]">
          <div className="flex items-center gap-4">
            <span className="flex items-center gap-1.5">
              <FileCheck2 className="w-3.5 h-3.5 text-[#3fb950]" />
              <span>Scanned: <strong className="text-[#f0f6fc] tabular-nums">{summary.totalFilesScanned ?? '—'}</strong> files</span>
            </span>

            {summary.totalFilesSkipped !== null && summary.totalFilesSkipped > 0 && (
              <span className="flex items-center gap-1.5 text-[#d29922]">
                <FileX2 className="w-3.5 h-3.5" />
                <span>Skipped: <strong className="tabular-nums">{summary.totalFilesSkipped}</strong> files</span>
              </span>
            )}
          </div>

          <div className="flex items-center gap-3">
            {summary.scanCompletedAt && (
              <span className="flex items-center gap-1.5">
                <Calendar className="w-3.5 h-3.5" />
                <span>Completed: {new Date(summary.scanCompletedAt).toLocaleTimeString()}</span>
              </span>
            )}

            {onViewCoverage && summary.coverageImpact && (
              <button
                type="button"
                onClick={onViewCoverage}
                className="text-[#58a6ff] hover:underline cursor-pointer"
              >
                Inspect Coverage Details &rarr;
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
