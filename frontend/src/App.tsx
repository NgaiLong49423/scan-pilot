import { useState } from 'react';
import { 
  ShieldAlert, ShieldCheck, Shield, ChevronRight, AlertTriangle, 
  Search, ArrowLeft, Code, Cpu, GitBranch, Clock, 
  CheckCircle, RefreshCw, Info, CheckSquare, Square,
  Terminal
} from 'lucide-react';

// --- Types ---

type AttentionStatus = 'Critical' | 'Warning' | 'Secure';
type FindingStatus = 'OPEN' | 'RESOLVED' | 'SCANNING...';
type RemediationQuality = 'ACTION_REQUIRED' | 'RISK_CONTAINED' | 'VERIFIED_COMPLETE';

interface Repository {
  id: string;
  name: string;
  branch: string;
  lastScanned: string;
  findingCount: number;
  attentionStatus: AttentionStatus;
}

interface Finding {
  id: string;
  ruleId: string;
  ruleName: string;
  severity: 'High';
  status: FindingStatus;
  remediationQuality: RemediationQuality;
  filePath: string;
  lineNumber: number;
  snippet: string;
}

// --- Prototype Data ---

const PROTOTYPE_REPOS: Repository[] = [
  {
    id: 'repo-1',
    name: 'acme-corp/ai-service-frontend',
    branch: 'main',
    lastScanned: '2m ago',
    findingCount: 3,
    attentionStatus: 'Critical',
  },
  {
    id: 'repo-2',
    name: 'acme-corp/auth-backend',
    branch: 'staging',
    lastScanned: '1h ago',
    findingCount: 0,
    attentionStatus: 'Secure',
  },
  {
    id: 'repo-3',
    name: 'acme-corp/data-pipeline',
    branch: 'feature/ml-model',
    lastScanned: '45m ago',
    findingCount: 1,
    attentionStatus: 'Warning',
  }
];

const INITIAL_FINDING: Finding = {
  id: 'find-001',
  ruleId: 'SP-CONFIG-001',
  ruleName: 'Source Code Secret Exposure',
  severity: 'High',
  status: 'OPEN',
  remediationQuality: 'ACTION_REQUIRED',
  filePath: 'src/config/ai-client.ts',
  lineNumber: 42,
  snippet: 'const GEMINI_API_KEY = "AIza...REDACTED";',
};

// --- Components ---

export default function App() {
  const [currentView, setCurrentView] = useState<'dashboard' | 'repository'>('dashboard');
  const [selectedRepo, setSelectedRepo] = useState<Repository | null>(null);

  const navigateToRepo = (repo: Repository) => {
    setSelectedRepo(repo);
    setCurrentView('repository');
  };

  const navigateToDashboard = () => {
    setSelectedRepo(null);
    setCurrentView('dashboard');
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-300 font-sans flex flex-col">
      {/* Top Bar Contract */}
      <header className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-900/50 backdrop-blur-sm sticky top-0 z-10">
        <div className="flex items-center gap-3">
          <div className="bg-blue-600/20 p-2 rounded-lg border border-blue-500/30">
            <Cpu className="w-5 h-5 text-blue-400" />
          </div>
          <span className="text-white font-semibold tracking-tight text-lg">Scan Pilot</span>
        </div>
        
        <nav className="hidden md:flex items-center gap-6">
          <a href="#" className="text-sm font-medium text-white">Dashboard</a>
          <a href="#" className="text-sm font-medium text-slate-400 hover:text-white transition-colors">Rules</a>
          <a href="#" className="text-sm font-medium text-slate-400 hover:text-white transition-colors">Integrations</a>
          <a href="#" className="text-sm font-medium text-slate-400 hover:text-white transition-colors">Reports</a>
        </nav>
        
        <div className="flex items-center gap-4">
          <button className="text-sm font-medium text-slate-400 hover:text-white transition-colors flex items-center gap-2">
            <Info className="w-4 h-4" />
            <span className="hidden sm:inline">Docs</span>
          </button>
          <div className="w-8 h-8 rounded-full bg-slate-800 border border-slate-700 flex items-center justify-center">
            <span className="text-xs font-medium text-slate-300">JD</span>
          </div>
        </div>
      </header>

      <main className="flex-1 p-6 lg:p-8 max-w-7xl mx-auto w-full">
        {currentView === 'dashboard' ? (
          <DashboardView onSelectRepo={navigateToRepo} />
        ) : (
          <RepositoryView repo={selectedRepo!} onBack={navigateToDashboard} />
        )}
      </main>
    </div>
  );
}

