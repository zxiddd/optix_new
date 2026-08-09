import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
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
  Smartphone,
  ChevronRight,
  ShieldCheck,
  Ban,
  Package,
  FileText,
  Users,
  Key,
  Calendar,
  CheckCircle,
  AlertTriangle,
  Plus
} from 'lucide-react';
import { cn } from '@/lib/utils';

const API_URL = 'https://api.optixapp.in/api/v1/super-admin';
const auth = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });

const BusinessDetail: React.FC = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [activeTab, setActiveTab] = useState<'overview' | 'staff' | 'products' | 'orders' | 'devices'>('overview');
  const [showExtendModal, setShowExtendModal] = useState(false);
  const [extendDays, setExtendDays] = useState(30);
  const [selectedPlan, setSelectedPlan] = useState('STARTER');
  const [actionSuccessMsg, setActionSuccessMsg] = useState<string | null>(null);

  // Fetch expanded entity graph
  const { data: business, isLoading, isError, refetch } = useQuery({
    queryKey: ['business-detail', id],
    queryFn: async () => {
      const { data } = await axios.get(`${API_URL}/businesses/${id}`, auth());
      return data;
    },
    enabled: !!id,
  });

  // Toggle status mutation
  const toggleStatusMutation = useMutation({
    mutationFn: async (status: string) => {
      const { data } = await axios.patch(`${API_URL}/businesses/${id}/status`, { status }, auth());
      return data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['business-detail', id] });
      triggerSuccess('Business status updated successfully!');
    },
  });

  // Remote command mutation
  const remoteCommandMutation = useMutation({
    mutationFn: async (command: string) => {
      const { data } = await axios.post(`${API_URL}/remote-command`, {
        command,
        businessId: id,
      }, auth());
      return data;
    },
    onSuccess: (_, command) => {
      triggerSuccess(`Remote command [${command}] broadcasted over WebSocket!`);
    },
  });

  // Extend subscription mutation
  const extendSubMutation = useMutation({
    mutationFn: async () => {
      const expiry = new Date(Date.now() + extendDays * 86400000).toISOString();
      const { data } = await axios.post(`${API_URL}/subscriptions`, {
        businessId: id,
        planId: selectedPlan,
        expiryDate: expiry,
        status: 'ACTIVE',
      }, auth());
      return data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['business-detail', id] });
      setShowExtendModal(false);
      triggerSuccess('Subscription extended & synced over WebSocket!');
    },
  });

  // Delete business mutation
  const deleteBusinessMutation = useMutation({
    mutationFn: async () => {
      await axios.delete(`${API_URL}/businesses/${id}`, auth());
    },
    onSuccess: () => {
      navigate('/businesses');
    },
  });

  const triggerSuccess = (msg: string) => {
    setActionSuccessMsg(msg);
    setTimeout(() => setActionSuccessMsg(null), 4000);
  };

  if (isLoading) return <div className="flex items-center justify-center h-96 font-black animate-pulse text-sm uppercase tracking-widest">Loading tenant data graph...</div>;
  if (isError || !business) return <div className="p-8 text-center text-destructive font-black uppercase">CRITICAL ERROR: Failed to retrieve business record.</div>;

  const currentSub = business.subscriptions?.[0] || {};
  const status = currentSub.status?.toUpperCase() || 'TRIAL';
  const owner = business.users?.find((u: any) => u.role === 'OWNER') || business.users?.[0] || {};

  return (
    <div className="space-y-8 animate-in fade-in duration-500 max-w-7xl mx-auto pb-20">
      {/* Toast banner */}
      {actionSuccessMsg && (
        <div className="p-4 bg-green-500/10 border border-green-500/30 text-green-500 rounded-2xl flex items-center justify-between font-bold text-xs">
          <div className="flex items-center gap-2">
            <CheckCircle size={16} />
            <span>{actionSuccessMsg}</span>
          </div>
          <button onClick={() => setActionSuccessMsg(null)} className="text-xs uppercase hover:underline">Dismiss</button>
        </div>
      )}

      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-card p-6 border border-border rounded-3xl shadow-sm">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/businesses')}
            className="p-3 hover:bg-muted border border-border rounded-2xl transition-all active:scale-95"
          >
            <ArrowLeft size={20} />
          </button>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-3xl font-black tracking-tight uppercase">{business.name}</h1>
              <span className={cn(
                "text-[10px] font-black uppercase tracking-widest px-3 py-1 rounded-full border",
                status === 'ACTIVE' ? "bg-green-500/10 text-green-500 border-green-500/20" : "bg-yellow-500/10 text-yellow-500 border-yellow-500/20"
              )}>
                {status}
              </span>
            </div>
            <p className="text-muted-foreground text-xs font-mono mt-1">Tenant ID: {business.id}</p>
          </div>
        </div>

        <div className="flex items-center gap-3 flex-wrap">
          <button
            onClick={() => refetch()}
            className="p-3 bg-muted/40 hover:bg-muted border border-border rounded-2xl transition-all"
            title="Refresh Data"
          >
            <RefreshCw size={18} />
          </button>

          <button
            onClick={() => setShowExtendModal(true)}
            className="flex items-center gap-2 bg-primary text-black px-5 py-3 rounded-2xl text-xs font-black hover:opacity-90 transition-all shadow-lg shadow-primary/20"
          >
            <CreditCard size={16} /> EXTEND SUBSCRIPTION
          </button>

          {status === 'SUSPENDED' ? (
            <button
              onClick={() => toggleStatusMutation.mutate('ACTIVE')}
              className="flex items-center gap-2 bg-green-600 hover:bg-green-700 text-white px-5 py-3 rounded-2xl text-xs font-black transition-all"
            >
              <ShieldCheck size={16} /> REACTIVATE TENANT
            </button>
          ) : (
            <button
              onClick={() => toggleStatusMutation.mutate('SUSPENDED')}
              className="flex items-center gap-2 bg-red-500/10 hover:bg-red-500/20 text-red-500 border border-red-500/20 px-5 py-3 rounded-2xl text-xs font-black transition-all"
            >
              <Ban size={16} /> SUSPEND TENANT
            </button>
          )}

          <button
            onClick={() => {
              if (confirm('Are you sure you want to PERMANENTLY DELETE this business and all related data?')) {
                deleteBusinessMutation.mutate();
              }
            }}
            className="p-3 bg-red-500/10 text-red-500 hover:bg-red-500/20 border border-red-500/20 rounded-2xl transition-all"
            title="Delete Tenant"
          >
            <Trash2 size={18} />
          </button>
        </div>
      </div>

      {/* Tabs Navigation */}
      <div className="flex items-center gap-2 border-b border-border pb-2 overflow-x-auto">
        <TabButton id="overview" label="Overview & Subscription" icon={Store} active={activeTab} onClick={setActiveTab} />
        <TabButton id="staff" label={`Staff Directory (${business._count?.staff || business.staffList?.length || 0})`} icon={Users} active={activeTab} onClick={setActiveTab} />
        <TabButton id="products" label={`Products & Inventory (${business._count?.products || business.productsList?.length || 0})`} icon={Package} active={activeTab} onClick={setActiveTab} />
        <TabButton id="orders" label={`Invoices & Sales (${business._count?.orders || business.recentOrders?.length || 0})`} icon={FileText} active={activeTab} onClick={setActiveTab} />
        <TabButton id="devices" label={`Connected Terminals (${business.devices?.length || 0})`} icon={Smartphone} active={activeTab} onClick={setActiveTab} />
      </div>

      {/* TAB 1: OVERVIEW & SUBSCRIPTION */}
      {activeTab === 'overview' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2 space-y-8">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
              <Card title="Business Info" icon={Store}>
                <div className="grid grid-cols-2 gap-y-4 text-xs">
                  <div>
                    <span className="text-[10px] text-muted-foreground uppercase font-bold">Registered On</span>
                    <p className="font-bold">{new Date(business.createdAt).toLocaleDateString()}</p>
                  </div>
                  <div>
                    <span className="text-[10px] text-muted-foreground uppercase font-bold">Country</span>
                    <p className="font-bold">{business.country || 'India'}</p>
                  </div>
                  <div>
                    <span className="text-[10px] text-muted-foreground uppercase font-bold">Phone</span>
                    <p className="font-bold">{business.phone || 'N/A'}</p>
                  </div>
                  <div>
                    <span className="text-[10px] text-muted-foreground uppercase font-bold">Address</span>
                    <p className="font-bold truncate">{business.address || 'N/A'}</p>
                  </div>
                </div>
              </Card>

              <Card title="Owner Account" icon={User}>
                <div className="grid grid-cols-1 gap-y-4 text-xs">
                  <div>
                    <span className="text-[10px] text-muted-foreground uppercase font-bold">Email</span>
                    <p className="font-bold font-mono text-primary">{owner.email || business.email || 'N/A'}</p>
                  </div>
                  <div>
                    <span className="text-[10px] text-muted-foreground uppercase font-bold">Account ID</span>
                    <p className="font-mono text-[11px] text-muted-foreground">{owner.id || 'N/A'}</p>
                  </div>
                </div>
              </Card>
            </div>

            <Card title="Usage Summary" icon={BarChart2}>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <StatBox value={business._count?.products || 0} label="Total Products" />
                <StatBox value={business._count?.orders || 0} label="Invoices Issued" />
                <StatBox value={business._count?.staff || 0} label="Staff Members" />
                <StatBox value={business.devices?.length || 0} label="Active Terminals" />
              </div>
            </Card>

            <Card title="Recent Technical Audit Logs" icon={Clock}>
              <div className="space-y-3">
                {business.recentLogs?.length > 0 ? (
                  business.recentLogs.map((log: any) => (
                    <div key={log.id} className="flex items-center justify-between p-3 bg-muted/20 border border-border/50 rounded-xl text-xs">
                      <div>
                        <p className="font-bold">{log.action.replace(/_/g, ' ')}</p>
                        <p className="text-[10px] text-muted-foreground font-mono">{log.staff?.name || 'Owner'} ({new Date(log.createdAt).toLocaleString()})</p>
                      </div>
                      <span className="text-[10px] font-mono bg-primary/10 text-primary px-2 py-0.5 rounded">{log.endpoint || 'POS'}</span>
                    </div>
                  ))
                ) : (
                  <p className="text-xs text-muted-foreground italic text-center py-4">No audit logs recorded yet.</p>
                )}
              </div>
            </Card>
          </div>

          <div className="space-y-8">
            <Card title="Subscription Plan" icon={CreditCard}>
              <div className="space-y-4">
                <div className="p-4 bg-primary/10 border border-primary/20 rounded-2xl">
                  <p className="text-[10px] font-black text-primary uppercase">Active Tier</p>
                  <p className="text-3xl font-black italic">{currentSub.planId || 'STARTER'}</p>
                  <p className="text-xs text-muted-foreground font-bold mt-1">Cycle: {currentSub.billingCycle || 'MONTHLY'}</p>
                </div>
                <div className="text-xs space-y-1">
                  <span className="text-[10px] text-muted-foreground uppercase font-bold">Expiration Date</span>
                  <p className="font-bold">{currentSub.expiryDate ? new Date(currentSub.expiryDate).toLocaleDateString(undefined, { dateStyle: 'full' }) : 'Lifetime / Trial'}</p>
                </div>
                <button
                  onClick={() => setShowExtendModal(true)}
                  className="w-full bg-primary text-black py-3 rounded-2xl text-xs font-black uppercase tracking-widest hover:opacity-90 transition-all"
                >
                  Extend Access
                </button>
              </div>
            </Card>

            <Card title="Remote Realtime Commands" icon={ShieldAlert}>
              <div className="space-y-3">
                <CommandButton label="Force Database Sync" icon={RefreshCw} onClick={() => remoteCommandMutation.mutate('FORCE_SYNC')} />
                <CommandButton label="Refresh Subscription" icon={Zap} onClick={() => remoteCommandMutation.mutate('REFRESH_SUBSCRIPTION')} />
                <CommandButton label="Logout All Devices" icon={Ban} onClick={() => remoteCommandMutation.mutate('LOGOUT_ALL_DEVICES')} />
                <CommandButton label="Clear App Cache" icon={Trash2} onClick={() => remoteCommandMutation.mutate('CLEAR_CACHE')} />
              </div>
            </Card>
          </div>
        </div>
      )}

      {/* TAB 2: STAFF DIRECTORY */}
      {activeTab === 'staff' && (
        <Card title="Staff Members & Permissions" icon={Users}>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-border bg-muted/30">
                  <th className="p-4 font-bold uppercase text-[10px] text-muted-foreground">Staff Name</th>
                  <th className="p-4 font-bold uppercase text-[10px] text-muted-foreground">Role</th>
                  <th className="p-4 font-bold uppercase text-[10px] text-muted-foreground">PIN Code</th>
                  <th className="p-4 font-bold uppercase text-[10px] text-muted-foreground">Status</th>
                  <th className="p-4 font-bold uppercase text-[10px] text-muted-foreground">Created</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {business.staffList?.length > 0 ? (
                  business.staffList.map((st: any) => (
                    <tr key={st.id} className="hover:bg-muted/20">
                      <td className="p-4 font-bold">{st.name}</td>
                      <td className="p-4"><span className="px-2 py-0.5 bg-primary/10 text-primary font-mono rounded text-[10px]">{st.role}</span></td>
                      <td className="p-4 font-mono text-muted-foreground">••••</td>
                      <td className="p-4">
                        <span className={cn("px-2 py-0.5 rounded text-[10px] font-bold", st.isActive !== false ? "bg-green-500/10 text-green-500" : "bg-red-500/10 text-red-500")}>
                          {st.isActive !== false ? 'ACTIVE' : 'DISABLED'}
                        </span>
                      </td>
                      <td className="p-4 text-muted-foreground">{new Date(st.createdAt).toLocaleDateString()}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={5} className="p-8 text-center text-muted-foreground italic">No staff members registered for this business.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* TAB 3: PRODUCTS & INVENTORY */}
      {activeTab === 'products' && (
        <Card title="Inventory Products" icon={Package}>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-border bg-muted/30">
                  <th className="p-4 font-bold uppercase text-[10px] text-muted-foreground">Product Name</th>
                  <th className="p-4 font-bold uppercase text-[10px] text-muted-foreground">Category</th>
                  <th className="p-4 font-bold uppercase text-[10px] text-muted-foreground">Price</th>
                  <th className="p-4 font-bold uppercase text-[10px] text-muted-foreground">Stock Level</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {business.productsList?.length > 0 ? (
                  business.productsList.map((prod: any) => (
                    <tr key={prod.id} className="hover:bg-muted/20">
                      <td className="p-4 font-bold">{prod.name}</td>
                      <td className="p-4 font-mono text-muted-foreground">{prod.category?.name || 'Uncategorized'}</td>
                      <td className="p-4 font-bold">₹{prod.price}</td>
                      <td className="p-4">
                        <span className={cn("px-2 py-0.5 rounded text-[10px] font-mono font-bold", prod.stock > 10 ? "bg-green-500/10 text-green-500" : "bg-yellow-500/10 text-yellow-500")}>
                          {prod.stock ?? 0} units
                        </span>
                      </td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={4} className="p-8 text-center text-muted-foreground italic">No products added to catalog yet.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* TAB 4: ORDERS & INVOICES */}
      {activeTab === 'orders' && (
        <Card title="Sales Invoices History" icon={FileText}>
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-border bg-muted/30">
                  <th className="p-4 font-bold uppercase text-[10px] text-muted-foreground">Invoice #</th>
                  <th className="p-4 font-bold uppercase text-[10px] text-muted-foreground">Total Amount</th>
                  <th className="p-4 font-bold uppercase text-[10px] text-muted-foreground">Payment Method</th>
                  <th className="p-4 font-bold uppercase text-[10px] text-muted-foreground">Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {business.recentOrders?.length > 0 ? (
                  business.recentOrders.map((ord: any) => (
                    <tr key={ord.id} className="hover:bg-muted/20">
                      <td className="p-4 font-mono font-bold">{ord.orderNumber || ord.id.slice(0, 8)}</td>
                      <td className="p-4 font-bold text-green-500">₹{ord.totalAmount}</td>
                      <td className="p-4 font-mono text-[10px] uppercase">{ord.paymentMode || 'CASH'}</td>
                      <td className="p-4 text-muted-foreground">{new Date(ord.createdAt).toLocaleString()}</td>
                    </tr>
                  ))
                ) : (
                  <tr>
                    <td colSpan={4} className="p-8 text-center text-muted-foreground italic">No invoices issued by this tenant.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </Card>
      )}

      {/* TAB 5: CONNECTED TERMINALS */}
      {activeTab === 'devices' && (
        <Card title="Active POS Devices Fleet" icon={Smartphone}>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {business.devices?.length > 0 ? (
              business.devices.map((dev: any) => (
                <div key={dev.id} className="p-5 bg-muted/20 border border-border rounded-2xl space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <Smartphone className="text-primary" size={20} />
                      <div>
                        <p className="font-bold text-sm">{dev.deviceName || 'Android POS Terminal'}</p>
                        <p className="text-[10px] font-mono text-muted-foreground">ID: {dev.id}</p>
                      </div>
                    </div>
                    <span className="px-2 py-0.5 bg-green-500/10 text-green-500 text-[10px] font-bold rounded">ONLINE</span>
                  </div>
                  <div className="grid grid-cols-2 gap-2 text-[11px] pt-2 border-t border-border/50">
                    <div><span className="text-muted-foreground">App Version:</span> <span className="font-mono font-bold">{dev.appVersion || 'v2.4.0'}</span></div>
                    <div><span className="text-muted-foreground">Last Sync:</span> <span className="font-mono">{new Date(dev.lastSeen).toLocaleTimeString()}</span></div>
                  </div>
                </div>
              ))
            ) : (
              <div className="col-span-2 p-8 text-center text-muted-foreground italic">No terminals currently connected to this tenant.</div>
            )}
          </div>
        </Card>
      )}

      {/* Extend Modal */}
      {showExtendModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-card border border-border rounded-3xl p-6 max-w-md w-full space-y-6 shadow-2xl animate-in zoom-in-95">
            <h3 className="text-xl font-black">Extend Subscription Access</h3>

            <div className="space-y-4 text-xs">
              <div>
                <label className="text-[10px] font-bold uppercase text-muted-foreground">Select Tier Plan</label>
                <select
                  value={selectedPlan}
                  onChange={(e) => setSelectedPlan(e.target.value)}
                  className="w-full mt-1 p-3 bg-muted border border-border rounded-xl font-bold"
                >
                  <option value="STARTER">STARTER</option>
                  <option value="GROWTH">GROWTH</option>
                  <option value="ENTERPRISE">ENTERPRISE</option>
                </select>
              </div>

              <div>
                <label className="text-[10px] font-bold uppercase text-muted-foreground">Extend Duration (Days)</label>
                <input
                  type="number"
                  value={extendDays}
                  onChange={(e) => setExtendDays(Number(e.target.value))}
                  className="w-full mt-1 p-3 bg-muted border border-border rounded-xl font-bold"
                  min={1}
                />
              </div>
            </div>

            <div className="flex gap-3 pt-2">
              <button
                onClick={() => setShowExtendModal(false)}
                className="flex-1 py-3 bg-muted border border-border rounded-2xl text-xs font-bold"
              >
                Cancel
              </button>
              <button
                onClick={() => extendSubMutation.mutate()}
                disabled={extendSubMutation.isPending}
                className="flex-1 py-3 bg-primary text-black rounded-2xl text-xs font-black uppercase"
              >
                {extendSubMutation.isPending ? 'Saving...' : 'Confirm Extension'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

const Card: React.FC<{ title: string; icon: any; children: React.ReactNode }> = ({ title, icon: Icon, children }) => (
  <div className="bg-card border border-border rounded-3xl p-6 space-y-4 shadow-sm">
    <div className="flex items-center gap-3 border-b border-border/50 pb-4">
      <div className="p-2 bg-primary/10 text-primary rounded-xl">
        <Icon size={18} />
      </div>
      <h2 className="text-sm font-black uppercase tracking-wider">{title}</h2>
    </div>
    {children}
  </div>
);

const TabButton: React.FC<{ id: string; label: string; icon: any; active: string; onClick: (id: any) => void }> = ({ id, label, icon: Icon, active, onClick }) => (
  <button
    onClick={() => onClick(id)}
    className={cn(
      "flex items-center gap-2 px-4 py-2.5 rounded-2xl text-xs font-black uppercase tracking-tight transition-all shrink-0 border",
      active === id ? "bg-primary text-black border-primary shadow-md" : "bg-card hover:bg-muted text-muted-foreground border-border"
    )}
  >
    <Icon size={14} />
    <span>{label}</span>
  </button>
);

const StatBox: React.FC<{ value: number; label: string }> = ({ value, label }) => (
  <div className="bg-muted/30 p-4 rounded-2xl border border-border/50 text-center">
    <p className="text-2xl font-black">{value}</p>
    <p className="text-[10px] text-muted-foreground font-bold uppercase mt-1">{label}</p>
  </div>
);

const CommandButton: React.FC<{ label: string; icon: any; onClick: () => void }> = ({ label, icon: Icon, onClick }) => (
  <button
    onClick={onClick}
    className="w-full flex items-center justify-between p-3.5 bg-muted/20 hover:bg-muted/50 border border-border rounded-2xl text-xs font-bold transition-all active:scale-[0.98]"
  >
    <div className="flex items-center gap-3">
      <Icon size={16} className="text-primary" />
      <span>{label}</span>
    </div>
    <ChevronRight size={14} className="text-muted-foreground" />
  </button>
);

export default BusinessDetail;
