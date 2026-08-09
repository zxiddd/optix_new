"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var SupportService_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.SupportService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
const sync_gateway_1 = require("../sync/sync.gateway");
let SupportService = SupportService_1 = class SupportService {
    constructor(prisma, syncGateway) {
        this.prisma = prisma;
        this.syncGateway = syncGateway;
        this.logger = new common_1.Logger(SupportService_1.name);
    }
    async processAiChat(businessId, userMessage, history = []) {
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
                const data = await response.json();
                reply = data?.candidates?.[0]?.content?.parts?.[0]?.text || '';
            }
            catch (err) {
                this.logger.error(`Gemini API Error: ${err.message}`);
            }
        }
        if (!reply) {
            const lower = userMessage.toLowerCase();
            if (lower.includes('bill') || lower.includes('order')) {
                reply = `To create a bill in **Optix POS**:\n1. Tap **Billing** on the side navigation.\n2. Tap items to add to cart.\n3. Tap **Charge** and select Cash, UPI QR, or Card.\n4. Print thermal receipt or send SMS receipt.`;
            }
            else if (lower.includes('item') || lower.includes('product') || lower.includes('stock')) {
                reply = `You currently have **${productCount} products** in your catalog. Go to **Menu / Items** tab to add new products, scan barcodes, update prices, or adjust stock levels.`;
            }
            else if (lower.includes('support') || lower.includes('contact') || lower.includes('help') || lower.includes('ticket')) {
                reply = `You can submit an official support ticket directly to our admin team! Click **"Create Ticket"** below or type your issue details and our engineering team will respond live.`;
            }
            else {
                reply = `Hello! I'm **Optix AI Copilot**. I can help you manage your catalog (${productCount} items), check today's sales (${todayOrders} orders), set up printers, or connect you directly with Super Admin support. How can I assist you today?`;
            }
        }
        return { reply, timestamp: new Date().toISOString() };
    }
    async createTicket(data) {
        const ticketCount = await this.prisma.supportTicket.count();
        const ticketNumber = `TICK-${String(ticketCount + 1001).padStart(5, '0')}`;
        let targetBusinessId = data.businessId;
        if (targetBusinessId) {
            const exists = await this.prisma.business.findUnique({ where: { id: targetBusinessId } });
            if (!exists)
                targetBusinessId = '';
        }
        if (!targetBusinessId) {
            const fallback = await this.prisma.business.findFirst();
            targetBusinessId = fallback?.id || '00c75603-d16c-46a5-810d-26810f5dc9cb';
        }
        const ticket = await this.prisma.supportTicket.create({
            data: {
                ticketNumber,
                businessId: targetBusinessId,
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
    async getBusinessTickets(businessId) {
        return this.prisma.supportTicket.findMany({
            where: { businessId },
            include: {
                messages: { orderBy: { createdAt: 'asc' } },
            },
            orderBy: { updatedAt: 'desc' },
        });
    }
    async getAllTicketsForAdmin(query) {
        const where = {};
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
    async getTicketDetails(ticketId) {
        return this.prisma.supportTicket.findUnique({
            where: { id: ticketId },
            include: {
                business: { select: { id: true, name: true, country: true } },
                messages: { orderBy: { createdAt: 'asc' } },
            },
        });
    }
    async addMessage(ticketId, senderType, senderName, message) {
        const ticket = await this.prisma.supportTicket.findUnique({ where: { id: ticketId } });
        if (!ticket)
            throw new Error('Support ticket not found');
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
    async updateTicketStatus(ticketId, status, priority) {
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
};
exports.SupportService = SupportService;
exports.SupportService = SupportService = SupportService_1 = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        sync_gateway_1.SyncGateway])
], SupportService);
//# sourceMappingURL=support.service.js.map