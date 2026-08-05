import { Body, Controller, Delete, Get, Param, Post, UseGuards } from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { ProductsService } from './products.service';
import { AtGuard } from '../auth/guards';
import { GetCurrentUser } from '../auth/decorators';

@ApiTags('Products')
@ApiBearerAuth()
@UseGuards(AtGuard)
@Controller('products')
export class ProductsController {
  constructor(private productsService: ProductsService) {}

  @Get()
  @ApiOperation({ summary: 'Get all products' })
  getProducts(@GetCurrentUser('businessId') businessId: string) {
    return this.productsService.getProducts(businessId);
  }

  @Post()
  @ApiOperation({ summary: 'Create or update a product' })
  saveProduct(
    @GetCurrentUser('businessId') businessId: string,
    @Body() body: any,
  ) {
    return this.productsService.saveProduct(businessId, body);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Delete a product' })
  deleteProduct(
    @GetCurrentUser('businessId') businessId: string,
    @Param('id') id: string,
  ) {
    return this.productsService.deleteProduct(businessId, id);
  }
}
