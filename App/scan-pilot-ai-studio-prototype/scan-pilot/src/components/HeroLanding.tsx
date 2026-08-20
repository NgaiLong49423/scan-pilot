import React from 'react';
import { 
  ShieldCheck, 
  Github, 
  Play, 
  Sparkles, 
  GitBranch, 
  Lock, 
  Layers, 
  Fingerprint, 
  ExternalLink,
  CheckCircle 
} from 'lucide-react';

interface HeroLandingProps {
  onSignIn: () => void;
  onExploreDemo: () => void;
}

export const HeroLanding: React.FC<HeroLandingProps> = ({ onSignIn, onExploreDemo }) => {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-between relative overflow-hidden">
      {/* Ambient background glow effects */}
      <div className="absolute top-1/4 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[350px] bg-indigo-600/15 rounded-full blur-[120px] pointer-events-none" />
      <div className="absolute top-1/3 left-1/3 w-[300px] h-[200px] bg-cyan-500/10 rounded-full blur-[90px] pointer-events-none" />

      {/* Header */}
      <header className="w-full max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-5 flex items-center justify-between relative z-10">
        <div className="flex items-center gap-2.5">
          <div className="p-2 rounded-xl bg-indigo-600/20 border border-indigo-500/30 text-indigo-400">
            <ShieldCheck className="w-5 h-5" />
          </div>
          <span className="text-white font-bold text-lg tracking-tight">Scan Pilot</span>
        </div>

        <div className="flex items-center gap-4">
          <div className="hidden sm:inline-flex items-center gap-2 px-3 py-1 rounded-full bg-slate-900/80 border border-slate-800 text-xs text-slate-300">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>Systems Operational</span>
          </div>

          <button
            type="button"
            onClick={onSignIn}
            className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-lg bg-white hover:bg-slate-100 text-slate-900 text-xs font-semibold shadow-sm transition-all duration-150 active:scale-95"
          >
            <Github className="w-4 h-4" />
            <span>Sign in with GitHub</span>
          </button>
        </div>
      </header>

      {/* Hero Section */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-8 pb-16 relative z-10 flex flex-col items-center text-center">
        {/* Pill Badge */}
        <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-950/60 border border-indigo-500/30 text-indigo-300 text-xs font-medium mb-6 shadow-sm">
          <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
          <span>Continuous Secret Scanning & AI Remediation</span>
        </div>

        {/* Massive Headline */}
        <h1 className="text-3xl sm:text-5xl lg:text-6xl font-extrabold text-white tracking-tight max-w-4xl leading-tight">
          Continuous Security Guardrails for <span className="text-transparent bg-clip-text bg-gradient-to-r from-indigo-400 via-cyan-400 to-emerald-400">AI Codebases</span>
        </h1>

        <p className="mt-4 text-sm sm:text-base text-slate-400 max-w-2xl leading-relaxed">
          Zero secret leaks. Deep commit history verification. One-click Gemini AI remediation. Secure your AI-assisted development workflow before it reaches production.
        </p>

        {/* CTA Buttons */}
        <div className="mt-8 flex flex-wrap items-center justify-center gap-4">
          <button
            type="button"
            onClick={onSignIn}
            className="inline-flex items-center gap-2.5 px-6 py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-semibold shadow-lg shadow-indigo-600/25 transition-all duration-150 active:scale-95"
          >
            <Github className="w-4 h-4" />
            <span>Connect with GitHub</span>
          </button>

          <button
            type="button"
            onClick={onExploreDemo}
            className="inline-flex items-center gap-2 px-5 py-3 rounded-xl bg-slate-900/90 hover:bg-slate-800 text-slate-200 text-sm font-medium border border-slate-800 transition-all duration-150 active:scale-95"
          >
            <Play className="w-4 h-4 text-indigo-400 fill-indigo-400" />
            <span>Explore Live Demo</span>
          </button>
        </div>

        {/* 3D Perspective Hero Mockup Card */}
        <div className="mt-12 w-full max-w-4xl [perspective:1200px] group">
          <div className="relative rounded-2xl p-[1px] bg-gradient-to-b from-indigo-500/40 via-slate-800 to-slate-900 shadow-2xl shadow-indigo-500/10 transition-all duration-500 ease-out [transform:rotateX(12deg)_scale(0.96)] group-hover:[transform:rotateX(0deg)_scale(1)] group-hover:shadow-indigo-500/25">
            {/* Ambient Glow Aura */}
            <div className="absolute -inset-1 bg-gradient-to-r from-indigo-500/20 via-cyan-500/15 to-indigo-500/20 rounded-2xl blur-xl opacity-50 group-hover:opacity-100 transition duration-500 pointer-events-none" />

            <div className="relative bg-slate-900/95 border border-slate-800/80 rounded-2xl p-4 sm:p-6 text-left overflow-hidden backdrop-blur-xl">
              {/* Top Mockup Bar */}
              <div className="flex items-center justify-between pb-4 border-b border-slate-800 text-xs text-slate-400">
                <div className="flex items-center gap-2">
                  <span className="w-3 h-3 rounded-full bg-slate-700 inline-block" />
                  <span className="w-3 h-3 rounded-full bg-slate-700 inline-block" />
                  <span className="w-3 h-3 rounded-full bg-slate-700 inline-block" />
                  <span className="ml-2 font-mono text-slate-300">scan-pilot/dashboard</span>
                </div>
                <div className="flex items-center gap-2 font-semibold text-emerald-400 bg-emerald-500/10 px-2.5 py-0.5 rounded border border-emerald-500/20">
                  <CheckCircle className="w-3.5 h-3.5" />
                  <span>Health: 92/100</span>
                </div>
              </div>

          {/* Inner Content Grid */}
          <div className="grid grid-cols-1 md:grid-cols-12 gap-4 mt-4">
            {/* Recent Scans list */}
            <div className="md:col-span-4 space-y-2 text-xs font-mono">
              <span className="text-[10px] font-sans uppercase font-bold text-slate-500 tracking-wider">
                Recent Scans
              </span>
              <div className="p-2.5 rounded-lg bg-slate-950/70 border border-slate-800 flex items-center justify-between text-slate-300">
                <span className="flex items-center gap-1.5 truncate">
                  <GitBranch className="w-3 h-3 text-indigo-400" /> feat/auth-module
                </span>
                <span className="text-slate-500 text-[10px]">2m ago</span>
              </div>
              <div className="p-2.5 rounded-lg bg-rose-950/20 border border-rose-500/30 flex items-center justify-between text-rose-300">
                <span className="flex items-center gap-1.5 truncate">
                  <GitBranch className="w-3 h-3 text-rose-400" /> fix/api-keys
                </span>
                <span className="text-rose-400 text-[10px] font-bold">1 High</span>
              </div>
              <div className="p-2.5 rounded-lg bg-slate-950/70 border border-slate-800 flex items-center justify-between text-slate-300">
                <span className="flex items-center gap-1.5 truncate">
                  <GitBranch className="w-3 h-3 text-indigo-400" /> main
                </span>
                <span className="text-slate-500 text-[10px]">1h ago</span>
              </div>
            </div>

            {/* AI Remediation Diff */}
            <div className="md:col-span-8 rounded-xl bg-slate-950 border border-slate-800 p-3 font-mono text-xs overflow-x-auto">
              <div className="flex items-center justify-between pb-2 mb-2 border-b border-slate-800/80 font-sans text-xs">
                <span className="text-indigo-400 font-semibold flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5" /> GEMINI AI REMEDIATION
                </span>
                <span className="px-2 py-0.5 rounded bg-indigo-600/30 text-indigo-300 text-[10px] font-medium border border-indigo-500/40">
                  Ready to Apply
                </span>
              </div>
              <div className="space-y-1">
                <div className="text-rose-400 bg-rose-950/30 px-2 py-1 rounded">
                  - const apiKey = <span className="font-bold">"sk_live_51M************9X"</span>;
                </div>
                <div className="text-emerald-400 bg-emerald-950/30 px-2 py-1 rounded">
                  + const apiKey = <span className="font-bold">process.env.STRIPE_API_KEY</span>;
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

        {/* Feature Bento Grid */}
        <div className="mt-16 w-full max-w-4xl text-left">
          <h2 className="text-center text-xl font-bold text-white tracking-tight mb-2">
            Enterprise-Grade Security Engine
          </h2>
          <p className="text-center text-xs text-slate-400 mb-8">
            Built for zero-trust environments. We don't just scan; we understand the context of your code.
          </p>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* Card 1 */}
            <div className="p-6 bg-slate-900/60 border border-slate-800 rounded-2xl">
              <div className="p-2.5 w-fit rounded-xl bg-indigo-600/10 border border-indigo-500/20 text-indigo-400 mb-4">
                <Layers className="w-5 h-5" />
              </div>
              <h3 className="text-base font-semibold text-white mb-1">Dual-Stage Scanning</h3>
              <p className="text-xs text-slate-400 leading-relaxed mb-4">
                Inspects active working directories alongside deep Git tree history to catch secrets buried in past commits.
              </p>
              <div className="p-2.5 rounded-lg bg-slate-950/80 border border-slate-800 text-xs font-mono space-y-1">
                <div className="text-emerald-400 flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-emerald-400" /> Working Tree (Clean)
                </div>
                <div className="text-rose-400 flex items-center gap-1.5">
                  <span className="w-2 h-2 rounded-full bg-rose-400" /> Commit abc123f (Exposed Secret Detected)
                </div>
              </div>
            </div>

            {/* Card 2 */}
            <div className="p-6 bg-slate-900/60 border border-slate-800 rounded-2xl">
              <div className="p-2.5 w-fit rounded-xl bg-indigo-600/10 border border-indigo-500/20 text-indigo-400 mb-4">
                <Fingerprint className="w-5 h-5" />
              </div>
              <div className="flex items-center justify-between mb-1">
                <h3 className="text-base font-semibold text-white">Cryptographic Fingerprinting</h3>
                <span className="text-[10px] text-cyan-400 bg-cyan-950/40 px-2 py-0.5 rounded border border-cyan-800/40 font-mono">
                  Zero Exposure
                </span>
              </div>
              <p className="text-xs text-slate-400 leading-relaxed mb-4">
                Raw secrets never leave your environment. We generate one-way SHA-256 hashes locally to verify without exposing plaintext.
              </p>
              <div className="p-2.5 rounded-lg bg-slate-950/80 border border-slate-800 text-xs font-mono flex items-center justify-between text-slate-400">
                <span>"ghp_xyz123..."</span>
                <span className="text-slate-500">→ SHA-256 →</span>
                <span className="text-cyan-400 text-[11px] truncate max-w-[120px]">
                  e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
                </span>
              </div>
            </div>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="w-full border-t border-slate-800/80 bg-slate-950 py-6 text-xs text-slate-500">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <span className="font-semibold text-slate-400">Scan Pilot Security</span>
            <span>•</span>
            <span>Google Cloud Run & Spring Boot 3 Engine</span>
          </div>

          <div className="flex items-center gap-6">
            <span>OWASP Top 10 Aligned</span>
            <span>CWE Mapped</span>
            <span>Zero Raw Secret Policy</span>
          </div>
        </div>
      </footer>
    </div>
  );
};
