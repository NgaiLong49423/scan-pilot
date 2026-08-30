import React, { useState, useEffect } from 'react';
import { 
  X, 
  Search, 
  Lock, 
  Globe, 
  GitBranch, 
  ExternalLink, 
  Check, 
  Clock, 
  FolderPlus,
  CheckCircle2,
  AlertCircle,
  AlertTriangle,
  RefreshCw,
  LogIn
} from 'lucide-react';
import { Repository } from '../types';

export interface RepoSelectModalProps {
  isOpen: boolean;
  status: 'LOADING' | 'SUCCESS' | 'UNAUTHORIZED' | 'ERROR';
  errorMessage?: string | null;
  availableRepos: Repository[];
  monitoredRepos: Repository[];
  installUrl?: string | null;
  selectedRepoId?: string;
  onSelectRepo: (repo: Repository) => void;
  onClose: () => void;
  onRetry?: () => void;
  onSignIn?: () => void;
}

export const RepoSelectModal: React.FC<RepoSelectModalProps> = ({
  isOpen,
  status,
  errorMessage,
  availableRepos,
  monitoredRepos,
  installUrl,
  selectedRepoId,
  onSelectRepo,
  onClose,
  onRetry,
  onSignIn,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [activeId, setActiveId] = useState<string>('');

  // Unmonitored repos = available from GitHub App minus already monitored in Scan Pilot
  const unmonitoredRepos = availableRepos.filter(
    (avail) =>
      !monitoredRepos.some(
        (m) =>
          m.name.toLowerCase() === avail.name.toLowerCase() ||
          (avail.githubRepoId && m.githubRepoId === avail.githubRepoId)
      )
  );

  // Default active selection to first available unmonitored repo
  useEffect(() => {
    if (unmonitoredRepos.length > 0) {
      const match = unmonitoredRepos.find((r) => r.id === selectedRepoId);
      setActiveId(match ? match.id : unmonitoredRepos[0].id);
    } else {
      setActiveId('');
    }
  }, [availableRepos, monitoredRepos, selectedRepoId, isOpen]);

  if (!isOpen) return null;

  const filteredRepos = unmonitoredRepos.filter((repo) =>
    repo.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    repo.language.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleConfirm = () => {
    const found = unmonitoredRepos.find((r) => r.id === activeId);
    if (found) {
      onSelectRepo(found);
    }
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[#010409]/80 backdrop-blur-md animate-in fade-in duration-200" role="dialog" aria-modal="true" aria-labelledby="repo-modal-title">
      <div className="w-full max-w-xl bg-[#161b22] border border-[#30363d] rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="p-5 border-b border-[#30363d] flex items-start justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-[#1f6feb]/15 border border-[#1f6feb]/30 text-[#58a6ff]">
              <FolderPlus className="w-5 h-5" />
            </div>
            <div>
              <h2 id="repo-modal-title" className="text-base font-bold text-[#f0f6fc] tracking-tight">
                Import Repository to Monitor
              </h2>
              <p className="text-xs text-[#8b949e] mt-0.5">
                Choose an unmonitored GitHub repository to add to your organization fleet.
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            aria-label="Close modal"
            className="p-1.5 rounded-lg text-[#8b949e] hover:text-[#f0f6fc] hover:bg-[#21262d] transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search & Add Bar */}
        <div className="p-4 border-b border-[#30363d] bg-[#0d1117] flex items-center gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-[#8b949e] absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              disabled={status !== 'SUCCESS' || unmonitoredRepos.length === 0}
              placeholder="Search available repositories... (e.g. studypack, java)"
              className="w-full bg-[#161b22] border border-[#30363d] rounded-lg py-2 pl-9 pr-14 text-xs text-[#f0f6fc] placeholder:text-[#8b949e] focus:outline-none focus:ring-2 focus:ring-[#1f6feb]/50 disabled:opacity-50 disabled:cursor-not-allowed"
            />
            <span className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[10px] text-[#8b949e] bg-[#21262d] px-1.5 py-0.5 rounded border border-[#30363d] font-mono">
              Ctrl K
            </span>
          </div>

          {installUrl ? (
            <a
              href={installUrl}
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg bg-[#21262d] hover:bg-[#30363d] text-[#c9d1d9] text-xs font-medium border border-[#30363d] transition-all duration-150 shrink-0 active:scale-95"
            >
              <span>Configure App</span>
              <ExternalLink className="w-3 h-3 text-[#8b949e]" />
            </a>
          ) : (
            <button
              type="button"
              disabled
              className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg bg-[#21262d] text-[#8b949e] text-xs font-medium border border-[#30363d] opacity-60 cursor-not-allowed shrink-0"
            >
              <span>Configure App</span>
              <ExternalLink className="w-3 h-3 text-[#8b949e]" />
            </button>
          )}
        </div>

        {/* Repository List / Distinct Status States */}
        <div className="p-4 overflow-y-auto space-y-2.5 flex-1">
          {status === 'LOADING' ? (
            <div className="p-8 text-center space-y-3">
              <RefreshCw className="w-6 h-6 text-[#58a6ff] animate-spin mx-auto" />
              <p className="text-xs text-[#8b949e]">Loading accessible repositories from GitHub App...</p>
            </div>
          ) : status === 'UNAUTHORIZED' ? (
            /* State 1: Session Expired */
            <div data-testid="modal-state-unauthorized" className="p-8 text-center bg-[#0d1117] border border-[#d29922]/30 rounded-xl space-y-3 my-4">
              <div className="w-10 h-10 rounded-full bg-[#d29922]/15 border border-[#d29922]/30 text-[#d29922] mx-auto flex items-center justify-center">
                <AlertTriangle className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-bold text-[#f0f6fc]">Session Expired</h3>
              <p className="text-xs text-[#8b949e] max-w-sm mx-auto">
                Your authentication session has expired. Please sign in again with GitHub to view and import your repositories.
              </p>
              {onSignIn && (
                <button
                  type="button"
                  onClick={onSignIn}
                  className="mt-2 inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-[#1f6feb] hover:bg-[#388bfd] text-white text-xs font-semibold shadow-sm transition-all active:scale-95"
                >
                  <LogIn className="w-3.5 h-3.5" />
                  <span>Sign In with GitHub</span>
                </button>
              )}
            </div>
          ) : status === 'ERROR' ? (
            /* State 2: Request Failed */
            <div data-testid="modal-state-error" className="p-8 text-center bg-[#0d1117] border border-[#f85149]/30 rounded-xl space-y-3 my-4">
              <div className="w-10 h-10 rounded-full bg-[#f85149]/15 border border-[#f85149]/30 text-[#f85149] mx-auto flex items-center justify-center">
                <AlertCircle className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-bold text-[#f0f6fc]">Repository Request Failed</h3>
              <p className="text-xs text-[#8b949e] max-w-sm mx-auto">
                {errorMessage || 'Could not retrieve repositories from GitHub API. Please check your network and try again.'}
              </p>
              {onRetry && (
                <button
                  type="button"
                  onClick={onRetry}
                  className="mt-2 inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-[#21262d] hover:bg-[#30363d] text-[#f0f6fc] text-xs font-semibold border border-[#30363d] shadow-sm transition-all active:scale-95"
                >
                  <RefreshCw className="w-3.5 h-3.5" />
                  <span>Retry</span>
                </button>
              )}
            </div>
          ) : availableRepos.length === 0 ? (
            /* State 3: No Repositories Accessible */
            <div data-testid="modal-state-no-accessible" className="p-8 text-center bg-[#0d1117] border border-[#30363d] rounded-xl space-y-3 my-4">
              <div className="w-10 h-10 rounded-full bg-[#58a6ff]/15 border border-[#58a6ff]/30 text-[#58a6ff] mx-auto flex items-center justify-center">
                <FolderPlus className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-bold text-[#f0f6fc]">No Repositories Accessible</h3>
              <p className="text-xs text-[#8b949e] max-w-sm mx-auto">
                No repositories were found under your GitHub App installation. Install or configure the Scan Pilot GitHub App to grant repository access.
              </p>
              {installUrl && (
                <a
                  href={installUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="mt-2 inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-[#238636] hover:bg-[#2ea043] text-white text-xs font-semibold shadow-sm transition-all active:scale-95"
                >
                  <span>Install / Configure GitHub App</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>
              )}
            </div>
          ) : unmonitoredRepos.length === 0 ? (
            /* State 4: All Repositories Monitored */
            <div data-testid="modal-state-all-monitored" className="p-8 text-center bg-[#0d1117] border border-[#238636]/30 rounded-xl space-y-3 my-4">
              <div className="w-10 h-10 rounded-full bg-[#238636]/15 border border-[#238636]/30 text-[#3fb950] mx-auto flex items-center justify-center">
                <CheckCircle2 className="w-5 h-5" />
              </div>
              <h3 className="text-sm font-bold text-[#f0f6fc]">All Repositories Monitored</h3>
              <p className="text-xs text-[#8b949e] max-w-sm mx-auto">
                All {availableRepos.length} accessible repositories from your GitHub App installation have already been imported into your monitored fleet.
              </p>
              {installUrl && (
                <a
                  href={installUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="mt-2 inline-flex items-center gap-1.5 px-4 py-2 rounded-lg bg-[#21262d] hover:bg-[#30363d] text-[#f0f6fc] text-xs font-semibold border border-[#30363d] shadow-sm transition-all active:scale-95"
                >
                  <span>Add More Repositories</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>
              )}
            </div>
          ) : filteredRepos.length === 0 ? (
            /* Search filter with no match */
            <div className="p-8 text-center bg-[#0d1117] border border-[#30363d] rounded-xl space-y-2 my-4">
              <h3 className="text-sm font-bold text-[#f0f6fc]">No Matching Repositories</h3>
              <p className="text-xs text-[#8b949e] max-w-sm mx-auto">
                No unmonitored repositories match "{searchTerm}".
              </p>
            </div>
          ) : (
            /* Normal List of Available Unmonitored Repos */
            <>
              <span className="text-[11px] font-semibold uppercase tracking-wider text-[#8b949e] px-1 block mb-1">
                Available Repositories ({filteredRepos.length})
              </span>

              {filteredRepos.map((repo) => {
                const isSelected = repo.id === activeId;
                return (
                  <div
                    key={repo.id}
                    onClick={() => setActiveId(repo.id)}
                    className={`p-3.5 rounded-xl border transition-all duration-150 cursor-pointer flex items-center justify-between gap-3 ${
                      isSelected
                        ? 'bg-[#1f6feb]/15 border-[#1f6feb] shadow-sm'
                        : 'bg-[#0d1117] border-[#30363d] hover:bg-[#21262d] hover:border-[#8b949e]/60'
                    }`}
                  >
                    <div className="flex items-center gap-3 min-w-0">
                      <div className="text-[#8b949e]">
                        {repo.isPrivate ? <Lock className="w-4 h-4" /> : <Globe className="w-4 h-4" />}
                      </div>

                      <div className="min-w-0">
                        <div className="text-xs font-semibold text-[#f0f6fc] truncate font-mono">
                          {repo.name}
                        </div>
                        <div className="flex items-center gap-3 mt-1 text-[11px] text-[#8b949e]">
                          <span className="flex items-center gap-1 font-mono">
                            <GitBranch className="w-3 h-3 text-[#8b949e]" />
                            {repo.branch}
                          </span>
                          <span className="px-1.5 py-0.5 rounded bg-[#21262d] text-[#c9d1d9] border border-[#30363d]">
                            {repo.language}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Right: Unmonitored badge + Checkmark */}
                    <div className="flex items-center gap-3 shrink-0">
                      <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-medium text-[#8b949e] bg-[#21262d] border border-[#30363d]">
                        <Clock className="w-3 h-3 text-[#8b949e]" />
                        <span>Ready to Import</span>
                      </span>

                      <div
                        className={`w-5 h-5 rounded-full flex items-center justify-center border transition-all ${
                          isSelected
                            ? 'bg-[#1f6feb] border-[#1f6feb] text-white'
                            : 'border-[#30363d] bg-[#21262d] text-transparent'
                        }`}
                      >
                        <Check className="w-3 h-3" />
                      </div>
                    </div>
                  </div>
                );
              })}
            </>
          )}
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-[#30363d] bg-[#0d1117] flex items-center justify-between gap-3">
          <span className="text-[11px] text-[#8b949e] hidden sm:inline">
            {status === 'SUCCESS' ? `${filteredRepos.length} available unmonitored repositories` : '—'}
          </span>

          <div className="flex items-center gap-2 ml-auto">
            <button
              type="button"
              onClick={onClose}
              className="px-3.5 py-1.5 rounded-lg text-xs font-medium text-[#8b949e] hover:text-[#f0f6fc] hover:bg-[#21262d] transition-colors"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleConfirm}
              disabled={status !== 'SUCCESS' || filteredRepos.length === 0 || !activeId}
              className={`px-4 py-1.5 rounded-lg text-xs font-semibold shadow-sm transition-all duration-150 active:scale-95 ${
                status !== 'SUCCESS' || filteredRepos.length === 0 || !activeId
                  ? 'bg-[#21262d] text-[#8b949e] cursor-not-allowed border border-[#30363d]'
                  : 'bg-[#238636] hover:bg-[#2ea043] text-white'
              }`}
            >
              Monitor This Repository
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
