import { Module } from '@nestjs/common';
import { JwtModule } from '@nestjs/jwt';
import { SyncService } from './sync.service';
import { SyncController } from './sync.controller';
import { SyncGateway } from './sync.gateway';

@Module({
  imports: [
    JwtModule.register({
      secret: process.env.JWT_SECRET || 'supersecret_staging_key_2026',
    }),
  ],
  providers: [SyncService, SyncGateway],
  controllers: [SyncController],
  exports: [SyncService, SyncGateway],
})
export class SyncModule {}
