import React from 'react';
import { TrendingDown } from 'lucide-react';

interface TrendSparklineProps {
  data: number[];
}

export const TrendSparkline: React.FC<TrendSparklineProps> = ({ data }) => {
  const min = Math.min(...data);
  const max = Math.max(...data, 1);
  const height = 64;
  const width = 220;

  const points = data
    .map((val, idx) => {
      const x = (idx / (data.length - 1)) * width;
      const y = height - ((val - min) / (max - min || 1)) * (height - 16) - 8;
      return `${x},${y}`;
    })
    .join(' ');

  return (
    <div className="h-full flex flex-col justify-between p-5 bg-[#161b22] border border-[#30363d] rounded-2xl shadow-sm hover:border-[#8b949e]/50 transition-all duration-150">
      <div>
        <span className="text-xs font-semibold uppercase tracking-wider text-[#8b949e]">
          Leak Trend (30 Days)
        </span>
        <div className="flex items-center gap-1.5 mt-1.5 text-[#3fb950] text-xs font-medium">
          <TrendingDown className="w-3.5 h-3.5" />
          <span>-75% Leaks Reduced</span>
        </div>
      </div>

      <div className="relative my-auto py-2 w-full flex items-end">
        <svg className="w-full h-16 overflow-visible" viewBox={`0 0 ${width} ${height}`}>
          <polyline
            fill="none"
            stroke="#58a6ff"
            strokeWidth="2.5"
            strokeLinecap="round"
            strokeLinejoin="round"
            points={points}
          />
        </svg>
      </div>

      <div className="flex items-center justify-between text-[11px] text-[#8b949e] border-t border-[#30363d]/60 pt-2 font-mono">
        <span>30d ago: 12 leaks</span>
        <span className="text-[#3fb950] font-semibold">Today: 3 leaks</span>
      </div>
    </div>
  );
};
