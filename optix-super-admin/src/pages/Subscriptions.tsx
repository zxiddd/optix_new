import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Search, ChevronLeft, ChevronRight, RefreshCw, Calendar, Zap, PauseCircle, XCircle, PlayCircle, TrendingUp, TrendingDown, RotateCcw } from 'lucide-react';
import { subscriptionService } from '@/services/subscription.service';
import StatusBadge from '@/components/shared/StatusBadge';
import ConfirmDialog from '@/components/shared/ConfirmDialog';
import { motion, AnimatePresence } from 'framer-motion';

const STATUSES = ['', 'ACTIVE', 'TRIAL', 'EXPIRED', 'CANCELLED', 'SUSPENDED'];
const PLANS = ['', 'STARTER', 'GROWTH'];
const BILLING = ['MONTHLY', 'YEARLY'];

const Subscriptions: React.FC = () => {
  const qc = useQueryClient();
  const [filters, setFilters] = useState<Record<string, string>>({});
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [selectedSub, setSelectedSub] = useState<any | null>(null);
  const [actionPanel, setActionPanel] = useState<string | null>(null); // businessId
  const [extendDays, setExtendDays] = useState(30);
  const [newPlan, setNewPlan] = useState('STARTER');
  const [newCycle, setNewCycle] = useState('MONTHLY');
  const [confirmAction, setConfirmAction] = useState<{ type: string; businessId: string; label: string } | null>(null);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['subscriptions', filters, page, search],
    queryFn: () => subscriptionService.getAll({ ...filters, page, limit: 50, search }),
  });

  const changePlanMutation = useMutation({
    mutationFn: ({ businessId, planId, billingCycle }: any) => subscriptionService.changePlan(businessId, planId, billingCycle),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['subscriptions'] }); setActionPanel(null); },
  });

  const extendMutation = useMutation({
    mutationFn: ({ businessId, days }: any) => subscriptionService.extend(businessId, days),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['subscriptions'] }); setActionPanel(null); },
  });

  const statusMutation = useMutation({
    mutationFn: ({ businessId, status }: any) => subscriptionService.updateStatus(businessId, status),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['subscriptions'] }); setConfirmAction(null); },
  });

  const resetTrialMutation = useMutation({
    mutationFn: (businessId: string) => subscriptionService.resetTrial(businessId),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['subscriptions'] }); setConfirmAction(null); },
  });

  const setFilter = (key: string, val: string) => { setFilters(f => ({ ...f, [key]: val })); setPage(1); };
  const subs: any[] = data?.items ?? [];
  const meta = data?.meta;

  const daysLeft = (expiryDate: string) => {
    const diff = new Date(expiryDate).getTime() - Date.now();
    return Math.max(0, Math.ceil(diff / (1000 * 60 * 60 * 24)));
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black tracking-tight">Subscriptions</h1>
          <p className="text-muted-foreground text-sm mt-0.5">Manage every business subscription</p>
        </div>
        <button onClick={() => refetch()} className="p-2.5 rounded-xl border border-border hover:bg-muted transition-colors">
          <RefreshCw size={16} />
        </button>
      </div>

      {/* Filters */}
      <div className="bg-card border border-border rounded-2xl p-4">
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative flex-1 min-w-[200px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={15} />
            <input value={search} onChange={e => { setSearch(e.target.value); setPage(1); }}
              placeholder="Search by business name..."
              className="w-full bg-muted/50 border border-border rounded-xl py-2.5 pl-9 pr-4 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20" />
          </div>
          <select value={filters.status ?? ''} onChange={e => setFilter('status', e.target.value)}
            className="bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20">
            {STATUSES.map(s => <option key={s} value={s}>{s || 'All Statuses'}</option>)}
          </select>
          <select value={filters.planId ?? ''} onChange={e => setFilter('planId', e.target.value)}
            className="bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20">
            {PLANS.map(p => <option key={p} value={p}>{p || 'All Plans'}</option>)}
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="bg-card border border-border rounded-2xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/30">
                {['Business', 'Owner', 'Plan', 'Status', 'Cycle', 'Days Left', 'Expiry', 'Actions'].map(h => (
                  <th key={h} className="px-4 py-3.5 text-left text-xs font-bold text-muted-foreground uppercase tracking-wider whitespace-nowrap">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                Array.from({ length: 8 }).map((_, i) => (
                  <tr key={i} className="border-b border-border/50 animate-pulse">
                    {Array.from({ length: 8 }).map((_, j) => (
                      <td key={j} className="px-4 py-4"><div className="h-3 bg-muted rounded w-20" /></td>
                    ))}
                  </tr>
                ))
              ) : subs.length === 0 ? (
                <tr><td colSpan={8} className="px-4 py-16 text-center text-muted-foreground">
                  No subscriptions found
                </td></tr>
              ) : subs.map((sub: any, i: number) => {
                const days = daysLeft(sub.expiryDate);
                const isExpiring = days <= 7 && days > 0;
                const isExpired = days === 0;
                return (
                  <React.Fragment key={sub.id}>
                    <motion.tr
                      initial={{ opacity: 0 }}
                      animate={{ opacity: 1 }}
                      transition={{ delay: i * 0.02 }}
                      className="border-b border-border/50 hover:bg-muted/10 transition-colors"
                    >
                      <td className="px-4 py-3.5 font-semibold">{sub.business?.name ?? '—'}</td>
                      <td className="px-4 py-3.5 text-xs text-muted-foreground">{sub.business?.users?.[0]?.email ?? '—'}</td>
                      <td className="px-4 py-3.5">
                        <span className="px-2 py-0.5 text-xs font-bold bg-primary/10 text-primary rounded-lg">{sub.planId}</span>
                      </td>
                      <td className="px-4 py-3.5"><StatusBadge status={sub.status} size="sm" /></td>
                      <td className="px-4 py-3.5 text-xs text-muted-foreground">{sub.billingCycle}</td>
                      <td className="px-4 py-3.5">
                        <span className={`text-xs font-bold ${isExpired ? 'text-red-400' : isExpiring ? 'text-orange-400' : 'text-green-400'}`}>
                          {days}d
                        </span>
                      </td>
                      <td className="px-4 py-3.5 text-xs text-muted-foreground whitespace-nowrap">
                        {new Date(sub.expiryDate).toLocaleDateString()}
                      </td>
                      <td className="px-4 py-3.5">
                        <button
                          onClick={() => setActionPanel(actionPanel === sub.businessId ? null : sub.businessId)}
                          className="px-3 py-1.5 text-xs font-semibold bg-primary/10 text-primary hover:bg-primary/20 rounded-lg transition-colors"
                        >
                          Actions
                        </button>
                      </td>
                    </motion.tr>

                    {/* Inline Action Panel */}
                    <AnimatePresence>
                      {actionPanel === sub.businessId && (
                        <motion.tr
                          initial={{ opacity: 0, height: 0 }}
                          animate={{ opacity: 1, height: 'auto' }}
                          exit={{ opacity: 0, height: 0 }}
                          className="bg-muted/20"
                        >
                          <td colSpan={8} className="px-6 py-5">
                            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                              {/* Change Plan */}
                              <div className="bg-card border border-border rounded-xl p-4 space-y-3">
                                <h4 className="text-xs font-bold uppercase text-muted-foreground">Change Plan</h4>
                                <select value={newPlan} onChange={e => setNewPlan(e.target.value)}
                                  className="w-full bg-muted/50 border border-border rounded-lg px-3 py-2 text-sm">
                                  {PLANS.filter(Boolean).map(p => <option key={p} value={p}>{p}</option>)}
                                </select>
                                <select value={newCycle} onChange={e => setNewCycle(e.target.value)}
                                  className="w-full bg-muted/50 border border-border rounded-lg px-3 py-2 text-sm">
                                  {BILLING.map(b => <option key={b} value={b}>{b}</option>)}
                                </select>
                                <button
                                  onClick={() => changePlanMutation.mutate({ businessId: sub.businessId, planId: newPlan, billingCycle: newCycle })}
                                  disabled={changePlanMutation.isPending}
                                  className="w-full flex items-center justify-center gap-2 px-3 py-2 text-xs font-bold bg-primary text-black rounded-lg hover:bg-primary/90 transition-colors disabled:opacity-50"
                                >
                                  <Zap size={12} /> Apply Plan Change
                                </button>
                              </div>

                              {/* Extend */}
                              <div className="bg-card border border-border rounded-xl p-4 space-y-3">
                                <h4 className="text-xs font-bold uppercase text-muted-foreground">Extend Subscription</h4>
                                <div className="flex items-center gap-2">
                                  <input type="number" value={extendDays} onChange={e => setExtendDays(Number(e.target.value))}
                                    min={1} max={365}
                                    className="flex-1 bg-muted/50 border border-border rounded-lg px-3 py-2 text-sm" />
                                  <span className="text-sm text-muted-foreground">days</span>
                                </div>
                                <button
                                  onClick={() => extendMutation.mutate({ businessId: sub.businessId, days: extendDays })}
                                  disabled={extendMutation.isPending}
                                  className="w-full flex items-center justify-center gap-2 px-3 py-2 text-xs font-bold bg-blue-500/10 text-blue-400 border border-blue-500/20 rounded-lg hover:bg-blue-500/20 transition-colors disabled:opacity-50"
                                >
                                  <Calendar size={12} /> Extend +{extendDays}d
                                </button>
                              </div>

                              {/* Status Actions */}
                              <div className="bg-card border border-border rounded-xl p-4 space-y-2">
                                <h4 className="text-xs font-bold uppercase text-muted-foreground mb-3">Status Actions</h4>
                                {[
                                  { status: 'ACTIVE', icon: PlayCircle, label: 'Activate', color: 'text-green-400 border-green-500/20 bg-green-500/10 hover:bg-green-500/20' },
                                  { status: 'SUSPENDED', icon: PauseCircle, label: 'Suspend', color: 'text-orange-400 border-orange-500/20 bg-orange-500/10 hover:bg-orange-500/20' },
                                  { status: 'CANCELLED', icon: XCircle, label: 'Cancel', color: 'text-red-400 border-red-500/20 bg-red-500/10 hover:bg-red-500/20' },
                                ].map(({ status, icon: Icon, label, color }) => (
                                  <button key={status}
                                    onClick={() => setConfirmAction({ type: status, businessId: sub.businessId, label })}
                                    className={`w-full flex items-center gap-2 px-3 py-2 text-xs font-bold border rounded-lg transition-colors ${color}`}
                                  >
                                    <Icon size={12} /> {label}
                                  </button>
                                ))}
                                <button
                                  onClick={() => setConfirmAction({ type: 'RESET_TRIAL', businessId: sub.businessId, label: 'Reset Trial' })}
                                  className="w-full flex items-center gap-2 px-3 py-2 text-xs font-bold border border-purple-500/20 bg-purple-500/10 text-purple-400 hover:bg-purple-500/20 rounded-lg transition-colors"
                                >
                                  <RotateCcw size={12} /> Reset Trial
                                </button>
                              </div>
                            </div>
                          </td>
                        </motion.tr>
                      )}
                    </AnimatePresence>
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>

        {meta && meta.lastPage > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-border bg-muted/20">
            <p className="text-xs text-muted-foreground">Total: {meta.total} subscriptions</p>
            <div className="flex items-center gap-1">
              <button onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page === 1}
                className="p-1.5 rounded-lg hover:bg-muted disabled:opacity-40 transition-colors"><ChevronLeft size={15} /></button>
              <span className="px-3 py-1 text-xs font-bold">{page} / {meta.lastPage}</span>
              <button onClick={() => setPage(p => Math.min(meta.lastPage, p + 1))} disabled={page === meta.lastPage}
                className="p-1.5 rounded-lg hover:bg-muted disabled:opacity-40 transition-colors"><ChevronRight size={15} /></button>
            </div>
          </div>
        )}
      </div>

      {/* Confirm Dialog */}
      <ConfirmDialog
        open={!!confirmAction}
        title={`${confirmAction?.label} Subscription`}
        description={confirmAction?.type === 'RESET_TRIAL'
          ? 'This will reset all trial usage counters (bills, products) to zero.'
          : `Are you sure you want to set this subscription to ${confirmAction?.type}?`}
        confirmLabel={confirmAction?.label ?? 'Confirm'}
        confirmVariant={['CANCELLED', 'SUSPENDED'].includes(confirmAction?.type ?? '') ? 'danger' : 'primary'}
        loading={statusMutation.isPending || resetTrialMutation.isPending}
        onConfirm={() => {
          if (!confirmAction) return;
          if (confirmAction.type === 'RESET_TRIAL') resetTrialMutation.mutate(confirmAction.businessId);
          else statusMutation.mutate({ businessId: confirmAction.businessId, status: confirmAction.type });
        }}
        onCancel={() => setConfirmAction(null)}
      />
    </div>
  );
};

export default Subscriptions;
