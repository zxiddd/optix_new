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
exports.ProductsService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
const sync_gateway_1 = require("../sync/sync.gateway");
let ProductsService = class ProductsService {
    constructor(prisma, syncGateway) {
        this.prisma = prisma;
        this.syncGateway = syncGateway;
    }
    async getProducts(businessId) {
        return this.prisma.product.findMany({
            where: { businessId, isDeleted: false },
            include: { category: true },
            orderBy: { name: 'asc' },
        });
    }
    async saveProduct(businessId, dto) {
        const data = {
            name: dto.name,
            description: dto.description,
            barcode: dto.barcode,
            sku: dto.sku,
            price: dto.price,
            pricingType: dto.pricingType || 'FIXED',
            unit: dto.unit || 'Piece',
            categoryId: dto.categoryId,
            imageUrl: dto.imageUrl,
            isOutOfStock: dto.isOutOfStock ?? false,
        };
        let p;
        if (dto.id) {
            p = await this.prisma.product.upsert({
                where: { id: dto.id },
                update: data,
                create: {
                    id: dto.id,
                    businessId,
                    ...data,
                },
            });
        }
        else {
            p = await this.prisma.product.create({
                data: {
                    businessId,
                    ...data,
                },
            });
        }
        this.syncGateway.emitToBusiness(businessId, 'product.updated', p);
        return p;
    }
    async deleteProduct(businessId, id) {
        const res = await this.prisma.product.updateMany({
            where: { id, businessId },
            data: { isDeleted: true },
        });
        this.syncGateway.emitToBusiness(businessId, 'product.deleted', { id });
        return res;
    }
};
exports.ProductsService = ProductsService;
exports.ProductsService = ProductsService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        sync_gateway_1.SyncGateway])
], ProductsService);
//# sourceMappingURL=products.service.js.map