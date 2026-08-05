import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';

@Injectable()
export class CategoriesService {
  constructor(
    private prisma: PrismaService,
    private syncGateway: SyncGateway,
  ) {}

  async getCategories(businessId: string) {
    return this.prisma.category.findMany({
      where: { businessId, isDeleted: false },
      orderBy: { sortOrder: 'asc' },
    });
  }

  async saveCategory(businessId: string, dto: { id?: string; name: string; sortOrder?: number }) {
    let cat;
    if (dto.id) {
      cat = await this.prisma.category.upsert({
        where: { id: dto.id },
        update: {
          name: dto.name,
          sortOrder: dto.sortOrder ?? 0,
        },
        create: {
          id: dto.id,
          businessId,
          name: dto.name,
          sortOrder: dto.sortOrder ?? 0,
        },
      });
    } else {
      cat = await this.prisma.category.create({
        data: {
          businessId,
          name: dto.name,
          sortOrder: dto.sortOrder ?? 0,
        },
      });
    }

    this.syncGateway.emitToBusiness(businessId, 'category.updated', cat);
    return cat;
  }

  async deleteCategory(businessId: string, id: string) {
    const res = await this.prisma.category.updateMany({
      where: { id, businessId },
      data: { isDeleted: true },
    });
    this.syncGateway.emitToBusiness(businessId, 'category.deleted', { id });
    return res;
  }
}
