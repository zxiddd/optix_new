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
exports.CategoriesService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
const sync_gateway_1 = require("../sync/sync.gateway");
let CategoriesService = class CategoriesService {
    constructor(prisma, syncGateway) {
        this.prisma = prisma;
        this.syncGateway = syncGateway;
    }
    async getCategories(businessId) {
        return this.prisma.category.findMany({
            where: { businessId, isDeleted: false },
            orderBy: { sortOrder: 'asc' },
        });
    }
    async saveCategory(businessId, dto) {
        let cat;
        if (dto.id) {
            cat = await this.prisma.category.upsert({
                where: { id: dto.id },
                update: {
                    name: dto.name,
                    sortOrder: dto.sortOrder ?? 0,
                },
                create: {
                    id: dto.id,
                    businessId,
                    name: dto.name,
                    sortOrder: dto.sortOrder ?? 0,
                },
            });
        }
        else {
            cat = await this.prisma.category.create({
                data: {
                    businessId,
                    name: dto.name,
                    sortOrder: dto.sortOrder ?? 0,
                },
            });
        }
        this.syncGateway.emitToBusiness(businessId, 'category.updated', cat);
        return cat;
    }
    async deleteCategory(businessId, id) {
        const res = await this.prisma.category.updateMany({
            where: { id, businessId },
            data: { isDeleted: true },
        });
        this.syncGateway.emitToBusiness(businessId, 'category.deleted', { id });
        return res;
    }
};
exports.CategoriesService = CategoriesService;
exports.CategoriesService = CategoriesService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        sync_gateway_1.SyncGateway])
], CategoriesService);
//# sourceMappingURL=categories.service.js.map