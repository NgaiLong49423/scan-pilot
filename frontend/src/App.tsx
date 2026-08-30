import React, { useState, useEffect, useRef } from 'react';
import {
  Search,
  CheckCircle2,
  ArrowLeft,
  AlertCircle,
  RefreshCw
} from 'lucide-react';
import { Repository, Finding, UserProfile, SecurityActionSummary } from './types';
import { 
  fetchAvailableGitHubRepositories,
  fetchAvailableGitHubRepositoriesResult,
  fetchInstallUrl,
  fetchMonitoredProjects,
  selectRepositoryOnBackend,
  fetchFindingsForRepo, 
  fetchCoverageForRepo,
  fetchCurrentUser,
  loginWithGitHub,
  logoutUser,
  triggerRealScan,
  fetchScanJob,
  fetchScanEvents,
  isValidUuid,
  CoverageSummaryDto
} from './services/api';
import { resolveRepositoryPosture } from './services/postureResolver';
import { shouldContinueTelemetryPolling } from './services/telemetryPolling';
import { Navbar } from './components/Navbar';
import { FleetDashboard } from './components/FleetDashboard';
import { SecurityActionSummaryCard } from './components/SecurityActionSummaryCard';
import { FindingCard } from './components/FindingCard';
import { RepoSelectModal } from './components/RepoSelectModal';
import { HeroLanding } from './components/HeroLanding';
import { ScanProgressStepper } from './components/ScanProgressStepper';
import { CoverageAuditView } from './components/CoverageAuditView';
import { LiveScanTerminal, ScanLogEntry, formatScanEventLog } from './components/LiveScanTerminal';
import { CoverageWarningBanner } from './components/CoverageWarningBanner';

