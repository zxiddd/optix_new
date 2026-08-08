import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { Search, Download, RefreshCw, Eye, RotateCcw, ChevronLeft, ChevronRight, Plus, Edit3, X } from 'lucide-react';
import { paymentService } from '@/services/payment.service';
import StatusBadge from '@/components/shared/StatusBadge';
import RevenueCards from '@/components/shared/RevenueCards';
import ConfirmDialog from '@/components/shared/ConfirmDialog';
import axios from 'axios';

const authHeader = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
const API_URL = 'https://api.optixapp.in/api/v1/super-admin/payments';

const STATUSES = ['', 'PENDING', 'CAPTURED', 'FAILED', 'REFUNDED', 'CANCELLED', 'EXPIRED'];
const PLANS = ['', 'STARTER', 'GROWTH'];

const Payments: React.FC = () => {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [filters, setFilters] = useState<Record<string, string>>({});
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [refundTarget, setRefundTarget] = useState<{ id: string; amount: number } | null>(null);

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingPayment, setEditingPayment] = useState<any | null>(null);

  const [createForm, setCreateForm] = useState({
    businessId: '',
    amount: 1999,
    planId: 'STARTER',
    billingCycle: 'MONTHLY',
    status: 'CAPTURED',
    gatewayPaymentId: '',
  });

  const [editForm, setEditForm] = useState({
    status: 'CAPTURED',
    amount: 1999,
    gatewayPaymentId: '',
    gatewayOrderId: '',
  });

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['revenue-stats'],
    queryFn: paymentService.getRevenueStats,
  });

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['payments', filters, page, search],
    queryFn: () => paymentService.getAll({ ...filters, page, limit: 50, search }),
  });

  const createMutation = useMutation({
    mutationFn: async (data: any) => {
      const res = await axios.post(API_URL, data, authHeader());
      return res.data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['payments'] });
      qc.invalidateQueries({ queryKey: ['revenue-stats'] });
      setShowCreateModal(false);
    },
  });

  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: string; data: any }) => {
      const res = await axios.patch(`${API_URL}/${id}`, data, authHeader());
      return res.data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['payments'] });
      qc.invalidateQueries({ queryKey: ['revenue-stats'] });
      setEditingPayment(null);
    },
  });

  const refundMutation = useMutation({
    mutationFn: (id: string) => paymentService.refund(id, 'Admin initiated refund'),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ['payments'] }); setRefundTarget(null); },
  });

  const setFilter = (key: string, val: string) => {
    setFilters(f => ({ ...f, [key]: val }));
    setPage(1);
  };

  const exportCsv = () => {
    const rows = data?.items ?? [];
    if (!rows.length) return;
    const headers = ['ID', 'Business', 'Plan', 'Cycle', 'Amount', 'Currency', 'Country', 'Status', 'Gateway Order', 'Gateway Payment', 'Created'];
    const csvContent = [headers, ...rows.map((r: any) => [
      r.id, r.business?.name ?? '', r.planId, r.billingCycle, r.amount, r.currency,
      r.country, r.status, r.razorpayOrderId, r.razorpayPaymentId ?? '',
      new Date(r.createdAt).toLocaleString(),
    ])].map(row => row.join(',')).join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = 'payments.csv'; a.click();
    URL.revokeObjectURL(url);
  };

  const payments: any[] = data?.items ?? [];
  const meta = data?.meta;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black tracking-tight">Payments & Revenue Control</h1>
          <p className="text-muted-foreground text-sm mt-0.5">Manage, override, create and refund transactions across every business.</p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowCreateModal(true)}
            className="flex items-center gap-1.5 px-4 py-2 bg-primary text-black font-black text-xs rounded-xl hover:bg-primary/90 transition-colors shadow-lg shadow-primary/20"
          >
            <Plus size={15} /> Create Payment Record
          </button>
          <button onClick={() => refetch()} className="p-2.5 rounded-xl border border-border hover:bg-muted transition-colors" title="Refresh">
            <RefreshCw size={16} />
          </button>
          <button onClick={exportCsv} className="flex items-center gap-2 px-4 py-2 text-xs rounded-xl border border-border hover:bg-muted transition-colors font-bold">
            <Download size={14} /> Export CSV
          </button>
        </div>
      </div>

      {/* Revenue Cards */}
      <RevenueCards stats={stats} loading={statsLoading} />

      {/* Search & Filters */}
      <div className="bg-card border border-border rounded-2xl p-4 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-2 flex-1 max-w-sm">
          <Search size={15} className="text-muted-foreground" />
          <input
            value={search}
            onChange={e => { setSearch(e.target.value); setPage(1); }}
            placeholder="Search payment ID, business, gateway ID..."
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
              <th className="px-4 py-3 text-left font-bold text-muted-foreground">Transaction ID</th>
              <th className="px-4 py-3 text-left font-bold text-muted-foreground">Business</th>
              <th className="px-4 py-3 text-left font-bold text-muted-foreground">Plan / Cycle</th>
              <th className="px-4 py-3 text-left font-bold text-muted-foreground">Amount</th>
              <th className="px-4 py-3 text-left font-bold text-muted-foreground">Gateway Payment ID</th>
              <th className="px-4 py-3 text-left font-bold text-muted-foreground">Status</th>
              <th className="px-4 py-3 text-left font-bold text-muted-foreground">Date</th>
              <th className="px-4 py-3 text-right font-bold text-muted-foreground">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {isLoading ? (
              Array.from({ length: 5 }).map((_, i) => (
                <tr key={i} className="animate-pulse">
                  <td colSpan={8} className="px-4 py-3"><div className="h-4 bg-muted rounded" /></td>
                </tr>
              ))
            ) : payments.length === 0 ? (
              <tr>
                <td colSpan={8} className="px-4 py-8 text-center text-muted-foreground">No payments found matching criteria.</td>
              </tr>
            ) : (
              payments.map((p: any) => (
                <tr key={p.id} className="hover:bg-muted/20">
                  <td className="px-4 py-3 font-mono font-bold text-primary">{p.id.slice(0, 8)}...</td>
                  <td className="px-4 py-3 font-bold">{p.business?.name || 'Unknown'}</td>
                  <td className="px-4 py-3"><span className="font-bold">{p.planId}</span> ({p.billingCycle})</td>
                  <td className="px-4 py-3 font-mono font-black text-sm">₹{Number(p.amount).toLocaleString('en-IN')}</td>
                  <td className="px-4 py-3 font-mono text-[11px] text-muted-foreground">{p.razorpayPaymentId || p.razorpayOrderId || '-'}</td>
                  <td className="px-4 py-3"><StatusBadge status={p.status} /></td>
                  <td className="px-4 py-3 text-muted-foreground">{new Date(p.createdAt).toLocaleDateString()}</td>
                  <td className="px-4 py-3 text-right">
                    <div className="flex items-center justify-end gap-1.5">
                      <button
                        onClick={() => {
                          setEditingPayment(p);
                          setEditForm({
                            status: p.status,
                            amount: Number(p.amount),
                            gatewayPaymentId: p.razorpayPaymentId || '',
                            gatewayOrderId: p.razorpayOrderId || '',
                          });
                        }}
                        className="p-1.5 rounded bg-blue-500/10 text-blue-400 border border-blue-500/20 hover:bg-blue-500/20"
                        title="Edit Payment"
                      >
                        <Edit3 size={13} />
                      </button>
                      <button
                        onClick={() => navigate(`/payments/${p.id}`)}
                        className="p-1.5 rounded bg-muted hover:bg-muted/80 text-muted-foreground"
                        title="View Details"
                      >
                        <Eye size={13} />
                      </button>
                      {p.status === 'CAPTURED' && (
                        <button
                          onClick={() => setRefundTarget({ id: p.id, amount: Number(p.amount) })}
                          className="p-1.5 rounded bg-red-500/10 text-red-400 border border-red-500/20 hover:bg-red-500/20"
                          title="Issue Refund"
                        >
                          <RotateCcw size={13} />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        {/* Pagination */}
        <div className="p-3 border-t border-border flex items-center justify-between text-xs text-muted-foreground">
          <span>Showing page {page} of {meta?.lastPage || 1}</span>
          <div className="flex gap-2">
            <button disabled={page <= 1} onClick={() => setPage(p => p - 1)} className="px-3 py-1 bg-card border border-border rounded-lg disabled:opacity-40"><ChevronLeft size={14} /></button>
            <button disabled={page >= (meta?.lastPage || 1)} onClick={() => setPage(p => p + 1)} className="px-3 py-1 bg-card border border-border rounded-lg disabled:opacity-40"><ChevronRight size={14} /></button>
          </div>
        </div>
      </div>

      {/* Create Payment Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-card border border-border rounded-2xl p-6 max-w-md w-full space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <h3 className="font-black text-base flex items-center gap-2">
                <Plus className="text-primary" size={18} /> Create Manual Payment
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
                  <label className="font-bold text-muted-foreground">Amount (₹) *</label>
                  <input
                    type="number"
                    value={createForm.amount}
                    onChange={e => setCreateForm({ ...createForm, amount: Number(e.target.value) })}
                    className="w-full bg-muted/60 border border-border rounded-xl px-3 py-2 mt-1 font-bold focus:outline-none focus:border-primary"
                  />
                </div>
                <div>
                  <label className="font-bold text-muted-foreground">Plan</label>
                  <select
                    value={createForm.planId}
                    onChange={e => setCreateForm({ ...createForm, planId: e.target.value })}
                    className="w-full bg-muted/60 border border-border rounded-xl px-3 py-2 mt-1 focus:outline-none focus:border-primary"
                  >
                    <option value="STARTER">Starter</option>
                    <option value="GROWTH">Growth</option>
                  </select>
                </div>
              </div>
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="font-bold text-muted-foreground">Billing Cycle</label>
                  <select
                    value={createForm.billingCycle}
                    onChange={e => setCreateForm({ ...createForm, billingCycle: e.target.value })}
                    className="w-full bg-muted/60 border border-border rounded-xl px-3 py-2 mt-1 focus:outline-none focus:border-primary"
                  >
                    <option value="MONTHLY">Monthly</option>
                    <option value="YEARLY">Yearly</option>
                  </select>
                </div>
                <div>
                  <label className="font-bold text-muted-foreground">Status</label>
                  <select
                    value={createForm.status}
                    onChange={e => setCreateForm({ ...createForm, status: e.target.value })}
                    className="w-full bg-muted/60 border border-border rounded-xl px-3 py-2 mt-1 focus:outline-none focus:border-primary"
                  >
                    <option value="CAPTURED">Captured</option>
                    <option value="PENDING">Pending</option>
                    <option value="FAILED">Failed</option>
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
                Create Payment
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Edit Payment Modal */}
      {editingPayment && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-card border border-border rounded-2xl p-6 max-w-md w-full space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <h3 className="font-black text-base flex items-center gap-2">
                <Edit3 className="text-primary" size={18} /> Override Payment Record
              </h3>
              <button onClick={() => setEditingPayment(null)} className="p-1 rounded-lg hover:bg-muted">
                <X size={16} />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="font-bold text-muted-foreground">Status</label>
                  <select
                    value={editForm.status}
                    onChange={e => setEditForm({ ...editForm, status: e.target.value })}
                    className="w-full bg-muted/60 border border-border rounded-xl px-3 py-2 mt-1 focus:outline-none focus:border-primary font-bold"
                  >
                    <option value="CAPTURED">CAPTURED</option>
                    <option value="PENDING">PENDING</option>
                    <option value="FAILED">FAILED</option>
                    <option value="REFUNDED">REFUNDED</option>
                    <option value="CANCELLED">CANCELLED</option>
                  </select>
                </div>
                <div>
                  <label className="font-bold text-muted-foreground">Amount (₹)</label>
                  <input
                    type="number"
                    value={editForm.amount}
                    onChange={e => setEditForm({ ...editForm, amount: Number(e.target.value) })}
                    className="w-full bg-muted/60 border border-border rounded-xl px-3 py-2 mt-1 focus:outline-none focus:border-primary font-bold"
                  />
                </div>
              </div>
              <div>
                <label className="font-bold text-muted-foreground">Gateway Payment ID</label>
                <input
                  value={editForm.gatewayPaymentId}
                  onChange={e => setEditForm({ ...editForm, gatewayPaymentId: e.target.value })}
                  className="w-full bg-muted/60 border border-border rounded-xl px-3 py-2 mt-1 font-mono focus:outline-none focus:border-primary"
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-border">
              <button onClick={() => setEditingPayment(null)} className="px-4 py-2 border border-border rounded-xl text-xs font-bold">
                Cancel
              </button>
              <button
                onClick={() => updateMutation.mutate({ id: editingPayment.id, data: editForm })}
                disabled={updateMutation.isPending}
                className="px-4 py-2 bg-primary text-black font-black text-xs rounded-xl hover:bg-primary/90 disabled:opacity-50"
              >
                Save Override
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Refund Confirm */}
      <ConfirmDialog
        open={!!refundTarget}
        title="Issue Refund?"
        description={`Refund ₹${refundTarget?.amount} back to business customer.`}
        confirmLabel="Issue Refund"
        confirmVariant="danger"
        loading={refundMutation.isPending}
        onConfirm={() => refundTarget && refundMutation.mutate(refundTarget.id)}
        onCancel={() => setRefundTarget(null)}
      />
    </div>
  );
};

export default Payments;
