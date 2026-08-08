import React from 'react';
import {
  Users,
  Activity,
  BadgeCheck,
  Zap,
  TrendingUp,
  Clock,
  AlertCircle,
  ArrowUpRight,
  ArrowDownRight,
  ChevronRight,
  Server,
  CreditCard
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar
} from 'recharts';
import { mockStats, mockRevenueData, mockActivities } from '@/services/mockData';
import { cn } from '@/lib/utils';

const StatCard: React.FC<{
  title: string;
  value: string | number;
  icon: any;
  trend?: { value: string; positive: boolean };
  subtitle?: string;
  color?: string;
}> = ({ title, value, icon: Icon, trend, subtitle, color = "primary" }) => (
  <div className="bg-card border border-border p-6 rounded-2xl hover:border-primary/50 transition-all group">
    <div className="flex justify-between items-start mb-4">
      <div className={cn("p-3 rounded-xl", `bg-${color}/10 text-${color}`)}>
        <Icon size={24} />
      </div>
      {trend && (
        <div className={cn(
          "flex items-center gap-1 text-xs font-bold px-2 py-1 rounded-lg",
          trend.positive ? "bg-green-500/10 text-green-500" : "bg-red-500/10 text-red-500"
        )}>
          {trend.positive ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />}
          {trend.value}
        </div>
      )}
    </div>
    <h3 className="text-muted-foreground text-sm font-medium mb-1">{title}</h3>
    <div className="flex items-baseline gap-2">
      <span className="text-3xl font-black tracking-tight">{value}</span>
      {subtitle && <span className="text-[10px] text-muted-foreground font-bold uppercase tracking-wider">{subtitle}</span>}
    </div>
  </div>
);

