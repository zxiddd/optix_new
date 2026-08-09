import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';
export declare class ProductsService {
    private prisma;
    private syncGateway;
    constructor(prisma: PrismaService, syncGateway: SyncGateway);
    getProducts(businessId: string): Promise<({
        category: {
            id: string;
            name: string;
            isDeleted: boolean;
            businessId: string;
            version: number;
            lastModified: Date;
            sortOrder: number;
        };
    } & {
        id: string;
        name: string;
        isDeleted: boolean;
        businessId: string;
        description: string | null;
        barcode: string | null;
        sku: string | null;
        price: import("@prisma/client/runtime/library").Decimal;
        pricingType: import(".prisma/client").$Enums.PricingType;
        unit: string;
        categoryId: string;
        imageUrl: string | null;
        isOutOfStock: boolean;
        version: number;
        lastModified: Date;
    })[]>;
    saveProduct(businessId: string, dto: any): Promise<any>;
    deleteProduct(businessId: string, id: string): Promise<import(".prisma/client").Prisma.BatchPayload>;
}
