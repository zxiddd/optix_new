import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';
import * as argon from '@node-rs/argon2';

const SUSPICIOUS_FAILED_LOGIN_THRESHOLD = 5;

@Injectable()
export class StaffService {
  constructor(
    private prisma: PrismaService,
    private syncGateway: SyncGateway,
  ) {}

  // ─── READ ────────────────────────────────────────────────────────────────────

  async getStaff(businessId: string) {
    const staffList = await this.prisma.staff.findMany({
      where: { businessId },
      include: { permissions: true },
      orderBy: { createdAt: 'desc' },
    });

    return staffList.map((s) => this.formatStaff(s));
  }

  async getStaffById(businessId: string, id: string) {
    const staff = await this.prisma.staff.findFirst({
      where: { id, businessId },
      include: { permissions: true },
    });
    if (!staff) throw new NotFoundException('Staff member not found');
    return this.formatStaff(staff);
  }

  // ─── CREATE / UPDATE ─────────────────────────────────────────────────────────

  async saveStaff(businessId: string, dto: any, senderSocketId?: string) {
    const passwordHash = dto.password ? await argon.hash(dto.password) : undefined;

    const existingByUsername = await this.prisma.staff.findUnique({
      where: { username: dto.username },
    });

    const targetId = dto.id || existingByUsername?.id;
    const isUpdate = !!targetId;

    let staff: any;

    const roleUpper = (dto.role ? String(dto.role).toUpperCase() : 'STAFF') as any;

    if (isUpdate) {
      staff = await this.prisma.staff.upsert({
        where: { id: targetId },
        update: {
          name: dto.name,
          username: dto.username,
          role: roleUpper,
          phone: dto.phone ?? undefined,
          email: dto.email ?? undefined,
          isDisabled: dto.isDisabled ?? false,
          ...(passwordHash && { password: passwordHash }),
          lastActivityAt: new Date(),
        },
        create: {
          id: targetId,
          businessId,
          name: dto.name,
          username: dto.username,
          role: roleUpper,
          phone: dto.phone ?? undefined,
          email: dto.email ?? undefined,
          isDisabled: dto.isDisabled ?? false,
          password: passwordHash || (await argon.hash('123456')),
        },
        include: { permissions: true },
      });
    } else {
      staff = await this.prisma.staff.create({
        data: {
          businessId,
          name: dto.name,
          username: dto.username,
          role: roleUpper,
          phone: dto.phone ?? undefined,
          email: dto.email ?? undefined,
          isDisabled: dto.isDisabled ?? false,
          password: passwordHash || (await argon.hash('123456')),
        },
        include: { permissions: true },
      });
    }

    // Sync permissions if provided
    if (dto.permissions && Array.isArray(dto.permissions)) {
      await this.prisma.permission.deleteMany({ where: { staffId: staff.id } });
      if (dto.permissions.length > 0) {
        await this.prisma.permission.createMany({
          data: dto.permissions.map((action: string) => ({
            staffId: staff.id,
            action,
          })),
        });
      }
      staff = await this.prisma.staff.findUnique({
        where: { id: staff.id },
        include: { permissions: true },
      });
    }

    // Audit log
    await this.prisma.auditLog.create({
      data: {
        businessId,
        action: isUpdate ? 'UPDATE_STAFF' : 'CREATE_STAFF',
        entity: 'STAFF',
        entityId: staff.id,
        newValue: { name: staff.name, role: staff.role, username: staff.username },
      },
    });

    // WebSocket broadcast (echo-suppressed)
    const formatted = this.formatStaff(staff);
    this.syncGateway.emitToBusiness(
      businessId,
      isUpdate ? 'staff.updated' : 'staff.created',
      formatted,
      senderSocketId,
    );

    return formatted;
  }

  // ─── DELETE ──────────────────────────────────────────────────────────────────

