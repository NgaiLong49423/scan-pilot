import React, { useEffect, useState } from 'react';

import {

  X,

  GitPullRequest,

  ShieldAlert,

  FileCode,

  ArrowRight,

  CheckCircle2,

  AlertTriangle,

  Loader2,

  ExternalLink,

  GitBranch

} from 'lucide-react';

import { Finding, FindingRemediationPrPreviewDto, FindingRemediationPrLinkDto } from '../types';

import { fetchFindingRemediationPrPreview, createFindingRemediationPr } from '../services/api';



interface RemediationPrModalProps {

  isOpen: boolean;

  finding: Finding;

  onClose: () => void;

  onPrCreated: (findingId: string, prLink: FindingRemediationPrLinkDto) => void;

}



export const RemediationPrModal: React.FC<RemediationPrModalProps> = ({

  isOpen,

  finding,

  onClose,

  onPrCreated,

}) => {

  const [loadingPreview, setLoadingPreview] = useState(false);

  const [preview, setPreview] = useState<FindingRemediationPrPreviewDto | null>(null);

  const [previewError, setPreviewError] = useState<string | null>(null);

  const [isSubmitting, setIsSubmitting] = useState(false);

  const [submitError, setSubmitError] = useState<string | null>(null);

  const [successLink, setSuccessLink] = useState<FindingRemediationPrLinkDto | null>(null);



  useEffect(() => {

    if (!isOpen) {

      setPreview(null);

      setPreviewError(null);

      setSubmitError(null);

      setSuccessLink(null);

      return;

    }



    let isMounted = true;

    setLoadingPreview(true);

    setPreviewError(null);

    setSubmitError(null);



    fetchFindingRemediationPrPreview(finding.id)

      .then((result) => {

        if (!isMounted) return;

        if (result.status === 'SUCCESS') {

          setPreview(result.data);

          if (result.data.alreadyLinked && result.data.existingPrUrl) {

            setSuccessLink({

              id: '',

              findingId: finding.id,

              repositoryId: result.data.repositoryId,

              sourceRevisionCommit: result.data.targetCommitSha,

              targetBranch: result.data.targetBranch,

              headBranch: result.data.remediationBranchName,

              state: 'CREATED',

              githubPrNumber: result.data.existingPrNumber,

              githubPrUrl: result.data.existingPrUrl,

              idempotencyMarker: '',

              createdAt: '',

              updatedAt: '',

            });

          }

        } else if (result.status === 'ERROR') {

          if (result.error === 'MANUAL_REMEDIATION_REQUIRED') {

            setPreviewError('This configuration file or structure cannot be safely patched automatically. Manual remediation is required.');

          } else if (result.error === 'AUTH_REQUIRED') {

            setPreviewError('Authentication session expired. Please sign in again.');

          } else if (result.error === 'GITHUB_APP_REQUIRED') {

            setPreviewError('GitHub App write permissions are required to create remediation branches and Pull Requests.');

          } else {

            setPreviewError('Unable to generate remediation preview. Please try again later.');

          }

        }

      })

      .catch(() => {

        if (isMounted) {

          setPreviewError('Network error while loading preview.');

        }

      })

      .finally(() => {

        if (isMounted) {

          setLoadingPreview(false);

        }

      });



    return () => {

      isMounted = false;

    };

  }, [isOpen, finding.id]);



  if (!isOpen) return null;



  const handleSubmit = async () => {

    if (!preview || !preview.previewToken || isSubmitting) return;



    setIsSubmitting(true);

    setSubmitError(null);



    try {

      const result = await createFindingRemediationPr(finding.id, {

        previewToken: preview.previewToken,

      });



      if (result.status === 'SUCCESS') {

        setSuccessLink(result.data);

        onPrCreated(finding.id, result.data);

      } else if (result.status === 'ERROR') {

        if (result.error === 'STALE_REVISION_ERROR') {

          setSubmitError('The target branch HEAD has changed since preview generation. Please close and re-open to refresh the preview.');

        } else if (result.error === 'PREVIEW_TOKEN_EXPIRED_OR_INVALID') {

          setSubmitError('Preview token expired (15-minute limit). Please refresh the preview.');

        } else if (result.error === 'GITHUB_APP_REQUIRED') {

          setSubmitError('GitHub App installation with pull_requests:write permission required.');

        } else {

          setSubmitError('Failed to create remediation Pull Request on GitHub. Please check repository permissions.');

        }

      }

    } catch {

      setSubmitError('An unexpected error occurred during PR creation.');

    } finally {

      setIsSubmitting(false);

    }

  };



  return (

    <div

      role="dialog"

      aria-modal="true"

      aria-labelledby="remediation-pr-modal-title"

      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/75 backdrop-blur-sm animate-in fade-in duration-200"

    >

      <div className="bg-[#161b22] border border-[#30363d] rounded-2xl w-full max-w-2xl overflow-hidden shadow-2xl flex flex-col max-h-[90vh]">

        {/* Header */}

        <div className="flex items-center justify-between p-5 border-b border-[#30363d] bg-[#0d1117]">

          <div className="flex items-center gap-2.5">

            <div className="p-2 rounded-lg bg-[#238636]/15 border border-[#238636]/30 text-[#3fb950]">

              <GitPullRequest className="w-5 h-5" />

            </div>

            <div>

              <h2 id="remediation-pr-modal-title" className="text-base font-semibold text-[#f0f6fc]">

                Spring Boot Safe Remediation PR

              </h2>

              <p className="text-xs text-[#8b949e]">

                Automated single-line secret replacement for {finding.ruleId}

              </p>

            </div>

          </div>

          <button

            type="button"

            onClick={onClose}

            aria-label="Close modal"

            className="text-[#8b949e] hover:text-[#f0f6fc] p-1.5 rounded-lg hover:bg-[#21262d] transition-colors"

          >

            <X className="w-5 h-5" />

          </button>

        </div>



        {/* Content Body */}

        <div className="p-5 overflow-y-auto space-y-4 flex-1">

          {/* Mandatory Revocation Notice Banner */}

          <div className="bg-[#da3633]/10 border border-[#da3633]/30 rounded-xl p-4 flex gap-3 text-xs leading-relaxed text-[#f85149]">

            <ShieldAlert className="w-5 h-5 flex-shrink-0 text-[#f85149] mt-0.5" />

            <div>

              <strong className="font-semibold block text-[#f85149] mb-1">

                MANDATORY REVOCATION & ROTATION NOTICE

              </strong>

              Merging this Pull Request replaces hardcoded secrets with the environment variable placeholder{' '}

              <code className="font-mono bg-[#0d1117] px-1.5 py-0.5 rounded border border-[#da3633]/30 text-white font-bold">

                {preview ? `\${${preview.envVariableName}}` : '${ENV_VAR_NAME}'}

              </code>

              , but <strong>DOES NOT revoke or invalidate the exposed credential</strong>. You must immediately revoke and rotate the secret in your cloud/service provider console.

            </div>

          </div>



          {loadingPreview && (

            <div className="py-12 flex flex-col items-center justify-center gap-3 text-center">

              <Loader2 className="w-7 h-7 text-[#58a6ff] animate-spin" />

              <p className="text-xs text-[#8b949e]">Verifying in-memory source proof & generating patch...</p>

            </div>

          )}



          {previewError && (

            <div className="bg-[#f85149]/10 border border-[#f85149]/30 rounded-xl p-4 text-xs text-[#f85149] flex items-center gap-2.5">

              <AlertTriangle className="w-4 h-4 flex-shrink-0" />

              <span>{previewError}</span>

            </div>

          )}



          {successLink && (

            <div className="bg-[#238636]/10 border border-[#238636]/30 rounded-xl p-5 text-center space-y-3">

              <CheckCircle2 className="w-8 h-8 text-[#3fb950] mx-auto" />

              <h3 className="text-sm font-semibold text-[#f0f6fc]">Remediation Pull Request Created!</h3>

              <p className="text-xs text-[#8b949e]">

                Feature branch and Pull Request #{successLink.githubPrNumber} have been opened on GitHub.

              </p>

              {successLink.githubPrUrl && (

                <a

                  href={successLink.githubPrUrl}

                  target="_blank"

                  rel="noopener noreferrer"

                  className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-xs font-semibold text-white bg-[#238636] hover:bg-[#2ea043] transition-colors shadow-sm"

                >

                  <span>View Pull Request #{successLink.githubPrNumber} on GitHub</span>

                  <ExternalLink className="w-3.5 h-3.5" />

                </a>

              )}

            </div>

          )}



          {preview && !successLink && (

            <div className="space-y-4">

              {/* Branch & Target Metadata */}

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">

                <div className="bg-[#0d1117] border border-[#30363d] rounded-lg p-3 space-y-1">

                  <span className="text-[#8b949e] block">Target Branch (Base)</span>

                  <span className="font-mono text-[#58a6ff] flex items-center gap-1.5 font-semibold">

                    <GitBranch className="w-3.5 h-3.5" />

                    {preview.targetBranch} ({preview.targetCommitSha.substring(0, 7)})

                  </span>

                </div>

                <div className="bg-[#0d1117] border border-[#30363d] rounded-lg p-3 space-y-1">

                  <span className="text-[#8b949e] block">New Remediation Branch (Head)</span>

                  <span className="font-mono text-[#3fb950] flex items-center gap-1.5 font-semibold">

                    <GitBranch className="w-3.5 h-3.5" />

                    {preview.remediationBranchName}

                  </span>

                </div>

              </div>



              {/* File location */}

              <div className="flex items-center gap-2 text-xs font-mono text-[#8b949e] bg-[#0d1117] px-3 py-2 rounded-lg border border-[#30363d]">

                <FileCode className="w-4 h-4 text-[#58a6ff]" />

                <span className="text-[#f0f6fc] font-semibold">{preview.filePath}</span>

                <span>: line {preview.lineNumber}</span>

              </div>



              {/* Masked Side-by-Side Diff */}

              <div className="border border-[#30363d] rounded-xl overflow-hidden text-xs font-mono">

                <div className="bg-[#161b22] px-3 py-1.5 border-b border-[#30363d] text-[#8b949e] font-sans font-medium">

                  Masked Remediation Patch

                </div>

                <div className="p-3 bg-[#0d1117] space-y-2">

                  <div className="flex items-start gap-2 bg-[#da3633]/15 text-[#f85149] p-2.5 rounded-lg border border-[#da3633]/30">

                    <span className="select-none font-bold text-[#f85149]">-</span>

                    <span className="break-all">{preview.originalLineMasked}</span>

                  </div>

                  <div className="flex items-start gap-2 bg-[#238636]/15 text-[#3fb950] p-2.5 rounded-lg border border-[#238636]/30">

                    <span className="select-none font-bold text-[#3fb950]">+</span>

                    <span className="break-all font-semibold">{preview.patchedLine}</span>

                  </div>

                </div>

              </div>



              {submitError && (

                <div className="bg-[#f85149]/10 border border-[#f85149]/30 rounded-xl p-3 text-xs text-[#f85149] flex items-center gap-2">

                  <AlertTriangle className="w-4 h-4 flex-shrink-0" />

                  <span>{submitError}</span>

                </div>

              )}

            </div>

          )}

        </div>



        {/* Footer Actions */}

        <div className="p-4 border-t border-[#30363d] bg-[#0d1117] flex items-center justify-end gap-3">

          <button

            type="button"

            onClick={onClose}

            className="px-4 py-2 text-xs font-medium text-[#c9d1d9] hover:text-white bg-[#21262d] hover:bg-[#30363d] rounded-lg border border-[#30363d] transition-colors"

          >

            {successLink ? 'Close' : 'Cancel'}

          </button>

          {!successLink && (

            <button

              type="button"

              onClick={handleSubmit}

              disabled={!preview || loadingPreview || isSubmitting}

              className="inline-flex items-center gap-2 px-4 py-2 text-xs font-semibold text-white bg-[#238636] hover:bg-[#2ea043] disabled:opacity-50 disabled:cursor-not-allowed rounded-lg shadow-sm transition-colors focus-visible:ring-2 focus-visible:ring-[#238636] focus-visible:outline-none"

            >

              {isSubmitting ? (

                <>

                  <Loader2 className="w-4 h-4 animate-spin" />

                  <span>Opening Pull Request...</span>

                </>

              ) : (

                <>

                  <GitPullRequest className="w-4 h-4" />

                  <span>Confirm & Open Remediation PR</span>

                  <ArrowRight className="w-3.5 h-3.5" />

                </>

              )}

            </button>

          )}

        </div>

      </div>

    </div>

  );

};