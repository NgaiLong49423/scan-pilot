import { AlertCircle, RefreshCw, X } from 'lucide-react';

interface ErrorBannerProps {
  message: string;
  onRetry?: () => void;
  onDismiss?: () => void;
  retryText?: string;
}

export function ErrorBanner({
  message,
  onRetry,
  onDismiss,
  retryText = 'Retry',
}: ErrorBannerProps) {
  return (
    <div className="bg-rose-950/40 border border-rose-800/60 rounded-xl p-4 flex items-start justify-between gap-3 text-rose-200 shadow-sm animate-in fade-in duration-200">
      <div className="flex items-start gap-3">
        <AlertCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
        <div>
          <p className="text-sm font-medium text-rose-200 leading-snug">{message}</p>
        </div>
      </div>
      <div className="flex items-center gap-2 shrink-0">
        {onRetry && (
          <button
            onClick={onRetry}
            className="flex items-center gap-1.5 bg-rose-900/60 hover:bg-rose-800/80 text-rose-100 text-xs font-semibold px-3 py-1.5 rounded-md border border-rose-700/60 transition-colors focus:outline-none focus:ring-2 focus:ring-rose-500/50"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            {retryText}
          </button>
        )}
        {onDismiss && (
          <button
            onClick={onDismiss}
            className="text-rose-400 hover:text-rose-200 p-1 rounded transition-colors focus:outline-none"
            aria-label="Dismiss error"
          >
            <X className="w-4 h-4" />
          </button>
        )}
      </div>
    </div>
  );
}
