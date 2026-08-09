"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.AppModule = void 0;
const common_1 = require("@nestjs/common");
const config_1 = require("@nestjs/config");
const serve_static_1 = require("@nestjs/serve-static");
const path_1 = require("path");
const prisma_module_1 = require("./prisma/prisma.module");
const auth_module_1 = require("./auth/auth.module");
const sync_module_1 = require("./sync/sync.module");
const upload_module_1 = require("./upload/upload.module");
const health_module_1 = require("./health/health.module");
const business_module_1 = require("./business/business.module");
const categories_module_1 = require("./categories/categories.module");
const products_module_1 = require("./products/products.module");
const orders_module_1 = require("./orders/orders.module");
const staff_module_1 = require("./staff/staff.module");
const subscriptions_module_1 = require("./subscriptions/subscriptions.module");
const payment_qr_module_1 = require("./payment-qr/payment-qr.module");
const payments_module_1 = require("./payments/payments.module");
const super_admin_module_1 = require("./super-admin/super-admin.module");
const support_module_1 = require("./support/support.module");
let AppModule = class AppModule {
};
exports.AppModule = AppModule;
exports.AppModule = AppModule = __decorate([
    (0, common_1.Module)({
        imports: [
            config_1.ConfigModule.forRoot({
                isGlobal: true,
            }),
            serve_static_1.ServeStaticModule.forRoot({
                rootPath: (0, path_1.join)(process.cwd(), 'uploads'),
                serveRoot: '/uploads',
            }),
            serve_static_1.ServeStaticModule.forRoot({
                rootPath: (0, path_1.join)(process.cwd(), 'admin-dist'),
                serveRoot: '/admin',
            }),
            prisma_module_1.PrismaModule,
            auth_module_1.AuthModule,
            sync_module_1.SyncModule,
            upload_module_1.UploadModule,
            health_module_1.HealthModule,
            business_module_1.BusinessModule,
            categories_module_1.CategoriesModule,
            products_module_1.ProductsModule,
            orders_module_1.OrdersModule,
            staff_module_1.StaffModule,
            subscriptions_module_1.SubscriptionsModule,
            payment_qr_module_1.PaymentQrModule,
            payments_module_1.PaymentsModule,
            super_admin_module_1.SuperAdminModule,
            support_module_1.SupportModule,
        ],
    })
], AppModule);
//# sourceMappingURL=app.module.js.map