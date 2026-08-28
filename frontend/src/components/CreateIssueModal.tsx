import React, { useEffect, useReducer } from 'react';
import { 
  X, 
  ShieldAlert, 
  ExternalLink, 
  Loader2, 
  CheckCircle2, 
  AlertCircle 
} from 'lucide-react';
import { Finding, FindingIssueLinkDto } from '../types';
import { fetchFindingIssuePreview, createFindingIssue } from '../services/api';
import { 
  reduceFindingIssueState, 
  initialFindingIssueState, 
  buildCreateIssuePayload 
} from '../services/findingIssueHelper';

interface CreateIssueModalProps {
  isOpen: boolean;
  finding: Finding | null;
  onClose: () => void;
  onIssueCreated?: (findingId: string, issueLink: FindingIssueLinkDto) => void;
}

export const CreateIssueModal: React.FC<CreateIssueModalProps> = ({
  isOpen,
  finding,
  onClose,
  onIssueCreated,
}) => {
  const [state, dispatch] = useReducer(reduceFindingIssueState, initialFindingIssueState);

  useEffect(() => {
    if (isOpen && finding) {
      dispatch({ type: 'OPEN_MODAL' });
      fetchFindingIssuePreview(finding.id).then((result) => {
        if (result.status === 'SUCCESS') {
          dispatch({ type: 'PREVIEW_LOADED', preview: result.data });
        } else if (result.status === 'ERROR') {
          dispatch({ type: 'PREVIEW_FAILED', error: result.error });
        } else {
          dispatch({ type: 'PREVIEW_FAILED', error: 'Finding not found' });
        }
      });
    } else {
      dispatch({ type: 'CLOSE_MODAL' });
    }
  }, [isOpen, finding]);

  if (!isOpen || !finding) {
    return null;
  }

  const handleConfirm = async () => {
    if (!state.preview?.previewToken) return;

    dispatch({ type: 'SUBMIT_START' });
    const payload = buildCreateIssuePayload(state.preview.previewToken);
    const result = await createFindingIssue(finding.id, payload);

    if (result.status === 'SUCCESS') {
      dispatch({ type: 'SUBMIT_SUCCESS', link: result.data });
      onIssueCreated?.(finding.id, result.data);
    } else if (result.status === 'ERROR') {
      if (result.statusCode === 409 && result.error.includes('CREATION_IN_PROGRESS')) {
        dispatch({ type: 'SUBMIT_CONFLICT_PENDING' });
      } else {
        dispatch({ type: 'SUBMIT_FAILED', error: result.error });
      }
    } else {
      dispatch({ type: 'SUBMIT_FAILED', error: 'Failed to create issue' });
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-[#161b22] border border-[#30363d] rounded-2xl w-full max-w-2xl overflow-hidden shadow-2xl flex flex-col max-h-[90vh]">
        {/* Modal Header */}
        <div className="p-5 border-b border-[#30363d] flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="p-2 bg-[#1f6feb]/15 rounded-lg border border-[#1f6feb]/30 text-[#58a6ff]">
              <ShieldAlert className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-semibold text-[#f0f6fc]">
                Create Secret-Safe GitHub Issue
              </h3>
              <p className="text-xs text-[#8b949e]">
                Preview canonical markdown before creating issue on repository
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close modal"
            className="text-[#8b949e] hover:text-[#f0f6fc] p-1.5 rounded-lg hover:bg-[#21262d] transition-colors focus-visible:ring-2 focus-visible:ring-[#58a6ff] focus-visible:outline-none"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Content Body */}
        <div className="p-5 overflow-y-auto space-y-4 flex-1">
          {/* Loading state */}
          {state.status === 'PREVIEW_LOADING' && (
            <div className="flex flex-col items-center justify-center py-12 space-y-3">
              <Loader2 className="w-8 h-8 text-[#58a6ff] animate-spin" />
              <p className="text-sm text-[#8b949e]">
                Generating secret-safe canonical issue preview...
              </p>
            </div>
          )}

          {/* Error Banner */}
          {state.status === 'ERROR' && (
            <div className="p-4 bg-[#da3633]/15 border border-[#da3633]/30 rounded-xl text-[#f85149] flex items-start gap-3">
              <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
              <div className="space-y-1">
                <p className="text-sm font-medium">Issue Creation Error</p>
                <p className="text-xs text-[#f85149]/90">{state.errorMessage || 'An error occurred.'}</p>
              </div>
            </div>
          )}

          {/* Creation in progress Banner */}
          {state.status === 'CREATION_IN_PROGRESS' && (
            <div className="p-4 bg-[#d29922]/15 border border-[#d29922]/30 rounded-xl text-[#e3b341] flex items-start gap-3">
              <Loader2 className="w-5 h-5 shrink-0 mt-0.5 animate-spin" />
              <div className="space-y-1">
                <p className="text-sm font-medium">Issue Creation in Progress</p>
                <p className="text-xs text-[#e3b341]/90">
                  An issue creation request is currently being processed for this finding. Please wait a moment.
                </p>
              </div>
            </div>
          )}

          {/* Success Banner / Issue Created */}
          {state.status === 'SUCCESS' && (
            <div className="p-4 bg-[#238636]/15 border border-[#238636]/30 rounded-xl text-[#3fb950] flex flex-col items-center justify-center py-8 text-center space-y-3">
              <CheckCircle2 className="w-10 h-10 text-[#3fb950]" />
              <div className="space-y-1">
                <p className="text-base font-semibold text-[#f0f6fc]">
                  GitHub Issue Successfully Linked!
                </p>
                <p className="text-xs text-[#8b949e]">
                  Finding is tracked under GitHub Issue #{state.link?.githubIssueNumber || state.preview?.existingIssueNumber || ''}.
                </p>
              </div>
              {(state.link?.githubIssueUrl || state.preview?.existingIssueUrl) && (
                <a
                  href={state.link?.githubIssueUrl || state.preview?.existingIssueUrl || '#'}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-2 px-4 py-2 bg-[#238636] hover:bg-[#2ea043] text-white text-xs font-semibold rounded-lg shadow transition-colors focus-visible:ring-2 focus-visible:ring-[#58a6ff] focus-visible:outline-none"
                >
                  <span>Open Issue #{state.link?.githubIssueNumber || state.preview?.existingIssueNumber} on GitHub</span>
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>
              )}
            </div>
          )}

          {/* Preview Details */}
          {(state.status === 'PREVIEW_READY' || state.status === 'SUBMITTING' || state.status === 'CREATION_IN_PROGRESS') && (
            <>
              {/* Security Banner */}
              <div className="p-3 bg-[#1f6feb]/10 border border-[#1f6feb]/25 rounded-xl flex items-start gap-2.5">
                <ShieldAlert className="w-4 h-4 text-[#58a6ff] shrink-0 mt-0.5" />
                <p className="text-xs text-[#58a6ff] leading-relaxed">
                  <strong>Security Invariant:</strong> Raw secrets, source code lines, and absolute filesystem paths are strictly redacted in this preview and resulting GitHub issue.
                </p>
              </div>

              {/* Issue Title Preview */}
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-[#8b949e] uppercase tracking-wider">
                  Issue Title (Read-Only)
                </label>
                <input
                  type="text"
                  readOnly
                  value={state.preview?.title || ''}
                  className="w-full px-3 py-2 bg-[#0d1117] border border-[#30363d] rounded-lg text-sm text-[#f0f6fc] font-mono focus:outline-none cursor-default"
                />
              </div>

              {/* Issue Body Preview */}
              <div className="space-y-1.5">
                <label className="text-xs font-medium text-[#8b949e] uppercase tracking-wider">
                  Canonical Markdown Body (Read-Only)
                </label>
                <textarea
                  readOnly
                  rows={10}
                  value={state.preview?.body || ''}
                  className="w-full px-3 py-2 bg-[#0d1117] border border-[#30363d] rounded-lg text-xs text-[#f0f6fc] font-mono leading-relaxed focus:outline-none resize-none cursor-default"
                />
              </div>
            </>
          )}
        </div>

        {/* Modal Footer */}
        <div className="p-4 bg-[#0d1117] border-t border-[#30363d] flex items-center justify-end gap-3">
          {state.status === 'SUCCESS' ? (
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 bg-[#21262d] hover:bg-[#30363d] text-[#f0f6fc] text-xs font-medium rounded-lg transition-colors focus-visible:ring-2 focus-visible:ring-[#58a6ff] focus-visible:outline-none"
            >
              Close
            </button>
          ) : (
            <>
              <button
                type="button"
                onClick={onClose}
                disabled={state.status === 'SUBMITTING'}
                className="px-4 py-2 bg-[#21262d] hover:bg-[#30363d] disabled:opacity-50 text-[#f0f6fc] text-xs font-medium rounded-lg transition-colors focus-visible:ring-2 focus-visible:ring-[#58a6ff] focus-visible:outline-none"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirm}
                disabled={
                  state.status !== 'PREVIEW_READY' ||
                  !state.preview?.previewToken
                }
                className="inline-flex items-center gap-2 px-4 py-2 bg-[#238636] hover:bg-[#2ea043] disabled:opacity-50 disabled:cursor-not-allowed text-white text-xs font-semibold rounded-lg shadow transition-colors focus-visible:ring-2 focus-visible:ring-[#58a6ff] focus-visible:outline-none"
              >
                {state.status === 'SUBMITTING' ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    <span>Creating Issue...</span>
                  </>
                ) : (
                  <span>Confirm & Create GitHub Issue</span>
                )}
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
};
