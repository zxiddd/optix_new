import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Bell, Send, CheckCircle2, AlertTriangle, AlertCircle, Info, Sparkles, Building2, Layers, Globe, ShieldAlert } from 'lucide-react';
import { notificationService } from '@/services/notification.service';
import { businessService } from '@/services/business.service';

const TEMPLATES = [
  {
    label: '⏳ Subscription Expiring Soon',
    title: 'Subscription Expiration Notice',
    message: 'Your Optix POS subscription will expire in 3 days. Please renew your plan to prevent disruption to billing operations.',
    type: 'SUBSCRIPTION_EXPIRING',
    severity: 'WARNING',
  },
  {
    label: '🛠️ Scheduled Maintenance Tonight',
    title: 'Platform Maintenance Alert',
    message: 'Optix cloud system maintenance is scheduled tonight from 2:00 AM to 3:00 AM IST. Offline POS billing will continue uninterrupted.',
    type: 'MAINTENANCE_WARNING',
    severity: 'WARNING',
  },
  {
    label: '🚀 New Feature Release',
    title: 'New Features Available',
    message: 'Exciting update! AI Menu Import and Advanced Sales Reports are now live on your terminal. Syncing updates automatically.',
    type: 'SYSTEM_UPDATE',
    severity: 'INFO',
  },
  {
    label: '💳 Payment Action Required',
    title: 'Subscription Payment Update Required',
    message: 'We were unable to process your automatic subscription renewal. Please update your payment method in the Subscription tab.',
    type: 'SUBSCRIPTION_EXPIRING',
    severity: 'CRITICAL',
  },
];

