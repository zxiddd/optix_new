import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';
import * as crypto from 'crypto';

@Injectable()
export class SuperAdminService {
  constructor(
    private prisma: PrismaService,
    private syncGateway: SyncGateway,
  ) {}

  // ─────────────────────────────────────────────────────────────────────────
  // BUSINESSES
  // ─────────────────────────────────────────────────────────────────────────

  async getBusinesses(query: {
    page?: number;
    limit?: number;
    search?: string;
    planId?: string;
    status?: string;
    country?: string;
  }) {
    const page = Number(query.page) || 1;
    const limit = Number(query.limit) || 20;
    const skip = (page - 1) * limit;

    const where: any = { isDeleted: false };

    if (query.search) {
      where.OR = [
        { name: { contains: query.search, mode: 'insensitive' } },
        { id: { contains: query.search, mode: 'insensitive' } },
        { phone: { contains: query.search, mode: 'insensitive' } },
        { email: { contains: query.search, mode: 'insensitive' } },
      ];
    }

    if (query.planId) {
      where.subscriptions = { some: { planId: query.planId } };
    }

    if (query.status) {
      where.subscriptions = { some: { status: query.status } };
    }

    if (query.country) {
      where.country = query.country;
    }

    const [total, items] = await Promise.all([
      this.prisma.business.count({ where }),
      this.prisma.business.findMany({
        where,
        include: {
          subscriptions: {
            include: { plan: true },
            orderBy: { createdAt: 'desc' },
            take: 1,
          },
          _count: {
            select: {
              products: true,
              orders: true,
              staff: true,
            },
          },
          users: {
            where: { role: 'OWNER' },
            select: { email: true },
            take: 1,
          },
        },
        orderBy: { createdAt: 'desc' },
        skip,
        take: limit,
      }),
    ]);

    return {
      items,
      meta: {
        total,
        page,
        lastPage: Math.ceil(total / limit),
      },
    };
  }

  async getBusinessDetail(id: string) {
    const business = await this.prisma.business.findUnique({
      where: { id },
      include: {
        settings: true,
        subscriptions: {
          include: { plan: true },
          orderBy: { createdAt: 'desc' },
        },
        users: {
          select: { id: true, email: true, role: true, createdAt: true },
        },
        _count: {
          select: {
            products: true,
            orders: true,
            staff: true,
            categories: true,
            devices: true,
          },
        },
        devices: {
          orderBy: { lastSeen: 'desc' },
          take: 5,
        },
        transactions: {
          orderBy: { createdAt: 'desc' },
          take: 10,
        },
      },
    });

    if (!business) throw new NotFoundException('Business not found');

    const recentOrders = await this.prisma.order.findMany({
      where: { businessId: id },
      orderBy: { createdAt: 'desc' },
      take: 5,
      include: { items: true },
    });

    const recentLogs = await this.prisma.staffActivityLog.findMany({
      where: { businessId: id },
      orderBy: { createdAt: 'desc' },
      take: 10,
      include: { staff: { select: { name: true } } },
    });

    return { ...business, recentOrders, recentLogs };
  }

  async updateBusinessStatus(id: string, status: 'ACTIVE' | 'EXPIRED' | 'CANCELLED' | 'SUSPENDED') {
    const sub = await this.prisma.subscription.findUnique({ where: { businessId: id } });
    if (!sub) throw new NotFoundException('Subscription not found');

    const updated = await this.prisma.subscription.update({
      where: { businessId: id },
      data: { status: status as any },
    });

    await this.writeAdminAuditLog(id, 'UPDATE_STATUS', 'SUBSCRIPTION', sub.id, { status: sub.status }, { status });
    this.syncGateway.emitToBusiness(id, 'subscription_updated', { status });
    return updated;
  }

