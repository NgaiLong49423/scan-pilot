import React from 'react';
import { FileCode, AlertOctagon, Sparkles, CheckCircle2, FileQuestion, ArrowRight } from 'lucide-react';
import { HealthMetrics } from '../types';

interface MetricsGridProps {
  metrics: HealthMetrics;
  isScanned?: boolean;
  onViewCoverage?: () => void;
}

export const MetricsGrid: React.FC<MetricsGridProps> = ({ 
  metrics, 
  isScanned = false,
  onViewCoverage
}) => {
  const totalFiles = metrics.totalFilesCount || metrics.scannedFilesCount || 0;
  const scannedFiles = metrics.scannedFilesCount || 0;
  const skippedFiles = metrics.skippedFilesCount !== undefined 
    ? metrics.skippedFilesCount 
    : (totalFiles > scannedFiles ? totalFiles - scannedFiles : 0);

  // Exact real mathematical coverage percentage
  const coveragePercent = totalFiles > 0 
    ? ((scannedFiles / totalFiles) * 100).toFixed(1)
    : '0';

  const skippedPercent = totalFiles > 0
    ? ((skippedFiles / totalFiles) * 100).toFixed(1)
    : '0';

  return (
    <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 h-full">
      {/* Metric 1: Real Scanned Files & Exact Coverage % */}
      <div className="p-3.5 bg-[#161b22] border border-[#30363d] rounded-2xl hover:border-[#8b949e]/50 transition-all duration-150 flex flex-col justify-between shadow-sm">
        <div className="flex items-center justify-between text-[#8b949e]">
          <span className="text-[11px] font-semibold uppercase tracking-wider">Eligible Files</span>
          <FileCode className="w-4 h-4 text-[#58a6ff]" />
        </div>
        <div className="mt-1">
          <div className="text-xl font-bold text-[#f0f6fc] tabular-nums">
            {isScanned ? scannedFiles : 'Not available'}
          </div>
          <span className="text-[10px] text-[#58a6ff] font-medium">
            {isScanned ? (totalFiles > 0 ? `${coveragePercent}% Code Coverage` : 'Not available') : 'Awaiting scan'}
          </span>
        </div>
      </div>

      {/* Metric 2: Skipped Files & Direct Audit Link */}
      <div 
        onClick={isScanned && onViewCoverage ? onViewCoverage : undefined}
        className={`p-3.5 bg-[#161b22] border border-[#30363d] rounded-2xl transition-all duration-150 flex flex-col justify-between shadow-sm ${
          isScanned && onViewCoverage ? 'cursor-pointer hover:border-[#d29922]/70 hover:bg-[#161b22]/90 group' : ''
        }`}
      >
        <div className="flex items-center justify-between text-[#8b949e]">
          <span className="text-[11px] font-semibold uppercase tracking-wider">Skipped Files</span>
          <FileQuestion className="w-4 h-4 text-[#d29922]" />
        </div>
        <div className="mt-1">
          <div className="text-xl font-bold text-[#d29922] tabular-nums">
            {isScanned ? skippedFiles : 'Not available'}
          </div>
          <div className="flex items-center justify-between text-[10px] text-[#8b949e] mt-0.5">
            <span>{isScanned ? (totalFiles > 0 ? `${skippedPercent}% Excluded` : 'Not available') : 'Awaiting scan'}</span>
            {isScanned && onViewCoverage && (
              <span className="text-[#58a6ff] group-hover:underline flex items-center gap-0.5 font-sans">
                <span>Audit</span>
                <ArrowRight className="w-2.5 h-2.5" />
              </span>
            )}
          </div>
        </div>
      </div>

      {/* Metric 3: Open Actionable Leaks */}
      <div className="p-3.5 bg-[#161b22] border border-[#30363d] rounded-2xl hover:border-[#8b949e]/50 transition-all duration-150 flex flex-col justify-between shadow-sm">
        <div className="flex items-center justify-between text-[#8b949e]">
          <span className="text-[11px] font-semibold uppercase tracking-wider">Open Leaks</span>
          <AlertOctagon className="w-4 h-4 text-[#f85149]" />
        </div>
        <div className="mt-1">
          <div className={`text-xl font-bold tabular-nums ${isScanned && metrics.openLeaksCount > 0 ? 'text-[#f85149]' : 'text-[#f0f6fc]'}`}>
            {isScanned ? metrics.openLeaksCount : 'Not available'}
          </div>
          <span className={`text-[10px] ${isScanned && metrics.openLeaksCount > 0 ? 'text-[#f85149]/80 font-medium' : 'text-[#8b949e]'}`}>
            {isScanned ? (metrics.openLeaksCount > 0 ? 'Action Required' : '0 Detected') : 'Awaiting scan'}
          </span>
        </div>
      </div>

      {/* Metric 4: AI Fixes Ready */}
      <div className="p-3.5 bg-[#161b22] border border-[#30363d] rounded-2xl hover:border-[#8b949e]/50 transition-all duration-150 flex flex-col justify-between shadow-sm">
        <div className="flex items-center justify-between text-[#8b949e]">
          <span className="text-[11px] font-semibold uppercase tracking-wider">AI Fix Ready</span>
          <Sparkles className="w-4 h-4 text-[#58a6ff]" />
        </div>
        <div className="mt-1">
          <div className="text-xl font-bold text-[#58a6ff] tabular-nums">
            {isScanned ? metrics.aiFixReadyCount : 'Not available'}
          </div>
          <span className="text-[10px] text-[#8b949e]">
            {isScanned ? 'Guidance-Only Fix Diffs' : 'Awaiting scan'}
          </span>
        </div>
      </div>

      {/* Metric 5: Resolved Findings */}
      <div className="p-3.5 bg-[#161b22] border border-[#30363d] rounded-2xl hover:border-[#8b949e]/50 transition-all duration-150 flex flex-col justify-between shadow-sm col-span-2 sm:col-span-2">
        <div className="flex items-center justify-between text-[#8b949e]">
          <span className="text-[11px] font-semibold uppercase tracking-wider">Resolved Leaks</span>
          <CheckCircle2 className="w-4 h-4 text-[#3fb950]" />
        </div>
        <div className="mt-1">
          <div className="text-xl font-bold text-[#3fb950] tabular-nums">
            {isScanned ? metrics.resolvedLeaksCount : 'Not available'}
          </div>
          <span className="text-[10px] text-[#8b949e]">
            {isScanned ? 'Verified Closed Findings' : 'Awaiting scan'}
          </span>
        </div>
      </div>
    </div>
  );
};
