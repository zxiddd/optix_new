import { Response } from 'express';
export declare class UploadsController {
    uploadFile(file: Express.Multer.File, businessId: string): {
        url: string;
    };
    viewFile(filename: string, res: Response): void;
}
