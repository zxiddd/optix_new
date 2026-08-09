import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { Database, Table, Edit3, Trash2, Plus, RefreshCw, Search, Check, X, ShieldAlert, ChevronLeft, ChevronRight, Download, Code, FileText } from 'lucide-react';
import ConfirmDialog from '@/components/shared/ConfirmDialog';

const authHeader = () => ({ headers: { Authorization: `Bearer ${localStorage.getItem('token')}` } });
const API_URL = 'https://api.optixapp.in/api/v1/super-admin/db-explorer';

const DatabaseExplorer: React.FC = () => {
  const qc = useQueryClient();
  const [selectedTable, setSelectedTable] = useState('Business');
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [editingRow, setEditingRow] = useState<any | null>(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [deletingRowId, setDeletingRowId] = useState<string | null>(null);
  const [editFormData, setEditFormData] = useState<Record<string, any>>({});
  const [addFormData, setAddFormData] = useState<Record<string, any>>({});
  const [jsonViewerData, setJsonViewerData] = useState<any | null>(null);

  // 1. Fetch available PostgreSQL tables
  const { data: tables } = useQuery({
    queryKey: ['db-tables'],
    queryFn: async () => {
      const { data } = await axios.get(`${API_URL}/tables`, authHeader());
      return data;
    },
  });

  // 2. Fetch rows for selected table
  const { data: tableData, isLoading, refetch } = useQuery({
    queryKey: ['db-rows', selectedTable, page, search],
    queryFn: async () => {
      const { data } = await axios.get(`${API_URL}/tables/${selectedTable}/rows`, {
        params: { page, limit: 15, search },
        ...authHeader(),
      });
      return data;
    },
    refetchInterval: 10000,
  });

  // 3. Update row mutation
  const updateMutation = useMutation({
    mutationFn: async ({ id, data }: { id: string; data: any }) => {
      const res = await axios.patch(`${API_URL}/tables/${selectedTable}/rows/${id}`, data, authHeader());
      return res.data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['db-rows', selectedTable] });
      setEditingRow(null);
    },
  });

  // 4. Delete row mutation
  const deleteMutation = useMutation({
    mutationFn: async (id: string) => {
      const res = await axios.delete(`${API_URL}/tables/${selectedTable}/rows/${id}`, authHeader());
      return res.data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['db-rows', selectedTable] });
      setDeletingRowId(null);
    },
  });

  // 5. Create row mutation
  const createMutation = useMutation({
    mutationFn: async (data: any) => {
      const res = await axios.post(`${API_URL}/tables/${selectedTable}/rows`, data, authHeader());
      return res.data;
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['db-rows', selectedTable] });
      setShowAddModal(false);
      setAddFormData({});
    },
  });

  const rows = tableData?.items || [];
  const columns = rows.length > 0 ? Object.keys(rows[0]) : ['id', 'name', 'createdAt'];

  const startEditing = (row: any) => {
    setEditingRow(row);
    setEditFormData({ ...row });
  };

  const exportTableData = () => {
    if (!rows.length) return;
    const jsonStr = JSON.stringify(rows, null, 2);
    const blob = new Blob([jsonStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a'); a.href = url; a.download = `${selectedTable}_rows.json`; a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black tracking-tight flex items-center gap-2">
            <Database className="text-primary" size={24} /> PostgreSQL Database Explorer
          </h1>
          <p className="text-muted-foreground text-sm mt-0.5">
            Direct PgAdmin interface inside Super Admin. Inspect, edit, insert, or delete records live in PostgreSQL.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => refetch()} className="p-2.5 rounded-xl border border-border hover:bg-muted transition-colors">
            <RefreshCw size={16} />
          </button>
          <button onClick={exportTableData} className="flex items-center gap-2 px-4 py-2.5 rounded-xl border border-border hover:bg-muted text-sm font-semibold transition-colors">
            <Download size={15} /> Export JSON
          </button>
          <button
            onClick={() => setShowAddModal(true)}
            className="flex items-center gap-2 bg-primary text-black font-black text-sm px-4 py-2.5 rounded-xl hover:opacity-90 transition-all shadow-md shadow-primary/20"
          >
            <Plus size={16} /> Insert Record
          </button>
        </div>
      </div>

      {/* Sidebar + Table Viewer */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        {/* Sidebar: Table Selection */}
        <div className="bg-card border border-border rounded-2xl p-4 space-y-2">
          <h2 className="text-xs font-black uppercase text-muted-foreground tracking-wider mb-3 px-2">Public Schema Tables</h2>
          <div className="space-y-1">
            {(tables || [
              { name: 'Business', description: 'Business Tenants' },
              { name: 'User', description: 'User Credentials' },
              { name: 'Staff', description: 'Staff Directory' },
              { name: 'Order', description: 'POS Invoices' },
              { name: 'Product', description: 'Catalog Products' },
              { name: 'Category', description: 'Product Categories' },
              { name: 'Customer', description: 'Customer Directory' },
              { name: 'Expense', description: 'Business Expenses' },
              { name: 'Subscription', description: 'SaaS Subscriptions' },
              { name: 'PaymentTransaction', description: 'Payment Logs' },
              { name: 'AuditLog', description: 'System Audit Logs' },
              { name: 'Device', description: 'Connected Terminals' },
              { name: 'SyncQueue', description: 'Sync Queue' },
            ]).map((tbl: any) => (
              <button
                key={tbl.name}
                onClick={() => { setSelectedTable(tbl.name); setPage(1); setSearch(''); }}
                className={`w-full flex items-center justify-between p-3 rounded-xl text-left transition-all text-xs font-bold ${
                  selectedTable === tbl.name
                    ? 'bg-primary text-black font-black shadow-sm'
                    : 'hover:bg-muted text-muted-foreground'
                }`}
              >
                <div className="flex items-center gap-2">
                  <Table size={14} />
                  <span>{tbl.name}</span>
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* Main Grid View */}
        <div className="md:col-span-3 space-y-4">
          <div className="flex items-center justify-between bg-card border border-border rounded-2xl p-4">
            <div className="relative flex-1 max-w-md">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={15} />
              <input
                value={search}
                onChange={e => { setSearch(e.target.value); setPage(1); }}
                placeholder={`Search in ${selectedTable}...`}
                className="w-full bg-muted/50 border border-border rounded-xl py-2 pl-9 pr-4 text-xs font-mono focus:outline-none"
              />
            </div>
            <span className="text-xs font-mono text-muted-foreground">
              Total Records: <strong className="text-foreground">{tableData?.meta?.total ?? rows.length}</strong>
            </span>
          </div>

          {/* Grid Table */}
          <div className="overflow-x-auto border border-border rounded-2xl bg-card max-h-[580px]">
            <table className="w-full text-xs font-mono">
              <thead className="bg-muted/60 sticky top-0 border-b border-border z-10">
                <tr>
                  <th className="px-3 py-3 text-left font-bold text-muted-foreground uppercase">Actions</th>
                  {columns.map(col => (
                    <th key={col} className="px-3 py-3 text-left font-bold text-muted-foreground uppercase">{col}</th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {isLoading ? (
                  Array.from({ length: 5 }).map((_, i) => (
                    <tr key={i} className="border-b border-border/50 animate-pulse">
                      <td colSpan={columns.length + 1} className="px-3 py-3"><div className="h-4 bg-muted rounded" /></td>
                    </tr>
                  ))
                ) : rows.length === 0 ? (
                  <tr>
                    <td colSpan={columns.length + 1} className="px-3 py-8 text-center text-muted-foreground italic">
                      No records found in table '{selectedTable}'
                    </td>
                  </tr>
                ) : rows.map((row: any, idx: number) => {
                  const targetId = row.id || row.code || String(idx);
                  return (
                    <tr key={targetId} className="border-b border-border/50 hover:bg-muted/20 transition-colors">
                      <td className="px-3 py-2 flex items-center gap-1.5">
                        <button
                          onClick={() => startEditing(row)}
                          className="p-1.5 rounded bg-blue-500/10 text-blue-400 border border-blue-500/20 hover:bg-blue-500/20"
                          title="Edit Row"
                        >
                          <Edit3 size={13} />
                        </button>
                        <button
                          onClick={() => setDeletingRowId(targetId)}
                          className="p-1.5 rounded bg-red-500/10 text-red-400 border border-red-500/20 hover:bg-red-500/20"
                          title="Delete Row"
                        >
                          <Trash2 size={13} />
                        </button>
                      </td>
                      {columns.map(col => {
                        const val = row[col];
                        const valStr = typeof val === 'object' ? JSON.stringify(val) : String(val ?? '');
                        return (
                          <td
                            key={col}
                            onClick={() => typeof val === 'object' && setJsonViewerData(val)}
                            className={`px-3 py-2 max-w-[200px] truncate ${typeof val === 'object' ? 'cursor-pointer text-primary underline' : ''}`}
                            title={valStr}
                          >
                            {valStr}
                          </td>
                        );
                      })}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          <div className="flex items-center justify-between text-xs text-muted-foreground pt-2">
            <span>Page {page} of {tableData?.meta?.lastPage || 1}</span>
            <div className="flex gap-2">
              <button
                disabled={page <= 1}
                onClick={() => setPage(p => p - 1)}
                className="px-3 py-1.5 bg-card border border-border rounded-lg disabled:opacity-40 hover:bg-muted font-bold"
              >
                <ChevronLeft size={14} />
              </button>
              <button
                disabled={page >= (tableData?.meta?.lastPage || 1)}
                onClick={() => setPage(p => p + 1)}
                className="px-3 py-1.5 bg-card border border-border rounded-lg disabled:opacity-40 hover:bg-muted font-bold"
              >
                <ChevronRight size={14} />
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Edit Row Modal */}
      {editingRow && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-card border border-border rounded-3xl p-6 max-w-xl w-full max-h-[85vh] overflow-y-auto space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <h3 className="font-black text-base flex items-center gap-2">
                <Edit3 className="text-primary" size={18} /> Edit Row in {selectedTable}
              </h3>
              <button onClick={() => setEditingRow(null)} className="p-1 rounded-lg hover:bg-muted">
                <X size={16} />
              </button>
            </div>

            <div className="space-y-3 font-mono text-xs">
              {Object.keys(editingRow).map(key => {
                if (key === 'id' || key === 'createdAt' || key === 'updatedAt') {
                  return (
                    <div key={key}>
                      <label className="text-[10px] font-bold text-muted-foreground uppercase">{key} (Read-only)</label>
                      <input disabled value={String(editingRow[key] ?? '')} className="w-full bg-muted/30 border border-border/50 rounded-xl px-3 py-2 text-muted-foreground" />
                    </div>
                  );
                }
                return (
                  <div key={key}>
                    <label className="text-[10px] font-bold text-primary uppercase">{key}</label>
                    <input
                      value={typeof editFormData[key] === 'object' ? JSON.stringify(editFormData[key]) : (editFormData[key] ?? '')}
                      onChange={e => setEditFormData({ ...editFormData, [key]: e.target.value })}
                      className="w-full bg-muted/60 border border-border rounded-xl px-3 py-2 text-foreground focus:outline-none focus:border-primary"
                    />
                  </div>
                );
              })}
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-border">
              <button onClick={() => setEditingRow(null)} className="px-4 py-2 border border-border rounded-xl text-xs font-bold">
                Cancel
              </button>
              <button
                onClick={() => updateMutation.mutate({ id: editingRow.id || editingRow.code, data: editFormData })}
                disabled={updateMutation.isPending}
                className="px-4 py-2 bg-primary text-black font-black text-xs rounded-xl hover:bg-primary/90 disabled:opacity-50"
              >
                Save Changes
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Insert Row Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-card border border-border rounded-3xl p-6 max-w-xl w-full max-h-[85vh] overflow-y-auto space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <h3 className="font-black text-base flex items-center gap-2">
                <Plus className="text-primary" size={18} /> Insert Record into {selectedTable}
              </h3>
              <button onClick={() => setShowAddModal(false)} className="p-1 rounded-lg hover:bg-muted">
                <X size={16} />
              </button>
            </div>

            <div className="space-y-3 font-mono text-xs">
              {columns.filter(c => c !== 'id' && c !== 'createdAt' && c !== 'updatedAt').map(col => (
                <div key={col}>
                  <label className="text-[10px] font-bold text-primary uppercase">{col}</label>
                  <input
                    value={addFormData[col] ?? ''}
                    onChange={e => setAddFormData({ ...addFormData, [col]: e.target.value })}
                    placeholder={`Enter ${col}...`}
                    className="w-full bg-muted/60 border border-border rounded-xl px-3 py-2 text-foreground focus:outline-none focus:border-primary"
                  />
                </div>
              ))}
            </div>

            <div className="flex justify-end gap-2 pt-3 border-t border-border">
              <button onClick={() => setShowAddModal(false)} className="px-4 py-2 border border-border rounded-xl text-xs font-bold">
                Cancel
              </button>
              <button
                onClick={() => createMutation.mutate(addFormData)}
                disabled={createMutation.isPending}
                className="px-4 py-2 bg-primary text-black font-black text-xs rounded-xl hover:bg-primary/90 disabled:opacity-50"
              >
                Insert Record
              </button>
            </div>
          </div>
        </div>
      )}

      {/* JSON Viewer Modal */}
      {jsonViewerData && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-card border border-border rounded-3xl p-6 max-w-xl w-full space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <h3 className="font-black text-sm flex items-center gap-2 font-mono">
                <Code className="text-primary" size={16} /> JSON Field Viewer
              </h3>
              <button onClick={() => setJsonViewerData(null)} className="p-1 rounded-lg hover:bg-muted">
                <X size={16} />
              </button>
            </div>
            <pre className="p-4 bg-muted/50 border border-border rounded-2xl text-xs font-mono overflow-x-auto text-primary">
              {JSON.stringify(jsonViewerData, null, 2)}
            </pre>
            <div className="flex justify-end">
              <button onClick={() => setJsonViewerData(null)} className="px-4 py-2 bg-muted text-xs font-bold rounded-xl">
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation */}
      <ConfirmDialog
        open={!!deletingRowId}
        title={`Delete Record from ${selectedTable}?`}
        description="This will permanently delete the selected row from PostgreSQL database."
        confirmLabel="Permanently Delete"
        confirmVariant="danger"
        loading={deleteMutation.isPending}
        onConfirm={() => deletingRowId && deleteMutation.mutate(deletingRowId)}
        onCancel={() => setDeletingRowId(null)}
      />
    </div>
  );
};

export default DatabaseExplorer;
