import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';
export declare class OrdersService {
    private prisma;
    private syncGateway;
    private readonly logger;
    constructor(prisma: PrismaService, syncGateway: SyncGateway);
    getOrders(businessId: string): Promise<({
        items: {
            id: string;
            price: import("@prisma/client/runtime/library").Decimal;
            unit: string | null;
            orderId: string;
            productId: string;
            productName: string;
            quantity: number | null;
            weight: import("@prisma/client/runtime/library").Decimal | null;
        }[];
    } & {
        id: string;
        createdAt: Date;
        businessId: string;
        status: import(".prisma/client").$Enums.OrderStatus;
        tokenNumber: string;
        invoiceNumber: string;
        subtotal: import("@prisma/client/runtime/library").Decimal;
        discount: import("@prisma/client/runtime/library").Decimal;
        tax: import("@prisma/client/runtime/library").Decimal;
        total: import("@prisma/client/runtime/library").Decimal;
        paymentMethod: import(".prisma/client").$Enums.PaymentMethod;
        cashierName: string;
        customerName: string | null;
        customerId: string | null;
    })[]>;
    saveOrder(businessId: string, dto: any): Promise<{
        items: {
            id: string;
            price: import("@prisma/client/runtime/library").Decimal;
            unit: string | null;
            orderId: string;
            productId: string;
            productName: string;
            quantity: number | null;
            weight: import("@prisma/client/runtime/library").Decimal | null;
        }[];
    } & {
        id: string;
        createdAt: Date;
        businessId: string;
        status: import(".prisma/client").$Enums.OrderStatus;
        tokenNumber: string;
        invoiceNumber: string;
        subtotal: import("@prisma/client/runtime/library").Decimal;
        discount: import("@prisma/client/runtime/library").Decimal;
        tax: import("@prisma/client/runtime/library").Decimal;
        total: import("@prisma/client/runtime/library").Decimal;
        paymentMethod: import(".prisma/client").$Enums.PaymentMethod;
        cashierName: string;
        customerName: string | null;
        customerId: string | null;
    }>;
    deleteOrder(businessId: string, id: string): Promise<import(".prisma/client").Prisma.BatchPayload>;
}
