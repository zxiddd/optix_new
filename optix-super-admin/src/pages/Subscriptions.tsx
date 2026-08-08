import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Search, ChevronLeft, ChevronRight, RefreshCw, Plus, Edit3, X } from 'lucide-react';
import { subscriptionService } from '@/services/subscription.service';
import StatusBadge from '@/components/shared/StatusBadge';
import ConfirmDialog from '@/components/shared/ConfirmDialog';
import axios from 'axios';

const authHeader = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
const API_URL = 'https://api.optixapp.in/api/v1/super-admin/subscriptions';

const STATUSES = ['', 'ACTIVE', 'TRIAL', 'EXPIRED', 'CANCELLED', 'SUSPENDED'];
const PLANS = ['', 'STARTER', 'GROWTH'];

const Subscriptions: React.FC = () => {
  const qc = useQueryClient();
  const [filters, setFilters] = useState<Record<string, string>>({});
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [confirmAction, setConfirmAction] = useState<{ type: string; businessId: string; label: string } | null>(null);

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingSub, setEditingSub] = useState<any | null>(null);

  const [createForm, setCreateForm] = useState({
    businessId: '',
    planId: 'STARTER',
    status: 'ACTIVE',
  });

  const [editForm, setEditForm] = useState({
    planId: 'STARTER',
    status: 'ACTIVE',
  });

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['subscriptions', filters, page, search],
    queryFn: () => subscriptionService.getAll({ ...filters, page, limit: 50, search }),
  });

  const createMutation = useMutation({
    mutationFn: async (data: any) => {
      const res = await axios.post(API_URL, data, authHeader());
      return res.data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['subscriptions'] });
      setShowCreateModal(false);
    },
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: string; data: any }) => {
      const res = await axios.patch(`${API_URL}/${id}`, data, authHeader());
      return res.data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['subscriptions'] });
      setEditingSub(null);
    },
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
          <h1 className="text-2xl font-black tracking-tight">Subscriptions Management</h1>
          <p className="text-muted-foreground text-sm mt-0.5">Control, assign, extend and edit every business subscription.</p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowCreateModal(true)}
            className="flex items-center gap-1.5 px-4 py-2 bg-primary text-black font-black text-xs rounded-xl hover:bg-primary/90 transition-colors shadow-lg shadow-primary/20"
          >
            <Plus size={15} /> Create Subscription
          </button>
          <button onClick={() => refetch()} className="p-2.5 rounded-xl border border-border hover:bg-muted transition-colors">
            <RefreshCw size={16} />
          </button>
        </div>
      </div>

      {/* Search & Filters */}
      <div className="bg-card border border-border rounded-2xl p-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2 flex-1 max-w-sm">
          <Search size={15} className="text-muted-foreground" />
          <input
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(1); }}
            placeholder="Search business name, email, plan..."
            className="w-full bg-muted/50 border border-border rounded-xl px-3 py-1.5 text-xs focus:outline-none"
          />
        </div>
        <div className="flex items-center gap-2">
          <select value={filters.status || ''} onChange={e => setFilter('status', e.target.value)} className="bg-muted/50 border border-border rounded-xl px-3 py-1.5 text-xs">
            <option value="">All Statuses</option>
            {STATUSES.filter(Boolean).map(s => <option key={s} value={s}>{s}</option>)}
          </select>
          <select value={filters.planId || ''} onChange={e => setFilter('planId', e.target.value)} className="bg-muted/50 border border-border rounded-xl px-3 py-1.5 text-xs">
            <option value="">All Plans</option>
            {PLANS.filter(Boolean).map(p => <option key={p} value={p}>{p}</option>)}
          </select>
        </div>
      </div>

      {/* Table */}
      <div className="bg-card border border-border rounded-2xl overflow-hidden">
        <table className="w-full text-xs">
          <thead className="bg-muted/40 border-b border-border">
            <tr>
              <th className="px-4 py-3 text-left font-bold text-muted-foreground">Business</th>
              <th className="px-4 py-3 text-left font-bold text-muted-foreground">Plan</th>
              <th className="px-4 py-3 text-left font-bold text-muted-foreground">Billing Cycle</th>
              <th className="px-4 py-3 text-left font-bold text-muted-foreground">Days Left</th>
              <th className="px-4 py-3 text-left font-bold text-muted-foreground">Status</th>
              <th className="px-4 py-3 text-right font-bold text-muted-foreground">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i} className="animate-pulse">
                  <td colSpan={6} className="px-4 py-3"><div className="h-4 bg-muted rounded" /></td>
                </tr>
              ))
            ) : subs.length === 0 ? (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-muted-foreground">No subscriptions found.</td>
              </tr>
            ) : (
              subs.map((s: any) => (
                <tr key={s.id} className="hover:bg-muted/20">
                  <td className="px-4 py-3 font-bold">{s.business?.name || 'Unknown'}</td>
                  <td className="px-4 py-3 font-bold text-primary">{s.planId}</td>
                  <td className="px-4 py-3 text-muted-foreground">{s.billingCycle}</td>
                  <td className="px-4 py-3 font-mono font-bold">{daysLeft(s.expiryDate)} days</td>
                  <td className="px-4 py-3"><StatusBadge status={s.status} /></td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex items-center justify-end gap-1.5">
                      <button
                        onClick={() => {
                          setEditingSub(s);
                          setEditForm({ planId: s.planId, status: s.status });
                        }}
                        className="p-1.5 rounded bg-blue-500/10 text-blue-400 border border-blue-500/20 hover:bg-blue-500/20"
                        title="Edit Subscription"
                      >
                        <Edit3 size={13} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Pagination */}
        <div className="p-3 border-t border-border flex items-center justify-between text-xs text-muted-foreground">
          <span>Page {page} of {meta?.lastPage || 1}</span>
          <div className="flex gap-2">
            <button disabled={page <= 1} onClick={() => setPage(p => p - 1)} className="px-3 py-1 bg-card border border-border rounded-lg disabled:opacity-40"><ChevronLeft size={14} /></button>
            <button disabled={page >= (meta?.lastPage || 1)} onClick={() => setPage(p => p + 1)} className="px-3 py-1 bg-card border border-border rounded-lg disabled:opacity-40"><ChevronRight size={14} /></button>
          </div>
        </div>
      </div>

      {/* Create Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-card border border-border rounded-2xl p-6 max-w-md w-full space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <h3 className="font-black text-base flex items-center gap-2">
                <Plus className="text-primary" size={18} /> Assign / Create Subscription
              </h3>
              <button onClick={() => setShowCreateModal(false)} className="p-1 rounded-lg hover:bg-muted">
                <X size={16} />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div>
                <label className="font-bold text-muted-foreground">Business ID *</label>
                <input
                  value={createForm.businessId}
                  onChange={e => setCreateForm({ ...createForm, businessId: e.target.value })}
                  placeholder="e.g. uuid of business"
                  className="w-full bg-muted/60 border border-border rounded-xl px-3 py-2 mt-1 focus:outline-none focus:border-primary font-mono"
                />
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="font-bold text-muted-foreground">Plan</label>
                  <select
                    value={createForm.planId}
                    onChange={e => setCreateForm({ ...createForm, planId: e.target.value })}
                    className="w-full bg-muted/60 border border-border rounded-xl px-3 py-2 mt-1 focus:outline-none focus:border-primary font-bold"
                  >
                    <option value="STARTER">Starter</option>
                    <option value="GROWTH">Growth</option>
                    <option value="TRIAL">Trial</option>
                  </select>
                </div>
                <div>
                  <label className="font-bold text-muted-foreground">Status</label>
                  <select
                    value={createForm.status}
                    onChange={e => setCreateForm({ ...createForm, status: e.target.value })}
                    className="w-full bg-muted/60 border border-border rounded-xl px-3 py-2 mt-1 focus:outline-none focus:border-primary font-bold"
                  >
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="TRIAL">TRIAL</option>
                    <option value="EXPIRED">EXPIRED</option>
                    <option value="CANCELLED">CANCELLED</option>
                  </select>
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-border">
              <button onClick={() => setShowCreateModal(false)} className="px-4 py-2 border border-border rounded-xl text-xs font-bold">
                Cancel
              </button>
              <button
                onClick={() => createMutation.mutate(createForm)}
                disabled={createMutation.isPending || !createForm.businessId}
                className="px-4 py-2 bg-primary text-black font-black text-xs rounded-xl hover:bg-primary/90 disabled:opacity-50"
              >
                Create Subscription
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Modal */}
      {editingSub && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-card border border-border rounded-2xl p-6 max-w-md w-full space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <h3 className="font-black text-base flex items-center gap-2">
                <Edit3 className="text-primary" size={18} /> Edit Subscription Details
              </h3>
              <button onClick={() => setEditingSub(null)} className="p-1 rounded-lg hover:bg-muted">
                <X size={16} />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="font-bold text-muted-foreground">Plan</label>
                  <select
                    value={editForm.planId}
                    onChange={e => setEditForm({ ...editForm, planId: e.target.value })}
                    className="w-full bg-muted/60 border border-border rounded-xl px-3 py-2 mt-1 focus:outline-none focus:border-primary font-bold"
                  >
                    <option value="STARTER">Starter</option>
                    <option value="GROWTH">Growth</option>
                    <option value="TRIAL">Trial</option>
                  </select>
                </div>
                <div>
                  <label className="font-bold text-muted-foreground">Status</label>
                  <select
                    value={editForm.status}
                    onChange={e => setEditForm({ ...editForm, status: e.target.value })}
                    className="w-full bg-muted/60 border border-border rounded-xl px-3 py-2 mt-1 focus:outline-none focus:border-primary font-bold"
                  >
                    <option value="ACTIVE">ACTIVE</option>
                    <option value="TRIAL">TRIAL</option>
                    <option value="EXPIRED">EXPIRED</option>
                    <option value="CANCELLED">CANCELLED</option>
                  </select>
                </div>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-border">
              <button onClick={() => setEditingSub(null)} className="px-4 py-2 border border-border rounded-xl text-xs font-bold">
                Cancel
              </button>
              <button
                onClick={() => updateMutation.mutate({ id: editingSub.id, data: editForm })}
                disabled={updateMutation.isPending}
                className="px-4 py-2 bg-primary text-black font-black text-xs rounded-xl hover:bg-primary/90 disabled:opacity-50"
              >
                Save Changes
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Confirmation Dialog */}
      <ConfirmDialog
        open={!!confirmAction}
        title={confirmAction?.label || 'Confirm Action'}
        description="Are you sure you want to perform this operation on the subscription?"
        confirmLabel="Confirm"
        confirmVariant="danger"
        loading={statusMutation.isPending || resetTrialMutation.isPending}
        onConfirm={() => {
          if (!confirmAction) return;
          if (confirmAction.type === 'resetTrial') resetTrialMutation.mutate(confirmAction.businessId);
          else statusMutation.mutate({ businessId: confirmAction.businessId, status: confirmAction.type });
        }}
        onCancel={() => setConfirmAction(null)}
      />
    </div>
  );
};

export default Subscriptions;
