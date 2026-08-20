import React from 'react';
import { 
  ShieldCheck, 
  ChevronDown, 
  RefreshCw, 
  FolderGit2, 
  GitBranch,
  ShieldAlert,
  FileCheck,
  LogOut
} from 'lucide-react';
import { Repository } from '../types';

interface NavbarProps {
  selectedRepo: Repository | null;
  activeTab: 'findings' | 'coverage';
  onTabChange: (tab: 'findings' | 'coverage') => void;
  isScanning: boolean;
  onTriggerRescan: () => void;
  onOpenRepoModal: () => void;
  onNavigateHome?: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  selectedRepo,
  activeTab,
  onTabChange,
  isScanning,
  onTriggerRescan,
  onOpenRepoModal,
  onNavigateHome,
}) => {
  return (
    <header className="sticky top-0 z-30 w-full border-b border-slate-800/80 bg-slate-950/80 backdrop-blur-md">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
        {/* Left: Logo + Repo Selector */}
        <div className="flex items-center gap-3 sm:gap-6">
          <button
            type="button"
            onClick={onNavigateHome}
            className="flex items-center gap-2.5 text-white font-bold text-lg tracking-tight hover:opacity-90 transition-opacity"
          >
            <div className="p-2 rounded-xl bg-indigo-600/20 border border-indigo-500/30 text-indigo-400 shadow-sm shadow-indigo-500/10">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <span className="hidden sm:inline">Scan Pilot</span>
          </button>

          {/* Repo & Branch Switcher Dropdown */}
          {selectedRepo && (
            <button
              type="button"
              onClick={onOpenRepoModal}
              className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-900/90 border border-slate-800 hover:border-slate-700 hover:bg-slate-800/60 text-slate-200 text-xs font-medium transition-all duration-150 active:scale-98"
            >
              <FolderGit2 className="w-4 h-4 text-indigo-400" />
              <span className="max-w-[130px] sm:max-w-[180px] truncate font-mono">
                {selectedRepo.name}
              </span>
              <span className="hidden md:inline-flex items-center gap-1 text-slate-500 font-mono">
                • <GitBranch className="w-3 h-3 text-slate-400" /> {selectedRepo.branch}
              </span>
              <ChevronDown className="w-3.5 h-3.5 text-slate-400 ml-0.5" />
            </button>
          )}
        </div>

        {/* Center: 2 Navigation Tabs (Findings & Remediation vs Coverage & Audit) */}
        {selectedRepo && (
          <div className="flex items-center p-1 rounded-xl bg-slate-900 border border-slate-800 text-xs font-semibold">
            <button
              type="button"
              onClick={() => onTabChange('findings')}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg transition-all duration-150 ${
                activeTab === 'findings'
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
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
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              <FileCheck className="w-3.5 h-3.5" />
              <span>Coverage & Audit</span>
            </button>
          </div>
        )}

        {/* Right: Monitoring status, Rescan button & Profile */}
        <div className="flex items-center gap-3 sm:gap-4">
          {/* Live Status Pill */}
          <div className="hidden lg:inline-flex items-center gap-2 px-3 py-1 rounded-full bg-slate-900/80 border border-slate-800 text-xs text-slate-300">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>Dual-Stage Active • 4.3s</span>
          </div>

          {/* Trigger Rescan Button */}
          <button
            type="button"
            onClick={onTriggerRescan}
            disabled={isScanning}
            className={`inline-flex items-center gap-2 px-3.5 py-1.5 rounded-lg text-xs font-semibold shadow-sm transition-all duration-150 active:scale-95 ${
              isScanning
                ? 'bg-slate-800 text-slate-400 cursor-not-allowed border border-slate-700'
                : 'bg-indigo-600 hover:bg-indigo-500 text-white shadow-indigo-600/20'
            }`}
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isScanning ? 'animate-spin text-indigo-400' : ''}`} />
            <span>{isScanning ? 'Scanning...' : 'Trigger Rescan'}</span>
          </button>

          {/* User Avatar + Sign Out */}
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-indigo-600 to-cyan-500 p-[1px] shadow-sm">
              <div className="w-full h-full rounded-full bg-slate-950 flex items-center justify-center text-xs font-bold text-white">
                NL
              </div>
            </div>

            <button
              type="button"
              onClick={onNavigateHome}
              className="p-1.5 rounded-lg text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
              title="Sign Out / Back to Landing"
            >
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
