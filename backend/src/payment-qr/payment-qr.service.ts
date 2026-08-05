import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';

@Injectable()
export class PaymentQrService {
  constructor(
    private prisma: PrismaService,
    private syncGateway: SyncGateway,
  ) {}

  async getPaymentQrs(businessId: string) {
    return this.prisma.paymentQr.findMany({
      where: { businessId },
      orderBy: { createdAt: 'desc' },
    });
  }

  async savePaymentQr(businessId: string, dto: any, senderSocketId?: string) {
    const isNew = !dto.id || !(await this.prisma.paymentQr.findUnique({ where: { id: dto.id } }));

    if (dto.isActive) {
      await this.prisma.paymentQr.updateMany({
        where: { businessId },
        data: { isActive: false },
      });
    }

    const qr = await this.prisma.paymentQr.upsert({
      where: { id: dto.id || 'new-id' },
      create: {
        id: dto.id,
        businessId,
        name: dto.name || 'UPI QR',
        upiId: dto.upiId,
        imageUrl: dto.imageUrl || dto.imagePath,
        isActive: dto.isActive ?? true,
      },
      update: {
        name: dto.name,
        upiId: dto.upiId,
        imageUrl: dto.imageUrl || dto.imagePath,
        isActive: dto.isActive,
      },
    });

    const eventName = isNew ? 'paymentQr.created' : 'paymentQr.updated';
    this.syncGateway.emitToBusiness(businessId, eventName, qr, senderSocketId);

    if (dto.isActive) {
      this.syncGateway.emitToBusiness(businessId, 'paymentQr.selected', { id: qr.id, businessId }, senderSocketId);
    }

    return qr;
  }

  async selectPaymentQr(businessId: string, id: string, senderSocketId?: string) {
    await this.prisma.paymentQr.updateMany({
      where: { businessId },
      data: { isActive: false },
    });

    const selected = await this.prisma.paymentQr.update({
      where: { id },
      data: { isActive: true },
    });

    this.syncGateway.emitToBusiness(businessId, 'paymentQr.selected', { id, businessId }, senderSocketId);
    return selected;
  }

  async deletePaymentQr(businessId: string, id: string, senderSocketId?: string) {
    await this.prisma.paymentQr.deleteMany({
      where: { id, businessId },
    });

    this.syncGateway.emitToBusiness(businessId, 'paymentQr.deleted', { id }, senderSocketId);
    return { success: true, id };
  }
}
