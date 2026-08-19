import { useState, useEffect, useRef } from 'react';
import { 
  ShieldCheck, 
  Search, 
  GitBranch, 
  RefreshCw, 
  AlertTriangle,
  Play,
  Github,
  ExternalLink,
  Filter,
  CheckSquare,
  Layers
} from 'lucide-react';
import { 
  UserProfile, 
  MonitoredProject, 
  Finding, 
  CoverageSummary, 
  ScanJob
} from './types/api';
import { authApi } from './api/authApi';
import { projectsApi } from './api/projectsApi';
import { scansApi } from './api/scansApi';
import { Header } from './components/Header';
import { RepoSelectorModal } from './components/RepoSelectorModal';
import { ScanProgressBar } from './components/ScanProgressBar';
import { FindingCard } from './components/FindingCard';
import { CoverageTab } from './components/CoverageTab';
import { CardSkeleton, MetricSkeleton } from './components/LoadingSkeleton';
import { EmptyState } from './components/EmptyState';
import { ErrorBanner } from './components/ErrorBanner';

export default function App() {
  // Global & Session State
  const [user, setUser] = useState<UserProfile | null>(null);
  const [project, setProject] = useState<MonitoredProject | null>(null);
  const [activeTab, setActiveTab] = useState<'findings' | 'coverage'>('findings');
  const [isRepoModalOpen, setIsRepoModalOpen] = useState(false);

  // Data State
  const [findings, setFindings] = useState<Finding[]>([]);
  const [coverage, setCoverage] = useState<CoverageSummary | null>(null);
  const [selectedBranch, setSelectedBranch] = useState<string>('main');

  // Scan Job & Polling State
  const [activeScanJob, setActiveScanJob] = useState<ScanJob | null>(null);
  const [isTriggeringScan, setIsTriggeringScan] = useState(false);
  const pollingTimerRef = useRef<number | null>(null);

  // Filter & Search State
  const [severityFilter, setSeverityFilter] = useState<string>('ALL');
  const [lifecycleFilter, setLifecycleFilter] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Loading & Error States (4-state completeness)
  const [isLoadingInitial, setIsLoadingInitial] = useState(true);
  const [isLoadingFindings, setIsLoadingFindings] = useState(false);
  const [isLoadingCoverage, setIsLoadingCoverage] = useState(false);
  const [globalError, setGlobalError] = useState<string | null>(null);
  const [authError, setAuthError] = useState<string | null>(null);

  // 1. Initial Load & OAuth Error Handling
  useEffect(() => {
    const urlParams = new URLSearchParams(window.location.search);
    const authErr = urlParams.get('auth_error') || urlParams.get('error');
    if (authErr) {
      setAuthError(`GitHub Authentication Notice: ${authErr.replace(/_/g, ' ')}`);
      // Clean query string without page reload
      window.history.replaceState({}, document.title, window.location.pathname);
    }

    bootstrapApp();

    return () => {
      if (pollingTimerRef.current) {
        clearInterval(pollingTimerRef.current);
      }
    };
  }, []);

  const bootstrapApp = async () => {
    setIsLoadingInitial(true);
    setGlobalError(null);
    try {
      // Fetch authenticated user
      const currentUser = await authApi.getMe();
      setUser(currentUser);

      if (currentUser) {
        // Fetch currently monitored project
        const currentProj = await projectsApi.getCurrentProject();
        setProject(currentProj);

        if (currentProj) {
          setSelectedBranch(currentProj.primaryBranch || currentProj.defaultBranch || 'main');
          await Promise.allSettled([
            loadFindings(currentProj.id),
            loadCoverage(currentProj.id),
          ]);
        }
      }
    } catch (err: any) {
      setGlobalError(err?.message || 'Failed to initialize Scan Pilot dashboard.');
    } finally {
      setIsLoadingInitial(false);
    }
  };

  const loadFindings = async (repositoryId: string) => {
    setIsLoadingFindings(true);
    try {
      const data = await scansApi.getFindings(repositoryId);
      setFindings(data || []);
    } catch (err: any) {
      setGlobalError(err?.message || 'Failed to load security findings.');
    } finally {
      setIsLoadingFindings(false);
    }
  };

  const loadCoverage = async (repositoryId: string) => {
    setIsLoadingCoverage(true);
    try {
      const data = await scansApi.getCoverage(repositoryId);
      setCoverage(data);
    } catch (err: any) {
      // 404 is normal before first scan
      if (err?.status !== 404) {
        setGlobalError(err?.message || 'Failed to load coverage report.');
      }
    } finally {
      setIsLoadingCoverage(false);
    }
  };

  // 2. Scan Trigger & Real-Time Polling (UC-003)
  const handleTriggerScan = async (branchName?: string) => {
    if (!project) return;
    const targetBranch = branchName || selectedBranch || project.primaryBranch || 'main';

    setIsTriggeringScan(true);
    setGlobalError(null);

    try {
      const response = await scansApi.triggerScan(targetBranch, project.id);
      
      const initialJob: ScanJob = {
        id: response.jobId,
        repositoryId: response.repositoryId || project.id,
        branchName: response.branchName || targetBranch,
        scanMode: 'CONTINUOUS_MONITORING',
        status: (response.status as any) || 'PENDING',
        commitSha: null,
        durationMs: null,
        errorMessage: null,
        startedAt: new Date().toISOString(),
        completedAt: null,
      };
      setActiveScanJob(initialJob);

      // Start real-time polling loop every 1.5 seconds
      startScanPolling(response.jobId, project.id);
    } catch (err: any) {
      setGlobalError(err?.message || 'Failed to trigger security scan.');
      setIsTriggeringScan(false);
    }
  };

  const startScanPolling = (jobId: string, repositoryId: string) => {
    if (pollingTimerRef.current) {
      clearInterval(pollingTimerRef.current);
    }

    pollingTimerRef.current = window.setInterval(async () => {
      try {
        const job = await scansApi.getScanJob(jobId);
        setActiveScanJob(job);

        if (job.status === 'COMPLETED') {
          if (pollingTimerRef.current) clearInterval(pollingTimerRef.current);
          setIsTriggeringScan(false);
          // Refresh findings and coverage silently
          loadFindings(repositoryId);
          loadCoverage(repositoryId);
        } else if (job.status === 'FAILED') {
          if (pollingTimerRef.current) clearInterval(pollingTimerRef.current);
          setIsTriggeringScan(false);
          setGlobalError(job.errorMessage || 'Scan job failed during execution.');
        }
      } catch (err: any) {
        if (pollingTimerRef.current) clearInterval(pollingTimerRef.current);
        setIsTriggeringScan(false);
        setGlobalError(err?.message || 'Error polling scan job status.');
      }
    }, 1500);
  };

  const handleSelectProjectSuccess = async (newProject: MonitoredProject) => {
    setProject(newProject);
    setSelectedBranch(newProject.primaryBranch || newProject.defaultBranch || 'main');
    await Promise.allSettled([
      loadFindings(newProject.id),
      loadCoverage(newProject.id),
    ]);
  };

  const handleLogout = async () => {
    try {
      await authApi.logout();
      setUser(null);
      setProject(null);
      setFindings([]);
      setCoverage(null);
    } catch (err: any) {
      setGlobalError(err?.message || 'Failed to log out.');
    }
  };

  const handleLogin = () => {
    window.location.href = authApi.getLoginUrl();
  };

  // Filtered findings calculation
  const filteredFindings = findings.filter((f) => {
    const matchesSeverity = severityFilter === 'ALL' || f.severity === severityFilter;
    const matchesLifecycle = lifecycleFilter === 'ALL' || f.lifecycle === lifecycleFilter;
    const matchesSearch = 
      f.title?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      f.ruleId?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      f.description?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (f.locations && f.locations.some((l) => l.filePath.toLowerCase().includes(searchQuery.toLowerCase())));

    return matchesSeverity && matchesLifecycle && matchesSearch;
  });

  const openFindingsCount = findings.filter((f) => f.lifecycle === 'OPEN').length;
  const resolvedFindingsCount = findings.filter((f) => f.lifecycle === 'RESOLVED').length;

  return (
    <div className="min-h-screen bg-slate-950 text-slate-300 font-sans flex flex-col antialiased">
      {/* Top Header Navigation */}
      <Header
        user={user}
        project={project}
        activeTab={activeTab}
        onTabChange={setActiveTab}
        onOpenRepoSelector={() => setIsRepoModalOpen(true)}
        onLogout={handleLogout}
        onLogin={handleLogin}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8 space-y-6">
        {/* Auth Error Banner */}
        {authError && (
          <ErrorBanner
            message={authError}
            onDismiss={() => setAuthError(null)}
          />
        )}

        {/* Global Error Banner with Retry */}
        {globalError && (
          <ErrorBanner
            message={globalError}
            onRetry={project ? () => bootstrapApp() : undefined}
            onDismiss={() => setGlobalError(null)}
          />
        )}

        {/* Initial Loading Skeleton */}
        {isLoadingInitial ? (
          <div className="space-y-6 pt-4">
            <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
              <MetricSkeleton />
              <MetricSkeleton />
              <MetricSkeleton />
              <MetricSkeleton />
            </div>
            <CardSkeleton />
            <CardSkeleton />
          </div>
        ) : !user ? (
          /* Unauthenticated Landing State */
          <div className="py-12 sm:py-20 text-center max-w-2xl mx-auto space-y-8 animate-in fade-in duration-300">
            <div className="w-16 h-16 rounded-3xl bg-gradient-to-tr from-blue-600 to-indigo-600 p-0.5 mx-auto shadow-xl shadow-blue-500/20 flex items-center justify-center">
              <ShieldCheck className="w-9 h-9 text-white" />
            </div>
            <div className="space-y-3">
              <h1 className="text-3xl sm:text-4xl font-bold text-white tracking-tight">
                Continuous Secret Detection & AI-Assisted Remediation
              </h1>
              <p className="text-sm sm:text-base text-slate-400 leading-relaxed max-w-xl mx-auto">
                Scan Pilot guards your GitHub repositories with instant snapshot scans, git history verification, and actionable Gemini AI remediation guidance.
              </p>
            </div>
            <div className="pt-2">
              <button
                onClick={handleLogin}
                className="bg-blue-600 hover:bg-blue-500 active:bg-blue-700 text-white font-semibold px-6 py-3 rounded-xl text-sm inline-flex items-center gap-2.5 transition-all shadow-lg shadow-blue-600/30 focus:outline-none focus:ring-2 focus:ring-blue-500/50 cursor-pointer"
              >
                <Github className="w-5 h-5" />
                <span>Sign in with GitHub</span>
              </button>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-left pt-8 border-t border-slate-800/80 text-xs">
              <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
                <div className="font-semibold text-white mb-1">Dual-Stage Detection</div>
                <div className="text-slate-400 leading-relaxed">Scans current HEAD snapshots and reachable Git commit history.</div>
              </div>
              <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
                <div className="font-semibold text-white mb-1">Zero Secret Leaks</div>
                <div className="text-slate-400 leading-relaxed">Full masking, redaction, and SHA-256 fingerprinting at all times.</div>
              </div>
              <div className="p-4 rounded-xl bg-slate-900/60 border border-slate-800">
                <div className="font-semibold text-white mb-1">Gemini AI Guidance</div>
                <div className="text-slate-400 leading-relaxed">Structured checklists, code diffs, and revocation commands.</div>
              </div>
            </div>
          </div>
        ) : !project ? (
          /* Authenticated but No Repository Selected */
          <EmptyState
            type="no-project"
            title="Connect a Repository to Start Monitoring"
            description="Select an accessible repository to enable continuous secret scanning, git history verification, and AI remediation guidance."
            actionText="Select Monitored Repository"
            onAction={() => setIsRepoModalOpen(true)}
          />
        ) : (
          /* Authenticated & Monitored Repository Active */
          <div className="space-y-6 animate-in fade-in duration-300">
            {/* Project Header Bar */}
            <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-5 shadow-sm flex flex-col lg:flex-row lg:items-center justify-between gap-4">
              <div className="space-y-1">
                <div className="flex flex-wrap items-center gap-3">
                  <h2 className="text-xl font-bold text-white tracking-tight">
                    {project.fullName}
                  </h2>
                  <a
                    href={`https://github.com/${project.fullName}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-slate-500 hover:text-slate-300 transition-colors"
                    title="View repository on GitHub"
                  >
                    <ExternalLink className="w-4 h-4" />
                  </a>
                  {project.isPrivate ? (
                    <span className="text-[11px] font-medium bg-slate-800 text-slate-400 px-2 py-0.5 rounded border border-slate-700">
                      Private
                    </span>
                  ) : (
                    <span className="text-[11px] font-medium bg-slate-800 text-slate-400 px-2 py-0.5 rounded border border-slate-700">
                      Public
                    </span>
                  )}
                </div>
                <div className="flex flex-wrap items-center gap-3 text-xs text-slate-400">
                  <span className="flex items-center gap-1 text-slate-300 font-medium">
                    <GitBranch className="w-3.5 h-3.5 text-blue-400" />
                    Primary: {project.primaryBranch}
                  </span>
                  {project.secondaryBranches && project.secondaryBranches.length > 0 && (
                    <span className="flex items-center gap-1 text-slate-400">
                      Secondary slots ({project.secondaryBranches.length}): {project.secondaryBranches.join(', ')}
                    </span>
                  )}
                </div>
              </div>

              {/* Branch Selector & Trigger Scan Button */}
              <div className="flex flex-wrap items-center gap-3">
                <div className="flex items-center gap-2">
                  <select
                    value={selectedBranch}
                    onChange={(e) => setSelectedBranch(e.target.value)}
                    disabled={isTriggeringScan}
                    className="bg-slate-950 border border-slate-800 rounded-lg px-3 py-2 text-xs text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/50"
                  >
                    <option value={project.primaryBranch}>{project.primaryBranch} (Primary)</option>
                    {(project.secondaryBranches || []).map((b) => (
                      <option key={b} value={b}>{b}</option>
                    ))}
                  </select>

                  <button
                    onClick={() => handleTriggerScan(selectedBranch)}
                    disabled={isTriggeringScan}
                    className="bg-blue-600 hover:bg-blue-500 active:bg-blue-700 disabled:bg-slate-800 disabled:text-slate-500 disabled:cursor-not-allowed text-white text-xs font-semibold px-4 py-2 rounded-lg flex items-center gap-2 transition-colors shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50 cursor-pointer"
                  >
                    {isTriggeringScan ? (
                      <>
                        <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                        <span>Scanning...</span>
                      </>
                    ) : (
                      <>
                        <Play className="w-3.5 h-3.5 fill-current" />
                        <span>Run Security Scan</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>

            {/* Real-time Scan Progress Bar (UC-003) */}
            {activeScanJob && (
              <ScanProgressBar
                scanJob={activeScanJob}
                onDismiss={() => setActiveScanJob(null)}
                onRetry={() => handleTriggerScan(activeScanJob.branchName)}
              />
            )}

            {/* Metrics Overview Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-sm">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                    Total Findings
                  </span>
                  <div className="p-2 rounded-xl bg-blue-500/10 text-blue-400 border border-blue-500/20">
                    <Layers className="w-4 h-4" />
                  </div>
                </div>
                <div className="mt-3">
                  <span className="text-2xl font-bold text-white tabular-nums">
                    {findings.length}
                  </span>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Across current & historical commits
                  </p>
                </div>
              </div>

              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-sm">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                    Open / Action Required
                  </span>
                  <div className="p-2 rounded-xl bg-rose-500/10 text-rose-400 border border-rose-500/20">
                    <AlertTriangle className="w-4 h-4" />
                  </div>
                </div>
                <div className="mt-3">
                  <span className={`text-2xl font-bold tabular-nums ${openFindingsCount > 0 ? 'text-rose-400' : 'text-emerald-400'}`}>
                    {openFindingsCount}
                  </span>
                  <p className="text-xs text-slate-500 mt-0.5">
                    {openFindingsCount > 0 ? 'Requires immediate remediation' : 'No active exposures'}
                  </p>
                </div>
              </div>

              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-sm">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                    Resolved Findings
                  </span>
                  <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                    <ShieldCheck className="w-4 h-4" />
                  </div>
                </div>
                <div className="mt-3">
                  <span className="text-2xl font-bold text-emerald-400 tabular-nums">
                    {resolvedFindingsCount}
                  </span>
                  <p className="text-xs text-slate-500 mt-0.5">
                    Contained or verified clean (UC-005)
                  </p>
                </div>
              </div>

              <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-sm">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
                    Scan Coverage
                  </span>
                  <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
                    <CheckSquare className="w-4 h-4" />
                  </div>
                </div>
                <div className="mt-3">
                  <span className="text-2xl font-bold text-white tabular-nums">
                    {coverage && coverage.totalFiles > 0
                      ? `${Math.round((coverage.scannedFiles / coverage.totalFiles) * 100)}%`
                      : '100%'}
                  </span>
                  <p className="text-xs text-slate-500 mt-0.5">
                    {coverage ? `${coverage.scannedFiles} of ${coverage.totalFiles} files` : 'Ready for evaluation'}
                  </p>
                </div>
              </div>
            </div>

            {/* Tab Views */}
            {activeTab === 'findings' ? (
              /* Findings Tab */
              <div className="space-y-4">
                {/* Search and Filters Toolbar */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 bg-slate-900/60 p-4 rounded-xl border border-slate-800">
                  <div className="relative flex-1 max-w-md">
                    <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      placeholder="Search by rule, title, or file path..."
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      className="w-full bg-slate-950 border border-slate-800 rounded-lg py-2 pl-9 pr-4 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500/50"
                    />
                  </div>

                  <div className="flex flex-wrap items-center gap-2.5">
                    <div className="flex items-center gap-1.5 text-xs text-slate-400">
                      <Filter className="w-3.5 h-3.5 text-slate-500" />
                      <span>Severity:</span>
                    </div>
                    <select
                      value={severityFilter}
                      onChange={(e) => setSeverityFilter(e.target.value)}
                      className="bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/50"
                    >
                      <option value="ALL">All Severities</option>
                      <option value="CRITICAL">Critical</option>
                      <option value="HIGH">High</option>
                      <option value="MEDIUM">Medium</option>
                      <option value="LOW">Low</option>
                    </select>

                    <div className="flex items-center gap-1.5 text-xs text-slate-400 ml-2">
                      <span>Lifecycle:</span>
                    </div>
                    <select
                      value={lifecycleFilter}
                      onChange={(e) => setLifecycleFilter(e.target.value)}
                      className="bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-200 focus:outline-none focus:ring-2 focus:ring-blue-500/50"
                    >
                      <option value="ALL">All Lifecycle</option>
                      <option value="OPEN">Open</option>
                      <option value="RESOLVED">Resolved</option>
                      <option value="REGRESSED">Regressed</option>
                    </select>
                  </div>
                </div>

                {/* Findings List */}
                {isLoadingFindings ? (
                  <div className="space-y-4 pt-2">
                    <CardSkeleton />
                    <CardSkeleton />
                  </div>
                ) : filteredFindings.length === 0 ? (
                  <EmptyState
                    type="no-findings"
                    title={
                      findings.length === 0
                        ? "Zero Secret Exposures Detected"
                        : "No Findings Match Your Filter"
                    }
                    description={
                      findings.length === 0
                        ? "Both the current HEAD snapshot and reachable git commits are clean of active secrets."
                        : "Try adjusting your search query, severity, or lifecycle filters."
                    }
                    actionText={findings.length === 0 ? "Run Verification Scan" : undefined}
                    onAction={findings.length === 0 ? () => handleTriggerScan() : undefined}
                  />
                ) : (
                  <div className="space-y-4">
                    {filteredFindings.map((finding) => (
                      <FindingCard key={finding.id} finding={finding} />
                    ))}
                  </div>
                )}
              </div>
            ) : (
              /* Coverage & Audit Tab */
              <CoverageTab
                coverage={coverage}
                isLoading={isLoadingCoverage}
              />
            )}
          </div>
        )}
      </main>

      {/* Repository Selection & Branch Slot Modal (UC-002) */}
      <RepoSelectorModal
        isOpen={isRepoModalOpen}
        currentProject={project}
        onClose={() => setIsRepoModalOpen(false)}
        onSelectSuccess={handleSelectProjectSuccess}
      />
    </div>
  );
}
