import { useState } from 'react';
import { 
  FileCheck, 
  FileX, 
  HardDrive, 
  Search, 
  ShieldCheck, 
  FileCode,
  Binary
} from 'lucide-react';
import { CoverageSummary } from '../types/api';
import { TableRowSkeleton, MetricSkeleton } from './LoadingSkeleton';
import { EmptyState } from './EmptyState';

interface CoverageTabProps {
  coverage: CoverageSummary | null;
  isLoading: boolean;
}

export function CoverageTab({ coverage, isLoading }: CoverageTabProps) {
  const [filterReason, setFilterReason] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  if (isLoading) {
    return (
      <div className="space-y-6 animate-in fade-in duration-300">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <MetricSkeleton />
          <MetricSkeleton />
          <MetricSkeleton />
          <MetricSkeleton />
        </div>
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5">
          <div className="h-6 w-48 bg-slate-800 rounded mb-4"></div>
          <table className="w-full">
            <tbody>
              <TableRowSkeleton />
              <TableRowSkeleton />
              <TableRowSkeleton />
            </tbody>
          </table>
        </div>
      </div>
    );
  }

  if (!coverage) {
    return (
      <EmptyState
        type="no-coverage"
        title="No Scan Coverage Recorded"
        description="Run a scan on your repository to generate file classification telemetry and skipped content breakdown."
      />
    );
  }

  const formatBytes = (bytes: number) => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
  };

  const totalFiles = coverage.totalFiles || 0;
  const scannedFiles = coverage.scannedFiles || 0;
  const skippedFiles = coverage.skippedFiles || 0;
  const undeterminedFiles = coverage.undeterminedFiles || 0;

  const scannedPercent = totalFiles > 0 ? Math.round((scannedFiles / totalFiles) * 100) : 100;
  const skippedPercent = totalFiles > 0 ? Math.round((skippedFiles / totalFiles) * 100) : 0;

  const skippedItems = coverage.skippedItems || (coverage.items || []).filter((i) => i.status === 'SKIPPED');

  const filteredItems = skippedItems.filter((item) => {
    const matchesSearch = item.filePath.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (item.reasonCode && item.reasonCode.toLowerCase().includes(searchQuery.toLowerCase()));
    
    if (filterReason === 'ALL') return matchesSearch;
    return matchesSearch && item.reasonCode === filterReason;
  });

  const getReasonBadge = (reasonCode: string) => {
    switch (reasonCode) {
      case 'UNSUPPORTED_BINARY_DOCUMENT':
        return 'bg-amber-500/10 text-amber-400 border-amber-500/30';
      case 'UNSUPPORTED_BINARY_FILE':
        return 'bg-slate-800 text-slate-300 border-slate-700';
      case 'MONITORING_FILE_SIZE_LIMIT_EXCEEDED':
        return 'bg-rose-500/10 text-rose-400 border-rose-500/30';
      case 'RELEASE_FILE_SIZE_CEILING_EXCEEDED':
        return 'bg-rose-500/10 text-rose-400 border-rose-500/30';
      default:
        return 'bg-blue-500/10 text-blue-400 border-blue-500/30';
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Top Overview Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Files Card */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-sm flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Total Files
            </span>
            <div className="p-2 rounded-xl bg-blue-500/10 text-blue-400 border border-blue-500/20">
              <FileCode className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-bold text-white tabular-nums tracking-tight">
              {totalFiles.toLocaleString()}
            </span>
            <p className="text-xs text-slate-500 mt-0.5">
              Evaluated repository inventory
            </p>
          </div>
        </div>

        {/* Scanned Text Files */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-sm flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Scanned Text Files
            </span>
            <div className="p-2 rounded-xl bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
              <FileCheck className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-bold text-emerald-400 tabular-nums tracking-tight">
              {scannedFiles.toLocaleString()}
            </span>
            <p className="text-xs text-slate-500 mt-0.5">
              {scannedPercent}% of total inventory
            </p>
          </div>
        </div>

        {/* Skipped Content */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-sm flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Skipped Files
            </span>
            <div className="p-2 rounded-xl bg-amber-500/10 text-amber-400 border border-amber-500/20">
              <FileX className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-bold text-amber-400 tabular-nums tracking-tight">
              {skippedFiles.toLocaleString()}
            </span>
            <p className="text-xs text-slate-500 mt-0.5">
              Binaries & oversized assets
            </p>
          </div>
        </div>

        {/* Total Evaluated Bytes */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-sm flex flex-col justify-between">
          <div className="flex items-center justify-between">
            <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider">
              Evaluated Size
            </span>
            <div className="p-2 rounded-xl bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">
              <HardDrive className="w-4 h-4" />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-bold text-white tabular-nums tracking-tight">
              {formatBytes(coverage.totalBytes || 0)}
            </span>
            <p className="text-xs text-slate-500 mt-0.5">
              Coverage Impact: <span className="text-slate-300 font-medium">{coverage.coverageImpact || 'COMPLETE'}</span>
            </p>
          </div>
        </div>
      </div>

      {/* Segmented Coverage Bar */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 space-y-3">
        <div className="flex items-center justify-between text-xs">
          <span className="font-semibold text-white tracking-tight">
            Repository Scan Coverage Ratio
          </span>
          <span className="text-slate-400 tabular-nums">
            {scannedPercent}% Scanned Text • {skippedPercent}% Skipped
          </span>
        </div>
        <div className="w-full h-3 rounded-full bg-slate-800 overflow-hidden flex">
          <div
            style={{ width: `${scannedPercent}%` }}
            className="bg-emerald-500 h-full transition-all duration-500"
            title={`Scanned: ${scannedFiles} files`}
          />
          <div
            style={{ width: `${skippedPercent}%` }}
            className="bg-amber-500 h-full transition-all duration-500"
            title={`Skipped: ${skippedFiles} files`}
          />
        </div>
        <div className="flex flex-wrap items-center gap-4 text-xs text-slate-400 pt-1">
          <div className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span>
            <span>Text files scanned ({coverage.textFiles || scannedFiles})</span>
          </div>
          <div className="flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-amber-500"></span>
            <span>Binary documents & media skipped ({coverage.binaryFiles || skippedFiles})</span>
          </div>
          {undeterminedFiles > 0 && (
            <div className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-slate-500"></span>
              <span>Undetermined ({undeterminedFiles})</span>
            </div>
          )}
        </div>
      </div>

      {/* Skipped Content Audit Table (UC-006) */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl overflow-hidden shadow-sm">
        <div className="p-5 border-b border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h3 className="text-base font-semibold text-white tracking-tight flex items-center gap-2">
              <span>Skipped Content Audit Log</span>
              <span className="text-xs font-normal text-slate-400">
                ({skippedItems.length} files)
              </span>
            </h3>
            <p className="text-xs text-slate-400 mt-0.5">
              Transparent log of non-text documents and oversized assets excluded by classifier (FR-034, FR-037).
            </p>
          </div>

          {/* Search and Reason Filter */}
          <div className="flex flex-wrap items-center gap-2.5">
            <div className="relative">
              <Search className="w-3.5 h-3.5 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Filter skipped files..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="bg-slate-950 border border-slate-800 rounded-lg py-1.5 pl-8 pr-3 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500/50 w-44 sm:w-56"
              />
            </div>

            <select
              value={filterReason}
              onChange={(e) => setFilterReason(e.target.value)}
              className="bg-slate-950 border border-slate-800 rounded-lg px-2.5 py-1.5 text-xs text-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500/50"
            >
              <option value="ALL">All Reasons</option>
              <option value="UNSUPPORTED_BINARY_DOCUMENT">Binary Documents</option>
              <option value="UNSUPPORTED_BINARY_FILE">Binary Assets</option>
              <option value="MONITORING_FILE_SIZE_LIMIT_EXCEEDED">Size Limit Exceeded</option>
            </select>
          </div>
        </div>

        {filteredItems.length === 0 ? (
          <div className="p-8 text-center">
            {skippedItems.length === 0 ? (
              <EmptyState
                type="custom"
                title="100% Repository Files Scanned"
                description="Zero files were excluded or skipped during the latest scan pipeline execution."
                icon={<ShieldCheck className="w-12 h-12 text-emerald-400" />}
              />
            ) : (
              <p className="text-xs text-slate-500">
                No skipped files match the selected filter query.
              </p>
            )}
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-950/60 border-b border-slate-800 text-slate-400 font-semibold uppercase tracking-wider text-[11px]">
                <tr>
                  <th className="py-3 px-4">File Path</th>
                  <th className="py-3 px-4">Classification</th>
                  <th className="py-3 px-4">File Size</th>
                  <th className="py-3 px-4">Reason Code</th>
                  <th className="py-3 px-4">Coverage Impact</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-slate-300">
                {filteredItems.map((item) => (
                  <tr key={item.id || item.filePath} className="hover:bg-slate-800/40 transition-colors">
                    <td className="py-3 px-4 font-mono text-slate-200 truncate max-w-xs">
                      {item.filePath}
                    </td>
                    <td className="py-3 px-4">
                      <span className="flex items-center gap-1 text-slate-400">
                        <Binary className="w-3.5 h-3.5 text-slate-500" />
                        {item.classification}
                      </span>
                    </td>
                    <td className="py-3 px-4 tabular-nums text-slate-400">
                      {formatBytes(item.sizeBytes)}
                    </td>
                    <td className="py-3 px-4">
                      <span className={`text-[10px] font-semibold px-2 py-0.5 rounded border uppercase tracking-wide ${getReasonBadge(item.reasonCode)}`}>
                        {item.reasonCode}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-400">
                      {item.impact || item.details || 'SKIPPED'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
