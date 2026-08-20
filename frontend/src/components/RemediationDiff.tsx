import React, { useState } from 'react';
import { Sparkles, Check, Copy, Wand2 } from 'lucide-react';
import { CodeDiffSnippet } from '../types';

interface RemediationDiffProps {
  diff: CodeDiffSnippet;
  findingId?: string;
  isResolved?: boolean;
  onApplyFix?: (findingId: string) => void;
}

export const RemediationDiff: React.FC<RemediationDiffProps> = ({ 
  diff, 
  findingId, 
  isResolved, 
  onApplyFix 
}) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(diff.suggestedFixSnippet);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleApply = () => {
    if (onApplyFix && findingId) {
      onApplyFix(findingId);
    }
  };

  return (
    <div className="mt-4 rounded-xl border border-[#30363d] bg-[#0d1117] overflow-hidden shadow-inner font-mono text-xs">
      {/* Header bar */}
      <div className="flex flex-wrap items-center justify-between gap-2 px-4 py-2.5 bg-[#161b22] border-b border-[#30363d] text-[#8b949e] font-sans">
        <div className="flex items-center gap-2">
          <Sparkles className="w-4 h-4 text-[#58a6ff]" />
          <span className="font-semibold text-[#f0f6fc] text-xs">Gemini AI Remediation Diff</span>
          <span className="hidden sm:inline-block text-[11px] text-[#8b949e] font-mono">
            ({diff.filePath}:{diff.startLine})
          </span>
        </div>

        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={handleCopy}
            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md bg-[#21262d] hover:bg-[#30363d] text-[#c9d1d9] text-xs font-medium transition-all duration-150 active:scale-95 border border-[#30363d]"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-[#3fb950]" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{copied ? 'Copied' : 'Copy Fix'}</span>
          </button>

          <button
            type="button"
            onClick={handleApply}
            disabled={isResolved}
            className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-medium shadow-sm transition-all duration-150 active:scale-95 ${
              isResolved
                ? 'bg-[#238636]/20 text-[#3fb950] border border-[#238636]/40 cursor-default'
                : 'bg-[#238636] hover:bg-[#2ea043] text-white shadow-sm'
            }`}
          >
            {isResolved ? (
              <>
                <Check className="w-3.5 h-3.5" />
                <span>Fix Applied (Safe)</span>
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

      {/* Side-by-side or stacked diff panels */}
      <div className="grid grid-cols-1 md:grid-cols-2 divide-y md:divide-y-0 md:divide-x divide-[#30363d]">
        {/* Left: Original (Vulnerable) snippet with GitHub red highlight */}
        <div className="p-4 bg-[#da3633]/5 text-[#f85149] space-y-1">
          <div className="text-[10px] font-sans font-semibold uppercase tracking-wider text-[#f85149] mb-2 flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-[#f85149]" />
            <span>Raw Secret Detected</span>
          </div>
          <pre className="overflow-x-auto whitespace-pre leading-relaxed">
            {diff.originalSnippet}
          </pre>
        </div>

        {/* Right: Suggested (Secure) snippet with GitHub green highlight */}
        <div className="p-4 bg-[#238636]/5 text-[#3fb950] space-y-1">
          <div className="text-[10px] font-sans font-semibold uppercase tracking-wider text-[#3fb950] mb-2 flex items-center gap-1">
            <span className="w-2 h-2 rounded-full bg-[#3fb950]" />
            <span>1-Click Safe Remediation</span>
          </div>
          <pre className="overflow-x-auto whitespace-pre leading-relaxed">
            {diff.suggestedFixSnippet}
          </pre>
        </div>
      </div>

      {/* Explanation note at the bottom */}
      {diff.explanation && (
        <div className="px-4 py-2.5 bg-[#161b22]/70 border-t border-[#30363d] text-[11px] text-[#8b949e] font-sans">
          <span className="font-semibold text-[#c9d1d9]">Remediation Guidance: </span>
          <span>{diff.explanation}</span>
        </div>
      )}
    </div>
  );
};
