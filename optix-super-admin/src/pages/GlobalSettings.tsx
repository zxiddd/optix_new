import React, { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Settings, ShieldAlert, Save, RefreshCw, Mail, Phone, MessageSquare, Globe, AlertTriangle } from 'lucide-react';
import { globalConfigService } from '@/services/globalConfig.service';
import ConfirmDialog from '@/components/shared/ConfirmDialog';
import { motion } from 'framer-motion';

const GlobalSettings: React.FC = () => {
  const qc = useQueryClient();
  const [form, setForm] = useState({
    maintenanceMode: false,
    maintenanceMessage: 'System under maintenance. Please try again shortly.',
    minSupportedAppVersion: '1.0.0',
    latestStableVersion: '1.2.0',
    forceUpdate: false,
    apiEndpoint: 'https://api.optixapp.in',
    webSocketEndpoint: 'https://api.optixapp.in/events',
    supportEmail: 'support@optixapp.in',
    supportPhone: '+919876543210',
    supportWhatsApp: '+919876543210',
  });

  const [showConfirm, setShowConfirm] = useState(false);

  const { data, isLoading, refetch } = useQuery({
    queryKey: ['global-config'],
    queryFn: globalConfigService.getConfig,
  });

  useEffect(() => {
    if (data) {
      setForm({
        maintenanceMode: !!data.maintenanceMode,
        maintenanceMessage: data.maintenanceMessage || '',
        minSupportedAppVersion: data.minSupportedAppVersion || '1.0.0',
        latestStableVersion: data.latestStableVersion || '1.0.0',
        forceUpdate: !!data.forceUpdate,
        apiEndpoint: data.apiEndpoint || 'https://api.optixapp.in',
        webSocketEndpoint: data.webSocketEndpoint || 'https://api.optixapp.in/events',
        supportEmail: data.supportEmail || 'support@optixapp.in',
        supportPhone: data.supportPhone || '+919876543210',
        supportWhatsApp: data.supportWhatsApp || '+919876543210',
      });
    }
  }, [data]);

  const updateMutation = useMutation({
    mutationFn: globalConfigService.updateConfig,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['global-config'] });
      setShowConfirm(false);
    },
  });

  return (
    <div className="space-y-6 max-w-4xl">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-black tracking-tight flex items-center gap-2">
            <Settings className="text-primary" size={24} /> Global System Settings
          </h1>
          <p className="text-muted-foreground text-sm mt-0.5">
            Configure system-wide maintenance mode, minimum Android app versions, endpoints, and support contacts.
          </p>
        </div>
        <button onClick={() => refetch()} className="p-2.5 rounded-xl border border-border hover:bg-muted transition-colors">
          <RefreshCw size={16} />
        </button>
      </div>

      {/* Maintenance Mode Card */}
      <div className={`border rounded-2xl p-6 space-y-4 transition-colors ${
        form.maintenanceMode ? 'bg-red-500/10 border-red-500/30' : 'bg-card border-border'
      }`}>
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className={`p-2.5 rounded-xl ${form.maintenanceMode ? 'bg-red-500/20 text-red-400' : 'bg-muted text-muted-foreground'}`}>
              <ShieldAlert size={20} />
            </div>
            <div>
              <h2 className="font-bold text-base">Global Maintenance Mode</h2>
              <p className="text-xs text-muted-foreground">
                When enabled, non-admin access will be suspended across all client apps in real-time.
              </p>
            </div>
          </div>
          <button
            onClick={() => setForm(f => ({ ...f, maintenanceMode: !f.maintenanceMode }))}
            className={`px-4 py-2 text-xs font-bold rounded-xl transition-colors ${
              form.maintenanceMode ? 'bg-red-500 text-white' : 'bg-muted text-muted-foreground hover:bg-muted/80'
            }`}
          >
            {form.maintenanceMode ? 'MAINTENANCE ACTIVE' : 'SYSTEM NORMAL'}
          </button>
        </div>

        <div>
          <label className="text-xs font-semibold text-muted-foreground block mb-1">Maintenance Message</label>
          <input
            type="text"
            value={form.maintenanceMessage}
            onChange={e => setForm(f => ({ ...f, maintenanceMessage: e.target.value }))}
            className="w-full bg-muted/50 border border-border rounded-xl px-4 py-2.5 text-sm focus:outline-none"
          />
        </div>
      </div>

      {/* Version Control & Force Update */}
      <div className="bg-card border border-border rounded-2xl p-6 space-y-4">
        <h2 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Android App Versioning</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="text-xs font-semibold text-muted-foreground block mb-1">Minimum Supported Version</label>
            <input
              type="text"
              value={form.minSupportedAppVersion}
              onChange={e => setForm(f => ({ ...f, minSupportedAppVersion: e.target.value }))}
              placeholder="1.0.0"
              className="w-full bg-muted/50 border border-border rounded-xl px-4 py-2.5 text-sm font-mono focus:outline-none"
            />
          </div>
          <div>
            <label className="text-xs font-semibold text-muted-foreground block mb-1">Latest Stable Version</label>
            <input
              type="text"
              value={form.latestStableVersion}
              onChange={e => setForm(f => ({ ...f, latestStableVersion: e.target.value }))}
              placeholder="1.2.0"
              className="w-full bg-muted/50 border border-border rounded-xl px-4 py-2.5 text-sm font-mono focus:outline-none"
            />
          </div>
        </div>

        <div className="flex items-center justify-between pt-2">
          <div>
            <p className="text-sm font-bold">Force Update Requirement</p>
            <p className="text-xs text-muted-foreground">Block app usage for clients running versions below minimum supported version.</p>
          </div>
          <button
            onClick={() => setForm(f => ({ ...f, forceUpdate: !f.forceUpdate }))}
            className={`px-4 py-2 text-xs font-bold rounded-xl transition-colors ${
              form.forceUpdate ? 'bg-orange-500 text-black' : 'bg-muted text-muted-foreground'
            }`}
          >
            {form.forceUpdate ? 'FORCE UPDATE ON' : 'OPTIONAL UPDATE'}
          </button>
        </div>
      </div>

      {/* Endpoints & Support Contacts */}
      <div className="bg-card border border-border rounded-2xl p-6 space-y-4">
        <h2 className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Endpoints & Support Information</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="text-xs font-semibold text-muted-foreground block mb-1 flex items-center gap-1.5"><Globe size={13} /> API Endpoint</label>
            <input type="text" value={form.apiEndpoint} onChange={e => setForm(f => ({ ...f, apiEndpoint: e.target.value }))}
              className="w-full bg-muted/50 border border-border rounded-xl px-4 py-2.5 text-sm font-mono focus:outline-none" />
          </div>
          <div>
            <label className="text-xs font-semibold text-muted-foreground block mb-1 flex items-center gap-1.5"><Globe size={13} /> WebSocket Endpoint</label>
            <input type="text" value={form.webSocketEndpoint} onChange={e => setForm(f => ({ ...f, webSocketEndpoint: e.target.value }))}
              className="w-full bg-muted/50 border border-border rounded-xl px-4 py-2.5 text-sm font-mono focus:outline-none" />
          </div>
          <div>
            <label className="text-xs font-semibold text-muted-foreground block mb-1 flex items-center gap-1.5"><Mail size={13} /> Support Email</label>
            <input type="text" value={form.supportEmail} onChange={e => setForm(f => ({ ...f, supportEmail: e.target.value }))}
              className="w-full bg-muted/50 border border-border rounded-xl px-4 py-2.5 text-sm focus:outline-none" />
          </div>
          <div>
            <label className="text-xs font-semibold text-muted-foreground block mb-1 flex items-center gap-1.5"><Phone size={13} /> Support Phone</label>
            <input type="text" value={form.supportPhone} onChange={e => setForm(f => ({ ...f, supportPhone: e.target.value }))}
              className="w-full bg-muted/50 border border-border rounded-xl px-4 py-2.5 text-sm focus:outline-none" />
          </div>
        </div>
      </div>

      <div className="flex justify-end">
        <button
          onClick={() => setShowConfirm(true)}
          className="flex items-center gap-2 px-6 py-3 bg-primary text-black font-black text-sm rounded-xl hover:bg-primary/90 transition-colors shadow-lg shadow-primary/10"
        >
          <Save size={16} /> Save System Settings
        </button>
      </div>

      <ConfirmDialog
        open={showConfirm}
        title="Save Global Settings?"
        description="Updating system settings will broadcast real-time updates to all connected Android clients and servers."
        confirmLabel="Save Settings"
        confirmVariant="primary"
        loading={updateMutation.isPending}
        onConfirm={() => updateMutation.mutate(form)}
        onCancel={() => setShowConfirm(false)}
      />
    </div>
  );
};

export default GlobalSettings;
