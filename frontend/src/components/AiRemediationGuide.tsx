import { useState } from 'react';
import { 
  Terminal, 
  Sparkles, 
  CheckCircle2, 
  Copy, 
  Check, 
  AlertTriangle, 
  ShieldAlert, 
  FileCode, 
  RefreshCw,
  Info
} from 'lucide-react';
import { AiExplanation } from '../types/api';
import { CardSkeleton } from './LoadingSkeleton';
import { ErrorBanner } from './ErrorBanner';

interface AiRemediationGuideProps {
  explanation: AiExplanation | null;
  isLoading: boolean;
  error: string | null;
  onRetry: () => void;
}

export function AiRemediationGuide({
  explanation,
  isLoading,
  error,
  onRetry,
}: AiRemediationGuideProps) {
  const [completedSteps, setCompletedSteps] = useState<Record<number, boolean>>({});
  const [copiedCommand, setCopiedCommand] = useState(false);

  const toggleStep = (index: number) => {
    setCompletedSteps((prev) => ({ ...prev, [index]: !prev[index] }));
  };

  const copyToClipboard = (text: string) => {
    if (!text) return;
    navigator.clipboard.writeText(text);
    setCopiedCommand(true);
    setTimeout(() => setCopiedCommand(false), 2000);
  };

  if (isLoading) {
    return (
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 space-y-4">
        <div className="flex items-center gap-2 text-blue-400 text-xs font-semibold uppercase tracking-wider">
          <RefreshCw className="w-4 h-4 animate-spin" />
          <span>Generating Gemini Security Explanation...</span>
        </div>
        <CardSkeleton />
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-3">
        <ErrorBanner
          message={`AI Analysis Error: ${error}`}
          onRetry={onRetry}
          retryText="Regenerate Explanation"
        />
      </div>
    );
  }

  if (!explanation) {
    return null;
  }

  // Parse before/after code diff if provided
  const diffLines = explanation.remediationDiff ? explanation.remediationDiff.split('\n') : [];

  return (
    <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-lg space-y-0 animate-in fade-in duration-300">
      {/* Header with Source Attribution */}
      <div className="p-4 bg-slate-900/90 border-b border-slate-800 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2.5">
          <div className="p-1.5 rounded-lg bg-blue-600/20 border border-blue-500/30 text-blue-400">
            <Sparkles className="w-4 h-4" />
          </div>
          <div>
            <h4 className="text-sm font-semibold text-white tracking-tight">
              Gemini Security Analysis & Remediation
            </h4>
            <p className="text-[11px] text-slate-400">
              Deterministic reasoning & bounded risk containment guidance (FR-005).
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <span className="text-[11px] font-medium bg-slate-800 text-slate-300 px-2.5 py-1 rounded-md border border-slate-700">
            {explanation.sourceAttribution || 'Google Gemini 1.5 Flash'}
          </span>
        </div>
      </div>

      <div className="p-5 space-y-6 text-xs text-slate-300">
        {/* 1. Plain-Language Summary */}
        <div className="space-y-1.5">
          <h5 className="font-semibold text-slate-200 uppercase tracking-wider flex items-center gap-2 text-[11px]">
            <Info className="w-3.5 h-3.5 text-blue-400" />
            Plain-Language Summary
          </h5>
          <p className="text-slate-300 leading-relaxed pl-3 border-l-2 border-blue-500/60 bg-blue-950/20 py-2 pr-3 rounded-r-lg">
            {explanation.summary}
          </p>
        </div>

        {/* 2. Risk Impact */}
        <div className="space-y-1.5">
          <h5 className="font-semibold text-rose-300 uppercase tracking-wider flex items-center gap-2 text-[11px]">
            <ShieldAlert className="w-3.5 h-3.5 text-rose-400" />
            Risk Impact
          </h5>
          <p className="text-slate-300 leading-relaxed pl-3 border-l-2 border-rose-500/60 bg-rose-950/20 py-2 pr-3 rounded-r-lg">
            {explanation.riskImpact}
          </p>
        </div>

        {/* 3. Evidence Limits */}
        {explanation.evidenceLimits && (
          <div className="space-y-1.5">
            <h5 className="font-semibold text-amber-300 uppercase tracking-wider flex items-center gap-2 text-[11px]">
              <AlertTriangle className="w-3.5 h-3.5 text-amber-400" />
              Evidence Limits (What scan proves vs cannot prove)
            </h5>
            <p className="text-slate-400 leading-relaxed pl-3 border-l-2 border-amber-500/60 bg-amber-950/20 py-2 pr-3 rounded-r-lg">
              {explanation.evidenceLimits}
            </p>
          </div>
        )}

        {/* 4. Actionable Step-by-Step Remediation Checklist */}
        {explanation.remediationSteps && explanation.remediationSteps.length > 0 && (
          <div className="space-y-2.5">
            <div className="flex items-center justify-between">
              <h5 className="font-semibold text-slate-200 uppercase tracking-wider flex items-center gap-2 text-[11px]">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />
                Actionable Remediation Checklist
              </h5>
              <span className="text-[11px] text-slate-500 tabular-nums">
                {Object.values(completedSteps).filter(Boolean).length} / {explanation.remediationSteps.length} done
              </span>
            </div>

            <div className="space-y-2 bg-slate-950/60 border border-slate-800/80 rounded-xl p-3">
              {explanation.remediationSteps.map((step, idx) => {
                const isChecked = !!completedSteps[idx];
                return (
                  <button
                    key={idx}
                    onClick={() => toggleStep(idx)}
                    type="button"
                    className="w-full text-left flex items-start gap-3 p-2 rounded-lg hover:bg-slate-900/80 transition-colors group cursor-pointer"
                  >
                    <div className={`mt-0.5 w-4 h-4 rounded border flex items-center justify-center shrink-0 transition-colors ${
                      isChecked
                        ? 'bg-emerald-600 border-emerald-500 text-white'
                        : 'border-slate-600 bg-slate-900 group-hover:border-slate-400'
                    }`}>
                      {isChecked && <Check className="w-3 h-3" />}
                    </div>
                    <span className={`text-xs leading-relaxed ${
                      isChecked ? 'text-slate-500 line-through' : 'text-slate-300'
                    }`}>
                      {step}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        )}

        {/* 5. Before / After Syntax Diff */}
        {explanation.remediationDiff && (
          <div className="space-y-2">
            <h5 className="font-semibold text-slate-200 uppercase tracking-wider flex items-center gap-2 text-[11px]">
              <FileCode className="w-3.5 h-3.5 text-blue-400" />
              Suggested Remediation Diff
            </h5>
            <div className="bg-[#0b0f19] border border-slate-800 rounded-xl p-3 font-mono text-xs overflow-x-auto leading-relaxed">
              {diffLines.map((line, i) => {
                const isAddition = line.startsWith('+');
                const isDeletion = line.startsWith('-');
                return (
                  <div
                    key={i}
                    className={`px-2 py-0.5 rounded-sm flex items-start gap-2 ${
                      isAddition
                        ? 'bg-emerald-950/40 text-emerald-300 border-l-2 border-emerald-500'
                        : isDeletion
                        ? 'bg-rose-950/40 text-rose-300 border-l-2 border-rose-500'
                        : 'text-slate-400'
                    }`}
                  >
                    <span className="select-none text-slate-600 w-4 text-right shrink-0">
                      {isAddition ? '+' : isDeletion ? '-' : ' '}
                    </span>
                    <span className="break-all">{line.replace(/^[+-]/, '')}</span>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* 6. Key Revocation Command */}
        {explanation.revocationCommandHint && (
          <div className="space-y-2">
            <h5 className="font-semibold text-slate-200 uppercase tracking-wider flex items-center gap-2 text-[11px]">
              <Terminal className="w-3.5 h-3.5 text-amber-400" />
              Key Revocation Command Hint
            </h5>
            <div className="bg-slate-950 border border-slate-800 rounded-xl p-3 flex items-center justify-between gap-3 group">
              <code className="text-xs text-amber-300 font-mono break-all flex-1">
                {explanation.revocationCommandHint}
              </code>
              <button
                onClick={() => copyToClipboard(explanation.revocationCommandHint)}
                className="p-2 rounded-lg bg-slate-900 hover:bg-slate-800 border border-slate-700 text-slate-300 hover:text-white transition-colors shrink-0 flex items-center gap-1.5 text-[11px]"
                title="Copy revocation command"
              >
                {copiedCommand ? (
                  <>
                    <Check className="w-3.5 h-3.5 text-emerald-400" />
                    <span className="text-emerald-400">Copied</span>
                  </>
                ) : (
                  <>
                    <Copy className="w-3.5 h-3.5" />
                    <span>Copy</span>
                  </>
                )}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
