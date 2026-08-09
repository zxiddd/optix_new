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
var OrdersService_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.OrdersService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
const sync_gateway_1 = require("../sync/sync.gateway");
let OrdersService = OrdersService_1 = class OrdersService {
    constructor(prisma, syncGateway) {
        this.prisma = prisma;
        this.syncGateway = syncGateway;
        this.logger = new common_1.Logger(OrdersService_1.name);
    }
    async getOrders(businessId) {
        return this.prisma.order.findMany({
            where: { businessId },
            include: { items: true },
            orderBy: { createdAt: 'desc' },
        });
    }
    async saveOrder(businessId, dto) {
        this.logger.log(`[BACKEND ORDER RECEIVED] Business ID: ${businessId}, Temp Token: ${dto.tokenNumber}, Invoice: ${dto.invoiceNumber}`);
        const savedOrder = await this.prisma.$transaction(async (tx) => {
            const business = await tx.business.findUnique({
                where: { id: businessId },
                select: { tokenCounter: true, lastTokenResetDateTime: true },
            });
            const now = new Date();
            let newTokenCounter = (business?.tokenCounter || 0) + 1;
            if (business?.lastTokenResetDateTime) {
                const lastReset = new Date(business.lastTokenResetDateTime);
                if (now.getFullYear() !== lastReset.getFullYear() ||
                    now.getMonth() !== lastReset.getMonth() ||
                    now.getDate() !== lastReset.getDate()) {
                    newTokenCounter = 1;
                }
            }
            await tx.business.update({
                where: { id: businessId },
                data: {
                    tokenCounter: newTokenCounter,
                    lastTokenResetDateTime: now,
                },
            });
            const generatedTokenNumber = String(newTokenCounter).padStart(3, '0');
            const pmInput = (dto.paymentMethod || 'CASH').toUpperCase();
            const validPaymentMethod = ['CASH', 'CARD', 'UPI', 'NET_BANKING', 'OTHER', 'CREDIT'].includes(pmInput)
                ? pmInput
                : 'CASH';
            const orderData = {
                tokenNumber: generatedTokenNumber,
                invoiceNumber: dto.invoiceNumber || `INV-${Date.now()}`,
                status: dto.status || 'PAID',
                subtotal: dto.subtotal || dto.total,
                discount: dto.discount || 0,
                tax: dto.tax || 0,
                total: dto.total,
                paymentMethod: validPaymentMethod,
                cashierName: dto.cashierName || 'Admin',
                customerName: dto.customerName,
                customerId: dto.customerId,
            };
            if (dto.id) {
                return tx.order.upsert({
                    where: { id: dto.id },
                    update: orderData,
                    create: {
                        id: dto.id,
                        businessId,
                        ...orderData,
                        items: {
                            create: dto.items?.map((item) => ({
                                productId: item.productId || item.id || require('crypto').randomUUID(),
                                productName: item.name || item.productName || 'Item',
                                price: item.price,
                                quantity: item.quantity,
                                weight: item.weight,
                                unit: item.unit,
                            })),
                        },
                    },
                    include: { items: true },
                });
            }
            const res = await tx.order.create({
                data: {
                    businessId,
                    ...orderData,
                    items: {
                        create: dto.items?.map((item) => ({
                            productId: item.productId || item.id || require('crypto').randomUUID(),
                            productName: item.name || item.productName || 'Item',
                            price: item.price,
                            quantity: item.quantity,
                            weight: item.weight,
                            unit: item.unit,
                        })),
                    },
                },
                include: { items: true },
            });
            this.logger.log(`[BACKEND OFFICIAL TOKEN ASSIGNED] Temp Token: ${dto.tokenNumber} -> Official Atomic Token: ${res.tokenNumber}`);
            return res;
        });
        this.syncGateway.emitToBusiness(businessId, 'order.created', savedOrder);
        try {
            const act = await this.prisma.staffActivityLog.create({
                data: {
                    staffId: dto.staffId || undefined,
                    businessId,
                    action: 'BILL_CREATED',
                    entityType: 'ORDER',
                    entityId: savedOrder.id,
                    metadata: { total: savedOrder.total, token: savedOrder.tokenNumber },
                    severity: 'NORMAL',
                },
            });
            this.syncGateway.emitToBusiness(businessId, 'staff.activity.created', act);
        }
        catch (e) { }
        return savedOrder;
    }
    async deleteOrder(businessId, id) {
        const res = await this.prisma.order.deleteMany({
            where: { id, businessId },
        });
        this.syncGateway.emitToBusiness(businessId, 'order.deleted', { id });
        try {
            const act = await this.prisma.staffActivityLog.create({
                data: {
                    staffId: undefined,
                    businessId,
                    action: 'BILL_CANCELLED',
                    entityType: 'ORDER',
                    entityId: id,
                    isSuspicious: true,
                    severity: 'WARNING',
                },
            });
            this.syncGateway.emitToBusiness(businessId, 'staff.activity.created', act);
        }
        catch (e) { }
        return res;
    }
};
exports.OrdersService = OrdersService;
exports.OrdersService = OrdersService = OrdersService_1 = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        sync_gateway_1.SyncGateway])
], OrdersService);
//# sourceMappingURL=orders.service.js.map