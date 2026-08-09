import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';
export declare class BusinessService {
    private prisma;
    private syncGateway;
    constructor(prisma: PrismaService, syncGateway: SyncGateway);
    getProfile(businessId: string): Promise<{
        settings: {
            id: string;
            businessId: string;
            currency: string;
            openingTime: string;
            closingTime: string;
            timezone: string;
            taxEnabled: boolean;
            taxPercentage: import("@prisma/client/runtime/library").Decimal;
            lastResetBusinessDate: string | null;
        };
        receiptSettings: {
            id: string;
            businessId: string;
            taxPercentage: import("@prisma/client/runtime/library").Decimal;
            showLogo: boolean;
            logoUrl: string | null;
            headerMessage: string | null;
            footerMessage: string;
            showBusinessName: boolean;
            showAddress: boolean;
            showPhone: boolean;
            showGst: boolean;
            showDateTime: boolean;
            showOrderNumber: boolean;
            showCashierName: boolean;
            showDiscounts: boolean;
            showTaxes: boolean;
            qrEnabled: boolean;
            showVisitAgain: boolean;
        };
        printerSettings: {
            id: string;
            businessId: string;
            deviceName: string | null;
            deviceAddress: string | null;
            connectionType: string;
            paperWidth: number;
            autoConnect: boolean;
            copies: number;
            isDefault: boolean;
        }[];
        paymentQrs: {
            id: string;
            name: string;
            createdAt: Date;
            updatedAt: Date;
            businessId: string;
            isActive: boolean;
            imageUrl: string | null;
            upiId: string | null;
        }[];
    } & {
        id: string;
        name: string;
        email: string | null;
        phone: string | null;
        address: string | null;
        country: string;
        setupCompleted: boolean;
        tokenCounter: number;
        lastTokenResetDateTime: Date;
        createdAt: Date;
        updatedAt: Date;
        isDeleted: boolean;
        deletedAt: Date | null;
        deletedBy: string | null;
    }>;
    updateProfile(businessId: string, data: any, senderSocketId?: string): Promise<{
        settings: {
            id: string;
            businessId: string;
            currency: string;
            openingTime: string;
            closingTime: string;
            timezone: string;
            taxEnabled: boolean;
            taxPercentage: import("@prisma/client/runtime/library").Decimal;
            lastResetBusinessDate: string | null;
        };
        receiptSettings: {
            id: string;
            businessId: string;
            taxPercentage: import("@prisma/client/runtime/library").Decimal;
            showLogo: boolean;
            logoUrl: string | null;
            headerMessage: string | null;
            footerMessage: string;
            showBusinessName: boolean;
            showAddress: boolean;
            showPhone: boolean;
            showGst: boolean;
            showDateTime: boolean;
            showOrderNumber: boolean;
            showCashierName: boolean;
            showDiscounts: boolean;
            showTaxes: boolean;
            qrEnabled: boolean;
            showVisitAgain: boolean;
        };
        paymentQrs: {
            id: string;
            name: string;
            createdAt: Date;
            updatedAt: Date;
            businessId: string;
            isActive: boolean;
            imageUrl: string | null;
            upiId: string | null;
        }[];
    } & {
        id: string;
        name: string;
        email: string | null;
        phone: string | null;
        address: string | null;
        country: string;
        setupCompleted: boolean;
        tokenCounter: number;
        lastTokenResetDateTime: Date;
        createdAt: Date;
        updatedAt: Date;
        isDeleted: boolean;
        deletedAt: Date | null;
        deletedBy: string | null;
    }>;
}
