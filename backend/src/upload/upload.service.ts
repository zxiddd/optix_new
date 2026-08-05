import { Injectable, BadRequestException } from '@nestjs/common';
import * as fs from 'fs';
import * as path from 'path';

@Injectable()
export class UploadService {
  private readonly uploadDir = path.join(process.cwd(), 'uploads');

  constructor() {
    if (!fs.existsSync(this.uploadDir)) {
      fs.mkdirSync(this.uploadDir, { recursive: true });
    }
  }

  async saveFile(file: Express.Multer.File, category: 'businesses' | 'products', businessId: string): Promise<string> {
    if (!file) {
      throw new BadRequestException('No file provided');
    }

    const targetDir = path.join(this.uploadDir, category, businessId);
    if (!fs.existsSync(targetDir)) {
      fs.mkdirSync(targetDir, { recursive: true });
    }

    const ext = path.extname(file.originalname) || '.png';
    const filename = `${Date.now()}_${Math.random().toString(36).substring(2, 8)}${ext}`;
    const filePath = path.join(targetDir, filename);

    await fs.promises.writeFile(filePath, file.buffer);

    const baseUrl = process.env.BASE_API_URL || 'https://api.optixapp.in';
    return `${baseUrl}/uploads/${category}/${businessId}/${filename}`;
  }
}
