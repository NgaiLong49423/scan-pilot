import React, { useState } from 'react';
import { 
  ShieldCheck, 
  Search, 
  FolderGit2, 
  Lock, 
  Globe, 
  GitBranch, 
  ArrowRight,
  AlertTriangle,
  Clock,
  Plus,
  CheckCircle2,
  FolderPlus,
  AlertCircle,
  RefreshCw,
  RotateCcw
} from 'lucide-react';
import { Repository, UserProfile } from '../types';

interface FleetDashboardProps {
  monitoredRepositories: Repository[];
  currentUser: UserProfile | null;
  onSelectRepo: (repo: Repository) => void;
  onOpenImportModal: () => void;
  onLogout?: () => void;
  onRetry?: () => void;
}

export const FleetDashboard: React.FC<FleetDashboardProps> = ({
  monitoredRepositories,
  currentUser,
  onSelectRepo,
  onOpenImportModal,
  onRetry,
}) => {
  const [activeTab, setActiveTab] = useState<
    'ALL' | 'ACTION_REQUIRED' | 'NO_OPEN_FINDINGS' | 'COVERAGE_INCOMPLETE' | 'AWAITING_SCAN' | 'SCAN_IN_PROGRESS'
  >('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedLanguage, setSelectedLanguage] = useState<string>('ALL');

  // Compute fleet-level aggregates strictly from verified repository posture
  const auditedRepos = monitoredRepositories.filter(
    (r) => r.postureStatus === 'ACTION_REQUIRED' || r.postureStatus === 'NO_OPEN_FINDINGS' || r.postureStatus === 'COVERAGE_INCOMPLETE'
  );
  const actionRequiredRepos = monitoredRepositories.filter(
    (r) => r.postureStatus === 'ACTION_REQUIRED' || (r.postureStatus === 'COVERAGE_INCOMPLETE' && r.findingCount > 0)
  );
  const cleanRepos = monitoredRepositories.filter((r) => r.postureStatus === 'NO_OPEN_FINDINGS');
  const incompleteCoverageRepos = monitoredRepositories.filter((r) => r.postureStatus === 'COVERAGE_INCOMPLETE');
  const inProgressRepos = monitoredRepositories.filter((r) => r.postureStatus === 'SCAN_IN_PROGRESS');
  const awaitingScanRepos = monitoredRepositories.filter(
    (r) => r.postureStatus === 'AWAITING_INITIAL_SCAN' || (!r.postureStatus && !r.isScanned)
  );
  const unavailableRepos = monitoredRepositories.filter((r) => r.postureStatus === 'SCAN_UNAVAILABLE');

  // Total open leaks and severity counts describe the exact same verified actionable population (actionRequiredRepos)
  const totalOpenLeaks = actionRequiredRepos.reduce((acc, r) => acc + (r.findingCount || 0), 0);

  const fleetSeverityCounts = actionRequiredRepos.reduce(
    (acc, r) => {
      if (r.severityCounts) {
        acc.critical += r.severityCounts.critical || 0;
        acc.high += r.severityCounts.high || 0;
        acc.medium += r.severityCounts.medium || 0;
        acc.low += r.severityCounts.low || 0;
      }
      return acc;
    },
    { critical: 0, high: 0, medium: 0, low: 0 }
  );

  // Languages from monitored repos
  const languages = ['ALL', ...Array.from(new Set(monitoredRepositories.map((r) => r.language).filter(Boolean)))];

  // Filter repositories
  const filteredRepos = monitoredRepositories.filter((repo) => {
    const matchesSearch = 
      repo.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      repo.language.toLowerCase().includes(searchQuery.toLowerCase());
    
    if (!matchesSearch) return false;

    if (selectedLanguage !== 'ALL' && repo.language !== selectedLanguage) return false;

    if (activeTab === 'ACTION_REQUIRED') {
      return repo.postureStatus === 'ACTION_REQUIRED' || (repo.postureStatus === 'COVERAGE_INCOMPLETE' && repo.findingCount > 0);
    }
    if (activeTab === 'NO_OPEN_FINDINGS') {
      return repo.postureStatus === 'NO_OPEN_FINDINGS';
    }
    if (activeTab === 'COVERAGE_INCOMPLETE') {
      return repo.postureStatus === 'COVERAGE_INCOMPLETE';
    }
    if (activeTab === 'SCAN_IN_PROGRESS') {
      return repo.postureStatus === 'SCAN_IN_PROGRESS';
    }
    if (activeTab === 'AWAITING_SCAN') {
      return repo.postureStatus === 'AWAITING_INITIAL_SCAN' || (!repo.postureStatus && !repo.isScanned);
    }

    return true;
  });

  const getRepoPostureBadge = (repo: Repository) => {
    if (repo.postureStatus === 'SCAN_IN_PROGRESS') {
      return (
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-[#1f6feb]/15 text-[#58a6ff] border border-[#1f6feb]/30">
          <RefreshCw className="w-3.5 h-3.5 animate-spin motion-reduce:animate-none" />
          <span>Scan In Progress</span>
        </span>
      );
    }
    if (repo.postureStatus === 'COVERAGE_INCOMPLETE') {
      return (
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-[#d29922]/15 text-[#d29922] border border-[#d29922]/30">
          <AlertCircle className="w-3.5 h-3.5" />
          <span>Coverage Incomplete ({repo.findingCount} open)</span>
        </span>
      );
    }
    if (repo.postureStatus === 'SCAN_UNAVAILABLE') {
      return (
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-[#da3633]/15 text-[#f85149] border border-[#da3633]/30">
          <AlertTriangle className="w-3.5 h-3.5" />
          <span>Scan Unavailable</span>
        </span>
      );
    }
    if (!repo.isScanned || repo.postureStatus === 'AWAITING_INITIAL_SCAN') {
      return (
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium text-[#8b949e] bg-[#21262d] border border-[#30363d]">
          <Clock className="w-3.5 h-3.5 text-[#8b949e]" />
          <span>Awaiting Initial Scan</span>
        </span>
      );
    }
    if (repo.findingCount === 0) {
      return (
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-[#238636]/15 text-[#3fb950] border border-[#238636]/30">
          <CheckCircle2 className="w-3.5 h-3.5" />
          <span>No Open Findings</span>
        </span>
      );
    }
    return (
      <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-medium bg-[#da3633]/15 text-[#f85149] border border-[#da3633]/30">
        <AlertTriangle className="w-3.5 h-3.5" />
        <span>{repo.findingCount} Open {repo.findingCount === 1 ? 'Finding' : 'Findings'}</span>
      </span>
    );
  };

  return (
    <div className="space-y-8 animate-in fade-in duration-200">
      {/* Top Organization Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-[#30363d] pb-6">
        <div>
          <div className="flex items-center gap-2 text-xs font-mono text-[#8b949e] mb-1">
            <span>Organization:</span>
            <span className="text-[#58a6ff] font-semibold bg-[#1f6feb]/15 px-2 py-0.5 rounded border border-[#1f6feb]/30">
              {currentUser?.login || 'Scan Pilot Workspace'}
            </span>
            <span>•</span>
            <span className="text-[#3fb950] flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-[#3fb950]" />
              GitHub App Connected
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-[#f0f6fc] tracking-tight">
            Organization Fleet Overview
          </h1>
          <p className="text-xs sm:text-sm text-[#8b949e] mt-1">
            Verified evidence-based security posture and action summaries across monitored repositories.
          </p>
        </div>

        <div className="flex items-center gap-3">
          {onRetry && (
            <button
              type="button"
              onClick={onRetry}
              className="inline-flex items-center gap-2 px-3 py-2 rounded-xl bg-[#21262d] hover:bg-[#30363d] text-[#c9d1d9] hover:text-[#f0f6fc] text-xs font-medium border border-[#30363d] transition-all focus:outline-none focus:ring-2 focus:ring-[#58a6ff] focus:ring-offset-2 focus:ring-offset-[#0d1117]"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Refresh Fleet</span>
            </button>
          )}

          <button
            type="button"
            onClick={onOpenImportModal}
            className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-[#238636] hover:bg-[#2ea043] text-white text-xs font-semibold shadow-sm transition-all duration-150 active:scale-95"
          >
            <FolderPlus className="w-4 h-4" />
            <span>Import Repositories</span>
          </button>
        </div>
      </div>

      {/* Global Fleet Posture Bento Row (Evidence-Based ONLY) */}
      <section className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Card 1: Monitored Repos */}
        <div className="p-5 bg-[#161b22] border border-[#30363d] rounded-2xl flex flex-col justify-between shadow-sm">
          <div className="flex items-center justify-between text-[#8b949e]">
            <span className="text-xs font-semibold uppercase tracking-wider">Monitored Fleet</span>
            <FolderGit2 className="w-4 h-4 text-[#58a6ff]" />
          </div>
          <div className="mt-4">
            <div className="text-3xl font-bold text-[#f0f6fc] tabular-nums">
              {monitoredRepositories.length}
            </div>
            <span className="text-xs text-[#8b949e] mt-1 block">
              {auditedRepos.length} Audited • {awaitingScanRepos.length} Awaiting Scan • {unavailableRepos.length} Evidence Unavailable
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-[#30363d]/60 text-[11px] text-[#58a6ff] font-mono">
            Continuous Monitoring
          </div>
        </div>

        {/* Card 2: Action Required */}
        <div className="p-5 bg-[#161b22] border border-[#30363d] rounded-2xl flex flex-col justify-between shadow-sm">
          <div className="flex items-center justify-between text-[#8b949e]">
            <span className="text-xs font-semibold uppercase tracking-wider">Action Required</span>
            <AlertTriangle className={`w-4 h-4 ${actionRequiredRepos.length > 0 ? 'text-[#f85149]' : 'text-[#8b949e]'}`} />
          </div>
          <div className="mt-4">
            <div className={`text-3xl font-bold tabular-nums ${actionRequiredRepos.length > 0 ? 'text-[#f85149]' : 'text-[#f0f6fc]'}`}>
              {actionRequiredRepos.length}
            </div>
            <span className="text-xs text-[#8b949e] mt-1 block">
              {actionRequiredRepos.length > 0 ? 'Repositories with open findings' : 'No repos require urgent remediation'}
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-[#30363d]/60 text-[11px] text-[#8b949e] font-mono">
            Verified Findings
          </div>
        </div>

        {/* Card 3: Total Open Findings */}
        <div className="p-5 bg-[#161b22] border border-[#30363d] rounded-2xl flex flex-col justify-between shadow-sm">
          <div className="flex items-center justify-between text-[#8b949e]">
            <span className="text-xs font-semibold uppercase tracking-wider">Total Open Leaks</span>
            <AlertTriangle className={`w-4 h-4 ${totalOpenLeaks > 0 ? 'text-[#f85149]' : 'text-[#3fb950]'}`} />
          </div>
          <div className="mt-3">
            <div className={`text-3xl font-bold tabular-nums ${totalOpenLeaks > 0 ? 'text-[#f85149]' : 'text-[#f0f6fc]'}`}>
              {totalOpenLeaks}
            </div>
            <span className={`text-xs mt-1 block ${totalOpenLeaks > 0 ? 'text-[#f85149]/80 font-medium' : 'text-[#8b949e]'}`}>
              {totalOpenLeaks > 0 ? `Across ${actionRequiredRepos.length} ${actionRequiredRepos.length === 1 ? 'Repository' : 'Repositories'}` : 'Zero Open Leaks Detected'}
            </span>
            {/* Severity Distribution Pills */}
            <div className="flex items-center gap-1.5 mt-2.5 text-[11px] font-mono">
              <span className="px-1.5 py-0.5 rounded bg-[#da3633]/15 text-[#f85149] border border-[#da3633]/30">
                {fleetSeverityCounts.critical} Crit
              </span>
              <span className="px-1.5 py-0.5 rounded bg-[#d29922]/15 text-[#d29922] border border-[#d29922]/30">
                {fleetSeverityCounts.high} High
              </span>
              <span className="px-1.5 py-0.5 rounded bg-[#1f6feb]/15 text-[#58a6ff] border border-[#1f6feb]/30">
                {fleetSeverityCounts.medium} Med
              </span>
              <span className="px-1.5 py-0.5 rounded bg-[#21262d] text-[#8b949e] border border-[#30363d]">
                {fleetSeverityCounts.low} Low
              </span>
            </div>
          </div>
          <div className="mt-3 pt-3 border-t border-[#30363d]/60 text-[11px] text-[#8b949e] font-mono">
            SP-CONFIG-001 Severity Breakdown
          </div>
        </div>

        {/* Card 4: Clean & Audited Baseline */}
        <div className="p-5 bg-[#161b22] border border-[#30363d] rounded-2xl flex flex-col justify-between shadow-sm">
          <div className="flex items-center justify-between text-[#8b949e]">
            <span className="text-xs font-semibold uppercase tracking-wider">Verified Clean</span>
            <ShieldCheck className="w-4 h-4 text-[#3fb950]" />
          </div>
          <div className="mt-4">
            <div className="text-3xl font-bold text-[#f0f6fc] tabular-nums">
              {cleanRepos.length}
            </div>
            <span className="text-xs text-[#8b949e] mt-1 block">
              {cleanRepos.length > 0 ? 'Completed scans with 0 open findings' : 'Awaiting clean audit baseline'}
            </span>
          </div>
          <div className="mt-3 pt-3 border-t border-[#30363d]/60 text-[11px] text-[#8b949e] font-mono">
            Audit Posture Clean
          </div>
        </div>
      </section>

      {/* Multi-Repository Portfolio Management Section */}
      <section className="space-y-4">
        {/* Filter Controls Bar */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-[#161b22] border border-[#30363d] p-3.5 rounded-2xl">
          {/* Tabs */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 md:pb-0">
            <button
              type="button"
              onClick={() => setActiveTab('ALL')}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-150 ${
                activeTab === 'ALL'
                  ? 'bg-[#1f6feb] text-white shadow-sm'
                  : 'text-[#8b949e] hover:text-[#f0f6fc] hover:bg-[#21262d]'
              }`}
            >
              All Monitored ({monitoredRepositories.length})
            </button>

            <button
              type="button"
              onClick={() => setActiveTab('ACTION_REQUIRED')}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-150 ${
                activeTab === 'ACTION_REQUIRED'
                  ? 'bg-[#da3633] text-white shadow-sm'
                  : 'text-[#8b949e] hover:text-[#f0f6fc] hover:bg-[#21262d]'
              }`}
            >
              Action Required ({actionRequiredRepos.length})
            </button>

            <button
              type="button"
              onClick={() => setActiveTab('NO_OPEN_FINDINGS')}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-150 ${
                activeTab === 'NO_OPEN_FINDINGS'
                  ? 'bg-[#238636] text-white shadow-sm'
                  : 'text-[#8b949e] hover:text-[#f0f6fc] hover:bg-[#21262d]'
              }`}
            >
              No Open Findings ({cleanRepos.length})
            </button>

            {incompleteCoverageRepos.length > 0 && (
              <button
                type="button"
                onClick={() => setActiveTab('COVERAGE_INCOMPLETE')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-150 ${
                  activeTab === 'COVERAGE_INCOMPLETE'
                    ? 'bg-[#d29922] text-black shadow-sm'
                    : 'text-[#8b949e] hover:text-[#f0f6fc] hover:bg-[#21262d]'
                }`}
              >
                Incomplete Coverage ({incompleteCoverageRepos.length})
              </button>
            )}

            {inProgressRepos.length > 0 && (
              <button
                type="button"
                onClick={() => setActiveTab('SCAN_IN_PROGRESS')}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-150 ${
                  activeTab === 'SCAN_IN_PROGRESS'
                    ? 'bg-[#1f6feb] text-white shadow-sm'
                    : 'text-[#8b949e] hover:text-[#f0f6fc] hover:bg-[#21262d]'
                }`}
              >
                In Progress ({inProgressRepos.length})
              </button>
            )}

            <button
              type="button"
              onClick={() => setActiveTab('AWAITING_SCAN')}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-150 ${
                activeTab === 'AWAITING_SCAN'
                  ? 'bg-[#21262d] text-[#c9d1d9] border border-[#30363d]'
                  : 'text-[#8b949e] hover:text-[#f0f6fc] hover:bg-[#21262d]'
              }`}
            >
              Awaiting Scan ({awaitingScanRepos.length})
            </button>
          </div>

          {/* Search & Language Dropdown */}
          <div className="flex items-center gap-3">
            <div className="relative w-full sm:w-60">
              <Search className="w-4 h-4 text-[#8b949e] absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search monitored repos... (Ctrl+K)"
                className="w-full bg-[#0d1117] border border-[#30363d] rounded-lg py-1.5 pl-9 pr-3 text-xs text-[#f0f6fc] placeholder:text-[#8b949e] focus:outline-none focus:ring-2 focus:ring-[#1f6feb]/50"
              />
            </div>

            <select
              value={selectedLanguage}
              onChange={(e) => setSelectedLanguage(e.target.value)}
              className="bg-[#0d1117] border border-[#30363d] rounded-lg py-1.5 px-3 text-xs text-[#c9d1d9] focus:outline-none focus:ring-2 focus:ring-[#1f6feb]/50"
            >
              {languages.map((lang) => (
                <option key={lang} value={lang}>
                  {lang === 'ALL' ? 'All Languages' : lang}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Horizontal List / Table Rows */}
        <div className="space-y-3">
          {filteredRepos.length > 0 ? (
            filteredRepos.map((repo) => (
              <div
                key={repo.id}
                onClick={() => onSelectRepo(repo)}
                className="p-4 sm:p-5 bg-[#161b22] border border-[#30363d] rounded-2xl hover:border-[#58a6ff] transition-all duration-150 cursor-pointer flex flex-col md:flex-row md:items-center justify-between gap-4 group shadow-sm hover:shadow-md"
              >
                {/* Column 1: Repo info & tags */}
                <div className="flex items-center gap-3.5 min-w-0 md:w-2/5">
                  <div className="p-2.5 rounded-xl bg-[#0d1117] border border-[#30363d] text-[#8b949e] group-hover:text-[#58a6ff] group-hover:border-[#58a6ff]/40 transition-colors shrink-0">
                    {repo.isPrivate ? <Lock className="w-4 h-4" /> : <Globe className="w-4 h-4" />}
                  </div>

                  <div className="min-w-0">
                    <div className="text-sm font-bold text-[#f0f6fc] group-hover:text-[#58a6ff] transition-colors truncate font-mono">
                      {repo.name}
                    </div>
                    <div className="flex items-center gap-2 mt-1 text-xs text-[#8b949e]">
                      <span className="flex items-center gap-1 font-mono">
                        <GitBranch className="w-3 h-3 text-[#8b949e]" />
                        {repo.branch}
                      </span>
                      <span>•</span>
                      <span className="px-2 py-0.5 rounded bg-[#0d1117] text-[#c9d1d9] border border-[#30363d] font-mono text-[10px]">
                        {repo.language}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Column 2: Status Badge */}
                <div className="flex items-center gap-4 text-xs md:w-1/3">
                  {getRepoPostureBadge(repo)}
                </div>

                {/* Column 3: Audit Timestamp */}
                <div className="flex items-center gap-6 text-xs text-[#8b949e] font-mono md:w-1/5">
                  <div>
                    <span className="text-[10px] uppercase text-[#8b949e] block font-sans">Audit Status</span>
                    <span className="text-[#c9d1d9] text-xs">
                      {repo.lastScanned || 'Never'}
                    </span>
                  </div>
                </div>

                {/* Column 4: Direct Action Button */}
                <div className="flex items-center justify-end shrink-0">
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      onSelectRepo(repo);
                    }}
                    className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-lg bg-[#21262d] group-hover:bg-[#1f6feb] text-[#c9d1d9] group-hover:text-white border border-[#30363d] group-hover:border-[#1f6feb] text-xs font-medium transition-all duration-150 active:scale-95"
                  >
                    <span>Inspect Posture</span>
                    <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" />
                  </button>
                </div>
              </div>
            ))
          ) : (
            <div className="p-12 text-center bg-[#161b22] border border-[#30363d] rounded-2xl space-y-4">
              <div className="w-12 h-12 rounded-full bg-[#1f6feb]/15 border border-[#1f6feb]/30 text-[#58a6ff] mx-auto flex items-center justify-center">
                <FolderPlus className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-base font-bold text-[#f0f6fc]">
                  {monitoredRepositories.length === 0 
                    ? 'No Repositories Imported Yet' 
                    : 'No Matching Repositories'}
                </h3>
                <p className="text-xs text-[#8b949e] max-w-md mx-auto mt-1">
                  {monitoredRepositories.length === 0
                    ? 'Select and import GitHub repositories from your connected GitHub App to start continuous secret monitoring.'
                    : 'Try changing your search keywords or filter criteria above.'}
                </p>
              </div>
              {monitoredRepositories.length === 0 && (
                <button
                  type="button"
                  onClick={onOpenImportModal}
                  className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-[#238636] hover:bg-[#2ea043] text-white text-xs font-semibold shadow-sm transition-all duration-150 active:scale-95"
                >
                  <Plus className="w-4 h-4" />
                  <span>Import Your First Repository</span>
                </button>
              )}
            </div>
          )}
        </div>
      </section>
    </div>
  );
};
