"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var PaymentsService_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.PaymentsService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
const config_1 = require("@nestjs/config");
const sync_gateway_1 = require("../sync/sync.gateway");
const Razorpay = require('razorpay');
const crypto = __importStar(require("crypto"));
let PaymentsService = PaymentsService_1 = class PaymentsService {
    constructor(prisma, config, syncGateway) {
        this.prisma = prisma;
        this.config = config;
        this.syncGateway = syncGateway;
        this.logger = new common_1.Logger(PaymentsService_1.name);
        const keyId = this.config.get('RAZORPAY_KEY_ID') || 'rzp_live_TMU8GBAj19LGFg';
        const keySecret = this.config.get('RAZORPAY_KEY_SECRET') || 'XMvL1kbLF5e0X1VzDwdNUhEA';
        this.razorpay = new Razorpay({
            key_id: keyId,
            key_secret: keySecret,
        });
        this.logger.log(`Razorpay Initialized in ${keyId.startsWith('rzp_test') ? 'TEST' : 'LIVE'} mode`);
    }
    async createOrder(businessId, planId, cycle) {
        this.logger.log(`[CREATE ORDER] Business: ${businessId}, Plan: ${planId}, Cycle: ${cycle}`);
        const business = await this.prisma.business.findUnique({
            where: { id: businessId },
            include: { settings: true },
        });
        if (!business)
            throw new common_1.BadRequestException('Business not found');
        let amount = 0;
        const currency = business.settings?.currency || 'INR';
        const country = (business.country || 'India').trim();
        const countryLower = country.toLowerCase();
        if (countryLower === 'india') {
            if (planId === 'STARTER')
                amount = cycle === 'MONTHLY' ? 499 : 4849;
            else if (planId === 'GROWTH')
                amount = cycle === 'MONTHLY' ? 999 : 9709;
        }
        else if (countryLower === 'saudi arabia') {
            if (planId === 'STARTER')
                amount = cycle === 'MONTHLY' ? 120 : 1296;
            else if (planId === 'GROWTH')
                amount = cycle === 'MONTHLY' ? 199 : 2149;
        }
        if (amount === 0)
            throw new common_1.BadRequestException('Invalid plan or country configuration');
        const options = {
            amount: amount * 100,
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
                key_id: this.config.get('RAZORPAY_KEY_ID') || 'rzp_live_TMU8GBAj19LGFg'
            };
        }
        catch (error) {
            this.logger.error('Razorpay Order Creation Failed', error);
            throw new common_1.BadRequestException('Failed to initiate payment');
        }
    }
    async verifyPayment(businessId, data) {
        const secret = this.config.get('RAZORPAY_KEY_SECRET') || 'XMvL1kbLF5e0X1VzDwdNUhEA';
        const body = data.razorpay_order_id + '|' + data.razorpay_payment_id;
        const expectedSignature = crypto
            .createHmac('sha256', secret)
            .update(body.toString())
            .digest('hex');
        if (expectedSignature !== data.razorpay_signature) {
            throw new common_1.BadRequestException('Invalid signature');
        }
        return this.completePayment(data.razorpay_order_id, data.razorpay_payment_id, data.razorpay_signature);
    }
    async handleWebhook(signature, rawBody) {
        const secret = this.config.get('RAZORPAY_WEBHOOK_SECRET') || 'OJMTJqo6Oc8yBrbioMVWClcu';
        const expectedSignature = crypto
            .createHmac('sha256', secret)
            .update(rawBody)
            .digest('hex');
        const event = JSON.parse(rawBody);
        this.logger.log(`Received Webhook: ${event.event}`);
        if (event.event === 'payment.captured') {
            const payload = event.payload.payment.entity;
            await this.completePayment(payload.order_id, payload.id, signature, payload);
        }
        else if (event.event === 'payment.failed') {
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
    async completePayment(orderId, paymentId, signature, metadata) {
        const tx = await this.prisma.paymentTransaction.findUnique({
            where: { razorpayOrderId: orderId },
        });
        if (!tx || tx.status === 'CAPTURED')
            return;
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
        let plan = await this.prisma.plan.findFirst({
            where: {
                OR: [{ id: tx.planId }, { name: tx.planId }],
            },
        });
        if (!plan) {
            plan = await this.prisma.plan.create({
                data: {
                    id: tx.planId,
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
        await this.prisma.paymentTransaction.update({
            where: { id: tx.id },
            data: { subscriptionId: subscription.id },
        });
        this.logger.log(`Subscription Activated for Business: ${tx.businessId}, Plan: ${tx.planId}`);
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
};
exports.PaymentsService = PaymentsService;
exports.PaymentsService = PaymentsService = PaymentsService_1 = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        config_1.ConfigService,
        sync_gateway_1.SyncGateway])
], PaymentsService);
//# sourceMappingURL=payments.service.js.map