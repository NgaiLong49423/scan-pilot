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
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-[#010409]/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className="w-full max-w-xl bg-[#161b22] border border-[#30363d] rounded-2xl shadow-2xl overflow-hidden flex flex-col max-h-[85vh]">
        {/* Header */}
        <div className="p-5 border-b border-[#30363d] flex items-start justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-xl bg-[#1f6feb]/15 border border-[#1f6feb]/30 text-[#58a6ff]">
              <FolderGit2 className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base font-bold text-[#f0f6fc] tracking-tight">
                Select Monitored Repository
              </h2>
              <p className="text-xs text-[#8b949e] mt-0.5">
                Choose a GitHub repository to monitor with continuous secret scanning.
              </p>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
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
              placeholder="Search repositories... (e.g. scan-pilot, java)"
              className="w-full bg-[#161b22] border border-[#30363d] rounded-lg py-2 pl-9 pr-14 text-xs text-[#f0f6fc] placeholder:text-[#8b949e] focus:outline-none focus:ring-2 focus:ring-[#1f6feb]/50"
            />
            <span className="absolute right-2.5 top-1/2 -translate-y-1/2 text-[10px] text-[#8b949e] bg-[#21262d] px-1.5 py-0.5 rounded border border-[#30363d] font-mono">
              Ctrl K
            </span>
          </div>

          <a
            href="https://github.com/apps"
            target="_blank"
            rel="noreferrer"
            className="inline-flex items-center gap-1.5 px-3 py-2 rounded-lg bg-[#21262d] hover:bg-[#30363d] text-[#c9d1d9] text-xs font-medium border border-[#30363d] transition-all duration-150 shrink-0"
          >
            <span>Add Repos</span>
            <ExternalLink className="w-3 h-3 text-[#8b949e]" />
          </a>
        </div>

        {/* Repository List */}
        <div className="p-4 overflow-y-auto space-y-2.5 flex-1 divide-y-0">
          <span className="text-[11px] font-semibold uppercase tracking-wider text-[#8b949e] px-1 block mb-1">
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

                {/* Right: Health badge + Checkmark */}
                <div className="flex items-center gap-3 shrink-0">
                  {repo.findingCount === 0 ? (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-[#238636]/15 text-[#3fb950] border border-[#238636]/30">
                      <ShieldCheck className="w-3 h-3" />
                      <span>100% Safe</span>
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[10px] font-medium bg-[#d29922]/15 text-[#e3b341] border border-[#d29922]/30">
                      <AlertTriangle className="w-3 h-3" />
                      <span>{repo.findingCount} Leaks</span>
                    </span>
                  )}

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
        </div>

        {/* Footer */}
        <div className="p-4 border-t border-[#30363d] bg-[#0d1117] flex items-center justify-between gap-3">
          <span className="text-[11px] text-[#8b949e] hidden sm:inline">
            33 repositories synced with GitHub App
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
              className="px-4 py-1.5 rounded-lg text-xs font-semibold bg-[#238636] hover:bg-[#2ea043] text-white shadow-sm transition-all duration-150 active:scale-95"
            >
              Monitor This Repository
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
