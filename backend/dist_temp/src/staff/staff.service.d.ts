import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';
export declare class StaffService {
    private prisma;
    private syncGateway;
    constructor(prisma: PrismaService, syncGateway: SyncGateway);
    getStaff(businessId: string): Promise<{
        id: any;
        name: any;
        username: any;
        role: any;
        phone: any;
        email: any;
        businessId: any;
        isDisabled: any;
        failedLoginCount: any;
        lastActivityAt: any;
        createdAt: any;
        updatedAt: any;
        permissions: any;
    }[]>;
    getStaffById(businessId: string, id: string): Promise<{
        id: any;
        name: any;
        username: any;
        role: any;
        phone: any;
        email: any;
        businessId: any;
        isDisabled: any;
        failedLoginCount: any;
        lastActivityAt: any;
        createdAt: any;
        updatedAt: any;
        permissions: any;
    }>;
    saveStaff(businessId: string, dto: any, senderSocketId?: string): Promise<{
        id: any;
        name: any;
        username: any;
        role: any;
        phone: any;
        email: any;
        businessId: any;
        isDisabled: any;
        failedLoginCount: any;
        lastActivityAt: any;
        createdAt: any;
        updatedAt: any;
        permissions: any;
    }>;
    deleteStaff(businessId: string, id: string, senderSocketId?: string): Promise<{
        success: boolean;
        id: string;
    }>;
    disableStaff(businessId: string, id: string, senderSocketId?: string): Promise<{
        success: boolean;
        id: string;
        isDisabled: boolean;
    }>;
    enableStaff(businessId: string, id: string, senderSocketId?: string): Promise<{
        success: boolean;
        id: string;
        isDisabled: boolean;
    }>;
    updatePermissions(businessId: string, staffId: string, permissions: string[], senderSocketId?: string): Promise<{
        success: boolean;
        staffId: string;
        permissions: string[];
    }>;
    getActivityLogs(businessId: string, staffId?: string, limit?: number): Promise<({
        staff: {
            name: string;
            role: import(".prisma/client").$Enums.UserRole;
            username: string;
        };
    } & {
        id: string;
        createdAt: Date;
        businessId: string;
        action: string;
        entityType: string | null;
        entityId: string | null;
        metadata: import("@prisma/client/runtime/library").JsonValue | null;
        deviceId: string | null;
        ipAddress: string | null;
        isSuspicious: boolean;
        severity: string;
        staffId: string | null;
    })[]>;
    recordActivity(staffId: string, businessId: string, action: string, entityType?: string, entityId?: string, metadata?: any, deviceId?: string, isSuspicious?: boolean): Promise<{
        id: string;
        createdAt: Date;
        businessId: string;
        action: string;
        entityType: string | null;
        entityId: string | null;
        metadata: import("@prisma/client/runtime/library").JsonValue | null;
        deviceId: string | null;
        ipAddress: string | null;
        isSuspicious: boolean;
        severity: string;
        staffId: string | null;
    }>;
    getSessions(businessId: string, staffId?: string): Promise<({
        staff: {
            name: string;
            role: import(".prisma/client").$Enums.UserRole;
            username: string;
        };
    } & {
        id: string;
        businessId: string;
        deviceId: string | null;
        ipAddress: string | null;
        staffId: string;
        deviceName: string | null;
        loginAt: Date;
        logoutAt: Date | null;
        isActive: boolean;
    })[]>;
    openSession(staffId: string, businessId: string, deviceId?: string, deviceName?: string): Promise<{
        id: string;
        businessId: string;
        deviceId: string | null;
        ipAddress: string | null;
        staffId: string;
        deviceName: string | null;
        loginAt: Date;
        logoutAt: Date | null;
        isActive: boolean;
    }>;
    closeSession(staffId: string, deviceId?: string): Promise<import(".prisma/client").Prisma.BatchPayload>;
    terminateSession(businessId: string, sessionId: string, senderSocketId?: string): Promise<{
        success: boolean;
        sessionId: string;
    }>;
    getNotifications(businessId: string): Promise<{
        id: string;
        createdAt: Date;
        businessId: string;
        type: string;
        title: string;
        metadata: import("@prisma/client/runtime/library").JsonValue | null;
        severity: string;
        message: string;
        read: boolean;
        isArchived: boolean;
    }[]>;
    markNotificationsRead(businessId: string): Promise<{
        success: boolean;
    }>;
    createNotification(businessId: string, title: string, message: string, type: string, severity?: string, metadata?: any): Promise<{
        id: string;
        createdAt: Date;
        businessId: string;
        type: string;
        title: string;
        metadata: import("@prisma/client/runtime/library").JsonValue | null;
        severity: string;
        message: string;
        read: boolean;
        isArchived: boolean;
    }>;
    handleFailedLogin(staffId: string, businessId: string): Promise<void>;
    resetFailedLoginCount(staffId: string): Promise<void>;
    private formatStaff;
}
