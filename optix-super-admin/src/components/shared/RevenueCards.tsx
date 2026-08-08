import React from 'react';
import { TrendingUp, TrendingDown, Minus } from 'lucide-react';

interface StatCard {
  label: string;
  value: string;
  subValue?: string;
  trend?: 'up' | 'down' | 'neutral';
  color?: string;
}

interface RevenueCardsProps {
  stats?: {
    today?: { revenue: number; count: number };
    month?: { revenue: number; count: number };
    year?: { revenue: number; count: number };
    mrr?: number;
    arr?: number;
    avgRevPerBusiness?: number;
    activeSubscriptions?: number;
    failedPayments?: number;
    refunds?: number;
  };
  loading?: boolean;
}

const fmt = (n: number, currency = '₹') =>
  n >= 100000 ? `${currency}${(n / 100000).toFixed(1)}L`
  : n >= 1000 ? `${currency}${(n / 1000).toFixed(1)}K`
  : `${currency}${n.toFixed(0)}`;

const RevenueCards: React.FC<RevenueCardsProps> = ({ stats, loading }) => {
  if (loading) {
    return (
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {Array.from({ length: 8 }).map((_, i) => (
          <div key={i} className="bg-card border border-border rounded-2xl p-5 animate-pulse">
            <div className="h-3 bg-muted rounded w-20 mb-3" />
            <div className="h-7 bg-muted rounded w-28" />
          </div>
        ))}
      </div>
    );
  }

  const cards: StatCard[] = [
    { label: "Today's Revenue", value: fmt(stats?.today?.revenue ?? 0), subValue: `${stats?.today?.count ?? 0} txns`, trend: 'up', color: 'text-primary' },
    { label: 'Monthly Revenue', value: fmt(stats?.month?.revenue ?? 0), subValue: `${stats?.month?.count ?? 0} txns`, trend: 'up', color: 'text-green-400' },
    { label: 'Yearly Revenue', value: fmt(stats?.year?.revenue ?? 0), subValue: `${stats?.year?.count ?? 0} txns`, trend: 'up', color: 'text-blue-400' },
    { label: 'MRR', value: fmt(stats?.mrr ?? 0), subValue: `ARR ${fmt(stats?.arr ?? 0)}`, trend: 'neutral', color: 'text-purple-400' },
    { label: 'Avg Rev / Business', value: fmt(stats?.avgRevPerBusiness ?? 0), trend: 'neutral', color: 'text-cyan-400' },
    { label: 'Active Subscriptions', value: String(stats?.activeSubscriptions ?? 0), trend: 'up', color: 'text-green-400' },
    { label: 'Failed Payments', value: String(stats?.failedPayments ?? 0), trend: 'down', color: 'text-red-400' },
    { label: 'Refunds', value: String(stats?.refunds ?? 0), trend: 'down', color: 'text-orange-400' },
  ];

  return (
    <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
      {cards.map((card, i) => (
        <div key={i} className="bg-card border border-border rounded-2xl p-5 hover:border-primary/30 transition-colors group">
          <div className="flex items-center justify-between mb-2">
            <p className="text-xs text-muted-foreground font-medium tracking-wide uppercase">{card.label}</p>
            {card.trend === 'up' && <TrendingUp size={14} className="text-green-500 opacity-0 group-hover:opacity-100 transition-opacity" />}
            {card.trend === 'down' && <TrendingDown size={14} className="text-red-500 opacity-0 group-hover:opacity-100 transition-opacity" />}
            {card.trend === 'neutral' && <Minus size={14} className="text-muted-foreground opacity-0 group-hover:opacity-100 transition-opacity" />}
          </div>
          <p className={`text-2xl font-black tracking-tight ${card.color}`}>{card.value}</p>
          {card.subValue && <p className="text-xs text-muted-foreground mt-1">{card.subValue}</p>}
        </div>
      ))}
    </div>
  );
};

export default RevenueCards;
