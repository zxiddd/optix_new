import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useBusinessDetail, useUpdateBusinessStatus } from '@/hooks/useBusinesses';
import {
  ArrowLeft,
  Store,
  User,
  CreditCard,
  BarChart2,
  Clock,
  ShieldAlert,
  Trash2,
  RefreshCw,
  Zap,
  Globe,
  Smartphone,
  ChevronRight,
  ShieldCheck,
  Ban
} from 'lucide-react';
import { cn } from '@/lib/utils';

const DetailCard: React.FC<{
  title: string;
  icon: any;
  children: React.ReactNode;
  action?: React.ReactNode;
}> = ({ title, icon: Icon, children, action }) => (
  <div className="bg-card border border-border rounded-3xl overflow-hidden shadow-sm">
    <div className="p-6 border-b border-border flex justify-between items-center bg-muted/20">
      <div className="flex items-center gap-3">
        <div className="p-2 bg-primary/10 text-primary rounded-lg">
          <Icon size={18} />
        </div>
        <h2 className="text-sm font-black uppercase tracking-widest">{title}</h2>
      </div>
      {action}
    </div>
    <div className="p-6">
      {children}
    </div>
  </div>
);

const InfoItem: React.FC<{ label: string; value: string | React.ReactNode; mono?: boolean }> = ({ label, value, mono }) => (
  <div className="space-y-1">
    <p className="text-[10px] text-muted-foreground font-black uppercase tracking-wider">{label}</p>
    <div className={cn(
      "text-sm font-bold tracking-tight truncate",
      mono && "font-mono text-xs text-primary bg-primary/5 px-2 py-0.5 rounded w-fit"
    )}>
      {value}
    </div>
  </div>
);

