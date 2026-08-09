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
exports.BusinessController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const business_service_1 = require("./business.service");
const business_reset_service_1 = require("./business-reset.service");
const guards_1 = require("../auth/guards");
const decorators_1 = require("../auth/decorators");
let BusinessController = class BusinessController {
    constructor(businessService, businessResetService) {
        this.businessService = businessService;
        this.businessResetService = businessResetService;
    }
    getProfile(businessId) {
        return this.businessService.getProfile(businessId);
    }
    updateProfile(businessId, body, socketId) {
        return this.businessService.updateProfile(businessId, body, socketId || body.senderSocketId);
    }
    resetBusinessDay(businessId, body, socketId) {
        return this.businessResetService.resetBusinessDay(businessId, body?.targetBusinessDate, socketId || body?.senderSocketId);
    }
};
exports.BusinessController = BusinessController;
__decorate([
    (0, common_1.Get)('profile'),
    (0, swagger_1.ApiOperation)({ summary: 'Get current business profile' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], BusinessController.prototype, "getProfile", null);
__decorate([
    (0, common_1.Post)('profile'),
    (0, swagger_1.ApiOperation)({ summary: 'Update business profile' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Body)()),
    __param(2, (0, common_1.Headers)('x-socket-id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object, String]),
    __metadata("design:returntype", void 0)
], BusinessController.prototype, "updateProfile", null);
__decorate([
    (0, common_1.Post)('reset'),
    (0, swagger_1.ApiOperation)({ summary: 'Reset business day (exactly once per business date)' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Body)()),
    __param(2, (0, common_1.Headers)('x-socket-id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object, String]),
    __metadata("design:returntype", void 0)
], BusinessController.prototype, "resetBusinessDay", null);
exports.BusinessController = BusinessController = __decorate([
    (0, swagger_1.ApiTags)('Business'),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.UseGuards)(guards_1.AtGuard),
    (0, common_1.Controller)('business'),
    __metadata("design:paramtypes", [business_service_1.BusinessService,
        business_reset_service_1.BusinessResetService])
], BusinessController);
//# sourceMappingURL=business.controller.js.map