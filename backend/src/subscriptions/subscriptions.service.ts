import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';

@Injectable()
export class SubscriptionsService {
  constructor(
    private prisma: PrismaService,
    private syncGateway: SyncGateway,
  ) {}

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

  async activateCode(businessId: string, code: string) {
    if (!code) throw new BadRequestException('Activation code is required');

    const cleanCode = code.trim().toUpperCase();
    const activationCode = await this.prisma.activationCode.findUnique({
      where: { code: cleanCode },
    });

    if (!activationCode) {
      throw new BadRequestException('Invalid activation code. Please check and try again.');
    }

    if (!activationCode.isActive) {
      throw new BadRequestException('This activation code has been deactivated.');
    }

    if (activationCode.expiresAt && new Date(activationCode.expiresAt) < new Date()) {
      throw new BadRequestException('This activation code has expired.');
    }

    if (activationCode.maxUses > 0 && activationCode.usedCount >= activationCode.maxUses) {
      throw new BadRequestException('This activation code has reached its maximum usage limit.');
    }

    const daysToAdd = activationCode.billingCycle === 'YEARLY' ? 365 : 30;
    const expiryDate = new Date();
    expiryDate.setDate(expiryDate.getDate() + daysToAdd);

    const sub = await this.prisma.subscription.upsert({
      where: { businessId },
      create: {
        businessId,
        planId: activationCode.planId || 'STARTER',
        status: 'ACTIVE',
        billingCycle: activationCode.billingCycle || 'MONTHLY',
        activationCode: activationCode.code,
        expiryDate,
      },
      update: {
        planId: activationCode.planId || 'STARTER',
        status: 'ACTIVE',
        billingCycle: activationCode.billingCycle || 'MONTHLY',
        activationCode: activationCode.code,
        expiryDate,
      },
    });

    await this.prisma.activationCode.update({
      where: { id: activationCode.id },
      data: { usedCount: { increment: 1 } },
    });

    if (this.syncGateway) {
      this.syncGateway.emitToBusiness(businessId, 'subscription_updated', {
        status: 'ACTIVE',
        planId: sub.planId,
        expiryDate: sub.expiryDate,
      });
      this.syncGateway.emitToBusiness(businessId, 'remote_command', {
        action: 'REFRESH_SUBSCRIPTION',
        timestamp: Date.now(),
      });
    }

    return {
      success: true,
      message: `Successfully activated ${sub.planId} subscription!`,
      subscription: sub,
    };
  }
}
