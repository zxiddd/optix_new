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
Object.defineProperty(exports, "__esModule", { value: true });
exports.PaymentQrService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
const sync_gateway_1 = require("../sync/sync.gateway");
let PaymentQrService = class PaymentQrService {
    constructor(prisma, syncGateway) {
        this.prisma = prisma;
        this.syncGateway = syncGateway;
    }
    async getPaymentQrs(businessId) {
        return this.prisma.paymentQr.findMany({
            where: { businessId },
            orderBy: { createdAt: 'desc' },
        });
    }
    async savePaymentQr(businessId, dto, senderSocketId) {
        const isNew = !dto.id || !(await this.prisma.paymentQr.findUnique({ where: { id: dto.id } }));
        if (dto.isActive) {
            await this.prisma.paymentQr.updateMany({
                where: { businessId },
                data: { isActive: false },
            });
        }
        const qr = await this.prisma.paymentQr.upsert({
            where: { id: dto.id || 'new-id' },
            create: {
                id: dto.id,
                businessId,
                name: dto.name || 'UPI QR',
                upiId: dto.upiId,
                imageUrl: dto.imageUrl || dto.imagePath,
                isActive: dto.isActive ?? true,
            },
            update: {
                name: dto.name,
                upiId: dto.upiId,
                imageUrl: dto.imageUrl || dto.imagePath,
                isActive: dto.isActive,
            },
        });
        const eventName = isNew ? 'paymentQr.created' : 'paymentQr.updated';
        this.syncGateway.emitToBusiness(businessId, eventName, qr, senderSocketId);
        if (dto.isActive) {
            this.syncGateway.emitToBusiness(businessId, 'paymentQr.selected', { id: qr.id, businessId }, senderSocketId);
        }
        return qr;
    }
    async selectPaymentQr(businessId, id, senderSocketId) {
        await this.prisma.paymentQr.updateMany({
            where: { businessId },
            data: { isActive: false },
        });
        const selected = await this.prisma.paymentQr.update({
            where: { id },
            data: { isActive: true },
        });
        this.syncGateway.emitToBusiness(businessId, 'paymentQr.selected', { id, businessId }, senderSocketId);
        return selected;
    }
    async deletePaymentQr(businessId, id, senderSocketId) {
        await this.prisma.paymentQr.deleteMany({
            where: { id, businessId },
        });
        this.syncGateway.emitToBusiness(businessId, 'paymentQr.deleted', { id }, senderSocketId);
        return { success: true, id };
    }
};
exports.PaymentQrService = PaymentQrService;
exports.PaymentQrService = PaymentQrService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        sync_gateway_1.SyncGateway])
], PaymentQrService);
//# sourceMappingURL=payment-qr.service.js.map