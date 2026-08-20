import React, { useState } from 'react';
import { Sparkles, Check, Copy } from 'lucide-react';
import { CodeDiffSnippet } from '../types';

interface RemediationDiffProps {
  diff: CodeDiffSnippet;
  findingId?: string;
  isResolved?: boolean;
  onApplyFix?: (findingId: string) => void;
}

export const RemediationDiff: React.FC<RemediationDiffProps> = ({ 
  diff, 
}) => {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(diff.suggestedFixSnippet);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
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
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md bg-[#1f6feb] hover:bg-[#388bfd] text-white text-xs font-medium transition-all duration-150 active:scale-95 shadow-sm"
          >
            {copied ? <Check className="w-3.5 h-3.5" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{copied ? 'Code Copied to Clipboard!' : 'Copy Remediation Code'}</span>
          </button>
          <span className="text-[10px] text-[#8b949e] bg-[#21262d] px-2 py-1 rounded border border-[#30363d] font-sans font-medium">
            Guidance-only (Manual Apply)
          </span>
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
            <span>Suggested Safe Remediation (Guidance Only)</span>
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
