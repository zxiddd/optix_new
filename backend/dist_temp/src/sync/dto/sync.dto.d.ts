import { PricingType } from '@prisma/client';
export declare class SyncProductDto {
    id: string;
    name: string;
    description?: string;
    barcode?: string;
    sku?: string;
    price: number;
    pricingType: PricingType;
    unit: string;
    categoryId: string;
    imageUrl?: string;
    isOutOfStock: boolean;
    version: number;
    isDeleted: boolean;
    lastModified: number;
}
export declare class SyncCategoryDto {
    id: string;
    name: string;
    sortOrder: number;
    version: number;
    isDeleted: boolean;
    lastModified: number;
}
export declare class SyncPaymentQrDto {
    id: string;
    name: string;
    imageUrl: string;
    isActive?: boolean;
    isDeleted?: boolean;
}
export declare class SyncPushDto {
    categories?: SyncCategoryDto[];
    products?: SyncProductDto[];
    paymentQrs?: SyncPaymentQrDto[];
}
