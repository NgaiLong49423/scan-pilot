import React, { useState, useRef, useEffect } from 'react';
import { 
  ShieldCheck, 
  ChevronDown, 
  RefreshCw, 
  FolderGit2, 
  GitBranch,
  ShieldAlert,
  FileCheck,
  LogOut,
  ExternalLink,
  Github,
  Activity
} from 'lucide-react';
import { Repository, UserProfile } from '../types';

interface NavbarProps {
  selectedRepo: Repository | null;
  currentUser: UserProfile | null;
  activeTab: 'findings' | 'coverage';
  onTabChange: (tab: 'findings' | 'coverage') => void;
  isScanning: boolean;
  onTriggerRescan: () => void;
  onOpenRepoModal: () => void;
  onNavigateHome?: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  selectedRepo,
  currentUser,
  activeTab,
  onTabChange,
  isScanning,
  onTriggerRescan,
  onOpenRepoModal,
  onNavigateHome,
}) => {
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const profileRef = useRef<HTMLDivElement>(null);

  // Close profile dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (profileRef.current && !profileRef.current.contains(event.target as Node)) {
        setIsProfileOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <header className="sticky top-0 z-30 w-full border-b border-[#30363d] bg-[#010409]/95 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
        {/* Left: Logo + Repo Selector */}
        <div className="flex items-center gap-3 sm:gap-6">
          <button
            type="button"
            onClick={onNavigateHome}
            className="flex items-center gap-2.5 text-[#f0f6fc] font-bold text-lg tracking-tight hover:opacity-90 transition-opacity"
          >
            <div className="p-2 rounded-xl bg-[#1f6feb]/15 border border-[#1f6feb]/30 text-[#58a6ff] shadow-sm">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <span className="hidden sm:inline">Scan Pilot</span>
          </button>

          {/* Repo & Branch Switcher Dropdown */}
          {selectedRepo && (
            <button
              type="button"
              onClick={onOpenRepoModal}
              className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-[#161b22] border border-[#30363d] hover:border-[#8b949e] hover:bg-[#21262d] text-[#c9d1d9] text-xs font-medium transition-all duration-150 active:scale-98"
            >
              <FolderGit2 className="w-4 h-4 text-[#58a6ff]" />
              <span className="max-w-[130px] sm:max-w-[180px] truncate font-mono text-[#f0f6fc]">
                {selectedRepo.name}
              </span>
              <span className="hidden md:inline-flex items-center gap-1 text-[#8b949e] font-mono">
                • <GitBranch className="w-3 h-3 text-[#8b949e]" /> {selectedRepo.branch}
              </span>
              <ChevronDown className="w-3.5 h-3.5 text-[#8b949e] ml-0.5" />
            </button>
          )}
        </div>

        {/* Center: 2 Navigation Tabs (Findings & Remediation vs Coverage & Audit) */}
        {selectedRepo && (
          <div className="flex items-center p-1 rounded-xl bg-[#161b22] border border-[#30363d] text-xs font-semibold">
            <button
              type="button"
              onClick={() => onTabChange('findings')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-all duration-150 ${
                activeTab === 'findings'
                  ? 'bg-[#1f6feb] text-white shadow-sm'
                  : 'text-[#8b949e] hover:text-[#f0f6fc]'
              }`}
            >
              <ShieldAlert className="w-3.5 h-3.5" />
              <span>Findings & Remediation</span>
            </button>

            <button
              type="button"
              onClick={() => onTabChange('coverage')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-all duration-150 ${
                activeTab === 'coverage'
                  ? 'bg-[#1f6feb] text-white shadow-sm'
                  : 'text-[#8b949e] hover:text-[#f0f6fc]'
              }`}
            >
              <FileCheck className="w-3.5 h-3.5" />
              <span>Coverage & Audit</span>
            </button>
          </div>
        )}

        {/* Right: Monitoring status, Rescan button & Google-Style Profile Avatar */}
        <div className="flex items-center gap-3 sm:gap-4">
          {/* Live Status Pill */}
          <div className="hidden lg:inline-flex items-center gap-2 px-3 py-1 rounded-full bg-[#161b22] border border-[#30363d] text-xs text-[#c9d1d9]">
            {isScanning ? (
              <>
                <span className="w-2 h-2 rounded-full bg-[#58a6ff] animate-ping" />
                <span className="text-[#58a6ff]">Scanning Pipeline Active</span>
              </>
            ) : selectedRepo?.isScanned ? (
              <>
                <span className="w-2 h-2 rounded-full bg-[#3fb950] animate-pulse" />
                <span>Dual-Stage Verified • 4.31s</span>
              </>
            ) : (
              <>
                <span className="w-2 h-2 rounded-full bg-[#8b949e]" />
                <span className="text-[#8b949e]">Engine Standby • Ready</span>
              </>
            )}
          </div>

          {/* Trigger Rescan Button */}
          <button
            type="button"
            onClick={onTriggerRescan}
            disabled={isScanning}
            className={`inline-flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-semibold shadow-sm transition-all duration-150 active:scale-95 ${
              isScanning
                ? 'bg-[#21262d] text-[#8b949e] cursor-not-allowed border border-[#30363d]'
                : 'bg-[#238636] hover:bg-[#2ea043] text-white shadow-sm'
            }`}
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isScanning ? 'animate-spin text-white' : ''}`} />
            <span>{isScanning ? 'Scanning...' : 'Trigger Rescan'}</span>
          </button>

          {/* Real GitHub Avatar with Google-style Profile Popover */}
          <div className="relative" ref={profileRef}>
            <button
              type="button"
              onClick={() => setIsProfileOpen(!isProfileOpen)}
              className="w-9 h-9 rounded-full bg-gradient-to-tr from-[#1f6feb] to-[#238636] p-[1.5px] shadow-sm hover:scale-105 transition-transform flex items-center justify-center focus:outline-none focus:ring-2 focus:ring-[#1f6feb]/50"
              title="Account Profile & Sign Out"
            >
              {currentUser?.avatarUrl ? (
                <img
                  src={currentUser.avatarUrl}
                  alt={currentUser.name || currentUser.login}
                  className="w-full h-full rounded-full object-cover"
                />
              ) : (
                <div className="w-full h-full rounded-full bg-[#0d1117] flex items-center justify-center text-xs font-bold text-[#f0f6fc]">
                  {currentUser?.login ? currentUser.login.substring(0, 2).toUpperCase() : 'NL'}
                </div>
              )}
            </button>

            {/* Google-style Profile Popover Modal */}
            {isProfileOpen && (
              <div className="absolute right-0 mt-2.5 w-72 bg-[#161b22] border border-[#30363d] rounded-2xl shadow-2xl p-4 z-50 animate-in fade-in zoom-in-95 duration-150">
                {/* Header User info */}
                <div className="flex items-center gap-3 pb-3 border-b border-[#30363d]">
                  <div className="w-12 h-12 rounded-full overflow-hidden bg-[#21262d] shrink-0 border border-[#30363d]">
                    {currentUser?.avatarUrl ? (
                      <img src={currentUser.avatarUrl} alt="" className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-sm font-bold text-[#f0f6fc]">
                        NL
                      </div>
                    )}
                  </div>
                  <div className="min-w-0">
                    <div className="text-sm font-bold text-[#f0f6fc] truncate">
                      {currentUser?.name || currentUser?.login || 'Scan Pilot User'}
                    </div>
                    <div className="text-xs text-[#8b949e] truncate">
                      {currentUser?.email || (currentUser?.login ? `@${currentUser.login}` : 'user@scanpilot.dev')}
                    </div>
                    <div className="inline-flex items-center gap-1 mt-1 text-[10px] text-[#3fb950] bg-[#238636]/15 px-1.5 py-0.5 rounded border border-[#238636]/30">
                      <span className="w-1.5 h-1.5 rounded-full bg-[#3fb950]" />
                      <span>GitHub Connected</span>
                    </div>
                  </div>
                </div>

                {/* GitHub Profile Action Link */}
                <div className="py-2.5">
                  <a
                    href={currentUser?.login ? `https://github.com/${currentUser.login}` : 'https://github.com'}
                    target="_blank"
                    rel="noreferrer"
                    className="w-full flex items-center justify-between px-3 py-2 rounded-xl bg-[#0d1117] hover:bg-[#21262d] text-xs text-[#c9d1d9] font-medium border border-[#30363d] transition-colors"
                  >
                    <span className="flex items-center gap-2">
                      <Github className="w-3.5 h-3.5 text-[#58a6ff]" />
                      <span>View GitHub Profile</span>
                    </span>
                    <ExternalLink className="w-3.5 h-3.5 text-[#8b949e]" />
                  </a>
                </div>

                {/* Sign Out Button */}
                <button
                  type="button"
                  onClick={() => {
                    setIsProfileOpen(false);
                    if (onNavigateHome) onNavigateHome();
                  }}
                  className="w-full flex items-center justify-center gap-2 px-3 py-2 rounded-xl bg-[#da3633]/15 hover:bg-[#da3633]/25 text-[#f85149] text-xs font-semibold border border-[#da3633]/30 transition-all duration-150 active:scale-98"
                >
                  <LogOut className="w-3.5 h-3.5" />
                  <span>Sign out of all accounts</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};
