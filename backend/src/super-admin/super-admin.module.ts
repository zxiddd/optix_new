import { Module } from '@nestjs/common';
import { SuperAdminService } from './super-admin.service';
import { SuperAdminController } from './super-admin.controller';
import { FeatureFlagsController } from '../feature-flags/feature-flags.controller';
import { PrismaModule } from '../prisma/prisma.module';
import { SyncModule } from '../sync/sync.module';

@Module({
  imports: [PrismaModule, SyncModule],
  controllers: [SuperAdminController, FeatureFlagsController],
  providers: [SuperAdminService],
  exports: [SuperAdminService],
})
export class SuperAdminModule {}
