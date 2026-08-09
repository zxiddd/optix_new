import {
  Controller, Get, Query, UseGuards, Param, Patch, Body,
  Post, Delete, HttpCode, HttpStatus
} from '@nestjs/common';
import { SuperAdminService } from './super-admin.service';
import { AtGuard } from '../auth/guards/at.guard';
import { RolesGuard } from '../auth/guards/roles.guard';
import { Roles } from '../auth/decorators/roles.decorator';
import { Public } from '../auth/decorators';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';
import { UserRole } from '@prisma/client';

import { InfraMonitoringService } from './infra-monitoring.service';

@ApiTags('Super Admin')
@Controller('super-admin')
@UseGuards(AtGuard, RolesGuard)
@Roles(UserRole.SUPER_ADMIN)
@ApiBearerAuth()
export class SuperAdminController {

  constructor(
    private readonly adminService: SuperAdminService,
    private readonly infraService: InfraMonitoringService,
  ) {}


  // ─── BUSINESSES ────────────────────────────────────────────────────────────

  @Public()
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

  @Public()
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

  @Public()
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

  @Public()
  @Get('payments/:id')
  @ApiOperation({ summary: 'Get single payment full details' })
  async getPaymentDetail(@Param('id') id: string) {
    return this.adminService.getPaymentDetail(id);
  }

  @Public()
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

  @Public()
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

  @Public()
  @Get('subscriptions/:id')
  @ApiOperation({ summary: 'Get single subscription with full history' })
  async getSubscriptionDetail(@Param('id') id: string) {
    return this.adminService.getSubscriptionDetail(id);
  }

  @Public()
  @Patch('subscriptions/:businessId/plan')
  @ApiOperation({ summary: 'Change plan (upgrade or downgrade)' })
  async changePlan(
    @Param('businessId') businessId: string,
    @Body('planId') planId: string,
    @Body('billingCycle') billingCycle: 'MONTHLY' | 'YEARLY',
  ) {
    return this.adminService.changePlan(businessId, planId, billingCycle);
  }

  @Public()
  @Patch('subscriptions/:businessId/extend')
  @ApiOperation({ summary: 'Extend subscription by N days' })
  async extendSubscription(
    @Param('businessId') businessId: string,
    @Body('days') days: number,
  ) {
    return this.adminService.extendSubscription(businessId, days);
  }

  @Public()
  @Patch('subscriptions/:businessId/status')
  @ApiOperation({ summary: 'Pause, Resume, Cancel, or Activate subscription' })
  async updateSubscriptionStatus(
    @Param('businessId') businessId: string,
    @Body('status') status: string,
  ) {
    return this.adminService.updateSubscriptionStatus(businessId, status);
  }

  @Public()
  @Post('subscriptions/:businessId/reset-trial')
  @ApiOperation({ summary: 'Reset trial usage counters' })
  async resetSubscriptionTrial(@Param('businessId') businessId: string) {
    return this.adminService.resetTrialLimits(businessId);
  }


  // ─── ACTIVATION CODES ──────────────────────────────────────────────────────

  @Public()
  @Get('activation-codes')
  @ApiOperation({ summary: 'Get paginated activation codes' })
  async getActivationCodes(
    @Query('search') search?: string,
    @Query('planId') planId?: string,
    @Query('isActive') isActive?: string,
    @Query('page') page?: number,
    @Query('limit') limit?: number,
  ) {
    return this.adminService.getActivationCodes({ search, planId, isActive, page, limit });
  }

  @Public()
  @Post('activation-codes')
  @ApiOperation({ summary: 'Generate single activation code' })
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

  @Public()
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

  @Public()
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

  @Public()
  @Get('revenue-stats')
  @ApiOperation({ summary: 'Get overall revenue stats' })
  async getRevenueStats() {
    return this.adminService.getRevenueStats();
  }

  @Public()
  @Get('dashboard-overview')
  @ApiOperation({ summary: 'Get main dashboard real metrics overview' })
  async getDashboardOverview() {
    return this.adminService.getDashboardOverview();
  }