const Notifications: React.FC = () => {
  const [targetType, setTargetType] = useState<'ALL' | 'BUSINESS' | 'PLAN'>('ALL');
  const [selectedBusinessId, setSelectedBusinessId] = useState('');
  const [selectedPlanId, setSelectedPlanId] = useState('STARTER');
  const [title, setTitle] = useState('');
  const [message, setMessage] = useState('');
  const [type, setType] = useState('CUSTOM');
  const [severity, setSeverity] = useState('INFO');
  const [statusMessage, setStatusMessage] = useState<string | null>(null);

  const { data: businessData } = useQuery({
    queryKey: ['businesses-list'],
    queryFn: () => businessService.getBusinesses({ limit: 100 }),
  });

  const businesses: any[] = businessData?.items ?? [];

  const sendMutation = useMutation({
    mutationFn: notificationService.sendNotification,
    onSuccess: (res) => {
      setStatusMessage(res.message || 'Notification broadcasted live over Socket.IO!');
      setTitle('');
      setMessage('');
      setTimeout(() => setStatusMessage(null), 5000);
    },
    onError: (err: any) => {
      alert(err?.response?.data?.message || 'Failed to send notification.');
    },
  });

  const handleApplyTemplate = (tpl: typeof TEMPLATES[0]) => {
    setTitle(tpl.title);
    setMessage(tpl.message);
    setType(tpl.type);
    setSeverity(tpl.severity);
  };

  const handleSend = (e: React.FormEvent) => {
    e.preventDefault();
    if (!title.trim() || !message.trim()) {
      alert('Please fill in title and message.');
      return;
    }
    sendMutation.mutate({
      targetType,
      businessId: targetType === 'BUSINESS' ? selectedBusinessId : undefined,
      planId: targetType === 'PLAN' ? selectedPlanId : undefined,
      title: title.trim(),
      message: message.trim(),
      type,
      severity,
    });
  };

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-black tracking-tight flex items-center gap-2">
          <Bell className="text-primary" size={24} /> Broadcast & Notifications Control Center
        </h1>
        <p className="text-muted-foreground text-sm mt-0.5">
          Send targeted or system-wide push notifications directly to Android POS terminals in real time via Socket.IO (&lt;200ms).
        </p>
      </div>

      {statusMessage && (
        <div className="p-4 bg-green-500/10 border border-green-500/30 text-green-400 text-xs font-bold rounded-xl flex items-center gap-2 animate-in fade-in">
          <CheckCircle2 size={16} /> {statusMessage}
        </div>
      )}

      {/* Quick Templates */}
      <div className="bg-card border border-border rounded-2xl p-5 space-y-3">
        <span className="text-xs font-bold text-muted-foreground uppercase flex items-center gap-1.5">
          <Sparkles size={14} className="text-primary" /> Quick Notification Templates
        </span>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
          {TEMPLATES.map((tpl) => (
            <button
              key={tpl.label}
              type="button"
              onClick={() => handleApplyTemplate(tpl)}
              className="p-3 text-left bg-muted/40 hover:bg-muted border border-border rounded-xl transition-all hover:border-primary/40 text-xs font-bold flex items-center justify-between"
            >
              <span>{tpl.label}</span>
              <span className="text-[10px] text-muted-foreground font-normal">Apply</span>
            </button>
          ))}
        </div>
      </div>

      {/* Form Card */}
      <form onSubmit={handleSend} className="bg-card border border-border rounded-2xl p-6 space-y-5">
        <h2 className="text-sm font-black uppercase tracking-wider text-foreground border-b border-border pb-3">
          Compose Push Broadcast
        </h2>

        {/* Target Selector */}
        <div className="space-y-2">
          <label className="text-xs font-bold text-muted-foreground block uppercase">Recipient Target</label>
          <div className="grid grid-cols-3 gap-3">
            {[
              { id: 'ALL', label: 'All Businesses', icon: Globe },
              { id: 'BUSINESS', label: 'Single Business', icon: Building2 },
              { id: 'PLAN', label: 'By Subscription Plan', icon: Layers },
            ].map((t) => (
              <button
                key={t.id}
                type="button"
                onClick={() => setTargetType(t.id as any)}
                className={`p-3 rounded-xl border text-xs font-bold flex items-center justify-center gap-2 transition-all ${
                  targetType === t.id
                    ? 'bg-primary text-black border-primary font-black shadow-lg shadow-primary/20'
                    : 'bg-muted/40 border-border text-muted-foreground hover:bg-muted'
                }`}
              >
                <t.icon size={16} /> {t.label}
              </button>
            ))}
          </div>
        </div>

        {/* Conditional Target Dropdowns */}
        {targetType === 'BUSINESS' && (
          <div>
            <label className="text-xs font-bold text-muted-foreground block mb-1.5 uppercase">Select Target Business</label>
            <select
              value={selectedBusinessId}
              onChange={(e) => setSelectedBusinessId(e.target.value)}
              required
              className="w-full bg-muted/40 border border-border rounded-xl px-4 py-2.5 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              <option value="">Select a business tenant...</option>
              {businesses.map((b: any) => (
                <option key={b.id} value={b.id}>{b.name} ({b.country || 'India'})</option>
              ))}
            </select>
          </div>
        )}

        {targetType === 'PLAN' && (
          <div>
            <label className="text-xs font-bold text-muted-foreground block mb-1.5 uppercase">Select Target Plan</label>
            <select
              value={selectedPlanId}
              onChange={(e) => setSelectedPlanId(e.target.value)}
              className="w-full bg-muted/40 border border-border rounded-xl px-4 py-2.5 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              <option value="TRIAL">TRIAL Plan Users</option>
              <option value="STARTER">STARTER Plan Users</option>
              <option value="GROWTH">GROWTH Plan Users</option>
            </select>
          </div>
        )}

        {/* Category & Severity */}
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="text-xs font-bold text-muted-foreground block mb-1.5 uppercase">Notification Category</label>
            <select
              value={type}
              onChange={(e) => setType(e.target.value)}
              className="w-full bg-muted/40 border border-border rounded-xl px-4 py-2.5 text-sm font-medium focus:outline-none"
            >
              <option value="CUSTOM">Custom Message</option>
              <option value="SUBSCRIPTION_EXPIRING">Subscription Expiring</option>
              <option value="SYSTEM_UPDATE">System Release / Feature Update</option>
              <option value="MAINTENANCE_WARNING">Maintenance Warning</option>
            </select>
          </div>

          <div>
            <label className="text-xs font-bold text-muted-foreground block mb-1.5 uppercase">Severity Level</label>
            <select
              value={severity}
              onChange={(e) => setSeverity(e.target.value)}
              className="w-full bg-muted/40 border border-border rounded-xl px-4 py-2.5 text-sm font-medium focus:outline-none"
            >
              <option value="INFO">INFO (Blue Badge)</option>
              <option value="WARNING">WARNING (Orange Alert)</option>
              <option value="CRITICAL">CRITICAL (Red Priority Alert)</option>
            </select>
          </div>
        </div>

        {/* Title */}
        <div>
          <label className="text-xs font-bold text-muted-foreground block mb-1.5 uppercase">Notification Title</label>
          <input
            type="text"
            required
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Subscription Expiring Soon"
            className="w-full bg-muted/40 border border-border rounded-xl px-4 py-2.5 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-primary/20"
          />
        </div>

        {/* Message */}
        <div>
          <label className="text-xs font-bold text-muted-foreground block mb-1.5 uppercase">Message Body</label>
          <textarea
            required
            rows={4}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            placeholder="Type notification message to be displayed on POS devices..."
            className="w-full bg-muted/40 border border-border rounded-xl p-4 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-primary/20"
          />
        </div>

        {/* Action Button */}
        <button
          type="submit"
          disabled={sendMutation.isPending}
          className="w-full py-3.5 bg-primary hover:bg-primary/90 text-black font-black text-xs rounded-xl flex items-center justify-center gap-2 transition-all shadow-lg shadow-primary/20 uppercase tracking-wider disabled:opacity-50"
        >
          <Send size={16} /> {sendMutation.isPending ? 'Broadcasting via Socket.IO...' : 'Broadcast Notification Live'}
        </button>
      </form>
    </div>
  );
};

export default Notifications;
