import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';
export declare class SubscriptionsService {
    private prisma;
    private syncGateway;
    constructor(prisma: PrismaService, syncGateway: SyncGateway);
    getSubscription(businessId: string): Promise<{
        plan: {
            id: string;
            name: string;
            createdAt: Date;
            price: import("@prisma/client/runtime/library").Decimal;
            billingPeriod: string;
            features: import("@prisma/client/runtime/library").JsonValue;
        };
    } & {
        id: string;
        country: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        activationCode: string | null;
        planId: string;
        status: import(".prisma/client").$Enums.SubscriptionStatus;
        currency: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        billsUsed: number;
        productsUsed: number;
        expiryDate: Date;
        renewalDate: Date | null;
    }>;
    getPlans(): Promise<{
        id: string;
        name: string;
        createdAt: Date;
        price: import("@prisma/client/runtime/library").Decimal;
        billingPeriod: string;
        features: import("@prisma/client/runtime/library").JsonValue;
    }[]>;
    saveSubscription(businessId: string, dto: any): Promise<{
        plan: {
            id: string;
            name: string;
            createdAt: Date;
            price: import("@prisma/client/runtime/library").Decimal;
            billingPeriod: string;
            features: import("@prisma/client/runtime/library").JsonValue;
        };
    } & {
        id: string;
        country: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        activationCode: string | null;
        planId: string;
        status: import(".prisma/client").$Enums.SubscriptionStatus;
        currency: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        billsUsed: number;
        productsUsed: number;
        expiryDate: Date;
        renewalDate: Date | null;
    }>;
    activateCode(businessId: string, code: string): Promise<{
        success: boolean;
        message: string;
        subscription: {
            id: string;
            country: string;
            createdAt: Date;
            updatedAt: Date;
            businessId: string;
            activationCode: string | null;
            planId: string;
            status: import(".prisma/client").$Enums.SubscriptionStatus;
            currency: string;
            billingCycle: import(".prisma/client").$Enums.BillingCycle;
            billsUsed: number;
            productsUsed: number;
            expiryDate: Date;
            renewalDate: Date | null;
        };
    }>;
}
