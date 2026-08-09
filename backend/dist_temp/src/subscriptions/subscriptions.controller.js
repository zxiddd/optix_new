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
exports.StandaloneSubscriptionController = exports.SubscriptionsController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const subscriptions_service_1 = require("./subscriptions.service");
const guards_1 = require("../auth/guards");
const decorators_1 = require("../auth/decorators");
let SubscriptionsController = class SubscriptionsController {
    constructor(subscriptionsService) {
        this.subscriptionsService = subscriptionsService;
    }
    getSubscription(businessId) {
        return this.subscriptionsService.getSubscription(businessId);
    }
    getPlans() {
        return this.subscriptionsService.getPlans();
    }
    saveSubscription(businessId, body) {
        return this.subscriptionsService.saveSubscription(businessId, body);
    }
    activateCode(businessId, code) {
        return this.subscriptionsService.activateCode(businessId, code);
    }
};
exports.SubscriptionsController = SubscriptionsController;
__decorate([
    (0, common_1.UseGuards)(guards_1.AtGuard),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.Get)(),
    (0, swagger_1.ApiOperation)({ summary: 'Get current business subscription' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], SubscriptionsController.prototype, "getSubscription", null);
__decorate([
    (0, decorators_1.Public)(),
    (0, common_1.Get)('plans'),
    (0, swagger_1.ApiOperation)({ summary: 'Get all available subscription plans' }),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", []),
    __metadata("design:returntype", void 0)
], SubscriptionsController.prototype, "getPlans", null);
__decorate([
    (0, common_1.UseGuards)(guards_1.AtGuard),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.Post)(),
    (0, swagger_1.ApiOperation)({ summary: 'Save or renew business subscription' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", void 0)
], SubscriptionsController.prototype, "saveSubscription", null);
__decorate([
    (0, common_1.UseGuards)(guards_1.AtGuard),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.Post)('activate'),
    (0, swagger_1.ApiOperation)({ summary: 'Activate subscription via code' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Body)('code')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String]),
    __metadata("design:returntype", void 0)
], SubscriptionsController.prototype, "activateCode", null);
exports.SubscriptionsController = SubscriptionsController = __decorate([
    (0, swagger_1.ApiTags)('Subscriptions'),
    (0, common_1.Controller)('subscriptions'),
    __metadata("design:paramtypes", [subscriptions_service_1.SubscriptionsService])
], SubscriptionsController);
let StandaloneSubscriptionController = class StandaloneSubscriptionController {
    constructor(subscriptionsService) {
        this.subscriptionsService = subscriptionsService;
    }
    activateCode(businessId, code) {
        return this.subscriptionsService.activateCode(businessId, code);
    }
};
exports.StandaloneSubscriptionController = StandaloneSubscriptionController;
__decorate([
    (0, common_1.UseGuards)(guards_1.AtGuard),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.Post)('activate'),
    (0, swagger_1.ApiOperation)({ summary: 'Activate subscription via code from Android POS app' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Body)('code')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String]),
    __metadata("design:returntype", void 0)
], StandaloneSubscriptionController.prototype, "activateCode", null);
exports.StandaloneSubscriptionController = StandaloneSubscriptionController = __decorate([
    (0, swagger_1.ApiTags)('Subscription (Singular compatibility for Android App)'),
    (0, common_1.Controller)('subscription'),
    __metadata("design:paramtypes", [subscriptions_service_1.SubscriptionsService])
], StandaloneSubscriptionController);
//# sourceMappingURL=subscriptions.controller.js.map