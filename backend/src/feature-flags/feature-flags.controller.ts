import { Controller, Get, UseGuards, Request } from '@nestjs/common';
import { SuperAdminService } from '../super-admin/super-admin.service';
import { AtGuard } from '../auth/guards/at.guard';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';

@ApiTags('Feature Flags')
@Controller('feature-flags')
export class FeatureFlagsController {
  constructor(private readonly adminService: SuperAdminService) {}

  @Get('effective')
  @UseGuards(AtGuard)
  @ApiBearerAuth()
  @ApiOperation({ summary: 'Get effective resolved feature flags for the authenticated business' })
  async getEffectiveFlags(@Request() req: any) {
    const businessId = req.user?.businessId;
    return this.adminService.getEffectiveFeatureFlags(businessId);
  }

  @Get('public-config')
  @ApiOperation({ summary: 'Get public global config (maintenance mode, min version)' })
  async getPublicConfig() {
    const cfg = await this.adminService.getGlobalConfig();
    return {
      maintenanceMode: cfg.maintenanceMode,
      maintenanceMessage: cfg.maintenanceMessage,
      minSupportedAppVersion: cfg.minSupportedAppVersion,
      latestStableVersion: cfg.latestStableVersion,
      forceUpdate: cfg.forceUpdate,
      supportEmail: cfg.supportEmail,
      supportPhone: cfg.supportPhone,
      supportWhatsApp: cfg.supportWhatsApp,
    };
  }
}
