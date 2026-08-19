import { useState } from 'react';
import type { MouseEvent } from 'react';
import { 
  ShieldCheck, 
  ChevronDown, 
  ChevronUp, 
  Code, 
  Copy, 
  Check, 
  GitCommit, 
  User, 
  Clock
} from 'lucide-react';
import { Finding, AiExplanation, FindingSeverity, FindingLifecycle, RemediationQuality } from '../types/api';
import { aiApi } from '../api/aiApi';
import { AiRemediationGuide } from './AiRemediationGuide';

interface FindingCardProps {
  finding: Finding;
}

export function FindingCard({ finding }: FindingCardProps) {
  const [isExpanded, setIsExpanded] = useState(false);
  const [aiExplanation, setAiExplanation] = useState<AiExplanation | null>(null);
  const [isAiLoading, setIsAiLoading] = useState(false);
  const [aiError, setAiError] = useState<string | null>(null);
  const [copiedSecret, setCopiedSecret] = useState(false);

  const primaryLocation = finding.locations && finding.locations.length > 0 ? finding.locations[0] : null;
  const primaryEvidence = finding.evidenceItems && finding.evidenceItems.length > 0 ? finding.evidenceItems[0] : null;

  const maskedSecretText = primaryEvidence?.maskedSecret || '●●●●●●●●●●●●[REDACTED_SECRET]';
  const redactedSnippet = primaryEvidence?.redactedSnippet || null;

  const handleToggleExpand = async () => {
    const nextState = !isExpanded;
    setIsExpanded(nextState);

    if (nextState && !aiExplanation && !isAiLoading) {
      loadAiExplanation();
    }
  };

  const loadAiExplanation = async () => {
    setIsAiLoading(true);
    setAiError(null);
    try {
      // 1. Try to fetch existing explanation
      let exp = await aiApi.getFindingExplanation(finding.id);
      if (!exp) {
        // 2. Otherwise trigger explanation generation
        exp = await aiApi.explainFinding(finding.id);
      }
      setAiExplanation(exp);
    } catch (err: any) {
      setAiError(err?.message || 'Failed to generate AI explanation.');
    } finally {
      setIsAiLoading(false);
    }
  };

  const copyMaskedSecret = (e: MouseEvent) => {
    e.stopPropagation();
    navigator.clipboard.writeText(maskedSecretText);
    setCopiedSecret(true);
    setTimeout(() => setCopiedSecret(false), 2000);
  };

  // Severity style helper
  const getSeverityBadge = (severity: FindingSeverity) => {
    switch (severity) {
      case 'CRITICAL':
        return 'bg-rose-500/10 text-rose-400 border-rose-500/30';
      case 'HIGH':
        return 'bg-orange-500/10 text-orange-400 border-orange-500/30';
      case 'MEDIUM':
        return 'bg-amber-500/10 text-amber-400 border-amber-500/30';
      case 'LOW':
        return 'bg-blue-500/10 text-blue-400 border-blue-500/30';
      default:
        return 'bg-slate-500/10 text-slate-400 border-slate-500/30';
    }
  };

  // Lifecycle style helper
  const getLifecycleBadge = (lifecycle: FindingLifecycle) => {
    switch (lifecycle) {
      case 'OPEN':
        return 'bg-amber-500/10 text-amber-400 border-amber-500/30';
      case 'RESOLVED':
        return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30';
      case 'REGRESSED':
        return 'bg-rose-500/10 text-rose-400 border-rose-500/30';
      default:
        return 'bg-slate-500/10 text-slate-400 border-slate-500/30';
    }
  };

  // Remediation Quality style helper
  const getRemediationBadge = (quality: RemediationQuality) => {
    switch (quality) {
      case 'ACTION_REQUIRED':
        return {
          badge: 'bg-rose-500/10 text-rose-400 border-rose-500/30',
          label: 'Action Required',
        };
      case 'RISK_CONTAINED':
        return {
          badge: 'bg-amber-500/10 text-amber-400 border-amber-500/30',
          label: 'Risk Contained (History Exposure)',
        };
      case 'VERIFIED_COMPLETE':
        return {
          badge: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30',
          label: 'Verified Complete',
        };
      default:
        return {
          badge: 'bg-slate-500/10 text-slate-400 border-slate-500/30',
          label: quality,
        };
    }
  };

  const remediationInfo = getRemediationBadge(finding.remediationQuality);

  return (
    <div className={`bg-slate-900 border rounded-2xl transition-all shadow-sm overflow-hidden ${
      finding.lifecycle === 'RESOLVED' 
        ? 'border-emerald-900/40 bg-slate-900/60' 
        : finding.lifecycle === 'REGRESSED'
        ? 'border-rose-900/50 bg-slate-900'
        : 'border-slate-800 hover:border-slate-700 bg-slate-900'
    }`}>
      {/* Top Card Summary */}
      <div 
        onClick={handleToggleExpand}
        className="p-5 cursor-pointer flex flex-col gap-4 select-none"
      >
        <div className="flex flex-wrap items-center justify-between gap-3">
          {/* Badges */}
          <div className="flex flex-wrap items-center gap-2">
            <span className={`text-[11px] font-bold px-2.5 py-0.5 rounded-md border uppercase tracking-wider ${getSeverityBadge(finding.severity)}`}>
              {finding.severity}
            </span>
            <span className={`text-[11px] font-semibold px-2.5 py-0.5 rounded-md border uppercase tracking-wider ${getLifecycleBadge(finding.lifecycle)}`}>
              {finding.lifecycle}
            </span>
            <span className={`text-[11px] font-medium px-2.5 py-0.5 rounded-md border ${remediationInfo.badge}`}>
              {remediationInfo.label}
            </span>
            <span className="text-xs font-mono text-slate-500">
              {finding.ruleId}
            </span>
          </div>

          {/* Timestamp & Expand Icon */}
          <div className="flex items-center gap-3 text-xs text-slate-400">
            <span className="flex items-center gap-1 tabular-nums">
              <Clock className="w-3.5 h-3.5 text-slate-500" />
              {new Date(finding.lastSeenAt || finding.firstSeenAt).toLocaleDateString()}
            </span>
            <div className="p-1 rounded-lg hover:bg-slate-800 text-slate-400 transition-colors">
              {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
            </div>
          </div>
        </div>

        {/* Title & Description */}
        <div>
          <h3 className="text-base font-semibold text-white tracking-tight flex items-center gap-2">
            {finding.title || finding.ruleId}
          </h3>
          <p className="text-xs text-slate-400 mt-1 leading-relaxed line-clamp-2">
            {finding.description}
          </p>
        </div>

        {/* Location & Masked Secret Bar */}
        <div className="flex flex-wrap items-center justify-between gap-3 pt-2 border-t border-slate-800/80 text-xs text-slate-300">
          <div className="flex items-center gap-3">
            {primaryLocation && (
              <div className="flex items-center gap-1.5 font-mono text-slate-300">
                <Code className="w-3.5 h-3.5 text-blue-400" />
                <span>{primaryLocation.filePath}</span>
                {primaryLocation.startLine && (
                  <span className="text-slate-400 tabular-nums">:L{primaryLocation.startLine}</span>
                )}
              </div>
            )}
            {primaryLocation?.commitSha && (
              <div className="hidden sm:flex items-center gap-1 text-[11px] text-slate-500 font-mono">
                <GitCommit className="w-3 h-3" />
                <span>{primaryLocation.commitSha.substring(0, 7)}</span>
              </div>
            )}
            {primaryLocation?.author && (
              <div className="hidden sm:flex items-center gap-1 text-[11px] text-slate-500">
                <User className="w-3 h-3" />
                <span>{primaryLocation.author}</span>
              </div>
            )}
          </div>

          {/* Masked Secret Pill */}
          <div 
            onClick={copyMaskedSecret}
            className="flex items-center gap-1.5 bg-slate-950 px-2.5 py-1 rounded-lg border border-slate-800 text-amber-200/90 font-mono text-xs hover:border-slate-700 transition-colors"
            title="Click to copy masked secret preview"
          >
            <span>{maskedSecretText}</span>
            {copiedSecret ? (
              <Check className="w-3 h-3 text-emerald-400 shrink-0" />
            ) : (
              <Copy className="w-3 h-3 text-slate-500 shrink-0" />
            )}
          </div>
        </div>
      </div>

      {/* Expanded Remediation & Code Preview Section */}
      {isExpanded && (
        <div className="border-t border-slate-800 p-5 space-y-5 bg-slate-950/40">
          {/* Redacted Code Snippet if available */}
          {redactedSnippet && (
            <div className="space-y-1.5">
              <div className="flex items-center justify-between text-xs text-slate-400">
                <span className="font-semibold uppercase tracking-wider text-[11px]">
                  Redacted Code Snippet
                </span>
                <span className="text-[11px] font-mono bg-slate-800 text-amber-300 px-2 py-0.5 rounded border border-slate-700">
                  [REDACTED_SECRET] Guaranteed
                </span>
              </div>
              <div className="bg-[#0b0f19] border border-slate-800 rounded-xl p-4 font-mono text-xs text-slate-300 overflow-x-auto leading-relaxed">
                <pre className="whitespace-pre-wrap break-all">{redactedSnippet}</pre>
              </div>
            </div>
          )}

          {/* Reassurance Banner for RESOLVED Findings (UC-005) */}
          {finding.lifecycle === 'RESOLVED' && (
            <div className="bg-emerald-950/30 border border-emerald-800/60 rounded-xl p-3.5 flex items-start gap-3 text-emerald-200 text-xs">
              <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
              <div>
                <p className="font-semibold text-emerald-100">Finding Marked as Resolved</p>
                <p className="text-emerald-300/90 mt-0.5 leading-relaxed">
                  {finding.remediationQuality === 'VERIFIED_COMPLETE'
                    ? 'The secret has been purged from both current source code and historical git commits.'
                    : 'The secret is no longer present in current code. Verify historical commits if public exposure occurred.'}
                </p>
              </div>
            </div>
          )}

          {/* Gemini AI Remediation Guide */}
          <AiRemediationGuide
            explanation={aiExplanation}
            isLoading={isAiLoading}
            error={aiError}
            onRetry={loadAiExplanation}
          />
        </div>
      )}
    </div>
  );
}
