import React, { useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { Zap, CheckSquare, Square, Search, RefreshCw, Send, ShieldAlert, Layers } from 'lucide-react';
import { businessService } from '@/services/business.service';
import { remoteCommandService } from '@/services/remoteCommand.service';
import ConfirmDialog from '@/components/shared/ConfirmDialog';
import { motion } from 'framer-motion';

const BulkActions: React.FC = () => {
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [search, setSearch] = useState('');
  const [action, setAction] = useState('ACTIVATE');
  const [notifTitle, setNotifTitle] = useState('System Announcement');
  const [notifMessage, setNotifMessage] = useState('');
  const [flagKey, setFlagKey] = useState('AI_MENU_IMPORT');
  const [flagStatus, setFlagStatus] = useState('ON');
  const [showConfirm, setShowConfirm] = useState(false);
  const [resultSummary, setResultSummary] = useState<any | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['businesses-bulk', search],
    queryFn: () => businessService.getBusinesses({ search, limit: 100 }),
  });

  const bulkMutation = useMutation({
    mutationFn: remoteCommandService.executeBulk,
    onSuccess: (res) => {
      setResultSummary(res);
      setShowConfirm(false);
      setSelectedIds([]);
    },
  });

  const businesses: any[] = data?.items ?? [];

  const toggleSelectAll = () => {
    if (selectedIds.length === businesses.length) {
      setSelectedIds([]);
    } else {
      setSelectedIds(businesses.map(b => b.id));
    }
  };

  const toggleSelect = (id: string) => {
    setSelectedIds(ids => ids.includes(id) ? ids.filter(x => x !== id) : [...ids, id]);
  };

  const handleExecute = () => {
    if (selectedIds.length === 0) {
      alert('Select at least one business.');
      return;
    }
    bulkMutation.mutate({
      action,
      businessIds: selectedIds,
      payload: {
        title: notifTitle,
        message: notifMessage,
        featureKey: flagKey,
        status: flagStatus,
      },
    });
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-black tracking-tight flex items-center gap-2">
          <Zap className="text-primary" size={24} /> Bulk Operations Console
        </h1>
        <p className="text-muted-foreground text-sm mt-0.5">
          Select multiple businesses to run batch subscription, notification, config refresh, or feature flag updates.
        </p>
      </div>

      {/* Action Selector Card */}
      <div className="bg-card border border-border rounded-2xl p-6 space-y-4">
        <h2 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Select Operation</h2>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="text-xs font-semibold text-muted-foreground block mb-1">Bulk Action</label>
            <select
              value={action}
              onChange={e => setAction(e.target.value)}
              className="w-full bg-muted/50 border border-border rounded-xl px-4 py-2.5 text-sm focus:outline-none"
            >
              <option value="ACTIVATE">Activate Subscriptions</option>
              <option value="SUSPEND">Suspend Subscriptions</option>
              <option value="REFRESH_CONFIG">Force Sync / Refresh Config</option>
              <option value="BROADCAST_NOTIFICATION">Send Notification</option>
              <option value="FEATURE_FLAG_UPDATE">Update Feature Flag</option>
            </select>
          </div>

          {action === 'BROADCAST_NOTIFICATION' && (
            <>
              <div>
                <label className="text-xs font-semibold text-muted-foreground block mb-1">Notification Title</label>
                <input type="text" value={notifTitle} onChange={e => setNotifTitle(e.target.value)}
                  className="w-full bg-muted/50 border border-border rounded-xl px-4 py-2.5 text-sm focus:outline-none" />
              </div>
              <div>
                <label className="text-xs font-semibold text-muted-foreground block mb-1">Notification Message</label>
                <input type="text" value={notifMessage} onChange={e => setNotifMessage(e.target.value)} placeholder="Message content..."
                  className="w-full bg-muted/50 border border-border rounded-xl px-4 py-2.5 text-sm focus:outline-none" />
              </div>
            </>
          )}

          {action === 'FEATURE_FLAG_UPDATE' && (
            <>
              <div>
                <label className="text-xs font-semibold text-muted-foreground block mb-1">Feature Key</label>
                <input type="text" value={flagKey} onChange={e => setFlagKey(e.target.value)}
                  className="w-full bg-muted/50 border border-border rounded-xl px-4 py-2.5 text-sm focus:outline-none" />
              </div>
              <div>
                <label className="text-xs font-semibold text-muted-foreground block mb-1">Status</label>
                <select value={flagStatus} onChange={e => setFlagStatus(e.target.value)}
                  className="w-full bg-muted/50 border border-border rounded-xl px-4 py-2.5 text-sm focus:outline-none">
                  <option value="ON">ON</option>
                  <option value="OFF">OFF</option>
                  <option value="BETA">BETA</option>
                  <option value="MAINTENANCE">MAINTENANCE</option>
                </select>
              </div>
            </>
          )}
        </div>

        <div className="pt-2 flex items-center justify-between">
          <span className="text-xs font-bold text-primary">
            {selectedIds.length} business{selectedIds.length === 1 ? '' : 'es'} selected
          </span>
          <button
            onClick={() => setShowConfirm(true)}
            disabled={selectedIds.length === 0}
            className="flex items-center gap-2 px-5 py-2.5 bg-primary text-black font-bold text-sm rounded-xl hover:bg-primary/90 disabled:opacity-40 transition-colors"
          >
            <Send size={15} /> Execute Bulk {action}
          </button>
        </div>
      </div>

      {/* Target Table */}
      <div className="bg-card border border-border rounded-2xl overflow-hidden">
        <div className="p-4 border-b border-border flex items-center justify-between">
          <div className="relative max-w-xs w-full">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={14} />
            <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Filter businesses..."
              className="w-full bg-muted/50 border border-border rounded-xl py-2 pl-9 pr-4 text-xs focus:outline-none" />
          </div>
          <button onClick={toggleSelectAll} className="text-xs text-primary font-bold hover:underline">
            {selectedIds.length === businesses.length ? 'Deselect All' : 'Select All'}
          </button>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/30">
                <th className="px-4 py-3 text-left w-10">Select</th>
                <th className="px-4 py-3 text-left text-xs font-bold text-muted-foreground">Business</th>
                <th className="px-4 py-3 text-left text-xs font-bold text-muted-foreground">Country</th>
                <th className="px-4 py-3 text-left text-xs font-bold text-muted-foreground">Plan</th>
                <th className="px-4 py-3 text-left text-xs font-bold text-muted-foreground">Status</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="border-b border-border/50 animate-pulse">
                    <td colSpan={5} className="px-4 py-4"><div className="h-4 bg-muted rounded w-full" /></td>
                  </tr>
                ))
              ) : businesses.map((b: any) => {
                const isSelected = selectedIds.includes(b.id);
                return (
                  <tr key={b.id} onClick={() => toggleSelect(b.id)} className={`border-b border-border/50 cursor-pointer hover:bg-muted/10 transition-colors ${isSelected ? 'bg-primary/5' : ''}`}>
                    <td className="px-4 py-3">
                      {isSelected ? <CheckSquare size={16} className="text-primary" /> : <Square size={16} className="text-muted-foreground" />}
                    </td>
                    <td className="px-4 py-3 font-semibold">{b.name}</td>
                    <td className="px-4 py-3 text-xs text-muted-foreground">{b.country}</td>
                    <td className="px-4 py-3"><span className="px-2 py-0.5 bg-primary/10 text-primary text-xs font-bold rounded-lg">{b.subscriptions?.[0]?.planId || 'TRIAL'}</span></td>
                    <td className="px-4 py-3 text-xs text-muted-foreground">{b.subscriptions?.[0]?.status || 'ACTIVE'}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Result Output */}
      {resultSummary && (
        <div className="bg-card border border-border rounded-2xl p-5">
          <h3 className="text-xs font-bold uppercase tracking-wider text-green-400 mb-2">Bulk Execution Completed</h3>
          <p className="text-sm font-semibold mb-3">Processed {resultSummary.processed} business targets successfully.</p>
          <div className="bg-muted/50 rounded-xl p-3 max-h-40 overflow-y-auto font-mono text-xs space-y-1">
            {resultSummary.results.map((r: any, i: number) => (
              <div key={i} className={r.success ? 'text-green-400' : 'text-red-400'}>
                [{r.businessId}] {r.success ? 'OK' : `FAIL: ${r.error}`}
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Confirm Dialog */}
      <ConfirmDialog
        open={showConfirm}
        title={`Confirm Bulk ${action}`}
        description={`Are you sure you want to execute '${action}' on ${selectedIds.length} selected business(es)? This will update databases and send live WebSocket events.`}
        confirmLabel="Run Bulk Operation"
        confirmVariant={action === 'SUSPEND' ? 'danger' : 'primary'}
        loading={bulkMutation.isPending}
        onConfirm={handleExecute}
        onCancel={() => setShowConfirm(false)}
      />
    </div>
  );
};

export default BulkActions;
