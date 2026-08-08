import React, { useState } from 'react';
import { useBusinesses } from '@/hooks/useBusinesses';
import {
  Search,
  RefreshCw,
  MoreVertical,
  ChevronLeft,
  ChevronRight,
  Download
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { cn } from '@/lib/utils';

const Businesses: React.FC = () => {
  const navigate = useNavigate();
  const [params, setParams] = useState({
    page: 1,
    limit: 10,
    search: '',
    planId: '',
    status: '',
  });

  const { data, isLoading, isError, refetch } = useBusinesses(params);

  const handleSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    setParams(prev => ({ ...prev, search: e.target.value, page: 1 }));
  };

  const getStatusColor = (status: string) => {
    switch (status.toUpperCase()) {
      case 'ACTIVE': return 'bg-green-500/10 text-green-500 border-green-500/20';
      case 'TRIAL': return 'bg-blue-500/10 text-blue-500 border-blue-500/20';
      case 'SUSPENDED': return 'bg-red-500/10 text-red-500 border-red-500/20';
      case 'EXPIRED': return 'bg-yellow-500/10 text-yellow-500 border-yellow-500/20';
      default: return 'bg-muted text-muted-foreground border-border';
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-black tracking-tighter">BUSINESSES</h1>
          <p className="text-muted-foreground text-sm font-medium">Manage and monitor all platform accounts.</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={() => refetch()}
            className="p-2 hover:bg-muted border border-border rounded-xl transition-colors"
            disabled={isLoading}
          >
            <RefreshCw size={18} className={cn(isLoading && "animate-spin")} />
          </button>
          <button className="flex items-center gap-2 bg-muted hover:bg-muted/80 border border-border px-4 py-2 rounded-xl text-sm font-bold transition-colors">
            <Download size={16} /> Export CSV
          </button>
        </div>
      </div>

      {/* Filters & Search */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 bg-card border border-border p-4 rounded-2xl">
        <div className="relative md:col-span-2">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={18} />
          <input
            type="text"
            placeholder="Search by name, email, ID..."
            value={params.search}
            onChange={handleSearch}
            className="w-full bg-background border border-border rounded-xl py-2 pl-10 pr-4 focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all text-sm"
          />
        </div>
        <select
          className="bg-background border border-border rounded-xl py-2 px-4 focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all text-sm appearance-none"
          value={params.planId}
          onChange={(e) => setParams(prev => ({ ...prev, planId: e.target.value, page: 1 }))}
        >
          <option value="">All Plans</option>
          <option value="TRIAL">Trial</option>
          <option value="STARTER">Starter</option>
          <option value="GROWTH">Growth</option>
        </select>
        <select
          className="bg-background border border-border rounded-xl py-2 px-4 focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all text-sm appearance-none"
          value={params.status}
          onChange={(e) => setParams(prev => ({ ...prev, status: e.target.value, page: 1 }))}
        >
          <option value="">All Status</option>
          <option value="ACTIVE">Active</option>
          <option value="TRIAL">Trial</option>
          <option value="SUSPENDED">Suspended</option>
          <option value="EXPIRED">Expired</option>
        </select>
      </div>

      {/* Data Table */}
      <div className="bg-card border border-border rounded-2xl overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-border bg-muted/30">
                <th className="p-4 text-xs font-black text-muted-foreground uppercase tracking-widest">Business Name</th>
                <th className="p-4 text-xs font-black text-muted-foreground uppercase tracking-widest">Owner</th>
                <th className="p-4 text-xs font-black text-muted-foreground uppercase tracking-widest">Plan</th>
                <th className="p-4 text-xs font-black text-muted-foreground uppercase tracking-widest text-center">Stats</th>
                <th className="p-4 text-xs font-black text-muted-foreground uppercase tracking-widest">Status</th>
                <th className="p-4 text-xs font-black text-muted-foreground uppercase tracking-widest">Joined</th>
                <th className="p-4 text-xs font-black text-muted-foreground uppercase tracking-widest text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {isLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="animate-pulse">
                    <td colSpan={7} className="p-8 text-center bg-muted/10 font-bold italic">Loading platform data...</td>
                  </tr>
                ))
              ) : isError ? (
                <tr>
                  <td colSpan={7} className="p-8 text-center text-destructive font-bold uppercase tracking-tighter italic">Failed to load data. API Error.</td>
                </tr>
              ) : data?.items?.length === 0 ? (
                <tr>
                  <td colSpan={7} className="p-8 text-center text-muted-foreground font-medium italic">No businesses found matching your criteria.</td>
                </tr>
              ) : (
                data?.items?.map((business: any) => (
                  <tr
                    key={business.id}
                    className="hover:bg-muted/30 transition-colors group cursor-pointer"
                    onClick={() => navigate(`/businesses/${business.id}`)}
                  >
                    <td className="p-4">
                      <div className="font-black text-sm group-hover:text-primary transition-colors uppercase tracking-tight">{business.name}</div>
                      <div className="text-[10px] text-muted-foreground font-mono truncate max-w-[150px]">{business.id}</div>
                    </td>
                    <td className="p-4">
                      <div className="text-sm font-bold">{business.users?.[0]?.email || 'N/A'}</div>
                      <div className="text-[10px] text-muted-foreground font-medium">{business.phone}</div>
                    </td>
                    <td className="p-4">
                      <div className="text-xs font-black tracking-tighter text-primary italic">{business.subscriptions?.[0]?.planId || 'TRIAL'}</div>
                      <div className="text-[10px] text-muted-foreground font-black uppercase">{business.subscriptions?.[0]?.billingCycle}</div>
                    </td>
                    <td className="p-4">
                      <div className="flex justify-center gap-3">
                        <div className="text-center">
                          <div className="text-xs font-black">{business._count?.products}</div>
                          <div className="text-[8px] text-muted-foreground font-black uppercase">Items</div>
                        </div>
                        <div className="text-center">
                          <div className="text-xs font-black">{business._count?.orders}</div>
                          <div className="text-[8px] text-muted-foreground font-black uppercase">Bills</div>
                        </div>
                      </div>
                    </td>
                    <td className="p-4">
                      <span className={cn(
                        "text-[9px] font-black uppercase tracking-widest px-2 py-0.5 rounded-full border",
                        getStatusColor(business.subscriptions?.[0]?.status || 'TRIAL')
                      )}>
                        {business.subscriptions?.[0]?.status || 'TRIAL'}
                      </span>
                    </td>
                    <td className="p-4 text-[10px] font-black text-muted-foreground uppercase tracking-wider">
                      {new Date(business.createdAt).toLocaleDateString(undefined, { day: '2-digit', month: 'short', year: 'numeric' })}
                    </td>
                    <td className="p-4 text-right">
                      <button className="p-2 hover:bg-muted rounded-lg text-muted-foreground transition-colors">
                        <MoreVertical size={16} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination */}
        <div className="p-4 border-t border-border flex items-center justify-between bg-muted/10">
          <div className="text-[10px] text-muted-foreground font-black uppercase tracking-widest italic">
            Showing {((params.page - 1) * params.limit) + 1} to {Math.min(params.page * params.limit, data?.meta?.total || 0)} of {data?.meta?.total || 0}
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-1">
              <button
                className="p-2 hover:bg-muted border border-border rounded-xl disabled:opacity-30 disabled:cursor-not-allowed transition-all"
                disabled={params.page === 1}
                onClick={(e) => { e.stopPropagation(); setParams(p => ({ ...p, page: p.page - 1 })); }}
              >
                <ChevronLeft size={16} />
              </button>
              <div className="text-sm font-black w-8 text-center">{params.page}</div>
              <button
                className="p-2 hover:bg-muted border border-border rounded-xl disabled:opacity-30 disabled:cursor-not-allowed transition-all"
                disabled={params.page >= (data?.meta?.lastPage || 1)}
                onClick={(e) => { e.stopPropagation(); setParams(p => ({ ...p, page: p.page + 1 })); }}
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Businesses;
