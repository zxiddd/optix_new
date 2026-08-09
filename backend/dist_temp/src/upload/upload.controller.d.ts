import { UploadService } from './upload.service';
export declare class UploadController {
    private uploadService;
    constructor(uploadService: UploadService);
    uploadFile(file: Express.Multer.File, category: 'businesses' | 'products', businessId: string): Promise<{
        url: string;
    }>;
}
