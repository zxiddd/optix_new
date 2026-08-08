import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axios from 'axios';
import { Database, Table, Edit3, Trash2, Plus, RefreshCw, Search, Check, X, ShieldAlert, ChevronLeft, ChevronRight } from 'lucide-react';
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

  return (
    <div className="space-y-6">
      {/* Top Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black tracking-tight flex items-center gap-2">
            <Database className="text-primary" size={24} /> Interactive PostgreSQL Database Explorer
          </h1>
          <p className="text-muted-foreground text-sm mt-0.5">
            Direct PgAdmin/Prisma Studio interface inside Optix Super Admin. Edit, create, or delete records in PostgreSQL cleanly.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowAddModal(true)}
            className="flex items-center gap-1.5 px-4 py-2 bg-primary text-black font-black text-xs rounded-xl hover:bg-primary/90 transition-colors shadow-lg shadow-primary/20"
          >
            <Plus size={15} /> Insert Row into {selectedTable}
          </button>
          <button onClick={() => refetch()} className="p-2.5 rounded-xl border border-border hover:bg-muted transition-colors">
            <RefreshCw size={16} />
          </button>
        </div>
      </div>

      {/* Main Studio Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
        {/* Table Selector Sidebar */}
        <div className="bg-card border border-border rounded-2xl p-4 space-y-2 lg:col-span-1">
          <h2 className="text-xs font-bold text-muted-foreground uppercase px-2 mb-3">PostgreSQL Tables</h2>
          {(tables || []).map((t: any) => (
            <button
              key={t.name}
              onClick={() => { setSelectedTable(t.name); setPage(1); }}
              className={`w-full text-left px-3 py-2.5 rounded-xl text-xs font-bold transition-all flex items-center justify-between ${
                selectedTable === t.name
                  ? 'bg-primary text-black shadow-md shadow-primary/20'
                  : 'text-muted-foreground hover:bg-muted hover:text-foreground'
              }`}
            >
              <span className="flex items-center gap-2">
                <Table size={14} /> {t.name}
              </span>
            </button>
          ))}
        </div>

        {/* Data Table View */}
        <div className="bg-card border border-border rounded-2xl p-6 lg:col-span-4 space-y-4">
          {/* Controls Bar */}
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-2 flex-1 max-w-sm">
              <Search size={15} className="text-muted-foreground" />
              <input
                value={search}
                onChange={e => { setSearch(e.target.value); setPage(1); }}
                placeholder={`Search ${selectedTable} rows...`}
                className="w-full bg-muted/50 border border-border rounded-xl px-3 py-1.5 text-xs focus:outline-none"
              />
            </div>
            <span className="text-xs font-mono text-muted-foreground">
              Total: {tableData?.meta?.total ?? 0} records
            </span>
          </div>

          {/* Grid Table */}
          <div className="overflow-x-auto border border-border rounded-xl max-h-[550px] custom-scrollbar">
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
                    <td colSpan={columns.length + 1} className="px-3 py-8 text-center text-muted-foreground">
                      No records found in table '{selectedTable}'
                    </td>
                  </tr>
                ) : rows.map((row: any) => (
                  <tr key={row.id || JSON.stringify(row)} className="border-b border-border/50 hover:bg-muted/20 transition-colors">
                    <td className="px-3 py-2 flex items-center gap-1.5">
                      <button
                        onClick={() => startEditing(row)}
                        className="p-1 rounded bg-blue-500/10 text-blue-400 border border-blue-500/20 hover:bg-blue-500/20"
                        title="Edit Row"
                      >
                        <Edit3 size={13} />
                      </button>
                      <button
                        onClick={() => setDeletingRowId(row.id)}
                        className="p-1 rounded bg-red-500/10 text-red-400 border border-red-500/20 hover:bg-red-500/20"
                        title="Delete Row"
                      >
                        <Trash2 size={13} />
                      </button>
                    </td>
                    {columns.map(col => {
                      const val = row[col];
                      const valStr = typeof val === 'object' ? JSON.stringify(val) : String(val ?? '');
                      return (
                        <td key={col} className="px-3 py-2 max-w-[200px] truncate" title={valStr}>
                          {valStr}
                        </td>
                      );
                    })}
                  </tr>
                ))}
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
          <div className="bg-card border border-border rounded-2xl p-6 max-w-xl w-full max-h-[85vh] overflow-y-auto space-y-4 shadow-2xl">
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
                      value={editFormData[key] ?? ''}
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
                onClick={() => updateMutation.mutate({ id: editingRow.id, data: editFormData })}
                disabled={updateMutation.isPending}
                className="px-4 py-2 bg-primary text-black font-black text-xs rounded-xl hover:bg-primary/90 disabled:opacity-50"
              >
                Save Changes to PostgreSQL
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Insert Row Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-card border border-border rounded-2xl p-6 max-w-xl w-full max-h-[85vh] overflow-y-auto space-y-4 shadow-2xl">
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

      {/* Delete Confirmation */}
      <ConfirmDialog
        open={!!deletingRowId}
        title={`Delete Row from ${selectedTable}?`}
        description="This will permanently delete the record from PostgreSQL database."
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
