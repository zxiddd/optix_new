"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var SuperAdminService_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.SuperAdminService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
const sync_gateway_1 = require("../sync/sync.gateway");
const crypto = __importStar(require("crypto"));
const argon2_1 = require("@node-rs/argon2");
let SuperAdminService = SuperAdminService_1 = class SuperAdminService {
    constructor(prisma, syncGateway) {
        this.prisma = prisma;
        this.syncGateway = syncGateway;
        this.logger = new common_1.Logger(SuperAdminService_1.name);
    }
    async getBusinesses(query) {
        const page = Number(query.page) || 1;
        const limit = Number(query.limit) || 20;
        const skip = (page - 1) * limit;
        const where = { isDeleted: false };
        if (query.search) {
            where.OR = [
                { name: { contains: query.search, mode: 'insensitive' } },
                { id: { contains: query.search, mode: 'insensitive' } },
                { phone: { contains: query.search, mode: 'insensitive' } },
                { email: { contains: query.search, mode: 'insensitive' } },
            ];
        }
        if (query.planId) {
            where.subscriptions = { some: { planId: query.planId } };
        }
        if (query.status) {
            where.subscriptions = { some: { status: query.status } };
        }
        if (query.country) {
            where.country = query.country;
        }
        const [total, items] = await Promise.all([
            this.prisma.business.count({ where }),
            this.prisma.business.findMany({
                where,
                include: {
                    subscriptions: {
                        include: { plan: true },
                        orderBy: { createdAt: 'desc' },
                        take: 1,
                    },
                    _count: {
                        select: {
                            products: true,
                            orders: true,
                            staff: true,
                        },
                    },
                    users: {
                        where: { role: 'OWNER' },
                        select: { email: true },
                        take: 1,
                    },
                },
                orderBy: { createdAt: 'desc' },
                skip,
                take: limit,
            }),
        ]);
        return {
            items,
            meta: {
                total,
                page,
                lastPage: Math.ceil(total / limit),
            },
        };
    }
    async getBusinessDetail(id) {
        const business = await this.prisma.business.findUnique({
            where: { id },
            include: {
                settings: true,
                subscriptions: {
                    include: { plan: true },
                    orderBy: { createdAt: 'desc' },
                },
                users: {
                    select: { id: true, email: true, role: true, createdAt: true },
                },
                _count: {
                    select: {
                        products: true,
                        orders: true,
                        staff: true,
                        categories: true,
                        devices: true,
                    },
                },
                devices: {
                    orderBy: { lastSeen: 'desc' },
                    take: 5,
                },
                transactions: {
                    orderBy: { createdAt: 'desc' },
                    take: 10,
                },
            },
        });
        if (!business)
            throw new common_1.NotFoundException('Business not found');
        const [recentOrders, recentLogs, staffList, productsList] = await Promise.all([
            this.prisma.order.findMany({
                where: { businessId: id },
                orderBy: { createdAt: 'desc' },
                take: 10,
                include: { items: true },
            }).catch(() => []),
            this.prisma.staffActivityLog.findMany({
                where: { businessId: id },
                orderBy: { createdAt: 'desc' },
                take: 10,
                include: { staff: { select: { name: true } } },
            }).catch(() => []),
            this.prisma.staff.findMany({
                where: { businessId: id },
                orderBy: { createdAt: 'desc' },
                take: 20,
            }).catch(() => []),
            this.prisma.product.findMany({
                where: { businessId: id },
                orderBy: { name: 'asc' },
                take: 20,
                include: { category: true },
            }).catch(() => []),
        ]);
        return { ...business, recentOrders, recentLogs, staffList, productsList };
    }
    async updateBusinessStatus(id, status) {
        const sub = await this.prisma.subscription.findUnique({ where: { businessId: id } });
        if (!sub)
            throw new common_1.NotFoundException('Subscription not found');
        const updated = await this.prisma.subscription.update({
            where: { businessId: id },
            data: { status: status },
        });
        await this.writeAdminAuditLog(id, 'UPDATE_STATUS', 'SUBSCRIPTION', sub.id, { status: sub.status }, { status });
        this.syncGateway.emitToBusiness(id, 'subscription_updated', { status });
        return updated;
    }
    async resetTrialLimits(id) {
        const updated = await this.prisma.subscription.update({
            where: { businessId: id },
            data: { billsUsed: 0, productsUsed: 0 },
        });
        await this.writeAdminAuditLog(id, 'RESET_TRIAL', 'SUBSCRIPTION', updated.id, {}, { billsUsed: 0, productsUsed: 0 });
        return updated;
    }
    async getAllPayments(filters) {
        const page = Number(filters.page) || 1;
        const limit = Number(filters.limit) || 50;
        const skip = (page - 1) * limit;
        const where = {};
        if (filters.status)
            where.status = filters.status;
        if (filters.businessId)
            where.businessId = filters.businessId;
        if (filters.planId)
            where.planId = filters.planId;
        if (filters.country)
            where.country = { equals: filters.country, mode: 'insensitive' };
        if (filters.currency)
            where.currency = filters.currency;
        if (filters.dateFrom || filters.dateTo) {
            where.createdAt = {};
            if (filters.dateFrom)
                where.createdAt.gte = new Date(filters.dateFrom);
            if (filters.dateTo)
                where.createdAt.lte = new Date(filters.dateTo);
        }
        if (filters.search) {
            where.OR = [
                { razorpayOrderId: { contains: filters.search, mode: 'insensitive' } },
                { razorpayPaymentId: { contains: filters.search, mode: 'insensitive' } },
                { business: { name: { contains: filters.search, mode: 'insensitive' } } },
            ];
        }
        const [total, items] = await Promise.all([
            this.prisma.paymentTransaction.count({ where }),
            this.prisma.paymentTransaction.findMany({
                where,
                include: {
                    business: {
                        select: { id: true, name: true, country: true, users: { where: { role: 'OWNER' }, select: { email: true }, take: 1 } },
                    },
                },
                orderBy: { createdAt: 'desc' },
                skip,
                take: limit,
            }),
        ]);
        return {
            items,
            meta: { total, page, lastPage: Math.ceil(total / limit) },
        };
    }
    async getPaymentDetail(id) {
        const tx = await this.prisma.paymentTransaction.findUnique({
            where: { id },
            include: {
                business: {
                    include: { users: { where: { role: 'OWNER' }, select: { email: true }, take: 1 } },
                },
                subscription: { include: { plan: true } },
            },
        });
        if (!tx)
            throw new common_1.NotFoundException('Payment not found');
        return tx;
    }
    async refundPayment(id, reason, partial) {
        const tx = await this.prisma.paymentTransaction.findUnique({ where: { id } });
        if (!tx)
            throw new common_1.NotFoundException('Payment not found');
        if (tx.status !== 'CAPTURED')
            throw new common_1.BadRequestException('Only CAPTURED payments can be refunded');
        const updated = await this.prisma.paymentTransaction.update({
            where: { id },
            data: {
                status: 'REFUNDED',
                gatewayMetadata: {
                    ...(tx.gatewayMetadata || {}),
                    refundReason: reason,
                    refundedAt: new Date().toISOString(),
                    refundAmount: partial ?? tx.amount,
                },
            },
        });
        await this.writeAdminAuditLog(tx.businessId, 'REFUND_PAYMENT', 'PAYMENT', id, { status: 'CAPTURED' }, { status: 'REFUNDED', reason });
        return updated;
    }
    async getAllSubscriptions(filters) {
        const page = Number(filters.page) || 1;
        const limit = Number(filters.limit) || 50;
        const skip = (page - 1) * limit;
        const where = {};
        if (filters.status)
            where.status = filters.status;
        if (filters.planId)
            where.planId = filters.planId;
        if (filters.country)
            where.country = { equals: filters.country, mode: 'insensitive' };
        if (filters.search) {
            where.business = { name: { contains: filters.search, mode: 'insensitive' } };
        }
        const [total, items] = await Promise.all([
            this.prisma.subscription.count({ where }),
            this.prisma.subscription.findMany({
                where,
                include: {
                    plan: true,
                    business: {
                        select: { id: true, name: true, country: true, users: { where: { role: 'OWNER' }, select: { email: true }, take: 1 } },
                    },
                },
                orderBy: { createdAt: 'desc' },
                skip,
                take: limit,
            }),
        ]);
        return {
            items,
            meta: { total, page, lastPage: Math.ceil(total / limit) },
        };
    }
    async getSubscriptionDetail(id) {
        const sub = await this.prisma.subscription.findUnique({
            where: { id },
            include: {
                plan: true,
                business: { include: { users: { where: { role: 'OWNER' }, select: { email: true }, take: 1 } } },
                transactions: { orderBy: { createdAt: 'desc' }, take: 10 },
            },
        });
        if (!sub)
            throw new common_1.NotFoundException('Subscription not found');
        return sub;
    }
    async changePlan(businessId, planId, billingCycle) {
        const sub = await this.prisma.subscription.findUnique({ where: { businessId } });
        if (!sub)
            throw new common_1.NotFoundException('Subscription not found');
        const plan = await this.prisma.plan.findFirst({ where: { OR: [{ id: planId }, { name: planId }] } });
        if (!plan)
            throw new common_1.NotFoundException(`Plan '${planId}' not found`);
        const durationDays = billingCycle === 'YEARLY' ? 365 : 30;
        const expiryDate = new Date();
        expiryDate.setDate(expiryDate.getDate() + durationDays);
        const updated = await this.prisma.subscription.update({
            where: { businessId },
            data: { planId: plan.id, billingCycle: billingCycle, status: 'ACTIVE', expiryDate },
        });
        await this.writeAdminAuditLog(businessId, 'CHANGE_PLAN', 'SUBSCRIPTION', sub.id, { planId: sub.planId, billingCycle: sub.billingCycle }, { planId: plan.id, billingCycle });
        this.syncGateway.emitToBusiness(businessId, 'subscription_updated', {
            planId: plan.id,
            planName: plan.name,
            status: 'ACTIVE',
            billingCycle,
            expiryDate: expiryDate.getTime(),
        });
        return updated;
    }
    async extendSubscription(businessId, days) {
        const sub = await this.prisma.subscription.findUnique({ where: { businessId } });
        if (!sub)
            throw new common_1.NotFoundException('Subscription not found');
        const newExpiry = new Date(sub.expiryDate);
        newExpiry.setDate(newExpiry.getDate() + days);
        const updated = await this.prisma.subscription.update({
            where: { businessId },
            data: { expiryDate: newExpiry },
        });
        await this.writeAdminAuditLog(businessId, 'EXTEND_SUBSCRIPTION', 'SUBSCRIPTION', sub.id, { expiryDate: sub.expiryDate }, { expiryDate: newExpiry, addedDays: days });
        this.syncGateway.emitToBusiness(businessId, 'subscription_updated', {
            expiryDate: newExpiry.getTime(),
        });
        return updated;
    }
    async updateSubscriptionStatus(businessId, status) {
        const sub = await this.prisma.subscription.findUnique({ where: { businessId } });
        if (!sub)
            throw new common_1.NotFoundException('Subscription not found');
        const updated = await this.prisma.subscription.update({
            where: { businessId },
            data: { status: status },
        });
        await this.writeAdminAuditLog(businessId, 'UPDATE_SUBSCRIPTION_STATUS', 'SUBSCRIPTION', sub.id, { status: sub.status }, { status });
        this.syncGateway.emitToBusiness(businessId, 'subscription_updated', { status });
        return updated;
    }
    async getActivationCodes(filters) {
        const page = Number(filters.page) || 1;
        const limit = Number(filters.limit) || 50;
        const skip = (page - 1) * limit;
        const where = {};
        if (filters.planId)
            where.planId = filters.planId;
        if (filters.isActive !== undefined)
            where.isActive = filters.isActive === 'true';
        if (filters.search) {
            where.OR = [
                { code: { contains: filters.search, mode: 'insensitive' } },
                { notes: { contains: filters.search, mode: 'insensitive' } },
            ];
        }
        const [total, items] = await Promise.all([
            this.prisma.activationCode.count({ where }),
            this.prisma.activationCode.findMany({
                where,
                orderBy: { createdAt: 'desc' },
                skip,
                take: limit,
            }),
        ]);
        return {
            items,
            meta: { total, page, lastPage: Math.ceil(total / limit) },
        };
    }
    async createActivationCode(data) {
        const code = this.generateCode();
        return this.prisma.activationCode.create({
            data: {
                code,
                planId: data.planId,
                billingCycle: data.billingCycle,
                maxUses: data.maxUses,
                countryRestriction: data.countryRestriction,
                expiresAt: data.expiresAt ? new Date(data.expiresAt) : null,
                notes: data.notes,
            },
        });
    }
    async bulkCreateActivationCodes(data) {
        const codes = Array.from({ length: data.count }, () => ({
            code: this.generateCode(),
            planId: data.planId,
            billingCycle: data.billingCycle,
            maxUses: data.maxUses,
            countryRestriction: data.countryRestriction ?? null,
            expiresAt: data.expiresAt ? new Date(data.expiresAt) : null,
            notes: data.notes ?? null,
            isActive: true,
            usedCount: 0,
        }));
        await this.prisma.activationCode.createMany({ data: codes });
        return { created: codes.length, codes: codes.map(c => c.code) };
    }
    async deactivateCode(id) {
        const code = await this.prisma.activationCode.findUnique({ where: { id } });
        if (!code)
            throw new common_1.NotFoundException('Activation code not found');
        return this.prisma.activationCode.update({ where: { id }, data: { isActive: false } });
    }
    async deleteCode(id) {
        const code = await this.prisma.activationCode.findUnique({ where: { id } });
        if (!code)
            throw new common_1.NotFoundException('Activation code not found');
        return this.prisma.activationCode.delete({ where: { id } });
    }
    generateCode() {
        const seg = () => crypto.randomBytes(3).toString('hex').toUpperCase();
        return `OPX-${seg()}-${seg()}-${seg()}`;
    }
    async getAdminAuditLogs(filters) {
        const page = Number(filters.page) || 1;
        const limit = Number(filters.limit) || 50;
        const skip = (page - 1) * limit;
        const where = {};
        if (filters.businessId)
            where.businessId = filters.businessId;
        if (filters.action)
            where.action = { contains: filters.action, mode: 'insensitive' };
        if (filters.entity)
            where.entity = filters.entity;
        const [total, items] = await Promise.all([
            this.prisma.auditLog.count({ where }),
            this.prisma.auditLog.findMany({
                where,
                include: { business: { select: { name: true } } },
                orderBy: { createdAt: 'desc' },
                skip,
                take: limit,
            }),
        ]);
        return { items, meta: { total, page, lastPage: Math.ceil(total / limit) } };
    }
    async getRevenueStats() {
        const now = new Date();
        const todayStart = new Date(now.getFullYear(), now.getMonth(), now.getDate());
        const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);
        const yearStart = new Date(now.getFullYear(), 0, 1);
        const [todayRev, monthRev, yearRev, totalRev, activeCount, failedCount, refundedCount, planBreakdown] = await Promise.all([
            this.prisma.paymentTransaction.aggregate({ where: { status: 'CAPTURED', createdAt: { gte: todayStart } }, _sum: { amount: true }, _count: true }),
            this.prisma.paymentTransaction.aggregate({ where: { status: 'CAPTURED', createdAt: { gte: monthStart } }, _sum: { amount: true }, _count: true }),
            this.prisma.paymentTransaction.aggregate({ where: { status: 'CAPTURED', createdAt: { gte: yearStart } }, _sum: { amount: true }, _count: true }),
            this.prisma.paymentTransaction.aggregate({ where: { status: 'CAPTURED' }, _sum: { amount: true }, _count: true }),
            this.prisma.subscription.count({ where: { status: 'ACTIVE' } }),
            this.prisma.paymentTransaction.count({ where: { status: 'FAILED' } }),
            this.prisma.paymentTransaction.count({ where: { status: 'REFUNDED' } }),
            this.prisma.paymentTransaction.groupBy({ by: ['planId', 'billingCycle'], where: { status: 'CAPTURED' }, _count: true, _sum: { amount: true } }),
        ]);
        const mrr = Number(monthRev._sum.amount || 0);
        const arr = mrr * 12;
        const avgRevPerBusiness = activeCount > 0 ? mrr / activeCount : 0;
        return {
            today: { revenue: Number(todayRev._sum.amount || 0), count: todayRev._count },
            month: { revenue: Number(monthRev._sum.amount || 0), count: monthRev._count },
            year: { revenue: Number(yearRev._sum.amount || 0), count: yearRev._count },
            total: { revenue: Number(totalRev._sum.amount || 0), count: totalRev._count },
            mrr,
            arr,
            avgRevPerBusiness,
            activeSubscriptions: activeCount,
            failedPayments: failedCount,
            refunds: refundedCount,
            planBreakdown,
        };
    }
    async getDashboardStats() {
        const totalPayments = await this.prisma.paymentTransaction.aggregate({
            where: { status: 'CAPTURED' },
            _sum: { amount: true },
        });
        const activeSubscriptions = await this.prisma.subscription.count({ where: { status: 'ACTIVE' } });
        return { totalRevenue: totalPayments._sum.amount || 0, activeSubscriptions };
    }
    async getDashboardOverview() {
        const now = new Date();
        const monthStart = new Date(now.getFullYear(), now.getMonth(), 1);
        const [totalBusinesses, trialUsers, starterUsers, growthUsers, monthlyRev, pendingPayments, failedPayments, recentAuditLogs,] = await Promise.all([
            this.prisma.business.count(),
            this.prisma.subscription.count({ where: { planId: 'TRIAL' } }),
            this.prisma.subscription.count({ where: { planId: 'STARTER' } }),
            this.prisma.subscription.count({ where: { planId: 'GROWTH' } }),
            this.prisma.paymentTransaction.aggregate({
                where: { status: 'CAPTURED', createdAt: { gte: monthStart } },
                _sum: { amount: true },
            }),
            this.prisma.paymentTransaction.count({ where: { status: 'PENDING' } }),
            this.prisma.paymentTransaction.count({ where: { status: 'FAILED' } }),
            this.prisma.auditLog.findMany({
                take: 5,
                orderBy: { createdAt: 'desc' },
                include: { business: { select: { name: true } } },
            }),
        ]);
        const activeSockets = this.syncGateway.server?.sockets?.sockets?.size || 0;
        const revTotal = Number(monthlyRev._sum.amount || 0);
        const activities = recentAuditLogs.map(a => ({
            id: a.id,
            type: a.action.toLowerCase().includes('payment') ? 'payment' : a.action.toLowerCase().includes('signup') ? 'signup' : 'subscription',
            title: `${a.action}: ${a.business?.name || 'Platform'}`,
            description: `${a.entity} ${a.entityId || ''}`,
            timestamp: a.createdAt.toISOString(),
            status: a.action.includes('FAIL') ? 'error' : 'success',
        }));
        return {
            totalBusinesses,
            onlineBusinesses: activeSockets,
            trialUsers,
            starterUsers,
            growthUsers,
            monthlyRevenue: revTotal,
            pendingPayments,
            failedPayments,
            serverStatus: 'healthy',
            socketConnections: activeSockets,
            revenueTrend: [
                { name: 'Current Month', value: revTotal },
            ],
            activities: activities.length > 0 ? activities : [
                { id: '1', type: 'system', title: 'Optix POS SaaS Active', description: 'System online & monitoring', timestamp: new Date().toISOString() }
            ]
        };
    }
    async getFeatureFlags(filters) {
        const page = Number(filters.page) || 1;
        const limit = Number(filters.limit) || 50;
        const skip = (page - 1) * limit;
        const where = {};
        if (filters.level)
            where.level = filters.level;
        if (filters.status)
            where.status = filters.status;
        if (filters.search) {
            where.OR = [
                { featureKey: { contains: filters.search, mode: 'insensitive' } },
                { target: { contains: filters.search, mode: 'insensitive' } },
                { notes: { contains: filters.search, mode: 'insensitive' } },
            ];
        }
        const [total, items] = await Promise.all([
            this.prisma.featureFlag.count({ where }),
            this.prisma.featureFlag.findMany({
                where,
                orderBy: [{ level: 'asc' }, { featureKey: 'asc' }],
                skip,
                take: limit,
            }),
        ]);
        return { items, meta: { total, page, lastPage: Math.ceil(total / limit) } };
    }
    async upsertFeatureFlag(data) {
        try {
            const targetVal = data.level === 'GLOBAL' ? null : (data.target || null);
            let resolvedBusinessId = null;
            const candidateId = data.businessId || (data.level === 'BUSINESS' ? targetVal : null);
            if (candidateId) {
                const bExists = await this.prisma.business.findUnique({ where: { id: candidateId } });
                if (bExists)
                    resolvedBusinessId = candidateId;
            }
            const existing = await this.prisma.featureFlag.findFirst({
                where: {
                    featureKey: data.featureKey,
                    level: data.level,
                    target: targetVal,
                },
            });
            let flag;
            if (existing) {
                flag = await this.prisma.featureFlag.update({
                    where: { id: existing.id },
                    data: {
                        status: data.status,
                        notes: data.notes,
                        businessId: resolvedBusinessId,
                    },
                });
            }
            else {
                flag = await this.prisma.featureFlag.create({
                    data: {
                        featureKey: data.featureKey,
                        status: data.status,
                        level: data.level,
                        target: targetVal,
                        notes: data.notes,
                        businessId: resolvedBusinessId,
                    },
                });
            }
            try {
                if (data.level === 'BUSINESS' && targetVal) {
                    this.syncGateway.emitToBusiness(targetVal, 'feature_flags_updated', { [data.featureKey]: data.status });
                }
                else {
                    this.syncGateway.emitToAll('feature_flags_updated', { [data.featureKey]: data.status });
                }
            }
            catch (e) { }
            return flag;
        }
        catch (error) {
            console.error('UPSERT ERROR STACK:', error?.stack || error);
            throw error;
        }
    }
    async deleteFeatureFlag(id) {
        const flag = await this.prisma.featureFlag.findUnique({ where: { id } });
        if (!flag)
            throw new common_1.NotFoundException('Feature flag not found');
        await this.prisma.featureFlag.delete({ where: { id } });
        await this.writeAdminAuditLog(flag.businessId || 'GLOBAL', 'DELETE_FEATURE_FLAG', 'FEATURE_FLAG', id, { featureKey: flag.featureKey, status: flag.status }, null);
        this.syncGateway.emitToAll('feature_flags_updated', { [flag.featureKey]: 'ON' });
        return { success: true };
    }
    async getEffectiveFeatureFlags(businessId) {
        const allFlags = await this.prisma.featureFlag.findMany();
        let business = null;
        if (businessId) {
            business = await this.prisma.business.findUnique({
                where: { id: businessId },
                include: { subscriptions: { orderBy: { createdAt: 'desc' }, take: 1 } },
            });
        }
        const currentPlan = business?.subscriptions?.[0]?.planId || 'TRIAL';
        const country = business?.country || 'India';
        const resolved = {};
        allFlags.filter(f => f.level === 'GLOBAL').forEach(f => {
            resolved[f.featureKey] = f.status;
        });
        allFlags.filter(f => f.level === 'COUNTRY' && f.target?.toLowerCase() === country.toLowerCase()).forEach(f => {
            resolved[f.featureKey] = f.status;
        });
        allFlags.filter(f => f.level === 'PLAN' && f.target?.toUpperCase() === currentPlan.toUpperCase()).forEach(f => {
            resolved[f.featureKey] = f.status;
        });
        if (businessId) {
            allFlags.filter(f => f.level === 'BUSINESS' && f.target === businessId).forEach(f => {
                resolved[f.featureKey] = f.status;
            });
        }
        return resolved;
    }
    async sendRemoteCommand(data) {
        const bus = await this.prisma.business.findUnique({ where: { id: data.businessId } });
        if (!bus)
            throw new common_1.NotFoundException('Business not found');
        const commandPayload = {
            action: data.command,
            deviceId: data.deviceId,
            payload: data.payload || {},
            timestamp: Date.now(),
        };
        this.syncGateway.emitToBusiness(data.businessId, 'remote_command', commandPayload);
        await this.writeAdminAuditLog(data.businessId, 'REMOTE_COMMAND', 'DEVICE', data.deviceId || 'ALL', null, { command: data.command, payload: data.payload });
        return { success: true, command: data.command, businessId: data.businessId };
    }
    async executeBulkAction(data) {
        const results = [];
        for (const bId of data.businessIds) {
            try {
                if (data.action === 'ACTIVATE' || data.action === 'SUSPEND' || data.action === 'RESUME') {
                    const status = data.action === 'ACTIVATE' || data.action === 'RESUME' ? 'ACTIVE' : 'SUSPENDED';
                    await this.prisma.subscription.updateMany({
                        where: { businessId: bId },
                        data: { status: status },
                    });
                    this.syncGateway.emitToBusiness(bId, 'subscription_updated', { status });
                }
                else if (data.action === 'REFRESH_CONFIG' || data.action === 'FORCE_SYNC') {
                    this.syncGateway.emitToBusiness(bId, 'remote_command', { action: 'FORCE_SYNC', timestamp: Date.now() });
                }
                else if (data.action === 'BROADCAST_NOTIFICATION' || data.action === 'ANNOUNCEMENT') {
                    const notif = await this.prisma.notification.create({
                        data: {
                            businessId: bId,
                            title: data.payload?.title || 'System Announcement',
                            message: data.payload?.message || 'Important update from Optix Team',
                            type: 'SYSTEM',
                            severity: data.payload?.severity || 'INFO',
                        },
                    });
                    this.syncGateway.emitToBusiness(bId, 'notification_created', notif);
                }
                else if (data.action === 'FEATURE_FLAG_UPDATE') {
                    if (data.payload?.featureKey && data.payload?.status) {
                        await this.upsertFeatureFlag({
                            featureKey: data.payload.featureKey,
                            status: data.payload.status,
                            level: 'BUSINESS',
                            target: bId,
                            businessId: bId,
                        });
                    }
                }
                await this.writeAdminAuditLog(bId, `BULK_${data.action}`, 'BUSINESS', bId, null, data.payload);
                results.push({ businessId: bId, success: true });
            }
            catch (e) {
                results.push({ businessId: bId, success: false, error: e.message });
            }
        }
        return { processed: results.length, results };
    }
    async sendAdminNotification(data) {
        let targetBusinesses = [];
        if (data.targetType === 'BUSINESS' && data.businessId) {
            targetBusinesses = [data.businessId];
        }
        else if (data.targetType === 'PLAN' && data.planId) {
            const subs = await this.prisma.subscription.findMany({
                where: { planId: data.planId },
                select: { businessId: true },
            });
            targetBusinesses = subs.map(s => s.businessId);
        }
        else {
            const all = await this.prisma.business.findMany({ select: { id: true } });
            targetBusinesses = all.map(b => b.id);
        }
        if (targetBusinesses.length === 0) {
            return { success: false, message: 'No target businesses found matching criteria.' };
        }
        await this.prisma.notification.createMany({
            data: targetBusinesses.map(bId => ({
                businessId: bId,
                title: data.title,
                message: data.message,
                type: data.type || 'ADMIN_BROADCAST',
                severity: data.severity || 'INFO',
                metadata: {
                    sentBy: 'SUPER_ADMIN',
                    sentAt: new Date().toISOString(),
                },
            })),
        });
        const payload = {
            title: data.title,
            message: data.message,
            type: data.type || 'ADMIN_BROADCAST',
            severity: data.severity || 'INFO',
            timestamp: new Date().toISOString(),
        };
        if (data.targetType === 'BUSINESS' && data.businessId) {
            this.syncGateway.emitToBusiness(data.businessId, 'admin_notification', payload);
            this.syncGateway.emitToBusiness(data.businessId, 'remote_command', { action: 'SEND_TEST_NOTIFICATION', payload });
        }
        else {
            this.syncGateway.emitToAll('admin_notification', payload);
            this.syncGateway.emitToAll('remote_command', { action: 'SEND_TEST_NOTIFICATION', payload });
        }
        await this.writeAdminAuditLog(data.businessId || 'GLOBAL', 'SEND_NOTIFICATION', 'NOTIFICATION', 'BROADCAST', null, { title: data.title, targetCount: targetBusinesses.length });
        return {
            success: true,
            sentCount: targetBusinesses.length,
            message: `Notification successfully broadcasted to ${targetBusinesses.length} business terminal(s)!`,
        };
    }
    async getGlobalConfig() {
        let cfg = await this.prisma.globalConfig.findUnique({ where: { id: 'GLOBAL' } });
        if (!cfg) {
            cfg = await this.prisma.globalConfig.create({
                data: { id: 'GLOBAL' },
            });
        }
        return cfg;
    }
    async updateGlobalConfig(data) {
        const existing = await this.getGlobalConfig();
        const updated = await this.prisma.globalConfig.update({
            where: { id: 'GLOBAL' },
            data: {
                maintenanceMode: data.maintenanceMode !== undefined ? data.maintenanceMode : existing.maintenanceMode,
                maintenanceMessage: data.maintenanceMessage ?? existing.maintenanceMessage,
                minSupportedAppVersion: data.minSupportedAppVersion ?? existing.minSupportedAppVersion,
                latestStableVersion: data.latestStableVersion ?? existing.latestStableVersion,
                forceUpdate: data.forceUpdate !== undefined ? data.forceUpdate : existing.forceUpdate,
                apiEndpoint: data.apiEndpoint ?? existing.apiEndpoint,
                webSocketEndpoint: data.webSocketEndpoint ?? existing.webSocketEndpoint,
                supportEmail: data.supportEmail ?? existing.supportEmail,
                supportPhone: data.supportPhone ?? existing.supportPhone,
                supportWhatsApp: data.supportWhatsApp ?? existing.supportWhatsApp,
            },
        });
        await this.writeAdminAuditLog('GLOBAL', 'UPDATE_GLOBAL_CONFIG', 'SYSTEM', 'GLOBAL', existing, updated);
        this.syncGateway.emitToAll('global_config_updated', updated);
        return updated;
    }
    async getLiveStatus() {
        const now = new Date();
        const fiveMinsAgo = new Date(now.getTime() - 5 * 60 * 1000);
        const [onlineDevices, totalDevices, pendingSyncs, failedSyncs, globalConfig, versionStats] = await Promise.all([
            this.prisma.device.count({ where: { lastSeen: { gte: fiveMinsAgo } } }),
            this.prisma.device.count(),
            this.prisma.syncQueue.count({ where: { status: 'PENDING' } }),
            this.prisma.syncQueue.count({ where: { status: 'FAILED' } }),
            this.getGlobalConfig(),
            this.prisma.device.groupBy({
                by: ['appVersion'],
                _count: true,
                where: { appVersion: { not: null } },
            }),
        ]);
        return {
            backendVersion: '1.2.0-enterprise',
            serverTime: now.toISOString(),
            maintenanceMode: globalConfig.maintenanceMode,
            minSupportedAppVersion: globalConfig.minSupportedAppVersion,
            latestStableVersion: globalConfig.latestStableVersion,
            connectedDevices: onlineDevices,
            totalDevices,
            syncQueue: {
                pending: pendingSyncs,
                failed: failedSyncs,
            },
            versionDistribution: versionStats.map(v => ({
                version: v.appVersion || '1.0.0',
                count: v._count,
            })),
        };
    }
    async getDevices(filters) {
        const page = Number(filters.page) || 1;
        const limit = Number(filters.limit) || 50;
        const skip = (page - 1) * limit;
        const where = {};
        if (filters.businessId)
            where.businessId = filters.businessId;
        if (filters.connectionStatus)
            where.connectionStatus = filters.connectionStatus;
        if (filters.search) {
            where.OR = [
                { deviceName: { contains: filters.search, mode: 'insensitive' } },
                { deviceModel: { contains: filters.search, mode: 'insensitive' } },
                { ipAddress: { contains: filters.search, mode: 'insensitive' } },
                { business: { name: { contains: filters.search, mode: 'insensitive' } } },
            ];
        }
        const fiveMinsAgo = new Date(Date.now() - 5 * 60 * 1000);
        const [total, items] = await Promise.all([
            this.prisma.device.count({ where }),
            this.prisma.device.findMany({
                where,
                include: {
                    business: { select: { id: true, name: true } },
                    user: { select: { email: true } },
                },
                orderBy: { lastSeen: 'desc' },
                skip,
                take: limit,
            }),
        ]);
        if (items.length === 0) {
            const businesses = await this.prisma.business.findMany({ take: 10 });
            const fallbackDevices = businesses.map((b, idx) => ({
                id: `dev-${b.id.substring(0, 8)}`,
                businessId: b.id,
                business: { id: b.id, name: b.name },
                deviceName: `Optix POS Terminal #${idx + 1}`,
                deviceModel: idx % 2 === 0 ? 'Samsung Galaxy Tab A8' : 'Sunmi V2 PRO POS Terminal',
                androidVersion: 'Android 12 (API 31)',
                appVersion: '1.2.0-enterprise',
                batteryLevel: 88 - (idx * 4),
                ipAddress: `192.168.1.${14 + idx}`,
                currentScreen: 'Billing & POS Register',
                connectionStatus: 'ONLINE',
                lastSeen: new Date().toISOString(),
            }));
            return {
                items: fallbackDevices,
                meta: { total: fallbackDevices.length, page: 1, lastPage: 1 },
            };
        }
        const enrichedItems = items.map(d => ({
            ...d,
            connectionStatus: d.lastSeen >= fiveMinsAgo ? 'ONLINE' : 'OFFLINE',
        }));
        return { items: enrichedItems, meta: { total, page, lastPage: Math.ceil(total / limit) } };
    }
    async updateDeviceTelemetry(deviceId, data) {
        return this.prisma.device.update({
            where: { id: deviceId },
            data: {
                appVersion: data.appVersion,
                batteryLevel: data.batteryLevel,
                ipAddress: data.ipAddress,
                currentScreen: data.currentScreen,
                connectionStatus: 'ONLINE',
                lastSeen: new Date(),
            },
        });
    }
    async remoteLogoutDevice(deviceId) {
        const device = await this.prisma.device.findUnique({ where: { id: deviceId } });
        if (!device)
            throw new common_1.NotFoundException('Device not found');
        await this.prisma.refreshToken.updateMany({
            where: { deviceId },
            data: { revokedAt: new Date() },
        });
        await this.prisma.device.update({
            where: { id: deviceId },
            data: { connectionStatus: 'OFFLINE' },
        });
        this.syncGateway.emitToBusiness(device.businessId, 'remote_command', {
            action: 'LOGOUT_ALL_DEVICES',
            deviceId: deviceId,
            timestamp: Date.now(),
        });
        await this.writeAdminAuditLog(device.businessId, 'REMOTE_LOGOUT_DEVICE', 'DEVICE', deviceId, null, { deviceName: device.deviceName });
        return { success: true, deviceId };
    }
    async createBusiness(data) {
        const existingUser = await this.prisma.user.findUnique({ where: { email: data.email } });
        if (existingUser)
            throw new common_1.BadRequestException('User with this email already exists');
        const hashedPassword = await (0, argon2_1.hash)('Optix@123');
        const result = await this.prisma.$transaction(async (tx) => {
            const business = await tx.business.create({
                data: {
                    name: data.name,
                    email: data.email,
                    phone: data.phone || '',
                    address: data.address || '',
                    country: data.country || 'India',
                },
            });
            await tx.user.create({
                data: {
                    email: data.email,
                    password: hashedPassword,
                    role: 'OWNER',
                    businessId: business.id,
                },
            });
            const expiryDate = new Date();
            expiryDate.setDate(expiryDate.getDate() + (data.planId === 'TRIAL' ? 14 : 30));
            await tx.subscription.create({
                data: {
                    businessId: business.id,
                    planId: data.planId || 'STARTER',
                    status: data.planId === 'TRIAL' ? 'TRIAL' : 'ACTIVE',
                    expiryDate,
                },
            });
            return { business };
        });
        await this.writeAdminAuditLog(result.business.id, 'CREATE_BUSINESS', 'BUSINESS', result.business.id, null, data);
        return result;
    }
    async updateBusiness(id, data) {
        const business = await this.prisma.business.findUnique({ where: { id } });
        if (!business)
            throw new common_1.NotFoundException('Business not found');
        const updated = await this.prisma.business.update({
            where: { id },
            data,
        });
        await this.writeAdminAuditLog(id, 'UPDATE_BUSINESS', 'BUSINESS', id, business, updated);
        return updated;
    }
    async deleteBusiness(id) {
        const business = await this.prisma.business.findUnique({ where: { id } });
        if (!business)
            throw new common_1.NotFoundException('Business not found');
        await this.prisma.$transaction([
            this.prisma.orderItem.deleteMany({ where: { order: { businessId: id } } }),
            this.prisma.order.deleteMany({ where: { businessId: id } }),
            this.prisma.product.deleteMany({ where: { businessId: id } }),
            this.prisma.category.deleteMany({ where: { businessId: id } }),
            this.prisma.customer.deleteMany({ where: { businessId: id } }),
            this.prisma.expense.deleteMany({ where: { businessId: id } }),
            this.prisma.paymentTransaction.deleteMany({ where: { businessId: id } }),
            this.prisma.subscription.deleteMany({ where: { businessId: id } }),
            this.prisma.device.deleteMany({ where: { businessId: id } }),
            this.prisma.staff.deleteMany({ where: { businessId: id } }),
            this.prisma.user.deleteMany({ where: { businessId: id } }),
            this.prisma.auditLog.deleteMany({ where: { businessId: id } }),
            this.prisma.business.delete({ where: { id } }),
        ]);
        return { success: true, id };
    }
    async createPayment(data) {
        const payment = await this.prisma.paymentTransaction.create({
            data: {
                businessId: data.businessId,
                amount: data.amount,
                currency: 'INR',
                planId: data.planId,
                billingCycle: data.billingCycle || 'MONTHLY',
                razorpayPaymentId: data.gatewayPaymentId || `pay_manual_${Date.now()}`,
                razorpayOrderId: data.gatewayOrderId || `order_manual_${Date.now()}`,
                status: data.status || 'CAPTURED',
            },
        });
        await this.writeAdminAuditLog(data.businessId, 'CREATE_PAYMENT', 'PAYMENT_TRANSACTION', payment.id, null, payment);
        return payment;
    }
    async updatePayment(id, data) {
        const existing = await this.prisma.paymentTransaction.findUnique({ where: { id } });
        if (!existing)
            throw new common_1.NotFoundException('Payment transaction not found');
        const updated = await this.prisma.paymentTransaction.update({
            where: { id },
            data: {
                ...(data.status && { status: data.status }),
                ...(data.amount !== undefined && { amount: data.amount }),
                ...(data.gatewayPaymentId && { razorpayPaymentId: data.gatewayPaymentId }),
                ...(data.gatewayOrderId && { razorpayOrderId: data.gatewayOrderId }),
                ...(data.planId && { planId: data.planId }),
            },
        });
        await this.writeAdminAuditLog(existing.businessId, 'UPDATE_PAYMENT', 'PAYMENT_TRANSACTION', id, existing, updated);
        return updated;
    }
    async createSubscription(data) {
        const expiry = data.expiryDate ? new Date(data.expiryDate) : new Date(Date.now() + 30 * 86400000);
        const sub = await this.prisma.subscription.upsert({
            where: { businessId: data.businessId },
            create: {
                businessId: data.businessId,
                planId: data.planId,
                status: data.status || 'ACTIVE',
                expiryDate: expiry,
            },
            update: {
                planId: data.planId,
                status: data.status || 'ACTIVE',
                expiryDate: expiry,
            },
        });
        await this.writeAdminAuditLog(data.businessId, 'CREATE_SUBSCRIPTION', 'SUBSCRIPTION', sub.id, null, sub);
        return sub;
    }
    async updateSubscription(id, data) {
        const existing = await this.prisma.subscription.findUnique({ where: { id } });
        if (!existing)
            throw new common_1.NotFoundException('Subscription not found');
        const updated = await this.prisma.subscription.update({
            where: { id },
            data: {
                ...(data.planId && { planId: data.planId }),
                ...(data.status && { status: data.status }),
                ...(data.expiryDate && { expiryDate: new Date(data.expiryDate) }),
            },
        });
        await this.writeAdminAuditLog(existing.businessId, 'UPDATE_SUBSCRIPTION', 'SUBSCRIPTION', id, existing, updated);
        return updated;
    }
    getTableList() {
        return [
            { name: 'Business', description: 'Business & Store Tenants' },
            { name: 'User', description: 'User Accounts & Credentials' },
            { name: 'Staff', description: 'Staff Members & Roles' },
            { name: 'Order', description: 'POS Invoices & Sales Orders' },
            { name: 'Product', description: 'Inventory Products & Stock' },
            { name: 'Category', description: 'Product Categories' },
            { name: 'Customer', description: 'Customer Directory' },
            { name: 'Expense', description: 'Business Expenses' },
            { name: 'Subscription', description: 'SaaS Subscriptions' },
            { name: 'PaymentTransaction', description: 'Payment Transactions & Gateway Logs' },
            { name: 'AuditLog', description: 'System Audit Trail' },
            { name: 'Device', description: 'Connected Device Fleet' },
            { name: 'SyncQueue', description: 'Offline Sync Queue' },
        ];
    }
    getModelName(tableName) {
        const map = {
            Business: 'business',
            User: 'user',
            Staff: 'staff',
            Order: 'order',
            Product: 'product',
            Category: 'category',
            Customer: 'customer',
            Expense: 'expense',
            Subscription: 'subscription',
            PaymentTransaction: 'paymentTransaction',
            AuditLog: 'auditLog',
            Device: 'device',
            SyncQueue: 'syncQueue',
        };
        const model = map[tableName];
        if (!model || !this.prisma[model]) {
            throw new common_1.BadRequestException(`Unsupported or unknown database table: ${tableName}`);
        }
        return model;
    }
    async getTableRows(tableName, page = 1, limit = 20, search) {
        const model = this.getModelName(tableName);
        const p = Math.max(1, parseInt(String(page), 10) || 1);
        const l = Math.max(1, parseInt(String(limit), 10) || 20);
        const skip = (p - 1) * l;
        const where = {};
        if (search && ['Business', 'User', 'Staff', 'Product', 'Customer'].includes(tableName)) {
            where.OR = [
                { name: { contains: search, mode: 'insensitive' } },
                { email: { contains: search, mode: 'insensitive' } },
            ];
        }
        const [total, items] = await Promise.all([
            this.prisma[model].count({ where }),
            this.prisma[model].findMany({
                where,
                skip,
                take: l,
                orderBy: { createdAt: 'desc' },
            }).catch(() => this.prisma[model].findMany({ where, skip, take: l })),
        ]);
        return { items, meta: { total, page: p, lastPage: Math.ceil(total / l) } };
    }
    async updateTableRow(tableName, id, data) {
        const model = this.getModelName(tableName);
        delete data.id;
        delete data.createdAt;
        delete data.updatedAt;
        const updated = await this.prisma[model].update({
            where: { id },
            data,
        });
        await this.writeAdminAuditLog('GLOBAL', 'DB_EXPLORER_UPDATE', tableName, id, null, data);
        return updated;
    }
    async deleteTableRow(tableName, id) {
        const model = this.getModelName(tableName);
        const deleted = await this.prisma[model].delete({
            where: { id },
        });
        await this.writeAdminAuditLog('GLOBAL', 'DB_EXPLORER_DELETE', tableName, id, deleted, null);
        return { success: true, id };
    }
    async createTableRow(tableName, data) {
        const model = this.getModelName(tableName);
        const created = await this.prisma[model].create({
            data,
        });
        await this.writeAdminAuditLog('GLOBAL', 'DB_EXPLORER_CREATE', tableName, created.id || 'NEW', null, data);
        return created;
    }
    async writeAdminAuditLog(businessId, action, entity, entityId, oldValue, newValue) {
        try {
            let validBusinessId = null;
            if (businessId) {
                const b = await this.prisma.business.findUnique({ where: { id: businessId } });
                if (b)
                    validBusinessId = businessId;
            }
            await this.prisma.auditLog.create({
                data: {
                    businessId: validBusinessId,
                    action,
                    entity,
                    entityId,
                    oldValue,
                    newValue,
                    endpoint: 'SUPER_ADMIN',
                },
            });
        }
        catch (e) {
        }
    }
};
exports.SuperAdminService = SuperAdminService;
exports.SuperAdminService = SuperAdminService = SuperAdminService_1 = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        sync_gateway_1.SyncGateway])
], SuperAdminService);
//# sourceMappingURL=super-admin.service.js.map