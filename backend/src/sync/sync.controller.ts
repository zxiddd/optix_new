import { Body, Controller, Get, Post, Query, UseGuards } from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { SyncService } from './sync.service';
import { SyncPushDto } from './dto/sync.dto';
import { AtGuard } from '../auth/guards';
import { GetCurrentUser } from '../auth/decorators';

@ApiTags('Sync')
@ApiBearerAuth()
@UseGuards(AtGuard)
@Controller('sync')
export class SyncController {
  constructor(private syncService: SyncService) {}

  @Post('push')
  @ApiOperation({ summary: 'Upload local changes to cloud' })
  push(
    @GetCurrentUser('businessId') businessId: string,
    @Body() dto: SyncPushDto,
  ) {
    return this.syncService.push(businessId, dto);
  }

  @Get('pull')
  @ApiOperation({ summary: 'Download cloud changes since last sync' })
  pull(
    @GetCurrentUser('businessId') businessId: string,
    @Query('lastSync') lastSync?: string,
    @Query('since') since?: string,
  ) {
    const timestamp = parseInt(since || lastSync || '0') || 0;
    return this.syncService.pull(businessId, timestamp);
  }

  @Get('full-dump')
  @ApiOperation({ summary: 'Download complete business data for local cache hydration' })
  fullDump(@GetCurrentUser('businessId') businessId: string) {
    return this.syncService.fullDump(businessId);
  }
}
