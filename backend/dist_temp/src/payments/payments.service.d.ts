import { PrismaService } from '../prisma/prisma.service';
import { ConfigService } from '@nestjs/config';
import { SyncGateway } from '../sync/sync.gateway';
export declare class PaymentsService {
    private prisma;
    private config;
    private syncGateway;
    private razorpay;
    private readonly logger;
    constructor(prisma: PrismaService, config: ConfigService, syncGateway: SyncGateway);
    createOrder(businessId: string, planId: string, cycle: 'MONTHLY' | 'YEARLY'): Promise<any>;
    verifyPayment(businessId: string, data: {
        razorpay_order_id: string;
        razorpay_payment_id: string;
        razorpay_signature: string;
    }): Promise<{
        success: boolean;
        subscription: {
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
        };
    }>;
    handleWebhook(signature: string, rawBody: string): Promise<void>;
    private completePayment;
}
