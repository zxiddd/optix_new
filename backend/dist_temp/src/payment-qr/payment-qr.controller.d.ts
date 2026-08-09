import { PaymentQrService } from './payment-qr.service';
export declare class PaymentQrController {
    private paymentQrService;
    constructor(paymentQrService: PaymentQrService);
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
    savePaymentQr(businessId: string, body: any, socketId?: string): Promise<{
        id: string;
        name: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        isActive: boolean;
        imageUrl: string | null;
        upiId: string | null;
    }>;
    selectPaymentQr(businessId: string, id: string, socketId?: string): Promise<{
        id: string;
        name: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        isActive: boolean;
        imageUrl: string | null;
        upiId: string | null;
    }>;
    deletePaymentQr(businessId: string, id: string, socketId?: string): Promise<{
        success: boolean;
        id: string;
    }>;
}