const BusinessDetail: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data: business, isLoading, isError } = useBusinessDetail(id!);
  const updateStatus = useUpdateBusinessStatus();

  if (isLoading) return <div className="flex items-center justify-center h-full font-black animate-pulse uppercase tracking-tighter italic">Loading business blueprint...</div>;
  if (isError) return <div className="p-8 text-center text-destructive font-black uppercase">CRITICAL ERROR: Failed to retrieve business record.</div>;

  const currentSub = business.subscriptions?.[0] || {};
  const status = currentSub.status?.toUpperCase() || 'TRIAL';

  return (
    <div className="space-y-8 animate-in fade-in duration-500 max-w-7xl mx-auto pb-20">
      {/* Top Navigation */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/businesses')}
            className="p-3 hover:bg-muted border border-border rounded-2xl transition-all active:scale-95"
          >
            <ArrowLeft size={20} />
          </button>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-4xl font-black tracking-tighter uppercase">{business.name}</h1>
              <span className={cn(
                "text-[10px] font-black uppercase tracking-widest px-2 py-0.5 rounded-full border",
                status === 'ACTIVE' ? "bg-green-500/10 text-green-500 border-green-500/20" : "bg-blue-500/10 text-blue-500 border-blue-500/20"
              )}>
                {status}
              </span>
            </div>
            <p className="text-muted-foreground text-xs font-mono mt-1 uppercase">Entity ID: {business.id}</p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {status === 'SUSPENDED' ? (
            <button
              onClick={() => updateStatus.mutate({ id: business.id, status: 'ACTIVE' })}
              className="flex items-center gap-2 bg-green-600 hover:bg-green-700 text-white px-6 py-3 rounded-2xl text-xs font-black transition-all shadow-lg shadow-green-600/20"
            >
              <ShieldCheck size={16} /> REACTIVATE ACCOUNT
            </button>
          ) : (
            <button
              onClick={() => updateStatus.mutate({ id: business.id, status: 'SUSPENDED' })}
              className="flex items-center gap-2 bg-red-500/10 hover:bg-red-500/20 text-red-500 border border-red-500/20 px-6 py-3 rounded-2xl text-xs font-black transition-all"
            >
              <Ban size={16} /> SUSPEND BUSINESS
            </button>
          )}
          <button className="flex items-center justify-center p-3 bg-card border border-border rounded-2xl text-destructive hover:bg-destructive/10 transition-colors">
            <Trash2 size={20} />
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: General & Owner */}
        <div className="lg:col-span-2 space-y-8">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
            <DetailCard title="General Information" icon={Store}>
              <div className="grid grid-cols-2 gap-y-6">
                <InfoItem label="Registration" value={new Date(business.createdAt).toLocaleDateString(undefined, { dateStyle: 'long' })} />
                <InfoItem label="Country" value={business.country} />
                <InfoItem label="Currency" value={business.settings?.currency || 'INR'} />
                <InfoItem label="Timezone" value={business.settings?.timezone || 'Asia/Riyadh'} />
                <InfoItem label="Address" value={business.address || 'Not Provided'} />
                <InfoItem label="Business Phone" value={business.phone || 'N/A'} />
              </div>
            </DetailCard>

            <DetailCard title="Owner Details" icon={User}>
              <div className="grid grid-cols-1 gap-y-6">
                <InfoItem label="Primary Email" value={business.users?.[0]?.email || 'N/A'} />
                <InfoItem label="User ID" value={business.users?.[0]?.id || 'N/A'} mono />
                <InfoItem label="Member Since" value={new Date(business.users?.[0]?.createdAt).toLocaleDateString()} />
              </div>
            </DetailCard>
          </div>

          <DetailCard title="Usage & Scalability" icon={BarChart2}>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="bg-muted/30 p-4 rounded-2xl border border-border/50 text-center">
                <p className="text-2xl font-black">{business._count?.products}</p>
                <p className="text-[10px] text-muted-foreground font-bold uppercase">Products</p>
              </div>
              <div className="bg-muted/30 p-4 rounded-2xl border border-border/50 text-center">
                <p className="text-2xl font-black">{business._count?.orders}</p>
                <p className="text-[10px] text-muted-foreground font-bold uppercase">Total Bills</p>
              </div>
              <div className="bg-muted/30 p-4 rounded-2xl border border-border/50 text-center">
                <p className="text-2xl font-black">{business._count?.staff}</p>
                <p className="text-[10px] text-muted-foreground font-bold uppercase">Staff Count</p>
              </div>
              <div className="bg-muted/30 p-4 rounded-2xl border border-border/50 text-center">
                <p className="text-2xl font-black">{business._count?.devices}</p>
                <p className="text-[10px] text-muted-foreground font-bold uppercase">Terminals</p>
              </div>
            </div>
          </DetailCard>

          {/* Activity Logs */}
          <DetailCard title="Recent Technical Logs" icon={Clock} action={
            <button className="text-[10px] font-black text-primary hover:underline uppercase tracking-widest">Full Audit</button>
          }>
            <div className="space-y-4">
              {business.recentLogs?.map((log: any) => (
                <div key={log.id} className="flex items-start gap-4 p-3 hover:bg-muted/30 rounded-xl transition-colors border border-transparent hover:border-border/50">
                  <div className={cn(
                    "w-2 h-2 mt-1.5 rounded-full shrink-0",
                    log.isSuspicious ? "bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.5)]" : "bg-green-500"
                  )}></div>
                  <div className="flex-1">
                    <p className="text-xs font-bold leading-none">{log.action.replace(/_/g, ' ')}</p>
                    <p className="text-[10px] text-muted-foreground mt-1 uppercase font-medium">{log.staff?.name || 'Owner'} via {log.deviceId || 'Terminal'}</p>
                  </div>
                  <div className="text-[10px] text-muted-foreground/60 font-mono italic">
                    {new Date(log.createdAt).toLocaleTimeString()}
                  </div>
                </div>
              ))}
            </div>
          </DetailCard>
        </div>

        {/* Right Column: Subscription & Quick Actions */}
        <div className="space-y-8">
          <DetailCard title="Subscription Plan" icon={CreditCard}>
            <div className="space-y-6">
              <div className="p-4 bg-primary/5 border border-primary/20 rounded-2xl">
                <p className="text-xs font-black text-primary uppercase tracking-widest mb-1">Active Plan</p>
                <p className="text-3xl font-black italic tracking-tighter">{currentSub.planId || 'TRIAL'}</p>
                <p className="text-[10px] text-muted-foreground font-bold uppercase mt-1">{currentSub.billingCycle} Cycle</p>
              </div>

              <div className="space-y-4 px-2">
                <UsageProgress label="Bills Usage" used={currentSub.billsUsed || 0} limit={currentSub.planId === 'TRIAL' ? 50 : 100000} />
                <UsageProgress label="Inventory Usage" used={currentSub.productsUsed || 0} limit={currentSub.planId === 'TRIAL' ? 5 : 100000} />
              </div>

              <div className="pt-4 border-t border-border">
                <InfoItem label="Subscription Expiry" value={new Date(currentSub.expiryDate).toLocaleDateString(undefined, { dateStyle: 'full' })} />
                <button className="w-full mt-4 bg-primary text-black py-3 rounded-2xl text-xs font-black hover:opacity-90 transition-all uppercase tracking-widest shadow-lg shadow-primary/20">
                  EXTEND ACCESS
                </button>
              </div>
            </div>
          </DetailCard>

          <DetailCard title="Account Health" icon={ShieldAlert}>
             <div className="space-y-3">
               <ActionButton label="Force Database Sync" icon={RefreshCw} color="primary" />
               <ActionButton label="Reset Trial Usage" icon={Zap} color="yellow-500" />
               <ActionButton label="Kill All Active Sessions" icon={Ban} color="red-500" />
               <ActionButton label="Generate Activation Code" icon={ChevronRight} color="muted-foreground" />
             </div>
          </DetailCard>

          {/* Active Devices */}
          <DetailCard title="Active Terminals" icon={Smartphone}>
             <div className="space-y-4">
               {business.devices?.map((device: any) => (
                 <div key={device.id} className="flex items-center gap-3">
                   <div className="p-2 bg-muted rounded-xl border border-border">
                     <Smartphone size={16} />
                   </div>
                   <div>
                     <p className="text-xs font-black leading-none">{device.deviceName}</p>
                     <p className="text-[10px] text-muted-foreground uppercase font-bold mt-1">Last Seen: {new Date(device.lastSeen).toLocaleTimeString()}</p>
                   </div>
                 </div>
               ))}
             </div>
          </DetailCard>
        </div>
      </div>
    </div>
  );
};

