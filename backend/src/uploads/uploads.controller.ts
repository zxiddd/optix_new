import { Controller, Post, UseInterceptors, UploadedFile, UseGuards, BadRequestException } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { ApiTags, ApiBearerAuth, ApiConsumes, ApiBody } from '@nestjs/swagger';
import { AtGuard } from '../auth/guards';
import { diskStorage } from 'multer';
import { extname } from 'path';
import { GetCurrentUser } from '../auth/decorators';

@ApiTags('Uploads')
@ApiBearerAuth()
@UseGuards(AtGuard)
@Controller('uploads')
export class UploadsController {
  @Post()
  @ApiConsumes('multipart/form-data')
  @ApiBody({
    schema: {
      type: 'object',
      properties: {
        file: {
          type: 'string',
          format: 'binary',
        },
      },
    },
  })
  @UseInterceptors(FileInterceptor('file', {
    storage: diskStorage({
      destination: './uploads',
      filename: (req, file, cb) => {
        const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9);
        cb(null, `${uniqueSuffix}${extname(file.originalname)}`);
      },
    }),
    fileFilter: (req, file, cb) => {
      if (!file.mimetype.match(/\/(jpg|jpeg|png|gif)$/)) {
        return cb(new BadRequestException('Only image files are allowed!'), false);
      }
      cb(null, true);
    },
  }))
  uploadFile(@UploadedFile() file: Express.Multer.File, @GetCurrentUser('businessId') businessId: string) {
    if (!file) {
      throw new BadRequestException('File is required');
    }
    // In a real SaaS, you'd store this in a folder scoped by businessId
    // and potentially use S3. For this VPS migration, we use local disk.
    return {
      url: `/api/v1/uploads/view/${file.filename}`,
    };
  }
}
