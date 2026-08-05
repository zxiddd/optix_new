import { Body, Controller, Delete, Get, Param, Post, UseGuards } from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { OrdersService } from './orders.service';
import { AtGuard, PermissionsGuard } from '../auth/guards';
import { GetCurrentUser, RequirePermissions } from '../auth/decorators';

@ApiTags('Orders')
@ApiBearerAuth()
@UseGuards(AtGuard, PermissionsGuard)
@Controller('orders')
export class OrdersController {
  constructor(private ordersService: OrdersService) {}

  @Get()
  @ApiOperation({ summary: 'Get all orders' })
  getOrders(@GetCurrentUser('businessId') businessId: string) {
    return this.ordersService.getOrders(businessId);
  }

  @Post()
  @RequirePermissions('CREATE_BILLS')
  @ApiOperation({ summary: 'Create or update an order' })
  saveOrder(
    @GetCurrentUser('businessId') businessId: string,
    @Body() body: any,
  ) {
    return this.ordersService.saveOrder(businessId, body);
  }

  @Delete(':id')
  @RequirePermissions('CANCEL_BILLS')
  @ApiOperation({ summary: 'Delete an order' })
  deleteOrder(
    @GetCurrentUser('businessId') businessId: string,
    @Param('id') id: string,
  ) {
    return this.ordersService.deleteOrder(businessId, id);
  }
}
