import React from 'react';
import { FileCode, AlertOctagon, Sparkles, CheckCircle2 } from 'lucide-react';
import { HealthMetrics } from '../types';

interface MetricsGridProps {
  metrics: HealthMetrics;
}

export const MetricsGrid: React.FC<MetricsGridProps> = ({ metrics }) => {
  return (
    <div className="grid grid-cols-2 gap-3 h-full">
      {/* Metric 1: Scanned Files */}
      <div className="p-3.5 bg-[#161b22] border border-[#30363d] rounded-2xl hover:border-[#8b949e]/50 transition-all duration-150 flex flex-col justify-between shadow-sm">
        <div className="flex items-center justify-between text-[#8b949e]">
          <span className="text-[11px] font-semibold uppercase tracking-wider">Files Scanned</span>
          <FileCode className="w-4 h-4 text-[#58a6ff]" />
        </div>
        <div className="mt-1">
          <div className="text-xl font-bold text-[#f0f6fc] tabular-nums">
            {metrics.scannedFilesCount}
          </div>
          <span className="text-[10px] text-[#8b949e]">100% Tree Coverage</span>
        </div>
      </div>

      {/* Metric 2: Open Actionable Leaks */}
      <div className="p-3.5 bg-[#161b22] border border-[#30363d] rounded-2xl hover:border-[#8b949e]/50 transition-all duration-150 flex flex-col justify-between shadow-sm">
        <div className="flex items-center justify-between text-[#8b949e]">
          <span className="text-[11px] font-semibold uppercase tracking-wider">Open Leaks</span>
          <AlertOctagon className="w-4 h-4 text-[#f85149]" />
        </div>
        <div className="mt-1">
          <div className="text-xl font-bold text-[#f85149] tabular-nums">
            {metrics.openLeaksCount}
          </div>
          <span className="text-[10px] text-[#f85149]/80 font-medium">Action Required</span>
        </div>
      </div>

      {/* Metric 3: AI Fixes Ready */}
      <div className="p-3.5 bg-[#161b22] border border-[#30363d] rounded-2xl hover:border-[#8b949e]/50 transition-all duration-150 flex flex-col justify-between shadow-sm">
        <div className="flex items-center justify-between text-[#8b949e]">
          <span className="text-[11px] font-semibold uppercase tracking-wider">AI Fix Ready</span>
          <Sparkles className="w-4 h-4 text-[#58a6ff]" />
        </div>
        <div className="mt-1">
          <div className="text-xl font-bold text-[#58a6ff] tabular-nums">
            {metrics.aiFixReadyCount}
          </div>
          <span className="text-[10px] text-[#8b949e]">1-Click Gemini Diffs</span>
        </div>
      </div>

      {/* Metric 4: Resolved Findings */}
      <div className="p-3.5 bg-[#161b22] border border-[#30363d] rounded-2xl hover:border-[#8b949e]/50 transition-all duration-150 flex flex-col justify-between shadow-sm">
        <div className="flex items-center justify-between text-[#8b949e]">
          <span className="text-[11px] font-semibold uppercase tracking-wider">Resolved</span>
          <CheckCircle2 className="w-4 h-4 text-[#3fb950]" />
        </div>
        <div className="mt-1">
          <div className="text-xl font-bold text-[#3fb950] tabular-nums">
            {metrics.resolvedLeaksCount}
          </div>
          <span className="text-[10px] text-[#3fb950]/80 font-medium">Safe in Git History</span>
        </div>
      </div>
    </div>
  );
};
