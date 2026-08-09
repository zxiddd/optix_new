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
exports.BusinessService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
const sync_gateway_1 = require("../sync/sync.gateway");
let BusinessService = class BusinessService {
    constructor(prisma, syncGateway) {
        this.prisma = prisma;
        this.syncGateway = syncGateway;
    }
    async getProfile(businessId) {
        const business = await this.prisma.business.findUnique({
            where: { id: businessId },
            include: {
                settings: true,
                receiptSettings: true,
                printerSettings: true,
                paymentQrs: true,
            },
        });
        if (!business) {
            throw new common_1.NotFoundException('Business profile not found');
        }
        return business;
    }
    async updateProfile(businessId, data, senderSocketId) {
        const sender = senderSocketId || data.senderSocketId;
        if (data.receiptSettings) {
            const rs = data.receiptSettings;
            const updatedRs = await this.prisma.receiptSettings.upsert({
                where: { businessId },
                create: {
                    businessId,
                    showLogo: rs.showLogo ?? false,
                    logoUrl: rs.logoUrl ?? null,
                    footerMessage: rs.footerMessage ?? 'Thank You! Visit Again 🙏',
                    showBusinessName: rs.showBusinessName ?? true,
                    showAddress: rs.showAddress ?? true,
                    showPhone: rs.showPhone ?? true,
                    showGst: rs.showGst ?? false,
                    showDateTime: rs.showDateTime ?? true,
                    showOrderNumber: rs.showOrderNumber ?? true,
                    showCashierName: rs.showCashierName ?? true,
                    showDiscounts: rs.showDiscounts ?? true,
                    showTaxes: rs.showTaxes ?? false,
                    taxPercentage: rs.taxPercentage ?? 0.0,
                    qrEnabled: rs.qrEnabled ?? false,
                    showVisitAgain: rs.showVisitAgain ?? true,
                },
                update: {
                    showLogo: rs.showLogo,
                    ...(rs.logoUrl !== undefined && { logoUrl: rs.logoUrl || null }),
                    footerMessage: rs.footerMessage,
                    showBusinessName: rs.showBusinessName,
                    showAddress: rs.showAddress,
                    showPhone: rs.showPhone,
                    showGst: rs.showGst,
                    showDateTime: rs.showDateTime,
                    showOrderNumber: rs.showOrderNumber,
                    showCashierName: rs.showCashierName,
                    showDiscounts: rs.showDiscounts,
                    showTaxes: rs.showTaxes,
                    taxPercentage: rs.taxPercentage,
                    qrEnabled: rs.qrEnabled,
                    showVisitAgain: rs.showVisitAgain,
                },
            });
            if (rs.logoUrl !== undefined || rs.showLogo !== undefined) {
                this.syncGateway.emitToBusiness(businessId, 'logo.updated', {
                    logoUrl: updatedRs.logoUrl,
                    showLogo: updatedRs.showLogo,
                }, sender);
            }
            if (rs.toggleKey !== undefined && rs.toggleValue !== undefined) {
                this.syncGateway.emitToBusiness(businessId, 'receipt.toggle.updated', {
                    key: rs.toggleKey,
                    value: rs.toggleValue,
                }, sender);
            }
            else {
                this.syncGateway.emitToBusiness(businessId, 'receipt.updated', updatedRs, sender);
            }
        }
        if (data.paymentQrs && Array.isArray(data.paymentQrs)) {
            for (const qr of data.paymentQrs) {
                if (qr.id) {
                    if (qr.isDeleted) {
                        await this.prisma.paymentQr.deleteMany({
                            where: { id: qr.id, businessId },
                        });
                        this.syncGateway.emitToBusiness(businessId, 'paymentQr.deleted', { id: qr.id }, sender);
                    }
                    else {
                        const updatedQr = await this.prisma.paymentQr.upsert({
                            where: { id: qr.id },
                            create: {
                                id: qr.id,
                                businessId,
                                name: qr.name || 'UPI QR',
                                upiId: qr.upiId,
                                imageUrl: qr.imageUrl || qr.imagePath,
                                isActive: qr.isActive ?? true,
                            },
                            update: {
                                name: qr.name,
                                upiId: qr.upiId,
                                imageUrl: qr.imageUrl || qr.imagePath,
                                isActive: qr.isActive,
                            },
                        });
                        this.syncGateway.emitToBusiness(businessId, 'paymentQr.updated', updatedQr, sender);
                    }
                }
            }
        }
        if (data.openingTime !== undefined || data.closingTime !== undefined || data.timezone !== undefined) {
            await this.prisma.businessSettings.upsert({
                where: { businessId },
                create: {
                    businessId,
                    openingTime: data.openingTime ?? '09:00',
                    closingTime: data.closingTime ?? '22:00',
                    timezone: data.timezone ?? 'Asia/Riyadh',
                },
                update: {
                    ...(data.openingTime !== undefined && { openingTime: data.openingTime }),
                    ...(data.closingTime !== undefined && { closingTime: data.closingTime }),
                    ...(data.timezone !== undefined && { timezone: data.timezone }),
                },
            });
        }
        if (data.name || data.address || data.phone || data.email || data.openingTime || data.closingTime || data.timezone) {
            const updatedBusiness = await this.prisma.business.update({
                where: { id: businessId },
                data: {
                    name: data.name,
                    email: data.email,
                    phone: data.phone,
                    address: data.address,
                    setupCompleted: true,
                },
                include: {
                    settings: true,
                    receiptSettings: true,
                    paymentQrs: true,
                },
            });
            this.syncGateway.emitToBusiness(businessId, 'business.updated', {
                name: updatedBusiness.name,
                address: updatedBusiness.address,
                phone: updatedBusiness.phone,
                email: updatedBusiness.email,
                openingTime: updatedBusiness.settings?.openingTime ?? '09:00',
                closingTime: updatedBusiness.settings?.closingTime ?? '22:00',
                timezone: updatedBusiness.settings?.timezone ?? 'Asia/Riyadh',
            }, sender);
            return updatedBusiness;
        }
        return this.getProfile(businessId);
    }
};
exports.BusinessService = BusinessService;
exports.BusinessService = BusinessService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        sync_gateway_1.SyncGateway])
], BusinessService);
//# sourceMappingURL=business.service.js.map