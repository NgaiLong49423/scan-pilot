import React, { useState } from 'react';
import {
  FileCode,
  CheckCircle2,
  Clock,
  HardDrive,
  FileQuestion,
  FileArchive,
  ShieldCheck,
  AlertTriangle
} from 'lucide-react';
import { Repository } from '../types';
import { CoverageSummary, CoverageItem } from '../types/api';

interface CoverageAuditViewProps {
  repo: Repository;
  coverageData?: CoverageSummary | null;
}

export const CoverageAuditView: React.FC<CoverageAuditViewProps> = ({ repo, coverageData }) => {
  const [filterReason, setFilterReason] = useState<string>('ALL');

  // 1. Determine coverage status strictly from coverageImpact (or lack thereof)
  const rawImpact = coverageData?.coverageImpact ? String(coverageData.coverageImpact).toUpperCase() : null;
  const isComplete = rawImpact === 'COMPLETE';
  const isIncomplete = rawImpact === 'INCOMPLETE' || rawImpact === 'PARTIAL';
  const isAwaiting = !coverageData || !rawImpact;

  const totalFiles = coverageData?.totalFiles || 0;
  const scannedFiles = coverageData?.scannedFiles || 0;
  const skippedFiles = coverageData?.skippedFiles || 0;
  const binaryFiles = coverageData?.binaryFiles || skippedFiles;
  const textFiles = coverageData?.textFiles || scannedFiles;
  const reasonCode = (coverageData as any)?.reasonCode || (isIncomplete ? 'INCOMPLETE_COVERAGE' : 'GUARDRAIL_COMPLIANT');
  const limitHitValue = (coverageData as any)?.limitHitValue;

  const totalBytes = coverageData?.totalBytes
    ? (coverageData.totalBytes > 1024 * 1024
        ? `${(coverageData.totalBytes / (1024 * 1024)).toFixed(2)} MB`
        : `${(coverageData.totalBytes / 1024).toFixed(1)} KB`)
    : '0 KB';

  // Extract skipped items directly from verified backend coverage payload (no synthetic mocks)
  const rawItems: CoverageItem[] = coverageData?.items || coverageData?.skippedItems || [];
  const skippedItems = rawItems.filter((it) => it.status === 'SKIPPED');

  const filteredSkippedList = skippedItems.filter((it) => {
    if (filterReason === 'ALL') return true;
    return it.reasonCode === filterReason;
  });

  return (
    <div className="space-y-6 animate-in fade-in duration-200" data-testid="coverage-audit-view">
      {/* Incomplete Coverage Warning Banner */}
      {isIncomplete && (
        <div 
          data-testid="incomplete-coverage-warning"
          className="p-4 sm:p-5 bg-[#1f1606] border border-[#d29922]/40 rounded-2xl flex items-start gap-3.5 text-[#f0f6fc] shadow-sm"
        >
          <div className="p-2 bg-[#d29922]/20 border border-[#d29922]/40 rounded-xl text-[#d29922] shrink-0 mt-0.5 sm:mt-0">
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div className="space-y-1">
            <div className="flex items-center gap-2 flex-wrap">
              <h4 className="text-sm font-bold text-[#f0f6fc] tracking-tight">Coverage Incomplete</h4>
              <span className="px-2 py-0.5 text-[10px] font-mono font-semibold bg-[#d29922]/20 text-[#d29922] border border-[#d29922]/40 rounded-md uppercase">
                INCOMPLETE COVERAGE
              </span>
            </div>
            <p className="text-xs text-[#c9d1d9] leading-relaxed">
              File evaluation for repository <span className="text-[#f0f6fc] font-mono">{repo.name}</span> did not achieve full coverage. Scan stages cannot be marked verified complete, and AI remediation readiness cannot be claimed until complete coverage is verified.
            </p>
          </div>
        </div>
      )}

      {/* Overview 3-Card Bento Row */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Stage 1 Coverage */}
        <div className="p-5 bg-[#161b22] border border-[#30363d] rounded-2xl space-y-2">
          <div className="flex items-center justify-between text-[#8b949e]">
            <span className="text-xs font-semibold uppercase tracking-wider">Eligible Code Coverage</span>
            <FileCode className="w-4 h-4 text-[#58a6ff]" />
          </div>
          <div className="text-2xl font-bold text-[#f0f6fc] tabular-nums">
            {!isAwaiting ? `${scannedFiles} / ${totalFiles} Files` : '— Files'}
          </div>
          <p className="text-xs text-[#8b949e]">
            {isComplete
              ? `${scannedFiles} text files analyzed • ${skippedFiles} skipped per guardrail policy.`
              : isIncomplete
              ? `${scannedFiles} text files evaluated (Coverage Incomplete).`
              : 'Snapshot inspection pending initial scan run.'}
          </p>
          <div className="w-full bg-[#21262d] h-1.5 rounded-full overflow-hidden mt-3">
            <div 
              className={`h-full rounded-full ${
                isComplete 
                  ? 'bg-[#1f6feb] w-full' 
                  : isIncomplete 
                  ? 'bg-[#d29922] w-full' 
                  : 'bg-[#30363d] w-0'
              }`} 
            />
          </div>
        </div>

        {/* Skipped Policy Breakdown */}
        <div className="p-5 bg-[#161b22] border border-[#30363d] rounded-2xl space-y-2">
          <div className="flex items-center justify-between text-[#8b949e]">
            <span className="text-xs font-semibold uppercase tracking-wider">Policy Exclusions</span>
            <FileQuestion className="w-4 h-4 text-[#d29922]" />
          </div>
          <div className="text-2xl font-bold text-[#d29922] tabular-nums">
            {!isAwaiting ? `${skippedFiles} Excluded` : '— Excluded'}
          </div>
          <p className="text-xs text-[#8b949e]">
            {!isAwaiting
              ? `${binaryFiles} non-text binaries or guardrail limits (${reasonCode}${limitHitValue ? `: ${limitHitValue}` : ''}).`
              : 'File classification awaiting scan execution.'}
          </p>
          <div className="w-full bg-[#21262d] h-1.5 rounded-full overflow-hidden mt-3">
            <div className={`h-full rounded-full ${skippedFiles > 0 ? 'bg-[#d29922] w-full' : 'bg-[#30363d] w-0'}`} />
          </div>
        </div>

        {/* Total Scanned Footprint */}
        <div className="p-5 bg-[#161b22] border border-[#30363d] rounded-2xl space-y-2">
          <div className="flex items-center justify-between text-[#8b949e]">
            <span className="text-xs font-semibold uppercase tracking-wider">Total Scanned Size</span>
            <HardDrive className="w-4 h-4 text-[#3fb950]" />
          </div>
          <div className="text-2xl font-bold text-[#3fb950] tabular-nums">
            {!isAwaiting ? totalBytes : '— KB'}
          </div>
          <p className="text-xs text-[#8b949e]">
            {isComplete
              ? `${textFiles} eligible source files in repository workspace.`
              : isIncomplete
              ? 'Partial volume measured before coverage completion.'
              : 'Archive bytes awaiting shallow checkout.'}
          </p>
          <div className="w-full bg-[#21262d] h-1.5 rounded-full overflow-hidden mt-3">
            <div 
              className={`h-full rounded-full ${
                isComplete 
                  ? 'bg-[#238636] w-full' 
                  : isIncomplete 
                  ? 'bg-[#d29922] w-full' 
                  : 'bg-[#30363d] w-0'
              }`} 
            />
          </div>
        </div>
      </div>

      {/* 3-Stage Pipeline Verification Table */}
      <div className="bg-[#161b22] border border-[#30363d] rounded-2xl overflow-hidden shadow-sm space-y-4 p-5">
        <div>
          <h3 className="text-base font-bold text-[#f0f6fc] flex items-center gap-2">
            <ShieldCheck className="w-4 h-4 text-[#3fb950]" />
            <span>Multi-Stage Pipeline Execution Audit</span>
          </h3>
          <p className="text-xs text-[#8b949e] mt-1">
            {isComplete 
              ? `Verified coverage evidence for repository ${repo.name} (${repo.branch}).`
              : isIncomplete 
              ? `Coverage incomplete for repository ${repo.name} (${repo.branch}) — stages unverified.`
              : `Verification status across sequential scan phases for ${repo.name} (${repo.branch}).`}
          </p>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#0d1117] text-[#8b949e] uppercase tracking-wider font-semibold border-b border-[#30363d]">
              <tr>
                <th className="px-5 py-3">Scan Phase</th>
                <th className="px-5 py-3">Engine / Target</th>
                <th className="px-5 py-3">Observed Metrics</th>
                <th className="px-5 py-3">Verification State</th>
                <th className="px-5 py-3">Security Guardrail</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#30363d] font-mono text-[#c9d1d9]">
              {/* Stage 1: Workspace Ingestion */}
              <tr className="hover:bg-[#21262d]/50 transition-colors">
                <td className="px-5 py-3.5 font-semibold text-[#f0f6fc]">Stage 1: Workspace Ingestion</td>
                <td className="px-5 py-3.5 text-[#8b949e]">
                  {isComplete 
                    ? `HEAD Commit (${scannedFiles} text files)` 
                    : isIncomplete 
                    ? `HEAD Commit (${scannedFiles} text files evaluated, incomplete)` 
                    : 'HEAD Commit (Pending)'}
                </td>
                <td className="px-5 py-3.5 text-[#8b949e]">
                  {!isAwaiting ? `${totalFiles} entries • ${totalBytes}` : '—'}
                </td>
                <td className="px-5 py-3.5">
                  {isComplete ? (
                    <span className="inline-flex items-center gap-1 text-[#3fb950] bg-[#238636]/15 px-2 py-0.5 rounded border border-[#238636]/30 text-[11px] font-sans font-medium">
                      <CheckCircle2 className="w-3 h-3" />
                      <span>Verified Complete</span>
                    </span>
                  ) : isIncomplete ? (
                    <span className="inline-flex items-center gap-1 text-[#d29922] bg-[#d29922]/15 px-2 py-0.5 rounded border border-[#d29922]/30 text-[11px] font-sans font-medium">
                      <AlertTriangle className="w-3 h-3" />
                      <span>Coverage Incomplete</span>
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 text-[#8b949e] bg-[#21262d] px-2 py-0.5 rounded border border-[#30363d] text-[11px] font-sans font-medium">
                      <Clock className="w-3 h-3" />
                      <span>Awaiting Coverage</span>
                    </span>
                  )}
                </td>
                <td className="px-5 py-3.5 text-[#8b949e]">Size & Zip-Bomb Filter</td>
              </tr>

              {/* Stage 2: Secret Scanning */}
              <tr className="hover:bg-[#21262d]/50 transition-colors">
                <td className="px-5 py-3.5 font-semibold text-[#f0f6fc]">Stage 2: Secret Scanning</td>
                <td className="px-5 py-3.5 text-[#8b949e]">Not available in coverage evidence</td>
                <td className="px-5 py-3.5 text-[#8b949e]">
                  {!isAwaiting ? `${scannedFiles} files scanned • ${skippedFiles} skipped` : '—'}
                </td>
                <td className="px-5 py-3.5">
                  {isComplete ? (
                    <span className="inline-flex items-center gap-1 text-[#8b949e] bg-[#21262d] px-2 py-0.5 rounded border border-[#30363d] text-[11px] font-sans font-medium">
                      <span>Not available in coverage evidence</span>
                    </span>
                  ) : isIncomplete ? (
                    <span className="inline-flex items-center gap-1 text-[#d29922] bg-[#d29922]/15 px-2 py-0.5 rounded border border-[#d29922]/30 text-[11px] font-sans font-medium">
                      <AlertTriangle className="w-3 h-3" />
                      <span>Coverage Incomplete</span>
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 text-[#8b949e] bg-[#21262d] px-2 py-0.5 rounded border border-[#30363d] text-[11px] font-sans font-medium">
                      <Clock className="w-3 h-3" />
                      <span>Awaiting Coverage</span>
                    </span>
                  )}
                </td>
                <td className="px-5 py-3.5 text-[#8b949e]">Gitleaks Isolation Boundary</td>
              </tr>

              {/* Stage 3: Guided Remediation */}
              <tr className="hover:bg-[#21262d]/50 transition-colors">
                <td className="px-5 py-3.5 font-semibold text-[#f0f6fc]">Stage 3: Guided Remediation</td>
                <td className="px-5 py-3.5 text-[#8b949e]">Not available in coverage evidence</td>
                <td className="px-5 py-3.5 text-[#8b949e]">—</td>
                <td className="px-5 py-3.5">
                  {isComplete ? (
                    <span className="inline-flex items-center gap-1 text-[#8b949e] bg-[#21262d] px-2 py-0.5 rounded border border-[#30363d] text-[11px] font-sans font-medium">
                      <span>Not available in coverage evidence</span>
                    </span>
                  ) : isIncomplete ? (
                    <span className="inline-flex items-center gap-1 text-[#d29922] bg-[#d29922]/15 px-2 py-0.5 rounded border border-[#d29922]/30 text-[11px] font-sans font-medium">
                      <AlertTriangle className="w-3 h-3" />
                      <span>Coverage Incomplete</span>
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1 text-[#8b949e] bg-[#21262d] px-2 py-0.5 rounded border border-[#30363d] text-[11px] font-sans font-medium">
                      <Clock className="w-3 h-3" />
                      <span>Awaiting Coverage</span>
                    </span>
                  )}
                </td>
                <td className="px-5 py-3.5 text-[#8b949e]">Signed Preview Token</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Skipped Artifacts & Policy Audit Table (FR-031 / FR-037 Disclosures) */}
      <div className="bg-[#161b22] border border-[#30363d] rounded-2xl overflow-hidden shadow-sm space-y-4 p-5">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-[#30363d] pb-4">
          <div>
            <h3 className="text-base font-bold text-[#f0f6fc] flex items-center gap-2">
              <FileQuestion className="w-4 h-4 text-[#d29922]" />
              <span>Excluded Artifacts & Eligibility Disclosure (FR-031 / FR-037)</span>
            </h3>
            <p className="text-xs text-[#8b949e] mt-1">
              Transparent report of all non-text binaries, media, and compilation artifacts safely bypassed during scanning.
            </p>
          </div>
        </div>

        {/* Skipped Items Table */}
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#0d1117] text-[#8b949e] uppercase tracking-wider font-semibold border-b border-[#30363d]">
              <tr>
                <th className="px-4 py-2.5">Artifact Target / Category</th>
                <th className="px-4 py-2.5">Classification</th>
                <th className="px-4 py-2.5">Size / Count</th>
                <th className="px-4 py-2.5">Skip Reason Code</th>
                <th className="px-4 py-2.5">Audit Detail</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#30363d] text-[#c9d1d9] font-mono">
              {filteredSkippedList.length > 0 ? (
                filteredSkippedList.map((item, idx) => (
                  <tr key={item.id || `${item.filePath}-${idx}`} className="hover:bg-[#21262d]/50 transition-colors">
                    <td className="px-4 py-3 font-semibold text-[#f0f6fc] flex items-center gap-2">
                      <FileArchive className="w-3.5 h-3.5 text-[#8b949e]" />
                      <span className="truncate max-w-[280px]">{item.filePath}</span>
                    </td>
                    <td className="px-4 py-3 text-[#8b949e]">{item.classification}</td>
                    <td className="px-4 py-3 text-[#8b949e]">
                      {item.sizeBytes ? `${(item.sizeBytes / 1024).toFixed(1)} KB` : '—'}
                    </td>
                    <td className="px-4 py-3">
                      <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] bg-[#d29922]/15 text-[#d29922] border border-[#d29922]/30">
                        {item.reasonCode}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-[#8b949e] text-[11px] font-sans max-w-xs truncate">
                      {item.details || item.impact || 'Excluded per guardrail policy'}
                    </td>
                  </tr>
                ))
              ) : !isAwaiting && skippedFiles > 0 ? (
                <tr className="hover:bg-[#21262d]/50 transition-colors">
                  <td className="px-4 py-3 font-semibold text-[#f0f6fc] flex items-center gap-2">
                    <FileArchive className="w-3.5 h-3.5 text-[#d29922]" />
                    <span>Non-text Binaries & Guardrail Exclusions</span>
                  </td>
                  <td className="px-4 py-3 text-[#8b949e]">BINARY / EXCLUDED</td>
                  <td className="px-4 py-3 text-[#8b949e]">{skippedFiles} files</td>
                  <td className="px-4 py-3">
                    <span className="inline-flex items-center px-2 py-0.5 rounded text-[10px] bg-[#d29922]/15 text-[#d29922] border border-[#d29922]/30">
                      {reasonCode}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-[#8b949e] text-[11px] font-sans">
                    {skippedFiles} files excluded per guardrail policy. Detailed file items tracked in execution telemetry.
                  </td>
                </tr>
              ) : (
                <tr>
                  <td colSpan={5} className="px-4 py-6 text-center text-[#8b949e] font-sans">
                    {!isAwaiting ? 'No excluded files in this complete scan.' : 'Awaiting scan to generate coverage item breakdown.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
