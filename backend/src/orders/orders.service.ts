import { Injectable, Logger } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';

@Injectable()
export class OrdersService {
  private readonly logger = new Logger(OrdersService.name);

  constructor(
    private prisma: PrismaService,
    private syncGateway: SyncGateway,
  ) {}

  async getOrders(businessId: string) {
    return this.prisma.order.findMany({
      where: { businessId },
      include: { items: true },
      orderBy: { createdAt: 'desc' },
    });
  }

  async saveOrder(businessId: string, dto: any) {
    this.logger.log(`[BACKEND ORDER RECEIVED] Business ID: ${businessId}, Temp Token: ${dto.tokenNumber}, Invoice: ${dto.invoiceNumber}`);
    const savedOrder = await this.prisma.$transaction(async (tx) => {
      const business = await tx.business.findUnique({
        where: { id: businessId },
        select: { tokenCounter: true, lastTokenResetDateTime: true },
      });

      const now = new Date();
      let newTokenCounter = (business?.tokenCounter || 0) + 1;

      if (business?.lastTokenResetDateTime) {
        const lastReset = new Date(business.lastTokenResetDateTime);
        if (
          now.getFullYear() !== lastReset.getFullYear() ||
          now.getMonth() !== lastReset.getMonth() ||
          now.getDate() !== lastReset.getDate()
        ) {
          newTokenCounter = 1;
        }
      }

      await tx.business.update({
        where: { id: businessId },
        data: {
          tokenCounter: newTokenCounter,
          lastTokenResetDateTime: now,
        },
      });

      const generatedTokenNumber = String(newTokenCounter).padStart(3, '0');

      const pmInput = (dto.paymentMethod || 'CASH').toUpperCase();
      const validPaymentMethod = ['CASH', 'CARD', 'UPI', 'NET_BANKING', 'OTHER', 'CREDIT'].includes(pmInput)
        ? pmInput
        : 'CASH';

      const orderData = {
        tokenNumber: generatedTokenNumber,
        invoiceNumber: dto.invoiceNumber || `INV-${Date.now()}`,
        status: dto.status || 'PAID',
        subtotal: dto.subtotal || dto.total,
        discount: dto.discount || 0,
        tax: dto.tax || 0,
        total: dto.total,
        paymentMethod: validPaymentMethod as any,
        cashierName: dto.cashierName || 'Admin',
        customerName: dto.customerName,
        customerId: dto.customerId,
      };

      if (dto.id) {
        return tx.order.upsert({
          where: { id: dto.id },
          update: orderData,
          create: {
            id: dto.id,
            businessId,
            ...orderData,
            items: {
              create: dto.items?.map((item: any) => ({
                productId: item.productId || item.id || require('crypto').randomUUID(),
                productName: item.name || item.productName || 'Item',
                price: item.price,
                quantity: item.quantity,
                weight: item.weight,
                unit: item.unit,
              })),
            },
          },
          include: { items: true },
        });
      }

      const res = await tx.order.create({
        data: {
          businessId,
          ...orderData,
          items: {
            create: dto.items?.map((item: any) => ({
              productId: item.productId || item.id || require('crypto').randomUUID(),
              productName: item.name || item.productName || 'Item',
              price: item.price,
              quantity: item.quantity,
              weight: item.weight,
              unit: item.unit,
            })),
          },
        },
        include: { items: true },
      });
      this.logger.log(`[BACKEND OFFICIAL TOKEN ASSIGNED] Temp Token: ${dto.tokenNumber} -> Official Atomic Token: ${res.tokenNumber}`);
      return res;
    });

    this.syncGateway.emitToBusiness(businessId, 'order.created', savedOrder);

    // Create Activity Log
    try {
      const act = await this.prisma.staffActivityLog.create({
        data: {
          staffId: dto.staffId || undefined,
          businessId,
          action: 'BILL_CREATED',
          entityType: 'ORDER',
          entityId: savedOrder.id,
          metadata: { total: savedOrder.total, token: savedOrder.tokenNumber },
          severity: 'NORMAL',
        },
      });
      this.syncGateway.emitToBusiness(businessId, 'staff.activity.created', act);
    } catch (e) {}

    return savedOrder;
  }

  async deleteOrder(businessId: string, id: string) {
    const res = await this.prisma.order.deleteMany({
      where: { id, businessId },
    });
    this.syncGateway.emitToBusiness(businessId, 'order.deleted', { id });

    try {
      const act = await this.prisma.staffActivityLog.create({
        data: {
          staffId: undefined,
          businessId,
          action: 'BILL_CANCELLED',
          entityType: 'ORDER',
          entityId: id,
          isSuspicious: true,
          severity: 'WARNING',
        },
      });
      this.syncGateway.emitToBusiness(businessId, 'staff.activity.created', act);
    } catch (e) {}

    return res;
  }
}
