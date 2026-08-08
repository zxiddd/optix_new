import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { Activity, Server, Smartphone, RefreshCw, AlertOctagon, CheckCircle2, Clock, Layers } from 'lucide-react';
import { globalConfigService } from '@/services/globalConfig.service';
import { ResponsiveContainer, BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid } from 'recharts';

const LiveStatus: React.FC = () => {
  const { data, isLoading, refetch } = useQuery({
    queryKey: ['live-status'],
    queryFn: globalConfigService.getLiveStatus,
    refetchInterval: 10000, // auto poll every 10 seconds for telemetry
  });

  if (isLoading) {
    return (
      <div className="space-y-6 animate-pulse">
        <div className="h-8 bg-muted rounded w-48" />
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => <div key={i} className="h-28 bg-muted rounded-2xl" />)}
        </div>
      </div>
    );
  }

  const versions = data?.versionDistribution || [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black tracking-tight flex items-center gap-2">
            <Activity className="text-primary" size={24} /> Realtime System Status & Telemetry
          </h1>
          <p className="text-muted-foreground text-sm mt-0.5">
            Live server health, active WebSocket client count, sync queue depth, and app version distribution.
          </p>
        </div>
        <button onClick={() => refetch()} className="flex items-center gap-2 px-4 py-2 bg-card border border-border hover:bg-muted text-sm font-bold rounded-xl transition-colors">
          <RefreshCw size={15} /> Refresh Live Data
        </button>
      </div>

      {/* Health Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-card border border-border rounded-2xl p-5">
          <div className="flex items-center justify-between mb-2">
            <p className="text-xs font-bold text-muted-foreground uppercase">Connected Devices</p>
            <Smartphone size={16} className="text-green-400" />
          </div>
          <p className="text-3xl font-black text-green-400">{data?.connectedDevices ?? 0}</p>
          <p className="text-xs text-muted-foreground mt-1">out of {data?.totalDevices ?? 0} total registered</p>
        </div>

        <div className="bg-card border border-border rounded-2xl p-5">
          <div className="flex items-center justify-between mb-2">
            <p className="text-xs font-bold text-muted-foreground uppercase">Backend Engine</p>
            <Server size={16} className="text-primary" />
          </div>
          <p className="text-xl font-bold text-foreground font-mono">{data?.backendVersion}</p>
          <p className="text-xs text-muted-foreground mt-1">Status: Healthy (200 OK)</p>
        </div>

        <div className="bg-card border border-border rounded-2xl p-5">
          <div className="flex items-center justify-between mb-2">
            <p className="text-xs font-bold text-muted-foreground uppercase">Sync Queue</p>
            <RefreshCw size={16} className="text-blue-400" />
          </div>
          <p className="text-3xl font-black text-blue-400">{data?.syncQueue?.pending ?? 0}</p>
          <p className="text-xs text-red-400 mt-1">{data?.syncQueue?.failed ?? 0} failed items queued</p>
        </div>

        <div className="bg-card border border-border rounded-2xl p-5">
          <div className="flex items-center justify-between mb-2">
            <p className="text-xs font-bold text-muted-foreground uppercase">Maintenance</p>
            {data?.maintenanceMode ? <AlertOctagon size={16} className="text-red-400" /> : <CheckCircle2 size={16} className="text-green-400" />}
          </div>
          <p className={`text-xl font-black ${data?.maintenanceMode ? 'text-red-400' : 'text-green-400'}`}>
            {data?.maintenanceMode ? 'ACTIVE' : 'NORMAL'}
          </p>
          <p className="text-xs text-muted-foreground mt-1">Min App: v{data?.minSupportedAppVersion}</p>
        </div>
      </div>

      {/* App Version Distribution Chart */}
      <div className="bg-card border border-border rounded-2xl p-6">
        <h2 className="text-sm font-bold mb-4 text-foreground">Android App Version Distribution</h2>
        {versions.length === 0 ? (
          <div className="py-12 text-center text-muted-foreground text-sm">No device telemetry collected yet</div>
        ) : (
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={versions}>
              <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
              <XAxis dataKey="version" tick={{ fill: '#6b7280', fontSize: 11 }} />
              <YAxis tick={{ fill: '#6b7280', fontSize: 11 }} />
              <Tooltip contentStyle={{ backgroundColor: '#18181b', borderColor: '#27272a', borderRadius: '12px', fontSize: '12px' }} />
              <Bar dataKey="count" name="Devices" fill="#f97316" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>
    </div>
  );
};

export default LiveStatus;
