import { Module } from '@nestjs/common';
import { BusinessController } from './business.controller';
import { BusinessService } from './business.service';
import { BusinessClockService } from './business-clock.service';
import { BusinessResetService } from './business-reset.service';
import { SyncModule } from '../sync/sync.module';

@Module({
  imports: [SyncModule],
  controllers: [BusinessController],
  providers: [BusinessService, BusinessClockService, BusinessResetService],
  exports: [BusinessService, BusinessClockService, BusinessResetService],
})
export class BusinessModule {}
