import React, { useState } from 'react';
import { 
  X, 
  Search, 
  FolderGit2, 
  Lock, 
  Globe, 
  GitBranch, 
  ExternalLink, 
  Check, 
  ShieldCheck,
  AlertTriangle 
} from 'lucide-react';
import { Repository } from '../types';

interface RepoSelectModalProps {
  isOpen: boolean;
  repositories: Repository[];
  selectedRepoId: string;
  onSelectRepo: (repo: Repository) => void;
  onClose: () => void;
}

export const RepoSelectModal: React.FC<RepoSelectModalProps> = ({
  isOpen,
  repositories,
  selectedRepoId,
  onSelectRepo,
  onClose,
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [activeId, setActiveId] = useState(selectedRepoId);

  if (!isOpen) return null;

  const filteredRepos = repositories.filter((repo) =>
    repo.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    repo.language.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleConfirm = () => {
    const found = repositories.find((r) => r.id === activeId);
    if (found) {
      onSelectRepo(found);
    }
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/70 backdrop-blur-md animate-in fade-in duration-200">
      <div className="w-full max-w-xl bg-slate-900 border border-slate-800 rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="p-5 border-b border-slate-800 flex items-start justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-indigo-600/10 border border-indigo-500/20 text-indigo-400">
              <FolderGit2 className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-white tracking-tight">
                Select Monitored Repository
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">
                Choose a GitHub repository to monitor with continuous secret scanning.
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Search & Add Bar */}
        <div className="p-4 border-b border-slate-800/80 bg-slate-950/40 flex items-center gap-3">
          <div className="relative flex-1">
            <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search repositories... (e.g. scan-pilot, java)"
              className="w-full bg-slate-900 border border-slate-800 rounded-lg py-2 pl-9 pr-14 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/50"
            />
            <span className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[10px] text-slate-500 bg-slate-800 px-1.5 py-0.5 rounded border border-slate-700 font-mono">
              Ctrl K
            </span>
          </div>

          <a
            href="https://github.com/apps"
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 text-xs font-medium border border-slate-700/60 transition-all duration-150 shrink-0"
          >
            <span>Add Repos</span>
            <ExternalLink className="w-3 h-3" />
          </a>
        </div>

        {/* Repository List */}
        <div className="p-4 overflow-y-auto space-y-2.5 flex-1 divide-y-0">
          <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-500 px-1 block mb-1">
            Accessible Repositories ({filteredRepos.length})
          </span>

          {filteredRepos.map((repo) => {
            const isSelected = repo.id === activeId;
            return (
              <div
                key={repo.id}
                onClick={() => setActiveId(repo.id)}
                className={`p-3.5 rounded-xl border transition-all duration-150 cursor-pointer flex items-center justify-between gap-3 ${
                  isSelected
                    ? 'bg-indigo-600/10 border-indigo-500/50 shadow-sm'
                    : 'bg-slate-950/60 border-slate-800/80 hover:bg-slate-800/40 hover:border-slate-700'
                }`}
              >
                <div className="flex items-center gap-3 min-w-0">
                  <div className="text-slate-400">
                    {repo.isPrivate ? <Lock className="w-4 h-4" /> : <Globe className="w-4 h-4" />}
                  </div>

                  <div className="min-w-0">
                    <div className="text-xs font-semibold text-white truncate font-mono">
                      {repo.name}
                    </div>
                    <div className="flex items-center gap-3 mt-1 text-[11px] text-slate-400">
                      <span className="flex items-center gap-1 font-mono">
                        <GitBranch className="w-3 h-3 text-slate-500" />
                        {repo.branch}
                      </span>
                      <span className="px-1.5 py-0.2 rounded bg-slate-800/80 text-slate-300 border border-slate-700/50">
                        {repo.language}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Right: Health badge + Checkmark */}
                <div className="flex items-center gap-3 shrink-0">
                  {repo.findingCount === 0 ? (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                      <ShieldCheck className="w-3 h-3" />
                      <span>100% Safe</span>
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20">
                      <AlertTriangle className="w-3 h-3" />
                      <span>{repo.findingCount} Leaks</span>
                    </span>
                  )}

                  <div
                    className={`w-5 h-5 rounded-full flex items-center justify-center border transition-all ${
                      isSelected
                        ? 'bg-indigo-600 border-indigo-500 text-white'
                        : 'border-slate-700 bg-slate-800 text-transparent'
                    }`}
                  >
                    <Check className="w-3 h-3" />
                  </div>
                </div>
              </div>
            );
          })}
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-slate-800 bg-slate-950/60 flex items-center justify-between gap-3">
          <span className="text-[11px] text-slate-500 hidden sm:inline">
            33 repositories synced with GitHub App
          </span>

          <div className="flex items-center gap-2 ml-auto">
            <button
              type="button"
              onClick={onClose}
              className="px-3.5 py-1.5 rounded-lg text-xs font-medium text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleConfirm}
              className="px-4 py-1.5 rounded-lg text-xs font-semibold bg-indigo-600 hover:bg-indigo-500 text-white shadow-sm shadow-indigo-600/20 transition-all duration-150 active:scale-95"
            >
              Monitor This Repository
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
