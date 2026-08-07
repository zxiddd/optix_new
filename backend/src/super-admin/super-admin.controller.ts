import { Controller, Get, Query, UseGuards } from '@nestjs/common';
import { SuperAdminService } from './super-admin.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';

@ApiTags('Super Admin')
@Controller('super-admin')
export class SuperAdminController {
  constructor(private readonly adminService: SuperAdminService) {}

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard) // Should also use a RoleGuard for 'SUPER_ADMIN'
  @Get('payments')
  @ApiOperation({ summary: 'Get all payment transactions' })
  async getPayments(
    @Query('status') status?: string,
    @Query('businessId') businessId?: string,
  ) {
    return this.adminService.getAllPayments({ status, businessId });
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Get('stats')
  @ApiOperation({ summary: 'Get SaaS dashboard stats' })
  async getStats() {
    return this.adminService.getDashboardStats();
  }
}
