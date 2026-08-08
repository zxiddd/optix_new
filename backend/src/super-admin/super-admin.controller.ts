import {
  Controller, Get, Query, UseGuards, Param, Patch, Body,
  Post, Delete, HttpCode, HttpStatus
} from '@nestjs/common';
import { SuperAdminService } from './super-admin.service';
import { AtGuard } from '../auth/guards/at.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { UserRole } from '@prisma/client';

@ApiTags('Super Admin')
@Controller('super-admin')
@UseGuards(AtGuard, RolesGuard)
@Roles(UserRole.SUPER_ADMIN)
@ApiBearerAuth()
export class SuperAdminController {
  constructor(private readonly adminService: SuperAdminService) {}

  // ─── BUSINESSES ────────────────────────────────────────────────────────────

  @Get('businesses')
  @ApiOperation({ summary: 'Get paginated list of businesses' })
  async getBusinesses(
    @Query('page') page?: number,
    @Query('limit') limit?: number,
    @Query('search') search?: string,
    @Query('planId') planId?: string,
    @Query('status') status?: string,
    @Query('country') country?: string,
  ) {
    return this.adminService.getBusinesses({ page, limit, search, planId, status, country });
  }

  @Get('businesses/:id')
  @ApiOperation({ summary: 'Get full details of a business' })
  async getBusinessDetail(@Param('id') id: string) {
    return this.adminService.getBusinessDetail(id);
  }

  @Patch('businesses/:id/status')
  @ApiOperation({ summary: 'Update business/subscription status' })
  async updateStatus(
    @Param('id') id: string,
    @Body('status') status: 'ACTIVE' | 'EXPIRED' | 'CANCELLED' | 'SUSPENDED',
  ) {
    return this.adminService.updateBusinessStatus(id, status);
  }

  @Post('businesses/:id/reset-trial')
  @ApiOperation({ summary: 'Reset trial limits for a business' })
  async resetTrial(@Param('id') id: string) {
    return this.adminService.resetTrialLimits(id);
  }

  // ─── PAYMENTS ──────────────────────────────────────────────────────────────

  @Get('payments')
  @ApiOperation({ summary: 'Get all payment transactions with full filters' })
  async getPayments(
    @Query('status') status?: string,
    @Query('businessId') businessId?: string,
    @Query('planId') planId?: string,
    @Query('country') country?: string,
    @Query('currency') currency?: string,
    @Query('dateFrom') dateFrom?: string,
    @Query('dateTo') dateTo?: string,
    @Query('page') page?: number,
    @Query('limit') limit?: number,
    @Query('search') search?: string,
  ) {
    return this.adminService.getAllPayments({ status, businessId, planId, country, currency, dateFrom, dateTo, page, limit, search });
  }

  @Get('payments/:id')
  @ApiOperation({ summary: 'Get single payment full details' })
  async getPaymentDetail(@Param('id') id: string) {
    return this.adminService.getPaymentDetail(id);
  }

  @Post('payments/:id/refund')
  @ApiOperation({ summary: 'Refund a payment' })
  @HttpCode(HttpStatus.OK)
  async refundPayment(
    @Param('id') id: string,
    @Body('reason') reason?: string,
    @Body('partial') partial?: number,
  ) {
    return this.adminService.refundPayment(id, reason, partial);
  }

  // ─── SUBSCRIPTIONS ─────────────────────────────────────────────────────────

  @Get('subscriptions')
  @ApiOperation({ summary: 'Get all subscriptions with filters' })
  async getSubscriptions(
    @Query('status') status?: string,
    @Query('planId') planId?: string,
    @Query('country') country?: string,
    @Query('page') page?: number,
    @Query('limit') limit?: number,
    @Query('search') search?: string,
  ) {
    return this.adminService.getAllSubscriptions({ status, planId, country, page, limit, search });
  }

  @Get('subscriptions/:id')
  @ApiOperation({ summary: 'Get single subscription with full history' })
  async getSubscriptionDetail(@Param('id') id: string) {
    return this.adminService.getSubscriptionDetail(id);
  }

