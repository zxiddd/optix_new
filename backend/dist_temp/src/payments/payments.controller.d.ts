import { RawBodyRequest } from '@nestjs/common';
import { PaymentsService } from './payments.service';
export declare class PaymentsController {
    private readonly paymentsService;
    constructor(paymentsService: PaymentsService);
    createOrder(req: any, body: {
        planId: string;
        billingCycle: 'MONTHLY' | 'YEARLY';
    }): Promise<any>;
    verifyPayment(req: any, body: {
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
    handleWebhook(signature: string, req: RawBodyRequest<any>): Promise<void>;
}
