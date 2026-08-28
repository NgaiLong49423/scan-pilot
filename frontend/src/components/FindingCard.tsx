import React, { useState } from 'react';
import { 
  AlertTriangle, 
  ShieldAlert, 
  GitCommit, 
  FileCode, 
  Clock, 
  ChevronDown, 
  ChevronUp, 
  Lock,
  ExternalLink,
  GitPullRequest
} from 'lucide-react';
import { Finding, FindingIssueLinkDto } from '../types';
import { RemediationDiff } from './RemediationDiff';
import { CreateIssueModal } from './CreateIssueModal';

interface FindingCardProps {
  finding: Finding;
  onApplyFix?: (findingId: string) => void;
}

export const FindingCard: React.FC<FindingCardProps> = ({ finding, onApplyFix }) => {
  const [isExpanded, setIsExpanded] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [currentFinding, setCurrentFinding] = useState<Finding>(finding);

  const getSeverityBadge = () => {
    switch (currentFinding.severity) {
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

  const handleIssueCreated = (_findingId: string, issueLink: FindingIssueLinkDto) => {
    setCurrentFinding((prev) => ({
      ...prev,
      githubIssueNumber: issueLink.githubIssueNumber,
      githubIssueUrl: issueLink.githubIssueUrl,
      issueLinkState: issueLink.state,
    }));
  };

  return (
    <div className="bg-[#161b22] border border-[#30363d] rounded-2xl p-5 shadow-sm hover:border-[#8b949e]/50 transition-all duration-150">
      {/* Top row: Severity, Rule ID, Location & Time */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2.5">
          <span className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-md text-xs font-semibold uppercase tracking-wide border ${getSeverityBadge()}`}>
            {currentFinding.severity === 'CRITICAL' ? (
              <ShieldAlert className="w-3.5 h-3.5 text-[#f85149]" />
            ) : (
              <AlertTriangle className="w-3.5 h-3.5 text-[#e3b341]" />
            )}
            <span>{currentFinding.severity}</span>
          </span>

          <span className="text-xs font-mono text-[#8b949e] bg-[#21262d] px-2 py-0.5 rounded border border-[#30363d]">
            {currentFinding.ruleId}
          </span>

          <span className="text-sm font-semibold text-[#f0f6fc]">
            {currentFinding.ruleName}
          </span>
        </div>

        <div className="flex items-center gap-4 text-xs text-[#8b949e]">
          <span className="flex items-center gap-1 font-mono">
            <GitCommit className="w-3.5 h-3.5 text-[#58a6ff]" />
            <span>{currentFinding.detectedCommit}</span>
          </span>
          <span className="flex items-center gap-1">
            <Clock className="w-3.5 h-3.5" />
            <span>{currentFinding.detectedAt}</span>
          </span>
        </div>
      </div>

      {/* Path, Secret Masked Badge & Issue Link Action */}
      <div className="mt-3 flex flex-wrap items-center gap-3 text-xs">
        <div className="flex items-center gap-1.5 text-[#8b949e] font-mono bg-[#0d1117] px-2.5 py-1 rounded-lg border border-[#30363d]">
          <FileCode className="w-3.5 h-3.5 text-[#58a6ff]" />
          <span className="text-[#f0f6fc]">{currentFinding.filePath}</span>
          <span>:{currentFinding.lineNumber}</span>
        </div>

        <div className="flex items-center gap-1.5 text-xs font-mono text-[#f0f6fc] bg-[#0d1117] px-2.5 py-1 rounded-lg border border-[#30363d]">
          <Lock className="w-3.5 h-3.5 text-[#f85149]" />
          <span>Masked: </span>
          <span className="text-[#f85149] font-bold">{currentFinding.rawSecretMasked}</span>
        </div>

        {/* GitHub Issue Link / Action Button */}
        {currentFinding.githubIssueUrl ? (
          <a
            href={currentFinding.githubIssueUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium text-[#58a6ff] bg-[#1f6feb]/15 hover:bg-[#1f6feb]/25 border border-[#1f6feb]/30 transition-colors focus-visible:ring-2 focus-visible:ring-[#58a6ff] focus-visible:outline-none"
          >
            <GitPullRequest className="w-3.5 h-3.5" />
            <span>Issue #{currentFinding.githubIssueNumber || ''}</span>
            <ExternalLink className="w-3 h-3 ml-0.5" />
          </a>
        ) : (
          <button
            type="button"
            onClick={() => setIsModalOpen(true)}
            className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-lg text-xs font-medium text-[#c9d1d9] bg-[#21262d] hover:bg-[#30363d] hover:text-white border border-[#30363d] transition-colors focus-visible:ring-2 focus-visible:ring-[#58a6ff] focus-visible:outline-none"
          >
            <GitPullRequest className="w-3.5 h-3.5 text-[#8b949e]" />
            <span>Create GitHub Issue</span>
          </button>
        )}

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
            diff={currentFinding.remediationDiff}
            findingId={currentFinding.id}
            isResolved={currentFinding.status === 'RESOLVED'}
            onApplyFix={onApplyFix}
          />
        </div>
      )}

      {/* Create Issue Modal */}
      <CreateIssueModal
        isOpen={isModalOpen}
        finding={currentFinding}
        onClose={() => setIsModalOpen(false)}
        onIssueCreated={handleIssueCreated}
      />
    </div>
  );
};
