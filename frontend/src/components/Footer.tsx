import React from 'react';
import { 
  Shield, 
  Sparkles, 
  ExternalLink, 
  Github, 
  CheckCircle2, 
  Lock, 
  FileText, 
  Layers, 
  Cloud 
} from 'lucide-react';

export const Footer: React.FC = () => {
  return (
    <footer className="w-full border-t border-slate-800/80 bg-slate-950 text-slate-400 mt-20">
      {/* Top section: Bento Links Grid */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 lg:py-16">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-8 lg:gap-12">
          
          {/* Col 1 & 2: Brand, Value Proposition & Tech Stack */}
          <div className="lg:col-span-2 space-y-4">
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-lg bg-gradient-to-br from-cyan-500 to-blue-600 flex items-center justify-center shadow-lg shadow-cyan-500/20">
                <Shield className="w-5 h-5 text-white" />
              </div>
              <span className="text-xl font-bold text-white tracking-tight">Scan Pilot</span>
              <span className="px-2 py-0.5 text-xs font-semibold rounded-full bg-cyan-950 text-cyan-400 border border-cyan-800/50">
                MVP
              </span>
            </div>
            
            <p className="text-sm text-slate-400 leading-relaxed max-w-sm">
              Continuous multi-project security & health monitoring platform for AI-assisted and AI-generated software. Guarding repositories with automated secret detection, lifecycle tracking, and Gemini AI guidance.
            </p>

            <div className="flex flex-wrap items-center gap-2 pt-2">
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-slate-900 text-slate-300 border border-slate-800">
                <Cloud className="w-3.5 h-3.5 text-cyan-400" />
                Google Cloud Run
              </span>
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-slate-900 text-slate-300 border border-slate-800">
                <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
                Gemini 1.5 Flash
              </span>
              <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-md text-xs font-medium bg-slate-900 text-slate-300 border border-slate-800">
                <Layers className="w-3.5 h-3.5 text-emerald-400" />
                Spring Boot 3
              </span>
            </div>
          </div>

          {/* Col 3: Core Features */}
          <div className="space-y-3">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-200">
              Core Capabilities
            </h4>
            <ul className="space-y-2.5 text-sm">
              <li>
                <span className="text-slate-400 hover:text-slate-200 transition-colors flex items-center gap-1.5">
                  <Lock className="w-3.5 h-3.5 text-cyan-400 shrink-0" />
                  SP-CONFIG-001 Secret Scan
                </span>
              </li>
              <li>
                <span className="text-slate-400 hover:text-slate-200 transition-colors flex items-center gap-1.5">
                  <Sparkles className="w-3.5 h-3.5 text-indigo-400 shrink-0" />
                  Gemini Remediation Guide
                </span>
              </li>
              <li>
                <span className="text-slate-400 hover:text-slate-200 transition-colors flex items-center gap-1.5">
                  <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400 shrink-0" />
                  3-Stage Finding Lifecycle
                </span>
              </li>
              <li>
                <span className="text-slate-400 hover:text-slate-200 transition-colors flex items-center gap-1.5">
                  <Layers className="w-3.5 h-3.5 text-amber-400 shrink-0" />
                  Scope & Coverage Audit
                </span>
              </li>
            </ul>
          </div>

          {/* Col 4: Security & Standards */}
          <div className="space-y-3">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-200">
              Security & Trust
            </h4>
            <ul className="space-y-2.5 text-sm">
              <li>
                <span className="text-slate-400 hover:text-slate-200 transition-colors flex items-center gap-1.5">
                  <CheckCircle2 className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                  OWASP Top 10 Alignment
                </span>
              </li>
              <li>
                <span className="text-slate-400 hover:text-slate-200 transition-colors flex items-center gap-1.5">
                  <CheckCircle2 className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                  Zero Raw Secret Policy
                </span>
              </li>
              <li>
                <span className="text-slate-400 hover:text-slate-200 transition-colors flex items-center gap-1.5">
                  <CheckCircle2 className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                  HMAC-SHA-256 Identity
                </span>
              </li>
              <li>
                <span className="text-slate-400 hover:text-slate-200 transition-colors flex items-center gap-1.5">
                  <CheckCircle2 className="w-3.5 h-3.5 text-slate-500 shrink-0" />
                  Ephemeral Workspace Sandbox
                </span>
              </li>
            </ul>
          </div>

          {/* Col 5: Resources & Event Links */}
          <div className="space-y-3">
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-200">
              Resources & Links
            </h4>
            <ul className="space-y-2.5 text-sm">
              <li>
                <a 
                  href="https://github.com/NgaiLong49423/scan-pilot" 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="text-slate-400 hover:text-cyan-400 transition-colors inline-flex items-center gap-1.5"
                >
                  <Github className="w-3.5 h-3.5" />
                  GitHub Repository
                  <ExternalLink className="w-3 h-3 text-slate-600" />
                </a>
              </li>
              <li>
                <a 
                  href="https://scan-pilot-api-drbjfwrlxq-as.a.run.app/api/v1/system/status" 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="text-slate-400 hover:text-emerald-400 transition-colors inline-flex items-center gap-1.5"
                >
                  <FileText className="w-3.5 h-3.5" />
                  Live System Health API
                  <ExternalLink className="w-3 h-3 text-slate-600" />
                </a>
              </li>
              <li>
                <a 
                  href="https://aistudio.google.com" 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="text-slate-400 hover:text-indigo-400 transition-colors inline-flex items-center gap-1.5"
                >
                  <Sparkles className="w-3.5 h-3.5" />
                  Google AI Studio
                  <ExternalLink className="w-3 h-3 text-slate-600" />
                </a>
              </li>
            </ul>
          </div>

        </div>
      </div>

      {/* Bottom Sub-footer bar */}
      <div className="border-t border-slate-900 bg-slate-950/80">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-5 flex flex-col sm:flex-row items-center justify-between gap-3 text-xs text-slate-500">
          <div className="flex items-center gap-2">
            <span>© 2026 Scan Pilot. Built for AI Riser Vietnam 2026.</span>
          </div>

          <div className="flex items-center gap-4">
            <span className="inline-flex items-center gap-1.5 text-emerald-400">
              <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
              Cloud Run Service: Active (asia-southeast1)
            </span>
          </div>
        </div>
      </div>
    </footer>
  );
};
