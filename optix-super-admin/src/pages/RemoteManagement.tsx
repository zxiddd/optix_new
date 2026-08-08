import React, { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Radio, RefreshCw, LogOut, Database, Bell, Terminal, Wifi, ShieldAlert, Cpu, HardDrive } from 'lucide-react';
import { remoteCommandService } from '@/services/remoteCommand.service';
import ConfirmDialog from '@/components/shared/ConfirmDialog';
import { motion } from 'framer-motion';

const COMMANDS = [
  { action: 'FORCE_SYNC', label: 'Force Sync', desc: 'Trigger immediate incremental sync', icon: RefreshCw, variant: 'primary' },
  { action: 'FORCE_FULL_SYNC', label: 'Force Full Sync', desc: 'Full cloud data pull & merge', icon: RefreshCw, variant: 'primary' },
  { action: 'REFRESH_SUBSCRIPTION', label: 'Refresh Subscription', desc: 'Force subscription state pull', icon: Cpu, variant: 'primary' },
  { action: 'REFRESH_FEATURE_FLAGS', label: 'Refresh Feature Flags', desc: 'Re-resolve & push remote flags', icon: Terminal, variant: 'primary' },
  { action: 'RESTART_SOCKET', label: 'Restart Socket', desc: 'Disconnect & reconnect WebSocket', icon: Wifi, variant: 'warning' },
  { action: 'RECONNECT_WEBSOCKET', label: 'Reconnect WebSocket', desc: 'Reconnect active socket connection', icon: Wifi, variant: 'primary' },
  { action: 'LOGOUT_ALL_DEVICES', label: 'Logout All Devices', desc: 'Revoke sessions & force app logout', icon: LogOut, variant: 'danger' },
  { action: 'CLEAR_CACHE', label: 'Clear Local Cache', desc: 'Purge image & memory cache', icon: HardDrive, variant: 'warning' },
  { action: 'REBUILD_LOCAL_DB', label: 'Rebuild Local DB', desc: 'Clear local SQLite DB & restore cloud data', icon: Database, variant: 'danger' },
  { action: 'SEND_TEST_NOTIFICATION', label: 'Test Notification', desc: 'Send test local notification to client', icon: Bell, variant: 'primary' },
  { action: 'RESTART_BACKGROUND_SYNC', label: 'Restart Worker', desc: 'Reschedule WorkManager background task', icon: RefreshCw, variant: 'primary' },
];

