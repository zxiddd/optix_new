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
exports.BusinessResetService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
const sync_gateway_1 = require("../sync/sync.gateway");
const business_clock_service_1 = require("./business-clock.service");
let BusinessResetService = class BusinessResetService {
    constructor(prisma, syncGateway, businessClockService) {
        this.prisma = prisma;
        this.syncGateway = syncGateway;
        this.businessClockService = businessClockService;
    }
    async resetBusinessDay(businessId, targetBusinessDate, senderSocketId) {
        const business = await this.prisma.business.findUnique({
            where: { id: businessId },
            include: { settings: true },
        });
        if (!business) {
            throw new common_1.NotFoundException('Business profile not found');
        }
        const settings = business.settings;
        const computedStatus = this.businessClockService.calculateStatus(settings?.openingTime || '09:00', settings?.closingTime || '22:00', settings?.timezone || 'Asia/Riyadh');
        const businessDateToReset = targetBusinessDate || computedStatus.businessDate;
        if (settings?.lastResetBusinessDate === businessDateToReset) {
            return {
                alreadyReset: true,
                businessId,
                lastResetBusinessDate: settings.lastResetBusinessDate,
                tokenCounter: business.tokenCounter,
            };
        }
        const updatedSettings = await this.prisma.businessSettings.upsert({
            where: { businessId },
            update: { lastResetBusinessDate: businessDateToReset },
            create: { businessId, lastResetBusinessDate: businessDateToReset },
        });
        const updatedBusiness = await this.prisma.business.update({
            where: { id: businessId },
            data: { tokenCounter: 0, lastTokenResetDateTime: new Date() },
        });
        const payload = {
            businessId,
            lastResetBusinessDate: updatedSettings.lastResetBusinessDate,
            tokenCounter: updatedBusiness.tokenCounter,
            resetAt: updatedBusiness.lastTokenResetDateTime.toISOString(),
            alreadyReset: false,
        };
        this.syncGateway.emitToBusiness(businessId, 'business.reset', payload, senderSocketId);
        return payload;
    }
};
exports.BusinessResetService = BusinessResetService;
exports.BusinessResetService = BusinessResetService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        sync_gateway_1.SyncGateway,
        business_clock_service_1.BusinessClockService])
], BusinessResetService);
//# sourceMappingURL=business-reset.service.js.map