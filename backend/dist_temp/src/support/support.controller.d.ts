import { SupportService } from './support.service';
export declare class SupportController {
    private readonly supportService;
    constructor(supportService: SupportService);
    processAiChat(body: {
        businessId: string;
        message: string;
        history?: any[];
    }): Promise<{
        reply: string;
        timestamp: string;
    }>;
    createTicket(body: any): Promise<{
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
    getAllTicketsForAdmin(query: {
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
    getTicketDetails(id: string): Promise<{
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
    addMessage(id: string, body: {
        senderType: 'USER' | 'ADMIN' | 'AI';
        senderName: string;
        message: string;
    }): Promise<{
        id: string;
        createdAt: Date;
        metadata: import("@prisma/client/runtime/library").JsonValue | null;
        message: string;
        senderType: string;
        senderName: string;
        ticketId: string;
    }>;
    updateTicketStatus(id: string, body: {
        status: string;
        priority?: string;
    }): Promise<{
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
