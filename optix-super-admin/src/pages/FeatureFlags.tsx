import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Search, Plus, Trash2, RefreshCw, Layers, ShieldAlert, CheckCircle2, AlertTriangle, Hammer, X } from 'lucide-react';
import { featureFlagService } from '@/services/featureFlag.service';
import ConfirmDialog from '@/components/shared/ConfirmDialog';
import { motion, AnimatePresence } from 'framer-motion';

const ALL_FEATURES = [
  'BILLING', 'PRODUCTS', 'CATEGORIES', 'STAFF_MANAGEMENT', 'INVENTORY',
  'CUSTOMERS', 'EXPENSES', 'REPORTS', 'ADVANCED_REPORTS', 'GST', 'TAXES',
  'RECEIPT_CUSTOMIZATION', 'PAYMENT_QR', 'MULTIPLE_QR', 'SUBSCRIPTIONS',
  'ACTIVATION_CODES', 'AI_MENU_IMPORT', 'BLUETOOTH_PRINTING', 'KITCHEN_ORDERS',
  'LOYALTY', 'COUPONS', 'WEBSOCKET_SYNC', 'OFFLINE_MODE', 'CLOUD_SYNC',
  'AUTO_BACKUP', 'BUSINESS_TIMINGS', 'TOKEN_SYSTEM', 'ANALYTICS',
  'NOTIFICATIONS', 'SUPPORT', 'FEEDBACK'
];

const LEVELS = ['GLOBAL', 'COUNTRY', 'PLAN', 'BUSINESS'];
const STATUSES = ['ON', 'OFF', 'BETA', 'MAINTENANCE'];

const STATUS_ICONS: Record<string, any> = {
  ON: CheckCircle2,
  OFF: ShieldAlert,
  BETA: AlertTriangle,
  MAINTENANCE: Hammer,
};

const STATUS_COLORS: Record<string, string> = {
  ON: 'bg-green-500/15 text-green-400 border-green-500/30',
  OFF: 'bg-red-500/15 text-red-400 border-red-500/30',
  BETA: 'bg-yellow-500/15 text-yellow-400 border-yellow-500/30',
  MAINTENANCE: 'bg-orange-500/15 text-orange-400 border-orange-500/30',
};