  @Patch('subscriptions/:businessId/plan')
  @ApiOperation({ summary: 'Change plan (upgrade or downgrade)' })
  async changePlan(
    @Param('businessId') businessId: string,
    @Body('planId') planId: string,
    @Body('billingCycle') billingCycle: 'MONTHLY' | 'YEARLY',
  ) {
    return this.adminService.changePlan(businessId, planId, billingCycle);
  }

  @Patch('subscriptions/:businessId/extend')
  @ApiOperation({ summary: 'Extend subscription by N days' })
  async extendSubscription(
    @Param('businessId') businessId: string,
    @Body('days') days: number,
  ) {
    return this.adminService.extendSubscription(businessId, days);
  }

  @Patch('subscriptions/:businessId/status')
  @ApiOperation({ summary: 'Pause, Resume, Cancel, or Activate subscription' })
  async updateSubscriptionStatus(
    @Param('businessId') businessId: string,
    @Body('status') status: string,
  ) {
    return this.adminService.updateSubscriptionStatus(businessId, status);
  }

  @Post('subscriptions/:businessId/reset-trial')
  @ApiOperation({ summary: 'Reset trial usage counters' })
  async resetSubscriptionTrial(@Param('businessId') businessId: string) {
    return this.adminService.resetTrialLimits(businessId);
  }

  // ─── ACTIVATION CODES ──────────────────────────────────────────────────────

  @Get('activation-codes')
  @ApiOperation({ summary: 'List activation codes with filters' })
  async getActivationCodes(
    @Query('search') search?: string,
    @Query('planId') planId?: string,
    @Query('isActive') isActive?: string,
    @Query('page') page?: number,
    @Query('limit') limit?: number,
  ) {
    return this.adminService.getActivationCodes({ search, planId, isActive, page, limit });
  }

  @Post('activation-codes')
  @ApiOperation({ summary: 'Generate a single activation code' })
  async createActivationCode(
    @Body() body: {
      planId: string;
      billingCycle: 'MONTHLY' | 'YEARLY';
      maxUses: number;
      countryRestriction?: string;
      expiresAt?: string;
      notes?: string;
    },
  ) {
    return this.adminService.createActivationCode(body);
  }

  @Post('activation-codes/bulk')
  @ApiOperation({ summary: 'Bulk generate activation codes' })
  async bulkCreateActivationCodes(
    @Body() body: {
      count: number;
      planId: string;
      billingCycle: 'MONTHLY' | 'YEARLY';
      maxUses: number;
      countryRestriction?: string;
      expiresAt?: string;
      notes?: string;
    },
  ) {
    return this.adminService.bulkCreateActivationCodes(body);
  }

  @Patch('activation-codes/:id/deactivate')
  @ApiOperation({ summary: 'Deactivate an activation code' })
  async deactivateCode(@Param('id') id: string) {
    return this.adminService.deactivateCode(id);
  }

  @Delete('activation-codes/:id')
  @ApiOperation({ summary: 'Delete an activation code' })
  @HttpCode(HttpStatus.NO_CONTENT)
  async deleteCode(@Param('id') id: string) {
    return this.adminService.deleteCode(id);
  }

  // ─── AUDIT LOGS ────────────────────────────────────────────────────────────

  @Get('audit-logs')
  @ApiOperation({ summary: 'Get paginated admin audit logs' })
  async getAuditLogs(
    @Query('businessId') businessId?: string,
    @Query('action') action?: string,
    @Query('entity') entity?: string,
    @Query('page') page?: number,
    @Query('limit') limit?: number,
  ) {
    return this.adminService.getAdminAuditLogs({ businessId, action, entity, page, limit });
  }

  // ─── REVENUE & STATS ───────────────────────────────────────────────────────

  @Get('revenue-stats')
  @ApiOperation({ summary: 'Get detailed revenue statistics' })
  async getRevenueStats() {
    return this.adminService.getRevenueStats();
  }

  @Get('stats')
  @ApiOperation({ summary: 'Get SaaS dashboard stats' })
  async getStats() {
    return this.adminService.getDashboardStats();
  }
}
