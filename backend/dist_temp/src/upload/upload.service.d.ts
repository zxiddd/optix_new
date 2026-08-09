export declare class UploadService {
    private readonly uploadDir;
    constructor();
    saveFile(file: Express.Multer.File, category: 'businesses' | 'products', businessId: string): Promise<string>;
}
