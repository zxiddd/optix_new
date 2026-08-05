import {
  Body,
  Controller,
  Delete,
  Get,
  Headers,
  Param,
  Post,
  Put,
  Query,
  UseGuards,
} from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { StaffService } from './staff.service';
import { AtGuard, PermissionsGuard } from '../auth/guards';
import { GetCurrentUser, RequirePermissions } from '../auth/decorators';

@ApiTags('Staff')
@ApiBearerAuth()
@UseGuards(AtGuard, PermissionsGuard)
@Controller('staff')
export class StaffController {
  constructor(private staffService: StaffService) {}

  // ─── LIST ────────────────────────────────────────────────────────────────────

  @Get()
  @RequirePermissions('VIEW_STAFF')
  @ApiOperation({ summary: 'Get all staff members for this business' })
  getStaff(@GetCurrentUser('businessId') businessId: string) {
    return this.staffService.getStaff(businessId);
  }

  @Get('activity-logs')
  @ApiOperation({ summary: 'Get staff activity logs (all staff or specific)' })
  getActivityLogs(
    @GetCurrentUser('businessId') businessId: string,
    @Query('staffId') staffId?: string,
    @Query('limit') limit?: string,
  ) {
    return this.staffService.getActivityLogs(businessId, staffId, limit ? parseInt(limit) : 100);
  }

  @Get('sessions')
  @ApiOperation({ summary: 'Get staff sessions (all staff or specific)' })
  getSessions(
    @GetCurrentUser('businessId') businessId: string,
    @Query('staffId') staffId?: string,
  ) {
    return this.staffService.getSessions(businessId, staffId);
  }

  @Get(':id')
  @ApiOperation({ summary: 'Get a single staff member by ID' })
  getStaffById(
    @GetCurrentUser('businessId') businessId: string,
    @Param('id') id: string,
  ) {
    return this.staffService.getStaffById(businessId, id);
  }

  // ─── CREATE / UPDATE ─────────────────────────────────────────────────────────

  @Post()
  @ApiOperation({ summary: 'Create or update a staff member' })
  saveStaff(
    @GetCurrentUser('businessId') businessId: string,
    @Body() body: any,
    @Headers('x-socket-id') socketId?: string,
  ) {
    return this.staffService.saveStaff(businessId, body, socketId);
  }

  // ─── DISABLE / ENABLE ────────────────────────────────────────────────────────

  @Put(':id/disable')
  @ApiOperation({ summary: 'Disable a staff member (prevents login)' })
  disableStaff(
    @GetCurrentUser('businessId') businessId: string,
    @Param('id') id: string,
    @Headers('x-socket-id') socketId?: string,
  ) {
    return this.staffService.disableStaff(businessId, id, socketId);
  }

  @Put(':id/enable')
  @ApiOperation({ summary: 'Enable a previously disabled staff member' })
  enableStaff(
    @GetCurrentUser('businessId') businessId: string,
    @Param('id') id: string,
    @Headers('x-socket-id') socketId?: string,
  ) {
    return this.staffService.enableStaff(businessId, id, socketId);
  }

  // ─── PERMISSIONS ─────────────────────────────────────────────────────────────

  @Put(':id/permissions')
  @ApiOperation({ summary: 'Replace all permissions for a staff member' })
  updatePermissions(
    @GetCurrentUser('businessId') businessId: string,
    @Param('id') id: string,
    @Body('permissions') permissions: string[],
    @Headers('x-socket-id') socketId?: string,
  ) {
    return this.staffService.updatePermissions(businessId, id, permissions || [], socketId);
  }

  // ─── TERMINATE SESSION ───────────────────────────────────────────────────────

  @Post('sessions/:id/terminate')
  @ApiOperation({ summary: 'Remotely terminate an active staff session' })
  terminateSession(
    @GetCurrentUser('businessId') businessId: string,
    @Param('id') id: string,
    @Headers('x-socket-id') socketId?: string,
  ) {
    return this.staffService.terminateSession(businessId, id, socketId);
  }

  // ─── NOTIFICATIONS ───────────────────────────────────────────────────────────

  @Get('notifications')
  @ApiOperation({ summary: 'Get owner notifications' })
  getNotifications(@GetCurrentUser('businessId') businessId: string) {
    return this.staffService.getNotifications(businessId);
  }

  @Put('notifications/read')
  @ApiOperation({ summary: 'Mark all notifications read' })
  markNotificationsRead(@GetCurrentUser('businessId') businessId: string) {
    return this.staffService.markNotificationsRead(businessId);
  }

  // ─── DELETE ──────────────────────────────────────────────────────────────────

  @Delete(':id')
  @ApiOperation({ summary: 'Delete a staff member permanently' })
  deleteStaff(
    @GetCurrentUser('businessId') businessId: string,
    @Param('id') id: string,
    @Headers('x-socket-id') socketId?: string,
  ) {
    return this.staffService.deleteStaff(businessId, id, socketId);
  }
}
