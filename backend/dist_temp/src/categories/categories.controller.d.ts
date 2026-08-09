import { CategoriesService } from './categories.service';
export declare class CategoriesController {
    private categoriesService;
    constructor(categoriesService: CategoriesService);
    getCategories(businessId: string): Promise<{
        id: string;
        name: string;
        isDeleted: boolean;
        businessId: string;
        version: number;
        lastModified: Date;
        sortOrder: number;
    }[]>;
    saveCategory(businessId: string, body: {
        id?: string;
        name: string;
        sortOrder?: number;
    }): Promise<any>;
    deleteCategory(businessId: string, id: string): Promise<import(".prisma/client").Prisma.BatchPayload>;
}
