import React from 'react';
import { FileCode, AlertOctagon, Sparkles, CheckCircle2 } from 'lucide-react';
import { HealthMetrics } from '../types';

interface MetricsGridProps {
  metrics: HealthMetrics;
}

export const MetricsGrid: React.FC<MetricsGridProps> = ({ metrics }) => {
  return (
    <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
      {/* Metric 1: Scanned Files */}
      <div className="p-4 bg-slate-900/60 border border-slate-800/80 rounded-xl hover:border-slate-700/80 transition-all duration-150 flex flex-col justify-between">
        <div className="flex items-center justify-between text-slate-400">
          <span className="text-xs font-semibold uppercase tracking-wider">Files Scanned</span>
          <FileCode className="w-4 h-4 text-indigo-400" />
        </div>
        <div className="mt-2">
          <div className="text-xl sm:text-2xl font-bold text-white tabular-nums">
            {metrics.scannedFilesCount}
          </div>
          <span className="text-[11px] text-slate-500">100% Tree Coverage</span>
        </div>
      </div>

      {/* Metric 2: Open Actionable Leaks */}
      <div className="p-4 bg-slate-900/60 border border-slate-800/80 rounded-xl hover:border-slate-700/80 transition-all duration-150 flex flex-col justify-between">
        <div className="flex items-center justify-between text-slate-400">
          <span className="text-xs font-semibold uppercase tracking-wider">Open Leaks</span>
          <AlertOctagon className="w-4 h-4 text-rose-400" />
        </div>
        <div className="mt-2">
          <div className="text-xl sm:text-2xl font-bold text-rose-400 tabular-nums">
            {metrics.openLeaksCount}
          </div>
          <span className="text-[11px] text-rose-400/80 font-medium">Action Required</span>
        </div>
      </div>

      {/* Metric 3: AI Fixes Ready */}
      <div className="p-4 bg-slate-900/60 border border-slate-800/80 rounded-xl hover:border-slate-700/80 transition-all duration-150 flex flex-col justify-between">
        <div className="flex items-center justify-between text-slate-400">
          <span className="text-xs font-semibold uppercase tracking-wider">AI Fix Ready</span>
          <Sparkles className="w-4 h-4 text-cyan-400" />
        </div>
        <div className="mt-2">
          <div className="text-xl sm:text-2xl font-bold text-cyan-400 tabular-nums">
            {metrics.aiFixReadyCount}
          </div>
          <span className="text-[11px] text-slate-500">1-Click Gemini Diffs</span>
        </div>
      </div>

      {/* Metric 4: Resolved Findings */}
      <div className="p-4 bg-slate-900/60 border border-slate-800/80 rounded-xl hover:border-slate-700/80 transition-all duration-150 flex flex-col justify-between">
        <div className="flex items-center justify-between text-slate-400">
          <span className="text-xs font-semibold uppercase tracking-wider">Resolved</span>
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
        </div>
        <div className="mt-2">
          <div className="text-xl sm:text-2xl font-bold text-emerald-400 tabular-nums">
            {metrics.resolvedLeaksCount}
          </div>
          <span className="text-[11px] text-emerald-400/80 font-medium">Safe in Git History</span>
        </div>
      </div>
    </div>
  );
};
