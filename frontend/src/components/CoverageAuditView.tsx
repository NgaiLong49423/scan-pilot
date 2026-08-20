import React from 'react';
import { 
  ShieldCheck, 
  Layers, 
  GitBranch, 
  GitCommit, 
  FileCode, 
  CheckCircle2, 
  Clock, 
  Fingerprint, 
  Lock 
} from 'lucide-react';
import { Repository } from '../types';

interface CoverageAuditViewProps {
  repo: Repository;
}

export const CoverageAuditView: React.FC<CoverageAuditViewProps> = ({ repo }) => {
  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      {/* Overview Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Stage 1 Coverage */}
        <div className="p-5 bg-slate-900/70 border border-slate-800 rounded-2xl space-y-2">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Stage 1: Snapshot (HEAD)</span>
            <FileCode className="w-4 h-4 text-indigo-400" />
          </div>
          <div className="text-2xl font-bold text-white tabular-nums">346 / 346 Files</div>
          <p className="text-xs text-slate-400">100% of tracked repository files scanned at HEAD commit.</p>
          <div className="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden mt-3">
            <div className="bg-indigo-500 h-full w-full rounded-full" />
          </div>
        </div>

        {/* Stage 2 Coverage */}
        <div className="p-5 bg-slate-900/70 border border-slate-800 rounded-2xl space-y-2">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Stage 2: Git History</span>
            <GitCommit className="w-4 h-4 text-cyan-400" />
          </div>
          <div className="text-2xl font-bold text-white tabular-nums">128 Commits</div>
          <p className="text-xs text-slate-400">Full reachable commit history traversed back to root commit.</p>
          <div className="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden mt-3">
            <div className="bg-cyan-500 h-full w-full rounded-full" />
          </div>
        </div>

        {/* Sandbox & Trust */}
        <div className="p-5 bg-slate-900/70 border border-slate-800 rounded-2xl space-y-2">
          <div className="flex items-center justify-between text-slate-400">
            <span className="text-xs font-semibold uppercase tracking-wider">Sandbox & Redaction</span>
            <Fingerprint className="w-4 h-4 text-emerald-400" />
          </div>
          <div className="text-2xl font-bold text-emerald-400">Zero Raw Secret</div>
          <p className="text-xs text-slate-400">Ephemeral workspace container purged after scan completion.</p>
          <div className="w-full bg-slate-800 h-1.5 rounded-full overflow-hidden mt-3">
            <div className="bg-emerald-500 h-full w-full rounded-full" />
          </div>
        </div>
      </div>

      {/* Audit Log Table */}
      <div className="bg-slate-900/70 border border-slate-800 rounded-2xl overflow-hidden shadow-sm">
        <div className="p-5 border-b border-slate-800 flex items-center justify-between">
          <div>
            <h3 className="text-base font-bold text-white tracking-tight">
              Deterministic Scan Audit Trail
            </h3>
            <p className="text-xs text-slate-400 mt-0.5">
              Verified detector execution evidence for {repo.name} ({repo.branch}).
            </p>
          </div>
          <span className="text-xs font-mono bg-slate-800 text-slate-300 px-2.5 py-1 rounded border border-slate-700">
            SP-CONFIG-001 • Gitleaks Adapter
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs font-mono">
            <thead className="bg-slate-950/60 border-b border-slate-800 text-slate-400 font-sans">
              <tr>
                <th className="py-3 px-4 font-semibold">Scope & Mode</th>
                <th className="py-3 px-4 font-semibold">Commit / Ref</th>
                <th className="py-3 px-4 font-semibold">Detector</th>
                <th className="py-3 px-4 font-semibold">Duration</th>
                <th className="py-3 px-4 font-semibold">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800 text-slate-300">
              <tr className="hover:bg-slate-800/30 transition-colors">
                <td className="py-3 px-4 flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-indigo-400" />
                  <span className="font-semibold text-white">Stage 1: Working Tree (HEAD)</span>
                </td>
                <td className="py-3 px-4 text-slate-400">HEAD (b7f9a1c)</td>
                <td className="py-3 px-4 text-indigo-300">Gitleaks Static Adapter</td>
                <td className="py-3 px-4">1.24s</td>
                <td className="py-3 px-4">
                  <span className="text-emerald-400 font-sans font-medium px-2 py-0.5 rounded bg-emerald-500/10 border border-emerald-500/20">
                    Completed
                  </span>
                </td>
              </tr>
              <tr className="hover:bg-slate-800/30 transition-colors">
                <td className="py-3 px-4 flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-cyan-400" />
                  <span className="font-semibold text-white">Stage 2: Git Commit Tree</span>
                </td>
                <td className="py-3 px-4 text-slate-400">128 reachable commits</td>
                <td className="py-3 px-4 text-cyan-300">Gitleaks History Adapter</td>
                <td className="py-3 px-4">3.07s</td>
                <td className="py-3 px-4">
                  <span className="text-emerald-400 font-sans font-medium px-2 py-0.5 rounded bg-emerald-500/10 border border-emerald-500/20">
                    Completed
                  </span>
                </td>
              </tr>
              <tr className="hover:bg-slate-800/30 transition-colors">
                <td className="py-3 px-4 flex items-center gap-2">
                  <span className="w-2 h-2 rounded-full bg-emerald-400" />
                  <span className="font-semibold text-white">Stage 3: Workspace Purge</span>
                </td>
                <td className="py-3 px-4 text-slate-400">Sandbox Sandbox-eph-89</td>
                <td className="py-3 px-4 text-slate-400">Scan Pilot Core Engine</td>
                <td className="py-3 px-4">0.12s</td>
                <td className="py-3 px-4">
                  <span className="text-emerald-400 font-sans font-medium px-2 py-0.5 rounded bg-emerald-500/10 border border-emerald-500/20">
                    Cleaned
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