const Dashboard: React.FC = () => {
  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <div className="flex justify-between items-end">
        <div>
          <h1 className="text-4xl font-black tracking-tighter">OVERVIEW</h1>
          <p className="text-muted-foreground mt-1 font-medium">Real-time platform metrics and ecosystem health.</p>
        </div>
        <div className="flex gap-3">
          <button className="bg-muted hover:bg-muted/80 border border-border px-4 py-2 rounded-xl text-sm font-bold transition-colors">
            Last 30 Days
          </button>
          <button className="bg-primary text-black px-4 py-2 rounded-xl text-sm font-black hover:opacity-90 transition-all shadow-lg shadow-primary/20">
            EXPORT DATA
          </button>
        </div>
      </div>

      {/* Main Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Total Businesses"
          value={mockStats.totalBusinesses}
          icon={Users}
          trend={{ value: "+12%", positive: true }}
          subtitle="Lifetime"
        />
        <StatCard
          title="Online Now"
          value={mockStats.onlineBusinesses}
          icon={Activity}
          color="green-500"
          subtitle="Active Nodes"
        />
        <StatCard
          title="Monthly Revenue"
          value={`₹${(mockStats.monthlyRevenue / 100000).toFixed(1)}L`}
          icon={TrendingUp}
          trend={{ value: "+24%", positive: true }}
          subtitle="Projected"
        />
        <StatCard
          title="Socket Connections"
          value={mockStats.socketConnections}
          icon={Zap}
          color="yellow-500"
          subtitle="Live Sync"
        />
      </div>

      {/* Plan Distribution Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-card border border-border p-4 rounded-2xl flex items-center gap-4">
          <div className="w-12 h-12 bg-muted rounded-xl flex items-center justify-center font-black text-xl">T</div>
          <div>
            <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider">Trial Users</p>
            <p className="text-2xl font-black leading-tight">{mockStats.trialUsers}</p>
          </div>
        </div>
        <div className="bg-card border border-border p-4 rounded-2xl flex items-center gap-4">
          <div className="w-12 h-12 bg-primary/10 text-primary rounded-xl flex items-center justify-center font-black text-xl italic">S</div>
          <div>
            <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider">Starter Users</p>
            <p className="text-2xl font-black leading-tight">{mockStats.starterUsers}</p>
          </div>
        </div>
        <div className="bg-card border border-border p-4 rounded-2xl flex items-center gap-4">
          <div className="w-12 h-12 bg-yellow-500/10 text-yellow-500 rounded-xl flex items-center justify-center font-black text-xl italic">G</div>
          <div>
            <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider">Growth Users</p>
            <p className="text-2xl font-black leading-tight">{mockStats.growthUsers}</p>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Revenue Chart */}
        <div className="lg:col-span-2 bg-card border border-border rounded-3xl p-8">
          <div className="flex justify-between items-center mb-8">
            <h2 className="text-xl font-black tracking-tight">REVENUE TREND</h2>
            <div className="flex gap-2">
              <div className="flex items-center gap-2 text-xs font-bold text-muted-foreground border border-border px-3 py-1 rounded-lg">
                <div className="w-2 h-2 rounded-full bg-primary"></div>
                Subscription Sales
              </div>
            </div>
          </div>
          <div className="h-[350px] w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={mockRevenueData}>
                <defs>
                  <linearGradient id="colorValue" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#FF6B00" stopOpacity={0.3}/>
                    <stop offset="95%" stopColor="#FF6B00" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="#1c1c1e" vertical={false} />
                <XAxis
                  dataKey="name"
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: '#71717a', fontSize: 12, fontWeight: 600 }}
                  dy={10}
                />
                <YAxis
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: '#71717a', fontSize: 12, fontWeight: 600 }}
                  tickFormatter={(value) => `₹${value / 1000}k`}
                />
                <Tooltip
                  contentStyle={{ backgroundColor: '#1c1c1e', border: '1px solid #27272a', borderRadius: '12px' }}
                  itemStyle={{ color: '#FF6B00', fontWeight: 'bold' }}
                />
                <Area
                  type="monotone"
                  dataKey="value"
                  stroke="#FF6B00"
                  strokeWidth={4}
                  fillOpacity={1}
                  fill="url(#colorValue)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Activity Feed */}
        <div className="bg-card border border-border rounded-3xl p-8">
          <h2 className="text-xl font-black tracking-tight mb-8">RECENT ACTIVITY</h2>
          <div className="space-y-6">
            {mockActivities.map((activity) => (
              <div key={activity.id} className="flex gap-4 group cursor-default">
                <div className={cn(
                  "w-10 h-10 rounded-xl flex items-center justify-center shrink-0 border border-border transition-colors",
                  activity.type === 'signup' ? "bg-blue-500/10 text-blue-500" :
                  activity.type === 'payment' ? "bg-green-500/10 text-green-500" :
                  "bg-primary/10 text-primary"
                )}>
                  {activity.type === 'signup' ? <Users size={18} /> :
                   activity.type === 'payment' ? <CreditCard size={18} /> :
                   <BadgeCheck size={18} />}
                </div>
                <div className="flex-1">
                  <p className="text-sm font-black leading-none group-hover:text-primary transition-colors">{activity.title}</p>
                  <p className="text-xs text-muted-foreground mt-1 font-medium">{activity.description}</p>
                  <p className="text-[10px] text-muted-foreground/60 font-bold uppercase tracking-wider mt-2 flex items-center gap-1">
                    <Clock size={10} />
                    {activity.timestamp}
                  </p>
                </div>
              </div>
            ))}
          </div>
          <button className="w-full mt-8 py-3 bg-muted hover:bg-muted/80 rounded-xl text-xs font-black tracking-widest transition-all flex items-center justify-center gap-2 border border-border">
            VIEW ALL LOGS <ChevronRight size={14} />
          </button>
        </div>
      </div>

      {/* System Health Section */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <div className="bg-card border border-border rounded-3xl p-8 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 bg-green-500/10 rounded-full flex items-center justify-center">
              <Server className="text-green-500" size={28} />
            </div>
            <div>
              <h3 className="font-black text-lg">Main Server Health</h3>
              <p className="text-sm text-muted-foreground font-medium">Global latency: 42ms</p>
            </div>
          </div>
          <div className="text-right">
            <div className="text-green-500 font-black text-xl leading-none">99.98%</div>
            <p className="text-[10px] text-muted-foreground font-black uppercase tracking-widest mt-1">Uptime</p>
          </div>
        </div>

        <div className="bg-card border border-border rounded-3xl p-8 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 bg-yellow-500/10 rounded-full flex items-center justify-center">
              <AlertCircle className="text-yellow-500" size={28} />
            </div>
            <div>
              <h3 className="font-black text-lg">Pending Review</h3>
              <p className="text-sm text-muted-foreground font-medium">12 support tickets awaiting reply</p>
            </div>
          </div>
          <button className="bg-yellow-500 text-black px-4 py-2 rounded-xl text-xs font-black shadow-lg shadow-yellow-500/20">
            OPEN HELP DESK
          </button>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