const RemoteManagement: React.FC = () => {
  const [businessId, setBusinessId] = useState('');
  const [deviceId, setDeviceId] = useState('');
  const [pendingCmd, setPendingCmd] = useState<any | null>(null);
  const [logs, setLogs] = useState<Array<{ time: string; text: string; success: boolean }>>([]);

  const commandMutation = useMutation({
    mutationFn: remoteCommandService.sendCommand,
    onSuccess: (res) => {
      setLogs(l => [{
        time: new Date().toLocaleTimeString(),
        text: `Successfully dispatched remote command '${res.command}' to business ${res.businessId}`,
        success: true,
      }, ...l]);
      setPendingCmd(null);
    },
    onError: (err: any) => {
      setLogs(l => [{
        time: new Date().toLocaleTimeString(),
        text: `Failed to dispatch command: ${err.message}`,
        success: false,
      }, ...l]);
      setPendingCmd(null);
    },
  });

  const handleCommandClick = (cmd: any) => {
    if (!businessId.trim()) {
      alert('Please enter a valid Business ID first.');
      return;
    }
    if (cmd.variant === 'danger' || cmd.variant === 'warning') {
      setPendingCmd(cmd);
    } else {
      commandMutation.mutate({ command: cmd.action, businessId: businessId.trim(), deviceId: deviceId.trim() || undefined });
    }
  };

  return (
    <div className="space-y-6 max-w-5xl">
      <div>
        <h1 className="text-2xl font-black tracking-tight flex items-center gap-2">
          <Radio className="text-primary" size={24} /> Remote Command Console
        </h1>
        <p className="text-muted-foreground text-sm mt-0.5">
          Dispatch real-time commands to connected Android client devices via WebSocket without requiring an app update.
        </p>
      </div>

      {/* Target Input Card */}
      <div className="bg-card border border-border rounded-2xl p-6 space-y-4">
        <h2 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Target Client Details</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="text-xs font-semibold text-muted-foreground block mb-1">Business ID *</label>
            <input
              type="text"
              value={businessId}
              onChange={e => setBusinessId(e.target.value)}
              placeholder="e.g. 4118279f-1681-47cb-aff0-a6c35a00b30d"
              className="w-full bg-muted/50 border border-border rounded-xl px-4 py-2.5 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <div>
            <label className="text-xs font-semibold text-muted-foreground block mb-1">Target Device ID (Optional)</label>
            <input
              type="text"
              value={deviceId}
              onChange={e => setDeviceId(e.target.value)}
              placeholder="Leave empty for ALL connected devices"
              className="w-full bg-muted/50 border border-border rounded-xl px-4 py-2.5 text-sm font-mono focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
        </div>
      </div>

      {/* Commands Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {COMMANDS.map((cmd) => {
          const Icon = cmd.icon;
          const isDanger = cmd.variant === 'danger';
          const isWarning = cmd.variant === 'warning';

          return (
            <motion.button
              key={cmd.action}
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.98 }}
              onClick={() => handleCommandClick(cmd)}
              disabled={commandMutation.isPending}
              className={`p-5 rounded-2xl border text-left flex flex-col justify-between transition-all group ${
                isDanger
                  ? 'bg-red-500/5 border-red-500/20 hover:border-red-500/50'
                  : isWarning
                  ? 'bg-orange-500/5 border-orange-500/20 hover:border-orange-500/50'
                  : 'bg-card border-border hover:border-primary/50'
              }`}
            >
              <div>
                <div className="flex items-center justify-between mb-3">
                  <div className={`p-2.5 rounded-xl ${
                    isDanger ? 'bg-red-500/10 text-red-400' : isWarning ? 'bg-orange-500/10 text-orange-400' : 'bg-primary/10 text-primary'
                  }`}>
                    <Icon size={18} />
                  </div>
                  <span className="text-[10px] font-mono font-bold text-muted-foreground uppercase">{cmd.action}</span>
                </div>
                <h3 className="font-bold text-sm text-foreground group-hover:text-primary transition-colors">{cmd.label}</h3>
                <p className="text-xs text-muted-foreground mt-1 leading-relaxed">{cmd.desc}</p>
              </div>
              <div className="mt-4 pt-3 border-t border-border/50 flex items-center justify-between text-xs font-semibold text-muted-foreground">
                <span>Dispatch Action</span>
                <span>→</span>
              </div>
            </motion.button>
          );
        })}
      </div>

      {/* Dispatch Output Logs */}
      {logs.length > 0 && (
        <div className="bg-card border border-border rounded-2xl p-5">
          <h3 className="text-xs font-bold uppercase tracking-wider text-muted-foreground mb-3">Dispatch Activity Console</h3>
          <div className="bg-muted/50 rounded-xl p-4 font-mono text-xs space-y-2 max-h-48 overflow-y-auto">
            {logs.map((l, i) => (
              <div key={i} className="flex items-start gap-2">
                <span className="text-muted-foreground">[{l.time}]</span>
                <span className={l.success ? 'text-green-400' : 'text-red-400'}>{l.text}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Confirmation Dialog */}
      <ConfirmDialog
        open={!!pendingCmd}
        title={`Execute ${pendingCmd?.label}?`}
        description={`This command will be sent live over WebSocket to all active devices of business '${businessId}'. Proceed carefully.`}
        confirmLabel="Execute Command"
        confirmVariant={pendingCmd?.variant === 'danger' ? 'danger' : 'warning'}
        loading={commandMutation.isPending}
        onConfirm={() => {
          if (pendingCmd) {
            commandMutation.mutate({ command: pendingCmd.action, businessId: businessId.trim(), deviceId: deviceId.trim() || undefined });
          }
        }}
        onCancel={() => setPendingCmd(null)}
      />
    </div>
  );
};

export default RemoteManagement;
