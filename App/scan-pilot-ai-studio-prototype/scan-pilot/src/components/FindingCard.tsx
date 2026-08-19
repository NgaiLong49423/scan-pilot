import React, { useState } from 'react';
import { 
  AlertTriangle, 
  ShieldAlert, 
  GitCommit, 
  FileCode, 
  Clock, 
  ChevronDown, 
  ChevronUp, 
  Lock 
} from 'lucide-react';
import { Finding } from '../types';
import { RemediationDiff } from './RemediationDiff';

interface FindingCardProps {
  finding: Finding;
  onApplyFix?: (findingId: string) => void;
}

export const FindingCard: React.FC<FindingCardProps> = ({ finding, onApplyFix }) => {
  const [isExpanded, setIsExpanded] = useState(true);

  const getSeverityBadge = () => {
    switch (finding.severity) {
      case 'CRITICAL':
        return 'bg-rose-500/10 text-rose-400 border-rose-500/20';
      case 'HIGH':
        return 'bg-amber-500/10 text-amber-400 border-amber-500/20';
      case 'MEDIUM':
        return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20';
      default:
        return 'bg-sky-500/10 text-sky-400 border-sky-500/20';
    }
  };

  return (
    <div className="bg-slate-900/70 border border-slate-800 rounded-2xl p-5 shadow-sm hover:border-slate-700/80 transition-all duration-150">
      {/* Top row: Severity, Rule ID, Location & Time */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2.5">
          <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-xs font-semibold uppercase tracking-wide border ${getSeverityBadge()}`}>
            {finding.severity === 'CRITICAL' ? (
              <ShieldAlert className="w-3.5 h-3.5 text-rose-400" />
            ) : (
              <AlertTriangle className="w-3.5 h-3.5 text-amber-400" />
            )}
            <span>{finding.severity}</span>
          </span>

          <span className="text-xs font-mono text-slate-400 bg-slate-800/60 px-2 py-0.5 rounded border border-slate-700/50">
            {finding.ruleId}
          </span>

          <span className="text-xs font-semibold text-slate-200">
            {finding.ruleName}
          </span>
        </div>

        <div className="flex items-center gap-3 text-xs text-slate-400">
          <span className="flex items-center gap-1">
            <Clock className="w-3.5 h-3.5 text-slate-500" />
            <span>{finding.detectedAt}</span>
          </span>

          <button
            type="button"
            onClick={() => setIsExpanded(!isExpanded)}
            className="p-1 rounded-md hover:bg-slate-800 text-slate-400 hover:text-slate-200 transition-colors"
            aria-label={isExpanded ? 'Collapse' : 'Expand'}
          >
            {isExpanded ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
          </button>
        </div>
      </div>

      {/* File Path & Commit Metadata */}
      <div className="flex flex-wrap items-center gap-4 mt-3 text-xs text-slate-400 font-mono">
        <div className="flex items-center gap-1.5 bg-slate-950/60 px-2.5 py-1 rounded-md border border-slate-800/80 text-slate-300">
          <FileCode className="w-3.5 h-3.5 text-indigo-400" />
          <span>{finding.filePath}:{finding.lineNumber}</span>
        </div>

        <div className="flex items-center gap-1.5 text-slate-400">
          <GitCommit className="w-3.5 h-3.5 text-slate-500" />
          <span>{finding.detectedCommit}</span>
        </div>

        <div className="flex items-center gap-1.5 text-amber-400/90 font-medium">
          <Lock className="w-3.5 h-3.5" />
          <span>Masked: {finding.rawSecretMasked}</span>
        </div>
      </div>

      {/* Expanded Diff Section */}
      {isExpanded && (
        <RemediationDiff
          diff={finding.remediationDiff}
          onApplyFix={() => onApplyFix && onApplyFix(finding.id)}
        />
      )}
    </div>
  );
};
