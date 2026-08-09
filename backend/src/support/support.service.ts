import { Injectable, Logger } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';

@Injectable()
export class SupportService {
  private readonly logger = new Logger(SupportService.name);

  constructor(
    private readonly prisma: PrismaService,
    private readonly syncGateway: SyncGateway,
  ) {}

  // ─── GEMINI AI ASSISTANT ──────────────────────────────────────────────────

  async processAiChat(businessId: string, userMessage: string, history: any[] = []) {
    const business = await this.prisma.business.findUnique({
      where: { id: businessId },
    });

    const businessName = business?.name || 'Optix Merchant';
    const sub = await this.prisma.subscription.findFirst({
      where: { businessId, status: 'ACTIVE' },
    });
    const plan = sub?.planId || 'GROWTH';

    const productCount = await this.prisma.product.count({ where: { businessId } });
    const todayOrders = await this.prisma.order.count({
      where: {
        businessId,
        createdAt: { gte: new Date(new Date().setHours(0, 0, 0, 0)) },
      },
    });

    const apiKey = process.env.GEMINI_API_KEY || process.env.GOOGLE_AI_KEY;

    const systemPrompt = `You are Optix AI, an intelligent, ultra-fast business copilot and customer support assistant built into Optix Enterprise POS.
Merchant Business: "${businessName}"
Plan Tier: ${plan}
Active Menu Products: ${productCount} items
Orders Processed Today: ${todayOrders} orders

Your role:
1. Answer POS, inventory, billing, tax/GST, printer setup, staff permissions, and reports questions accurately and concisely.
2. Provide friendly, actionable business advice.
3. If the user expresses a critical bug or explicitly requests technical support, politely offer to create a Support Ticket for the Optix Super Admin team.
4. Keep responses clear, professional, concise, and formatted in clean markdown.`;

    let reply = '';

    if (apiKey) {
      try {
        const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`;
        const contents = [
          { role: 'user', parts: [{ text: systemPrompt }] },
          ...history.map(h => ({
            role: h.isUser ? 'user' : 'model',
            parts: [{ text: h.content }],
          })),
          { role: 'user', parts: [{ text: userMessage }] },
        ];

        const response = await fetch(geminiUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ contents }),
        });

        const data: any = await response.json();
        reply = data?.candidates?.[0]?.content?.parts?.[0]?.text || '';
      } catch (err: any) {
        this.logger.error(`Gemini API Error: ${err.message}`);
      }
    }

    if (!reply) {
      const lower = userMessage.toLowerCase();
      if (lower.includes('bill') || lower.includes('order')) {
        reply = `To create a bill in **Optix POS**:\n1. Tap **Billing** on the side navigation.\n2. Tap items to add to cart.\n3. Tap **Charge** and select Cash, UPI QR, or Card.\n4. Print thermal receipt or send SMS receipt.`;
      } else if (lower.includes('item') || lower.includes('product') || lower.includes('stock')) {
        reply = `You currently have **${productCount} products** in your catalog. Go to **Menu / Items** tab to add new products, scan barcodes, update prices, or adjust stock levels.`;
      } else if (lower.includes('support') || lower.includes('contact') || lower.includes('help') || lower.includes('ticket')) {
        reply = `You can submit an official support ticket directly to our admin team! Click **"Create Ticket"** below or type your issue details and our engineering team will respond live.`;
      } else {
        reply = `Hello! I'm **Optix AI Copilot**. I can help you manage your catalog (${productCount} items), check today's sales (${todayOrders} orders), set up printers, or connect you directly with Super Admin support. How can I assist you today?`;
      }
    }

    return { reply, timestamp: new Date().toISOString() };
  }

  // ─── SUPPORT TICKETS & CHAT ───────────────────────────────────────────────

  async createTicket(data: {
    businessId: string;
    subject: string;
    category?: string;
    priority?: string;
    initialMessage: string;
    createdById?: string;
    senderName?: string;
  }) {
    const ticketCount = await this.prisma.supportTicket.count();
    const ticketNumber = `TICK-${String(ticketCount + 1001).padStart(5, '0')}`;

    const ticket = await this.prisma.supportTicket.create({
      data: {
        ticketNumber,
        businessId: data.businessId,
        subject: data.subject,
        category: data.category || 'GENERAL',
        priority: data.priority || 'MEDIUM',
        status: 'OPEN',
        createdById: data.createdById,
        messages: {
          create: {
            senderType: 'USER',
            senderName: data.senderName || 'Merchant Admin',
            message: data.initialMessage,
          },
        },
      },
      include: {
        messages: { orderBy: { createdAt: 'asc' } },
        business: { select: { id: true, name: true, country: true } },
      },
    });

    this.syncGateway.emitToAll('support_ticket_created', ticket);
    return ticket;
  }

  async getBusinessTickets(businessId: string) {
    return this.prisma.supportTicket.findMany({
      where: { businessId },
      include: {
        messages: { orderBy: { createdAt: 'asc' } },
      },
      orderBy: { updatedAt: 'desc' },
    });
  }

  async getAllTicketsForAdmin(query?: { status?: string; search?: string }) {
    const where: any = {};
    if (query?.status && query.status !== 'ALL') {
      where.status = query.status;
    }
    if (query?.search) {
      where.OR = [
        { ticketNumber: { contains: query.search, mode: 'insensitive' } },
        { subject: { contains: query.search, mode: 'insensitive' } },
        { business: { name: { contains: query.search, mode: 'insensitive' } } },
      ];
    }

    return this.prisma.supportTicket.findMany({
      where,
      include: {
        business: { select: { id: true, name: true, country: true } },
        messages: { orderBy: { createdAt: 'asc' } },
      },
      orderBy: { updatedAt: 'desc' },
    });
  }

  async getTicketDetails(ticketId: string) {
    return this.prisma.supportTicket.findUnique({
      where: { id: ticketId },
      include: {
        business: { select: { id: true, name: true, country: true } },
        messages: { orderBy: { createdAt: 'asc' } },
      },
    });
  }

  async addMessage(ticketId: string, senderType: 'USER' | 'ADMIN' | 'AI', senderName: string, message: string) {
    const ticket = await this.prisma.supportTicket.findUnique({ where: { id: ticketId } });
    if (!ticket) throw new Error('Support ticket not found');

    const msg = await this.prisma.supportMessage.create({
      data: {
        ticketId,
        senderType,
        senderName,
        message,
      },
    });

    await this.prisma.supportTicket.update({
      where: { id: ticketId },
      data: { updatedAt: new Date() },
    });

    const payload = {
      ticketId,
      ticketNumber: ticket.ticketNumber,
      businessId: ticket.businessId,
      message: msg,
    };

    this.syncGateway.emitToBusiness(ticket.businessId, 'support_message_received', payload);
    this.syncGateway.emitToAll('support_message_received', payload);

    return msg;
  }

  async updateTicketStatus(ticketId: string, status: string, priority?: string) {
    const ticket = await this.prisma.supportTicket.update({
      where: { id: ticketId },
      data: {
        status,
        ...(priority ? { priority } : {}),
      },
      include: {
        messages: { orderBy: { createdAt: 'asc' } },
        business: { select: { id: true, name: true } },
      },
    });

    this.syncGateway.emitToBusiness(ticket.businessId, 'support_ticket_updated', ticket);
    this.syncGateway.emitToAll('support_ticket_updated', ticket);

    return ticket;
  }
}
