import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ShieldCheck, Database, HardDrive, Download, Trash2, Plus, Lock, Key, AlertOctagon, CheckCircle2 } from 'lucide-react';
import { infraService } from '@/services/infra.service';
import ConfirmDialog from '@/components/shared/ConfirmDialog';

const SecurityBackups: React.FC = () => {
  const qc = useQueryClient();
  const [showCleanupConfirm, setShowCleanupConfirm] = useState(false);

  const { data: sec } = useQuery({ queryKey: ['infra-security'], queryFn: infraService.getSecurityStats });
  const { data: backups, refetch: refetchBackups } = useQuery({ queryKey: ['infra-backups'], queryFn: infraService.getBackups });
  const { data: storage, refetch: refetchStorage } = useQuery({ queryKey: ['infra-storage'], queryFn: infraService.getStorageStats });

  const backupMutation = useMutation({
    mutationFn: infraService.createBackup,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['infra-backups'] });
    },
  });

  const cleanupMutation = useMutation({
    mutationFn: infraService.cleanupStorage,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['infra-storage'] });
      setShowCleanupConfirm(false);
    },
  });

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-black tracking-tight flex items-center gap-2">
          <ShieldCheck className="text-primary" size={24} /> Security, Backups & File Storage
        </h1>
        <p className="text-muted-foreground text-sm mt-0.5">
          Monitor authentication security, manage database snapshots, and clean up temporary storage assets.
        </p>
      </div>

      {/* Security Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-card border border-border rounded-2xl p-5">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold text-muted-foreground uppercase">Failed Logins</span>
            <Lock size={16} className="text-yellow-400" />
          </div>
          <p className="text-3xl font-black text-foreground">{sec?.failedLoginAttempts ?? 0}</p>
          <p className="text-xs text-muted-foreground mt-1">Audit Logged</p>
        </div>

        <div className="bg-card border border-border rounded-2xl p-5">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold text-muted-foreground uppercase">Active Sessions</span>
            <Key size={16} className="text-blue-400" />
          </div>
          <p className="text-3xl font-black text-blue-400">{(sec?.activeUserSessions ?? 0) + (sec?.activeStaffSessions ?? 0)}</p>
          <p className="text-xs text-muted-foreground mt-1">Owner & Staff Sessions</p>
        </div>

        <div className="bg-card border border-border rounded-2xl p-5">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold text-muted-foreground uppercase">Revoked Tokens</span>
            <AlertOctagon size={16} className="text-purple-400" />
          </div>
          <p className="text-3xl font-black text-purple-400">{sec?.revokedTokensCount ?? 0}</p>
          <p className="text-xs text-muted-foreground mt-1">Expired Refresh Tokens</p>
        </div>

        <div className="bg-card border border-border rounded-2xl p-5">
          <div className="flex items-center justify-between mb-2">
            <span className="text-xs font-bold text-muted-foreground uppercase">Platform Security</span>
            <CheckCircle2 size={16} className="text-green-400" />
          </div>
          <p className="text-xl font-black text-green-400">{sec?.status || 'SECURE'}</p>
          <p className="text-xs text-muted-foreground mt-1">Argon2 + JWT Hashing</p>
        </div>
      </div>

      {/* Database Backups & Storage Split Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Database Backups */}
        <div className="bg-card border border-border rounded-2xl p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-bold text-base flex items-center gap-2">
              <Database size={18} className="text-primary" /> Database Snapshot Backups
            </h2>
            <button
              onClick={() => backupMutation.mutate()}
              disabled={backupMutation.isPending}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-primary text-black font-bold text-xs rounded-xl hover:bg-primary/90 disabled:opacity-50 transition-colors"
            >
              <Plus size={14} /> Create Snapshot
            </button>
          </div>

          <div className="space-y-2">
            {(backups || []).map((b: any) => (
              <div key={b.id} className="p-3.5 rounded-xl border border-border bg-muted/20 flex items-center justify-between text-xs">
                <div>
                  <p className="font-bold font-mono text-foreground">{b.filename}</p>
                  <p className="text-[11px] text-muted-foreground mt-0.5">{b.size} • {new Date(b.createdAt).toLocaleDateString()}</p>
                </div>
                <span className="px-2 py-0.5 text-[10px] font-bold rounded bg-green-500/10 text-green-400 border border-green-500/20">
                  {b.status}
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* File Storage */}
        <div className="bg-card border border-border rounded-2xl p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="font-bold text-base flex items-center gap-2">
              <HardDrive size={18} className="text-purple-400" /> File Storage & Media Breakdown
            </h2>
            <button
              onClick={() => setShowCleanupConfirm(true)}
              className="flex items-center gap-1.5 px-3 py-1.5 bg-red-500/10 text-red-400 border border-red-500/20 font-bold text-xs rounded-xl hover:bg-red-500/20 transition-colors"
            >
              <Trash2 size={14} /> Clean Temp Files
            </button>
          </div>

          <div className="p-4 bg-muted/40 rounded-xl flex items-center justify-between">
            <div>
              <p className="text-xs font-bold text-muted-foreground uppercase">Total Uploads Storage Used</p>
              <p className="text-2xl font-black text-foreground mt-0.5">{storage?.totalUsedMb ?? 12} MB</p>
            </div>
            <span className="text-xs font-mono text-muted-foreground">{storage?.fileCount ?? 65} total files</span>
          </div>

          <div className="space-y-2">
            {(storage?.breakdown || []).map((s: any) => (
              <div key={s.category} className="flex items-center justify-between text-xs py-2 border-b border-border/50">
                <span className="font-semibold">{s.category}</span>
                <div className="flex items-center gap-3">
                  <span className="text-muted-foreground">{s.fileCount} files</span>
                  <span className="font-bold text-primary">{s.sizeMb} MB</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <ConfirmDialog
        open={showCleanupConfirm}
        title="Cleanup Temporary Export Files?"
        description="This will safely delete expired temporary PDF/CSV exports from the uploads directory."
        confirmLabel="Run Storage Cleanup"
        confirmVariant="danger"
        loading={cleanupMutation.isPending}
        onConfirm={() => cleanupMutation.mutate()}
        onCancel={() => setShowCleanupConfirm(false)}
      />
    </div>
  );
};

export default SecurityBackups;
