import { useState } from 'react';
import { 
  ShieldCheck, 
  GitBranch, 
  LogOut, 
  Github, 
  ExternalLink,
  ChevronDown
} from 'lucide-react';
import { UserProfile, MonitoredProject } from '../types/api';

interface HeaderProps {
  user: UserProfile | null;
  project: MonitoredProject | null;
  activeTab: 'findings' | 'coverage';
  onTabChange: (tab: 'findings' | 'coverage') => void;
  onOpenRepoSelector: () => void;
  onLogout: () => void;
  onLogin: () => void;
}

export function Header({
  user,
  project,
  activeTab,
  onTabChange,
  onOpenRepoSelector,
  onLogout,
  onLogin,
}: HeaderProps) {
  const [dropdownOpen, setDropdownOpen] = useState(false);

  return (
    <header className="border-b border-slate-800 bg-slate-900/70 backdrop-blur-md sticky top-0 z-30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between gap-4">
        {/* Brand & Project Info */}
        <div className="flex items-center gap-6">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-blue-600 to-indigo-500 p-0.5 shadow-md shadow-blue-500/20 flex items-center justify-center">
              <ShieldCheck className="w-5 h-5 text-white" />
            </div>
            <div className="flex flex-col">
              <span className="text-white font-bold tracking-tight text-base flex items-center gap-1.5">
                Scan Pilot
                <span className="text-[10px] uppercase font-semibold tracking-wider px-1.5 py-0.2 bg-blue-500/10 text-blue-400 border border-blue-500/20 rounded">
                  MVP
                </span>
              </span>
            </div>
          </div>

          {/* Active Monitored Repository Tag */}
          {user && (
            <button
              onClick={onOpenRepoSelector}
              className="group hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-lg bg-slate-800/80 hover:bg-slate-800 border border-slate-700/60 hover:border-slate-600 transition-all text-xs text-slate-200"
              title="Click to switch or configure monitored repository"
            >
              <span className="w-2 h-2 rounded-full bg-emerald-400"></span>
              <span className="font-medium text-slate-300 group-hover:text-white truncate max-w-[180px]">
                {project ? project.fullName : 'Select Repository'}
              </span>
              {project && (
                <span className="flex items-center gap-1 text-[11px] text-slate-400 bg-slate-900/60 px-1.5 py-0.5 rounded border border-slate-700/50">
                  <GitBranch className="w-3 h-3" />
                  {project.primaryBranch}
                </span>
              )}
              <ChevronDown className="w-3.5 h-3.5 text-slate-400 group-hover:text-slate-200" />
            </button>
          )}
        </div>

        {/* View Switcher (Tabs) */}
        {user && project && (
          <nav className="hidden md:flex items-center bg-slate-950/60 p-1 rounded-lg border border-slate-800/80">
            <button
              onClick={() => onTabChange('findings')}
              className={`px-3.5 py-1.5 rounded-md text-xs font-medium transition-all ${
                activeTab === 'findings'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
              }`}
            >
              Findings & Remediation
            </button>
            <button
              onClick={() => onTabChange('coverage')}
              className={`px-3.5 py-1.5 rounded-md text-xs font-medium transition-all ${
                activeTab === 'coverage'
                  ? 'bg-blue-600 text-white shadow-sm'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900'
              }`}
            >
              Coverage & Audit
            </button>
          </nav>
        )}

        {/* User Account Controls */}
        <div className="flex items-center gap-3">
          {user ? (
            <div className="relative">
              <button
                onClick={() => setDropdownOpen(!dropdownOpen)}
                className="flex items-center gap-2.5 p-1 rounded-full sm:rounded-lg sm:px-2.5 sm:py-1.5 hover:bg-slate-800 border border-transparent hover:border-slate-700 transition-colors focus:outline-none"
              >
                {user.avatarUrl ? (
                  <img
                    src={user.avatarUrl}
                    alt={user.login}
                    className="w-7 h-7 rounded-full border border-slate-700 object-cover"
                  />
                ) : (
                  <div className="w-7 h-7 rounded-full bg-blue-600/30 border border-blue-500/40 text-blue-300 text-xs font-bold flex items-center justify-center">
                    {user.login.substring(0, 2).toUpperCase()}
                  </div>
                )}
                <span className="hidden sm:inline text-xs font-medium text-slate-200 truncate max-w-[120px]">
                  {user.name || user.login}
                </span>
                <ChevronDown className="w-3.5 h-3.5 text-slate-400 hidden sm:block" />
              </button>

              {dropdownOpen && (
                <>
                  <div
                    className="fixed inset-0 z-40"
                    onClick={() => setDropdownOpen(false)}
                  />
                  <div className="absolute right-0 mt-2 w-56 bg-slate-900 border border-slate-800 rounded-xl shadow-xl z-50 p-2 text-xs divide-y divide-slate-800/80 animate-in fade-in zoom-in-95 duration-150">
                    <div className="p-2 space-y-0.5">
                      <p className="font-semibold text-slate-200">{user.name || user.login}</p>
                      <p className="text-slate-500 truncate">{user.email || `@${user.login}`}</p>
                    </div>
                    <div className="py-1">
                      <button
                        onClick={() => {
                          setDropdownOpen(false);
                          onOpenRepoSelector();
                        }}
                        className="w-full text-left px-2.5 py-1.5 rounded-lg text-slate-300 hover:bg-slate-800 hover:text-white transition-colors flex items-center justify-between"
                      >
                        <span>Change Repository</span>
                        <ExternalLink className="w-3.5 h-3.5 text-slate-500" />
                      </button>
                    </div>
                    <div className="pt-1">
                      <button
                        onClick={() => {
                          setDropdownOpen(false);
                          onLogout();
                        }}
                        className="w-full text-left px-2.5 py-1.5 rounded-lg text-rose-400 hover:bg-rose-950/40 hover:text-rose-300 transition-colors flex items-center gap-2"
                      >
                        <LogOut className="w-3.5 h-3.5" />
                        <span>Sign out</span>
                      </button>
                    </div>
                  </div>
                </>
              )}
            </div>
          ) : (
            <button
              onClick={onLogin}
              className="bg-slate-800 hover:bg-slate-700 text-white font-medium px-4 py-2 rounded-lg text-xs flex items-center gap-2 border border-slate-700 transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50"
            >
              <Github className="w-4 h-4" />
              <span>Sign in with GitHub</span>
            </button>
          )}
        </div>
      </div>

      {/* Mobile Subnav for tabs */}
      {user && project && (
        <div className="md:hidden border-t border-slate-800/80 px-4 py-2 flex items-center justify-around bg-slate-900/90">
          <button
            onClick={() => onTabChange('findings')}
            className={`px-3 py-1 text-xs font-medium rounded-md ${
              activeTab === 'findings' ? 'bg-blue-600 text-white' : 'text-slate-400'
            }`}
          >
            Findings
          </button>
          <button
            onClick={() => onTabChange('coverage')}
            className={`px-3 py-1 text-xs font-medium rounded-md ${
              activeTab === 'coverage' ? 'bg-blue-600 text-white' : 'text-slate-400'
            }`}
          >
            Coverage
          </button>
          <button
            onClick={onOpenRepoSelector}
            className="px-3 py-1 text-xs font-medium text-slate-400 flex items-center gap-1"
          >
            <GitBranch className="w-3 h-3" />
            Switch Repo
          </button>
        </div>
      )}
    </header>
  );
}