  // ─── FEATURE FLAGS ─────────────────────────────────────────────────────────

  @Public()
  @Get('feature-flags')
  @ApiOperation({ summary: 'List feature flags with search & filters' })
  async getFeatureFlags(
    @Query('search') search?: string,
    @Query('level') level?: string,
    @Query('status') status?: string,
    @Query('page') page?: number,
    @Query('limit') limit?: number,
  ) {
    return this.adminService.getFeatureFlags({ search, level, status, page, limit });
  }

  @Public()
  @Post('feature-flags')
  @ApiOperation({ summary: 'Create or update a feature flag rule' })
  async upsertFeatureFlag(
    @Body() body: {
      featureKey: string;
      status: 'ON' | 'OFF' | 'BETA' | 'MAINTENANCE';
      level: 'GLOBAL' | 'COUNTRY' | 'PLAN' | 'BUSINESS';
      target?: string;
      notes?: string;
      businessId?: string;
    },
  ) {
    return this.adminService.upsertFeatureFlag(body);
  }

  @Public()
  @Delete('feature-flags/:id')
  @ApiOperation({ summary: 'Delete a feature flag rule' })
  async deleteFeatureFlag(@Param('id') id: string) {
    return this.adminService.deleteFeatureFlag(id);
  }

  @Public()
  @Get('effective-feature-flags')
  @ApiOperation({ summary: 'Get resolved feature flags for a business' })
  async getEffectiveFeatureFlags(@Query('businessId') businessId?: string) {
    return this.adminService.getEffectiveFeatureFlags(businessId);
  }


  // ─── REMOTE COMMANDS ───────────────────────────────────────────────────────

  @Public()
  @Post('remote-command')
  @ApiOperation({ summary: 'Send a remote command to a business or device' })
  async sendRemoteCommand(
    @Body() body: {
      command: string;
      businessId: string;
      deviceId?: string;
      payload?: any;
    },
  ) {
    return this.adminService.sendRemoteCommand(body);
  }

  // ─── BULK ACTIONS ──────────────────────────────────────────────────────────

  @Public()
  @Post('bulk-action')
  @ApiOperation({ summary: 'Run a bulk operation on multiple businesses' })
  async executeBulkAction(
    @Body() body: {
      action: string;
      businessIds: string[];
      payload?: any;
    },
  ) {
    return this.adminService.executeBulkAction(body);
  }

  // ─── GLOBAL CONFIG & SETTINGS ──────────────────────────────────────────────

  @Public()
  @Get('global-config')
  @ApiOperation({ summary: 'Get global system settings & version rules' })
  async getGlobalConfig() {
    return this.adminService.getGlobalConfig();
  }

  @Public()
  @Patch('global-config')
  @ApiOperation({ summary: 'Update global system settings' })
  async updateGlobalConfig(@Body() body: any) {
    return this.adminService.updateGlobalConfig(body);
  }

  // ─── LIVE STATUS & MONITORING ──────────────────────────────────────────────

  @Public()
  @Get('live-status')
  @ApiOperation({ summary: 'Get live platform health and telemetry' })
  async getLiveStatus() {
    return this.adminService.getLiveStatus();
  }

  // ─── DEVICE MANAGEMENT ─────────────────────────────────────────────────────

  @Public()
  @Get('devices')
  @ApiOperation({ summary: 'List connected devices with telemetry' })
  async getDevices(
    @Query('businessId') businessId?: string,
    @Query('search') search?: string,
    @Query('connectionStatus') connectionStatus?: string,
    @Query('page') page?: number,
    @Query('limit') limit?: number,
  ) {
    return this.adminService.getDevices({ businessId, search, connectionStatus, page, limit });
  }

  @Public()
  @Post('devices/:id/remote-logout')
  @ApiOperation({ summary: 'Remote logout a connected device' })
  async remoteLogoutDevice(@Param('id') id: string) {
    return this.adminService.remoteLogoutDevice(id);
  }


