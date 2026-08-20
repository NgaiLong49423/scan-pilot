import React from 'react';
import { TrendingDown, TrendingUp, Activity } from 'lucide-react';

interface TrendSparklineProps {
  data: number[];
  isScanned?: boolean;
}

export const TrendSparkline: React.FC<TrendSparklineProps> = ({ data, isScanned = false }) => {
  const hasRealData = isScanned && data && data.length > 0 && data.some((v) => v > 0);
  const min = hasRealData ? Math.min(...data) : 0;
  const max = hasRealData ? Math.max(...data, 1) : 1;
  const height = 64;
  const width = 220;

  const firstVal = data && data.length > 0 ? data[0] : 0;
  const currentVal = data && data.length > 0 ? data[data.length - 1] : 0;
  
  const delta = firstVal > 0 
    ? Math.round(((currentVal - firstVal) / firstVal) * 100) 
    : (currentVal > 0 ? 100 : 0);
    
  const isReduced = delta < 0;
  const isIncreased = delta > 0;

  const points = hasRealData
    ? data
        .map((val, idx) => {
          const x = (idx / (data.length - 1)) * width;
          const y = height - ((val - min) / (max - min || 1)) * (height - 16) - 8;
          return `${x},${y}`;
        })
        .join(' ')
    : '';

  return (
    <div className="h-full flex flex-col justify-between p-5 bg-[#161b22] border border-[#30363d] rounded-2xl shadow-sm hover:border-[#8b949e]/50 transition-all duration-150">
      <div>
        <span className="text-xs font-semibold uppercase tracking-wider text-[#8b949e]">
          Leak Trend (30 Days)
        </span>
        {hasRealData ? (
          <div className="flex items-center gap-1.5 mt-1.5 text-xs font-medium">
            {isReduced ? (
              <div className="flex items-center gap-1 text-[#3fb950]">
                <TrendingDown className="w-3.5 h-3.5" />
                <span>{delta}% Leaks Reduced</span>
              </div>
            ) : isIncreased ? (
              <div className="flex items-center gap-1 text-[#f85149]">
                <TrendingUp className="w-3.5 h-3.5" />
                <span>+{delta}% Leaks Increased</span>
              </div>
            ) : (
              <div className="flex items-center gap-1 text-[#8b949e]">
                <Activity className="w-3.5 h-3.5" />
                <span>0% Net Change</span>
              </div>
            )}
          </div>
        ) : (
          <div className="flex items-center gap-1.5 mt-1.5 text-[#8b949e] text-xs font-medium">
            <Activity className="w-3.5 h-3.5" />
            <span>No Trend History</span>
          </div>
        )}
      </div>

      <div className="relative my-auto py-2 w-full flex items-center justify-center">
        {hasRealData ? (
          <svg className="w-full h-16 overflow-visible" viewBox={`0 0 ${width} ${height}`}>
            <polyline
              fill="none"
              stroke={isIncreased ? '#f85149' : '#58a6ff'}
              strokeWidth="2.5"
              strokeLinecap="round"
              strokeLinejoin="round"
              points={points}
            />
          </svg>
        ) : (
          <div className="w-full flex flex-col items-center justify-center py-3 text-center">
            <div className="w-full border-t border-dashed border-[#30363d] my-2" />
            <span className="text-[11px] text-[#8b949e]">
              Awaiting first scan to establish baseline
            </span>
          </div>
        )}
      </div>

      <div className="flex items-center justify-between text-[11px] text-[#8b949e] border-t border-[#30363d]/60 pt-2 font-mono">
        <span>30d Baseline: {hasRealData ? `${firstVal} leaks` : '—'}</span>
        <span className={hasRealData ? (isIncreased ? 'text-[#f85149] font-semibold' : 'text-[#3fb950] font-semibold') : 'text-[#8b949e]'}>
          Current: {hasRealData ? `${currentVal} leaks` : '—'}
        </span>
      </div>
    </div>
  );
};
