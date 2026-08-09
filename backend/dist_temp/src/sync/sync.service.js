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
exports.SyncService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
let SyncService = class SyncService {
    constructor(prisma) {
        this.prisma = prisma;
    }
    async push(businessId, dto) {
        return await this.prisma.$transaction(async (tx) => {
            if (dto.categories) {
                for (const cat of dto.categories) {
                    await tx.category.upsert({
                        where: { id: cat.id },
                        update: {
                            name: cat.name,
                            sortOrder: cat.sortOrder,
                            version: cat.version,
                            isDeleted: cat.isDeleted,
                            lastModified: cat.lastModified ? new Date(cat.lastModified) : new Date(),
                        },
                        create: {
                            id: cat.id,
                            businessId,
                            name: cat.name,
                            sortOrder: cat.sortOrder,
                            version: cat.version,
                            isDeleted: cat.isDeleted,
                            lastModified: cat.lastModified ? new Date(cat.lastModified) : new Date(),
                        },
                    });
                }
            }
            if (dto.products) {
                for (const prod of dto.products) {
                    await tx.product.upsert({
                        where: { id: prod.id },
                        update: {
                            name: prod.name,
                            description: prod.description,
                            barcode: prod.barcode,
                            sku: prod.sku,
                            price: prod.price,
                            pricingType: prod.pricingType || 'FIXED',
                            unit: prod.unit || 'Piece',
                            categoryId: prod.categoryId,
                            imageUrl: prod.imageUrl,
                            isOutOfStock: prod.isOutOfStock,
                            version: prod.version,
                            isDeleted: prod.isDeleted,
                            lastModified: prod.lastModified ? new Date(prod.lastModified) : new Date(),
                        },
                        create: {
                            id: prod.id,
                            businessId,
                            name: prod.name,
                            description: prod.description,
                            barcode: prod.barcode,
                            sku: prod.sku,
                            price: prod.price,
                            pricingType: prod.pricingType || 'FIXED',
                            unit: prod.unit || 'Piece',
                            categoryId: prod.categoryId,
                            imageUrl: prod.imageUrl,
                            isOutOfStock: prod.isOutOfStock,
                            version: prod.version,
                            isDeleted: prod.isDeleted,
                            lastModified: prod.lastModified ? new Date(prod.lastModified) : new Date(),
                        },
                    });
                }
            }
            if (dto.paymentQrs) {
                for (const qr of dto.paymentQrs) {
                    if (qr.isDeleted) {
                        await tx.paymentQr.deleteMany({
                            where: { id: qr.id, businessId },
                        });
                    }
                    else {
                        await tx.paymentQr.upsert({
                            where: { id: qr.id },
                            update: {
                                name: qr.name,
                                imageUrl: qr.imageUrl,
                                isActive: qr.isActive ?? true,
                            },
                            create: {
                                id: qr.id,
                                businessId,
                                name: qr.name,
                                imageUrl: qr.imageUrl,
                                isActive: qr.isActive ?? true,
                            },
                        });
                    }
                }
            }
            return { status: 'success', timestamp: Date.now() };
        });
    }
    async pull(businessId, lastSyncTimestamp) {
        const lastDate = new Date(Number(lastSyncTimestamp) || 0);
        const [business, subscription, categories, products, orders, staff, paymentQrs, customers, expenses] = await Promise.all([
            this.prisma.business.findUnique({
                where: { id: businessId },
                include: { settings: true, receiptSettings: true, printerSettings: true },
            }),
            this.prisma.subscription.findFirst({
                where: { businessId },
                include: { plan: true },
                orderBy: { createdAt: 'desc' },
            }),
            this.prisma.category.findMany({
                where: {
                    businessId,
                    lastModified: { gt: lastDate },
                },
            }),
            this.prisma.product.findMany({
                where: {
                    businessId,
                    lastModified: { gt: lastDate },
                },
            }),
            this.prisma.order.findMany({
                where: {
                    businessId,
                    createdAt: { gt: lastDate },
                },
                include: { items: true },
                orderBy: { createdAt: 'desc' },
            }),
            this.prisma.staff.findMany({
                where: {
                    businessId,
                    updatedAt: { gt: lastDate },
                },
                include: { permissions: true },
            }),
            this.prisma.paymentQr.findMany({ where: { businessId } }),
            this.prisma.customer.findMany({ where: { businessId, isDeleted: false } }),
            this.prisma.expense.findMany({ where: { businessId } }),
        ]);
        const formattedOrders = orders.map((o) => {
            const orderItems = o.items.map((i) => ({
                itemId: i.productId || i.id,
                itemName: i.productName,
                price: Number(i.price),
                quantity: i.quantity,
                weight: i.weight ? Number(i.weight) : undefined,
                unit: i.unit || undefined,
            }));
            return {
                id: o.id,
                tokenNumber: o.tokenNumber,
                invoiceNumber: o.invoiceNumber,
                timestamp: new Date(o.createdAt).getTime(),
                subtotal: Number(o.subtotal || o.total),
                discount: Number(o.discount || 0),
                tax: Number(o.tax || 0),
                total: Number(o.total),
                orderItemsJson: JSON.stringify(orderItems),
                paymentMethod: o.paymentMethod || 'Cash',
                cashierName: o.cashierName || 'Admin',
            };
        });
        const formatUrl = (url) => {
            if (!url)
                return url;
            return url.startsWith('/') ? `https://api.optixapp.in${url}` : url;
        };
        if (business && business.receiptSettings && business.receiptSettings.logoUrl) {
            business.receiptSettings.logoUrl = formatUrl(business.receiptSettings.logoUrl) || business.receiptSettings.logoUrl;
        }
        const formattedPaymentQrs = paymentQrs.map((q) => ({
            ...q,
            imageUrl: formatUrl(q.imageUrl),
        }));
        const formattedProducts = products.map((p) => ({
            ...p,
            imageUrl: formatUrl(p.imageUrl),
        }));
        return {
            business,
            subscription,
            categories,
            products: formattedProducts,
            orders: formattedOrders,
            staff: staff.map((s) => ({ ...s, permissions: s.permissions.map((p) => p.action) })),
            paymentQrs: formattedPaymentQrs,
            customers,
            expenses,
            serverTime: Date.now(),
        };
    }
    async fullDump(businessId) {
        const [business, subscription, categories, products, orders, staff, paymentQrs, customers, expenses, sessions, activityLogs] = await Promise.all([
            this.prisma.business.findUnique({
                where: { id: businessId },
                include: { settings: true, receiptSettings: true, printerSettings: true },
            }),
            this.prisma.subscription.findFirst({
                where: { businessId },
                include: { plan: true },
                orderBy: { createdAt: 'desc' },
            }),
            this.prisma.category.findMany({ where: { businessId, isDeleted: false } }),
            this.prisma.product.findMany({ where: { businessId, isDeleted: false } }),
            this.prisma.order.findMany({
                where: { businessId },
                include: { items: true },
                orderBy: { createdAt: 'desc' },
                take: 500,
            }),
            this.prisma.staff.findMany({
                where: { businessId },
                include: { permissions: true },
            }),
            this.prisma.paymentQr.findMany({ where: { businessId } }),
            this.prisma.customer.findMany({ where: { businessId, isDeleted: false } }),
            this.prisma.expense.findMany({ where: { businessId } }),
            this.prisma.staffSession.findMany({ where: { businessId }, orderBy: { loginAt: 'desc' }, take: 100 }),
            this.prisma.staffActivityLog.findMany({ where: { businessId }, orderBy: { createdAt: 'desc' }, take: 100 }),
        ]);
        const formattedOrders = orders.map((o) => {
            const orderItems = o.items.map((i) => ({
                itemId: i.productId || i.id,
                itemName: i.productName,
                price: Number(i.price),
                quantity: i.quantity,
                weight: i.weight ? Number(i.weight) : undefined,
                unit: i.unit || undefined,
            }));
            return {
                id: o.id,
                tokenNumber: o.tokenNumber,
                invoiceNumber: o.invoiceNumber,
                timestamp: new Date(o.createdAt).getTime(),
                subtotal: Number(o.subtotal || o.total),
                discount: Number(o.discount || 0),
                tax: Number(o.tax || 0),
                total: Number(o.total),
                orderItemsJson: JSON.stringify(orderItems),
                paymentMethod: o.paymentMethod || 'Cash',
                cashierName: o.cashierName || 'Admin',
            };
        });
        const formatUrl = (url) => {
            if (!url)
                return url;
            return url.startsWith('/') ? `https://api.optixapp.in${url}` : url;
        };
        if (business && business.receiptSettings && business.receiptSettings.logoUrl) {
            business.receiptSettings.logoUrl = formatUrl(business.receiptSettings.logoUrl) || business.receiptSettings.logoUrl;
        }
        const formattedPaymentQrs = paymentQrs.map((q) => ({
            ...q,
            imageUrl: formatUrl(q.imageUrl),
        }));
        const formattedProducts = products.map((p) => ({
            ...p,
            price: Number(p.price),
            imageUrl: formatUrl(p.imageUrl),
        }));
        return {
            business,
            subscription,
            categories,
            products: formattedProducts,
            orders: formattedOrders,
            staff: staff.map((s) => ({ ...s, permissions: s.permissions.map((p) => p.action) })),
            paymentQrs: formattedPaymentQrs,
            customers,
            expenses,
            sessions,
            activityLogs,
            timestamp: Date.now(),
        };
    }
};
exports.SyncService = SyncService;
exports.SyncService = SyncService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService])
], SyncService);
//# sourceMappingURL=sync.service.js.map