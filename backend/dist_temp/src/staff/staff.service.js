"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.StaffService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
const sync_gateway_1 = require("../sync/sync.gateway");
const argon = __importStar(require("@node-rs/argon2"));
const SUSPICIOUS_FAILED_LOGIN_THRESHOLD = 5;
let StaffService = class StaffService {
    constructor(prisma, syncGateway) {
        this.prisma = prisma;
        this.syncGateway = syncGateway;
    }
    async getStaff(businessId) {
        const staffList = await this.prisma.staff.findMany({
            where: { businessId },
            include: { permissions: true },
            orderBy: { createdAt: 'desc' },
        });
        return staffList.map((s) => this.formatStaff(s));
    }
    async getStaffById(businessId, id) {
        const staff = await this.prisma.staff.findFirst({
            where: { id, businessId },
            include: { permissions: true },
        });
        if (!staff)
            throw new common_1.NotFoundException('Staff member not found');
        return this.formatStaff(staff);
    }
    async saveStaff(businessId, dto, senderSocketId) {
        const passwordHash = dto.password ? await argon.hash(dto.password) : undefined;
        const existingByUsername = await this.prisma.staff.findUnique({
            where: { username: dto.username },
        });
        const targetId = dto.id || existingByUsername?.id;
        const isUpdate = !!targetId;
        let staff;
        const roleUpper = (dto.role ? String(dto.role).toUpperCase() : 'STAFF');
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
        }
        else {
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
        if (dto.permissions && Array.isArray(dto.permissions)) {
            await this.prisma.permission.deleteMany({ where: { staffId: staff.id } });
            if (dto.permissions.length > 0) {
                await this.prisma.permission.createMany({
                    data: dto.permissions.map((action) => ({
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
        await this.prisma.auditLog.create({
            data: {
                businessId,
                action: isUpdate ? 'UPDATE_STAFF' : 'CREATE_STAFF',
                entity: 'STAFF',
                entityId: staff.id,
                newValue: { name: staff.name, role: staff.role, username: staff.username },
            },
        });
        const formatted = this.formatStaff(staff);
        this.syncGateway.emitToBusiness(businessId, isUpdate ? 'staff.updated' : 'staff.created', formatted, senderSocketId);
        return formatted;
    }
    async deleteStaff(businessId, id, senderSocketId) {
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
    async disableStaff(businessId, id, senderSocketId) {
        const staff = await this.prisma.staff.findFirst({ where: { id, businessId } });
        if (!staff)
            throw new common_1.NotFoundException('Staff member not found');
        await this.prisma.staff.update({ where: { id }, data: { isDisabled: true, lastActivityAt: new Date() } });
        await this.prisma.auditLog.create({
            data: { businessId, action: 'DISABLE_STAFF', entity: 'STAFF', entityId: id },
        });
        this.syncGateway.emitToBusiness(businessId, 'staff.disabled', { id }, senderSocketId);
        return { success: true, id, isDisabled: true };
    }
    async enableStaff(businessId, id, senderSocketId) {
        const staff = await this.prisma.staff.findFirst({ where: { id, businessId } });
        if (!staff)
            throw new common_1.NotFoundException('Staff member not found');
        await this.prisma.staff.update({ where: { id }, data: { isDisabled: false, lastActivityAt: new Date() } });
        await this.prisma.auditLog.create({
            data: { businessId, action: 'ENABLE_STAFF', entity: 'STAFF', entityId: id },
        });
        this.syncGateway.emitToBusiness(businessId, 'staff.enabled', { id }, senderSocketId);
        return { success: true, id, isDisabled: false };
    }
    async updatePermissions(businessId, staffId, permissions, senderSocketId) {
        const staff = await this.prisma.staff.findFirst({ where: { id: staffId, businessId } });
        if (!staff)
            throw new common_1.NotFoundException('Staff member not found');
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
        this.syncGateway.emitToBusiness(businessId, 'staff.permissions.updated', { id: staffId, permissions }, senderSocketId);
        return { success: true, staffId, permissions };
    }
    async getActivityLogs(businessId, staffId, limit = 100) {
        return this.prisma.staffActivityLog.findMany({
            where: { businessId, ...(staffId && { staffId }) },
            orderBy: { createdAt: 'desc' },
            take: limit,
            include: { staff: { select: { name: true, username: true, role: true } } },
        });
    }
    async recordActivity(staffId, businessId, action, entityType, entityId, metadata, deviceId, isSuspicious = false) {
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
    async getSessions(businessId, staffId) {
        return this.prisma.staffSession.findMany({
            where: { businessId, ...(staffId && { staffId }) },
            orderBy: { loginAt: 'desc' },
            take: 50,
            include: { staff: { select: { name: true, username: true, role: true } } },
        });
    }
    async openSession(staffId, businessId, deviceId, deviceName) {
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
    async closeSession(staffId, deviceId) {
        return this.prisma.staffSession.updateMany({
            where: { staffId, isActive: true, ...(deviceId && { deviceId }) },
            data: { isActive: false, logoutAt: new Date() },
        });
    }
    async terminateSession(businessId, sessionId, senderSocketId) {
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
    async getNotifications(businessId) {
        return this.prisma.notification.findMany({
            where: { businessId, isArchived: false },
            orderBy: { createdAt: 'desc' },
            take: 50,
        });
    }
    async markNotificationsRead(businessId) {
        await this.prisma.notification.updateMany({
            where: { businessId, read: false },
            data: { read: true },
        });
        return { success: true };
    }
    async createNotification(businessId, title, message, type, severity = 'INFO', metadata) {
        const notif = await this.prisma.notification.create({
            data: { businessId, title, message, type, severity, metadata },
        });
        this.syncGateway.emitToBusiness(businessId, 'staff.notification.created', notif);
        return notif;
    }
    async handleFailedLogin(staffId, businessId) {
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
    async resetFailedLoginCount(staffId) {
        await this.prisma.staff.update({
            where: { id: staffId },
            data: { failedLoginCount: 0, lastActivityAt: new Date() },
        });
    }
    formatStaff(staff) {
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
            permissions: (staff.permissions ?? []).map((p) => p.action),
        };
    }
};
exports.StaffService = StaffService;
exports.StaffService = StaffService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        sync_gateway_1.SyncGateway])
], StaffService);
//# sourceMappingURL=staff.service.js.map