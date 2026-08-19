import React, { useState, useEffect } from 'react';
import { 
  ShieldCheck, 
  Search, 
  Filter, 
  Layers, 
  CheckCircle2, 
  Clock, 
  ArrowLeft,
  Sparkles,
  ExternalLink,
  GitCommit,
  FileCode 
} from 'lucide-react';
import { Repository, Finding, HealthMetrics, UserProfile } from './types';
import { 
  fetchRepositories, 
  fetchFindingsForRepo, 
  fetchHealthMetrics,
  fetchCurrentUser,
  loginWithGitHub,
  logoutUser,
  triggerRealScan
} from './services/api';
import { Navbar } from './components/Navbar';
import { HealthGauge } from './components/HealthGauge';
import { TrendSparkline } from './components/TrendSparkline';
import { MetricsGrid } from './components/MetricsGrid';
import { FindingCard } from './components/FindingCard';
import { RepoSelectModal } from './components/RepoSelectModal';
import { HeroLanding } from './components/HeroLanding';
import { ScanProgressStepper } from './components/ScanProgressStepper';
import { CoverageAuditView } from './components/CoverageAuditView';

export default function App() {
  const [currentView, setCurrentView] = useState<'landing' | 'dashboard'>('landing');
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  const [activeNavTab, setActiveNavTab] = useState<'findings' | 'coverage'>('findings');
  const [repositories, setRepositories] = useState<Repository[]>([]);
  const [selectedRepo, setSelectedRepo] = useState<Repository | null>(null);
  const [findings, setFindings] = useState<Finding[]>([]);
  const [metrics, setMetrics] = useState<HealthMetrics | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [isRepoModalOpen, setIsRepoModalOpen] = useState(false);
  const [severityFilter, setSeverityFilter] = useState<'ALL' | 'CRITICAL' | 'HIGH' | 'RESOLVED'>('ALL');
  const [scanModeFilter, setScanModeFilter] = useState<'ALL' | 'SNAPSHOT' | 'HISTORY'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');

  // Initial load: Check real backend authentication session
  useEffect(() => {
    async function loadData() {
      // 1. Check if user is already logged in via backend session cookie
      const user = await fetchCurrentUser();
      if (user) {
        setCurrentUser(user);
        setCurrentView('dashboard');
      }

      // 2. Load repositories
      const repos = await fetchRepositories();
      setRepositories(repos);
      if (repos.length > 0) {
        const firstRepo = repos[0];
        setSelectedRepo(firstRepo);
        const [repoFindings, repoMetrics] = await Promise.all([
          fetchFindingsForRepo(firstRepo.id),
          fetchHealthMetrics(firstRepo.id),
        ]);
        setFindings(repoFindings);
        setMetrics(repoMetrics);
      }
    }
    loadData();
  }, []);

  // Handle real GitHub OAuth Sign-in
  const handleGitHubLogin = () => {
    loginWithGitHub();
  };

  // Handle Demo Mode
  const handleExploreDemo = () => {
    setCurrentView('dashboard');
  };

  // Handle selecting another repo
  const handleSelectRepo = async (repo: Repository) => {
    setSelectedRepo(repo);
    const [repoFindings, repoMetrics] = await Promise.all([
      fetchFindingsForRepo(repo.id),
      fetchHealthMetrics(repo.id),
    ]);
    setFindings(repoFindings);
    setMetrics(repoMetrics);
  };

  // Handle trigger scan
  const handleTriggerRescan = async () => {
    setIsScanning(true);
    await triggerRealScan(selectedRepo?.branch);
    setTimeout(() => {
      setIsScanning(false);
    }, 2000);
  };

  // Handle applying an AI fix
  const handleApplyFix = (findingId: string) => {
    setFindings((prev) =>
      prev.map((f) =>
        f.id === findingId
          ? {
              ...f,
              status: 'RESOLVED',
              remediationQuality: 'VERIFIED_COMPLETE',
            }
          : f
      )
    );

    if (metrics) {
      setMetrics({
        ...metrics,
        openLeaksCount: Math.max(0, metrics.openLeaksCount - 1),
        resolvedLeaksCount: metrics.resolvedLeaksCount + 1,
        healthScore: Math.min(100, metrics.healthScore + 3),
      });
    }
  };

  if (currentView === 'landing') {
    return (
      <HeroLanding
        onSignIn={handleGitHubLogin}
        onExploreDemo={handleExploreDemo}
      />
    );
  }

  const filteredFindings = findings.filter((f) => {
    const matchesSearch = 
      f.ruleName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      f.filePath.toLowerCase().includes(searchQuery.toLowerCase()) ||
      f.ruleId.toLowerCase().includes(searchQuery.toLowerCase());

    if (!matchesSearch) return false;

    // Scan Mode filter
    if (scanModeFilter === 'SNAPSHOT' && !f.detectedCommit.includes('HEAD')) return false;
    if (scanModeFilter === 'HISTORY' && f.detectedCommit.includes('HEAD-02')) return false;

    if (severityFilter === 'ALL') return true;
    if (severityFilter === 'RESOLVED') return f.status === 'RESOLVED';
    return f.severity === severityFilter && f.status === 'OPEN';
  });

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 font-sans flex flex-col justify-between selection:bg-indigo-500/30">
      {/* Sticky Top Navbar with 2 Main Navigation Tabs */}
      <Navbar
        selectedRepo={selectedRepo}
        activeTab={activeNavTab}
        onTabChange={setActiveNavTab}
        isScanning={isScanning}
        onTriggerRescan={handleTriggerRescan}
        onOpenRepoModal={() => setIsRepoModalOpen(true)}
        onNavigateHome={logoutUser}
      />

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full space-y-6">
        {/* Dual-Stage Scan Stepper Progress Banner */}
        {selectedRepo && (
          <ScanProgressStepper
            isScanning={isScanning}
            branchName={selectedRepo.branch}
          />
        )}

        {/* Tab 1: Findings & Remediation View */}
        {activeNavTab === 'findings' && (
          <div className="space-y-8 animate-in fade-in duration-200">
            {/* Top Overview Section: Health Gauge + Trend Sparkline + Metrics */}
            {metrics && (
              <section className="space-y-4">
                <div className="flex items-center justify-between">
                  <div>
                    <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight">
                      Security Posture & Findings
                    </h1>
                    <p className="text-xs sm:text-sm text-slate-400 mt-1">
                      Continuous secret detection & AI-assisted remediation across commit history.
                    </p>
                  </div>

                  <button
                    type="button"
                    onClick={logoutUser}
                    className="hidden sm:inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-slate-900 border border-slate-800 hover:bg-slate-800 text-xs text-slate-400 hover:text-slate-200 transition-colors"
                  >
                    <ArrowLeft className="w-3.5 h-3.5" />
                    <span>Landing Page</span>
                  </button>
                </div>

                {/* Visual Analytics Bento Banner */}
                <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-stretch">
                  <div className="md:col-span-4 lg:col-span-3">
                    <HealthGauge score={metrics.healthScore} grade={metrics.grade} />
                  </div>
                  <div className="md:col-span-8 lg:col-span-4">
                    <TrendSparkline data={metrics.trendData} />
                  </div>
                  <div className="md:col-span-12 lg:col-span-5">
                    <MetricsGrid metrics={metrics} />
                  </div>
                </div>
              </section>
            )}

            {/* Finding Stream Section */}
            <section className="space-y-4">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-slate-800/80 pb-4">
                {/* Filter Tabs by Severity */}
                <div className="flex items-center gap-1.5 overflow-x-auto pb-1 sm:pb-0">
                  <button
                    type="button"
                    onClick={() => setSeverityFilter('ALL')}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-150 ${
                      severityFilter === 'ALL'
                        ? 'bg-indigo-600 text-white shadow-sm'
                        : 'bg-slate-900 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                    }`}
                  >
                    All Findings ({findings.length})
                  </button>

                  <button
                    type="button"
                    onClick={() => setSeverityFilter('CRITICAL')}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-150 ${
                      severityFilter === 'CRITICAL'
                        ? 'bg-rose-600 text-white shadow-sm'
                        : 'bg-slate-900 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                    }`}
                  >
                    Critical ({findings.filter((f) => f.severity === 'CRITICAL' && f.status === 'OPEN').length})
                  </button>

                  <button
                    type="button"
                    onClick={() => setSeverityFilter('HIGH')}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-150 ${
                      severityFilter === 'HIGH'
                        ? 'bg-amber-600 text-white shadow-sm'
                        : 'bg-slate-900 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                    }`}
                  >
                    High ({findings.filter((f) => f.severity === 'HIGH' && f.status === 'OPEN').length})
                  </button>

                  <button
                    type="button"
                    onClick={() => setSeverityFilter('RESOLVED')}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-150 ${
                      severityFilter === 'RESOLVED'
                        ? 'bg-emerald-600 text-white shadow-sm'
                        : 'bg-slate-900 text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                    }`}
                  >
                    Resolved ({findings.filter((f) => f.status === 'RESOLVED').length})
                  </button>
                </div>

                {/* Search Input */}
                <div className="relative w-full sm:w-64">
                  <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
                  <input
                    type="text"
                    value={searchQuery}
                    onChange={(e) => setSearchQuery(e.target.value)}
                    placeholder="Search rule, path..."
                    className="w-full bg-slate-900 border border-slate-800 rounded-lg py-1.5 pl-9 pr-3 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-indigo-500/50"
                  />
                </div>
              </div>

              {/* Finding Cards List */}
              <div className="space-y-4">
                {filteredFindings.length > 0 ? (
                  filteredFindings.map((finding) => (
                    <FindingCard
                      key={finding.id}
                      finding={finding}
                      onApplyFix={handleApplyFix}
                    />
                  ))
                ) : (
                  <div className="p-12 text-center bg-slate-900/40 border border-slate-800/80 rounded-2xl space-y-3">
                    <div className="w-12 h-12 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 mx-auto flex items-center justify-center">
                      <CheckCircle2 className="w-6 h-6" />
                    </div>
                    <h3 className="text-base font-semibold text-white">No Matching Findings</h3>
                    <p className="text-xs text-slate-400 max-w-sm mx-auto">
                      All monitored files are clean or no findings match your active filter criteria.
                    </p>
                  </div>
                )}
              </div>
            </section>
          </div>
        )}

        {/* Tab 2: Coverage & Audit View */}
        {activeNavTab === 'coverage' && selectedRepo && (
          <CoverageAuditView repo={selectedRepo} />
        )}
      </main>

      {/* Footer */}
      <footer className="w-full border-t border-slate-800/80 bg-slate-950 py-6 text-xs text-slate-500 mt-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <span className="font-semibold text-slate-400">Scan Pilot Security</span>
            <span>•</span>
            <span>Google Cloud Run & Spring Boot 3 Engine</span>
          </div>

          <div className="flex items-center gap-4 text-[11px]">
            <span>SP-CONFIG-001 Verified</span>
            <span>•</span>
            <span>Zero Raw Secret Policy</span>
          </div>
        </div>
      </footer>

      {/* Repository Selector Modal */}
      <RepoSelectModal
        isOpen={isRepoModalOpen}
        repositories={repositories}
        selectedRepoId={selectedRepo?.id || ''}
        onSelectRepo={handleSelectRepo}
        onClose={() => setIsRepoModalOpen(false)}
      />
    </div>
  );
}
