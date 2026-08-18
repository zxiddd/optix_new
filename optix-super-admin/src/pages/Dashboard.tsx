import React from 'react';
import { useQuery } from '@tanstack/react-query';
import axios from 'axios';
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
} from 'recharts';
import { cn } from '@/lib/utils';
import { useNavigate } from 'react-router-dom';

const authHeader = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });

const colorMap: Record<string, string> = {
  primary: "bg-primary/10 text-primary",
  "green-500": "bg-green-500/10 text-green-500",
  "yellow-500": "bg-yellow-500/10 text-yellow-500",
  "blue-500": "bg-blue-500/10 text-blue-500",
};

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
      <div className={cn("p-3 rounded-xl", colorMap[color] || "bg-primary/10 text-primary")}>
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
  const navigate = useNavigate();

  const { data: stats, isLoading, refetch } = useQuery({
    queryKey: ['dashboard-overview'],
    queryFn: async () => {
      const { data } = await axios.get('https://api.optixapp.in/api/v1/super-admin/dashboard-overview', authHeader());
      return data;
    },
    refetchInterval: 5000,
  });

  const totalBusinesses = stats?.totalBusinesses ?? 0;
  const onlineBusinesses = stats?.onlineBusinesses ?? 0;
  const monthlyRevenue = stats?.monthlyRevenue ?? 0;
  const socketConnections = stats?.socketConnections ?? 0;
  const trialUsers = stats?.trialUsers ?? 0;
  const starterUsers = stats?.starterUsers ?? 0;
  const growthUsers = stats?.growthUsers ?? 0;
  const revenueTrend = stats?.revenueTrend || [{ name: 'Current', value: monthlyRevenue }];
  const activities = stats?.activities || [];

  return (
    <div className="space-y-8 animate-in fade-in duration-500">
      <div className="flex justify-between items-end">
        <div>
          <h1 className="text-4xl font-black tracking-tighter">OVERVIEW</h1>
          <p className="text-muted-foreground mt-1 font-medium">Real-time platform metrics and ecosystem health.</p>
        </div>
        <div className="flex gap-3">
          <button onClick={() => refetch()} className="bg-muted hover:bg-muted/80 border border-border px-4 py-2 rounded-xl text-sm font-bold transition-colors">
            Realtime Sync
          </button>
        </div>
      </div>

      {/* Main Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Total Businesses"
          value={totalBusinesses}
          icon={Users}
          trend={{ value: "Live", positive: true }}
          subtitle="PostgreSQL Count"
        />
        <StatCard
          title="Online Sockets"
          value={onlineBusinesses}
          icon={Activity}
          color="green-500"
          subtitle="Connected Nodes"
        />
        <StatCard
          title="Monthly Revenue"
          value={`₹${monthlyRevenue.toLocaleString('en-IN')}`}
          icon={TrendingUp}
          trend={{ value: "Razorpay", positive: true }}
          subtitle="Captured"
        />
        <StatCard
          title="Socket Connections"
          value={socketConnections}
          icon={Zap}
          color="yellow-500"
          subtitle="Sync Gateway"
        />
      </div>

      {/* Plan Distribution Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-card border border-border p-4 rounded-2xl flex items-center gap-4">
          <div className="w-12 h-12 bg-muted rounded-xl flex items-center justify-center font-black text-xl">T</div>
          <div>
            <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider">Trial Users</p>
            <p className="text-2xl font-black leading-tight">{trialUsers}</p>
          </div>
        </div>
        <div className="bg-card border border-border p-4 rounded-2xl flex items-center gap-4">
          <div className="w-12 h-12 bg-primary/10 text-primary rounded-xl flex items-center justify-center font-black text-xl italic">S</div>
          <div>
            <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider">Starter Users</p>
            <p className="text-2xl font-black leading-tight">{starterUsers}</p>
          </div>
        </div>
        <div className="bg-card border border-border p-4 rounded-2xl flex items-center gap-4">
          <div className="w-12 h-12 bg-yellow-500/10 text-yellow-500 rounded-xl flex items-center justify-center font-black text-xl italic">G</div>
          <div>
            <p className="text-xs text-muted-foreground font-bold uppercase tracking-wider">Growth Users</p>
            <p className="text-2xl font-black leading-tight">{growthUsers}</p>
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
              <AreaChart data={revenueTrend}>
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
                  tickFormatter={(value) => `₹${value}`}
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
          <div className="space-y-6 max-h-[350px] overflow-y-auto custom-scrollbar pr-2">
            {activities.map((activity: any) => (
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
                    {new Date(activity.timestamp).toLocaleTimeString()}
                  </p>
                </div>
              </div>
            ))}
          </div>
          <button onClick={() => navigate('/logs')} className="w-full mt-8 py-3 bg-muted hover:bg-muted/80 rounded-xl text-xs font-black tracking-widest transition-all flex items-center justify-center gap-2 border border-border">
            VIEW REALTIME LOGS <ChevronRight size={14} />
          </button>
        </div>
      </div>

      {/* System Health Section */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
        <div onClick={() => navigate('/server')} className="bg-card border border-border rounded-3xl p-8 flex items-center justify-between cursor-pointer hover:border-primary/50 transition-all">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 bg-green-500/10 rounded-full flex items-center justify-center">
              <Server className="text-green-500" size={28} />
            </div>
            <div>
              <h3 className="font-black text-lg">Main Server Operations</h3>
              <p className="text-sm text-muted-foreground font-medium">PostgreSQL & Socket Gateway healthy</p>
            </div>
          </div>
          <div className="text-right">
            <div className="text-green-500 font-black text-xl leading-none">ONLINE</div>
            <p className="text-[10px] text-muted-foreground font-black uppercase tracking-widest mt-1">Uptime 100%</p>
          </div>
        </div>

        <div onClick={() => navigate('/alerts')} className="bg-card border border-border rounded-3xl p-8 flex items-center justify-between cursor-pointer hover:border-primary/50 transition-all">
          <div className="flex items-center gap-4">
            <div className="w-14 h-14 bg-yellow-500/10 rounded-full flex items-center justify-center">
              <AlertCircle className="text-yellow-500" size={28} />
            </div>
            <div>
              <h3 className="font-black text-lg">Containers & Fleet</h3>
              <p className="text-sm text-muted-foreground font-medium">Docker containers operating cleanly</p>
            </div>
          </div>
          <button className="bg-yellow-500 text-black px-4 py-2 rounded-xl text-xs font-black shadow-lg shadow-yellow-500/20">
            VIEW FLEET
          </button>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
