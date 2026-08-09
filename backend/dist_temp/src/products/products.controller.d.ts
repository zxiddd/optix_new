import { ProductsService } from './products.service';
export declare class ProductsController {
    private productsService;
    constructor(productsService: ProductsService);
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
    saveProduct(businessId: string, body: any): Promise<any>;
    deleteProduct(businessId: string, id: string): Promise<import(".prisma/client").Prisma.BatchPayload>;
}
