import React from 'react';

interface HealthGaugeProps {
  score: number;
  grade: string;
}

export const HealthGauge: React.FC<HealthGaugeProps> = ({ score, grade }) => {
  const radius = 40;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (score / 100) * circumference;

  const getScoreColor = () => {
    if (score >= 90) return 'text-[#3fb950] stroke-[#238636]';
    if (score >= 70) return 'text-[#e3b341] stroke-[#d29922]';
    return 'text-[#f85149] stroke-[#da3633]';
  };

  const getBadgeStyle = () => {
    if (score >= 90) return 'bg-[#238636]/15 text-[#3fb950] border-[#238636]/30';
    if (score >= 70) return 'bg-[#d29922]/15 text-[#e3b341] border-[#d29922]/30';
    return 'bg-[#da3633]/15 text-[#f85149] border-[#da3633]/30';
  };

  return (
    <div className="h-full flex flex-col items-center justify-between p-5 bg-[#161b22] border border-[#30363d] rounded-2xl shadow-sm hover:border-[#8b949e]/50 transition-all duration-150">
      <span className="text-xs font-semibold uppercase tracking-wider text-[#8b949e]">
        Repository Health
      </span>
      
      <div className="relative flex items-center justify-center w-28 h-28 my-auto">
        <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
          <circle
            cx="50"
            cy="50"
            r={radius}
            className="stroke-[#21262d]"
            strokeWidth="8"
            fill="transparent"
          />
          <circle
            cx="50"
            cy="50"
            r={radius}
            className={`transition-all duration-1000 ease-out ${getScoreColor()}`}
            strokeWidth="8"
            strokeDasharray={circumference}
            strokeDashoffset={strokeDashoffset}
            strokeLinecap="round"
            fill="transparent"
          />
        </svg>

        <div className="absolute flex flex-col items-center justify-center text-center">
          <span className="text-2xl font-bold tracking-tight text-[#f0f6fc] tabular-nums">
            {score}
          </span>
          <span className="text-[10px] text-[#8b949e] font-medium -mt-1">/100</span>
        </div>
      </div>

      <div className={`px-2.5 py-1 rounded-full text-xs font-medium border ${getBadgeStyle()}`}>
        {grade}
      </div>
    </div>
  );
};
