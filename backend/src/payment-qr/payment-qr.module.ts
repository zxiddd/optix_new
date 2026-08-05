import { Module } from '@nestjs/common';
import { PaymentQrController } from './payment-qr.controller';
import { PaymentQrService } from './payment-qr.service';
import { SyncModule } from '../sync/sync.module';

@Module({
  imports: [SyncModule],
  controllers: [PaymentQrController],
  providers: [PaymentQrService],
  exports: [PaymentQrService],
})
export class PaymentQrModule {}
