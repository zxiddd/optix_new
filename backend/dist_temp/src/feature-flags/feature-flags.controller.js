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
exports.FeatureFlagsController = void 0;
const common_1 = require("@nestjs/common");
const super_admin_service_1 = require("../super-admin/super-admin.service");
const at_guard_1 = require("../auth/guards/at.guard");
const swagger_1 = require("@nestjs/swagger");
let FeatureFlagsController = class FeatureFlagsController {
    constructor(adminService) {
        this.adminService = adminService;
    }
    async getEffectiveFlags(req) {
        const businessId = req.user?.businessId;
        return this.adminService.getEffectiveFeatureFlags(businessId);
    }
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
};
exports.FeatureFlagsController = FeatureFlagsController;
__decorate([
    (0, common_1.Get)('effective'),
    (0, common_1.UseGuards)(at_guard_1.AtGuard),
    (0, swagger_1.ApiBearerAuth)(),
    (0, swagger_1.ApiOperation)({ summary: 'Get effective resolved feature flags for the authenticated business' }),
    __param(0, (0, common_1.Request)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], FeatureFlagsController.prototype, "getEffectiveFlags", null);
__decorate([
    (0, common_1.Get)('public-config'),
    (0, swagger_1.ApiOperation)({ summary: 'Get public global config (maintenance mode, min version)' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", Promise)
], FeatureFlagsController.prototype, "getPublicConfig", null);
exports.FeatureFlagsController = FeatureFlagsController = __decorate([
    (0, swagger_1.ApiTags)('Feature Flags'),
    (0, common_1.Controller)('feature-flags'),
    __metadata("design:paramtypes", [super_admin_service_1.SuperAdminService])
], FeatureFlagsController);
//# sourceMappingURL=feature-flags.controller.js.map