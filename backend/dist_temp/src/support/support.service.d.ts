import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';
export declare class SupportService {
    private readonly prisma;
    private readonly syncGateway;
    private readonly logger;
    constructor(prisma: PrismaService, syncGateway: SyncGateway);
    processAiChat(businessId: string, userMessage: string, history?: any[]): Promise<{
        reply: string;
        timestamp: string;
    }>;
    createTicket(data: {
        businessId: string;
        subject: string;
        category?: string;
        priority?: string;
        initialMessage: string;
        createdById?: string;
        senderName?: string;
    }): Promise<{
        business: {
            id: string;
            name: string;
            country: string;
        };
        messages: {
            id: string;
            createdAt: Date;
            metadata: import("@prisma/client/runtime/library").JsonValue | null;
            message: string;
            senderType: string;
            senderName: string;
            ticketId: string;
        }[];
    } & {
        id: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        category: string;
        status: string;
        ticketNumber: string;
        subject: string;
        priority: string;
        createdById: string | null;
    }>;
    getBusinessTickets(businessId: string): Promise<({
        messages: {
            id: string;
            createdAt: Date;
            metadata: import("@prisma/client/runtime/library").JsonValue | null;
            message: string;
            senderType: string;
            senderName: string;
            ticketId: string;
        }[];
    } & {
        id: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        category: string;
        status: string;
        ticketNumber: string;
        subject: string;
        priority: string;
        createdById: string | null;
    })[]>;
    getAllTicketsForAdmin(query?: {
        status?: string;
        search?: string;
    }): Promise<({
        business: {
            id: string;
            name: string;
            country: string;
        };
        messages: {
            id: string;
            createdAt: Date;
            metadata: import("@prisma/client/runtime/library").JsonValue | null;
            message: string;
            senderType: string;
            senderName: string;
            ticketId: string;
        }[];
    } & {
        id: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        category: string;
        status: string;
        ticketNumber: string;
        subject: string;
        priority: string;
        createdById: string | null;
    })[]>;
    getTicketDetails(ticketId: string): Promise<{
        business: {
            id: string;
            name: string;
            country: string;
        };
        messages: {
            id: string;
            createdAt: Date;
            metadata: import("@prisma/client/runtime/library").JsonValue | null;
            message: string;
            senderType: string;
            senderName: string;
            ticketId: string;
        }[];
    } & {
        id: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        category: string;
        status: string;
        ticketNumber: string;
        subject: string;
        priority: string;
        createdById: string | null;
    }>;
    addMessage(ticketId: string, senderType: 'USER' | 'ADMIN' | 'AI', senderName: string, message: string): Promise<{
        id: string;
        createdAt: Date;
        metadata: import("@prisma/client/runtime/library").JsonValue | null;
        message: string;
        senderType: string;
        senderName: string;
        ticketId: string;
    }>;
    updateTicketStatus(ticketId: string, status: string, priority?: string): Promise<{
        business: {
            id: string;
            name: string;
        };
        messages: {
            id: string;
            createdAt: Date;
            metadata: import("@prisma/client/runtime/library").JsonValue | null;
            message: string;
            senderType: string;
            senderName: string;
            ticketId: string;
        }[];
    } & {
        id: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        category: string;
        status: string;
        ticketNumber: string;
        subject: string;
        priority: string;
        createdById: string | null;
    }>;
}
