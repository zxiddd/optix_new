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
exports.PaymentQrController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const payment_qr_service_1 = require("./payment-qr.service");
const guards_1 = require("../auth/guards");
const decorators_1 = require("../auth/decorators");
let PaymentQrController = class PaymentQrController {
    constructor(paymentQrService) {
        this.paymentQrService = paymentQrService;
    }
    getPaymentQrs(businessId) {
        return this.paymentQrService.getPaymentQrs(businessId);
    }
    savePaymentQr(businessId, body, socketId) {
        return this.paymentQrService.savePaymentQr(businessId, body, socketId || body.senderSocketId);
    }
    selectPaymentQr(businessId, id, socketId) {
        return this.paymentQrService.selectPaymentQr(businessId, id, socketId);
    }
    deletePaymentQr(businessId, id, socketId) {
        return this.paymentQrService.deletePaymentQr(businessId, id, socketId);
    }
};
exports.PaymentQrController = PaymentQrController;
__decorate([
    (0, common_1.Get)(),
    (0, swagger_1.ApiOperation)({ summary: 'Get all payment QRs for current business' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], PaymentQrController.prototype, "getPaymentQrs", null);
__decorate([
    (0, common_1.Post)(),
    (0, swagger_1.ApiOperation)({ summary: 'Create or update payment QR' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Body)()),
    __param(2, (0, common_1.Headers)('x-socket-id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object, String]),
    __metadata("design:returntype", void 0)
], PaymentQrController.prototype, "savePaymentQr", null);
__decorate([
    (0, common_1.Put)(':id/select'),
    (0, swagger_1.ApiOperation)({ summary: 'Select default active payment QR' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Headers)('x-socket-id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String]),
    __metadata("design:returntype", void 0)
], PaymentQrController.prototype, "selectPaymentQr", null);
__decorate([
    (0, common_1.Delete)(':id'),
    (0, swagger_1.ApiOperation)({ summary: 'Delete payment QR' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Headers)('x-socket-id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String]),
    __metadata("design:returntype", void 0)
], PaymentQrController.prototype, "deletePaymentQr", null);
exports.PaymentQrController = PaymentQrController = __decorate([
    (0, swagger_1.ApiTags)('Payment QR'),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.UseGuards)(guards_1.AtGuard),
    (0, common_1.Controller)('payment-qrs'),
    __metadata("design:paramtypes", [payment_qr_service_1.PaymentQrService])
], PaymentQrController);
//# sourceMappingURL=payment-qr.controller.js.map