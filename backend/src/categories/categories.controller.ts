import { Body, Controller, Delete, Get, Param, Post, UseGuards } from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { CategoriesService } from './categories.service';
import { AtGuard } from '../auth/guards';
import { GetCurrentUser } from '../auth/decorators';

@ApiTags('Categories')
@ApiBearerAuth()
@UseGuards(AtGuard)
@Controller('categories')
export class CategoriesController {
  constructor(private categoriesService: CategoriesService) {}

  @Get()
  @ApiOperation({ summary: 'Get all categories' })
  getCategories(@GetCurrentUser('businessId') businessId: string) {
    return this.categoriesService.getCategories(businessId);
  }

  @Post()
  @ApiOperation({ summary: 'Create or update a category' })
  saveCategory(
    @GetCurrentUser('businessId') businessId: string,
    @Body() body: { id?: string; name: string; sortOrder?: number },
  ) {
    return this.categoriesService.saveCategory(businessId, body);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Delete a category' })
  deleteCategory(
    @GetCurrentUser('businessId') businessId: string,
    @Param('id') id: string,
  ) {
    return this.categoriesService.deleteCategory(businessId, id);
  }
}
