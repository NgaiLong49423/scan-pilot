import React, { useState, useEffect, useRef } from 'react';
import { 
  Search, 
  CheckCircle2, 
  ArrowLeft,
  AlertCircle,
  RefreshCw
} from 'lucide-react';
import { Repository, Finding, HealthMetrics, UserProfile } from './types';
import { 
  fetchAvailableGitHubRepositories,
  fetchMonitoredProjects,
  selectRepositoryOnBackend,
  fetchFindingsForRepo, 
  fetchCoverageForRepo,
  fetchCurrentUser,
  loginWithGitHub,
  logoutUser,
  triggerRealScan,
  fetchScanJob,
  isValidUuid
} from './services/api';
import { Navbar } from './components/Navbar';
import { FleetDashboard } from './components/FleetDashboard';
import { HealthGauge } from './components/HealthGauge';
import { TrendSparkline } from './components/TrendSparkline';
import { MetricsGrid } from './components/MetricsGrid';
import { FindingCard } from './components/FindingCard';
import { RepoSelectModal } from './components/RepoSelectModal';
import { HeroLanding } from './components/HeroLanding';
import { ScanProgressStepper } from './components/ScanProgressStepper';
import { CoverageAuditView } from './components/CoverageAuditView';
import { LiveScanTerminal, ScanLogEntry } from './components/LiveScanTerminal';
import { CoverageWarningBanner } from './components/CoverageWarningBanner';

