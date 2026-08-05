import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class SubscriptionsService {
  constructor(private prisma: PrismaService) {}

  async getSubscription(businessId: string) {
    return this.prisma.subscription.findFirst({
      where: { businessId },
      include: { plan: true },
      orderBy: { createdAt: 'desc' },
    });
  }

  async getPlans() {
    return this.prisma.plan.findMany();
  }

  async saveSubscription(businessId: string, dto: any) {
    let plan = await this.prisma.plan.findFirst({ where: { name: dto.planName || 'Starter' } });
    if (!plan) {
      plan = await this.prisma.plan.create({
        data: {
          name: dto.planName || 'Starter',
          price: 999.00,
          billingPeriod: 'MONTHLY',
          features: { items: 'unlimited', sync: true },
        },
      });
    }

    return this.prisma.subscription.create({
      data: {
        businessId,
        planId: plan.id,
        status: dto.status || 'ACTIVE',
        expiryDate: new Date(dto.expiryDate || Date.now() + 30 * 24 * 60 * 60 * 1000),
      },
      include: { plan: true },
    });
  }
}
