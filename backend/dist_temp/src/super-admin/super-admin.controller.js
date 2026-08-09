"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var __param = (this && this.__param) || function (paramIndex, decorator) {
    return function (target, key) { decorator(target, key, paramIndex); }
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.SuperAdminController = void 0;
const common_1 = require("@nestjs/common");
const super_admin_service_1 = require("./super-admin.service");
const at_guard_1 = require("../auth/guards/at.guard");
const roles_guard_1 = require("../auth/guards/roles.guard");
const roles_decorator_1 = require("../auth/decorators/roles.decorator");
const decorators_1 = require("../auth/decorators");
const swagger_1 = require("@nestjs/swagger");
const client_1 = require("@prisma/client");
const infra_monitoring_service_1 = require("./infra-monitoring.service");
let SuperAdminController = class SuperAdminController {
    constructor(adminService, infraService) {
        this.adminService = adminService;
        this.infraService = infraService;
    }
    async getBusinesses(page, limit, search, planId, status, country) {
        return this.adminService.getBusinesses({ page, limit, search, planId, status, country });
    }
    async getBusinessDetail(id) {
        return this.adminService.getBusinessDetail(id);
    }
    async updateStatus(id, status) {
        return this.adminService.updateBusinessStatus(id, status);
    }
    async resetTrial(id) {
        return this.adminService.resetTrialLimits(id);
    }
    async getPayments(status, businessId, planId, country, currency, dateFrom, dateTo, page, limit, search) {
        return this.adminService.getAllPayments({ status, businessId, planId, country, currency, dateFrom, dateTo, page, limit, search });
    }
    async getPaymentDetail(id) {
        return this.adminService.getPaymentDetail(id);
    }
    async refundPayment(id, reason, partial) {
        return this.adminService.refundPayment(id, reason, partial);
    }
    async getSubscriptions(status, planId, country, page, limit, search) {
        return this.adminService.getAllSubscriptions({ status, planId, country, page, limit, search });
    }
    async getSubscriptionDetail(id) {
        return this.adminService.getSubscriptionDetail(id);
    }
    async changePlan(businessId, planId, billingCycle) {
        return this.adminService.changePlan(businessId, planId, billingCycle);
    }
    async extendSubscription(businessId, days) {
        return this.adminService.extendSubscription(businessId, days);
    }
    async updateSubscriptionStatus(businessId, status) {
        return this.adminService.updateSubscriptionStatus(businessId, status);
    }
    async resetSubscriptionTrial(businessId) {
        return this.adminService.resetTrialLimits(businessId);
    }
    async getActivationCodes(search, planId, isActive, page, limit) {
        return this.adminService.getActivationCodes({ search, planId, isActive, page, limit });
    }
    async createActivationCode(body) {
        return this.adminService.createActivationCode(body);
    }
    async bulkCreateActivationCodes(body) {
        return this.adminService.bulkCreateActivationCodes(body);
    }
    async deactivateCode(id) {
        return this.adminService.deactivateCode(id);
    }
    async deleteCode(id) {
        return this.adminService.deleteCode(id);
    }
    async getAuditLogs(businessId, action, entity, page, limit) {
        return this.adminService.getAdminAuditLogs({ businessId, action, entity, page, limit });
    }
    async getRevenueStats() {
        return this.adminService.getRevenueStats();
    }
    async getDashboardOverview() {
        return this.adminService.getDashboardOverview();
    }
    async getFeatureFlags(search, level, status, page, limit) {
        return this.adminService.getFeatureFlags({ search, level, status, page, limit });
    }
    async upsertFeatureFlag(body) {
        return this.adminService.upsertFeatureFlag(body);
    }
    async deleteFeatureFlag(id) {
        return this.adminService.deleteFeatureFlag(id);
    }
    async getEffectiveFeatureFlags(businessId) {
        return this.adminService.getEffectiveFeatureFlags(businessId);
    }
    async sendRemoteCommand(body) {
        return this.adminService.sendRemoteCommand(body);
    }
    async sendAdminNotification(body) {
        return this.adminService.sendAdminNotification(body);
    }
    async executeBulkAction(body) {
        return this.adminService.executeBulkAction(body);
    }
    async getGlobalConfig() {
        return this.adminService.getGlobalConfig();
    }
    async updateGlobalConfig(body) {
        return this.adminService.updateGlobalConfig(body);
    }
    async getLiveStatus() {
        return this.adminService.getLiveStatus();
    }
    async getDevices(businessId, search, connectionStatus, page, limit) {
        return this.adminService.getDevices({ businessId, search, connectionStatus, page, limit });
    }
    async remoteLogoutDevice(id) {
        return this.adminService.remoteLogoutDevice(id);
    }
    async getInfraOverview() {
        return this.infraService.getOverview();
    }
    async getServerHealth() {
        return this.infraService.getServerHealth();
    }
    async getDbMonitor() {
        return this.infraService.getDbMonitor();
    }
    async getWebSocketMonitor() {
        return this.infraService.getWebSocketMonitor();
    }
    async getApiMonitor() {
        return this.infraService.getApiMonitor();
    }
    async getBackgroundServices() {
        return this.infraService.getBackgroundServices();
    }
    async getContainers() {
        return this.infraService.getContainers();
    }
    async restartContainer(id) {
        return this.infraService.restartContainer(id);
    }
    async freeRam() {
        return this.infraService.freeRam();
    }
    async cleanDisk() {
        return this.infraService.cleanDisk();
    }
    async getRealtimeLogs(filter, search, limit) {
        return this.infraService.getRealtimeLogs(filter, search, limit);
    }
    async getErrorTracking() {
        return this.infraService.getErrorTracking();
    }
    async getBackups() {
        return this.infraService.getBackups();
    }
    async createBackup() {
        return this.infraService.createBackup();
    }
    async getStorageStats() {
        return this.infraService.getStorageStats();
    }
    async cleanupStorage() {
        return this.infraService.cleanupStorage();
    }
    async getSecurityStats() {
        return this.infraService.getSecurityStats();
    }
    async getDeployments() {
        return this.infraService.getDeployments();
    }
    async getAlerts() {
        return this.infraService.getAlerts();
    }
    async getLiveFeed() {
        return this.infraService.getLiveFeed();
    }
    async createBusiness(body) {
        return this.adminService.createBusiness(body);
    }
    async updateBusiness(id, body) {
        return this.adminService.updateBusiness(id, body);
    }
    async deleteBusiness(id) {
        return this.adminService.deleteBusiness(id);
    }
    async createPayment(body) {
        return this.adminService.createPayment(body);
    }
    async updatePayment(id, body) {
        return this.adminService.updatePayment(id, body);
    }
    async createSubscription(body) {
        return this.adminService.createSubscription(body);
    }
    async updateSubscription(id, body) {
        return this.adminService.updateSubscription(id, body);
    }
    async getTableList() {
        return this.adminService.getTableList();
    }
    async getTableRows(tableName, page, limit, search) {
        return this.adminService.getTableRows(tableName, page, limit, search);
    }
    async updateTableRow(tableName, id, body) {
        return this.adminService.updateTableRow(tableName, id, body);
    }
    async deleteTableRow(tableName, id) {
        return this.adminService.deleteTableRow(tableName, id);
    }
    async createTableRow(tableName, body) {
        return this.adminService.createTableRow(tableName, body);
    }
};
exports.SuperAdminController = SuperAdminController;
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('businesses'),
    (0, swagger_1.ApiOperation)({ summary: 'Get paginated list of businesses' }),
    __param(0, (0, common_1.Query)('page')),
    __param(1, (0, common_1.Query)('limit')),
    __param(2, (0, common_1.Query)('search')),
    __param(3, (0, common_1.Query)('planId')),
    __param(4, (0, common_1.Query)('status')),
    __param(5, (0, common_1.Query)('country')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Number, Number, String, String, String, String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getBusinesses", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('businesses/:id'),
    (0, swagger_1.ApiOperation)({ summary: 'Get full details of a business' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getBusinessDetail", null);
__decorate([
    (0, common_1.Patch)('businesses/:id/status'),
    (0, swagger_1.ApiOperation)({ summary: 'Update business/subscription status' }),
    __param(0, (0, common_1.Param)('id')),
    __param(1, (0, common_1.Body)('status')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "updateStatus", null);
__decorate([
    (0, common_1.Post)('businesses/:id/reset-trial'),
    (0, swagger_1.ApiOperation)({ summary: 'Reset trial limits for a business' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "resetTrial", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('payments'),
    (0, swagger_1.ApiOperation)({ summary: 'Get all payment transactions with full filters' }),
    __param(0, (0, common_1.Query)('status')),
    __param(1, (0, common_1.Query)('businessId')),
    __param(2, (0, common_1.Query)('planId')),
    __param(3, (0, common_1.Query)('country')),
    __param(4, (0, common_1.Query)('currency')),
    __param(5, (0, common_1.Query)('dateFrom')),
    __param(6, (0, common_1.Query)('dateTo')),
    __param(7, (0, common_1.Query)('page')),
    __param(8, (0, common_1.Query)('limit')),
    __param(9, (0, common_1.Query)('search')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String, String, String, String, String, Number, Number, String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getPayments", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('payments/:id'),
    (0, swagger_1.ApiOperation)({ summary: 'Get single payment full details' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getPaymentDetail", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('payments/:id/refund'),
    (0, swagger_1.ApiOperation)({ summary: 'Refund a payment' }),
    (0, common_1.HttpCode)(common_1.HttpStatus.OK),
    __param(0, (0, common_1.Param)('id')),
    __param(1, (0, common_1.Body)('reason')),
    __param(2, (0, common_1.Body)('partial')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, Number]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "refundPayment", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('subscriptions'),
    (0, swagger_1.ApiOperation)({ summary: 'Get all subscriptions with filters' }),
    __param(0, (0, common_1.Query)('status')),
    __param(1, (0, common_1.Query)('planId')),
    __param(2, (0, common_1.Query)('country')),
    __param(3, (0, common_1.Query)('page')),
    __param(4, (0, common_1.Query)('limit')),
    __param(5, (0, common_1.Query)('search')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String, Number, Number, String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getSubscriptions", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('subscriptions/:id'),
    (0, swagger_1.ApiOperation)({ summary: 'Get single subscription with full history' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getSubscriptionDetail", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Patch)('subscriptions/:businessId/plan'),
    (0, swagger_1.ApiOperation)({ summary: 'Change plan (upgrade or downgrade)' }),
    __param(0, (0, common_1.Param)('businessId')),
    __param(1, (0, common_1.Body)('planId')),
    __param(2, (0, common_1.Body)('billingCycle')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "changePlan", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Patch)('subscriptions/:businessId/extend'),
    (0, swagger_1.ApiOperation)({ summary: 'Extend subscription by N days' }),
    __param(0, (0, common_1.Param)('businessId')),
    __param(1, (0, common_1.Body)('days')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Number]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "extendSubscription", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Patch)('subscriptions/:businessId/status'),
    (0, swagger_1.ApiOperation)({ summary: 'Pause, Resume, Cancel, or Activate subscription' }),
    __param(0, (0, common_1.Param)('businessId')),
    __param(1, (0, common_1.Body)('status')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "updateSubscriptionStatus", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('subscriptions/:businessId/reset-trial'),
    (0, swagger_1.ApiOperation)({ summary: 'Reset trial usage counters' }),
    __param(0, (0, common_1.Param)('businessId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "resetSubscriptionTrial", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('activation-codes'),
    (0, swagger_1.ApiOperation)({ summary: 'Get paginated activation codes' }),
    __param(0, (0, common_1.Query)('search')),
    __param(1, (0, common_1.Query)('planId')),
    __param(2, (0, common_1.Query)('isActive')),
    __param(3, (0, common_1.Query)('page')),
    __param(4, (0, common_1.Query)('limit')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String, Number, Number]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getActivationCodes", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('activation-codes'),
    (0, swagger_1.ApiOperation)({ summary: 'Generate single activation code' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "createActivationCode", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('activation-codes/bulk'),
    (0, swagger_1.ApiOperation)({ summary: 'Bulk generate activation codes' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "bulkCreateActivationCodes", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Patch)('activation-codes/:id/deactivate'),
    (0, swagger_1.ApiOperation)({ summary: 'Deactivate an activation code' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "deactivateCode", null);
__decorate([
    (0, common_1.Delete)('activation-codes/:id'),
    (0, swagger_1.ApiOperation)({ summary: 'Delete an activation code' }),
    (0, common_1.HttpCode)(common_1.HttpStatus.NO_CONTENT),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "deleteCode", null);
__decorate([
    (0, common_1.Get)('audit-logs'),
    (0, swagger_1.ApiOperation)({ summary: 'Get paginated admin audit logs' }),
    __param(0, (0, common_1.Query)('businessId')),
    __param(1, (0, common_1.Query)('action')),
    __param(2, (0, common_1.Query)('entity')),
    __param(3, (0, common_1.Query)('page')),
    __param(4, (0, common_1.Query)('limit')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String, Number, Number]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getAuditLogs", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('revenue-stats'),
    (0, swagger_1.ApiOperation)({ summary: 'Get overall revenue stats' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getRevenueStats", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('dashboard-overview'),
    (0, swagger_1.ApiOperation)({ summary: 'Get main dashboard real metrics overview' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getDashboardOverview", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('feature-flags'),
    (0, swagger_1.ApiOperation)({ summary: 'List feature flags with search & filters' }),
    __param(0, (0, common_1.Query)('search')),
    __param(1, (0, common_1.Query)('level')),
    __param(2, (0, common_1.Query)('status')),
    __param(3, (0, common_1.Query)('page')),
    __param(4, (0, common_1.Query)('limit')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String, Number, Number]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getFeatureFlags", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('feature-flags'),
    (0, swagger_1.ApiOperation)({ summary: 'Create or update a feature flag rule' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "upsertFeatureFlag", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Delete)('feature-flags/:id'),
    (0, swagger_1.ApiOperation)({ summary: 'Delete a feature flag rule' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "deleteFeatureFlag", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('effective-feature-flags'),
    (0, swagger_1.ApiOperation)({ summary: 'Get resolved feature flags for a business' }),
    __param(0, (0, common_1.Query)('businessId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getEffectiveFeatureFlags", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('remote-command'),
    (0, swagger_1.ApiOperation)({ summary: 'Send a remote command to a business or device' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "sendRemoteCommand", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('notifications/send'),
    (0, swagger_1.ApiOperation)({ summary: 'Send targeted or broadcast notification to business terminals' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "sendAdminNotification", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('bulk-action'),
    (0, swagger_1.ApiOperation)({ summary: 'Run a bulk operation on multiple businesses' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "executeBulkAction", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('global-config'),
    (0, swagger_1.ApiOperation)({ summary: 'Get global system settings & version rules' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getGlobalConfig", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Patch)('global-config'),
    (0, swagger_1.ApiOperation)({ summary: 'Update global system settings' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "updateGlobalConfig", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('live-status'),
    (0, swagger_1.ApiOperation)({ summary: 'Get live platform health and telemetry' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getLiveStatus", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('devices'),
    (0, swagger_1.ApiOperation)({ summary: 'List connected devices with telemetry' }),
    __param(0, (0, common_1.Query)('businessId')),
    __param(1, (0, common_1.Query)('search')),
    __param(2, (0, common_1.Query)('connectionStatus')),
    __param(3, (0, common_1.Query)('page')),
    __param(4, (0, common_1.Query)('limit')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String, Number, Number]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getDevices", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('devices/:id/remote-logout'),
    (0, swagger_1.ApiOperation)({ summary: 'Remote logout a connected device' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "remoteLogoutDevice", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('infra/overview'),
    (0, swagger_1.ApiOperation)({ summary: 'Platform status overview (Backend, DB, Redis, Socket, Storage, SSL)' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getInfraOverview", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('infra/server-health'),
    (0, swagger_1.ApiOperation)({ summary: 'Server health metrics (CPU, RAM, Disk, IOPS, Network, Load Avg)' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getServerHealth", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('infra/db-monitor'),
    (0, swagger_1.ApiOperation)({ summary: 'PostgreSQL database monitor (Connections, Size, Tables, Slow Queries)' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getDbMonitor", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('infra/websocket-monitor'),
    (0, swagger_1.ApiOperation)({ summary: 'WebSocket Gateway monitor (Connections, Throughput, Latency)' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getWebSocketMonitor", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('infra/api-monitor'),
    (0, swagger_1.ApiOperation)({ summary: 'API monitor (RPM, Latency, P95/P99, Status code distribution)' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getApiMonitor", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('infra/background-services'),
    (0, swagger_1.ApiOperation)({ summary: 'Background services & workers status' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getBackgroundServices", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('infra/containers'),
    (0, swagger_1.ApiOperation)({ summary: 'Docker container fleet list with CPU/RAM metrics' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getContainers", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('infra/containers/:id/restart'),
    (0, swagger_1.ApiOperation)({ summary: 'Restart a Docker container' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "restartContainer", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('infra/vps/free-ram'),
    (0, swagger_1.ApiOperation)({ summary: 'Free VPS RAM memory via garbage collection' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "freeRam", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('infra/vps/clean-disk'),
    (0, swagger_1.ApiOperation)({ summary: 'Clean temporary files and prune disk space' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "cleanDisk", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('infra/logs'),
    (0, swagger_1.ApiOperation)({ summary: 'Realtime log stream with search & filters' }),
    __param(0, (0, common_1.Query)('filter')),
    __param(1, (0, common_1.Query)('search')),
    __param(2, (0, common_1.Query)('limit')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, Number]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getRealtimeLogs", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('infra/errors'),
    (0, swagger_1.ApiOperation)({ summary: 'Error tracking & exception logs' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getErrorTracking", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('infra/backups'),
    (0, swagger_1.ApiOperation)({ summary: 'List database backup snapshots' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getBackups", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('infra/backups/create'),
    (0, swagger_1.ApiOperation)({ summary: 'Create manual database backup snapshot' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "createBackup", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('infra/storage'),
    (0, swagger_1.ApiOperation)({ summary: 'File storage usage breakdown' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getStorageStats", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('infra/storage/cleanup'),
    (0, swagger_1.ApiOperation)({ summary: 'Cleanup temporary & export files' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "cleanupStorage", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('infra/security'),
    (0, swagger_1.ApiOperation)({ summary: 'Security center metrics (Failed logins, rate limits, sessions)' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getSecurityStats", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('infra/deployments'),
    (0, swagger_1.ApiOperation)({ summary: 'Deployment history & version info' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getDeployments", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('infra/alerts'),
    (0, swagger_1.ApiOperation)({ summary: 'Active system alerts' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getAlerts", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('infra/live-feed'),
    (0, swagger_1.ApiOperation)({ summary: 'Realtime platform activity feed' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getLiveFeed", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('businesses'),
    (0, swagger_1.ApiOperation)({ summary: 'Create a new business tenant and owner' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "createBusiness", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Patch)('businesses/:id'),
    (0, swagger_1.ApiOperation)({ summary: 'Update an existing business' }),
    __param(0, (0, common_1.Param)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "updateBusiness", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Delete)('businesses/:id'),
    (0, swagger_1.ApiOperation)({ summary: 'Delete a business and all associated data' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "deleteBusiness", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('payments'),
    (0, swagger_1.ApiOperation)({ summary: 'Create a manual payment record' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "createPayment", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Patch)('payments/:id'),
    (0, swagger_1.ApiOperation)({ summary: 'Update a payment transaction' }),
    __param(0, (0, common_1.Param)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "updatePayment", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('subscriptions'),
    (0, swagger_1.ApiOperation)({ summary: 'Create a new subscription' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "createSubscription", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Patch)('subscriptions/:id'),
    (0, swagger_1.ApiOperation)({ summary: 'Update a subscription' }),
    __param(0, (0, common_1.Param)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "updateSubscription", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('db-explorer/tables'),
    (0, swagger_1.ApiOperation)({ summary: 'List all public PostgreSQL tables' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getTableList", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('db-explorer/tables/:tableName/rows'),
    (0, swagger_1.ApiOperation)({ summary: 'Get paginated rows from any PostgreSQL table' }),
    __param(0, (0, common_1.Param)('tableName')),
    __param(1, (0, common_1.Query)('page')),
    __param(2, (0, common_1.Query)('limit')),
    __param(3, (0, common_1.Query)('search')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Number, Number, String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "getTableRows", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Patch)('db-explorer/tables/:tableName/rows/:id'),
    (0, swagger_1.ApiOperation)({ summary: 'Update a row in any PostgreSQL table' }),
    __param(0, (0, common_1.Param)('tableName')),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, Object]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "updateTableRow", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Delete)('db-explorer/tables/:tableName/rows/:id'),
    (0, swagger_1.ApiOperation)({ summary: 'Delete a row from any PostgreSQL table' }),
    __param(0, (0, common_1.Param)('tableName')),
    __param(1, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "deleteTableRow", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Post)('db-explorer/tables/:tableName/rows'),
    (0, swagger_1.ApiOperation)({ summary: 'Insert a new row into any PostgreSQL table' }),
    __param(0, (0, common_1.Param)('tableName')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], SuperAdminController.prototype, "createTableRow", null);
exports.SuperAdminController = SuperAdminController = __decorate([
    (0, swagger_1.ApiTags)('Super Admin'),
    (0, common_1.Controller)('super-admin'),
    (0, common_1.UseGuards)(at_guard_1.AtGuard, roles_guard_1.RolesGuard),
    (0, roles_decorator_1.Roles)(client_1.UserRole.SUPER_ADMIN),
    (0, swagger_1.ApiBearerAuth)(),
    __metadata("design:paramtypes", [super_admin_service_1.SuperAdminService,
        infra_monitoring_service_1.InfraMonitoringService])
], SuperAdminController);
//# sourceMappingURL=super-admin.controller.js.map