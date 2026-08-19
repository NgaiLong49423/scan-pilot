import React from 'react';
import { TrendingDown } from 'lucide-react';

interface TrendSparklineProps {
  data: number[];
}

export const TrendSparkline: React.FC<TrendSparklineProps> = ({ data }) => {
  const min = Math.min(...data);
  const max = Math.max(...data, 1);
  const height = 48;
  const width = 160;

  const points = data
    .map((val, idx) => {
      const x = (idx / (data.length - 1)) * width;
      const y = height - ((val - min) / (max - min || 1)) * (height - 12) - 6;
      return `${x},${y}`;
    })
    .join(' ');

  return (
    <div className="flex flex-col justify-between p-5 bg-slate-900/60 border border-slate-800/80 rounded-2xl shadow-sm hover:border-slate-700/80 transition-all duration-150">
      <div className="flex items-center justify-between gap-4">
        <div>
          <span className="text-xs font-semibold uppercase tracking-wider text-slate-400">
            Leak Trend (30 Days)
          </span>
          <div className="flex items-center gap-1.5 mt-1 text-emerald-400 text-xs font-medium">
            <TrendingDown className="w-3.5 h-3.5" />
            <span>-75% Leaks Reduced</span>
          </div>
        </div>
      </div>

      <div className="relative mt-3 h-12 w-full flex items-end">
        <svg className="w-full h-12 overflow-visible" viewBox={`0 0 ${width} ${height}`}>
          <defs>
            <linearGradient id="gradientTrend" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#6366F1" stopOpacity="0.4" />
              <stop offset="100%" stopColor="#6366F1" stopOpacity="0.0" />
            </linearGradient>
          </defs>
          <polyline
            fill="none"
            stroke="#6366F1"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
            points={points}
          />
        </svg>
      </div>
    </div>
  );
};
