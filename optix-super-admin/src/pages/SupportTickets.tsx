import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { MessageSquare, Search, Send, ShieldAlert, CheckCircle, Clock, AlertCircle, Building2, User, Bot, Tag, Filter } from 'lucide-react';
import { supportService } from '@/services/support.service';

const SupportTickets: React.FC = () => {
  const [selectedTicketId, setSelectedTicketId] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [replyMessage, setReplyMessage] = useState('');

  const queryClient = useQueryClient();

  const { data: tickets = [], isLoading } = useQuery({
    queryKey: ['admin-tickets', statusFilter, search],
    queryFn: () => supportService.getAllTickets({ status: statusFilter, search }),
    refetchInterval: 5000,
  });

  const { data: selectedTicket } = useQuery({
    queryKey: ['ticket-details', selectedTicketId],
    queryFn: () => (selectedTicketId ? supportService.getTicketDetails(selectedTicketId) : null),
    enabled: !!selectedTicketId,
    refetchInterval: 3000,
  });

  const replyMutation = useMutation({
    mutationFn: ({ id, msg }: { id: string; msg: string }) => supportService.addMessage(id, 'Super Admin', msg),
    onSuccess: () => {
      setReplyMessage('');
      queryClient.invalidateQueries({ queryKey: ['ticket-details', selectedTicketId] });
      queryClient.invalidateQueries({ queryKey: ['admin-tickets'] });
    },
  });

  const statusMutation = useMutation({
    mutationFn: ({ id, status, priority }: { id: string; status: string; priority?: string }) =>
      supportService.updateStatus(id, status, priority),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ticket-details', selectedTicketId] });
      queryClient.invalidateQueries({ queryKey: ['admin-tickets'] });
    },
  });

  const handleSendReply = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTicketId || !replyMessage.trim()) return;
    replyMutation.mutate({ id: selectedTicketId, msg: replyMessage.trim() });
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-black tracking-tight flex items-center gap-2">
          <MessageSquare className="text-primary" size={24} /> Support & Live Ticket Desk
        </h1>
        <p className="text-muted-foreground text-sm mt-0.5">
          Manage customer support tickets, review AI Copilot conversations, and reply live to merchant POS terminals (under 200ms real-time).
        </p>

      </div>

      {/* Main Grid: Ticket List + Conversation Panel */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 min-h-[600px]">
        {/* Left Column: Ticket List */}
        <div className="lg:col-span-5 space-y-4 flex flex-col">
          {/* Filters & Search */}
          <div className="bg-card border border-border rounded-2xl p-4 space-y-3">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={14} />
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search ticket #, subject or business..."
                className="w-full bg-muted/40 border border-border rounded-xl py-2 pl-9 pr-4 text-xs focus:outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>

            <div className="flex items-center gap-1.5 overflow-x-auto pb-1">
              {['ALL', 'OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'].map((st) => (
                <button
                  key={st}
                  onClick={() => setStatusFilter(st)}
                  className={`px-3 py-1.5 rounded-lg text-[11px] font-bold transition-all whitespace-nowrap ${
                    statusFilter === st
                      ? 'bg-primary text-black font-black'
                      : 'bg-muted/40 text-muted-foreground hover:bg-muted'
                  }`}
                >
                  {st.replace('_', ' ')}
                </button>
              ))}
            </div>
          </div>

          {/* Ticket Cards */}
          <div className="bg-card border border-border rounded-2xl overflow-hidden flex-1 divide-y divide-border/50 max-h-[550px] overflow-y-auto">
            {isLoading ? (
              <div className="p-8 text-center text-xs text-muted-foreground animate-pulse">Loading support tickets...</div>
            ) : tickets.length === 0 ? (
              <div className="p-12 text-center space-y-2">
                <MessageSquare className="mx-auto text-muted-foreground/40" size={32} />
                <p className="text-xs font-bold text-muted-foreground">No support tickets found</p>
              </div>
            ) : (
              tickets.map((t: any) => {
                const isSelected = selectedTicketId === t.id;
                const lastMsg = t.messages?.[t.messages.length - 1];
                return (
                  <div
                    key={t.id}
                    onClick={() => setSelectedTicketId(t.id)}
                    className={`p-4 cursor-pointer transition-all hover:bg-muted/20 ${
                      isSelected ? 'bg-primary/10 border-l-4 border-l-primary' : ''
                    }`}
                  >
                    <div className="flex items-center justify-between gap-2 mb-1">
                      <span className="text-xs font-black font-mono text-primary">{t.ticketNumber}</span>
                      <span
                        className={`px-2 py-0.5 text-[10px] font-bold rounded-md ${
                          t.status === 'OPEN'
                            ? 'bg-red-500/10 text-red-400 border border-red-500/20'
                            : t.status === 'IN_PROGRESS'
                            ? 'bg-orange-500/10 text-orange-400 border border-orange-500/20'
                            : 'bg-green-500/10 text-green-400 border border-green-500/20'
                        }`}
                      >
                        {t.status}
                      </span>
                    </div>

                    <h4 className="text-xs font-bold text-foreground truncate">{t.subject}</h4>

                    <div className="flex items-center justify-between text-[11px] text-muted-foreground mt-2">
                      <span className="flex items-center gap-1 font-semibold">
                        <Building2 size={12} /> {t.business?.name || 'Optix Tenant'}
                      </span>
                      <span className="text-[10px]">{new Date(t.updatedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                    </div>

                    {lastMsg && (
                      <p className="text-[11px] text-muted-foreground/80 truncate mt-1 italic">
                        "{lastMsg.senderName}": {lastMsg.message}
                      </p>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Right Column: Ticket Conversation Window */}
        <div className="lg:col-span-7 bg-card border border-border rounded-2xl p-6 flex flex-col min-h-[600px]">
          {!selectedTicket ? (
            <div className="m-auto text-center space-y-3 p-8">
              <MessageSquare className="mx-auto text-muted-foreground/30" size={48} />
              <h3 className="text-sm font-bold text-muted-foreground">Select a support ticket to view live conversation</h3>
            </div>
          ) : (
            <div className="flex flex-col h-full space-y-4">
              {/* Header */}
              <div className="border-b border-border pb-4 space-y-3">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-sm font-black font-mono text-primary">{selectedTicket.ticketNumber}</span>
                      <span className="text-xs font-bold px-2 py-0.5 bg-muted rounded-md text-muted-foreground">
                        {selectedTicket.category}
                      </span>
                    </div>
                    <h2 className="text-base font-black text-foreground">{selectedTicket.subject}</h2>
                    <p className="text-xs text-muted-foreground flex items-center gap-1.5 mt-0.5">
                      <Building2 size={13} className="text-primary" /> {selectedTicket.business?.name} ({selectedTicket.business?.country || 'India'})
                    </p>
                  </div>

                  {/* Status & Priority Controls */}
                  <div className="flex items-center gap-2">
                    <select
                      value={selectedTicket.status}
                      onChange={(e) => statusMutation.mutate({ id: selectedTicket.id, status: e.target.value })}
                      className="bg-muted border border-border rounded-xl px-3 py-1.5 text-xs font-bold focus:outline-none"
                    >
                      <option value="OPEN">Status: OPEN</option>
                      <option value="IN_PROGRESS">Status: IN PROGRESS</option>
                      <option value="RESOLVED">Status: RESOLVED</option>
                      <option value="CLOSED">Status: CLOSED</option>
                    </select>

                    <select
                      value={selectedTicket.priority}
                      onChange={(e) => statusMutation.mutate({ id: selectedTicket.id, status: selectedTicket.status, priority: e.target.value })}
                      className="bg-muted border border-border rounded-xl px-3 py-1.5 text-xs font-bold focus:outline-none"
                    >
                      <option value="LOW">Priority: LOW</option>
                      <option value="MEDIUM">Priority: MEDIUM</option>
                      <option value="HIGH">Priority: HIGH</option>
                      <option value="URGENT">Priority: URGENT</option>
                    </select>
                  </div>
                </div>
              </div>

              {/* Message Feed */}
              <div className="flex-1 overflow-y-auto space-y-3 py-2 pr-2 max-h-[380px]">
                {selectedTicket.messages?.map((m: any) => {
                  const isAdmin = m.senderType === 'ADMIN';
                  const isAI = m.senderType === 'AI';

                  return (
                    <div
                      key={m.id}
                      className={`flex flex-col ${isAdmin ? 'items-end' : 'items-start'}`}
                    >
                      <div className="flex items-center gap-1.5 text-[10px] font-bold text-muted-foreground mb-1">
                        {isAI ? <Bot size={12} className="text-purple-400" /> : isAdmin ? <ShieldAlert size={12} className="text-primary" /> : <User size={12} />}
                        <span>{m.senderName} ({m.senderType})</span>
                        <span>• {new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                      </div>

                      <div
                        className={`p-3.5 rounded-2xl max-w-lg text-xs leading-relaxed font-medium ${
                          isAdmin
                            ? 'bg-primary text-black font-semibold rounded-br-none'
                            : isAI
                            ? 'bg-purple-500/10 border border-purple-500/20 text-purple-200 rounded-bl-none'
                            : 'bg-muted border border-border text-foreground rounded-bl-none'
                        }`}
                      >
                        {m.message}
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Reply Box */}
              <form onSubmit={handleSendReply} className="pt-2 border-t border-border flex items-center gap-2">
                <input
                  type="text"
                  value={replyMessage}
                  onChange={(e) => setReplyMessage(e.target.value)}
                  placeholder="Type reply to merchant POS terminal..."
                  className="flex-1 bg-muted/40 border border-border rounded-xl px-4 py-3 text-xs font-medium focus:outline-none focus:ring-2 focus:ring-primary/20"
                />
                <button
                  type="submit"
                  disabled={replyMutation.isPending || !replyMessage.trim()}
                  className="px-5 py-3 bg-primary text-black font-black text-xs rounded-xl flex items-center gap-2 hover:bg-primary/90 disabled:opacity-40 transition-all uppercase tracking-wider"
                >
                  <Send size={14} /> Send Reply
                </button>
              </form>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default SupportTickets;
