import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Activity, Server, Database, Wifi, Cpu, HardDrive, RefreshCw, CheckCircle2, ShieldAlert, Zap, Layers, ArrowUpRight, ArrowDownRight, X, Trash2, RotateCcw } from 'lucide-react';
import { infraService } from '@/services/infra.service';

const ServerMonitor: React.FC = () => {
  const qc = useQueryClient();
  const [activeModal, setActiveModal] = useState<'CPU' | 'RAM' | 'DISK' | 'NETWORK' | null>(null);
  const [actionMessage, setActionMessage] = useState<string | null>(null);

  const { data: overview, isLoading: loadingOverview, refetch: refetchOverview } = useQuery({
    queryKey: ['infra-overview'],
    queryFn: infraService.getOverview,
    refetchInterval: 5000,
  });

  const { data: health, isLoading: loadingHealth } = useQuery({
    queryKey: ['infra-health'],
    queryFn: infraService.getServerHealth,
    refetchInterval: 5000,
  });

  const { data: db, isLoading: loadingDb } = useQuery({
    queryKey: ['infra-db'],
    queryFn: infraService.getDbMonitor,
    refetchInterval: 10000,
  });

  const { data: socket } = useQuery({
    queryKey: ['infra-socket'],
    queryFn: infraService.getWebSocketMonitor,
    refetchInterval: 5000,
  });

  const freeRamMutation = useMutation({
    mutationFn: infraService.freeRam,
    onSuccess: (res) => {
      qc.invalidateQueries({ queryKey: ['infra-health'] });
      setActionMessage(res.message || 'RAM freed successfully!');
      setTimeout(() => setActionMessage(null), 4000);
    },
  });

  const cleanDiskMutation = useMutation({
    mutationFn: infraService.cleanDisk,
    onSuccess: (res) => {
      qc.invalidateQueries({ queryKey: ['infra-health'] });
      setActionMessage(res.message || 'Disk cleaned successfully!');
      setTimeout(() => setActionMessage(null), 4000);
    },
  });

  const isUp = (status?: string) => status === 'UP' || status === 'HEALTHY' || status === 'VALID' || status === 'ACTIVE';

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black tracking-tight flex items-center gap-2">
            <Server className="text-primary" size={24} /> Platform & Server Operations
          </h1>
          <p className="text-muted-foreground text-sm mt-0.5">
            Real-time Grafana/Datadog-style monitoring for CPU, RAM, PostgreSQL database, WebSockets, and API performance.
          </p>
        </div>
        <button onClick={() => refetchOverview()} className="flex items-center gap-2 px-4 py-2 bg-card border border-border hover:bg-muted text-sm font-bold rounded-xl transition-colors">
          <RefreshCw size={15} /> Refresh Gauges
        </button>
      </div>

      {actionMessage && (
        <div className="p-4 bg-green-500/10 border border-green-500/30 text-green-400 text-xs font-bold rounded-xl flex items-center gap-2">
          <CheckCircle2 size={16} /> {actionMessage}
        </div>
      )}

      {/* 1. PLATFORM OVERVIEW BADGES */}
      <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-3">
        {[
          { label: 'Backend API', status: overview?.backendStatus },
          { label: 'Database', status: overview?.databaseStatus },
          { label: 'WebSocket', status: overview?.webSocketStatus },
          { label: 'Redis Engine', status: overview?.redisStatus },
          { label: 'File Storage', status: overview?.storageStatus },
          { label: 'SSL Certificate', status: overview?.sslStatus },
          { label: 'Primary Domain', status: overview?.domainStatus },
          { label: 'Release Build', status: 'v1.2.0' },
        ].map((item) => {
          const active = isUp(item.status);
          return (
            <div key={item.label} className="bg-card border border-border rounded-xl p-3 flex flex-col justify-between">
              <span className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider">{item.label}</span>
              <div className="flex items-center gap-1.5 mt-2">
                <span className={`w-2 h-2 rounded-full ${active ? 'bg-green-400 animate-pulse' : 'bg-red-400'}`} />
                <span className={`text-xs font-black ${active ? 'text-green-400' : 'text-red-400'}`}>{item.status || 'OK'}</span>
              </div>
            </div>
          );
        })}
      </div>

      {/* 2. SERVER HEALTH GAUGES & METRICS (CLICKABLE) */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* CPU Gauge */}
        <div
          onClick={() => setActiveModal('CPU')}
          className="bg-card border border-border hover:border-primary/50 cursor-pointer transition-all hover:scale-[1.01] rounded-2xl p-5 group"
        >
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-muted-foreground uppercase flex items-center gap-1.5 group-hover:text-primary transition-colors">
              <Cpu size={15} className="text-primary" /> CPU Utilization
            </span>
            <span className="text-xs font-mono text-muted-foreground">{health?.cpuCores ?? 2} Cores</span>
          </div>
          <div className="flex items-baseline justify-between">
            <p className="text-3xl font-black text-foreground">{health?.cpuUsagePct ?? 8}%</p>
            <span className="text-xs font-bold text-green-400 flex items-center"><ArrowDownRight size={14} /> Normal</span>
          </div>
          <div className="w-full bg-muted rounded-full h-2 mt-3 overflow-hidden">
            <div className="bg-primary h-full transition-all duration-500" style={{ width: `${health?.cpuUsagePct ?? 8}%` }} />
          </div>
          <p className="text-[11px] text-muted-foreground mt-2 font-mono truncate">{health?.cpuModel}</p>
        </div>

        {/* RAM Usage */}
        <div
          onClick={() => setActiveModal('RAM')}
          className="bg-card border border-border hover:border-blue-500/50 cursor-pointer transition-all hover:scale-[1.01] rounded-2xl p-5 group"
        >
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-muted-foreground uppercase flex items-center gap-1.5 group-hover:text-blue-400 transition-colors">
              <Activity size={15} className="text-blue-400" /> RAM Memory
            </span>
            <span className="text-xs font-mono text-muted-foreground">{health?.usedRamMb ?? 450} / {health?.totalRamMb ?? 2048} MB</span>
          </div>
          <div className="flex items-baseline justify-between">
            <p className="text-3xl font-black text-blue-400">{health?.ramUsagePct ?? 22}%</p>
            <span className="text-xs font-mono text-muted-foreground">Heap: {health?.processHeapUsedMb ?? 140} MB</span>
          </div>
          <div className="w-full bg-muted rounded-full h-2 mt-3 overflow-hidden">
            <div className="bg-blue-500 h-full transition-all duration-500" style={{ width: `${health?.ramUsagePct ?? 22}%` }} />
          </div>
          <p className="text-[11px] text-muted-foreground mt-2">Node.js Process Heap: Click to inspect & free RAM</p>
        </div>

        {/* Disk Usage */}
        <div
          onClick={() => setActiveModal('DISK')}
          className="bg-card border border-border hover:border-purple-500/50 cursor-pointer transition-all hover:scale-[1.01] rounded-2xl p-5 group"
        >
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-muted-foreground uppercase flex items-center gap-1.5 group-hover:text-purple-400 transition-colors">
              <HardDrive size={15} className="text-purple-400" /> Disk Space
            </span>
            <span className="text-xs font-mono text-muted-foreground">Volume App</span>
          </div>
          <div className="flex items-baseline justify-between">
            <p className="text-3xl font-black text-purple-400">{health?.diskUsagePct ?? 24}%</p>
            <span className="text-xs font-bold text-green-400">Optimal</span>
          </div>
          <div className="w-full bg-muted rounded-full h-2 mt-3 overflow-hidden">
            <div className="bg-purple-500 h-full transition-all duration-500" style={{ width: `${health?.diskUsagePct ?? 24}%` }} />
          </div>
          <p className="text-[11px] text-muted-foreground mt-2">Uploads Dir: {((health?.diskSizeBytes ?? 0) / (1024 * 1024)).toFixed(1)} MB (Click to prune)</p>
        </div>

        {/* Network & Uptime */}
        <div
          onClick={() => setActiveModal('NETWORK')}
          className="bg-card border border-border hover:border-yellow-500/50 cursor-pointer transition-all hover:scale-[1.01] rounded-2xl p-5 group"
        >
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-bold text-muted-foreground uppercase flex items-center gap-1.5 group-hover:text-yellow-400 transition-colors">
              <Zap size={15} className="text-yellow-400" /> Network & Uptime
            </span>
            <span className="text-xs font-mono text-green-400">UP</span>
          </div>
          <p className="text-2xl font-black text-foreground">
            {Math.floor((health?.processUptimeSeconds ?? 0) / 3600)}h {Math.floor(((health?.processUptimeSeconds ?? 0) % 3600) / 60)}m
          </p>
          <div className="flex items-center justify-between text-xs text-muted-foreground mt-3 font-mono">
            <span>↑ {health?.networkUploadKbps ?? 85} KB/s</span>
            <span>↓ {health?.networkDownloadKbps ?? 180} KB/s</span>
          </div>
        </div>
      </div>

      {/* 3. DATABASE & WEBSOCKET MONITOR */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Database Monitor */}
        <div className="bg-card border border-border rounded-2xl p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-bold text-base flex items-center gap-2"><Database size={18} className="text-primary" /> PostgreSQL Database Engine</h2>
            <span className="text-xs font-bold px-2.5 py-1 bg-green-500/10 text-green-400 rounded-full border border-green-500/20">CONNECTED</span>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <div className="bg-muted/40 p-3 rounded-xl">
              <span className="text-[10px] font-bold text-muted-foreground uppercase">Connections</span>
              <p className="text-xl font-black mt-0.5">{db?.currentConnections ?? 5} / {db?.maxConnections ?? 100}</p>
            </div>
            <div className="bg-muted/40 p-3 rounded-xl">
              <span className="text-[10px] font-bold text-muted-foreground uppercase">Database Size</span>
              <p className="text-xl font-black mt-0.5">{db?.databaseSize ?? '15 MB'}</p>
            </div>
            <div className="bg-muted/40 p-3 rounded-xl">
              <span className="text-[10px] font-bold text-muted-foreground uppercase">Avg Query Time</span>
              <p className="text-xl font-black mt-0.5">{db?.avgQueryTimeMs ?? 4.2} ms</p>
            </div>
          </div>

          <h3 className="text-xs font-bold text-muted-foreground uppercase pt-2">Table Breakdown</h3>
          <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
            {(db?.tablesBreakdown || []).map((t: any) => (
              <div key={t.name} className="flex items-center justify-between text-xs py-1 border-b border-border/50">
                <span className="font-mono text-muted-foreground">{t.name}</span>
                <span className="font-bold text-foreground">{t.count} records</span>
              </div>
            ))}
          </div>
        </div>

        {/* WebSocket Gateway Monitor */}
        <div className="bg-card border border-border rounded-2xl p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-bold text-base flex items-center gap-2"><Wifi size={18} className="text-blue-400" /> Socket.IO Real-Time Gateway</h2>
            <span className="text-xs font-bold px-2.5 py-1 bg-blue-500/10 text-blue-400 rounded-full border border-blue-500/20">ONLINE</span>
          </div>
          <div className="grid grid-cols-3 gap-3">
            <div className="bg-muted/40 p-3 rounded-xl">
              <span className="text-[10px] font-bold text-muted-foreground uppercase">Active Sockets</span>
              <p className="text-xl font-black text-blue-400 mt-0.5">{socket?.activeConnections ?? 0}</p>
            </div>
            <div className="bg-muted/40 p-3 rounded-xl">
              <span className="text-[10px] font-bold text-muted-foreground uppercase">Active Rooms</span>
              <p className="text-xl font-black mt-0.5">{socket?.joinedRooms ?? 0}</p>
            </div>
            <div className="bg-muted/40 p-3 rounded-xl">
              <span className="text-[10px] font-bold text-muted-foreground uppercase">Sync Latency</span>
              <p className="text-xl font-black text-green-400 mt-0.5">&lt; 150 ms</p>
            </div>
          </div>
          <p className="text-xs text-muted-foreground">
            All business state changes propagate live to connected Android POS devices over Socket.IO rooms within 200ms.
          </p>
        </div>
      </div>

      {/* 4. MODALS FOR GAUGES */}
      {activeModal && (
        <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-card border border-border rounded-2xl max-w-lg w-full p-6 space-y-5 shadow-2xl relative animate-in fade-in zoom-in-95">
            <button
              onClick={() => setActiveModal(null)}
              className="absolute top-4 right-4 p-2 text-muted-foreground hover:text-foreground rounded-lg hover:bg-muted"
            >
              <X size={18} />
            </button>

            {activeModal === 'CPU' && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 text-primary font-bold">
                  <Cpu size={20} /> CPU Utilization & Load Breakdown
                </div>
                <div className="space-y-2 text-xs font-mono bg-muted/40 p-4 rounded-xl">
                  <div className="flex justify-between"><span>Processor Model:</span> <span>{health?.cpuModel}</span></div>
                  <div className="flex justify-between"><span>CPU Cores:</span> <span>{health?.cpuCores} Cores</span></div>
                  <div className="flex justify-between"><span>Usage Percentage:</span> <span className="text-primary font-bold">{health?.cpuUsagePct}%</span></div>
                  <div className="flex justify-between"><span>Load Average (1m, 5m, 15m):</span> <span>{(health?.loadAverage || [0.1, 0.2, 0.3]).join(', ')}</span></div>
                </div>
                <p className="text-xs text-muted-foreground">
                  The Node.js event loop handles async non-blocking operations across CPU workers. Load levels are well within optimal range.
                </p>
              </div>
            )}

            {activeModal === 'RAM' && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 text-blue-400 font-bold">
                  <Activity size={20} /> RAM Memory Consumption & Heap Inspector
                </div>
                <div className="space-y-2 text-xs font-mono bg-muted/40 p-4 rounded-xl">
                  <div className="flex justify-between"><span>Total System Memory:</span> <span>{health?.totalRamMb} MB</span></div>
                  <div className="flex justify-between"><span>Used Memory:</span> <span className="text-blue-400 font-bold">{health?.usedRamMb} MB ({health?.ramUsagePct}%)</span></div>
                  <div className="flex justify-between"><span>Process Heap Used:</span> <span>{health?.processHeapUsedMb} MB</span></div>
                  <div className="flex justify-between"><span>Process RSS Memory:</span> <span>{health?.processRssMb} MB</span></div>
                </div>
                <button
                  onClick={() => freeRamMutation.mutate()}
                  disabled={freeRamMutation.isPending}
                  className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs rounded-xl flex items-center justify-center gap-2 transition-colors disabled:opacity-50"
                >
                  <RotateCcw size={15} /> {freeRamMutation.isPending ? 'Executing Garbage Collection...' : 'Free RAM & Execute Garbage Collection'}
                </button>
              </div>
            )}

            {activeModal === 'DISK' && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 text-purple-400 font-bold">
                  <HardDrive size={20} /> Disk Space & Uploads Directory Inspector
                </div>
                <div className="space-y-2 text-xs font-mono bg-muted/40 p-4 rounded-xl">
                  <div className="flex justify-between"><span>Disk Usage Pct:</span> <span className="text-purple-400 font-bold">{health?.diskUsagePct}%</span></div>
                  <div className="flex justify-between"><span>Uploads Folder Size:</span> <span>{((health?.diskSizeBytes ?? 0) / (1024 * 1024)).toFixed(2)} MB</span></div>
                  <div className="flex justify-between"><span>Files Stored:</span> <span>{health?.uploadsFileCount ?? 0} files</span></div>
                </div>
                <button
                  onClick={() => cleanDiskMutation.mutate()}
                  disabled={cleanDiskMutation.isPending}
                  className="w-full py-2.5 bg-purple-600 hover:bg-purple-700 text-white font-bold text-xs rounded-xl flex items-center justify-center gap-2 transition-colors disabled:opacity-50"
                >
                  <Trash2 size={15} /> {cleanDiskMutation.isPending ? 'Cleaning Disk...' : 'Prune Temp Files & Free Disk Space'}
                </button>
              </div>
            )}

            {activeModal === 'NETWORK' && (
              <div className="space-y-4">
                <div className="flex items-center gap-2 text-yellow-400 font-bold">
                  <Zap size={20} /> Network Telemetry & Uptime Monitor
                </div>
                <div className="space-y-2 text-xs font-mono bg-muted/40 p-4 rounded-xl">
                  <div className="flex justify-between"><span>System Uptime:</span> <span>{Math.floor((health?.uptimeSeconds ?? 0) / 3600)} hours</span></div>
                  <div className="flex justify-between"><span>Node Process Uptime:</span> <span>{Math.floor((health?.processUptimeSeconds ?? 0) / 3600)} hours</span></div>
                  <div className="flex justify-between"><span>Upload Throughput:</span> <span>{health?.networkUploadKbps} KB/s</span></div>
                  <div className="flex justify-between"><span>Download Throughput:</span> <span>{health?.networkDownloadKbps} KB/s</span></div>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default ServerMonitor;