export default function App() {
  const [currentView, setCurrentView] = useState<'landing' | 'fleet' | 'dashboard'>('landing');
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  const [activeNavTab, setActiveNavTab] = useState<'findings' | 'coverage'>('findings');
  
  // Available repos from GitHub App vs Explicitly Monitored repos in Scan Pilot
  const [availableRepos, setAvailableRepos] = useState<Repository[]>([]);
  const [monitoredRepos, setMonitoredRepos] = useState<Repository[]>([]);
  const [isBackendUnavailable, setIsBackendUnavailable] = useState<boolean>(false);
  
  const [selectedRepo, setSelectedRepo] = useState<Repository | null>(null);
  const [findings, setFindings] = useState<Finding[]>([]);
  const [coverageData, setCoverageData] = useState<any | null>(null);
  const [metrics, setMetrics] = useState<HealthMetrics | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [activeStage, setActiveStage] = useState<string | null>(null);
  const [scanDurationStr, setScanDurationStr] = useState<string | null>(null);
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [severityFilter, setSeverityFilter] = useState<'ALL' | 'CRITICAL' | 'HIGH' | 'RESOLVED'>('ALL');
  const [searchQuery, setSearchQuery] = useState('');
  const [scanError, setScanError] = useState<string | null>(null);

  const pollTimerRef = useRef<NodeJS.Timeout | number | null>(null);

  // Live Terminal Feed States
  const [isTerminalOpen, setIsTerminalOpen] = useState(false);
  const [scanLogs, setScanLogs] = useState<ScanLogEntry[]>([]);
  const [currentInspectingFile, setCurrentInspectingFile] = useState<string>('');
  const [liveScannedCount, setLiveScannedCount] = useState<number>(0);
  const [liveLeaksCount, setLiveLeaksCount] = useState<number>(0);

  // Sync monitored/available repositories directly from PostgreSQL
  const loadData = async () => {
    // 1. Check active user session
    try {
      const user = await fetchCurrentUser();
      if (user) {
        setCurrentUser(user);
        setCurrentView('fleet');
      }
    } catch (_e) {
      // Ignore error
    }

    // 2. Load available GitHub repos and explicitly monitored repos in parallel
    const [allGithubRepos, dbMonitored] = await Promise.all([
      fetchAvailableGitHubRepositories(),
      fetchMonitoredProjects(),
    ]);

    setAvailableRepos(allGithubRepos);

    // If DB returned monitored projects, strictly replace state (no local storage cache)
    if (dbMonitored === null) {
      setIsBackendUnavailable(true);
      setMonitoredRepos([]);
    } else {
      setIsBackendUnavailable(false);
      if (dbMonitored.length === 0) {
        setMonitoredRepos([]);
      } else {
        const hydrated = await Promise.all(
          dbMonitored.map(async (repo) => {
            if (repo.dbRepositoryId && isValidUuid(repo.dbRepositoryId)) {
              const [realFindings, realCoverage] = await Promise.all([
                fetchFindingsForRepo(repo.dbRepositoryId),
                fetchCoverageForRepo(repo.dbRepositoryId),
              ]);
              const isScanned = Boolean(realCoverage != null || (realFindings && realFindings.length > 0));
              const openCount = realFindings ? realFindings.filter(f => f.status === 'OPEN').length : 0;
              const isIncomplete = realCoverage?.coverageImpact === 'INCOMPLETE';
              const healthScore: number | null = !isScanned
                ? 0
                : isIncomplete
                ? null
                : Math.max(0, 100 - openCount * 15);
              return {
                ...repo,
                isScanned,
                lastScanned: isScanned ? 'Audited' : null,
                findingCount: openCount,
                healthScore,
              };
            }
            return repo;
          })
        );

        setMonitoredRepos(hydrated);
      }
    }
  };

  const stopPolling = () => {
    if (pollTimerRef.current) {
      clearInterval(pollTimerRef.current);
      pollTimerRef.current = null;
    }
  };

  useEffect(() => {
    loadData();
    return () => {
      stopPolling();
    };
  }, []);

  useEffect(() => {
    return () => {
      stopPolling();
    };
  }, [selectedRepo?.id]);

  // Helper to append a log line to live terminal
  const appendLog = (level: ScanLogEntry['level'], message: string, file?: string) => {
    const timeStr = new Date().toLocaleTimeString();
    const entry: ScanLogEntry = {
      id: `${Date.now()}-${Math.random().toString(36).substr(2, 6)}`,
      timestamp: timeStr,
      level,
      message,
      file,
    };
    setScanLogs((prev) => [...prev, entry]);
    if (file) {
      setCurrentInspectingFile(file);
    }
  };

  // Handle importing a repository into Scan Pilot monitoring
  const handleImportRepo = async (repo: Repository) => {
    setScanError(null);
    const dbRepoId = await selectRepositoryOnBackend(repo);
    if (!dbRepoId || !isValidUuid(dbRepoId)) {
      setScanError(`Failed to onboard repository ${repo.name}. Please try again.`);
      setIsImportModalOpen(false);
      return;
    }

    const newMonitoredRepo: Repository = {
      ...repo,
      id: dbRepoId,
      dbRepositoryId: dbRepoId,
      isScanned: false,
      lastScanned: null,
      findingCount: 0,
      healthScore: 0,
    };

    setMonitoredRepos((prev) => {
      const exists = prev.some((r) => r.dbRepositoryId === dbRepoId || r.name === repo.name);
      if (!exists) {
        return [newMonitoredRepo, ...prev];
      }
      return prev;
    });

    setIsImportModalOpen(false);
  };

  // Handle selecting a repository to inspect deep posture
  const handleSelectRepo = async (repo: Repository) => {
    // 1. Reset state completely to guarantee data isolation between repositories
    stopPolling();
    setIsScanning(false);
    setActiveStage(null);
    setScanDurationStr(null);
    setFindings([]);
    setCoverageData(null);
    setMetrics(null);
    setScanLogs([]);
    setLiveScannedCount(0);
    setLiveLeaksCount(0);
    setCurrentInspectingFile('');
    setScanError(null);

    let dbRepoId = repo.dbRepositoryId;
    if (!dbRepoId || !isValidUuid(dbRepoId)) {
      dbRepoId = (await selectRepositoryOnBackend(repo)) || undefined;
    }

    if (!dbRepoId || !isValidUuid(dbRepoId)) {
      setScanError(`Repository '${repo.name}' identity is not verified in database.`);
      setSelectedRepo(null);
      setCurrentView('fleet');
      return;
    }

    const updatedRepo: Repository = {
      ...repo,
      id: dbRepoId,
      dbRepositoryId: dbRepoId,
    };
    setSelectedRepo(updatedRepo);

    // Fetch real findings & coverage from PostgreSQL
    const [realFindings, realCoverage] = await Promise.all([
      fetchFindingsForRepo(dbRepoId),
      fetchCoverageForRepo(dbRepoId),
    ]);

    const isActuallyScanned = Boolean(realCoverage != null || (realFindings && realFindings.length > 0));
    setFindings(realFindings || []);
    setCoverageData(realCoverage);

    if (isActuallyScanned) {
      const criticalCount = realFindings ? realFindings.filter(f => f.severity === 'CRITICAL' && f.status === 'OPEN').length : 0;
      const highCount = realFindings ? realFindings.filter(f => f.severity === 'HIGH' && f.status === 'OPEN').length : 0;
      const mediumCount = realFindings ? realFindings.filter(f => f.severity === 'MEDIUM' && f.status === 'OPEN').length : 0;
      const openCount = realFindings ? realFindings.filter(f => f.status === 'OPEN').length : 0;
      const resolvedCount = realFindings ? realFindings.filter(f => f.status === 'RESOLVED').length : 0;
      const scannedFiles = realCoverage?.scannedFiles || 0;
      const totalFiles = realCoverage?.totalFiles || scannedFiles;
      const skippedFiles = realCoverage?.skippedFiles || (totalFiles > scannedFiles ? totalFiles - scannedFiles : 0);

      // Exact Formula: Score = max(0, 100 - (Critical * 15 + High * 8 + Medium * 4))
      const totalDeductions = criticalCount * 15 + highCount * 8 + mediumCount * 4;
      const isIncomplete = realCoverage?.coverageImpact === 'INCOMPLETE';
      const healthScore: number | null = isIncomplete ? null : Math.max(0, 100 - totalDeductions);

      const grade = isIncomplete
        ? 'Incomplete Coverage (Limits Reached)'
        : healthScore !== null && healthScore >= 90
        ? 'No open findings in this completed scan'
        : healthScore !== null && healthScore >= 70
        ? 'Grade B (Moderate Risk)'
        : 'Action Required (Critical Risk)';

      setMetrics({
        healthScore,
        grade,
        scannedFilesCount: scannedFiles,
        totalFilesCount: totalFiles,
        skippedFilesCount: skippedFiles,
        openLeaksCount: openCount,
        resolvedLeaksCount: resolvedCount,
        aiFixReadyCount: openCount,
        mttrMinutes: 0,
        trendData: [],
        reasonCode: realCoverage?.reasonCode,
        limitHitValue: realCoverage?.limitHitValue,
        isCoverageIncomplete: isIncomplete,
      });

      setSelectedRepo((prev) =>
        prev
          ? {
              ...prev,
              isScanned: true,
              lastScanned: 'Audited',
              findingCount: openCount,
              healthScore,
            }
          : null
      );

      setMonitoredRepos((prev) =>
        prev.map((r) =>
          r.dbRepositoryId === dbRepoId
            ? {
                ...r,
                isScanned: true,
                lastScanned: 'Audited',
                findingCount: openCount,
                healthScore,
              }
            : r
        )
      );
    } else {
      setMetrics({
        healthScore: 0,
        grade: 'Not Scanned Yet',
        scannedFilesCount: 0,
        openLeaksCount: 0,
        resolvedLeaksCount: 0,
        aiFixReadyCount: 0,
        mttrMinutes: 0,
        trendData: [],
      });

      setSelectedRepo((prev) =>
        prev
          ? {
              ...prev,
              isScanned: false,
              lastScanned: null,
              findingCount: 0,
              healthScore: 0,
            }
          : null
      );
    }

    setCurrentView('dashboard');
  };

  // Handle trigger real repository scan on Backend with async execution and live stage polling
  const handleTriggerRescan = async () => {
    if (!selectedRepo) return;
    stopPolling();
    setIsScanning(true);
    setActiveStage('QUEUED');
    setScanDurationStr(null);
    setIsTerminalOpen(true);
    setScanLogs([]);
    setLiveScannedCount(0);
    setLiveLeaksCount(0);
    setCurrentInspectingFile('');
    setScanError(null);

    appendLog('INIT', `🚀 Initializing scan request for repository ${selectedRepo.name} (${selectedRepo.branch})...`);

    try {
      let dbRepoId = selectedRepo.dbRepositoryId;
      if (!dbRepoId || !isValidUuid(dbRepoId)) {
        dbRepoId = (await selectRepositoryOnBackend(selectedRepo)) || undefined;
        if (dbRepoId && isValidUuid(dbRepoId)) {
          setSelectedRepo((prev) => prev ? { ...prev, dbRepositoryId: dbRepoId, id: dbRepoId } : null);
        }
      }

      if (!dbRepoId || !isValidUuid(dbRepoId)) {
        const errorMsg = 'Repository UUID is missing or invalid (fail-closed)';
        appendLog('ALERT', `❌ Scan pipeline failed: ${errorMsg}`);
        setScanError(errorMsg);
        setIsScanning(false);
        setActiveStage(null);
        setFindings([]);
        setCoverageData(null);
        setMetrics(null);
        setSelectedRepo((prev) =>
          prev
            ? {
                ...prev,
                isScanned: false,
                lastScanned: 'Failed',
                findingCount: 0,
                healthScore: 0,
              }
            : null
        );
        return;
      }

      const scanResult = await triggerRealScan(dbRepoId, selectedRepo.branch);

      if (!scanResult.success || !scanResult.jobId) {
        const errorMsg = scanResult.message || 'Scan trigger failed';
        appendLog('ALERT', `❌ Scan pipeline failed: ${errorMsg}`);
        setScanError(errorMsg);
        setIsScanning(false);
        setActiveStage(null);
        // Fail-closed: clear any stale findings/coverage, do NOT display previous scan data
        setFindings([]);
        setCoverageData(null);
        setMetrics(null);
        setSelectedRepo((prev) =>
          prev
            ? {
                ...prev,
                isScanned: false,
                lastScanned: 'Failed',
                findingCount: 0,
                healthScore: 0,
              }
            : null
        );
        return;
      }

      const jobId = scanResult.jobId;
      const initialStage = scanResult.stage || 'QUEUED';
      setActiveStage(initialStage);
      appendLog('INFO', `Scan job enqueued (ID: ${jobId}). Polling real-time worker stages...`);

      let prevStage = initialStage;
      let consecutiveErrors = 0;

      // Start polling backend job every 1.5 seconds
      pollTimerRef.current = setInterval(async () => {
        try {
          const result = await fetchScanJob(jobId);
          if (!result.success) {
            consecutiveErrors++;
            if (result.isTerminal || consecutiveErrors >= 5) {
              stopPolling();
              setIsScanning(false);
              setActiveStage('FAILED');
              const errorMsg = result.message || (result.isTerminal ? 'Terminal scan job error' : 'Scan polling timed out after consecutive failures');
              appendLog('ALERT', `❌ Scan pipeline failed: ${errorMsg}`);
              setScanError(errorMsg);
              setFindings([]);
              setCoverageData(null);
              setMetrics(null);
              setSelectedRepo((prev) =>
                prev
                  ? {
                      ...prev,
                      isScanned: false,
                      lastScanned: 'Failed',
                      findingCount: 0,
                      healthScore: 0,
                    }
                  : null
              );
            }
            return;
          }

          consecutiveErrors = 0;
          const job = result.job;
          if (!job) return;

          if (job.stage && job.stage !== prevStage) {
            prevStage = job.stage;
            setActiveStage(job.stage);
            appendLog('INFO', `Stage transition: ${job.stage}`);
          }

          if (job.durationMs) {
            setScanDurationStr(`${(job.durationMs / 1000).toFixed(1)}s`);
          }

          if (job.status === 'COMPLETED') {
            stopPolling();
            setIsScanning(false);
            setActiveStage('COMPLETED');
            const finalDuration = job.durationMs ? `${(job.durationMs / 1000).toFixed(1)}s` : null;
            setScanDurationStr(finalDuration);

            // Fetch real findings and coverage summary from DB ONCE upon completion
            const [realFindings, realCoverage] = await Promise.all([
              fetchFindingsForRepo(dbRepoId),
              fetchCoverageForRepo(dbRepoId),
            ]);

            const realFindingsList = realFindings || [];
            setFindings(realFindingsList);
            setCoverageData(realCoverage);

            const criticalCount = realFindingsList.filter(f => f.severity === 'CRITICAL' && f.status === 'OPEN').length;
            const highCount = realFindingsList.filter(f => f.severity === 'HIGH' && f.status === 'OPEN').length;
            const mediumCount = realFindingsList.filter(f => f.severity === 'MEDIUM' && f.status === 'OPEN').length;
            const openCount = realFindingsList.filter(f => f.status === 'OPEN').length;
            const resolvedCount = realFindingsList.filter(f => f.status === 'RESOLVED').length;
            const scannedFiles = realCoverage?.scannedFiles || 0;
            const totalFiles = realCoverage?.totalFiles || scannedFiles;
            const skippedFiles = realCoverage?.skippedFiles || (totalFiles > scannedFiles ? totalFiles - scannedFiles : 0);

            setLiveScannedCount(scannedFiles);
            setLiveLeaksCount(openCount);

            // Stream real alerts for findings
            if (realFindingsList.length > 0) {
              realFindingsList.slice(0, 3).forEach((f) => {
                appendLog('ALERT', `⚠️ Secret detected in ${f.filePath}:${f.lineNumber} [${f.ruleId}: ${f.rawSecretMasked}]`, f.filePath);
              });
              if (realFindingsList.length > 3) {
                appendLog('ALERT', `⚠️ +${realFindingsList.length - 3} additional secret exposures recorded in database.`);
              }
            }

            // Exact Formula: Score = max(0, 100 - (Critical * 15 + High * 8 + Medium * 4))
            const totalDeductions = criticalCount * 15 + highCount * 8 + mediumCount * 4;
            const isIncomplete = realCoverage?.coverageImpact === 'INCOMPLETE';
            const healthScore: number | null = isIncomplete ? null : Math.max(0, 100 - totalDeductions);

            const grade = isIncomplete
              ? 'Incomplete Coverage (Limits Reached)'
              : healthScore !== null && healthScore >= 90
              ? 'No open findings in this completed scan'
              : healthScore !== null && healthScore >= 70
              ? 'Grade B (Moderate Risk)'
              : 'Action Required (Critical Risk)';

            setMetrics({
              healthScore,
              grade,
              scannedFilesCount: scannedFiles,
              totalFilesCount: totalFiles,
              skippedFilesCount: skippedFiles,
              openLeaksCount: openCount,
              resolvedLeaksCount: resolvedCount,
              aiFixReadyCount: openCount,
              mttrMinutes: 0,
              trendData: [],
              reasonCode: realCoverage?.reasonCode,
              limitHitValue: realCoverage?.limitHitValue,
              isCoverageIncomplete: isIncomplete,
            });

            setSelectedRepo((prev) =>
              prev
                ? {
                    ...prev,
                    isScanned: true,
                    lastScanned: 'Just now',
                    findingCount: openCount,
                    healthScore,
                  }
                : null
            );

            setMonitoredRepos((prev) =>
              prev.map((r) =>
                r.dbRepositoryId === dbRepoId
                  ? {
                      ...r,
                      isScanned: true,
                      lastScanned: 'Just now',
                      findingCount: openCount,
                      healthScore,
                    }
                  : r
              )
            );

            const skippedCount = totalFiles - scannedFiles;
            if (skippedCount > 0) {
              appendLog('INFO', `ℹ️ File Eligibility (FR-031): ${scannedFiles} text files analyzed • ${skippedCount} non-text/binary files skipped (Reason: UNSUPPORTED_BINARY_FILE / UNSUPPORTED_BINARY_DOCUMENT).`);
            }

            appendLog('SUCCESS', `✅ Scan completed successfully (${scannedFiles}/${totalFiles} files verified, ${openCount} open leaks). Sandbox purged.`);
          } else if (job.status === 'FAILED') {
            stopPolling();
            setIsScanning(false);
            setActiveStage('FAILED');
            const errorMsg = job.errorMessage || 'Scan execution failed';
            appendLog('ALERT', `❌ Scan pipeline failed: ${errorMsg}`);
            setScanError(errorMsg);
            setFindings([]);
            setCoverageData(null);
            setMetrics(null);
            setSelectedRepo((prev) =>
              prev
                ? {
                    ...prev,
                    isScanned: false,
                    lastScanned: 'Failed',
                    findingCount: 0,
                    healthScore: 0,
                  }
                : null
            );
          }
        } catch (_e) {
          consecutiveErrors++;
          if (consecutiveErrors >= 5) {
            stopPolling();
            setIsScanning(false);
            setActiveStage('FAILED');
            const errorMsg = 'Scan polling failed after consecutive errors';
            appendLog('ALERT', `❌ Scan pipeline failed: ${errorMsg}`);
            setScanError(errorMsg);
            setFindings([]);
            setCoverageData(null);
            setMetrics(null);
            setSelectedRepo((prev) =>
              prev
                ? {
                    ...prev,
                    isScanned: false,
                    lastScanned: 'Failed',
                    findingCount: 0,
                    healthScore: 0,
                  }
                : null
            );
          }
        }
      }, 1500);

    } catch (e: any) {
      stopPolling();
      setIsScanning(false);
      setActiveStage(null);
      const errorMsg = e.message || 'Execution failed';
      appendLog('ALERT', `❌ Scan pipeline encountered error: ${errorMsg}`);
      setScanError(errorMsg);
      setFindings([]);
      setCoverageData(null);
      setMetrics(null);
      setSelectedRepo((prev) =>
        prev
          ? {
              ...prev,
              isScanned: false,
              lastScanned: 'Failed',
              findingCount: 0,
              healthScore: 0,
            }
          : null
      );
    }
  };

  if (currentView === 'landing') {
    return (
      <HeroLanding
        onSignIn={loginWithGitHub}
        onExploreDemo={() => setCurrentView('fleet')}
      />
    );
  }

  const isCurrentRepoScanned = Boolean(selectedRepo?.isScanned);

  const filteredFindings = findings.filter((f) => {
    const matchesSearch = 
      f.ruleName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      f.filePath.toLowerCase().includes(searchQuery.toLowerCase()) ||
      f.ruleId.toLowerCase().includes(searchQuery.toLowerCase());

    if (!matchesSearch) return false;

    if (severityFilter === 'ALL') return true;
    if (severityFilter === 'RESOLVED') return f.status === 'RESOLVED';
    return f.severity === severityFilter && f.status === 'OPEN';
  });

  return (
    <div className="min-h-screen bg-[#0d1117] text-[#c9d1d9] font-sans flex flex-col justify-between selection:bg-[#1f6feb]/30">
      {/* Sticky Top Navbar */}
      <Navbar
        currentView={currentView}
        selectedRepo={selectedRepo}
        currentUser={currentUser}
        activeTab={activeNavTab}
        onTabChange={setActiveNavTab}
        isScanning={isScanning}
        onTriggerRescan={handleTriggerRescan}
        onNavigateHome={() => setCurrentView('landing')}
        onNavigateFleet={() => setCurrentView('fleet')}
      />

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 w-full space-y-6">
        {/* VIEW 1: Organization Multi-Repository Fleet Overview Hub */}
        {currentView === 'fleet' && (
          <>
            {isBackendUnavailable ? (
              <div className="p-8 sm:p-12 text-center bg-[#161b22] border border-[#30363d] rounded-2xl space-y-4 shadow-sm animate-in fade-in duration-200">
                <div className="w-12 h-12 rounded-full bg-[#d29922]/15 border border-[#d29922]/30 text-[#d29922] mx-auto flex items-center justify-center">
                  <AlertCircle className="w-6 h-6" />
                </div>
                <div className="space-y-1">
                  <h3 className="text-base sm:text-lg font-bold text-[#f0f6fc]">
                    Backend connection unavailable
                  </h3>
                  <p className="text-xs sm:text-sm text-[#8b949e] max-w-md mx-auto">
                    Backend connection unavailable. Please check backend status or retry.
                  </p>
                </div>
                <div className="pt-2">
                  <button
                    type="button"
                    onClick={() => loadData()}
                    className="inline-flex items-center gap-2 px-4 py-2 rounded-xl bg-[#1f6feb] hover:bg-[#388bfd] text-white text-xs font-semibold shadow-sm transition-all duration-150 active:scale-95"
                  >
                    <RefreshCw className="w-4 h-4" />
                    <span>Retry Connection</span>
                  </button>
                </div>
              </div>
            ) : (
              <FleetDashboard
                monitoredRepositories={monitoredRepos}
                currentUser={currentUser}
                onSelectRepo={handleSelectRepo}
                onOpenImportModal={() => setIsImportModalOpen(true)}
                onLogout={logoutUser}
              />
            )}
          </>
        )}

        {/* VIEW 2: Single Repository Deep Health & Remediation Dashboard */}
        {currentView === 'dashboard' && (
          <>
            {/* Dual-Stage Scan Stepper Progress Banner */}
            {selectedRepo && (
              <ScanProgressStepper
                isScanning={isScanning}
                branchName={selectedRepo.branch}
                stage={activeStage}
                isScanned={isCurrentRepoScanned}
                findingCount={findings.filter(f => f.status === 'OPEN').length}
                scanDuration={scanDurationStr}
                onToggleTerminal={() => setIsTerminalOpen(!isTerminalOpen)}
                isTerminalOpen={isTerminalOpen}
                scanError={scanError}
              />
            )}

            {/* Live Scan Radar & Terminal Console */}
            {selectedRepo && (
              <LiveScanTerminal
                isOpen={isTerminalOpen}
                isScanning={isScanning}
                logs={scanLogs}
                currentFile={currentInspectingFile}
                scannedCount={liveScannedCount || coverageData?.scannedFiles || 0}
                totalFiles={coverageData?.totalFiles || 0}
                leaksFoundCount={liveLeaksCount || findings.filter(f => f.status === 'OPEN').length}
                onClose={() => setIsTerminalOpen(false)}
              />
            )}

            {/* Incomplete Coverage Guardrail Warning Banner (FR-028, FR-031) */}
            {(coverageData?.coverageImpact === 'INCOMPLETE' || metrics?.isCoverageIncomplete) && (
              <CoverageWarningBanner
                reasonCode={coverageData?.reasonCode || metrics?.reasonCode}
                limitHitValue={coverageData?.limitHitValue || metrics?.limitHitValue}
                totalBytes={coverageData?.totalBytes}
                totalFiles={coverageData?.totalFiles}
                onViewCoverage={() => setActiveNavTab('coverage')}
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
                        <h1 className="text-xl sm:text-2xl font-bold text-[#f0f6fc] tracking-tight">
                          Security Posture & Findings
                        </h1>
                        <p className="text-xs sm:text-sm text-[#8b949e] mt-1">
                          {isCurrentRepoScanned 
                            ? `Audited ${selectedRepo?.name} (${selectedRepo?.branch}).`
                            : `Repository ${selectedRepo?.name} has not been audited yet. Click 'Trigger Rescan' to inspect.`}
                        </p>
                      </div>

                      <button
                        type="button"
                        onClick={() => setCurrentView('fleet')}
                        className="hidden sm:inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#161b22] border border-[#30363d] hover:bg-[#21262d] text-xs text-[#8b949e] hover:text-[#f0f6fc] transition-colors"
                      >
                        <ArrowLeft className="w-3.5 h-3.5" />
                        <span>All Repositories</span>
                      </button>
                    </div>

                    {/* Visual Analytics Bento Banner with strict equal height */}
                    <div className="grid grid-cols-1 md:grid-cols-12 gap-4 items-stretch">
                      <div className="md:col-span-4 lg:col-span-3 flex flex-col">
                        <HealthGauge 
                          score={metrics.healthScore} 
                          grade={metrics.grade} 
                          isScanned={isCurrentRepoScanned}
                        />
                      </div>
                      <div className="md:col-span-8 lg:col-span-4 flex flex-col">
                        <TrendSparkline 
                          data={metrics.trendData} 
                          isScanned={isCurrentRepoScanned}
                        />
                      </div>
                      <div className="md:col-span-12 lg:col-span-5 flex flex-col">
                        <MetricsGrid 
                          metrics={metrics} 
                          isScanned={isCurrentRepoScanned}
                          onViewCoverage={() => setActiveNavTab('coverage')}
                        />
                      </div>
                    </div>
                  </section>
                )}

                {/* Finding Stream Section */}
                <section className="space-y-4">
                  <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-[#30363d] pb-4">
                    {/* Filter Tabs by Severity */}
                    <div className="flex items-center gap-1.5 overflow-x-auto pb-1 sm:pb-0">
                      <button
                        type="button"
                        onClick={() => setSeverityFilter('ALL')}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-150 ${
                          severityFilter === 'ALL'
                            ? 'bg-[#1f6feb] text-white shadow-sm'
                            : 'bg-[#161b22] text-[#8b949e] hover:text-[#f0f6fc] hover:bg-[#21262d] border border-[#30363d]'
                        }`}
                      >
                        All Findings ({findings.length})
                      </button>

                      <button
                        type="button"
                        onClick={() => setSeverityFilter('CRITICAL')}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-150 ${
                          severityFilter === 'CRITICAL'
                            ? 'bg-[#da3633] text-white shadow-sm'
                            : 'bg-[#161b22] text-[#8b949e] hover:text-[#f0f6fc] hover:bg-[#21262d] border border-[#30363d]'
                        }`}
                      >
                        Critical ({findings.filter((f) => f.severity === 'CRITICAL' && f.status === 'OPEN').length})
                      </button>

                      <button
                        type="button"
                        onClick={() => setSeverityFilter('HIGH')}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-150 ${
                          severityFilter === 'HIGH'
                            ? 'bg-[#d29922] text-white shadow-sm'
                            : 'bg-[#161b22] text-[#8b949e] hover:text-[#f0f6fc] hover:bg-[#21262d] border border-[#30363d]'
                        }`}
                      >
                        High ({findings.filter((f) => f.severity === 'HIGH' && f.status === 'OPEN').length})
                      </button>

                      <button
                        type="button"
                        onClick={() => setSeverityFilter('RESOLVED')}
                        className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all duration-150 ${
                          severityFilter === 'RESOLVED'
                            ? 'bg-[#238636] text-white shadow-sm'
                            : 'bg-[#161b22] text-[#8b949e] hover:text-[#f0f6fc] hover:bg-[#21262d] border border-[#30363d]'
                        }`}
                      >
                        Resolved ({findings.filter((f) => f.status === 'RESOLVED').length})
                      </button>
                    </div>

                    {/* Search Input */}
                    <div className="relative w-full sm:w-64">
                      <Search className="w-4 h-4 text-[#8b949e] absolute left-3 top-1/2 -translate-y-1/2" />
                      <input
                        type="text"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        placeholder="Search rule, path..."
                        className="w-full bg-[#161b22] border border-[#30363d] rounded-lg py-1.5 pl-9 pr-3 text-xs text-[#f0f6fc] placeholder:text-[#8b949e] focus:outline-none focus:ring-2 focus:ring-[#1f6feb]/50"
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
                        />
                      ))
                    ) : (
                      <div className="p-12 text-center bg-[#161b22] border border-[#30363d] rounded-2xl space-y-3">
                        <div className={`w-12 h-12 rounded-full mx-auto flex items-center justify-center border ${
                          isCurrentRepoScanned 
                            ? 'bg-[#238636]/15 border-[#238636]/30 text-[#3fb950]' 
                            : 'bg-[#21262d] border-[#30363d] text-[#8b949e]'
                        }`}>
                          {isCurrentRepoScanned ? (
                            <CheckCircle2 className="w-6 h-6" />
                          ) : (
                            <AlertCircle className="w-6 h-6" />
                          )}
                        </div>
                        <h3 className="text-base font-semibold text-[#f0f6fc]">
                          {isCurrentRepoScanned 
                            ? 'No open findings in this completed scan'
                            : 'No Scan Data Available Yet'}
                        </h3>
                        <p className="text-xs text-[#8b949e] max-w-sm mx-auto">
                          {isCurrentRepoScanned
                            ? 'No open secret exposures were detected in the audited files for this scan snapshot.'
                            : 'Click "Trigger Rescan" to download snapshot and run the Gitleaks + AST analysis pipeline.'}
                        </p>
                      </div>
                    )}
                  </div>
                </section>
              </div>
            )}

            {/* Tab 2: Coverage & Audit View */}
            {activeNavTab === 'coverage' && selectedRepo && (
              <CoverageAuditView repo={selectedRepo} coverageData={coverageData} />
            )}
          </>
        )}
      </main>

      {/* Footer */}
      <footer className="w-full border-t border-[#30363d] bg-[#010409] py-6 text-xs text-[#8b949e] mt-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <span className="font-semibold text-[#c9d1d9]">Scan Pilot Security Engine</span>
            <span>•</span>
            <span>Spring Boot 3 + PostgreSQL + Gitleaks</span>
          </div>

          <div className="flex items-center gap-4 text-[11px]">
            <span>SP-CONFIG-001 Verified</span>
            <span>•</span>
            <span>Zero Raw Secret Policy</span>
          </div>
        </div>
      </footer>

      {/* Import Repository Modal (Only displays unmonitored repositories) */}
      <RepoSelectModal
        isOpen={isImportModalOpen}
        repositories={availableRepos.filter(
          (avail) =>
            !monitoredRepos.some(
              (m) =>
                m.name.toLowerCase() === avail.name.toLowerCase() ||
                (avail.githubRepoId && m.githubRepoId === avail.githubRepoId)
            )
        )}
        onSelectRepo={handleImportRepo}
        onClose={() => setIsImportModalOpen(false)}
      />
    </div>
  );
}
