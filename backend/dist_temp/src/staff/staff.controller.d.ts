import { StaffService } from './staff.service';
export declare class StaffController {
    private staffService;
    constructor(staffService: StaffService);
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
    getActivityLogs(businessId: string, staffId?: string, limit?: string): Promise<({
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
    saveStaff(businessId: string, body: any, socketId?: string): Promise<{
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
    disableStaff(businessId: string, id: string, socketId?: string): Promise<{
        success: boolean;
        id: string;
        isDisabled: boolean;
    }>;
    enableStaff(businessId: string, id: string, socketId?: string): Promise<{
        success: boolean;
        id: string;
        isDisabled: boolean;
    }>;
    updatePermissions(businessId: string, id: string, permissions: string[], socketId?: string): Promise<{
        success: boolean;
        staffId: string;
        permissions: string[];
    }>;
    terminateSession(businessId: string, id: string, socketId?: string): Promise<{
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
    deleteStaff(businessId: string, id: string, socketId?: string): Promise<{
        success: boolean;
        id: string;
    }>;
}
