import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Search, Download, Trash2, PowerOff, ChevronLeft, ChevronRight, Copy, RefreshCw, X } from 'lucide-react';
import { activationService } from '@/services/activation.service';
import StatusBadge from '@/components/shared/StatusBadge';
import ConfirmDialog from '@/components/shared/ConfirmDialog';
import { motion, AnimatePresence } from 'framer-motion';

const PLANS = ['STARTER', 'GROWTH'];
const BILLING = ['MONTHLY', 'YEARLY'];

const defaultForm = {
  planId: 'STARTER',
  billingCycle: 'MONTHLY',
  maxUses: 1,
  countryRestriction: '',
  expiresAt: '',
  notes: '',
};

const ActivationCodes: React.FC = () => {
  const qc = useQueryClient();
  const [filters, setFilters] = useState<Record<string, string>>({});
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [showCreate, setShowCreate] = useState(false);
  const [bulkCount, setBulkCount] = useState(10);
  const [isBulk, setIsBulk] = useState(false);
  const [form, setForm] = useState(defaultForm);
  const [deleteTarget, setDeleteTarget] = useState<string | null>(null);
  const [deactivateTarget, setDeactivateTarget] = useState<string | null>(null);
  const [bulkResult, setBulkResult] = useState<string[] | null>(null);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['activation-codes', filters, page, search],
    queryFn: () => activationService.getAll({ ...filters, page, limit: 50, search }),
  });

  const createMutation = useMutation({
    mutationFn: () => activationService.create({ ...form, maxUses: Number(form.maxUses) }),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['activation-codes'] }); setShowCreate(false); setForm(defaultForm); },
  });

  const bulkMutation = useMutation({
    mutationFn: () => activationService.bulkCreate({ ...form, count: bulkCount, maxUses: Number(form.maxUses) }),
    onSuccess: (res) => {
      qc.invalidateQueries({ queryKey: ['activation-codes'] });
      setShowCreate(false);
      setBulkResult(res.codes);
    },
  });

  const deactivateMutation = useMutation({
    mutationFn: (id: string) => activationService.deactivate(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['activation-codes'] }); setDeactivateTarget(null); },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => activationService.delete(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['activation-codes'] }); setDeleteTarget(null); },
  });

  const codes: any[] = data?.items ?? [];
  const meta = data?.meta;

  const exportCsv = () => {
    if (!codes.length) return;
    const headers = ['Code', 'Plan', 'Cycle', 'Max Uses', 'Used', 'Country', 'Expires', 'Active', 'Created'];
    const rows = codes.map((c: any) => [
      c.code, c.planId, c.billingCycle, c.maxUses, c.usedCount,
      c.countryRestriction ?? 'Any', c.expiresAt ? new Date(c.expiresAt).toLocaleDateString() : 'Never',
      c.isActive, new Date(c.createdAt).toLocaleDateString()
    ]);
    const csv = [headers, ...rows].map(r => r.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = 'activation-codes.csv'; a.click();
    URL.revokeObjectURL(url);
  };

  const exportBulkCsv = (codes: string[]) => {
    const csv = ['Code', ...codes].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = 'bulk-activation-codes.csv'; a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black tracking-tight">Activation Codes</h1>
          <p className="text-muted-foreground text-sm mt-0.5">Generate and manage subscription activation codes</p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => refetch()} className="p-2.5 rounded-xl border border-border hover:bg-muted transition-colors">
            <RefreshCw size={16} />
          </button>
          <button onClick={exportCsv} className="flex items-center gap-2 px-4 py-2.5 rounded-xl border border-border hover:bg-muted text-sm font-semibold transition-colors">
            <Download size={15} /> Export CSV
          </button>
          <button onClick={() => setShowCreate(true)} className="flex items-center gap-2 px-4 py-2.5 rounded-xl bg-primary text-black text-sm font-bold hover:bg-primary/90 transition-colors">
            <Plus size={15} /> Generate Code
          </button>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-card border border-border rounded-2xl p-4 flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={15} />
          <input value={search} onChange={e => { setSearch(e.target.value); setPage(1); }}
            placeholder="Search by code or notes..."
            className="w-full bg-muted/50 border border-border rounded-xl py-2.5 pl-9 pr-4 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20" />
        </div>
        <select value={filters.planId ?? ''} onChange={e => { setFilters(f => ({ ...f, planId: e.target.value })); setPage(1); }}
          className="bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none">
          <option value="">All Plans</option>
          {PLANS.map(p => <option key={p} value={p}>{p}</option>)}
        </select>
        <select value={filters.isActive ?? ''} onChange={e => { setFilters(f => ({ ...f, isActive: e.target.value })); setPage(1); }}
          className="bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none">
          <option value="">All Status</option>
          <option value="true">Active</option>
          <option value="false">Inactive</option>
        </select>
      </div>

      {/* Table */}
      <div className="bg-card border border-border rounded-2xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/30">
                {['Code', 'Plan', 'Cycle', 'Uses', 'Country', 'Expires', 'Status', 'Created', 'Actions'].map(h => (
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
              ) : codes.length === 0 ? (
                <tr><td colSpan={9} className="px-4 py-16 text-center text-muted-foreground">
                  <p className="font-semibold">No activation codes found</p>
                  <p className="text-xs mt-1">Click "Generate Code" to create your first one</p>
                </td></tr>
              ) : codes.map((code: any, i: number) => (
                <motion.tr key={code.id} initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: i * 0.02 }}
                  className="border-b border-border/50 hover:bg-muted/10 transition-colors group">
                  <td className="px-4 py-3.5">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-xs font-bold text-primary">{code.code}</span>
                      <button onClick={() => navigator.clipboard.writeText(code.code)}
                        className="opacity-0 group-hover:opacity-100 transition-opacity text-muted-foreground hover:text-foreground">
                        <Copy size={11} />
                      </button>
                    </div>
                  </td>
                  <td className="px-4 py-3.5"><span className="px-2 py-0.5 bg-primary/10 text-primary text-xs font-bold rounded-lg">{code.planId}</span></td>
                  <td className="px-4 py-3.5 text-xs text-muted-foreground">{code.billingCycle}</td>
                  <td className="px-4 py-3.5">
                    <span className="text-xs font-bold">{code.usedCount}</span>
                    <span className="text-xs text-muted-foreground"> / {code.maxUses === -1 ? '∞' : code.maxUses}</span>
                  </td>
                  <td className="px-4 py-3.5 text-xs text-muted-foreground">{code.countryRestriction ?? 'Any'}</td>
                  <td className="px-4 py-3.5 text-xs text-muted-foreground">{code.expiresAt ? new Date(code.expiresAt).toLocaleDateString() : 'Never'}</td>
                  <td className="px-4 py-3.5"><StatusBadge status={code.isActive} size="sm" /></td>
                  <td className="px-4 py-3.5 text-xs text-muted-foreground">{new Date(code.createdAt).toLocaleDateString()}</td>
                  <td className="px-4 py-3.5">
                    <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      {code.isActive && (
                        <button onClick={() => setDeactivateTarget(code.id)}
                          className="p-1.5 hover:bg-orange-500/10 text-orange-400 rounded-lg transition-colors" title="Deactivate">
                          <PowerOff size={13} />
                        </button>
                      )}
                      <button onClick={() => setDeleteTarget(code.id)}
                        className="p-1.5 hover:bg-red-500/10 text-red-400 rounded-lg transition-colors" title="Delete">
                        <Trash2 size={13} />
                      </button>
                    </div>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>
        {meta && meta.lastPage > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-border bg-muted/20">
            <p className="text-xs text-muted-foreground">Total: {meta.total} codes</p>
            <div className="flex items-center gap-1">
              <button onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page === 1}
                className="p-1.5 rounded-lg hover:bg-muted disabled:opacity-40"><ChevronLeft size={15} /></button>
              <span className="px-3 py-1 text-xs font-bold">{page} / {meta.lastPage}</span>
              <button onClick={() => setPage(p => Math.min(meta.lastPage, p + 1))} disabled={page === meta.lastPage}
                className="p-1.5 rounded-lg hover:bg-muted disabled:opacity-40"><ChevronRight size={15} /></button>
            </div>
          </div>
        )}
      </div>

      {/* Create Modal */}
      <AnimatePresence>
        {showCreate && (
          <motion.div className="fixed inset-0 z-50 flex items-center justify-center" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setShowCreate(false)} />
            <motion.div className="relative bg-card border border-border rounded-2xl p-6 w-full max-w-md shadow-2xl"
              initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }}>
              <div className="flex items-center justify-between mb-5">
                <h2 className="text-base font-bold">Generate Activation Code</h2>
                <button onClick={() => setShowCreate(false)} className="p-1 rounded-lg hover:bg-muted"><X size={16} /></button>
              </div>

              <div className="space-y-3">
                <div className="flex gap-2">
                  <button onClick={() => setIsBulk(false)}
                    className={`flex-1 py-2 text-xs font-bold rounded-lg border transition-colors ${!isBulk ? 'bg-primary text-black border-primary' : 'border-border hover:bg-muted'}`}>
                    Single
                  </button>
                  <button onClick={() => setIsBulk(true)}
                    className={`flex-1 py-2 text-xs font-bold rounded-lg border transition-colors ${isBulk ? 'bg-primary text-black border-primary' : 'border-border hover:bg-muted'}`}>
                    Bulk
                  </button>
                </div>

                {isBulk && (
                  <div>
                    <label className="text-xs text-muted-foreground block mb-1">Count</label>
                    <input type="number" value={bulkCount} onChange={e => setBulkCount(Number(e.target.value))}
                      min={1} max={500}
                      className="w-full bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20" />
                  </div>
                )}

                {[
                  { key: 'planId', label: 'Plan', type: 'select', options: PLANS },
                  { key: 'billingCycle', label: 'Billing Cycle', type: 'select', options: BILLING },
                  { key: 'maxUses', label: 'Max Uses (-1 = unlimited)', type: 'number' },
                  { key: 'countryRestriction', label: 'Country Restriction (optional)', type: 'text' },
                  { key: 'expiresAt', label: 'Expires At (optional)', type: 'date' },
                  { key: 'notes', label: 'Notes (optional)', type: 'text' },
                ].map(({ key, label, type, options }) => (
                  <div key={key}>
                    <label className="text-xs text-muted-foreground block mb-1">{label}</label>
                    {type === 'select' ? (
                      <select value={(form as any)[key]} onChange={e => setForm(f => ({ ...f, [key]: e.target.value }))}
                        className="w-full bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20">
                        {options?.map(o => <option key={o} value={o}>{o}</option>)}
                      </select>
                    ) : (
                      <input type={type} value={(form as any)[key]} onChange={e => setForm(f => ({ ...f, [key]: e.target.value }))}
                        className="w-full bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20" />
                    )}
                  </div>
                ))}
              </div>

              <button
                onClick={() => isBulk ? bulkMutation.mutate() : createMutation.mutate()}
                disabled={createMutation.isPending || bulkMutation.isPending}
                className="w-full mt-5 py-2.5 bg-primary text-black text-sm font-black rounded-xl hover:bg-primary/90 disabled:opacity-50 transition-colors"
              >
                {createMutation.isPending || bulkMutation.isPending ? 'Generating...' : isBulk ? `Generate ${bulkCount} Codes` : 'Generate Code'}
              </button>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Bulk Result Modal */}
      <AnimatePresence>
        {bulkResult && (
          <motion.div className="fixed inset-0 z-50 flex items-center justify-center" initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }}>
            <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" />
            <motion.div className="relative bg-card border border-border rounded-2xl p-6 w-full max-w-lg shadow-2xl"
              initial={{ scale: 0.95 }} animate={{ scale: 1 }} exit={{ scale: 0.95 }}>
              <div className="flex items-center justify-between mb-4">
                <h2 className="font-bold">{bulkResult.length} Codes Generated</h2>
                <button onClick={() => setBulkResult(null)} className="p-1 rounded-lg hover:bg-muted"><X size={16} /></button>
              </div>
              <div className="bg-muted/50 rounded-xl p-4 max-h-60 overflow-y-auto font-mono text-xs space-y-1">
                {bulkResult.map(c => <div key={c} className="text-primary">{c}</div>)}
              </div>
              <button onClick={() => exportBulkCsv(bulkResult)}
                className="w-full mt-4 flex items-center justify-center gap-2 py-2.5 bg-primary text-black text-sm font-bold rounded-xl hover:bg-primary/90 transition-colors">
                <Download size={14} /> Download CSV
              </button>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Dialogs */}
      <ConfirmDialog open={!!deactivateTarget} title="Deactivate Code"
        description="This code will no longer be accepted for new activations."
        confirmLabel="Deactivate" confirmVariant="warning"
        loading={deactivateMutation.isPending}
        onConfirm={() => deactivateTarget && deactivateMutation.mutate(deactivateTarget)}
        onCancel={() => setDeactivateTarget(null)} />

      <ConfirmDialog open={!!deleteTarget} title="Delete Code"
        description="This activation code will be permanently deleted and cannot be recovered."
        confirmLabel="Delete" confirmVariant="danger"
        loading={deleteMutation.isPending}
        onConfirm={() => deleteTarget && deleteMutation.mutate(deleteTarget)}
        onCancel={() => setDeleteTarget(null)} />
    </div>
  );
};

export default ActivationCodes;
