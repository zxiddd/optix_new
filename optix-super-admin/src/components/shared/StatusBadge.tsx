import React from 'react';
import { cn } from '@/lib/utils';

const STATUS_MAP: Record<string, { label: string; className: string }> = {
  // Payment statuses
  PENDING:   { label: 'Pending',   className: 'bg-yellow-500/15 text-yellow-400 border-yellow-500/30' },
  CAPTURED:  { label: 'Captured',  className: 'bg-green-500/15 text-green-400 border-green-500/30' },
  FAILED:    { label: 'Failed',    className: 'bg-red-500/15 text-red-400 border-red-500/30' },
  REFUNDED:  { label: 'Refunded',  className: 'bg-blue-500/15 text-blue-400 border-blue-500/30' },
  CANCELLED: { label: 'Cancelled', className: 'bg-zinc-500/15 text-zinc-400 border-zinc-500/30' },
  EXPIRED:   { label: 'Expired',   className: 'bg-orange-500/15 text-orange-400 border-orange-500/30' },
  // Subscription statuses
  ACTIVE:    { label: 'Active',    className: 'bg-green-500/15 text-green-400 border-green-500/30' },
  TRIAL:     { label: 'Trial',     className: 'bg-purple-500/15 text-purple-400 border-purple-500/30' },
  SUSPENDED: { label: 'Suspended', className: 'bg-orange-500/15 text-orange-400 border-orange-500/30' },
  PAUSED:    { label: 'Paused',    className: 'bg-zinc-500/15 text-zinc-400 border-zinc-500/30' },
  // Boolean
  true:      { label: 'Active',    className: 'bg-green-500/15 text-green-400 border-green-500/30' },
  false:     { label: 'Inactive',  className: 'bg-zinc-500/15 text-zinc-400 border-zinc-500/30' },
};

interface StatusBadgeProps {
  status: string | boolean;
  size?: 'sm' | 'md';
}

const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'md' }) => {
  const key = String(status).toUpperCase();
  const config = STATUS_MAP[key] || STATUS_MAP[String(status)] || { label: String(status), className: 'bg-zinc-500/15 text-zinc-400 border-zinc-500/30' };

  return (
    <span className={cn(
      'inline-flex items-center gap-1.5 border rounded-full font-semibold tracking-wide',
      size === 'sm' ? 'px-2 py-0.5 text-[10px]' : 'px-2.5 py-1 text-xs',
      config.className
    )}>
      <span className="w-1.5 h-1.5 rounded-full bg-current opacity-70" />
      {config.label}
    </span>
  );
};

export default StatusBadge;
