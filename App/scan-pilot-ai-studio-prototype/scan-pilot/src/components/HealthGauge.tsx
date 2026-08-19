import React from 'react';

interface HealthGaugeProps {
  score: number;
  grade: string;
}

export const HealthGauge: React.FC<HealthGaugeProps> = ({ score, grade }) => {
  const radius = 42;
  const circumference = 2 * Math.PI * radius;
  const strokeDashoffset = circumference - (score / 100) * circumference;

  const getScoreColor = () => {
    if (score >= 90) return 'text-emerald-400 stroke-emerald-400';
    if (score >= 70) return 'text-amber-400 stroke-amber-400';
    return 'text-rose-500 stroke-rose-500';
  };

  const getBadgeStyle = () => {
    if (score >= 90) return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
    if (score >= 70) return 'bg-amber-500/10 text-amber-400 border-amber-500/20';
    return 'bg-rose-500/10 text-rose-400 border-rose-500/20';
  };

  return (
    <div className="flex flex-col items-center justify-center p-5 bg-slate-900/60 border border-slate-800/80 rounded-2xl shadow-sm hover:border-slate-700/80 transition-all duration-150">
      <span className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-3">
        Repository Health
      </span>
      
      <div className="relative flex items-center justify-center w-28 h-28">
        <svg className="w-full h-full -rotate-90" viewBox="0 0 100 100">
          <circle
            cx="50"
            cy="50"
            r={radius}
            className="stroke-slate-800"
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
          <span className="text-2xl font-bold tracking-tight text-white tabular-nums">
            {score}
          </span>
          <span className="text-[10px] text-slate-500 font-medium -mt-1">/100</span>
        </div>
      </div>

      <div className={`mt-3 px-2.5 py-1 rounded-full text-xs font-medium border ${getBadgeStyle()}`}>
        {grade}
      </div>
    </div>
  );
};
