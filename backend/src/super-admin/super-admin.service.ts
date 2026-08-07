import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class SuperAdminService {
  constructor(private prisma: PrismaService) {}

  async getAllPayments(filters: any) {
    const where: any = {};
    if (filters.status) where.status = filters.status;
    if (filters.businessId) where.businessId = filters.businessId;

    return this.prisma.paymentTransaction.findMany({
      where,
      include: {
        business: {
          select: {
            name: true,
            id: true,
            country: true,
          }
        },
      },
      orderBy: { createdAt: 'desc' },
      take: 100,
    });
  }

  async getDashboardStats() {
    const totalPayments = await this.prisma.paymentTransaction.aggregate({
      where: { status: 'CAPTURED' },
      _sum: { amount: true },
    });

    const activeSubscriptions = await this.prisma.subscription.count({
      where: { status: 'ACTIVE' },
    });

    return {
      totalRevenue: totalPayments._sum.amount || 0,
      activeSubscriptions,
    };
  }
}
