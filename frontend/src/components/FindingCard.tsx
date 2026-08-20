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
        return 'bg-[#da3633]/15 text-[#f85149] border-[#da3633]/30';
      case 'HIGH':
        return 'bg-[#d29922]/15 text-[#e3b341] border-[#d29922]/30';
      case 'MEDIUM':
        return 'bg-[#bb8009]/15 text-[#e3b341] border-[#bb8009]/30';
      default:
        return 'bg-[#1f6feb]/15 text-[#58a6ff] border-[#1f6feb]/30';
    }
  };

  return (
    <div className="bg-[#161b22] border border-[#30363d] rounded-2xl p-5 shadow-sm hover:border-[#8b949e]/50 transition-all duration-150">
      {/* Top row: Severity, Rule ID, Location & Time */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2.5">
          <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-xs font-semibold uppercase tracking-wide border ${getSeverityBadge()}`}>
            {finding.severity === 'CRITICAL' ? (
              <ShieldAlert className="w-3.5 h-3.5 text-[#f85149]" />
            ) : (
              <AlertTriangle className="w-3.5 h-3.5 text-[#e3b341]" />
            )}
            <span>{finding.severity}</span>
          </span>

          <span className="text-xs font-mono text-[#8b949e] bg-[#21262d] px-2 py-0.5 rounded border border-[#30363d]">
            {finding.ruleId}
          </span>

          <span className="text-sm font-semibold text-[#f0f6fc]">
            {finding.ruleName}
          </span>
        </div>

        <div className="flex items-center gap-4 text-xs text-[#8b949e]">
          <span className="flex items-center gap-1 font-mono">
            <GitCommit className="w-3.5 h-3.5 text-[#58a6ff]" />
            <span>{finding.detectedCommit}</span>
          </span>
          <span className="flex items-center gap-1">
            <Clock className="w-3.5 h-3.5" />
            <span>{finding.detectedAt}</span>
          </span>
        </div>
      </div>

      {/* Path & Secret Masked Badge */}
      <div className="mt-3 flex flex-wrap items-center gap-3 text-xs">
        <div className="flex items-center gap-1.5 text-[#8b949e] font-mono bg-[#0d1117] px-2.5 py-1 rounded-lg border border-[#30363d]">
          <FileCode className="w-3.5 h-3.5 text-[#58a6ff]" />
          <span className="text-[#f0f6fc]">{finding.filePath}</span>
          <span>:{finding.lineNumber}</span>
        </div>

        <div className="flex items-center gap-1.5 text-xs font-mono text-[#f0f6fc] bg-[#0d1117] px-2.5 py-1 rounded-lg border border-[#30363d]">
          <Lock className="w-3.5 h-3.5 text-[#f85149]" />
          <span>Masked: </span>
          <span className="text-[#f85149] font-bold">{finding.rawSecretMasked}</span>
        </div>

        <button
          type="button"
          onClick={() => setIsExpanded(!isExpanded)}
          className="ml-auto inline-flex items-center gap-1 text-xs text-[#58a6ff] hover:underline"
        >
          <span>{isExpanded ? 'Hide AI Remediation' : 'View AI Remediation'}</span>
          {isExpanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </button>
      </div>

      {/* Collapsible Remediation Diff Viewer */}
      {isExpanded && (
        <div className="mt-4 pt-4 border-t border-[#30363d]">
          <RemediationDiff
            diff={finding.remediationDiff}
            findingId={finding.id}
            isResolved={finding.status === 'RESOLVED'}
            onApplyFix={onApplyFix}
          />
        </div>
      )}
    </div>
  );
};
