import { Controller, Post, Body, UseGuards, Request, Headers, BadRequestException, RawBodyRequest } from '@nestjs/common';
import { PaymentsService } from './payments.service';
import { JwtAuthGuard } from '../auth/guards/jwt-auth.guard';
import { ApiTags, ApiOperation, ApiBearerAuth } from '@nestjs/swagger';

@ApiTags('Payments')
@Controller('payments')
export class PaymentsController {
  constructor(private readonly paymentsService: PaymentsService) {}

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Post('create-order')
  @ApiOperation({ summary: 'Create a Razorpay order' })
  async createOrder(
    @Request() req,
    @Body() body: { planId: string; billingCycle: 'MONTHLY' | 'YEARLY' }
  ) {
    return this.paymentsService.createOrder(req.user.businessId, body.planId, body.billingCycle);
  }

  @ApiBearerAuth()
  @UseGuards(JwtAuthGuard)
  @Post('verify')
  @ApiOperation({ summary: 'Verify Razorpay payment signature' })
  async verifyPayment(
    @Request() req,
    @Body() body: {
      razorpay_order_id: string;
      razorpay_payment_id: string;
      razorpay_signature: string;
    }
  ) {
    return this.paymentsService.verifyPayment(req.user.businessId, body);
  }

  @Post('webhook')
  @ApiOperation({ summary: 'Razorpay Webhook endpoint' })
  async handleWebhook(
    @Headers('x-razorpay-signature') signature: string,
    @Request() req: RawBodyRequest<any>,
  ) {
    if (!signature) throw new BadRequestException('No signature found');

    // We need the raw body for verification
    const rawBody = req.rawBody?.toString();
    if (!rawBody) throw new BadRequestException('No raw body found');

    return this.paymentsService.handleWebhook(signature, rawBody);
  }
}