  async resetTrialLimits(id: string) {
    const updated = await this.prisma.subscription.update({
      where: { businessId: id },
      data: { billsUsed: 0, productsUsed: 0 },
    });
    await this.writeAdminAuditLog(id, 'RESET_TRIAL', 'SUBSCRIPTION', updated.id, {}, { billsUsed: 0, productsUsed: 0 });
    return updated;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // PAYMENTS
  // ─────────────────────────────────────────────────────────────────────────

  async getAllPayments(filters: {
    status?: string;
    businessId?: string;
    planId?: string;
    country?: string;
    currency?: string;
    gateway?: string;
    dateFrom?: string;
    dateTo?: string;
    page?: number;
    limit?: number;
    search?: string;
  }) {
    const page = Number(filters.page) || 1;
    const limit = Number(filters.limit) || 50;
    const skip = (page - 1) * limit;

    const where: any = {};
    if (filters.status) where.status = filters.status;
    if (filters.businessId) where.businessId = filters.businessId;
    if (filters.planId) where.planId = filters.planId;
    if (filters.country) where.country = { equals: filters.country, mode: 'insensitive' };
    if (filters.currency) where.currency = filters.currency;
    if (filters.dateFrom || filters.dateTo) {
      where.createdAt = {};
      if (filters.dateFrom) where.createdAt.gte = new Date(filters.dateFrom);
      if (filters.dateTo) where.createdAt.lte = new Date(filters.dateTo);
    }
    if (filters.search) {
      where.OR = [
        { razorpayOrderId: { contains: filters.search, mode: 'insensitive' } },
        { razorpayPaymentId: { contains: filters.search, mode: 'insensitive' } },
        { business: { name: { contains: filters.search, mode: 'insensitive' } } },
      ];
    }

    const [total, items] = await Promise.all([
      this.prisma.paymentTransaction.count({ where }),
      this.prisma.paymentTransaction.findMany({
        where,
        include: {
          business: {
            select: { id: true, name: true, country: true, users: { where: { role: 'OWNER' }, select: { email: true }, take: 1 } },
          },
        },
        orderBy: { createdAt: 'desc' },
        skip,
        take: limit,
      }),
    ]);

    return {
      items,
      meta: { total, page, lastPage: Math.ceil(total / limit) },
    };
  }

  async getPaymentDetail(id: string) {
    const tx = await this.prisma.paymentTransaction.findUnique({
      where: { id },
      include: {
        business: {
          include: { users: { where: { role: 'OWNER' }, select: { email: true }, take: 1 } },
        },
        subscription: { include: { plan: true } },
      },
    });
    if (!tx) throw new NotFoundException('Payment not found');
    return tx;
  }

  async refundPayment(id: string, reason?: string, partial?: number) {
    const tx = await this.prisma.paymentTransaction.findUnique({ where: { id } });
    if (!tx) throw new NotFoundException('Payment not found');
    if (tx.status !== 'CAPTURED') throw new BadRequestException('Only CAPTURED payments can be refunded');

    const updated = await this.prisma.paymentTransaction.update({
      where: { id },
      data: {
        status: 'REFUNDED',
        gatewayMetadata: {
          ...(tx.gatewayMetadata as any || {}),
          refundReason: reason,
          refundedAt: new Date().toISOString(),
          refundAmount: partial ?? tx.amount,
        },
      },
    });

    await this.writeAdminAuditLog(tx.businessId, 'REFUND_PAYMENT', 'PAYMENT', id, { status: 'CAPTURED' }, { status: 'REFUNDED', reason });
    return updated;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // SUBSCRIPTIONS
  // ─────────────────────────────────────────────────────────────────────────

  async getAllSubscriptions(filters: {
    status?: string;
    planId?: string;
    country?: string;
    page?: number;
    limit?: number;
    search?: string;
  }) {
    const page = Number(filters.page) || 1;
    const limit = Number(filters.limit) || 50;
    const skip = (page - 1) * limit;

    const where: any = {};
    if (filters.status) where.status = filters.status;
    if (filters.planId) where.planId = filters.planId;
    if (filters.country) where.country = { equals: filters.country, mode: 'insensitive' };
    if (filters.search) {
      where.business = { name: { contains: filters.search, mode: 'insensitive' } };
    }

    const [total, items] = await Promise.all([
      this.prisma.subscription.count({ where }),
      this.prisma.subscription.findMany({
        where,
        include: {
          plan: true,
          business: {
            select: { id: true, name: true, country: true, users: { where: { role: 'OWNER' }, select: { email: true }, take: 1 } },
          },
        },
        orderBy: { createdAt: 'desc' },
        skip,
        take: limit,
      }),
    ]);

    return {
      items,
      meta: { total, page, lastPage: Math.ceil(total / limit) },
    };
  }

  async getSubscriptionDetail(id: string) {
    const sub = await this.prisma.subscription.findUnique({
      where: { id },
      include: {
        plan: true,
        business: { include: { users: { where: { role: 'OWNER' }, select: { email: true }, take: 1 } } },
        transactions: { orderBy: { createdAt: 'desc' }, take: 10 },
      },
    });
    if (!sub) throw new NotFoundException('Subscription not found');
    return sub;
  }

  async changePlan(businessId: string, planId: string, billingCycle: 'MONTHLY' | 'YEARLY') {
    const sub = await this.prisma.subscription.findUnique({ where: { businessId } });
    if (!sub) throw new NotFoundException('Subscription not found');

    const plan = await this.prisma.plan.findFirst({ where: { OR: [{ id: planId }, { name: planId }] } });
    if (!plan) throw new NotFoundException(`Plan '${planId}' not found`);

    const durationDays = billingCycle === 'YEARLY' ? 365 : 30;
    const expiryDate = new Date();
    expiryDate.setDate(expiryDate.getDate() + durationDays);

    const updated = await this.prisma.subscription.update({
      where: { businessId },
      data: { planId: plan.id, billingCycle: billingCycle as any, status: 'ACTIVE', expiryDate },
    });

    await this.writeAdminAuditLog(businessId, 'CHANGE_PLAN', 'SUBSCRIPTION', sub.id,
      { planId: sub.planId, billingCycle: sub.billingCycle },
      { planId: plan.id, billingCycle }
    );

    this.syncGateway.emitToBusiness(businessId, 'subscription_updated', {
      planId: plan.id,
      planName: plan.name,
      status: 'ACTIVE',
      billingCycle,
      expiryDate: expiryDate.getTime(),
    });

    return updated;
  }

  async extendSubscription(businessId: string, days: number) {
    const sub = await this.prisma.subscription.findUnique({ where: { businessId } });
    if (!sub) throw new NotFoundException('Subscription not found');

    const newExpiry = new Date(sub.expiryDate);
    newExpiry.setDate(newExpiry.getDate() + days);

    const updated = await this.prisma.subscription.update({
      where: { businessId },
      data: { expiryDate: newExpiry },
    });

    await this.writeAdminAuditLog(businessId, 'EXTEND_SUBSCRIPTION', 'SUBSCRIPTION', sub.id,
      { expiryDate: sub.expiryDate },
      { expiryDate: newExpiry, addedDays: days }
    );

    this.syncGateway.emitToBusiness(businessId, 'subscription_updated', {
      expiryDate: newExpiry.getTime(),
    });

    return updated;
  }

  async updateSubscriptionStatus(businessId: string, status: string) {
    const sub = await this.prisma.subscription.findUnique({ where: { businessId } });
    if (!sub) throw new NotFoundException('Subscription not found');

    const updated = await this.prisma.subscription.update({
      where: { businessId },
      data: { status: status as any },
    });

    await this.writeAdminAuditLog(businessId, 'UPDATE_SUBSCRIPTION_STATUS', 'SUBSCRIPTION', sub.id,
      { status: sub.status }, { status }
    );

    this.syncGateway.emitToBusiness(businessId, 'subscription_updated', { status });
    return updated;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // ACTIVATION CODES
  // ─────────────────────────────────────────────────────────────────────────

  async getActivationCodes(filters: {
    search?: string;
    planId?: string;
    isActive?: string;
    page?: number;
    limit?: number;
  }) {
    const page = Number(filters.page) || 1;
    const limit = Number(filters.limit) || 50;
    const skip = (page - 1) * limit;

    const where: any = {};
    if (filters.planId) where.planId = filters.planId;
    if (filters.isActive !== undefined) where.isActive = filters.isActive === 'true';
    if (filters.search) {
      where.OR = [
        { code: { contains: filters.search, mode: 'insensitive' } },
        { notes: { contains: filters.search, mode: 'insensitive' } },
      ];
    }

    const [total, items] = await Promise.all([
      this.prisma.activationCode.count({ where }),
      this.prisma.activationCode.findMany({
        where,
        orderBy: { createdAt: 'desc' },
        skip,
        take: limit,
      }),
    ]);

    return {
      items,
      meta: { total, page, lastPage: Math.ceil(total / limit) },
    };
  }

  async createActivationCode(data: {
    planId: string;
    billingCycle: 'MONTHLY' | 'YEARLY';
    maxUses: number;
    countryRestriction?: string;
    expiresAt?: string;
    notes?: string;
  }) {
    const code = this.generateCode();
    return this.prisma.activationCode.create({
      data: {
        code,
        planId: data.planId,
        billingCycle: data.billingCycle as any,
        maxUses: data.maxUses,
        countryRestriction: data.countryRestriction,
        expiresAt: data.expiresAt ? new Date(data.expiresAt) : null,
        notes: data.notes,
      },
    });
  }

  async bulkCreateActivationCodes(data: {
    count: number;
    planId: string;
    billingCycle: 'MONTHLY' | 'YEARLY';
    maxUses: number;
    countryRestriction?: string;
    expiresAt?: string;
    notes?: string;
  }) {
    const codes = Array.from({ length: data.count }, () => ({
      code: this.generateCode(),
      planId: data.planId,
      billingCycle: data.billingCycle as any,
      maxUses: data.maxUses,
      countryRestriction: data.countryRestriction ?? null,
      expiresAt: data.expiresAt ? new Date(data.expiresAt) : null,
      notes: data.notes ?? null,
      isActive: true,
      usedCount: 0,
    }));

    await this.prisma.activationCode.createMany({ data: codes });
    return { created: codes.length, codes: codes.map(c => c.code) };
  }

  async deactivateCode(id: string) {
    const code = await this.prisma.activationCode.findUnique({ where: { id } });
    if (!code) throw new NotFoundException('Activation code not found');
    return this.prisma.activationCode.update({ where: { id }, data: { isActive: false } });
  }

  async deleteCode(id: string) {
    const code = await this.prisma.activationCode.findUnique({ where: { id } });
    if (!code) throw new NotFoundException('Activation code not found');
    return this.prisma.activationCode.delete({ where: { id } });
  }

  private generateCode(): string {
    const seg = () => crypto.randomBytes(3).toString('hex').toUpperCase();
    return `OPX-${seg()}-${seg()}-${seg()}`;
  }

  // ─────────────────────────────────────────────────────────────────────────
  // AUDIT LOGS
  // ─────────────────────────────────────────────────────────────────────────

  async getAdminAuditLogs(filters: {
    businessId?: string;
    action?: string;
    entity?: string;
    page?: number;
    limit?: number;
  }) {
    const page = Number(filters.page) || 1;
    const limit = Number(filters.limit) || 50;
    const skip = (page - 1) * limit;

    const where: any = {};
    if (filters.businessId) where.businessId = filters.businessId;
    if (filters.action) where.action = { contains: filters.action, mode: 'insensitive' };
    if (filters.entity) where.entity = filters.entity;

    const [total, items] = await Promise.all([
      this.prisma.auditLog.count({ where }),
      this.prisma.auditLog.findMany({
        where,
        include: { business: { select: { name: true } } },
        orderBy: { createdAt: 'desc' },
        skip,
        take: limit,
      }),
    ]);

    return { items, meta: { total, page, lastPage: Math.ceil(total / limit) } };
  }

  // ─────────────────────────────────────────────────────────────────────────
  // REVENUE STATS
  // ─────────────────────────────────────────────────────────────────────────

  async getRevenueStats() {
    const now = new Date();
    const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);
    const yearStart = new Date(now.getFullYear(), 0, 1);

    const [todayRev, monthRev, yearRev, totalRev, activeCount, failedCount, refundedCount, planBreakdown] = await Promise.all([
      this.prisma.paymentTransaction.aggregate({ where: { status: 'CAPTURED', createdAt: { gte: todayStart } }, _sum: { amount: true }, _count: true }),
      this.prisma.paymentTransaction.aggregate({ where: { status: 'CAPTURED', createdAt: { gte: monthStart } }, _sum: { amount: true }, _count: true }),
      this.prisma.paymentTransaction.aggregate({ where: { status: 'CAPTURED', createdAt: { gte: yearStart } }, _sum: { amount: true }, _count: true }),
      this.prisma.paymentTransaction.aggregate({ where: { status: 'CAPTURED' }, _sum: { amount: true }, _count: true }),
      this.prisma.subscription.count({ where: { status: 'ACTIVE' } }),
      this.prisma.paymentTransaction.count({ where: { status: 'FAILED' } }),
      this.prisma.paymentTransaction.count({ where: { status: 'REFUNDED' } }),
      this.prisma.paymentTransaction.groupBy({ by: ['planId', 'billingCycle'], where: { status: 'CAPTURED' }, _count: true, _sum: { amount: true } }),
    ]);

    const mrr = Number(monthRev._sum.amount || 0);
    const arr = mrr * 12;
    const avgRevPerBusiness = activeCount > 0 ? mrr / activeCount : 0;

    return {
      today: { revenue: Number(todayRev._sum.amount || 0), count: todayRev._count },
      month: { revenue: Number(monthRev._sum.amount || 0), count: monthRev._count },
      year: { revenue: Number(yearRev._sum.amount || 0), count: yearRev._count },
      total: { revenue: Number(totalRev._sum.amount || 0), count: totalRev._count },
      mrr,
      arr,
      avgRevPerBusiness,
      activeSubscriptions: activeCount,
      failedPayments: failedCount,
      refunds: refundedCount,
      planBreakdown,
    };
  }

  async getDashboardStats() {
    const totalPayments = await this.prisma.paymentTransaction.aggregate({
      where: { status: 'CAPTURED' },
      _sum: { amount: true },
    });
    const activeSubscriptions = await this.prisma.subscription.count({ where: { status: 'ACTIVE' } });
    return { totalRevenue: totalPayments._sum.amount || 0, activeSubscriptions };
  }

  // ─────────────────────────────────────────────────────────────────────────
  // PRIVATE HELPERS
  // ─────────────────────────────────────────────────────────────────────────

  private async writeAdminAuditLog(
    businessId: string,
    action: string,
    entity: string,
    entityId: string,
    oldValue: any,
    newValue: any,
  ) {
    try {
      await this.prisma.auditLog.create({
        data: {
          businessId,
          action,
          entity,
          entityId,
          oldValue,
          newValue,
          endpoint: 'SUPER_ADMIN',
        },
      });
    } catch (e) {
      // Non-blocking — audit log failure should not break the operation
    }
  }
}
