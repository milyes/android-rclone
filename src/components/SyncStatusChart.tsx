import React from 'react';
import { SyncLog } from '../types';

interface SyncStatusChartProps {
  syncLogs: SyncLog[];
}

export const SyncStatusChart: React.FC<SyncStatusChartProps> = ({ syncLogs }) => {
  // Process logs to get the latest progress for the last 5-7 unique sync tasks
  const taskMap = new Map<string, SyncLog>();
  syncLogs.forEach(log => {
    const key = `${log.commandExecuted}_${log.recordingId ?? 0}`;
    const existing = taskMap.get(key);
    if (!existing || log.timestamp > existing.timestamp) {
      taskMap.set(key, log);
    }
  });

  const latestTasks = Array.from(taskMap.values())
    .sort((a, b) => b.timestamp - a.timestamp)
    .slice(0, 7)
    .reverse();

  return (
    <div className="p-4 rounded-2xl bg-[#1E293B] border border-slate-700/80 shadow-md my-2">
      <div className="flex items-center justify-between mb-3">
        <h3 className="font-bold text-sm text-[#00E5FF]">Progression des Transferts</h3>
        <span className="text-[10px] font-mono text-slate-400">
          {latestTasks.length} tâche(s) récente(s)
        </span>
      </div>

      {latestTasks.length === 0 ? (
        <div className="h-28 flex items-center justify-center text-xs text-slate-400">
          Aucun transfert récent.
        </div>
      ) : (
        <div className="space-y-2">
          {/* Bar Chart Visualization */}
          <div className="h-32 flex items-end gap-2 pt-4 px-2 pb-2 bg-slate-900/60 rounded-xl border border-slate-800">
            {latestTasks.map((task, idx) => {
              const heightPercent = Math.max(8, task.progressPercent);
              const isSuccess = task.status === 'SUCCESS';
              const isFailed = task.status === 'FAILED';

              let barColor = 'bg-[#00E5FF]';
              if (isSuccess) barColor = 'bg-emerald-400';
              if (isFailed) barColor = 'bg-rose-400';

              return (
                <div key={idx} className="flex-1 flex flex-col items-center gap-1 group relative">
                  {/* Tooltip */}
                  <div className="absolute -top-7 opacity-0 group-hover:opacity-100 transition-opacity bg-slate-800 text-[10px] font-mono text-slate-200 px-1.5 py-0.5 rounded shadow pointer-events-none whitespace-nowrap z-10">
                    {task.progressPercent}% • {task.status}
                  </div>

                  {/* Percentage label above bar */}
                  <span className="text-[9px] font-mono text-slate-400">
                    {task.progressPercent}%
                  </span>

                  {/* Bar */}
                  <div
                    style={{ height: `${heightPercent}%` }}
                    className={`w-full max-w-[28px] rounded-t-md transition-all ${barColor}`}
                  />

                  {/* Index / task label */}
                  <span className="text-[9px] font-mono text-slate-500 truncate w-full text-center">
                    T{idx + 1}
                  </span>
                </div>
              );
            })}
          </div>

          <div className="flex items-center justify-between text-[10px] text-slate-400 px-1 pt-1 font-mono">
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-emerald-400 inline-block" /> Succès (100%)
            </span>
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-[#00E5FF] inline-block" /> En cours
            </span>
            <span className="flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-rose-400 inline-block" /> Échec
            </span>
          </div>
        </div>
      )}
    </div>
  );
};
