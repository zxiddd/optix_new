import { Controller, Post, UseInterceptors, UploadedFile, Body, UseGuards } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { ApiTags, ApiBearerAuth, ApiOperation, ApiConsumes, ApiBody } from '@nestjs/swagger';
import { UploadService } from './upload.service';
import { AtGuard } from '../auth/guards';
import { GetCurrentUser } from '../auth/decorators';

@ApiTags('Upload')
@ApiBearerAuth()
@UseGuards(AtGuard)
@Controller(['upload', 'uploads'])
export class UploadController {
  constructor(private uploadService: UploadService) {}

  @Post()
  @ApiOperation({ summary: 'Upload an image file (product or business)' })
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      properties: {
        file: { type: 'string', format: 'binary' },
        category: { type: 'string', enum: ['businesses', 'products'] },
      },
    },
  })
  @UseInterceptors(FileInterceptor('file'))
  async uploadFile(
    @UploadedFile() file: Express.Multer.File,
    @Body('category') category: 'businesses' | 'products' = 'products',
    @GetCurrentUser('businessId') businessId: string,
  ) {
    const url = await this.uploadService.saveFile(file, category, businessId);
    return { url };
  }
}
