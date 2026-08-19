export function CardSkeleton() {
  return (
    <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5 space-y-4 animate-pulse">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-20 h-6 bg-slate-800 rounded-md"></div>
          <div className="w-28 h-6 bg-slate-800 rounded-md"></div>
          <div className="w-32 h-6 bg-slate-800 rounded-md"></div>
        </div>
        <div className="w-24 h-6 bg-slate-800 rounded-md"></div>
      </div>
      <div className="space-y-2">
        <div className="w-3/4 h-5 bg-slate-800 rounded"></div>
        <div className="w-1/2 h-4 bg-slate-800/60 rounded"></div>
      </div>
      <div className="h-20 bg-slate-950/80 rounded-lg border border-slate-800/80 p-3">
        <div className="space-y-2">
          <div className="w-full h-3 bg-slate-800/50 rounded"></div>
          <div className="w-5/6 h-3 bg-slate-800/50 rounded"></div>
        </div>
      </div>
    </div>
  );
}

export function TableRowSkeleton() {
  return (
    <tr className="border-b border-slate-800/60 animate-pulse">
      <td className="py-3 px-4"><div className="h-4 bg-slate-800 rounded w-48"></div></td>
      <td className="py-3 px-4"><div className="h-4 bg-slate-800 rounded w-20"></div></td>
      <td className="py-3 px-4"><div className="h-4 bg-slate-800 rounded w-16"></div></td>
      <td className="py-3 px-4"><div className="h-4 bg-slate-800 rounded w-28"></div></td>
      <td className="py-3 px-4"><div className="h-4 bg-slate-800 rounded w-32"></div></td>
    </tr>
  );
}

export function MetricSkeleton() {
  return (
    <div className="bg-slate-900/60 border border-slate-800 rounded-xl p-5 animate-pulse space-y-3">
      <div className="h-4 bg-slate-800 rounded w-24"></div>
      <div className="h-8 bg-slate-800 rounded w-16"></div>
      <div className="h-3 bg-slate-800/60 rounded w-32"></div>
    </div>
  );
}
