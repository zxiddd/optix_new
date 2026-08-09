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
exports.SubscriptionsService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
const sync_gateway_1 = require("../sync/sync.gateway");
let SubscriptionsService = class SubscriptionsService {
    constructor(prisma, syncGateway) {
        this.prisma = prisma;
        this.syncGateway = syncGateway;
    }
    async getSubscription(businessId) {
        return this.prisma.subscription.findFirst({
            where: { businessId },
            include: { plan: true },
            orderBy: { createdAt: 'desc' },
        });
    }
    async getPlans() {
        return this.prisma.plan.findMany();
    }
    async saveSubscription(businessId, dto) {
        let plan = await this.prisma.plan.findFirst({ where: { name: dto.planName || 'Starter' } });
        if (!plan) {
            plan = await this.prisma.plan.create({
                data: {
                    name: dto.planName || 'Starter',
                    price: 999.00,
                    billingPeriod: 'MONTHLY',
                    features: { items: 'unlimited', sync: true },
                },
            });
        }
        return this.prisma.subscription.create({
            data: {
                businessId,
                planId: plan.id,
                status: dto.status || 'ACTIVE',
                expiryDate: new Date(dto.expiryDate || Date.now() + 30 * 24 * 60 * 60 * 1000),
            },
            include: { plan: true },
        });
    }
    async activateCode(businessId, code) {
        if (!code)
            throw new common_1.BadRequestException('Activation code is required');
        const cleanCode = code.trim().toUpperCase();
        const activationCode = await this.prisma.activationCode.findUnique({
            where: { code: cleanCode },
        });
        if (!activationCode) {
            throw new common_1.BadRequestException('Invalid activation code. Please check and try again.');
        }
        if (!activationCode.isActive) {
            throw new common_1.BadRequestException('This activation code has been deactivated.');
        }
        if (activationCode.expiresAt && new Date(activationCode.expiresAt) < new Date()) {
            throw new common_1.BadRequestException('This activation code has expired.');
        }
        if (activationCode.maxUses > 0 && activationCode.usedCount >= activationCode.maxUses) {
            throw new common_1.BadRequestException('This activation code has reached its maximum usage limit.');
        }
        const daysToAdd = activationCode.billingCycle === 'YEARLY' ? 365 : 30;
        const expiryDate = new Date();
        expiryDate.setDate(expiryDate.getDate() + daysToAdd);
        const sub = await this.prisma.subscription.upsert({
            where: { businessId },
            create: {
                businessId,
                planId: activationCode.planId || 'STARTER',
                status: 'ACTIVE',
                billingCycle: activationCode.billingCycle || 'MONTHLY',
                activationCode: activationCode.code,
                expiryDate,
            },
            update: {
                planId: activationCode.planId || 'STARTER',
                status: 'ACTIVE',
                billingCycle: activationCode.billingCycle || 'MONTHLY',
                activationCode: activationCode.code,
                expiryDate,
            },
        });
        await this.prisma.activationCode.update({
            where: { id: activationCode.id },
            data: { usedCount: { increment: 1 } },
        });
        if (this.syncGateway) {
            this.syncGateway.emitToBusiness(businessId, 'subscription_updated', {
                status: 'ACTIVE',
                planId: sub.planId,
                expiryDate: sub.expiryDate,
            });
            this.syncGateway.emitToBusiness(businessId, 'remote_command', {
                action: 'REFRESH_SUBSCRIPTION',
                timestamp: Date.now(),
            });
        }
        return {
            success: true,
            message: `Successfully activated ${sub.planId} subscription!`,
            subscription: sub,
        };
    }
};
exports.SubscriptionsService = SubscriptionsService;
exports.SubscriptionsService = SubscriptionsService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        sync_gateway_1.SyncGateway])
], SubscriptionsService);
//# sourceMappingURL=subscriptions.service.js.map