import { Body, Controller, Get, Post, UseGuards } from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { SubscriptionsService } from './subscriptions.service';
import { AtGuard } from '../auth/guards';
import { GetCurrentUser, Public } from '../auth/decorators';

@ApiTags('Subscriptions')
@Controller('subscriptions')
export class SubscriptionsController {
  constructor(private subscriptionsService: SubscriptionsService) {}

  @UseGuards(AtGuard)
  @ApiBearerAuth()
  @Get()
  @ApiOperation({ summary: 'Get current business subscription' })
  getSubscription(@GetCurrentUser('businessId') businessId: string) {
    return this.subscriptionsService.getSubscription(businessId);
  }

  @Public()
  @Get('plans')
  @ApiOperation({ summary: 'Get all available subscription plans' })
  getPlans() {
    return this.subscriptionsService.getPlans();
  }

  @UseGuards(AtGuard)
  @ApiBearerAuth()
  @Post()
  @ApiOperation({ summary: 'Save or renew business subscription' })
  saveSubscription(
    @GetCurrentUser('businessId') businessId: string,
    @Body() body: any,
  ) {
    return this.subscriptionsService.saveSubscription(businessId, body);
  }
}
