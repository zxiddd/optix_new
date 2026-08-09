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
exports.SupportController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const support_service_1 = require("./support.service");
const public_decorator_1 = require("../auth/decorators/public.decorator");
let SupportController = class SupportController {
    constructor(supportService) {
        this.supportService = supportService;
    }
    async processAiChat(body) {
        return this.supportService.processAiChat(body.businessId, body.message, body.history);
    }
    async createTicket(body) {
        return this.supportService.createTicket(body);
    }
    async getBusinessTickets(businessId) {
        return this.supportService.getBusinessTickets(businessId);
    }
    async getAllTicketsForAdmin(query) {
        return this.supportService.getAllTicketsForAdmin(query);
    }
    async getTicketDetails(id) {
        return this.supportService.getTicketDetails(id);
    }
    async addMessage(id, body) {
        return this.supportService.addMessage(id, body.senderType, body.senderName, body.message);
    }
    async updateTicketStatus(id, body) {
        return this.supportService.updateTicketStatus(id, body.status, body.priority);
    }
};
exports.SupportController = SupportController;
__decorate([
    (0, public_decorator_1.Public)(),
    (0, common_1.Post)('ai/chat'),
    (0, swagger_1.ApiOperation)({ summary: 'Process AI Assistant prompt using Gemini API' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], SupportController.prototype, "processAiChat", null);
__decorate([
    (0, public_decorator_1.Public)(),
    (0, common_1.Post)('tickets'),
    (0, swagger_1.ApiOperation)({ summary: 'Create a new support ticket' }),
    __param(0, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], SupportController.prototype, "createTicket", null);
__decorate([
    (0, public_decorator_1.Public)(),
    (0, common_1.Get)('tickets/business/:businessId'),
    (0, swagger_1.ApiOperation)({ summary: 'Get support tickets for a business tenant' }),
    __param(0, (0, common_1.Param)('businessId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], SupportController.prototype, "getBusinessTickets", null);
__decorate([
    (0, public_decorator_1.Public)(),
    (0, common_1.Get)('tickets/admin/all'),
    (0, swagger_1.ApiOperation)({ summary: 'Get all support tickets for Super Admin dashboard' }),
    __param(0, (0, common_1.Query)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [Object]),
    __metadata("design:returntype", Promise)
], SupportController.prototype, "getAllTicketsForAdmin", null);
__decorate([
    (0, public_decorator_1.Public)(),
    (0, common_1.Get)('tickets/:id'),
    (0, swagger_1.ApiOperation)({ summary: 'Get support ticket details and conversation history' }),
    __param(0, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", Promise)
], SupportController.prototype, "getTicketDetails", null);
__decorate([
    (0, public_decorator_1.Public)(),
    (0, common_1.Post)('tickets/:id/messages'),
    (0, swagger_1.ApiOperation)({ summary: 'Add a message to a support ticket' }),
    __param(0, (0, common_1.Param)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], SupportController.prototype, "addMessage", null);
__decorate([
    (0, public_decorator_1.Public)(),
    (0, common_1.Patch)('tickets/:id/status'),
    (0, swagger_1.ApiOperation)({ summary: 'Update ticket status or priority' }),
    __param(0, (0, common_1.Param)('id')),
    __param(1, (0, common_1.Body)()),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object]),
    __metadata("design:returntype", Promise)
], SupportController.prototype, "updateTicketStatus", null);
exports.SupportController = SupportController = __decorate([
    (0, swagger_1.ApiTags)('support'),
    (0, common_1.Controller)('support'),
    __metadata("design:paramtypes", [support_service_1.SupportService])
], SupportController);
//# sourceMappingURL=support.controller.js.map