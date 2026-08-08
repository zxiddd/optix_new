import React from 'react';
import {
  AreaChart, Area, BarChart, Bar, PieChart, Pie, Cell,
  XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, Legend
} from 'recharts';

const COLORS = ['#f97316', '#22c55e', '#3b82f6', '#a855f7', '#06b6d4', '#ec4899'];

interface RevenueChartProps {
  planBreakdown?: Array<{ planId: string; billingCycle: string; _count: number; _sum: { amount: number } }>;
  revenueData?: Array<{ date: string; revenue: number; count: number }>;
}

const CustomTooltip = ({ active, payload, label }: any) => {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-card border border-border rounded-xl px-4 py-3 shadow-xl text-sm">
      <p className="text-muted-foreground mb-1 text-xs">{label}</p>
      {payload.map((p: any, i: number) => (
        <p key={i} style={{ color: p.color }} className="font-bold">
          {p.name}: {typeof p.value === 'number' && p.name.toLowerCase().includes('rev') ? `₹${p.value.toLocaleString()}` : p.value}
        </p>
      ))}
    </div>
  );
};

const RevenueChart: React.FC<RevenueChartProps> = ({ planBreakdown = [], revenueData = [] }) => {
  // Build plan pie data
  const pieData = planBreakdown.map(p => ({
    name: `${p.planId} ${p.billingCycle}`,
    value: p._count,
    amount: Number(p._sum?.amount ?? 0),
  }));

  // Build dummy trend if no real data (graceful fallback)
  const trendData = revenueData.length > 0 ? revenueData : [
    { date: 'Jan', revenue: 0, count: 0 },
    { date: 'Feb', revenue: 0, count: 0 },
    { date: 'Mar', revenue: 0, count: 0 },
  ];

  return (
    <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
      {/* Revenue Trend */}
      <div className="lg:col-span-2 bg-card border border-border rounded-2xl p-6">
        <h3 className="text-sm font-bold text-foreground mb-4">Revenue Trend</h3>
        <ResponsiveContainer width="100%" height={220}>
          <AreaChart data={trendData}>
            <defs>
              <linearGradient id="revGrad" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#f97316" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#f97316" stopOpacity={0} />
              </linearGradient>
            </defs>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
            <XAxis dataKey="date" tick={{ fill: '#6b7280', fontSize: 11 }} axisLine={false} tickLine={false} />
            <YAxis tick={{ fill: '#6b7280', fontSize: 11 }} axisLine={false} tickLine={false} />
            <Tooltip content={<CustomTooltip />} />
            <Area type="monotone" dataKey="revenue" name="Revenue" stroke="#f97316" strokeWidth={2} fill="url(#revGrad)" />
          </AreaChart>
        </ResponsiveContainer>
      </div>

      {/* Plan Breakdown */}
      <div className="bg-card border border-border rounded-2xl p-6">
        <h3 className="text-sm font-bold text-foreground mb-4">Plan Breakdown</h3>
        {pieData.length === 0 ? (
          <div className="flex items-center justify-center h-[220px] text-muted-foreground text-sm">No data yet</div>
        ) : (
          <>
            <ResponsiveContainer width="100%" height={140}>
              <PieChart>
                <Pie data={pieData} cx="50%" cy="50%" innerRadius={45} outerRadius={65} paddingAngle={3} dataKey="value">
                  {pieData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip content={<CustomTooltip />} />
              </PieChart>
            </ResponsiveContainer>
            <div className="space-y-2 mt-2">
              {pieData.map((p, i) => (
                <div key={i} className="flex items-center justify-between text-xs">
                  <div className="flex items-center gap-2">
                    <div className="w-2 h-2 rounded-full" style={{ background: COLORS[i % COLORS.length] }} />
                    <span className="text-muted-foreground">{p.name}</span>
                  </div>
                  <span className="font-bold text-foreground">{p.value}</span>
                </div>
              ))}
            </div>
          </>
        )}
      </div>
    </div>
  );
};

export default RevenueChart;
