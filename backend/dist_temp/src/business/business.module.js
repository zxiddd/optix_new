"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.BusinessModule = void 0;
const common_1 = require("@nestjs/common");
const business_controller_1 = require("./business.controller");
const business_service_1 = require("./business.service");
const business_clock_service_1 = require("./business-clock.service");
const business_reset_service_1 = require("./business-reset.service");
const sync_module_1 = require("../sync/sync.module");
let BusinessModule = class BusinessModule {
};
exports.BusinessModule = BusinessModule;
exports.BusinessModule = BusinessModule = __decorate([
    (0, common_1.Module)({
        imports: [sync_module_1.SyncModule],
        controllers: [business_controller_1.BusinessController],
        providers: [business_service_1.BusinessService, business_clock_service_1.BusinessClockService, business_reset_service_1.BusinessResetService],
        exports: [business_service_1.BusinessService, business_clock_service_1.BusinessClockService, business_reset_service_1.BusinessResetService],
    })
], BusinessModule);
//# sourceMappingURL=business.module.js.map