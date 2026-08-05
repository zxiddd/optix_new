import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';

@Injectable()
export class ProductsService {
  constructor(
    private prisma: PrismaService,
    private syncGateway: SyncGateway,
  ) {}

  async getProducts(businessId: string) {
    return this.prisma.product.findMany({
      where: { businessId, isDeleted: false },
      include: { category: true },
      orderBy: { name: 'asc' },
    });
  }

  async saveProduct(businessId: string, dto: any) {
    const data = {
      name: dto.name,
      description: dto.description,
      barcode: dto.barcode,
      sku: dto.sku,
      price: dto.price,
      pricingType: dto.pricingType || 'FIXED',
      unit: dto.unit || 'Piece',
      categoryId: dto.categoryId,
      imageUrl: dto.imageUrl,
      isOutOfStock: dto.isOutOfStock ?? false,
    };

    let p;
    if (dto.id) {
      p = await this.prisma.product.upsert({
        where: { id: dto.id },
        update: data,
        create: {
          id: dto.id,
          businessId,
          ...data,
        },
      });
    } else {
      p = await this.prisma.product.create({
        data: {
          businessId,
          ...data,
        },
      });
    }

    this.syncGateway.emitToBusiness(businessId, 'product.updated', p);
    return p;
  }

  async deleteProduct(businessId: string, id: string) {
    const res = await this.prisma.product.updateMany({
      where: { id, businessId },
      data: { isDeleted: true },
    });
    this.syncGateway.emitToBusiness(businessId, 'product.deleted', { id });
    return res;
  }
}
