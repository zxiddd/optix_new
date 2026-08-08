import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { AlertOctagon, Box, RefreshCw, Activity, Layers, CheckCircle2, ShieldAlert, Cpu, HardDrive } from 'lucide-react';
import { infraService } from '@/services/infra.service';
import { motion } from 'framer-motion';

const SystemAlerts: React.FC = () => {
  const qc = useQueryClient();

  const { data: containers, isLoading: loadingContainers, refetch: refetchContainers } = useQuery({
    queryKey: ['infra-containers'],
    queryFn: infraService.getContainers,
    refetchInterval: 5000,
  });

  const { data: alerts } = useQuery({
    queryKey: ['infra-alerts'],
    queryFn: infraService.getAlerts,
    refetchInterval: 5000,
  });

  const { data: feed } = useQuery({
    queryKey: ['infra-feed'],
    queryFn: infraService.getLiveFeed,
    refetchInterval: 5000,
  });

  const { data: services } = useQuery({
    queryKey: ['infra-bg-services'],
    queryFn: infraService.getBackgroundServices,
    refetchInterval: 10000,
  });

  const restartMutation = useMutation({
    mutationFn: infraService.restartContainer,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['infra-containers'] });
    },
  });

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black tracking-tight flex items-center gap-2">
            <AlertOctagon className="text-primary" size={24} /> Containers, Background Services & Live Stream
          </h1>
          <p className="text-muted-foreground text-sm mt-0.5">
            Monitor Docker container fleet, PM2 background workers, live system alerts, and platform event activity stream.
          </p>
        </div>
        <button onClick={() => refetchContainers()} className="p-2.5 rounded-xl border border-border hover:bg-muted transition-colors">
          <RefreshCw size={16} />
        </button>
      </div>

      {/* Docker Fleet & PM2 Containers */}
      <div className="bg-card border border-border rounded-2xl p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="font-bold text-base flex items-center gap-2">
            <Box size={18} className="text-primary" /> Docker & PM2 Container Fleet
          </h2>
          <span className="text-xs font-mono text-muted-foreground">{containers?.length ?? 0} active containers</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/30">
                {['Container ID / Name', 'Status', 'CPU Usage', 'Memory RAM', 'Restarts', 'Uptime', 'Actions'].map(h => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-bold text-muted-foreground uppercase">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {loadingContainers ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <tr key={i} className="border-b border-border/50 animate-pulse">
                    <td colSpan={7} className="px-4 py-4"><div className="h-4 bg-muted rounded w-full" /></td>
                  </tr>
                ))
              ) : (containers || []).map((c: any) => (
                <tr key={c.id} className="border-b border-border/50 hover:bg-muted/10 transition-colors">
                  <td className="px-4 py-3.5 font-bold font-mono text-xs">
                    <p className="text-foreground">{c.name}</p>
                    <p className="text-[10px] text-muted-foreground">{c.id}</p>
                  </td>
                  <td className="px-4 py-3.5">
                    <span className="px-2 py-0.5 text-xs font-bold bg-green-500/15 text-green-400 border border-green-500/30 rounded-full">
                      {c.status}
                    </span>
                  </td>
                  <td className="px-4 py-3.5 font-mono text-xs font-bold text-primary">{c.cpuPct}%</td>
                  <td className="px-4 py-3.5 font-mono text-xs text-muted-foreground">{c.memoryMb} MB</td>
                  <td className="px-4 py-3.5 text-xs font-mono">{c.restartCount}</td>
                  <td className="px-4 py-3.5 text-xs text-muted-foreground">{c.uptime}</td>
                  <td className="px-4 py-3.5">
                    <button
                      onClick={() => restartMutation.mutate(c.id)}
                      disabled={restartMutation.isPending}
                      className="flex items-center gap-1 px-2.5 py-1 text-xs font-bold bg-orange-500/10 text-orange-400 border border-orange-500/20 hover:bg-orange-500/20 rounded-lg transition-colors"
                    >
                      <RefreshCw size={12} /> Restart
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Background Services & Live Event Stream Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Background Workers */}
        <div className="bg-card border border-border rounded-2xl p-6 space-y-4">
          <h2 className="font-bold text-base flex items-center gap-2">
            <Layers size={18} className="text-blue-400" /> Background Services & Workers
          </h2>
          <div className="space-y-2">
            {(services || []).map((s: any) => (
              <div key={s.name} className="p-3 rounded-xl border border-border bg-muted/20 flex items-center justify-between text-xs">
                <div>
                  <p className="font-bold text-foreground">{s.name}</p>
                  <p className="text-[11px] text-muted-foreground mt-0.5">Uptime: {s.uptime}</p>
                </div>
                <span className="px-2.5 py-0.5 font-bold rounded-full bg-green-500/15 text-green-400 border border-green-500/30">
                  {s.status}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Live Activity Feed */}
        <div className="bg-card border border-border rounded-2xl p-6 space-y-4">
          <h2 className="font-bold text-base flex items-center gap-2">
            <Activity size={18} className="text-primary" /> Live Platform Activity Feed
          </h2>
          <div className="space-y-3 font-mono text-xs">
            {(feed || []).length === 0 ? (
              <p className="text-muted-foreground py-8 text-center">No recent activity</p>
            ) : (feed || []).map((item: any) => (
              <div key={item.id} className="p-3 rounded-xl border border-border/50 bg-muted/30 flex items-start justify-between">
                <div>
                  <span className="px-1.5 py-0.5 bg-primary/10 text-primary text-[10px] font-bold rounded mr-2">{item.type}</span>
                  <span className="text-foreground font-semibold">{item.text}</span>
                </div>
                <span className="text-[10px] text-muted-foreground shrink-0">{new Date(item.time).toLocaleTimeString()}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

export default SystemAlerts;
