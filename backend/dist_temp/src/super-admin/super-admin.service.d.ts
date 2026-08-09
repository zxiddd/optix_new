import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';
export declare class SuperAdminService {
    private prisma;
    private syncGateway;
    private readonly logger;
    constructor(prisma: PrismaService, syncGateway: SyncGateway);
    getBusinesses(query: {
        page?: number;
        limit?: number;
        search?: string;
        planId?: string;
        status?: string;
        country?: string;
    }): Promise<{
        items: ({
            users: {
                email: string;
            }[];
            subscriptions: ({
                plan: {
                    id: string;
                    name: string;
                    createdAt: Date;
                    price: import("@prisma/client/runtime/library").Decimal;
                    billingPeriod: string;
                    features: import("@prisma/client/runtime/library").JsonValue;
                };
            } & {
                id: string;
                country: string;
                createdAt: Date;
                updatedAt: Date;
                businessId: string;
                planId: string;
                status: import(".prisma/client").$Enums.SubscriptionStatus;
                currency: string;
                billingCycle: import(".prisma/client").$Enums.BillingCycle;
                billsUsed: number;
                productsUsed: number;
                activationCode: string | null;
                expiryDate: Date;
                renewalDate: Date | null;
            })[];
            _count: {
                staff: number;
                products: number;
                orders: number;
            };
        } & {
            id: string;
            name: string;
            email: string | null;
            phone: string | null;
            address: string | null;
            country: string;
            setupCompleted: boolean;
            tokenCounter: number;
            lastTokenResetDateTime: Date;
            createdAt: Date;
            updatedAt: Date;
            isDeleted: boolean;
            deletedAt: Date | null;
            deletedBy: string | null;
        })[];
        meta: {
            total: number;
            page: number;
            lastPage: number;
        };
    }>;
    getBusinessDetail(id: string): Promise<{
        recentOrders: any[] | ({
            items: {
                id: string;
                price: import("@prisma/client/runtime/library").Decimal;
                unit: string | null;
                orderId: string;
                productId: string;
                productName: string;
                quantity: number | null;
                weight: import("@prisma/client/runtime/library").Decimal | null;
            }[];
        } & {
            id: string;
            createdAt: Date;
            businessId: string;
            status: import(".prisma/client").$Enums.OrderStatus;
            tokenNumber: string;
            invoiceNumber: string;
            subtotal: import("@prisma/client/runtime/library").Decimal;
            discount: import("@prisma/client/runtime/library").Decimal;
            tax: import("@prisma/client/runtime/library").Decimal;
            total: import("@prisma/client/runtime/library").Decimal;
            paymentMethod: import(".prisma/client").$Enums.PaymentMethod;
            cashierName: string;
            customerName: string | null;
            customerId: string | null;
        })[];
        recentLogs: any[] | ({
            staff: {
                name: string;
            };
        } & {
            id: string;
            createdAt: Date;
            businessId: string;
            ipAddress: string | null;
            staffId: string | null;
            action: string;
            entityType: string | null;
            entityId: string | null;
            metadata: import("@prisma/client/runtime/library").JsonValue | null;
            deviceId: string | null;
            isSuspicious: boolean;
            severity: string;
        })[];
        staffList: any[] | {
            id: string;
            name: string;
            email: string | null;
            phone: string | null;
            createdAt: Date;
            updatedAt: Date;
            businessId: string;
            password: string;
            role: import(".prisma/client").$Enums.UserRole;
            username: string;
            isDisabled: boolean;
            failedLoginCount: number;
            lastFailedLoginAt: Date | null;
            lastActivityAt: Date | null;
        }[];
        productsList: any[] | ({
            category: {
                id: string;
                name: string;
                isDeleted: boolean;
                businessId: string;
                version: number;
                lastModified: Date;
                sortOrder: number;
            };
        } & {
            id: string;
            name: string;
            isDeleted: boolean;
            businessId: string;
            price: import("@prisma/client/runtime/library").Decimal;
            description: string | null;
            barcode: string | null;
            sku: string | null;
            pricingType: import(".prisma/client").$Enums.PricingType;
            unit: string;
            categoryId: string;
            isOutOfStock: boolean;
            imageUrl: string | null;
            version: number;
            lastModified: Date;
        })[];
        settings: {
            id: string;
            businessId: string;
            currency: string;
            openingTime: string;
            closingTime: string;
            timezone: string;
            taxEnabled: boolean;
            taxPercentage: import("@prisma/client/runtime/library").Decimal;
            lastResetBusinessDate: string | null;
        };
        users: {
            id: string;
            email: string;
            createdAt: Date;
            role: import(".prisma/client").$Enums.UserRole;
        }[];
        subscriptions: ({
            plan: {
                id: string;
                name: string;
                createdAt: Date;
                price: import("@prisma/client/runtime/library").Decimal;
                billingPeriod: string;
                features: import("@prisma/client/runtime/library").JsonValue;
            };
        } & {
            id: string;
            country: string;
            createdAt: Date;
            updatedAt: Date;
            businessId: string;
            planId: string;
            status: import(".prisma/client").$Enums.SubscriptionStatus;
            currency: string;
            billingCycle: import(".prisma/client").$Enums.BillingCycle;
            billsUsed: number;
            productsUsed: number;
            activationCode: string | null;
            expiryDate: Date;
            renewalDate: Date | null;
        })[];
        devices: {
            id: string;
            businessId: string;
            lastSeen: Date;
            userId: string | null;
            deviceName: string;
            deviceModel: string | null;
            androidVersion: string | null;
            appVersion: string | null;
            batteryLevel: number | null;
            ipAddress: string | null;
            currentScreen: string | null;
            connectionStatus: string;
            pushToken: string | null;
        }[];
        transactions: {
            id: string;
            country: string;
            createdAt: Date;
            businessId: string;
            planId: string;
            status: string;
            currency: string;
            billingCycle: import(".prisma/client").$Enums.BillingCycle;
            subscriptionId: string | null;
            amount: import("@prisma/client/runtime/library").Decimal;
            razorpayOrderId: string;
            razorpayPaymentId: string | null;
            razorpaySignature: string | null;
            failureReason: string | null;
            gatewayMetadata: import("@prisma/client/runtime/library").JsonValue | null;
            capturedAt: Date | null;
        }[];
        _count: {
            staff: number;
            categories: number;
            products: number;
            orders: number;
            devices: number;
        };
        id: string;
        name: string;
        email: string | null;
        phone: string | null;
        address: string | null;
        country: string;
        setupCompleted: boolean;
        tokenCounter: number;
        lastTokenResetDateTime: Date;
        createdAt: Date;
        updatedAt: Date;
        isDeleted: boolean;
        deletedAt: Date | null;
        deletedBy: string | null;
    }>;
    updateBusinessStatus(id: string, status: 'ACTIVE' | 'EXPIRED' | 'CANCELLED' | 'SUSPENDED'): Promise<{
        id: string;
        country: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        planId: string;
        status: import(".prisma/client").$Enums.SubscriptionStatus;
        currency: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        billsUsed: number;
        productsUsed: number;
        activationCode: string | null;
        expiryDate: Date;
        renewalDate: Date | null;
    }>;
    resetTrialLimits(id: string): Promise<{
        id: string;
        country: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        planId: string;
        status: import(".prisma/client").$Enums.SubscriptionStatus;
        currency: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        billsUsed: number;
        productsUsed: number;
        activationCode: string | null;
        expiryDate: Date;
        renewalDate: Date | null;
    }>;
    getAllPayments(filters: {
        status?: string;
        businessId?: string;
        planId?: string;
        country?: string;
        currency?: string;
        gateway?: string;
        dateFrom?: string;
        dateTo?: string;
        page?: number;
        limit?: number;
        search?: string;
    }): Promise<{
        items: ({
            business: {
                id: string;
                name: string;
                country: string;
                users: {
                    email: string;
                }[];
            };
        } & {
            id: string;
            country: string;
            createdAt: Date;
            businessId: string;
            planId: string;
            status: string;
            currency: string;
            billingCycle: import(".prisma/client").$Enums.BillingCycle;
            subscriptionId: string | null;
            amount: import("@prisma/client/runtime/library").Decimal;
            razorpayOrderId: string;
            razorpayPaymentId: string | null;
            razorpaySignature: string | null;
            failureReason: string | null;
            gatewayMetadata: import("@prisma/client/runtime/library").JsonValue | null;
            capturedAt: Date | null;
        })[];
        meta: {
            total: number;
            page: number;
            lastPage: number;
        };
    }>;
    getPaymentDetail(id: string): Promise<{
        business: {
            users: {
                email: string;
            }[];
        } & {
            id: string;
            name: string;
            email: string | null;
            phone: string | null;
            address: string | null;
            country: string;
            setupCompleted: boolean;
            tokenCounter: number;
            lastTokenResetDateTime: Date;
            createdAt: Date;
            updatedAt: Date;
            isDeleted: boolean;
            deletedAt: Date | null;
            deletedBy: string | null;
        };
        subscription: {
            plan: {
                id: string;
                name: string;
                createdAt: Date;
                price: import("@prisma/client/runtime/library").Decimal;
                billingPeriod: string;
                features: import("@prisma/client/runtime/library").JsonValue;
            };
        } & {
            id: string;
            country: string;
            createdAt: Date;
            updatedAt: Date;
            businessId: string;
            planId: string;
            status: import(".prisma/client").$Enums.SubscriptionStatus;
            currency: string;
            billingCycle: import(".prisma/client").$Enums.BillingCycle;
            billsUsed: number;
            productsUsed: number;
            activationCode: string | null;
            expiryDate: Date;
            renewalDate: Date | null;
        };
    } & {
        id: string;
        country: string;
        createdAt: Date;
        businessId: string;
        planId: string;
        status: string;
        currency: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        subscriptionId: string | null;
        amount: import("@prisma/client/runtime/library").Decimal;
        razorpayOrderId: string;
        razorpayPaymentId: string | null;
        razorpaySignature: string | null;
        failureReason: string | null;
        gatewayMetadata: import("@prisma/client/runtime/library").JsonValue | null;
        capturedAt: Date | null;
    }>;
    refundPayment(id: string, reason?: string, partial?: number): Promise<{
        id: string;
        country: string;
        createdAt: Date;
        businessId: string;
        planId: string;
        status: string;
        currency: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        subscriptionId: string | null;
        amount: import("@prisma/client/runtime/library").Decimal;
        razorpayOrderId: string;
        razorpayPaymentId: string | null;
        razorpaySignature: string | null;
        failureReason: string | null;
        gatewayMetadata: import("@prisma/client/runtime/library").JsonValue | null;
        capturedAt: Date | null;
    }>;
    getAllSubscriptions(filters: {
        status?: string;
        planId?: string;
        country?: string;
        page?: number;
        limit?: number;
        search?: string;
    }): Promise<{
        items: ({
            business: {
                id: string;
                name: string;
                country: string;
                users: {
                    email: string;
                }[];
            };
            plan: {
                id: string;
                name: string;
                createdAt: Date;
                price: import("@prisma/client/runtime/library").Decimal;
                billingPeriod: string;
                features: import("@prisma/client/runtime/library").JsonValue;
            };
        } & {
            id: string;
            country: string;
            createdAt: Date;
            updatedAt: Date;
            businessId: string;
            planId: string;
            status: import(".prisma/client").$Enums.SubscriptionStatus;
            currency: string;
            billingCycle: import(".prisma/client").$Enums.BillingCycle;
            billsUsed: number;
            productsUsed: number;
            activationCode: string | null;
            expiryDate: Date;
            renewalDate: Date | null;
        })[];
        meta: {
            total: number;
            page: number;
            lastPage: number;
        };
    }>;
    getSubscriptionDetail(id: string): Promise<{
        business: {
            users: {
                email: string;
            }[];
        } & {
            id: string;
            name: string;
            email: string | null;
            phone: string | null;
            address: string | null;
            country: string;
            setupCompleted: boolean;
            tokenCounter: number;
            lastTokenResetDateTime: Date;
            createdAt: Date;
            updatedAt: Date;
            isDeleted: boolean;
            deletedAt: Date | null;
            deletedBy: string | null;
        };
        transactions: {
            id: string;
            country: string;
            createdAt: Date;
            businessId: string;
            planId: string;
            status: string;
            currency: string;
            billingCycle: import(".prisma/client").$Enums.BillingCycle;
            subscriptionId: string | null;
            amount: import("@prisma/client/runtime/library").Decimal;
            razorpayOrderId: string;
            razorpayPaymentId: string | null;
            razorpaySignature: string | null;
            failureReason: string | null;
            gatewayMetadata: import("@prisma/client/runtime/library").JsonValue | null;
            capturedAt: Date | null;
        }[];
        plan: {
            id: string;
            name: string;
            createdAt: Date;
            price: import("@prisma/client/runtime/library").Decimal;
            billingPeriod: string;
            features: import("@prisma/client/runtime/library").JsonValue;
        };
    } & {
        id: string;
        country: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        planId: string;
        status: import(".prisma/client").$Enums.SubscriptionStatus;
        currency: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        billsUsed: number;
        productsUsed: number;
        activationCode: string | null;
        expiryDate: Date;
        renewalDate: Date | null;
    }>;
    changePlan(businessId: string, planId: string, billingCycle: 'MONTHLY' | 'YEARLY'): Promise<{
        id: string;
        country: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        planId: string;
        status: import(".prisma/client").$Enums.SubscriptionStatus;
        currency: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        billsUsed: number;
        productsUsed: number;
        activationCode: string | null;
        expiryDate: Date;
        renewalDate: Date | null;
    }>;
    extendSubscription(businessId: string, days: number): Promise<{
        id: string;
        country: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        planId: string;
        status: import(".prisma/client").$Enums.SubscriptionStatus;
        currency: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        billsUsed: number;
        productsUsed: number;
        activationCode: string | null;
        expiryDate: Date;
        renewalDate: Date | null;
    }>;
    updateSubscriptionStatus(businessId: string, status: string): Promise<{
        id: string;
        country: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        planId: string;
        status: import(".prisma/client").$Enums.SubscriptionStatus;
        currency: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        billsUsed: number;
        productsUsed: number;
        activationCode: string | null;
        expiryDate: Date;
        renewalDate: Date | null;
    }>;
    getActivationCodes(filters: {
        search?: string;
        planId?: string;
        isActive?: string;
        page?: number;
        limit?: number;
    }): Promise<{
        items: {
            id: string;
            createdAt: Date;
            updatedAt: Date;
            planId: string;
            billingCycle: import(".prisma/client").$Enums.BillingCycle;
            code: string;
            maxUses: number;
            usedCount: number;
            countryRestriction: string | null;
            isActive: boolean;
            expiresAt: Date | null;
            notes: string | null;
        }[];
        meta: {
            total: number;
            page: number;
            lastPage: number;
        };
    }>;
    createActivationCode(data: {
        planId: string;
        billingCycle: 'MONTHLY' | 'YEARLY';
        maxUses: number;
        countryRestriction?: string;
        expiresAt?: string;
        notes?: string;
    }): Promise<{
        id: string;
        createdAt: Date;
        updatedAt: Date;
        planId: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        code: string;
        maxUses: number;
        usedCount: number;
        countryRestriction: string | null;
        isActive: boolean;
        expiresAt: Date | null;
        notes: string | null;
    }>;
    bulkCreateActivationCodes(data: {
        count: number;
        planId: string;
        billingCycle: 'MONTHLY' | 'YEARLY';
        maxUses: number;
        countryRestriction?: string;
        expiresAt?: string;
        notes?: string;
    }): Promise<{
        created: number;
        codes: string[];
    }>;
    deactivateCode(id: string): Promise<{
        id: string;
        createdAt: Date;
        updatedAt: Date;
        planId: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        code: string;
        maxUses: number;
        usedCount: number;
        countryRestriction: string | null;
        isActive: boolean;
        expiresAt: Date | null;
        notes: string | null;
    }>;
    deleteCode(id: string): Promise<{
        id: string;
        createdAt: Date;
        updatedAt: Date;
        planId: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        code: string;
        maxUses: number;
        usedCount: number;
        countryRestriction: string | null;
        isActive: boolean;
        expiresAt: Date | null;
        notes: string | null;
    }>;
    private generateCode;
    getAdminAuditLogs(filters: {
        businessId?: string;
        action?: string;
        entity?: string;
        page?: number;
        limit?: number;
    }): Promise<{
        items: ({
            business: {
                name: string;
            };
        } & {
            id: string;
            createdAt: Date;
            businessId: string;
            userId: string | null;
            ipAddress: string | null;
            action: string;
            entityId: string | null;
            entity: string;
            oldValue: import("@prisma/client/runtime/library").JsonValue | null;
            newValue: import("@prisma/client/runtime/library").JsonValue | null;
            endpoint: string | null;
            requestId: string | null;
        })[];
        meta: {
            total: number;
            page: number;
            lastPage: number;
        };
    }>;
    getRevenueStats(): Promise<{
        today: {
            revenue: number;
            count: number;
        };
        month: {
            revenue: number;
            count: number;
        };
        year: {
            revenue: number;
            count: number;
        };
        total: {
            revenue: number;
            count: number;
        };
        mrr: number;
        arr: number;
        avgRevPerBusiness: number;
        activeSubscriptions: number;
        failedPayments: number;
        refunds: number;
        planBreakdown: (import(".prisma/client").Prisma.PickEnumerable<import(".prisma/client").Prisma.PaymentTransactionGroupByOutputType, ("planId" | "billingCycle")[]> & {
            _count: number;
            _sum: {
                amount: import("@prisma/client/runtime/library").Decimal;
            };
        })[];
    }>;
    getDashboardStats(): Promise<{
        totalRevenue: number | import("@prisma/client/runtime/library").Decimal;
        activeSubscriptions: number;
    }>;
    getDashboardOverview(): Promise<{
        totalBusinesses: number;
        onlineBusinesses: number;
        trialUsers: number;
        starterUsers: number;
        growthUsers: number;
        monthlyRevenue: number;
        pendingPayments: number;
        failedPayments: number;
        serverStatus: string;
        socketConnections: number;
        revenueTrend: {
            name: string;
            value: number;
        }[];
        activities: {
            id: string;
            type: string;
            title: string;
            description: string;
            timestamp: string;
            status: string;
        }[] | {
            id: string;
            type: string;
            title: string;
            description: string;
            timestamp: string;
        }[];
    }>;
    getFeatureFlags(filters: {
        search?: string;
        level?: string;
        status?: string;
        page?: number;
        limit?: number;
    }): Promise<{
        items: {
            id: string;
            createdAt: Date;
            updatedAt: Date;
            businessId: string | null;
            status: string;
            notes: string | null;
            featureKey: string;
            level: string;
            target: string | null;
        }[];
        meta: {
            total: number;
            page: number;
            lastPage: number;
        };
    }>;
    upsertFeatureFlag(data: {
        featureKey: string;
        status: 'ON' | 'OFF' | 'BETA' | 'MAINTENANCE';
        level: 'GLOBAL' | 'COUNTRY' | 'PLAN' | 'BUSINESS';
        target?: string;
        notes?: string;
        businessId?: string;
    }): Promise<any>;
    deleteFeatureFlag(id: string): Promise<{
        success: boolean;
    }>;
    getEffectiveFeatureFlags(businessId?: string): Promise<Record<string, string>>;
    sendRemoteCommand(data: {
        command: string;
        businessId: string;
        deviceId?: string;
        payload?: any;
    }): Promise<{
        success: boolean;
        command: string;
        businessId: string;
    }>;
    executeBulkAction(data: {
        action: string;
        businessIds: string[];
        payload?: any;
    }): Promise<{
        processed: number;
        results: any[];
    }>;
    sendAdminNotification(data: {
        targetType: 'ALL' | 'BUSINESS' | 'PLAN';
        businessId?: string;
        planId?: string;
        title: string;
        message: string;
        type?: string;
        severity?: string;
    }): Promise<{
        success: boolean;
        message: string;
        sentCount?: undefined;
    } | {
        success: boolean;
        sentCount: number;
        message: string;
    }>;
    getGlobalConfig(): Promise<{
        id: string;
        updatedAt: Date;
        maintenanceMode: boolean;
        maintenanceMessage: string;
        minSupportedAppVersion: string;
        latestStableVersion: string;
        forceUpdate: boolean;
        apiEndpoint: string;
        webSocketEndpoint: string;
        supportEmail: string;
        supportPhone: string;
        supportWhatsApp: string;
    }>;
    updateGlobalConfig(data: any): Promise<{
        id: string;
        updatedAt: Date;
        maintenanceMode: boolean;
        maintenanceMessage: string;
        minSupportedAppVersion: string;
        latestStableVersion: string;
        forceUpdate: boolean;
        apiEndpoint: string;
        webSocketEndpoint: string;
        supportEmail: string;
        supportPhone: string;
        supportWhatsApp: string;
    }>;
    getLiveStatus(): Promise<{
        backendVersion: string;
        serverTime: string;
        maintenanceMode: boolean;
        minSupportedAppVersion: string;
        latestStableVersion: string;
        connectedDevices: number;
        totalDevices: number;
        syncQueue: {
            pending: number;
            failed: number;
        };
        versionDistribution: {
            version: string;
            count: number;
        }[];
    }>;
    getDevices(filters: {
        businessId?: string;
        search?: string;
        connectionStatus?: string;
        page?: number;
        limit?: number;
    }): Promise<{
        items: {
            id: string;
            businessId: string;
            business: {
                id: string;
                name: string;
            };
            deviceName: string;
            deviceModel: string;
            androidVersion: string;
            appVersion: string;
            batteryLevel: number;
            ipAddress: string;
            currentScreen: string;
            connectionStatus: string;
            lastSeen: string;
        }[];
        meta: {
            total: number;
            page: number;
            lastPage: number;
        };
    } | {
        items: {
            connectionStatus: string;
            business: {
                id: string;
                name: string;
            };
            user: {
                email: string;
            };
            id: string;
            businessId: string;
            lastSeen: Date;
            userId: string | null;
            deviceName: string;
            deviceModel: string | null;
            androidVersion: string | null;
            appVersion: string | null;
            batteryLevel: number | null;
            ipAddress: string | null;
            currentScreen: string | null;
            pushToken: string | null;
        }[];
        meta: {
            total: number;
            page: number;
            lastPage: number;
        };
    }>;
    updateDeviceTelemetry(deviceId: string, data: {
        appVersion?: string;
        batteryLevel?: number;
        ipAddress?: string;
        currentScreen?: string;
    }): Promise<{
        id: string;
        businessId: string;
        lastSeen: Date;
        userId: string | null;
        deviceName: string;
        deviceModel: string | null;
        androidVersion: string | null;
        appVersion: string | null;
        batteryLevel: number | null;
        ipAddress: string | null;
        currentScreen: string | null;
        connectionStatus: string;
        pushToken: string | null;
    }>;
    remoteLogoutDevice(deviceId: string): Promise<{
        success: boolean;
        deviceId: string;
    }>;
    createBusiness(data: {
        name: string;
        email: string;
        phone?: string;
        address?: string;
        country?: string;
        planId?: string;
    }): Promise<{
        business: {
            id: string;
            name: string;
            email: string | null;
            phone: string | null;
            address: string | null;
            country: string;
            setupCompleted: boolean;
            tokenCounter: number;
            lastTokenResetDateTime: Date;
            createdAt: Date;
            updatedAt: Date;
            isDeleted: boolean;
            deletedAt: Date | null;
            deletedBy: string | null;
        };
    }>;
    updateBusiness(id: string, data: {
        name?: string;
        email?: string;
        phone?: string;
        address?: string;
        country?: string;
    }): Promise<{
        id: string;
        name: string;
        email: string | null;
        phone: string | null;
        address: string | null;
        country: string;
        setupCompleted: boolean;
        tokenCounter: number;
        lastTokenResetDateTime: Date;
        createdAt: Date;
        updatedAt: Date;
        isDeleted: boolean;
        deletedAt: Date | null;
        deletedBy: string | null;
    }>;
    deleteBusiness(id: string): Promise<{
        success: boolean;
        id: string;
    }>;
    createPayment(data: {
        businessId: string;
        amount: number;
        planId: string;
        billingCycle?: string;
        gatewayPaymentId?: string;
        gatewayOrderId?: string;
        status?: string;
    }): Promise<{
        id: string;
        country: string;
        createdAt: Date;
        businessId: string;
        planId: string;
        status: string;
        currency: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        subscriptionId: string | null;
        amount: import("@prisma/client/runtime/library").Decimal;
        razorpayOrderId: string;
        razorpayPaymentId: string | null;
        razorpaySignature: string | null;
        failureReason: string | null;
        gatewayMetadata: import("@prisma/client/runtime/library").JsonValue | null;
        capturedAt: Date | null;
    }>;
    updatePayment(id: string, data: {
        status?: string;
        amount?: number;
        gatewayPaymentId?: string;
        gatewayOrderId?: string;
        planId?: string;
    }): Promise<{
        id: string;
        country: string;
        createdAt: Date;
        businessId: string;
        planId: string;
        status: string;
        currency: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        subscriptionId: string | null;
        amount: import("@prisma/client/runtime/library").Decimal;
        razorpayOrderId: string;
        razorpayPaymentId: string | null;
        razorpaySignature: string | null;
        failureReason: string | null;
        gatewayMetadata: import("@prisma/client/runtime/library").JsonValue | null;
        capturedAt: Date | null;
    }>;
    createSubscription(data: {
        businessId: string;
        planId: string;
        status?: string;
        expiryDate?: string;
    }): Promise<{
        id: string;
        country: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        planId: string;
        status: import(".prisma/client").$Enums.SubscriptionStatus;
        currency: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        billsUsed: number;
        productsUsed: number;
        activationCode: string | null;
        expiryDate: Date;
        renewalDate: Date | null;
    }>;
    updateSubscription(id: string, data: {
        planId?: string;
        status?: string;
        expiryDate?: string;
    }): Promise<{
        id: string;
        country: string;
        createdAt: Date;
        updatedAt: Date;
        businessId: string;
        planId: string;
        status: import(".prisma/client").$Enums.SubscriptionStatus;
        currency: string;
        billingCycle: import(".prisma/client").$Enums.BillingCycle;
        billsUsed: number;
        productsUsed: number;
        activationCode: string | null;
        expiryDate: Date;
        renewalDate: Date | null;
    }>;
    getTableList(): {
        name: string;
        description: string;
    }[];
    private getModelName;
    getTableRows(tableName: string, page?: any, limit?: any, search?: string): Promise<{
        items: any;
        meta: {
            total: any;
            page: number;
            lastPage: number;
        };
    }>;
    updateTableRow(tableName: string, id: string, data: Record<string, any>): Promise<any>;
    deleteTableRow(tableName: string, id: string): Promise<{
        success: boolean;
        id: string;
    }>;
    createTableRow(tableName: string, data: Record<string, any>): Promise<any>;
    private writeAdminAuditLog;
}