  async deleteStaff(businessId: string, id: string, senderSocketId?: string) {
    await this.prisma.permission.deleteMany({ where: { staffId: id } });
    await this.prisma.staffActivityLog.deleteMany({ where: { staffId: id } });
    await this.prisma.staffSession.deleteMany({ where: { staffId: id } });
    await this.prisma.staff.deleteMany({ where: { id, businessId } });

    await this.prisma.auditLog.create({
      data: {
        businessId,
        action: 'DELETE_STAFF',
        entity: 'STAFF',
        entityId: id,
      },
    });

    this.syncGateway.emitToBusiness(businessId, 'staff.deleted', { id }, senderSocketId);
    return { success: true, id };
  }

  // ─── DISABLE / ENABLE ────────────────────────────────────────────────────────

  async disableStaff(businessId: string, id: string, senderSocketId?: string) {
    const staff = await this.prisma.staff.findFirst({ where: { id, businessId } });
    if (!staff) throw new NotFoundException('Staff member not found');

    await this.prisma.staff.update({ where: { id }, data: { isDisabled: true, lastActivityAt: new Date() } });

    await this.prisma.auditLog.create({
      data: { businessId, action: 'DISABLE_STAFF', entity: 'STAFF', entityId: id },
    });

    this.syncGateway.emitToBusiness(businessId, 'staff.disabled', { id }, senderSocketId);
    return { success: true, id, isDisabled: true };
  }

  async enableStaff(businessId: string, id: string, senderSocketId?: string) {
    const staff = await this.prisma.staff.findFirst({ where: { id, businessId } });
    if (!staff) throw new NotFoundException('Staff member not found');

    await this.prisma.staff.update({ where: { id }, data: { isDisabled: false, lastActivityAt: new Date() } });

    await this.prisma.auditLog.create({
      data: { businessId, action: 'ENABLE_STAFF', entity: 'STAFF', entityId: id },
    });

    this.syncGateway.emitToBusiness(businessId, 'staff.enabled', { id }, senderSocketId);
    return { success: true, id, isDisabled: false };
  }

  // ─── PERMISSIONS ─────────────────────────────────────────────────────────────

  async updatePermissions(businessId: string, staffId: string, permissions: string[], senderSocketId?: string) {
    const staff = await this.prisma.staff.findFirst({ where: { id: staffId, businessId } });
    if (!staff) throw new NotFoundException('Staff member not found');

    await this.prisma.permission.deleteMany({ where: { staffId } });
    if (permissions.length > 0) {
      await this.prisma.permission.createMany({
        data: permissions.map((action) => ({ staffId, action })),
      });
    }

    await this.prisma.auditLog.create({
      data: {
        businessId,
        action: 'UPDATE_PERMISSIONS',
        entity: 'STAFF',
        entityId: staffId,
        newValue: { permissions },
      },
    });

    this.syncGateway.emitToBusiness(
      businessId,
      'staff.permissions.updated',
      { id: staffId, permissions },
      senderSocketId,
    );

    return { success: true, staffId, permissions };
  }

  // ─── ACTIVITY LOGS ───────────────────────────────────────────────────────────

  async getActivityLogs(businessId: string, staffId?: string, limit = 100) {
    return this.prisma.staffActivityLog.findMany({
      where: { businessId, ...(staffId && { staffId }) },
      orderBy: { createdAt: 'desc' },
      take: limit,
      include: { staff: { select: { name: true, username: true, role: true } } },
    });
  }

  async recordActivity(
    staffId: string,
    businessId: string,
    action: string,
    entityType?: string,
    entityId?: string,
    metadata?: any,
    deviceId?: string,
    isSuspicious = false,
  ) {
    const log = await this.prisma.staffActivityLog.create({
      data: { staffId, businessId, action, entityType, entityId, metadata, deviceId, isSuspicious },
    });

    if (isSuspicious) {
      this.syncGateway.emitToBusiness(businessId, 'staff.suspicious_activity', {
        staffId,
        action,
        entityType,
        entityId,
        isSuspicious: true,
        timestamp: log.createdAt,
      });
    }

    return log;
  }

