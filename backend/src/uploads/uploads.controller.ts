import { Controller, Post, Get, Param, Res, UseInterceptors, UploadedFile, UseGuards, BadRequestException, NotFoundException } from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { ApiTags, ApiBearerAuth, ApiConsumes, ApiBody } from '@nestjs/swagger';
import { AtGuard } from '../auth/guards';
import { Public, GetCurrentUser } from '../auth/decorators';
import { diskStorage } from 'multer';
import { extname, join } from 'path';
import { Response } from 'express';
import { existsSync } from 'fs';

@ApiTags('Uploads')
@Controller('uploads')
export class UploadsController {
  @UseGuards(AtGuard)
  @ApiBearerAuth()
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
    return {
      url: `/api/v1/uploads/view/${file.filename}`,
    };
  }

  @Public()
  @Get('view/:filename')
  viewFile(@Param('filename') filename: string, @Res() res: Response) {
    const filePath = join(process.cwd(), 'uploads', filename);
    if (!existsSync(filePath)) {
      throw new NotFoundException('File not found');
    }
    return res.sendFile(filePath);
  }
}