export default function App() {
  const [currentView, setCurrentView] = useState<'landing' | 'fleet' | 'dashboard'>('landing');
  const [currentUser, setCurrentUser] = useState<UserProfile | null>(null);
  const [activeNavTab, setActiveNavTab] = useState<'findings' | 'coverage'>('findings');
  
  // Available repos from GitHub App vs Explicitly Monitored repos in Scan Pilot
  const [availableRepos, setAvailableRepos] = useState<Repository[]>([]);
  const [availableReposStatus, setAvailableReposStatus] = useState<'LOADING' | 'SUCCESS' | 'UNAUTHORIZED' | 'ERROR'>('LOADING');
  const [availableReposError, setAvailableReposError] = useState<string | null>(null);
  const [installUrl, setInstallUrl] = useState<string | null>(null);
  const [monitoredRepos, setMonitoredRepos] = useState<Repository[]>([]);
  const [isBackendUnavailable, setIsBackendUnavailable] = useState<boolean>(false);
  
  const [selectedRepo, setSelectedRepo] = useState<Repository | null>(null);
  const [findings, setFindings] = useState<Finding[]>([]);
  const [coverageData, setCoverageData] = useState<CoverageSummaryDto | null>(null);
  const [postureSummary, setPostureSummary] = useState<SecurityActionSummary | null>(null);
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
    setAvailableReposStatus('LOADING');
    setAvailableReposError(null);

    // 1. Check active user session
    try {
      const user = await fetchCurrentUser();
      if (user) {
        setCurrentUser(user);
        setCurrentView('fleet');
      } else {
        setCurrentUser(null);
      }
    } catch (_e) {
      setCurrentUser(null);
    }

    // 2. Load installUrl, available GitHub repos, and monitored projects in parallel
    const [installUrlData, reposResult, dbMonitored] = await Promise.all([
      fetchInstallUrl(),
      fetchAvailableGitHubRepositoriesResult(),
      fetchMonitoredProjects(),
    ]);

    setInstallUrl(installUrlData);

    if (reposResult.status === 'SUCCESS') {
      setAvailableRepos(reposResult.data);
      setAvailableReposStatus('SUCCESS');
    } else if (reposResult.status === 'UNAUTHORIZED') {
      setAvailableRepos([]);
      setAvailableReposStatus('UNAUTHORIZED');
    } else {
      setAvailableRepos([]);
      setAvailableReposStatus('ERROR');
      setAvailableReposError(reposResult.error);
    }

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
              const [findingsResult, coverageResult] = await Promise.all([
                fetchFindingsForRepo(repo.dbRepositoryId),
                fetchCoverageForRepo(repo.dbRepositoryId),
              ]);

              let jobResult = null;
              if (coverageResult.status === 'SUCCESS' && coverageResult.data.scanJobId) {
                jobResult = await fetchScanJob(coverageResult.data.scanJobId);
              }

              const summary = resolveRepositoryPosture(findingsResult, coverageResult, jobResult);
              const isAudited = summary.status === 'ACTION_REQUIRED' || summary.status === 'NO_OPEN_FINDINGS' || summary.status === 'COVERAGE_INCOMPLETE';
              const openCount = summary.severityCounts.total;

              return {
                ...repo,
                isScanned: isAudited,
                lastScanned: isAudited ? (summary.scanCompletedAt ? new Date(summary.scanCompletedAt).toLocaleDateString() : 'Audited') : null,
                findingCount: openCount,
                postureStatus: summary.status,
                severityCounts: summary.severityCounts,
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
      postureStatus: 'AWAITING_INITIAL_SCAN',
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
    setPostureSummary(null);
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
    const [findingsResult, coverageResult] = await Promise.all([
      fetchFindingsForRepo(dbRepoId),
      fetchCoverageForRepo(dbRepoId),
    ]);

    let jobResult = null;
    if (coverageResult.status === 'SUCCESS' && coverageResult.data.scanJobId) {
      jobResult = await fetchScanJob(coverageResult.data.scanJobId);
    }

    const summary = resolveRepositoryPosture(findingsResult, coverageResult, jobResult);
    setPostureSummary(summary);

    if (findingsResult.status === 'SUCCESS') {
      setFindings(findingsResult.data);
    } else {
      setFindings([]);
    }

    if (coverageResult.status === 'SUCCESS') {
      setCoverageData(coverageResult.data);
    } else {
      setCoverageData(null);
    }

    const isAudited = summary.status === 'ACTION_REQUIRED' || summary.status === 'NO_OPEN_FINDINGS' || summary.status === 'COVERAGE_INCOMPLETE';
    const openCount = summary.severityCounts.total;

    setSelectedRepo((prev) =>
      prev
        ? {
            ...prev,
            isScanned: isAudited,
            lastScanned: isAudited ? (summary.scanCompletedAt ? new Date(summary.scanCompletedAt).toLocaleDateString() : 'Audited') : null,
            findingCount: openCount,
            postureStatus: summary.status,
            severityCounts: summary.severityCounts,
          }
        : null
    );

    setMonitoredRepos((prev) =>
      prev.map((r) =>
        r.dbRepositoryId === dbRepoId
          ? {
              ...r,
              isScanned: isAudited,
              lastScanned: isAudited ? (summary.scanCompletedAt ? new Date(summary.scanCompletedAt).toLocaleDateString() : 'Audited') : null,
              findingCount: openCount,
              postureStatus: summary.status,
              severityCounts: summary.severityCounts,
            }
          : r
      )
    );

    setCurrentView('dashboard');
  };

  // Fresh retry handler for single repo view
  const handleRetryDetailEvidence = async () => {
    if (selectedRepo) {
      await handleSelectRepo(selectedRepo);
    }
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
        setPostureSummary(resolveRepositoryPosture({ status: 'ERROR', error: errorMsg }, { status: 'ERROR', error: errorMsg }, null));
        setSelectedRepo((prev) =>
          prev
            ? {
                ...prev,
                isScanned: false,
                lastScanned: 'Failed',
                findingCount: 0,
                postureStatus: 'SCAN_UNAVAILABLE',
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
        setPostureSummary(resolveRepositoryPosture({ status: 'ERROR', error: errorMsg }, { status: 'ERROR', error: errorMsg }, null));
        setSelectedRepo((prev) =>
          prev
            ? {
                ...prev,
                isScanned: false,
                lastScanned: 'Failed',
                findingCount: 0,
                postureStatus: 'SCAN_UNAVAILABLE',
              }
            : null
        );
        return;
      }

      const jobId = scanResult.jobId;
      const initialStage = scanResult.stage || 'QUEUED';
      setActiveStage(initialStage);

      let cursorSeq = 0;
      let consecutiveErrors = 0;
      const startTime = Date.now();

      // Start polling backend telemetry events every 1.0s (Issue #69, AC-05, AC-07)
      pollTimerRef.current = setInterval(async () => {
        try {
          // Live duration timer update
          const elapsedSec = ((Date.now() - startTime) / 1000).toFixed(1);
          setScanDurationStr(`${elapsedSec}s`);

          const eventResult = await fetchScanEvents(jobId, cursorSeq, 50);
          if (!eventResult.success || !eventResult.data) {
            consecutiveErrors++;
            if (eventResult.isTerminal || consecutiveErrors >= 5) {
              stopPolling();
              setIsScanning(false);
              setActiveStage('FAILED');
              const errorMsg = eventResult.message || (eventResult.isTerminal ? 'Terminal scan job error' : 'Scan polling timed out after consecutive failures');
              appendLog('ALERT', `❌ Scan pipeline failed: ${errorMsg}`);
              setScanError(errorMsg);
              setFindings([]);
              setCoverageData(null);
              setPostureSummary(resolveRepositoryPosture({ status: 'ERROR', error: errorMsg }, { status: 'ERROR', error: errorMsg }, null));
              setSelectedRepo((prev) =>
                prev
                  ? {
                      ...prev,
                      isScanned: false,
                      lastScanned: 'Failed',
                      findingCount: 0,
                      postureStatus: 'SCAN_UNAVAILABLE',
                    }
                  : null
              );
            }
            return;
          }

          consecutiveErrors = 0;
          const { status, stage, lastSequence, hasMore, events } = eventResult.data;

          if (stage) {
            setActiveStage(stage);
          }

          if (events && events.length > 0) {
            for (const event of events) {
              if (event.sequenceNumber > cursorSeq) {
                cursorSeq = event.sequenceNumber;
                const { level, message } = formatScanEventLog(event);
                appendLog(level, message);

                if (event.messageCode === 'FINDING_ALERT') {
                  setLiveLeaksCount((prev) => prev + 1);
                } else if (event.messageCode === 'FILES_CLASSIFIED' && event.payloadJson) {
                  try {
                    const payload = JSON.parse(event.payloadJson);
                    const eligible = payload.eligibleFiles || 0;
                    setLiveScannedCount(eligible);
                  } catch {}
                }
              }
            }
          }

          const shouldContinue = shouldContinueTelemetryPolling(status, hasMore, cursorSeq, lastSequence);
          if (!shouldContinue) {
            stopPolling();
            setIsScanning(false);

            if (status === 'COMPLETED') {
              setActiveStage('COMPLETED');
              appendLog('SUCCESS', '✅ Scan pipeline completed successfully. Persisting evidence.');

              // Fetch final persisted findings, coverage, and scan job status
              const [findingsResult, coverageResult, jobPoll] = await Promise.all([
                fetchFindingsForRepo(dbRepoId),
                fetchCoverageForRepo(dbRepoId),
                fetchScanJob(jobId),
              ]);

              const realFindingsList = findingsResult.status === 'SUCCESS' ? findingsResult.data : [];
              const realCoverage = coverageResult.status === 'SUCCESS' ? coverageResult.data : null;

              setFindings(realFindingsList);
              setCoverageData(realCoverage);

              const summary = resolveRepositoryPosture(findingsResult, coverageResult, jobPoll);
              setPostureSummary(summary);

              const openCount = summary.severityCounts.total;
              const scannedFiles = realCoverage?.scannedFiles || 0;

              setLiveScannedCount(scannedFiles);
              setLiveLeaksCount(openCount);

              const isAudited = summary.status === 'ACTION_REQUIRED' || summary.status === 'NO_OPEN_FINDINGS' || summary.status === 'COVERAGE_INCOMPLETE';

              setSelectedRepo((prev) =>
                prev
                  ? {
                      ...prev,
                      isScanned: isAudited,
                      lastScanned: isAudited ? 'Just now' : (summary.status === 'SCAN_UNAVAILABLE' ? 'Failed' : null),
                      findingCount: openCount,
                      postureStatus: summary.status,
                      severityCounts: summary.severityCounts,
                    }
                  : null
              );

              setMonitoredRepos((prev) =>
                prev.map((r) =>
                  r.dbRepositoryId === dbRepoId
                    ? {
                        ...r,
                        isScanned: isAudited,
                        lastScanned: isAudited ? 'Just now' : (summary.status === 'SCAN_UNAVAILABLE' ? 'Failed' : null),
                        findingCount: openCount,
                        postureStatus: summary.status,
                        severityCounts: summary.severityCounts,
                      }
                    : r
                )
              );
            } else if (status === 'FAILED') {
              setActiveStage('FAILED');
              const errorMsg = 'Scan execution failed';
              setScanError(errorMsg);
              setFindings([]);
              setCoverageData(null);
              setPostureSummary(resolveRepositoryPosture(
                { status: 'ERROR', error: errorMsg },
                { status: 'ERROR', error: errorMsg },
                { success: true, job: { id: jobId, repositoryId: dbRepoId, status: 'FAILED' } }
              ));
              setSelectedRepo((prev) =>
                prev
                  ? {
                      ...prev,
                      isScanned: false,
                      lastScanned: 'Failed',
                      findingCount: 0,
                      postureStatus: 'SCAN_UNAVAILABLE',
                    }
                  : null
              );
            }
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
            setPostureSummary(resolveRepositoryPosture({ status: 'ERROR', error: errorMsg }, { status: 'ERROR', error: errorMsg }, null));
            setSelectedRepo((prev) =>
              prev
                ? {
                    ...prev,
                    isScanned: false,
                    lastScanned: 'Failed',
                    findingCount: 0,
                    postureStatus: 'SCAN_UNAVAILABLE',
                  }
                : null
            );
          }
        }
      }, 1000);

    } catch (e: any) {
      stopPolling();
      setIsScanning(false);
      setActiveStage(null);
      const errorMsg = e.message || 'Execution failed';
      appendLog('ALERT', `❌ Scan pipeline encountered error: ${errorMsg}`);
      setScanError(errorMsg);
      setFindings([]);
      setCoverageData(null);
      setPostureSummary(resolveRepositoryPosture({ status: 'ERROR', error: errorMsg }, { status: 'ERROR', error: errorMsg }, null));
      setSelectedRepo((prev) =>
        prev
          ? {
              ...prev,
              isScanned: false,
              lastScanned: 'Failed',
              findingCount: 0,
              postureStatus: 'SCAN_UNAVAILABLE',
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
                onRetry={loadData}
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
                activeStage={activeStage}
                durationStr={scanDurationStr}
                onClose={() => setIsTerminalOpen(false)}
              />
            )}

            {/* Incomplete Coverage Guardrail Warning Banner (FR-028, FR-031) */}
            {coverageData?.coverageImpact === 'INCOMPLETE' && (
              <CoverageWarningBanner
                reasonCode={coverageData?.reasonCode}
                limitHitValue={coverageData?.limitHitValue}
                totalBytes={coverageData?.totalBytes}
                totalFiles={coverageData?.totalFiles}
                onViewCoverage={() => setActiveNavTab('coverage')}
              />
            )}

            {/* Tab 1: Findings & Remediation View */}
            {activeNavTab === 'findings' && (
              <div className="space-y-8 animate-in fade-in duration-200">
                {/* Top Overview Section: Verified Security Posture Summary Card */}
                {postureSummary && (
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

                    <SecurityActionSummaryCard
                      summary={postureSummary}
                      onRetry={handleRetryDetailEvidence}
                      onViewCoverage={() => setActiveNavTab('coverage')}
                    />
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
        status={availableReposStatus}
        errorMessage={availableReposError}
        availableRepos={availableRepos}
        monitoredRepos={monitoredRepos}
        installUrl={installUrl}
        onSelectRepo={handleImportRepo}
        onClose={() => setIsImportModalOpen(false)}
        onRetry={loadData}
        onSignIn={() => loginWithGitHub()}
      />
    </div>
  );
}
