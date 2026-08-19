import { useState, useEffect } from 'react';
import { 
  X, 
  Search, 
  Lock, 
  Globe, 
  GitBranch, 
  Check, 
  ExternalLink, 
  RefreshCw, 
  AlertCircle,
  Plus,
  Trash2
} from 'lucide-react';
import { GitHubRepository, MonitoredProject } from '../types/api';
import { githubApi } from '../api/githubApi';
import { projectsApi } from '../api/projectsApi';
import { CardSkeleton } from './LoadingSkeleton';
import { EmptyState } from './EmptyState';

interface RepoSelectorModalProps {
  isOpen: boolean;
  currentProject: MonitoredProject | null;
  onClose: () => void;
  onSelectSuccess: (project: MonitoredProject) => void;
}

export function RepoSelectorModal({
  isOpen,
  currentProject,
  onClose,
  onSelectSuccess,
}: RepoSelectorModalProps) {
  const [repositories, setRepositories] = useState<GitHubRepository[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [installUrl, setInstallUrl] = useState<string | null>(null);

  // Branch configuration sub-state
  const [selectedRepo, setSelectedRepo] = useState<GitHubRepository | null>(null);
  const [secondaryBranches, setSecondaryBranches] = useState<string[]>([]);
  const [newBranchInput, setNewBranchInput] = useState('');

  useEffect(() => {
    if (isOpen) {
      loadData();
    }
  }, [isOpen]);

  const loadData = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const [repos, installData] = await Promise.allSettled([
        githubApi.getAccessibleRepositories(),
        githubApi.getInstallUrl(),
      ]);

      if (repos.status === 'fulfilled') {
        setRepositories(repos.value);
        if (currentProject) {
          const match = repos.value.find((r) => r.id === currentProject.githubRepoId);
          if (match) {
            setSelectedRepo(match);
            setSecondaryBranches(currentProject.secondaryBranches || []);
          }
        }
      } else {
        throw repos.reason;
      }

      if (installData.status === 'fulfilled') {
        setInstallUrl(installData.value.installUrl);
      }
    } catch (err: any) {
      setError(err?.message || 'Failed to load GitHub repositories.');
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen) return null;

  const filteredRepos = repositories.filter((r) =>
    r.fullName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleSelectRepo = (repo: GitHubRepository) => {
    setSelectedRepo(repo);
    if (currentProject && currentProject.githubRepoId === repo.id) {
      setSecondaryBranches(currentProject.secondaryBranches || []);
    } else {
      setSecondaryBranches([]);
    }
  };

  const handleAddSecondaryBranch = () => {
    const trimmed = newBranchInput.trim();
    if (!trimmed) return;
    if (secondaryBranches.includes(trimmed)) {
      setNewBranchInput('');
      return;
    }
    if (secondaryBranches.length >= 2) {
      setError('Maximum 2 secondary branches allowed (FR-020, FR-023).');
      return;
    }
    setSecondaryBranches([...secondaryBranches, trimmed]);
    setNewBranchInput('');
    setError(null);
  };

  const handleRemoveSecondaryBranch = (branchToRemove: string) => {
    setSecondaryBranches(secondaryBranches.filter((b) => b !== branchToRemove));
  };

  const handleSubmit = async () => {
    if (!selectedRepo) return;
    setIsSaving(true);
    setError(null);
    try {
      // 1. Select / Onboard repository
      const project = await projectsApi.selectRepository(
        selectedRepo.id,
        selectedRepo.fullName
      );

      // 2. If secondary branches configured, update branches
      let updatedProject = project;
      if (secondaryBranches.length > 0) {
        updatedProject = await projectsApi.updateBranches(secondaryBranches);
      }

      onSelectSuccess(updatedProject);
      onClose();
    } catch (err: any) {
      setError(err?.message || 'Failed to update monitored repository.');
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 bg-slate-950/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div 
        className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-2xl shadow-2xl flex flex-col max-h-[90vh] overflow-hidden"
        role="dialog"
        aria-modal="true"
        aria-labelledby="repo-modal-title"
      >
        {/* Modal Header */}
        <div className="p-5 border-b border-slate-800 flex items-center justify-between">
          <div>
            <h2 id="repo-modal-title" className="text-lg font-semibold text-white tracking-tight">
              Select Monitored Repository
            </h2>
            <p className="text-xs text-slate-400 mt-0.5">
              Choose a GitHub repository to monitor with continuous secret scanning (UC-002).
            </p>
          </div>
          <button
            onClick={onClose}
            className="text-slate-400 hover:text-white p-1.5 rounded-lg hover:bg-slate-800 transition-colors"
            aria-label="Close modal"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body */}
        <div className="p-5 overflow-y-auto space-y-5 flex-1">
          {error && (
            <div className="bg-rose-950/40 border border-rose-800/60 rounded-xl p-3.5 flex items-start gap-3 text-rose-200 text-xs">
              <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
              <div className="flex-1">{error}</div>
            </div>
          )}

          {/* Search bar & Install App Link */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
            <div className="relative flex-1">
              <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Search repositories..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full bg-slate-950 border border-slate-800 rounded-lg py-2 pl-9 pr-4 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500/50"
              />
            </div>
            {installUrl && (
              <a
                href={installUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center gap-1.5 text-xs text-blue-400 hover:text-blue-300 bg-blue-500/10 hover:bg-blue-500/20 border border-blue-500/30 px-3 py-2 rounded-lg transition-colors"
              >
                <span>Add Repositories</span>
                <ExternalLink className="w-3.5 h-3.5" />
              </a>
            )}
          </div>

          {/* Repositories List */}
          <div className="space-y-2">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Accessible Repositories ({filteredRepos.length})
            </span>

            {isLoading ? (
              <div className="space-y-2 pt-2">
                <CardSkeleton />
                <CardSkeleton />
              </div>
            ) : filteredRepos.length === 0 ? (
              <EmptyState
                type="no-repos"
                title="No Repositories Found"
                description={
                  searchQuery
                    ? `No repositories matched "${searchQuery}".`
                    : "No repositories accessible. Install the GitHub App to grant repository access."
                }
                actionText={installUrl ? "Install Scan Pilot GitHub App" : undefined}
                onAction={installUrl ? () => window.open(installUrl, '_blank') : undefined}
              />
            ) : (
              <div className="max-h-56 overflow-y-auto space-y-1.5 pr-1 border border-slate-800/80 rounded-xl p-2 bg-slate-950/40">
                {filteredRepos.map((repo) => {
                  const isSelected = selectedRepo?.id === repo.id;
                  const isCurrentlyMonitored = currentProject?.githubRepoId === repo.id;

                  return (
                    <div
                      key={repo.id}
                      onClick={() => handleSelectRepo(repo)}
                      className={`p-3 rounded-lg border transition-all cursor-pointer flex items-center justify-between gap-3 ${
                        isSelected
                          ? 'bg-blue-600/10 border-blue-500/50 text-white shadow-sm'
                          : 'bg-slate-900/60 hover:bg-slate-800/60 border-slate-800/80 text-slate-300'
                      }`}
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
                        {repo.isPrivate ? (
                          <Lock className="w-4 h-4 text-amber-400 shrink-0" />
                        ) : (
                          <Globe className="w-4 h-4 text-slate-400 shrink-0" />
                        )}
                        <div className="min-w-0">
                          <p className="text-xs font-medium truncate">{repo.fullName}</p>
                          <div className="flex items-center gap-2 text-[11px] text-slate-400">
                            <span className="flex items-center gap-1">
                              <GitBranch className="w-3 h-3 text-slate-500" />
                              {repo.defaultBranch}
                            </span>
                            {isCurrentlyMonitored && (
                              <span className="text-emerald-400 bg-emerald-500/10 px-1.5 py-0.2 rounded border border-emerald-500/20">
                                Active Monitored
                              </span>
                            )}
                          </div>
                        </div>
                      </div>

                      <div className="shrink-0">
                        {isSelected && (
                          <div className="w-5 h-5 rounded-full bg-blue-600 flex items-center justify-center text-white">
                            <Check className="w-3.5 h-3.5" />
                          </div>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>

          {/* Secondary Branch Configuration (FR-020, FR-023) */}
          {selectedRepo && (
            <div className="p-4 rounded-xl bg-slate-950/60 border border-slate-800/80 space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold text-slate-300 uppercase tracking-wider flex items-center gap-1.5">
                  <GitBranch className="w-3.5 h-3.5 text-blue-400" />
                  Secondary Monitored Branches (Max 2 slots)
                </span>
                <span className="text-[11px] text-slate-500 tabular-nums">
                  {secondaryBranches.length}/2 slots used
                </span>
              </div>

              <div className="flex flex-wrap gap-2">
                <span className="text-xs bg-slate-800 border border-slate-700 text-slate-300 px-2.5 py-1 rounded-md flex items-center gap-1.5">
                  <span className="w-1.5 h-1.5 rounded-full bg-blue-400"></span>
                  Primary: {selectedRepo.defaultBranch}
                </span>

                {secondaryBranches.map((branch) => (
                  <span
                    key={branch}
                    className="text-xs bg-blue-950/40 border border-blue-800/60 text-blue-300 px-2.5 py-1 rounded-md flex items-center gap-1.5"
                  >
                    <span>{branch}</span>
                    <button
                      onClick={() => handleRemoveSecondaryBranch(branch)}
                      className="text-blue-400 hover:text-rose-400 transition-colors"
                      title="Remove branch slot"
                    >
                      <Trash2 className="w-3 h-3" />
                    </button>
                  </span>
                ))}
              </div>

              {secondaryBranches.length < 2 && (
                <div className="flex items-center gap-2 pt-1">
                  <input
                    type="text"
                    placeholder="e.g. staging, develop"
                    value={newBranchInput}
                    onChange={(e) => setNewBranchInput(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        handleAddSecondaryBranch();
                      }
                    }}
                    className="bg-slate-900 border border-slate-800 rounded-lg px-3 py-1.5 text-xs text-slate-200 placeholder:text-slate-600 focus:outline-none focus:ring-2 focus:ring-blue-500/50 flex-1"
                  />
                  <button
                    onClick={handleAddSecondaryBranch}
                    type="button"
                    className="bg-slate-800 hover:bg-slate-700 text-slate-200 px-3 py-1.5 rounded-lg text-xs font-medium flex items-center gap-1 border border-slate-700 transition-colors"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    Add Slot
                  </button>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-4 border-t border-slate-800 bg-slate-900/80 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            disabled={isSaving}
            className="px-4 py-2 rounded-lg text-xs font-medium text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSubmit}
            disabled={!selectedRepo || isSaving}
            className="bg-blue-600 hover:bg-blue-500 active:bg-blue-700 disabled:bg-slate-800 disabled:text-slate-500 disabled:cursor-not-allowed text-white text-xs font-semibold px-4 py-2 rounded-lg flex items-center gap-2 transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50"
          >
            {isSaving && <RefreshCw className="w-3.5 h-3.5 animate-spin" />}
            {isSaving ? 'Saving Selection...' : 'Monitor This Repository'}
          </button>
        </div>
      </div>
    </div>
  );
}
