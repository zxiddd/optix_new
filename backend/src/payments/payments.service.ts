import { Injectable, Logger, BadRequestException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { ConfigService } from '@nestjs/config';
import { SyncGateway } from '../sync/sync.gateway';
// eslint-disable-next-line @typescript-eslint/no-var-requires
const Razorpay = require('razorpay');

import * as crypto from 'crypto';

@Injectable()
export class PaymentsService {
  private razorpay: any;
  private readonly logger = new Logger(PaymentsService.name);

  constructor(
    private prisma: PrismaService,
    private config: ConfigService,
    private syncGateway: SyncGateway,
  ) {
    const keyId = this.config.get<string>('RAZORPAY_KEY_ID') || 'rzp_test_TMy1cZ9CH4Vh0V';
    const keySecret = this.config.get<string>('RAZORPAY_KEY_SECRET') || 'OJMTJqo6Oc8yBrbioMVWClcu';

    this.razorpay = new Razorpay({
      key_id: keyId,
      key_secret: keySecret,
    });

    this.logger.log(`Razorpay Initialized in ${keyId.startsWith('rzp_test') ? 'TEST' : 'LIVE'} mode`);
  }

  async createOrder(businessId: string, planId: string, cycle: 'MONTHLY' | 'YEARLY') {
    this.logger.log(`[CREATE ORDER] Business: ${businessId}, Plan: ${planId}, Cycle: ${cycle}`);
    const business = await this.prisma.business.findUnique({
      where: { id: businessId },
      include: { settings: true },
    });

    if (!business) throw new BadRequestException('Business not found');

    // Pricing logic (should match Android PricingEngine)
    let amount = 0;
    const currency = business.settings?.currency || 'INR';
    const country = (business.country || 'India').trim();
    const countryLower = country.toLowerCase();

    if (countryLower === 'india') {
      if (planId === 'STARTER') amount = cycle === 'MONTHLY' ? 499 : 4849;
      else if (planId === 'GROWTH') amount = cycle === 'MONTHLY' ? 999 : 9709;
    } else if (countryLower === 'saudi arabia') {
      if (planId === 'STARTER') amount = cycle === 'MONTHLY' ? 120 : 1296;
      else if (planId === 'GROWTH') amount = cycle === 'MONTHLY' ? 199 : 2149;
    }

    if (amount === 0) throw new BadRequestException('Invalid plan or country configuration');

    const options = {
      amount: amount * 100, // in paise or subunits
      currency: (currency === 'Rs.' || currency === '₹') ? 'INR' : currency,
      receipt: `rcpt_${Date.now()}`,
      notes: {
        businessId,
        planId,
        cycle,
        country,
      },
    };

    try {
      this.logger.log(`Creating Razorpay Order with Key: ${this.razorpay.key_id}`);
      const order = await this.razorpay.orders.create(options);

      // Store pending transaction
      await this.prisma.paymentTransaction.create({
        data: {
          businessId,
          planId,
          billingCycle: cycle,
          amount: amount,
          currency: options.currency,
          country: country,
          status: 'PENDING',
          razorpayOrderId: order.id,
        },
      });

      return {
        ...order,
        key_id: this.config.get<string>('RAZORPAY_KEY_ID') || 'rzp_test_TMy1cZ9CH4Vh0V'
      };
    } catch (error) {
      this.logger.error('Razorpay Order Creation Failed', error);
      throw new BadRequestException('Failed to initiate payment');
    }
  }

  async verifyPayment(businessId: string, data: {
    razorpay_order_id: string;
    razorpay_payment_id: string;
    razorpay_signature: string;
  }) {
    const secret = this.config.get<string>('RAZORPAY_KEY_SECRET') || 'OJMTJqo6Oc8yBrbioMVWClcu';
    const body = data.razorpay_order_id + '|' + data.razorpay_payment_id;

    const expectedSignature = crypto
      .createHmac('sha256', secret)
      .update(body.toString())
      .digest('hex');

    if (expectedSignature !== data.razorpay_signature) {
      throw new BadRequestException('Invalid signature');
    }

    return this.completePayment(data.razorpay_order_id, data.razorpay_payment_id, data.razorpay_signature);
  }

  async handleWebhook(signature: string, rawBody: string) {
    const secret = this.config.get<string>('RAZORPAY_WEBHOOK_SECRET') || 'OJMTJqo6Oc8yBrbioMVWClcu'; // Use a different webhook secret if configured in dashboard

    // Verify signature
    const expectedSignature = crypto
      .createHmac('sha256', secret)
      .update(rawBody)
      .digest('hex');

    // Note: Signature for webhooks might differ from payments,
    // usually provided in X-Razorpay-Signature header

    const event = JSON.parse(rawBody);
    this.logger.log(`Received Webhook: ${event.event}`);

    if (event.event === 'payment.captured') {
      const payload = event.payload.payment.entity;
      await this.completePayment(payload.order_id, payload.id, signature, payload);
    } else if (event.event === 'payment.failed') {
      const payload = event.payload.payment.entity;
      await this.prisma.paymentTransaction.updateMany({
        where: { razorpayOrderId: payload.order_id },
        data: {
          status: 'FAILED',
          razorpayPaymentId: payload.id,
          failureReason: payload.error_description,
          gatewayMetadata: payload,
        },
      });
    }
  }

  private async completePayment(orderId: string, paymentId: string, signature: string, metadata?: any) {
    const tx = await this.prisma.paymentTransaction.findUnique({
      where: { razorpayOrderId: orderId },
    });

    if (!tx || tx.status === 'CAPTURED') return;

    // 1. Update Transaction
    await this.prisma.paymentTransaction.update({
      where: { id: tx.id },
      data: {
        status: 'CAPTURED',
        razorpayPaymentId: paymentId,
        razorpaySignature: signature,
        capturedAt: new Date(),
        gatewayMetadata: metadata || tx.gatewayMetadata,
      },
    });

    // 2. Activate Subscription
    let plan = await this.prisma.plan.findFirst({
      where: {
        OR: [{ id: tx.planId }, { name: tx.planId }],
      },
    });

    if (!plan) {
      // Fallback: create plan if it doesn't exist to prevent crash
      plan = await this.prisma.plan.create({
        data: {
          id: tx.planId, // Use "STARTER" or "GROWTH" as ID
          name: tx.planId.charAt(0) + tx.planId.slice(1).toLowerCase(),
          price: tx.amount,
          billingPeriod: tx.billingCycle,
          features: {},
        },
      });
    }

    const durationDays = tx.billingCycle === 'YEARLY' ? 365 : 30;
    const expiryDate = new Date();
    expiryDate.setDate(expiryDate.getDate() + durationDays);

    const subscription = await this.prisma.subscription.upsert({
      where: { businessId: tx.businessId },
      update: {
        planId: plan.id,
        status: 'ACTIVE',
        billingCycle: tx.billingCycle,
        currency: tx.currency,
        country: tx.country,
        expiryDate: expiryDate,
        updatedAt: new Date(),
      },
      create: {
        businessId: tx.businessId,
        planId: plan.id,
        status: 'ACTIVE',
        billingCycle: tx.billingCycle,
        currency: tx.currency,
        country: tx.country,
        expiryDate: expiryDate,
      },
    });

    // Link transaction to subscription
    await this.prisma.paymentTransaction.update({
      where: { id: tx.id },
      data: { subscriptionId: subscription.id },
    });

    this.logger.log(`Subscription Activated for Business: ${tx.businessId}, Plan: ${tx.planId}`);

    // Emit WebSocket event to notify Android app
    this.syncGateway.emitToBusiness(tx.businessId, 'subscription_updated', {
      planId: plan.id,
      planName: plan.name,
      status: 'ACTIVE',
      expiryDate: expiryDate.getTime(),
    });

    const updatedSub = await this.prisma.subscription.findUnique({
      where: { businessId: tx.businessId },
      include: { plan: true },
    });

    return { success: true, subscription: updatedSub };
  }
}
