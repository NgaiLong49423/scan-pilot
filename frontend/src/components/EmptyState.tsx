import type { ReactNode } from 'react';
import { ShieldCheck, FolderGit2, FileText, CheckCircle2 } from 'lucide-react';

interface EmptyStateProps {
  type?: 'no-findings' | 'no-project' | 'no-coverage' | 'no-repos' | 'custom';
  title?: string;
  description?: string;
  actionText?: string;
  onAction?: () => void;
  icon?: ReactNode;
}

export function EmptyState({
  type = 'no-findings',
  title,
  description,
  actionText,
  onAction,
  icon,
}: EmptyStateProps) {
  let defaultIcon = <ShieldCheck className="w-12 h-12 text-emerald-400" />;
  let defaultTitle = 'No Active Findings';
  let defaultDesc = 'Your scanned code snapshot and reachable git commits are clean of secret exposures.';

  if (type === 'no-findings') {
    defaultIcon = <ShieldCheck className="w-12 h-12 text-emerald-400" />;
    defaultTitle = 'Zero Secret Exposures Detected';
    defaultDesc = 'All monitored branches and historical commits passed secret detection rules with zero leaks.';
  } else if (type === 'no-project') {
    defaultIcon = <FolderGit2 className="w-12 h-12 text-blue-400" />;
    defaultTitle = 'No Monitored Repository';
    defaultDesc = 'Connect a GitHub repository to enable snapshot scanning, git history verification, and AI remediation.';
  } else if (type === 'no-coverage') {
    defaultIcon = <FileText className="w-12 h-12 text-slate-400" />;
    defaultTitle = 'No Coverage Telemetry Yet';
    defaultDesc = 'Trigger a scan to evaluate file classifications, scanned text files, and skipped binary assets.';
  } else if (type === 'no-repos') {
    defaultIcon = <CheckCircle2 className="w-12 h-12 text-amber-400" />;
    defaultTitle = 'No GitHub Repositories Accessible';
    defaultDesc = 'Install the Scan Pilot GitHub App on your GitHub account or organizations to grant access.';
  }

  const finalTitle = title || defaultTitle;
  const finalDesc = description || defaultDesc;
  const finalIcon = icon || defaultIcon;

  return (
    <div className="bg-slate-900/40 border border-slate-800/80 rounded-2xl p-10 flex flex-col items-center justify-center text-center max-w-xl mx-auto my-6">
      <div className="p-4 rounded-2xl bg-slate-800/50 border border-slate-700/50 mb-4 flex items-center justify-center shadow-inner">
        {finalIcon}
      </div>
      <h3 className="text-lg font-semibold text-white tracking-tight mb-2">
        {finalTitle}
      </h3>
      <p className="text-sm text-slate-400 leading-relaxed mb-6 max-w-md">
        {finalDesc}
      </p>
      {actionText && onAction && (
        <button
          onClick={onAction}
          className="bg-blue-600 hover:bg-blue-500 active:bg-blue-700 text-white font-medium px-5 py-2.5 rounded-lg text-sm transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50"
        >
          {actionText}
        </button>
      )}
    </div>
  );
}