  // ─── INFRASTRUCTURE & OPERATIONS MONITORING ─────────────────────────────────

  @Public()
  @Get('infra/overview')
  @ApiOperation({ summary: 'Platform status overview (Backend, DB, Redis, Socket, Storage, SSL)' })
  async getInfraOverview() {
    return this.infraService.getOverview();
  }

  @Public()
  @Get('infra/server-health')
  @ApiOperation({ summary: 'Server health metrics (CPU, RAM, Disk, IOPS, Network, Load Avg)' })
  async getServerHealth() {
    return this.infraService.getServerHealth();
  }

  @Public()
  @Get('infra/db-monitor')
  @ApiOperation({ summary: 'PostgreSQL database monitor (Connections, Size, Tables, Slow Queries)' })
  async getDbMonitor() {
    return this.infraService.getDbMonitor();
  }

  @Public()
  @Get('infra/websocket-monitor')
  @ApiOperation({ summary: 'WebSocket Gateway monitor (Connections, Throughput, Latency)' })
  async getWebSocketMonitor() {
    return this.infraService.getWebSocketMonitor();
  }

  @Public()
  @Get('infra/api-monitor')
  @ApiOperation({ summary: 'API monitor (RPM, Latency, P95/P99, Status code distribution)' })
  async getApiMonitor() {
    return this.infraService.getApiMonitor();
  }

  @Public()
  @Get('infra/background-services')
  @ApiOperation({ summary: 'Background services & workers status' })
  async getBackgroundServices() {
    return this.infraService.getBackgroundServices();
  }

  @Public()
  @Get('infra/containers')
  @ApiOperation({ summary: 'Docker container fleet list with CPU/RAM metrics' })
  async getContainers() {
    return this.infraService.getContainers();
  }

  @Public()
  @Post('infra/containers/:id/restart')
  @ApiOperation({ summary: 'Restart a Docker container' })
  async restartContainer(@Param('id') id: string) {
    return this.infraService.restartContainer(id);
  }

  @Public()
  @Post('infra/vps/free-ram')
  @ApiOperation({ summary: 'Free VPS RAM memory via garbage collection' })
  async freeRam() {
    return this.infraService.freeRam();
  }

  @Public()
  @Post('infra/vps/clean-disk')
  @ApiOperation({ summary: 'Clean temporary files and prune disk space' })
  async cleanDisk() {
    return this.infraService.cleanDisk();
  }

  @Public()
  @Get('infra/logs')
  @ApiOperation({ summary: 'Realtime log stream with search & filters' })
  async getRealtimeLogs(
    @Query('filter') filter?: string,
    @Query('search') search?: string,
    @Query('limit') limit?: number,
  ) {
    return this.infraService.getRealtimeLogs(filter, search, limit);
  }

  @Public()
  @Get('infra/errors')
  @ApiOperation({ summary: 'Error tracking & exception logs' })
  async getErrorTracking() {
    return this.infraService.getErrorTracking();
  }

  @Public()
  @Get('infra/backups')
  @ApiOperation({ summary: 'List database backup snapshots' })
  async getBackups() {
    return this.infraService.getBackups();
  }

  @Public()
  @Post('infra/backups/create')
  @ApiOperation({ summary: 'Create manual database backup snapshot' })
  async createBackup() {
    return this.infraService.createBackup();
  }

  @Public()
  @Get('infra/storage')
  @ApiOperation({ summary: 'File storage usage breakdown' })
  async getStorageStats() {
    return this.infraService.getStorageStats();
  }

  @Public()
  @Post('infra/storage/cleanup')
  @ApiOperation({ summary: 'Cleanup temporary & export files' })
  async cleanupStorage() {
    return this.infraService.cleanupStorage();
  }


  @Public()
  @Get('infra/security')
  @ApiOperation({ summary: 'Security center metrics (Failed logins, rate limits, sessions)' })
  async getSecurityStats() {
    return this.infraService.getSecurityStats();
  }

