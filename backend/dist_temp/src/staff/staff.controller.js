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
exports.StaffController = void 0;
const common_1 = require("@nestjs/common");
const swagger_1 = require("@nestjs/swagger");
const staff_service_1 = require("./staff.service");
const guards_1 = require("../auth/guards");
const decorators_1 = require("../auth/decorators");
let StaffController = class StaffController {
    constructor(staffService) {
        this.staffService = staffService;
    }
    getStaff(businessId) {
        return this.staffService.getStaff(businessId);
    }
    getActivityLogs(businessId, staffId, limit) {
        return this.staffService.getActivityLogs(businessId, staffId, limit ? parseInt(limit) : 100);
    }
    getSessions(businessId, staffId) {
        return this.staffService.getSessions(businessId, staffId);
    }
    getStaffById(businessId, id) {
        return this.staffService.getStaffById(businessId, id);
    }
    saveStaff(businessId, body, socketId) {
        return this.staffService.saveStaff(businessId, body, socketId);
    }
    disableStaff(businessId, id, socketId) {
        return this.staffService.disableStaff(businessId, id, socketId);
    }
    enableStaff(businessId, id, socketId) {
        return this.staffService.enableStaff(businessId, id, socketId);
    }
    updatePermissions(businessId, id, permissions, socketId) {
        return this.staffService.updatePermissions(businessId, id, permissions || [], socketId);
    }
    terminateSession(businessId, id, socketId) {
        return this.staffService.terminateSession(businessId, id, socketId);
    }
    getNotifications(businessId) {
        return this.staffService.getNotifications(businessId);
    }
    markNotificationsRead(businessId) {
        return this.staffService.markNotificationsRead(businessId);
    }
    deleteStaff(businessId, id, socketId) {
        return this.staffService.deleteStaff(businessId, id, socketId);
    }
};
exports.StaffController = StaffController;
__decorate([
    (0, common_1.Get)(),
    (0, decorators_1.RequirePermissions)('VIEW_STAFF'),
    (0, swagger_1.ApiOperation)({ summary: 'Get all staff members for this business' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], StaffController.prototype, "getStaff", null);
__decorate([
    (0, common_1.Get)('activity-logs'),
    (0, swagger_1.ApiOperation)({ summary: 'Get staff activity logs (all staff or specific)' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Query)('staffId')),
    __param(2, (0, common_1.Query)('limit')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String]),
    __metadata("design:returntype", void 0)
], StaffController.prototype, "getActivityLogs", null);
__decorate([
    (0, common_1.Get)('sessions'),
    (0, swagger_1.ApiOperation)({ summary: 'Get staff sessions (all staff or specific)' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Query)('staffId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String]),
    __metadata("design:returntype", void 0)
], StaffController.prototype, "getSessions", null);
__decorate([
    (0, common_1.Get)(':id'),
    (0, swagger_1.ApiOperation)({ summary: 'Get a single staff member by ID' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Param)('id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String]),
    __metadata("design:returntype", void 0)
], StaffController.prototype, "getStaffById", null);
__decorate([
    (0, common_1.Post)(),
    (0, swagger_1.ApiOperation)({ summary: 'Create or update a staff member' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Body)()),
    __param(2, (0, common_1.Headers)('x-socket-id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, Object, String]),
    __metadata("design:returntype", void 0)
], StaffController.prototype, "saveStaff", null);
__decorate([
    (0, common_1.Put)(':id/disable'),
    (0, swagger_1.ApiOperation)({ summary: 'Disable a staff member (prevents login)' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Headers)('x-socket-id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String]),
    __metadata("design:returntype", void 0)
], StaffController.prototype, "disableStaff", null);
__decorate([
    (0, common_1.Put)(':id/enable'),
    (0, swagger_1.ApiOperation)({ summary: 'Enable a previously disabled staff member' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Headers)('x-socket-id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String]),
    __metadata("design:returntype", void 0)
], StaffController.prototype, "enableStaff", null);
__decorate([
    (0, common_1.Put)(':id/permissions'),
    (0, swagger_1.ApiOperation)({ summary: 'Replace all permissions for a staff member' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Body)('permissions')),
    __param(3, (0, common_1.Headers)('x-socket-id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, Array, String]),
    __metadata("design:returntype", void 0)
], StaffController.prototype, "updatePermissions", null);
__decorate([
    (0, common_1.Post)('sessions/:id/terminate'),
    (0, swagger_1.ApiOperation)({ summary: 'Remotely terminate an active staff session' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Headers)('x-socket-id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String]),
    __metadata("design:returntype", void 0)
], StaffController.prototype, "terminateSession", null);
__decorate([
    (0, common_1.Get)('notifications'),
    (0, swagger_1.ApiOperation)({ summary: 'Get owner notifications' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], StaffController.prototype, "getNotifications", null);
__decorate([
    (0, common_1.Put)('notifications/read'),
    (0, swagger_1.ApiOperation)({ summary: 'Mark all notifications read' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String]),
    __metadata("design:returntype", void 0)
], StaffController.prototype, "markNotificationsRead", null);
__decorate([
    (0, common_1.Delete)(':id'),
    (0, swagger_1.ApiOperation)({ summary: 'Delete a staff member permanently' }),
    __param(0, (0, decorators_1.GetCurrentUser)('businessId')),
    __param(1, (0, common_1.Param)('id')),
    __param(2, (0, common_1.Headers)('x-socket-id')),
    __metadata("design:type", Function),
    __metadata("design:paramtypes", [String, String, String]),
    __metadata("design:returntype", void 0)
], StaffController.prototype, "deleteStaff", null);
exports.StaffController = StaffController = __decorate([
    (0, swagger_1.ApiTags)('Staff'),
    (0, swagger_1.ApiBearerAuth)(),
    (0, common_1.UseGuards)(guards_1.AtGuard, guards_1.PermissionsGuard),
    (0, common_1.Controller)('staff'),
    __metadata("design:paramtypes", [staff_service_1.StaffService])
], StaffController);
//# sourceMappingURL=staff.controller.js.map