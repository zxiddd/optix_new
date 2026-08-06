import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';
import { BusinessClockService } from './business-clock.service';

@Injectable()
export class BusinessResetService {
  constructor(
    private prisma: PrismaService,
    private syncGateway: SyncGateway,
    private businessClockService: BusinessClockService,
  ) {}

  async resetBusinessDay(businessId: string, targetBusinessDate?: string, senderSocketId?: string) {
    const business = await this.prisma.business.findUnique({
      where: { id: businessId },
      include: { settings: true },
    });

    if (!business) {
      throw new NotFoundException('Business profile not found');
    }

    const settings = business.settings;
    const computedStatus = this.businessClockService.calculateStatus(
      settings?.openingTime || '09:00',
      settings?.closingTime || '22:00',
      settings?.timezone || 'Asia/Riyadh',
    );

    const businessDateToReset = targetBusinessDate || computedStatus.businessDate;

    // Idempotency Check: Exactly Once Guarantee
    if (settings?.lastResetBusinessDate === businessDateToReset) {
      return {
        alreadyReset: true,
        businessId,
        lastResetBusinessDate: settings.lastResetBusinessDate,
        tokenCounter: business.tokenCounter,
      };
    }

    // Perform Reset: Reset tokenCounter to 0 and update lastResetBusinessDate
    const updatedSettings = await this.prisma.businessSettings.upsert({
      where: { businessId },
      update: { lastResetBusinessDate: businessDateToReset },
      create: { businessId, lastResetBusinessDate: businessDateToReset },
    });

    const updatedBusiness = await this.prisma.business.update({
      where: { id: businessId },
      data: { tokenCounter: 0, lastTokenResetDateTime: new Date() },
    });

    const payload = {
      businessId,
      lastResetBusinessDate: updatedSettings.lastResetBusinessDate,
      tokenCounter: updatedBusiness.tokenCounter,
      resetAt: updatedBusiness.lastTokenResetDateTime.toISOString(),
      alreadyReset: false,
    };

    // Broadcast reset event to all connected devices in business room
    this.syncGateway.emitToBusiness(businessId, 'business.reset', payload, senderSocketId);

    return payload;
  }
}
