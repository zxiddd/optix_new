import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Terminal, Search, Pause, Play, Trash2, RefreshCw, AlertTriangle, ShieldAlert, CheckCircle2, Copy } from 'lucide-react';
import { infraService } from '@/services/infra.service';
import { motion } from 'framer-motion';

const ContainerLogs: React.FC = () => {
  const [filter, setFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [isPaused, setIsPaused] = useState(false);

  const { data: logs, refetch: refetchLogs } = useQuery({
    queryKey: ['infra-logs', filter, search],
    queryFn: () => infraService.getRealtimeLogs({ filter, search, limit: 100 }),
    refetchInterval: isPaused ? false : 3000,
  });

  const { data: errors } = useQuery({
    queryKey: ['infra-errors'],
    queryFn: infraService.getErrorTracking,
    refetchInterval: 10000,
  });

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black tracking-tight flex items-center gap-2">
            <Terminal className="text-primary" size={24} /> Realtime System Log Center & Error Tracker
          </h1>
          <p className="text-muted-foreground text-sm mt-0.5">
            Stream NestJS backend, WebSocket gateway, payment webhooks, and auth security logs in real-time.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setIsPaused(!isPaused)}
            className={`flex items-center gap-1.5 px-3 py-2 text-xs font-bold rounded-xl border transition-colors ${
              isPaused ? 'bg-yellow-500/10 text-yellow-400 border-yellow-500/30' : 'bg-card border-border hover:bg-muted'
            }`}
          >
            {isPaused ? <Play size={14} /> : <Pause size={14} />}
            {isPaused ? 'Resume Stream' : 'Pause Stream'}
          </button>
          <button onClick={() => refetchLogs()} className="p-2 rounded-xl border border-border hover:bg-muted">
            <RefreshCw size={15} />
          </button>
        </div>
      </div>

      {/* Log Console Controls */}
      <div className="bg-card border border-border rounded-2xl p-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2 flex-1 min-w-[240px]">
          <Search size={15} className="text-muted-foreground" />
          <input
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Search log messages..."
            className="w-full bg-muted/50 border border-border rounded-xl px-3 py-2 text-xs focus:outline-none"
          />
        </div>
        <div className="flex items-center gap-1.5 overflow-x-auto">
          {['ALL', 'NESTJS', 'WEBSOCKET', 'PAYMENT', 'AUTH', 'SYNC'].map(sys => (
            <button
              key={sys}
              onClick={() => setFilter(sys)}
              className={`px-3 py-1.5 text-xs font-bold rounded-xl border transition-colors ${
                filter === sys ? 'bg-primary text-black border-primary' : 'border-border text-muted-foreground hover:bg-muted'
              }`}
            >
              {sys}
            </button>
          ))}
        </div>
      </div>

      {/* Terminal Log Console */}
      <div className="bg-zinc-950 border border-zinc-800 rounded-2xl p-4 font-mono text-xs shadow-2xl overflow-hidden">
        <div className="flex items-center justify-between border-b border-zinc-800 pb-3 mb-3 text-zinc-500 text-[11px]">
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-red-500/80" />
            <span className="w-2.5 h-2.5 rounded-full bg-yellow-500/80" />
            <span className="w-2.5 h-2.5 rounded-full bg-green-500/80" />
            <span className="ml-2 font-bold text-zinc-400">optix-backend-staging.log</span>
          </div>
          <span>{logs?.length ?? 0} log entries</span>
        </div>

        <div className="space-y-2 max-h-[450px] overflow-y-auto custom-scrollbar pr-2">
          {logs?.length === 0 ? (
            <p className="text-zinc-600 py-8 text-center">No logs matching filter criteria</p>
          ) : logs?.map((log: any, i: number) => {
            const isWarn = log.level === 'WARN';
            const isErr = log.level === 'ERROR';

            return (
              <div key={i} className="flex items-start gap-2 hover:bg-zinc-900/60 p-1 rounded transition-colors">
                <span className="text-zinc-600 text-[10px] shrink-0">[{new Date(log.time).toLocaleTimeString()}]</span>
                <span className={`px-1.5 py-0.2 text-[10px] font-bold rounded shrink-0 ${
                  isErr ? 'bg-red-500/20 text-red-400' : isWarn ? 'bg-yellow-500/20 text-yellow-400' : 'bg-zinc-800 text-zinc-400'
                }`}>
                  {log.level}
                </span>
                <span className="text-primary font-bold shrink-0">[{log.service}]</span>
                <span className={isErr ? 'text-red-300' : isWarn ? 'text-yellow-200' : 'text-zinc-300'}>
                  {log.message}
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {/* Exception Tracker */}
      <div className="bg-card border border-border rounded-2xl p-6 space-y-4">
        <h2 className="font-bold text-base flex items-center gap-2">
          <AlertTriangle className="text-yellow-400" size={18} /> Exception & Error Tracker
        </h2>
        <div className="space-y-3">
          {(errors || []).map((err: any) => (
            <div key={err.id} className="p-4 rounded-xl border border-border bg-muted/20 flex items-center justify-between">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="font-mono font-bold text-xs text-red-400">{err.exception}</span>
                  <span className="px-2 py-0.5 text-[10px] font-bold rounded-full bg-yellow-500/10 text-yellow-400 border border-yellow-500/20">
                    Freq: {err.frequency}x
                  </span>
                </div>
                <p className="text-xs font-mono text-muted-foreground">{err.message}</p>
              </div>
              <span className={`px-2.5 py-1 text-xs font-bold rounded-full ${
                err.status === 'RESOLVED' ? 'bg-green-500/15 text-green-400' : 'bg-red-500/15 text-red-400'
              }`}>
                {err.status}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default ContainerLogs;
