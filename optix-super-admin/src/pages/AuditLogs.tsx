import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Search, ChevronLeft, ChevronRight, RefreshCw, Filter } from 'lucide-react';
import { auditService } from '@/services/audit.service';
import { motion } from 'framer-motion';

const ENTITIES = ['', 'SUBSCRIPTION', 'PAYMENT', 'PRODUCT', 'ORDER', 'STAFF', 'BUSINESS'];

const ACTION_COLORS: Record<string, string> = {
  UPDATE: 'text-blue-400 bg-blue-500/10',
  CHANGE: 'text-purple-400 bg-purple-500/10',
  RESET: 'text-orange-400 bg-orange-500/10',
  DELETE: 'text-red-400 bg-red-500/10',
  CREATE: 'text-green-400 bg-green-500/10',
  REFUND: 'text-yellow-400 bg-yellow-500/10',
  EXTEND: 'text-cyan-400 bg-cyan-500/10',
};

const getActionColor = (action: string) => {
  const key = Object.keys(ACTION_COLORS).find(k => action.includes(k));
  return key ? ACTION_COLORS[key] : 'text-muted-foreground bg-muted/50';
};

const AuditLogs: React.FC = () => {
  const [filters, setFilters] = useState<Record<string, string>>({});
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [expanded, setExpanded] = useState<string | null>(null);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['audit-logs', filters, page, search],
    queryFn: () => auditService.getAll({ ...filters, page, limit: 50, action: search }),
  });

  const logs: any[] = data?.items ?? [];
  const meta = data?.meta;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black tracking-tight">Audit Logs</h1>
          <p className="text-muted-foreground text-sm mt-0.5">Complete history of every admin action</p>
        </div>
        <button onClick={() => refetch()} className="p-2.5 rounded-xl border border-border hover:bg-muted transition-colors">
          <RefreshCw size={16} />
        </button>
      </div>

      {/* Filters */}
      <div className="bg-card border border-border rounded-2xl p-4 flex flex-wrap gap-3">
        <div className="relative flex-1 min-w-[200px]">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={15} />
          <input value={search} onChange={e => { setSearch(e.target.value); setPage(1); }}
            placeholder="Search by action name..."
            className="w-full bg-muted/50 border border-border rounded-xl py-2.5 pl-9 pr-4 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20" />
        </div>
        <select value={filters.entity ?? ''} onChange={e => { setFilters(f => ({ ...f, entity: e.target.value })); setPage(1); }}
          className="bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20">
          {ENTITIES.map(e => <option key={e} value={e}>{e || 'All Entities'}</option>)}
        </select>
        <input placeholder="Filter by Business ID..."
          value={filters.businessId ?? ''}
          onChange={e => { setFilters(f => ({ ...f, businessId: e.target.value })); setPage(1); }}
          className="bg-muted/50 border border-border rounded-xl px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary/20 w-56" />
      </div>

      {/* Log Entries */}
      <div className="space-y-2">
        {isLoading ? (
          Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="bg-card border border-border rounded-xl p-4 animate-pulse">
              <div className="flex gap-4">
                <div className="h-4 bg-muted rounded w-24" />
                <div className="h-4 bg-muted rounded w-32" />
                <div className="h-4 bg-muted rounded flex-1" />
              </div>
            </div>
          ))
        ) : logs.length === 0 ? (
          <div className="bg-card border border-border rounded-2xl py-16 text-center text-muted-foreground">
            <Filter size={32} className="mx-auto mb-3 opacity-30" />
            <p className="font-semibold">No audit logs found</p>
            <p className="text-xs mt-1">Actions will appear here as they happen</p>
          </div>
        ) : logs.map((log: any, i: number) => (
          <motion.div key={log.id} initial={{ opacity: 0, y: 4 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: i * 0.01 }}>
            <button
              onClick={() => setExpanded(expanded === log.id ? null : log.id)}
              className="w-full bg-card border border-border rounded-xl p-4 text-left hover:border-primary/30 transition-colors"
            >
              <div className="flex items-center gap-4 flex-wrap">
                <span className="text-xs text-muted-foreground whitespace-nowrap font-mono">
                  {new Date(log.createdAt).toLocaleString()}
                </span>
                <span className={`px-2.5 py-0.5 rounded-lg text-xs font-bold ${getActionColor(log.action)}`}>
                  {log.action}
                </span>
                <span className="text-xs font-bold text-muted-foreground bg-muted/50 px-2 py-0.5 rounded-lg">
                  {log.entity}
                </span>
                <span className="text-sm font-semibold text-foreground flex-1">
                  {log.business?.name ?? log.businessId}
                </span>
                {log.entityId && (
                  <span className="text-xs font-mono text-muted-foreground">{log.entityId.slice(0, 8)}…</span>
                )}
              </div>

              {/* Expanded Detail */}
              {expanded === log.id && (
                <div className="mt-4 grid grid-cols-2 gap-3 border-t border-border pt-4">
                  <div>
                    <p className="text-xs font-bold text-muted-foreground mb-2 uppercase">Before</p>
                    <pre className="text-xs bg-muted/50 rounded-xl p-3 overflow-auto max-h-40 text-muted-foreground leading-relaxed">
                      {JSON.stringify(log.oldValue, null, 2) || '—'}
                    </pre>
                  </div>
                  <div>
                    <p className="text-xs font-bold text-muted-foreground mb-2 uppercase">After</p>
                    <pre className="text-xs bg-muted/50 rounded-xl p-3 overflow-auto max-h-40 text-green-400/80 leading-relaxed">
                      {JSON.stringify(log.newValue, null, 2) || '—'}
                    </pre>
                  </div>
                  {log.endpoint && (
                    <div className="col-span-2">
                      <span className="text-xs text-muted-foreground">Endpoint: </span>
                      <span className="text-xs font-mono text-primary">{log.endpoint}</span>
                    </div>
                  )}
                </div>
              )}
            </button>
          </motion.div>
        ))}
      </div>

      {/* Pagination */}
      {meta && meta.lastPage > 1 && (
        <div className="flex items-center justify-between px-2">
          <p className="text-xs text-muted-foreground">Total: {meta.total} log entries</p>
          <div className="flex items-center gap-1">
            <button onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page === 1}
              className="p-1.5 rounded-lg hover:bg-muted disabled:opacity-40 transition-colors"><ChevronLeft size={15} /></button>
            <span className="px-3 py-1 text-xs font-bold">{page} / {meta.lastPage}</span>
            <button onClick={() => setPage(p => Math.min(meta.lastPage, p + 1))} disabled={page === meta.lastPage}
              className="p-1.5 rounded-lg hover:bg-muted disabled:opacity-40 transition-colors"><ChevronRight size={15} /></button>
          </div>
        </div>
      )}
    </div>
  );
};

export default AuditLogs;
