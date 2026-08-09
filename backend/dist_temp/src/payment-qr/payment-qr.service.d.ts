import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';
export declare class PaymentQrService {
    private prisma;
    private syncGateway;
    constructor(prisma: PrismaService, syncGateway: SyncGateway);
    getPaymentQrs(businessId: string): Promise<{
        id: string;
        name: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        isActive: boolean;
        imageUrl: string | null;
        upiId: string | null;
    }[]>;
    savePaymentQr(businessId: string, dto: any, senderSocketId?: string): Promise<{
        id: string;
        name: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        isActive: boolean;
        imageUrl: string | null;
        upiId: string | null;
    }>;
    selectPaymentQr(businessId: string, id: string, senderSocketId?: string): Promise<{
        id: string;
        name: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        isActive: boolean;
        imageUrl: string | null;
        upiId: string | null;
    }>;
    deletePaymentQr(businessId: string, id: string, senderSocketId?: string): Promise<{
        success: boolean;
        id: string;
    }>;
}
