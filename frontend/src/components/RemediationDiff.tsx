import React, { useState } from 'react';
import { Sparkles, Check, Copy, Wand2 } from 'lucide-react';
import { CodeDiffSnippet } from '../types';

interface RemediationDiffProps {
  diff: CodeDiffSnippet;
  onApplyFix?: () => void;
}

export const RemediationDiff: React.FC<RemediationDiffProps> = ({ diff, onApplyFix }) => {
  const [copied, setCopied] = useState(false);
  const [applied, setApplied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(diff.suggestedFixSnippet);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleApply = () => {
    setApplied(true);
    if (onApplyFix) {
      onApplyFix();
    }
  };

  return (
    <div className="mt-4 rounded-xl border border-slate-800 bg-slate-950/80 overflow-hidden shadow-inner font-mono text-xs">
      {/* Header bar */}
      <div className="flex flex-wrap items-center justify-between gap-2 px-4 py-2.5 bg-slate-900/90 border-b border-slate-800 text-slate-400 font-sans">
        <div className="flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-indigo-400" />
          <span className="font-semibold text-slate-200 text-xs">Gemini AI Remediation Diff</span>
          <span className="hidden sm:inline-block text-[11px] text-slate-500 font-mono">
            ({diff.filePath}:{diff.startLine})
          </span>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleCopy}
            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium transition-all duration-150 active:scale-95"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{copied ? 'Copied' : 'Copy Fix'}</span>
          </button>

          <button
            type="button"
            onClick={handleApply}
            disabled={applied}
            className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-medium shadow-sm transition-all duration-150 active:scale-95 ${
              applied
                ? 'bg-emerald-600 text-white cursor-default'
                : 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-indigo-500/20'
            }`}
          >
            {applied ? (
              <>
                <Check className="w-3.5 h-3.5 text-white" />
                <span>Fix Applied</span>
              </>
            ) : (
              <>
                <Wand2 className="w-3.5 h-3.5" />
                <span>Apply AI Fix</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Side-by-side or stacked diff viewer */}
      <div className="grid grid-cols-1 lg:grid-cols-2 divide-y lg:divide-y-0 lg:divide-x divide-slate-800">
        {/* Left: Original / Exposed Snippet */}
        <div className="p-4 bg-rose-950/10">
          <div className="flex items-center gap-2 mb-2 font-sans font-medium text-[11px] text-rose-400">
            <span className="w-2 h-2 rounded-full bg-rose-500" />
            <span>DETECTED LEAK (RAW CREDENTIAL)</span>
          </div>
          <pre className="text-slate-300 overflow-x-auto leading-relaxed whitespace-pre font-mono">
            {diff.originalSnippet}
          </pre>
        </div>

        {/* Right: Suggested Safe Fix */}
        <div className="p-4 bg-emerald-950/10">
          <div className="flex items-center gap-2 mb-2 font-sans font-medium text-[11px] text-emerald-400">
            <span className="w-2 h-2 rounded-full bg-emerald-500" />
            <span>GEMINI REMEDIATION (SAFE ENV VARIABLE)</span>
          </div>
          <pre className="text-emerald-300 overflow-x-auto leading-relaxed whitespace-pre font-mono">
            {diff.suggestedFixSnippet}
          </pre>
        </div>
      </div>

      {/* Explanation Footer */}
      <div className="px-4 py-2 bg-slate-900/40 border-t border-slate-800/80 font-sans text-xs text-slate-400 flex items-start gap-2">
        <span className="font-semibold text-slate-300">Rationale:</span>
        <span>{diff.explanation}</span>
      </div>
    </div>
  );
};