// --- Dashboard View ---

function DashboardView({ onSelectRepo }: { onSelectRepo: (repo: Repository) => void }) {
  return (
    <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <div>
          <h1 className="text-2xl font-semibold text-white tracking-tight">Monitored Repositories</h1>
          <p className="text-slate-400 mt-1 text-sm">Security overview of your connected codebases.</p>
        </div>
        <div className="relative">
          <Search className="w-4 h-4 text-slate-500 absolute left-3 top-1/2 -translate-y-1/2" />
          <input 
            type="text" 
            placeholder="Search repositories..." 
            className="bg-slate-900 border border-slate-800 rounded-md py-2 pl-9 pr-4 text-sm text-slate-200 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500/50 w-full sm:w-64"
          />
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4">
        {PROTOTYPE_REPOS.map((repo) => (
          <div 
            key={repo.id}
            onClick={() => onSelectRepo(repo)}
            className="group bg-slate-900/40 border border-slate-800/60 rounded-xl p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-slate-800/40 hover:border-slate-700 transition-all cursor-pointer"
          >
            <div className="flex items-start gap-4">
              <div className="mt-1">
                {repo.attentionStatus === 'Critical' && <ShieldAlert className="w-6 h-6 text-rose-500" />}
                {repo.attentionStatus === 'Warning' && <AlertTriangle className="w-6 h-6 text-amber-500" />}
                {repo.attentionStatus === 'Secure' && <ShieldCheck className="w-6 h-6 text-emerald-500" />}
              </div>
              <div>
                <h3 className="text-base font-medium text-white group-hover:text-blue-400 transition-colors flex items-center gap-2">
                  {repo.name}
                </h3>
                <div className="flex items-center gap-4 mt-2 text-xs text-slate-400">
                  <span className="flex items-center gap-1.5 bg-slate-800/50 px-2 py-0.5 rounded-sm border border-slate-700/50">
                    <GitBranch className="w-3.5 h-3.5" />
                    {repo.branch}
                  </span>
                  <span className="flex items-center gap-1.5">
                    <Clock className="w-3.5 h-3.5" />
                    {repo.lastScanned}
                  </span>
                </div>
              </div>
            </div>

            <div className="flex items-center justify-between sm:justify-end gap-6 sm:gap-8 border-t sm:border-t-0 border-slate-800 pt-4 sm:pt-0">
              <div className="flex flex-col sm:items-end">
                <span className="text-xs text-slate-500 uppercase tracking-wider font-semibold mb-1">Findings</span>
                <span className={`text-lg font-medium tabular-nums ${repo.findingCount > 0 ? 'text-white' : 'text-slate-500'}`}>
                  {repo.findingCount}
                </span>
              </div>
              <div className="flex flex-col sm:items-end w-32">
                <span className="text-xs text-slate-500 uppercase tracking-wider font-semibold mb-1">Status</span>
                <span className={`text-sm font-medium ${
                  repo.attentionStatus === 'Critical' ? 'text-rose-400' :
                  repo.attentionStatus === 'Warning' ? 'text-amber-400' : 'text-emerald-400'
                }`}>
                  {repo.attentionStatus}
                </span>
              </div>
              <ChevronRight className="w-5 h-5 text-slate-600 group-hover:text-slate-400 hidden sm:block" />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// --- Repository View ---

function RepositoryView({ repo, onBack }: { repo: Repository, onBack: () => void }) {
  const [finding, setFinding] = useState<Finding>(INITIAL_FINDING);
  const [isScanning, setIsScanning] = useState(false);
  const [checklist, setChecklist] = useState({
    remove: false,
    revoke: false,
    history: false,
  });

  const handleScan = () => {
    setIsScanning(true);
    setFinding(prev => ({ ...prev, status: 'SCANNING...' }));
    
    // Simulate scan process
    setTimeout(() => {
      setFinding(prev => ({ 
        ...prev, 
        status: 'RESOLVED',
        remediationQuality: 'RISK_CONTAINED'
      }));
      
      // Simulate follow-up verification
      setTimeout(() => {
        setFinding(prev => ({ 
          ...prev, 
          remediationQuality: 'VERIFIED_COMPLETE'
        }));
        setIsScanning(false);
      }, 1200);
      
    }, 1500);
  };

  const toggleCheck = (key: keyof typeof checklist) => {
    setChecklist(prev => ({ ...prev, [key]: !prev[key] }));
  };

  return (
    <div className="space-y-6 animate-in fade-in slide-in-from-right-4 duration-500">
      <div className="flex items-center gap-4">
        <button 
          onClick={onBack}
          className="p-2 -ml-2 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          aria-label="Back to dashboard"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-xl font-semibold text-white tracking-tight">{repo.name}</h1>
            <span className="flex items-center gap-1 text-xs font-medium bg-slate-800 text-slate-300 px-2 py-0.5 rounded border border-slate-700">
              <GitBranch className="w-3 h-3" />
              {repo.branch}
            </span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        {/* Left Column: Finding Details */}
        <div className="lg:col-span-7 space-y-6">
          <div className="bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shadow-sm">
            <div className="p-5 border-b border-slate-800 bg-slate-900/50 flex flex-col sm:flex-row sm:items-start justify-between gap-4">
              <div>
                <div className="flex items-center gap-3 mb-2">
                  <span className="bg-rose-500/10 text-rose-400 border border-rose-500/20 px-2.5 py-0.5 rounded-md text-xs font-semibold tracking-wide uppercase">
                    {finding.severity} Severity
                  </span>
                  <span className="text-slate-400 text-sm font-mono">{finding.ruleId}</span>
                </div>
                <h2 className="text-lg font-medium text-white">{finding.ruleName}</h2>
              </div>
              <div className="flex flex-col items-start sm:items-end gap-2">
                <div className="flex items-center gap-2">
                  <span className="text-xs text-slate-500 uppercase tracking-wider font-semibold">Status</span>
                  <span className={`text-sm font-bold flex items-center gap-1.5 ${
                    finding.status === 'OPEN' ? 'text-amber-400' :
                    finding.status === 'SCANNING...' ? 'text-blue-400' : 'text-emerald-400'
                  }`}>
                    {finding.status === 'SCANNING...' && <RefreshCw className="w-3.5 h-3.5 animate-spin" />}
                    {finding.status === 'RESOLVED' && <CheckCircle className="w-3.5 h-3.5" />}
                    {finding.status === 'OPEN' && <AlertTriangle className="w-3.5 h-3.5" />}
                    {finding.status}
                  </span>
                </div>
                <div className="flex items-center gap-2">
                  <span className="text-xs text-slate-500 uppercase tracking-wider font-semibold">Remediation</span>
                  <span className={`text-xs font-medium px-2 py-0.5 rounded-sm border ${
                    finding.remediationQuality === 'ACTION_REQUIRED' ? 'bg-amber-500/10 text-amber-400 border-amber-500/20' :
                    finding.remediationQuality === 'RISK_CONTAINED' ? 'bg-blue-500/10 text-blue-400 border-blue-500/20' :
                    'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                  }`}>
                    {finding.remediationQuality.replace('_', ' ')}
                  </span>
                </div>
              </div>
            </div>
            
            <div className="p-5">
              <div className="mb-4 flex items-center gap-2 text-sm text-slate-300">
                <Code className="w-4 h-4 text-slate-500" />
                <span className="font-mono text-xs">{finding.filePath}:{finding.lineNumber}</span>
              </div>
              
              <div className="bg-[#0d1117] rounded-lg border border-slate-800/80 p-4 font-mono text-sm overflow-x-auto relative group">
                <div className="flex gap-4">
                  <div className="text-slate-600 select-none text-right flex flex-col items-end min-w-[2rem]">
                    <span>41</span>
                    <span className="text-rose-500">42</span>
                    <span>43</span>
                  </div>
                  <div className="text-slate-300 flex flex-col">
                    <span><span className="text-blue-400">import</span> {'{ initClient }'} <span className="text-blue-400">from</span> <span className="text-emerald-400">'./client'</span>;</span>
                    <span className="bg-rose-500/10 -mx-2 px-2 border-l-2 border-rose-500"><span className="text-blue-400">const</span> GEMINI_API_KEY = <span className="text-amber-300">"{finding.snippet.split('"')[1]}"</span>;</span>
                    <span><span className="text-blue-400">export</span> <span className="text-blue-400">const</span> ai = initClient(GEMINI_API_KEY);</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div className="bg-slate-900 border border-slate-800 rounded-xl p-5 shadow-sm">
            <h3 className="text-base font-medium text-white flex items-center gap-2 mb-4">
              <Shield className="w-4 h-4 text-blue-400" />
              Remediation Checklist
            </h3>
            
            <div className="space-y-3 mb-6">
              <button onClick={() => toggleCheck('remove')} className="flex items-start gap-3 w-full text-left group">
                {checklist.remove ? <CheckSquare className="w-5 h-5 text-emerald-500 shrink-0 mt-0.5" /> : <Square className="w-5 h-5 text-slate-500 group-hover:text-slate-400 shrink-0 mt-0.5 transition-colors" />}
                <div>
                  <p className={`text-sm font-medium ${checklist.remove ? 'text-slate-400 line-through' : 'text-slate-200'}`}>Remove the secret from current source</p>
                  <p className="text-xs text-slate-500 mt-0.5">Move the configuration to environment variables (.env).</p>
                </div>
              </button>
              
              <button onClick={() => toggleCheck('revoke')} className="flex items-start gap-3 w-full text-left group">
                {checklist.revoke ? <CheckSquare className="w-5 h-5 text-emerald-500 shrink-0 mt-0.5" /> : <Square className="w-5 h-5 text-slate-500 group-hover:text-slate-400 shrink-0 mt-0.5 transition-colors" />}
                <div>
                  <p className={`text-sm font-medium ${checklist.revoke ? 'text-slate-400 line-through' : 'text-slate-200'}`}>Replace and revoke the exposed credential</p>
                  <p className="text-xs text-slate-500 mt-0.5">Generate a new key in Google Cloud Console and revoke the old one.</p>
                </div>
              </button>

              <button onClick={() => toggleCheck('history')} className="flex items-start gap-3 w-full text-left group">
                {checklist.history ? <CheckSquare className="w-5 h-5 text-emerald-500 shrink-0 mt-0.5" /> : <Square className="w-5 h-5 text-slate-500 group-hover:text-slate-400 shrink-0 mt-0.5 transition-colors" />}
                <div>
                  <p className={`text-sm font-medium ${checklist.history ? 'text-slate-400 line-through' : 'text-slate-200'}`}>Review Git history</p>
                  <p className="text-xs text-slate-500 mt-0.5">Ensure the secret is purged from previous commits if pushed remotely.</p>
                </div>
              </button>
            </div>

            <div className="pt-4 border-t border-slate-800 flex justify-end">
              <button 
                onClick={handleScan}
                disabled={isScanning || finding.status === 'RESOLVED'}
                className="bg-blue-600 hover:bg-blue-700 disabled:bg-slate-800 disabled:text-slate-500 disabled:cursor-not-allowed text-white font-medium py-2 px-4 rounded-lg flex items-center gap-2 transition-colors text-sm"
              >
                {isScanning ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    Scanning Repository...
                  </>
                ) : finding.status === 'RESOLVED' ? (
                  <>
                    <CheckCircle className="w-4 h-4" />
                    Verification Complete
                  </>
                ) : (
                  <>
                    <Search className="w-4 h-4" />
                    Run Verification Scan
                  </>
                )}
              </button>
            </div>
          </div>
        </div>

        {/* Right Column: AI Explanation */}
        <div className="lg:col-span-5 relative">
          {/* Subtle glow behind AI panel */}
          <div className="absolute -inset-0.5 bg-gradient-to-b from-blue-500/10 to-transparent rounded-2xl blur-xl pointer-events-none" />
          
          <div className="bg-slate-900 border border-slate-800/80 rounded-xl shadow-lg relative overflow-hidden flex flex-col h-full">
            <div className="p-4 border-b border-slate-800/80 bg-slate-900/80 flex items-center gap-3">
              <div className="bg-gradient-to-br from-blue-500 to-indigo-600 p-1.5 rounded-md shadow-sm">
                <Terminal className="w-4 h-4 text-white" />
              </div>
              <h3 className="text-sm font-semibold text-white tracking-wide">Gemini Security Analysis</h3>
            </div>
            
            <div className="p-5 space-y-6 text-sm flex-1">
              <div>
                <h4 className="font-medium text-slate-200 mb-2 flex items-center gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-blue-500"></span>
                  What was detected
                </h4>
                <p className="text-slate-400 leading-relaxed pl-3.5 border-l border-slate-800">
                  A hardcoded Google API Key (likely for Gemini or Maps) was found embedded directly in the frontend source code. The pattern <code className="text-xs bg-slate-800 px-1 py-0.5 rounded text-amber-200 border border-slate-700">AIza...</code> matched our credentials signature.
                </p>
              </div>

              <div>
                <h4 className="font-medium text-slate-200 mb-2 flex items-center gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-rose-500"></span>
                  Why it matters
                </h4>
                <p className="text-slate-400 leading-relaxed pl-3.5 border-l border-slate-800">
                  Client-side code is visible to any user. Hardcoding secrets here exposes your quota and billing account to unauthorized usage, abuse, and potential denial-of-service via quota exhaustion.
                </p>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="bg-emerald-500/5 border border-emerald-500/10 rounded-lg p-3">
                  <h4 className="font-medium text-emerald-400 mb-1.5 text-xs uppercase tracking-wider">Evidence Proves</h4>
                  <p className="text-slate-400 text-xs leading-relaxed">
                    The key is currently present in tracked Git files and matches valid structural patterns for active credentials.
                  </p>
                </div>
                <div className="bg-slate-800/30 border border-slate-700/50 rounded-lg p-3">
                  <h4 className="font-medium text-slate-400 mb-1.5 text-xs uppercase tracking-wider">Evidence Cannot Prove</h4>
                  <p className="text-slate-500 text-xs leading-relaxed">
                    We cannot determine if the key has already been extracted or actively exploited by third parties.
                  </p>
                </div>
              </div>

              <div>
                <h4 className="font-medium text-slate-200 mb-2 flex items-center gap-2">
                  <span className="w-1.5 h-1.5 rounded-full bg-blue-500"></span>
                  Recommended Remediation
                </h4>
                <p className="text-slate-400 leading-relaxed pl-3.5 border-l border-slate-800">
                  Move the credential to a server-side environment variable (e.g., <code className="text-xs bg-slate-800 px-1 py-0.5 rounded text-slate-300">process.env.GEMINI_API_KEY</code>). Proxy client requests through your own backend to keep the key completely hidden from the browser. Immediate revocation of the current key is required to contain the risk.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
