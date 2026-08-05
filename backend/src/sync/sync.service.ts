import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { SyncPushDto } from './dto/sync.dto';

@Injectable()
export class SyncService {
  constructor(private prisma: PrismaService) {}

  async push(businessId: string, dto: SyncPushDto) {
    return await this.prisma.$transaction(async (tx) => {
      // 1. Process Categories
      if (dto.categories) {
        for (const cat of dto.categories) {
          await tx.category.upsert({
            where: { id: cat.id },
            update: {
              name: cat.name,
              sortOrder: cat.sortOrder,
              version: cat.version,
              isDeleted: cat.isDeleted,
              lastModified: new Date(cat.lastModified),
            },
            create: {
              id: cat.id,
              businessId,
              name: cat.name,
              sortOrder: cat.sortOrder,
              version: cat.version,
              isDeleted: cat.isDeleted,
              lastModified: new Date(cat.lastModified),
            },
          });
        }
      }

      // 2. Process Products
      if (dto.products) {
        for (const prod of dto.products) {
          await tx.product.upsert({
            where: { id: prod.id },
            update: {
              name: prod.name,
              description: prod.description,
              barcode: prod.barcode,
              sku: prod.sku,
              price: prod.price,
              pricingType: prod.pricingType,
              unit: prod.unit,
              categoryId: prod.categoryId,
              imageUrl: prod.imageUrl,
              isOutOfStock: prod.isOutOfStock,
              version: prod.version,
              isDeleted: prod.isDeleted,
              lastModified: new Date(prod.lastModified),
            },
            create: {
              id: prod.id,
              businessId,
              name: prod.name,
              description: prod.description,
              barcode: prod.barcode,
              sku: prod.sku,
              price: prod.price,
              pricingType: prod.pricingType,
              unit: prod.unit,
              categoryId: prod.categoryId,
              imageUrl: prod.imageUrl,
              isOutOfStock: prod.isOutOfStock,
              version: prod.version,
              isDeleted: prod.isDeleted,
              lastModified: new Date(prod.lastModified),
            },
          });
        }
      }

      return { status: 'success', timestamp: Date.now() };
    });
  }

  async pull(businessId: string, lastSyncTimestamp: number) {
    const lastDate = new Date(lastSyncTimestamp);

    const [categories, products] = await Promise.all([
      this.prisma.category.findMany({
        where: {
          businessId,
          lastModified: { gt: lastDate },
        },
      }),
      this.prisma.product.findMany({
        where: {
          businessId,
          lastModified: { gt: lastDate },
        },
      }),
    ]);

    return {
      categories,
      products,
      serverTime: Date.now(),
    };
  }
}
