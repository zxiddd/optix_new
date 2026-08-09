import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';
export declare class CategoriesService {
    private prisma;
    private syncGateway;
    constructor(prisma: PrismaService, syncGateway: SyncGateway);
    getCategories(businessId: string): Promise<{
        id: string;
        name: string;
        isDeleted: boolean;
        businessId: string;
        version: number;
        lastModified: Date;
        sortOrder: number;
    }[]>;
    saveCategory(businessId: string, dto: {
        id?: string;
        name: string;
        sortOrder?: number;
    }): Promise<any>;
    deleteCategory(businessId: string, id: string): Promise<import(".prisma/client").Prisma.BatchPayload>;
}