  // ─── SESSIONS ────────────────────────────────────────────────────────────────

  async getSessions(businessId: string, staffId?: string) {
    return this.prisma.staffSession.findMany({
      where: { businessId, ...(staffId && { staffId }) },
      orderBy: { loginAt: 'desc' },
      take: 50,
      include: { staff: { select: { name: true, username: true, role: true } } },
    });
  }

  async openSession(staffId: string, businessId: string, deviceId?: string, deviceName?: string) {
    await this.prisma.staffSession.updateMany({
      where: { staffId, isActive: true, ...(deviceId && { deviceId }) },
      data: { isActive: false, logoutAt: new Date() },
    });

    const session = await this.prisma.staffSession.create({
      data: { staffId, businessId, deviceId, deviceName, isActive: true },
    });

    this.syncGateway.emitToBusiness(businessId, 'staff.session.started', session);
    return session;
  }

  async closeSession(staffId: string, deviceId?: string) {
    return this.prisma.staffSession.updateMany({
      where: { staffId, isActive: true, ...(deviceId && { deviceId }) },
      data: { isActive: false, logoutAt: new Date() },
    });
  }

  async terminateSession(businessId: string, sessionId: string, senderSocketId?: string) {
    const session = await this.prisma.staffSession.update({
      where: { id: sessionId },
      data: { isActive: false, logoutAt: new Date() },
    });

    this.syncGateway.emitToBusiness(businessId, 'staff.session.ended', {
      sessionId: session.id,
      staffId: session.staffId,
      terminatedRemotely: true,
    }, senderSocketId);

    return { success: true, sessionId };
  }

  // ─── NOTIFICATIONS ───────────────────────────────────────────────────────────

  async getNotifications(businessId: string) {
    return this.prisma.notification.findMany({
      where: { businessId, isArchived: false },
      orderBy: { createdAt: 'desc' },
      take: 50,
    });
  }

  async markNotificationsRead(businessId: string) {
    await this.prisma.notification.updateMany({
      where: { businessId, read: false },
      data: { read: true },
    });
    return { success: true };
  }

  async createNotification(
    businessId: string,
    title: string,
    message: string,
    type: string,
    severity = 'INFO',
    metadata?: any,
  ) {
    const notif = await this.prisma.notification.create({
      data: { businessId, title, message, type, severity, metadata },
    });

    this.syncGateway.emitToBusiness(businessId, 'staff.notification.created', notif);
    return notif;
  }

  // ─── STAFF AUTH SUPPORT ──────────────────────────────────────────────────────

  async handleFailedLogin(staffId: string, businessId: string) {
    const staff = await this.prisma.staff.update({
      where: { id: staffId },
      data: {
        failedLoginCount: { increment: 1 },
        lastFailedLoginAt: new Date(),
      },
    });

    if (staff.failedLoginCount >= SUSPICIOUS_FAILED_LOGIN_THRESHOLD) {
      await this.recordActivity(staffId, businessId, 'SUSPICIOUS_FAILED_LOGINS', 'AUTH', undefined, {
        failedCount: staff.failedLoginCount,
      }, undefined, true);
    }
  }

  async resetFailedLoginCount(staffId: string) {
    await this.prisma.staff.update({
      where: { id: staffId },
      data: { failedLoginCount: 0, lastActivityAt: new Date() },
    });
  }

  // ─── INTERNAL HELPERS ────────────────────────────────────────────────────────

  private formatStaff(staff: any) {
    return {
      id: staff.id,
      name: staff.name,
      username: staff.username,
      role: staff.role,
      phone: staff.phone || null,
      email: staff.email || null,
      businessId: staff.businessId,
      isDisabled: staff.isDisabled,
      failedLoginCount: staff.failedLoginCount ?? 0,
      lastActivityAt: staff.lastActivityAt,
      createdAt: staff.createdAt,
      updatedAt: staff.updatedAt,
      permissions: (staff.permissions ?? []).map((p: any) => p.action),
    };
  }
}
