import { SuperAdminService } from '../super-admin/super-admin.service';
export declare class FeatureFlagsController {
    private readonly adminService;
    constructor(adminService: SuperAdminService);
    getEffectiveFlags(req: any): Promise<Record<string, string>>;
    getPublicConfig(): Promise<{
        maintenanceMode: boolean;
        maintenanceMessage: string;
        minSupportedAppVersion: string;
        latestStableVersion: string;
        forceUpdate: boolean;
        supportEmail: string;
        supportPhone: string;
        supportWhatsApp: string;
    }>;
}
