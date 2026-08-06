import { Body, Controller, Get, Headers, Post, UseGuards } from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { BusinessService } from './business.service';
import { BusinessResetService } from './business-reset.service';
import { AtGuard } from '../auth/guards';
import { GetCurrentUser } from '../auth/decorators';

@ApiTags('Business')
@ApiBearerAuth()
@UseGuards(AtGuard)
@Controller('business')
export class BusinessController {
  constructor(
    private businessService: BusinessService,
    private businessResetService: BusinessResetService,
  ) {}

  @Get('profile')
  @ApiOperation({ summary: 'Get current business profile' })
  getProfile(@GetCurrentUser('businessId') businessId: string) {
    return this.businessService.getProfile(businessId);
  }

  @Post('profile')
  @ApiOperation({ summary: 'Update business profile' })
  updateProfile(
    @GetCurrentUser('businessId') businessId: string,
    @Body() body: any,
    @Headers('x-socket-id') socketId?: string,
  ) {
    return this.businessService.updateProfile(businessId, body, socketId || body.senderSocketId);
  }

  @Post('reset')
  @ApiOperation({ summary: 'Reset business day (exactly once per business date)' })
  resetBusinessDay(
    @GetCurrentUser('businessId') businessId: string,
    @Body() body: any,
    @Headers('x-socket-id') socketId?: string,
  ) {
    return this.businessResetService.resetBusinessDay(businessId, body?.targetBusinessDate, socketId || body?.senderSocketId);
  }
}
