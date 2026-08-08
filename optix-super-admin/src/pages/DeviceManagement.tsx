import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Smartphone, Search, RefreshCw, LogOut, Battery, Wifi, ShieldAlert, Monitor } from 'lucide-react';
import { deviceService } from '@/services/device.service';
import ConfirmDialog from '@/components/shared/ConfirmDialog';
import { motion } from 'framer-motion';

const DeviceManagement: React.FC = () => {
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(1);
  const [logoutId, setLogoutId] = useState<string | null>(null);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['devices', search, statusFilter, page],
    queryFn: () => deviceService.getDevices({ search, connectionStatus: statusFilter, page, limit: 50 }),
    refetchInterval: 15000,
  });

  const logoutMutation = useMutation({
    mutationFn: deviceService.remoteLogout,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['devices'] });
      setLogoutId(null);
    },
  });

  const devices: any[] = data?.items ?? [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black tracking-tight flex items-center gap-2">
            <Smartphone className="text-primary" size={24} /> Connected Devices Fleet
          </h1>
          <p className="text-muted-foreground text-sm mt-0.5">
            Monitor real-time telemetry, app versions, battery levels, and execute remote logouts.
          </p>
        </div>
        <button onClick={() => refetch()} className="p-2.5 rounded-xl border border-border hover:bg-muted transition-colors">
          <RefreshCw size={16} />
        </button>
      </div>

      {/* Filter Bar */}
      <div className="bg-card border border-border rounded-2xl p-4 flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={15} />
          <input
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(1); }}
            placeholder="Search by device model, IP, business..."
            className="w-full bg-muted/50 border border-border rounded-xl py-2.5 pl-9 pr-4 text-sm focus:outline-none"
          />
        </div>
        <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}
          className="bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none">
          <option value="">All Statuses</option>
          <option value="ONLINE">ONLINE</option>
          <option value="OFFLINE">OFFLINE</option>
        </select>
      </div>

      {/* Devices Table */}
      <div className="bg-card border border-border rounded-2xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/30">
                {['Device Name / Model', 'Business', 'OS & App Version', 'Battery', 'Screen', 'IP Address', 'Status', 'Last Seen', 'Actions'].map(h => (
                  <th key={h} className="px-4 py-3.5 text-left text-xs font-bold text-muted-foreground uppercase tracking-wider whitespace-nowrap">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                Array.from({ length: 6 }).map((_, i) => (
                  <tr key={i} className="border-b border-border/50 animate-pulse">
                    {Array.from({ length: 9 }).map((_, j) => <td key={j} className="px-4 py-4"><div className="h-3 bg-muted rounded w-20" /></td>)}
                  </tr>
                ))
              ) : devices.length === 0 ? (
                <tr>
                  <td colSpan={9} className="px-4 py-16 text-center text-muted-foreground">
                    No active devices registered yet
                  </td>
                </tr>
              ) : devices.map((d: any, i: number) => {
                const isOnline = d.connectionStatus === 'ONLINE';
                return (
                  <motion.tr key={d.id} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: i * 0.02 }}
                    className="border-b border-border/50 hover:bg-muted/10 transition-colors">
                    <td className="px-4 py-3.5">
                      <p className="font-bold text-foreground">{d.deviceName}</p>
                      <p className="text-xs text-muted-foreground font-mono">{d.deviceModel || 'Android Device'}</p>
                    </td>
                    <td className="px-4 py-3.5 font-semibold">{d.business?.name || '—'}</td>
                    <td className="px-4 py-3.5 text-xs text-muted-foreground">
                      Android {d.androidVersion || '13'} • <span className="text-primary font-bold">v{d.appVersion || '1.0.0'}</span>
                    </td>
                    <td className="px-4 py-3.5">
                      <div className="flex items-center gap-1 text-xs font-bold">
                        <Battery size={14} className={d.batteryLevel && d.batteryLevel < 20 ? 'text-red-400' : 'text-green-400'} />
                        {d.batteryLevel ?? 100}%
                      </div>
                    </td>
                    <td className="px-4 py-3.5 text-xs font-mono text-muted-foreground">{d.currentScreen || 'Main'}</td>
                    <td className="px-4 py-3.5 text-xs font-mono text-muted-foreground">{d.ipAddress || '—'}</td>
                    <td className="px-4 py-3.5">
                      <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-bold border ${
                        isOnline ? 'bg-green-500/15 text-green-400 border-green-500/30' : 'bg-zinc-500/15 text-zinc-400 border-zinc-500/30'
                      }`}>
                        <span className="w-1.5 h-1.5 rounded-full bg-current" />
                        {d.connectionStatus}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-xs text-muted-foreground whitespace-nowrap">
                      {new Date(d.lastSeen).toLocaleTimeString()}
                    </td>
                    <td className="px-4 py-3.5">
                      <button
                        onClick={() => setLogoutId(d.id)}
                        className="flex items-center gap-1 px-2.5 py-1 text-xs font-bold bg-red-500/10 text-red-400 border border-red-500/20 hover:bg-red-500/20 rounded-lg transition-colors"
                      >
                        <LogOut size={12} /> Logout
                      </button>
                    </td>
                  </motion.tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      <ConfirmDialog
        open={!!logoutId}
        title="Remote Logout Device"
        description="This will immediately revoke session tokens and trigger a forced logout on the selected device over WebSocket."
        confirmLabel="Logout Device"
        confirmVariant="danger"
        loading={logoutMutation.isPending}
        onConfirm={() => logoutId && logoutMutation.mutate(logoutId)}
        onCancel={() => setLogoutId(null)}
      />
    </div>
  );
};

export default DeviceManagement;