const FeatureFlags: React.FC = () => {
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [levelFilter, setLevelFilter] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [page, setPage] = useState(1);

  const [showModal, setShowModal] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);

  const [form, setForm] = useState<{
    featureKey: string;
    status: 'ON' | 'OFF' | 'BETA' | 'MAINTENANCE';
    level: 'GLOBAL' | 'COUNTRY' | 'PLAN' | 'BUSINESS';
    target: string;
    notes: string;
  }>({
    featureKey: ALL_FEATURES[0],
    status: 'ON',
    level: 'GLOBAL',
    target: '',
    notes: '',
  });

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['feature-flags', search, levelFilter, statusFilter, page],
    queryFn: () => featureFlagService.getAll({ search, level: levelFilter, status: statusFilter, page, limit: 50 }),
  });

  const upsertMutation = useMutation({
    mutationFn: featureFlagService.upsert,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['feature-flags'] });
      setShowModal(false);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: featureFlagService.delete,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['feature-flags'] });
      setDeleteId(null);
    },
  });

  const flags: any[] = data?.items ?? [];
  const meta = data?.meta;

  const handleQuickToggle = (flag: any, nextStatus: 'ON' | 'OFF' | 'BETA' | 'MAINTENANCE') => {
    upsertMutation.mutate({
      featureKey: flag.featureKey,
      status: nextStatus,
      level: flag.level,
      target: flag.target,
      notes: flag.notes,
      businessId: flag.businessId,
    });
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black tracking-tight flex items-center gap-2">
            <Layers className="text-primary" size={24} /> Feature Flags & Remote Config
          </h1>
          <p className="text-muted-foreground text-sm mt-0.5">
            Control platform capabilities globally, by country, by plan, or per business in real-time.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => refetch()} className="p-2.5 rounded-xl border border-border hover:bg-muted transition-colors">
            <RefreshCw size={16} />
          </button>
          <button onClick={() => setShowModal(true)} className="flex items-center gap-2 px-4 py-2.5 bg-primary text-black font-bold rounded-xl text-sm hover:bg-primary/90 transition-colors">
            <Plus size={16} /> Add Flag Rule
          </button>
        </div>
      </div>

      {/* Filters Bar */}
      <div className="bg-card border border-border rounded-2xl p-4 flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={15} />
          <input
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(1); }}
            placeholder="Search by feature key, target, notes..."
            className="w-full bg-muted/50 border border-border rounded-xl py-2.5 pl-9 pr-4 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
          />
        </div>
        <select value={levelFilter} onChange={e => setLevelFilter(e.target.value)}
          className="bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none">
          <option value="">All Scopes</option>
          {LEVELS.map(l => <option key={l} value={l}>{l}</option>)}
        </select>
        <select value={statusFilter} onChange={e => setStatusFilter(e.target.value)}
          className="bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none">
          <option value="">All Statuses</option>
          {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
        </select>
      </div>

      {/* Feature Flags Grid / Table */}
      <div className="bg-card border border-border rounded-2xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/30">
                {['Feature Key', 'Scope Level', 'Target', 'Status', 'Notes', 'Quick Override', 'Actions'].map(h => (
                  <th key={h} className="px-4 py-3.5 text-left text-xs font-bold text-muted-foreground uppercase tracking-wider whitespace-nowrap">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                Array.from({ length: 8 }).map((_, i) => (
                  <tr key={i} className="border-b border-border/50 animate-pulse">
                    {Array.from({ length: 7 }).map((_, j) => <td key={j} className="px-4 py-4"><div className="h-3 bg-muted rounded w-20" /></td>)}
                  </tr>
                ))
              ) : flags.length === 0 ? (
                <tr>
                  <td colSpan={7} className="px-4 py-16 text-center text-muted-foreground">
                    <p className="font-semibold">No active feature flag overrides</p>
                    <p className="text-xs mt-1">All features default to standard plan rules. Click "Add Flag Rule" to create one.</p>
                  </td>
                </tr>
              ) : flags.map((flag: any, i: number) => {
                const Icon = STATUS_ICONS[flag.status] || CheckCircle2;
                return (
                  <motion.tr key={flag.id} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: i * 0.02 }}
                    className="border-b border-border/50 hover:bg-muted/10 transition-colors">
                    <td className="px-4 py-3.5 font-bold font-mono text-primary text-xs">{flag.featureKey}</td>
                    <td className="px-4 py-3.5 text-xs">
                      <span className="px-2 py-0.5 rounded bg-muted text-muted-foreground font-semibold">{flag.level}</span>
                    </td>
                    <td className="px-4 py-3.5 text-xs font-mono text-muted-foreground">{flag.target || '—'}</td>
                    <td className="px-4 py-3.5">
                      <span className={`inline-flex items-center gap-1 px-2.5 py-1 text-xs font-bold border rounded-full ${STATUS_COLORS[flag.status]}`}>
                        <Icon size={12} /> {flag.status}
                      </span>
                    </td>
                    <td className="px-4 py-3.5 text-xs text-muted-foreground">{flag.notes || '—'}</td>
                    <td className="px-4 py-3.5">
                      <div className="flex items-center gap-1">
                        {STATUSES.map(st => (
                          <button
                            key={st}
                            onClick={() => handleQuickToggle(flag, st as any)}
                            className={`px-2 py-0.5 text-[10px] font-bold rounded border transition-colors ${
                              flag.status === st
                                ? 'bg-primary text-black border-primary'
                                : 'border-border text-muted-foreground hover:bg-muted'
                            }`}
                          >
                            {st}
                          </button>
                        ))}
                      </div>
                    </td>
                    <td className="px-4 py-3.5">
                      <button onClick={() => setDeleteId(flag.id)} className="p-1.5 hover:bg-red-500/10 text-red-400 rounded-lg transition-colors">
                        <Trash2 size={14} />
                      </button>
                    </td>
                  </motion.tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>

      {/* Add / Edit Modal */}
      <AnimatePresence>
        {showModal && (
          <motion.div className="fixed inset-0 z-50 flex items-center justify-center" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setShowModal(false)} />
            <motion.div className="relative bg-card border border-border rounded-2xl p-6 w-full max-w-md shadow-2xl" initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }}>
              <div className="flex items-center justify-between mb-4">
                <h2 className="font-bold text-base">Add / Override Feature Flag</h2>
                <button onClick={() => setShowModal(false)} className="p-1 rounded-lg hover:bg-muted"><X size={16} /></button>
              </div>

              <div className="space-y-3">
                <div>
                  <label className="text-xs text-muted-foreground block mb-1">Feature</label>
                  <select value={form.featureKey} onChange={e => setForm(f => ({ ...f, featureKey: e.target.value }))}
                    className="w-full bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none">
                    {ALL_FEATURES.map(feat => <option key={feat} value={feat}>{feat}</option>)}
                  </select>
                </div>

                <div>
                  <label className="text-xs text-muted-foreground block mb-1">Scope Level</label>
                  <select value={form.level} onChange={e => setForm(f => ({ ...f, level: e.target.value as any }))}
                    className="w-full bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none">
                    {LEVELS.map(l => <option key={l} value={l}>{l}</option>)}
                  </select>
                </div>

                {form.level !== 'GLOBAL' && (
                  <div>
                    <label className="text-xs text-muted-foreground block mb-1">
                      Target {form.level === 'COUNTRY' ? '(e.g. India)' : form.level === 'PLAN' ? '(e.g. STARTER)' : '(Business UUID)'}
                    </label>
                    <input
                      type="text"
                      value={form.target}
                      onChange={e => setForm(f => ({ ...f, target: e.target.value }))}
                      placeholder={form.level === 'COUNTRY' ? 'India' : form.level === 'PLAN' ? 'STARTER' : 'Business UUID'}
                      className="w-full bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none"
                    />
                  </div>
                )}

                <div>
                  <label className="text-xs text-muted-foreground block mb-1">Status</label>
                  <select value={form.status} onChange={e => setForm(f => ({ ...f, status: e.target.value as any }))}
                    className="w-full bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none">
                    {STATUSES.map(s => <option key={s} value={s}>{s}</option>)}
                  </select>
                </div>

                <div>
                  <label className="text-xs text-muted-foreground block mb-1">Notes (Optional)</label>
                  <input
                    type="text"
                    value={form.notes}
                    onChange={e => setForm(f => ({ ...f, notes: e.target.value }))}
                    placeholder="Reason for override..."
                    className="w-full bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none"
                  />
                </div>
              </div>

              <button
                onClick={() => upsertMutation.mutate(form as any)}
                disabled={upsertMutation.isPending}
                className="w-full mt-5 py-2.5 bg-primary text-black font-black text-sm rounded-xl hover:bg-primary/90 disabled:opacity-50 transition-colors"
              >
                {upsertMutation.isPending ? 'Saving...' : 'Apply Flag Rule'}
              </button>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      <ConfirmDialog
        open={!!deleteId}
        title="Delete Flag Rule"
        description="Deleting this rule will revert the feature back to default plan behavior."
        confirmLabel="Delete"
        confirmVariant="danger"
        loading={deleteMutation.isPending}
        onConfirm={() => deleteId && deleteMutation.mutate(deleteId)}
        onCancel={() => setDeleteId(null)}
      />
    </div>
  );
};

export default FeatureFlags;