const UsageProgress: React.FC<{ label: string; used: number; limit: number }> = ({ label, used, limit }) => {
  const percentage = Math.min((used / limit) * 100, 100);
  return (
    <div className="space-y-2">
      <div className="flex justify-between text-[10px] font-black uppercase tracking-widest">
        <span>{label}</span>
        <span className="text-muted-foreground">{used} / {limit === 100000 ? '∞' : limit}</span>
      </div>
      <div className="h-2 bg-muted rounded-full overflow-hidden border border-border/50">
        <div
          className={cn("h-full transition-all duration-1000", percentage > 90 ? "bg-red-500" : "bg-primary")}
          style={{ width: `${percentage}%` }}
        ></div>
      </div>
    </div>
  );
};

const ActionButton: React.FC<{ label: string; icon: any; color: string }> = ({ label, icon: Icon, color }) => (
  <button className="w-full flex items-center justify-between p-4 bg-muted/30 hover:bg-muted/50 border border-border rounded-2xl transition-all group active:scale-[0.98]">
    <div className="flex items-center gap-3">
      <Icon size={16} className={cn(`text-${color}`)} />
      <span className="text-xs font-black uppercase tracking-tight">{label}</span>
    </div>
    <ChevronRight size={14} className="text-muted-foreground group-hover:text-primary transition-colors" />
  </button>
);

export default BusinessDetail;