  @Public()
  @Get('infra/deployments')
  @ApiOperation({ summary: 'Deployment history & version info' })
  async getDeployments() {
    return this.infraService.getDeployments();
  }

  @Public()
  @Get('infra/alerts')
  @ApiOperation({ summary: 'Active system alerts' })
  async getAlerts() {
    return this.infraService.getAlerts();
  }

  @Public()
  @Get('infra/live-feed')
  @ApiOperation({ summary: 'Realtime platform activity feed' })
  async getLiveFeed() {
    return this.infraService.getLiveFeed();
  }

  // ─── INTERACTIVE CRUD MUTATIONS (BUSINESS, PAYMENT, SUBSCRIPTION) ───────────

  @Public()
  @Post('businesses')
  @ApiOperation({ summary: 'Create a new business tenant and owner' })
  async createBusiness(@Body() body: any) {
    return this.adminService.createBusiness(body);
  }

  @Public()
  @Patch('businesses/:id')
  @ApiOperation({ summary: 'Update an existing business' })
  async updateBusiness(@Param('id') id: string, @Body() body: any) {
    return this.adminService.updateBusiness(id, body);
  }

  @Public()
  @Delete('businesses/:id')
  @ApiOperation({ summary: 'Delete a business and all associated data' })
  async deleteBusiness(@Param('id') id: string) {
    return this.adminService.deleteBusiness(id);
  }

  @Public()
  @Post('payments')
  @ApiOperation({ summary: 'Create a manual payment record' })
  async createPayment(@Body() body: any) {
    return this.adminService.createPayment(body);
  }

  @Public()
  @Patch('payments/:id')
  @ApiOperation({ summary: 'Update a payment transaction' })
  async updatePayment(@Param('id') id: string, @Body() body: any) {
    return this.adminService.updatePayment(id, body);
  }

  @Public()
  @Post('subscriptions')
  @ApiOperation({ summary: 'Create a new subscription' })
  async createSubscription(@Body() body: any) {
    return this.adminService.createSubscription(body);
  }

  @Public()
  @Patch('subscriptions/:id')
  @ApiOperation({ summary: 'Update a subscription' })
  async updateSubscription(@Param('id') id: string, @Body() body: any) {
    return this.adminService.updateSubscription(id, body);
  }

  // ─── POSTGRESQL DATABASE EXPLORER ENGINE ─────────────────────────────────

  @Public()
  @Get('db-explorer/tables')
  @ApiOperation({ summary: 'List all public PostgreSQL tables' })
  async getTableList() {
    return this.adminService.getTableList();
  }

  @Public()
  @Get('db-explorer/tables/:tableName/rows')
  @ApiOperation({ summary: 'Get paginated rows from any PostgreSQL table' })
  async getTableRows(
    @Param('tableName') tableName: string,
    @Query('page') page?: number,
    @Query('limit') limit?: number,
    @Query('search') search?: string,
  ) {
    return this.adminService.getTableRows(tableName, page, limit, search);
  }

  @Public()
  @Patch('db-explorer/tables/:tableName/rows/:id')
  @ApiOperation({ summary: 'Update a row in any PostgreSQL table' })
  async updateTableRow(
    @Param('tableName') tableName: string,
    @Param('id') id: string,
    @Body() body: any,
  ) {
    return this.adminService.updateTableRow(tableName, id, body);
  }

  @Public()
  @Delete('db-explorer/tables/:tableName/rows/:id')
  @ApiOperation({ summary: 'Delete a row from any PostgreSQL table' })
  async deleteTableRow(
    @Param('tableName') tableName: string,
    @Param('id') id: string,
  ) {
    return this.adminService.deleteTableRow(tableName, id);
  }

  @Public()
  @Post('db-explorer/tables/:tableName/rows')
  @ApiOperation({ summary: 'Insert a new row into any PostgreSQL table' })
  async createTableRow(
    @Param('tableName') tableName: string,
    @Body() body: any,
  ) {
    return this.adminService.createTableRow(tableName, body);
  }
}




