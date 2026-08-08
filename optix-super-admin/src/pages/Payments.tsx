import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { Search, Filter, Download, RefreshCw, Eye, RotateCcw, ChevronLeft, ChevronRight } from 'lucide-react';
import { paymentService } from '@/services/payment.service';
import StatusBadge from '@/components/shared/StatusBadge';
import RevenueCards from '@/components/shared/RevenueCards';
import RevenueChart from '@/components/shared/RevenueChart';
import ConfirmDialog from '@/components/shared/ConfirmDialog';
import { motion } from 'framer-motion';

const STATUSES = ['', 'PENDING', 'CAPTURED', 'FAILED', 'REFUNDED', 'CANCELLED', 'EXPIRED'];
const PLANS = ['', 'STARTER', 'GROWTH'];

const Payments: React.FC = () => {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [filters, setFilters] = useState<Record<string, string>>({});
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [refundTarget, setRefundTarget] = useState<{ id: string; amount: number } | null>(null);

  const { data: stats, isLoading: statsLoading } = useQuery({
    queryKey: ['revenue-stats'],
    queryFn: paymentService.getRevenueStats,
  });

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['payments', filters, page, search],
    queryFn: () => paymentService.getAll({ ...filters, page, limit: 50, search }),
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
          <h1 className="text-2xl font-black tracking-tight">Payments</h1>
          <p className="text-muted-foreground text-sm mt-0.5">All transactions across every business</p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => refetch()} className="p-2.5 rounded-xl border border-border hover:bg-muted transition-colors" title="Refresh">
            <RefreshCw size={16} />
          </button>
          <button onClick={exportCsv} className="flex items-center gap-2 px-4 py-2.5 rounded-xl border border-border hover:bg-muted transition-colors text-sm font-semibold">
            <Download size={15} /> Export CSV
          </button>
        </div>
      </div>

      {/* Revenue Cards */}
      <RevenueCards stats={stats} loading={statsLoading} />

      {/* Revenue Chart */}
      <RevenueChart planBreakdown={stats?.planBreakdown ?? []} />

      {/* Filters */}
      <div className="bg-card border border-border rounded-2xl p-4">
        <div className="flex flex-wrap items-center gap-3">
          <div className="relative flex-1 min-w-[200px]">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={15} />
            <input
              value={search}
              onChange={e => { setSearch(e.target.value); setPage(1); }}
              placeholder="Search by business, order ID, payment ID..."
              className="w-full bg-muted/50 border border-border rounded-xl py-2.5 pl-9 pr-4 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <select value={filters.status ?? ''} onChange={e => setFilter('status', e.target.value)}
            className="bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20">
            {STATUSES.map(s => <option key={s} value={s}>{s || 'All Statuses'}</option>)}
          </select>
          <select value={filters.planId ?? ''} onChange={e => setFilter('planId', e.target.value)}
            className="bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20">
            {PLANS.map(p => <option key={p} value={p}>{p || 'All Plans'}</option>)}
          </select>
          <input type="date" value={filters.dateFrom ?? ''} onChange={e => setFilter('dateFrom', e.target.value)}
            className="bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
            placeholder="From" />
          <input type="date" value={filters.dateTo ?? ''} onChange={e => setFilter('dateTo', e.target.value)}
            className="bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
            placeholder="To" />
          {Object.values(filters).some(Boolean) && (
            <button onClick={() => { setFilters({}); setPage(1); }}
              className="px-3 py-2.5 text-sm text-red-400 hover:bg-red-500/10 rounded-xl transition-colors">
              Clear
            </button>
          )}
        </div>
      </div>

      {/* Table */}
      <div className="bg-card border border-border rounded-2xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-muted/30">
                {['Transaction ID', 'Business', 'Owner', 'Plan', 'Cycle', 'Amount', 'Country', 'Status', 'Gateway Order', 'Created', 'Actions'].map(h => (
                  <th key={h} className="px-4 py-3.5 text-left text-xs font-bold text-muted-foreground uppercase tracking-wider whitespace-nowrap">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                Array.from({ length: 8 }).map((_, i) => (
                  <tr key={i} className="border-b border-border/50 animate-pulse">
                    {Array.from({ length: 11 }).map((_, j) => (
                      <td key={j} className="px-4 py-4"><div className="h-3 bg-muted rounded w-20" /></td>
                    ))}
                  </tr>
                ))
              ) : payments.length === 0 ? (
                <tr><td colSpan={11} className="px-4 py-16 text-center text-muted-foreground">
                  <Filter size={32} className="mx-auto mb-3 opacity-30" />
                  <p className="font-semibold">No payments found</p>
                  <p className="text-xs mt-1">Try adjusting your filters</p>
                </td></tr>
              ) : payments.map((tx: any, i: number) => (
                <motion.tr
                  key={tx.id}
                  initial={{ opacity: 0, y: 4 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: i * 0.02 }}
                  className="border-b border-border/50 hover:bg-muted/20 transition-colors group"
                >
                  <td className="px-4 py-3.5">
                    <span className="font-mono text-xs text-muted-foreground">{tx.id.slice(0, 8)}…</span>
                  </td>
                  <td className="px-4 py-3.5 font-semibold whitespace-nowrap">{tx.business?.name ?? '—'}</td>
                  <td className="px-4 py-3.5 text-muted-foreground text-xs">{tx.business?.users?.[0]?.email ?? '—'}</td>
                  <td className="px-4 py-3.5">
                    <span className="px-2 py-0.5 text-xs font-bold bg-primary/10 text-primary rounded-lg">{tx.planId}</span>
                  </td>
                  <td className="px-4 py-3.5 text-xs text-muted-foreground">{tx.billingCycle}</td>
                  <td className="px-4 py-3.5 font-bold text-green-400">{tx.currency} {Number(tx.amount).toLocaleString()}</td>
                  <td className="px-4 py-3.5 text-xs text-muted-foreground">{tx.country}</td>
                  <td className="px-4 py-3.5"><StatusBadge status={tx.status} size="sm" /></td>
                  <td className="px-4 py-3.5 font-mono text-xs text-muted-foreground max-w-[140px] truncate">{tx.razorpayOrderId}</td>
                  <td className="px-4 py-3.5 text-xs text-muted-foreground whitespace-nowrap">
                    {new Date(tx.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-4 py-3.5">
                    <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                      <button onClick={() => navigate(`/payments/${tx.id}`)}
                        className="p-1.5 hover:bg-primary/10 text-primary rounded-lg transition-colors" title="View details">
                        <Eye size={14} />
                      </button>
                      {tx.status === 'CAPTURED' && (
                        <button onClick={() => setRefundTarget({ id: tx.id, amount: tx.amount })}
                          className="p-1.5 hover:bg-red-500/10 text-red-400 rounded-lg transition-colors" title="Refund">
                          <RotateCcw size={14} />
                        </button>
                      )}
                    </div>
                  </td>
                </motion.tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        {meta && meta.lastPage > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-border bg-muted/20">
            <p className="text-xs text-muted-foreground">
              Showing {((page - 1) * 50) + 1}–{Math.min(page * 50, meta.total)} of {meta.total}
            </p>
            <div className="flex items-center gap-1">
              <button onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page === 1}
                className="p-1.5 rounded-lg hover:bg-muted disabled:opacity-40 transition-colors">
                <ChevronLeft size={15} />
              </button>
              <span className="px-3 py-1 text-xs font-bold">{page} / {meta.lastPage}</span>
              <button onClick={() => setPage(p => Math.min(meta.lastPage, p + 1))} disabled={page === meta.lastPage}
                className="p-1.5 rounded-lg hover:bg-muted disabled:opacity-40 transition-colors">
                <ChevronRight size={15} />
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Refund Dialog */}
      <ConfirmDialog
        open={!!refundTarget}
        title="Refund Payment"
        description={`Are you sure you want to refund ₹${refundTarget?.amount ? Number(refundTarget.amount).toLocaleString() : ''}? This action cannot be undone.`}
        confirmLabel="Refund"
        confirmVariant="danger"
        loading={refundMutation.isPending}
        onConfirm={() => refundTarget && refundMutation.mutate(refundTarget.id)}
        onCancel={() => setRefundTarget(null)}
      />
    </div>
  );
};

export default Payments;
