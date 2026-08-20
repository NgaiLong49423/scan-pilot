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
        <div className="p-5 bg-[#161b22] border border-[#30363d] rounded-2xl space-y-2">
          <div className="flex items-center justify-between text-[#8b949e]">
            <span className="text-xs font-semibold uppercase tracking-wider">Stage 1: Snapshot (HEAD)</span>
            <FileCode className="w-4 h-4 text-[#58a6ff]" />
          </div>
          <div className="text-2xl font-bold text-[#f0f6fc] tabular-nums">346 / 346 Files</div>
          <p className="text-xs text-[#8b949e]">100% of tracked repository files scanned at HEAD commit.</p>
          <div className="w-full bg-[#21262d] h-1.5 rounded-full overflow-hidden mt-3">
            <div className="bg-[#1f6feb] h-full w-full rounded-full" />
          </div>
        </div>

        {/* Stage 2 Coverage */}
        <div className="p-5 bg-[#161b22] border border-[#30363d] rounded-2xl space-y-2">
          <div className="flex items-center justify-between text-[#8b949e]">
            <span className="text-xs font-semibold uppercase tracking-wider">Stage 2: Git History</span>
            <GitCommit className="w-4 h-4 text-[#3fb950]" />
          </div>
          <div className="text-2xl font-bold text-[#f0f6fc] tabular-nums">128 Commits</div>
          <p className="text-xs text-[#8b949e]">Full reachable commit history traversed back to root commit.</p>
          <div className="w-full bg-[#21262d] h-1.5 rounded-full overflow-hidden mt-3">
            <div className="bg-[#238636] h-full w-full rounded-full" />
          </div>
        </div>

        {/* Sandbox & Trust */}
        <div className="p-5 bg-[#161b22] border border-[#30363d] rounded-2xl space-y-2">
          <div className="flex items-center justify-between text-[#8b949e]">
            <span className="text-xs font-semibold uppercase tracking-wider">Security Architecture</span>
            <Lock className="w-4 h-4 text-[#3fb950]" />
          </div>
          <div className="text-2xl font-bold text-[#3fb950] flex items-center gap-2">
            <ShieldCheck className="w-6 h-6" />
            <span>Isolated Sandbox</span>
          </div>
          <p className="text-xs text-[#8b949e]">Ephemerally scanned in isolated worker, fully purged on complete.</p>
          <div className="w-full bg-[#21262d] h-1.5 rounded-full overflow-hidden mt-3">
            <div className="bg-[#238636] h-full w-full rounded-full" />
          </div>
        </div>
      </div>

      {/* Deterministic Audit Trail Table */}
      <div className="bg-[#161b22] border border-[#30363d] rounded-2xl overflow-hidden shadow-sm">
        <div className="p-5 border-b border-[#30363d] flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div>
            <h3 className="text-base font-bold text-[#f0f6fc]">Deterministic Audit Log</h3>
            <p className="text-xs text-[#8b949e] mt-0.5">
              Cryptographically verified pipeline stages for repository {repo.name} ({repo.branch}).
            </p>
          </div>
          <span className="text-xs font-mono text-[#8b949e] bg-[#0d1117] px-3 py-1 rounded-lg border border-[#30363d] self-start sm:self-auto">
            Run ID: scan-9f82c1a4
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="bg-[#0d1117] text-[#8b949e] uppercase tracking-wider font-semibold border-b border-[#30363d]">
              <tr>
                <th className="px-5 py-3">Stage</th>
                <th className="px-5 py-3">Scope / Target</th>
                <th className="px-5 py-3">Duration</th>
                <th className="px-5 py-3">Status</th>
                <th className="px-5 py-3">Engine</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#30363d] text-[#c9d1d9] font-mono">
              <tr className="hover:bg-[#21262d]/50 transition-colors">
                <td className="px-5 py-3.5 font-semibold text-[#f0f6fc]">Stage 1: Working Tree</td>
                <td className="px-5 py-3.5 text-[#8b949e]">HEAD Commit (346 files)</td>
                <td className="px-5 py-3.5 text-[#8b949e]">1.42s</td>
                <td className="px-5 py-3.5">
                  <span className="inline-flex items-center gap-1 text-[#3fb950] bg-[#238636]/15 px-2 py-0.5 rounded border border-[#238636]/30 text-[11px] font-sans font-medium">
                    <CheckCircle2 className="w-3 h-3" />
                    <span>Verified</span>
                  </span>
                </td>
                <td className="px-5 py-3.5 text-[#8b949e]">SP-CONFIG-001 Native AST</td>
              </tr>

              <tr className="hover:bg-[#21262d]/50 transition-colors">
                <td className="px-5 py-3.5 font-semibold text-[#f0f6fc]">Stage 2: History Tree</td>
                <td className="px-5 py-3.5 text-[#8b949e]">128 Commits (Full Log)</td>
                <td className="px-5 py-3.5 text-[#8b949e]">2.89s</td>
                <td className="px-5 py-3.5">
                  <span className="inline-flex items-center gap-1 text-[#3fb950] bg-[#238636]/15 px-2 py-0.5 rounded border border-[#238636]/30 text-[11px] font-sans font-medium">
                    <CheckCircle2 className="w-3 h-3" />
                    <span>Verified</span>
                  </span>
                </td>
                <td className="px-5 py-3.5 text-[#8b949e]">Gitleaks v8.18 Pipeline</td>
              </tr>

              <tr className="hover:bg-[#21262d]/50 transition-colors">
                <td className="px-5 py-3.5 font-semibold text-[#f0f6fc]">Stage 3: AI Remediation</td>
                <td className="px-5 py-3.5 text-[#8b949e]">3 Critical Findings</td>
                <td className="px-5 py-3.5 text-[#8b949e]">0.85s</td>
                <td className="px-5 py-3.5">
                  <span className="inline-flex items-center gap-1 text-[#58a6ff] bg-[#1f6feb]/15 px-2 py-0.5 rounded border border-[#1f6feb]/30 text-[11px] font-sans font-medium">
                    <CheckCircle2 className="w-3 h-3" />
                    <span>Ready</span>
                  </span>
                </td>
                <td className="px-5 py-3.5 text-[#8b949e]">Gemini 1.5 Flash Pro Guard</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
