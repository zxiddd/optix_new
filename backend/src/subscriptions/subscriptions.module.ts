import { Module } from '@nestjs/common';
import { SubscriptionsController, StandaloneSubscriptionController } from './subscriptions.controller';
import { SubscriptionsService } from './subscriptions.service';
import { SyncModule } from '../sync/sync.module';

@Module({
  imports: [SyncModule],
  controllers: [SubscriptionsController, StandaloneSubscriptionController],
  providers: [SubscriptionsService],
  exports: [SubscriptionsService],
})
export class SubscriptionsModule {}
