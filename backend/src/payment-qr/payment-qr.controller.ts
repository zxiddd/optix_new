import { Body, Controller, Delete, Get, Headers, Param, Post, Put, UseGuards } from '@nestjs/common';
import { ApiTags, ApiBearerAuth, ApiOperation } from '@nestjs/swagger';
import { PaymentQrService } from './payment-qr.service';
import { AtGuard } from '../auth/guards';
import { GetCurrentUser } from '../auth/decorators';

@ApiTags('Payment QR')
@ApiBearerAuth()
@UseGuards(AtGuard)
@Controller('payment-qrs')
export class PaymentQrController {
  constructor(private paymentQrService: PaymentQrService) {}

  @Get()
  @ApiOperation({ summary: 'Get all payment QRs for current business' })
  getPaymentQrs(@GetCurrentUser('businessId') businessId: string) {
    return this.paymentQrService.getPaymentQrs(businessId);
  }

  @Post()
  @ApiOperation({ summary: 'Create or update payment QR' })
  savePaymentQr(
    @GetCurrentUser('businessId') businessId: string,
    @Body() body: any,
    @Headers('x-socket-id') socketId?: string,
  ) {
    return this.paymentQrService.savePaymentQr(businessId, body, socketId || body.senderSocketId);
  }

  @Put(':id/select')
  @ApiOperation({ summary: 'Select default active payment QR' })
  selectPaymentQr(
    @GetCurrentUser('businessId') businessId: string,
    @Param('id') id: string,
    @Headers('x-socket-id') socketId?: string,
  ) {
    return this.paymentQrService.selectPaymentQr(businessId, id, socketId);
  }

  @Delete(':id')
  @ApiOperation({ summary: 'Delete payment QR' })
  deletePaymentQr(
    @GetCurrentUser('businessId') businessId: string,
    @Param('id') id: string,
    @Headers('x-socket-id') socketId?: string,
  ) {
    return this.paymentQrService.deletePaymentQr(businessId, id, socketId);
  }
}
