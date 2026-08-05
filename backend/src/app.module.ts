import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { ServeStaticModule } from '@nestjs/serve-static';
import { join } from 'path';
import { PrismaModule } from './prisma/prisma.module';
import { AuthModule } from './auth/auth.module';
import { SyncModule } from './sync/sync.module';
import { UploadModule } from './upload/upload.module';
import { HealthModule } from './health/health.module';
import { BusinessModule } from './business/business.module';
import { CategoriesModule } from './categories/categories.module';
import { ProductsModule } from './products/products.module';
import { OrdersModule } from './orders/orders.module';
import { StaffModule } from './staff/staff.module';
import { SubscriptionsModule } from './subscriptions/subscriptions.module';
import { PaymentQrModule } from './payment-qr/payment-qr.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    ServeStaticModule.forRoot({
      rootPath: join(process.cwd(), 'uploads'),
      serveRoot: '/uploads',
    }),
    PrismaModule,
    AuthModule,
    SyncModule,
    UploadModule,
    HealthModule,
    BusinessModule,
    CategoriesModule,
    ProductsModule,
    OrdersModule,
    StaffModule,
    SubscriptionsModule,
    PaymentQrModule,
  ],
})
export class AppModule {}
