import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, ExternalLink, Copy, CheckCircle2, Clock, XCircle, RotateCcw } from 'lucide-react';
import { paymentService } from '@/services/payment.service';
import StatusBadge from '@/components/shared/StatusBadge';

const InfoRow = ({ label, value, mono = false }: { label: string; value: React.ReactNode; mono?: boolean }) => (
  <div className="flex items-start justify-between py-3 border-b border-border/50 last:border-0">
    <span className="text-sm text-muted-foreground w-40 shrink-0">{label}</span>
    <span className={`text-sm font-semibold text-right ${mono ? 'font-mono text-xs' : ''}`}>{value ?? '—'}</span>
  </div>
);

const TimelineStep = ({ icon: Icon, color, label, time }: { icon: any; color: string; label: string; time?: string }) => (
  <div className="flex items-start gap-3">
    <div className={`w-8 h-8 rounded-full flex items-center justify-center shrink-0 ${color}`}>
      <Icon size={14} />
    </div>
    <div>
      <p className="text-sm font-semibold">{label}</p>
      {time && <p className="text-xs text-muted-foreground">{new Date(time).toLocaleString()}</p>}
    </div>
  </div>
);

const PaymentDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const { data: tx, isLoading } = useQuery({
    queryKey: ['payment', id],
    queryFn: () => paymentService.getDetail(id!),
    enabled: !!id,
  });

  const copyToClipboard = (text: string) => navigator.clipboard.writeText(text);

  if (isLoading) {
    return (
      <div className="space-y-4 animate-pulse">
        <div className="h-8 bg-muted rounded w-40" />
        <div className="grid grid-cols-2 gap-4">
          {Array.from({ length: 4 }).map((_, i) => <div key={i} className="h-48 bg-muted rounded-2xl" />)}
        </div>
      </div>
    );
  }

  if (!tx) return <div className="text-center py-20 text-muted-foreground">Payment not found</div>;

  const gateway = tx.gatewayMetadata as any;

  return (
    <div className="space-y-6 max-w-5xl">
      {/* Header */}
      <div className="flex items-center gap-4">
        <button onClick={() => navigate(-1)} className="p-2 rounded-xl hover:bg-muted transition-colors">
          <ArrowLeft size={18} />
        </button>
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-2xl font-black tracking-tight">Payment Detail</h1>
            <StatusBadge status={tx.status} />
          </div>
          <p className="text-muted-foreground text-xs font-mono mt-0.5">{tx.id}</p>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Business & Plan Info */}
        <div className="bg-card border border-border rounded-2xl p-6">
          <h2 className="text-sm font-bold mb-4 text-muted-foreground uppercase tracking-wider">Business & Plan</h2>
          <InfoRow label="Business" value={tx.business?.name} />
          <InfoRow label="Owner" value={tx.business?.users?.[0]?.email} />
          <InfoRow label="Country" value={tx.country} />
          <InfoRow label="Plan" value={
            <span className="px-2 py-0.5 bg-primary/10 text-primary text-xs font-bold rounded-lg">{tx.planId}</span>
          } />
          <InfoRow label="Billing Cycle" value={tx.billingCycle} />
          <InfoRow label="Amount" value={
            <span className="text-green-400 font-black text-base">{tx.currency} {Number(tx.amount).toLocaleString()}</span>
          } />
          <InfoRow label="Currency" value={tx.currency} />
        </div>

        {/* Gateway IDs */}
        <div className="bg-card border border-border rounded-2xl p-6">
          <h2 className="text-sm font-bold mb-4 text-muted-foreground uppercase tracking-wider">Gateway Details</h2>
          <div className="space-y-3">
            {[
              { label: 'Razorpay Order ID', value: tx.razorpayOrderId },
              { label: 'Razorpay Payment ID', value: tx.razorpayPaymentId },
              { label: 'Signature', value: tx.razorpaySignature },
            ].map(({ label, value }) => (
              <div key={label}>
                <p className="text-xs text-muted-foreground mb-1">{label}</p>
                <div className="flex items-center gap-2 bg-muted/50 rounded-xl px-3 py-2">
                  <span className="font-mono text-xs flex-1 truncate">{value ?? '—'}</span>
                  {value && (
                    <button onClick={() => copyToClipboard(value)} className="text-muted-foreground hover:text-foreground transition-colors shrink-0">
                      <Copy size={12} />
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>

          {tx.failureReason && (
            <div className="mt-4 p-3 bg-red-500/10 border border-red-500/20 rounded-xl">
              <p className="text-xs font-bold text-red-400 mb-1">Failure Reason</p>
              <p className="text-xs text-red-300">{tx.failureReason}</p>
            </div>
          )}
        </div>

        {/* Payment Timeline */}
        <div className="bg-card border border-border rounded-2xl p-6">
          <h2 className="text-sm font-bold mb-4 text-muted-foreground uppercase tracking-wider">Payment Timeline</h2>
          <div className="space-y-4">
            <TimelineStep icon={Clock} color="bg-yellow-500/15 text-yellow-400" label="Order Created" time={tx.createdAt} />
            {tx.status === 'CAPTURED' && (
              <TimelineStep icon={CheckCircle2} color="bg-green-500/15 text-green-400" label="Payment Captured" time={tx.capturedAt} />
            )}
            {tx.status === 'FAILED' && (
              <TimelineStep icon={XCircle} color="bg-red-500/15 text-red-400" label="Payment Failed" time={tx.capturedAt ?? tx.createdAt} />
            )}
            {tx.status === 'REFUNDED' && (
              <TimelineStep icon={RotateCcw} color="bg-blue-500/15 text-blue-400" label="Payment Refunded" time={gateway?.refundedAt ?? tx.capturedAt} />
            )}
          </div>
        </div>

        {/* Raw Gateway Metadata */}
        <div className="bg-card border border-border rounded-2xl p-6">
          <h2 className="text-sm font-bold mb-4 text-muted-foreground uppercase tracking-wider">Gateway Metadata</h2>
          {tx.gatewayMetadata ? (
            <pre className="text-xs text-muted-foreground bg-muted/50 rounded-xl p-4 overflow-auto max-h-48 leading-relaxed">
              {JSON.stringify(tx.gatewayMetadata, null, 2)}
            </pre>
          ) : (
            <p className="text-sm text-muted-foreground italic">No metadata available</p>
          )}
        </div>
      </div>

      {/* Subscription Info */}
      {tx.subscription && (
        <div className="bg-card border border-border rounded-2xl p-6">
          <h2 className="text-sm font-bold mb-4 text-muted-foreground uppercase tracking-wider">Linked Subscription</h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[
              { label: 'Plan', value: tx.subscription?.plan?.name },
              { label: 'Status', value: <StatusBadge status={tx.subscription?.status ?? ''} size="sm" /> },
              { label: 'Billing Cycle', value: tx.subscription?.billingCycle },
              { label: 'Expires', value: tx.subscription?.expiryDate ? new Date(tx.subscription.expiryDate).toLocaleDateString() : '—' },
            ].map(({ label, value }) => (
              <div key={label} className="bg-muted/30 rounded-xl p-4">
                <p className="text-xs text-muted-foreground mb-1">{label}</p>
                <p className="font-semibold">{value}</p>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};

export default PaymentDetail;
