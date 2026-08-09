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
exports.SyncController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const sync_service_1 = require("./sync.service");
const sync_dto_1 = require("./dto/sync.dto");
const guards_1 = require("../auth/guards");
const decorators_1 = require("../auth/decorators");
let SyncController = class SyncController {
    constructor(syncService) {
        this.syncService = syncService;
    }
    push(businessId, dto) {
        return this.syncService.push(businessId, dto);
    }
    pull(businessId, lastSync, since) {
        const timestamp = parseInt(since || lastSync || '0') || 0;
        return this.syncService.pull(businessId, timestamp);
    }
    fullDump(businessId) {
        return this.syncService.fullDump(businessId);
    }
};
exports.SyncController = SyncController;
__decorate([
    (0, common_1.Post)('push'),
    (0, swagger_1.ApiOperation)({ summary: 'Upload local changes to cloud' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, sync_dto_1.SyncPushDto]),
    __metadata("design:returntype", void 0)
], SyncController.prototype, "push", null);
__decorate([
    (0, common_1.Get)('pull'),
    (0, swagger_1.ApiOperation)({ summary: 'Download cloud changes since last sync' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Query)('lastSync')),
    __param(2, (0, common_1.Query)('since')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String]),
    __metadata("design:returntype", void 0)
], SyncController.prototype, "pull", null);
__decorate([
    (0, common_1.Get)('full-dump'),
    (0, swagger_1.ApiOperation)({ summary: 'Download complete business data for local cache hydration' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], SyncController.prototype, "fullDump", null);
exports.SyncController = SyncController = __decorate([
    (0, swagger_1.ApiTags)('Sync'),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.UseGuards)(guards_1.AtGuard),
    (0, common_1.Controller)('sync'),
    __metadata("design:paramtypes", [sync_service_1.SyncService])
], SyncController);
//# sourceMappingURL=sync.controller.js.map