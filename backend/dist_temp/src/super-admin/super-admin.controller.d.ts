import { SuperAdminService } from './super-admin.service';
import { InfraMonitoringService } from './infra-monitoring.service';
export declare class SuperAdminController {
    private readonly adminService;
    private readonly infraService;
    constructor(adminService: SuperAdminService, infraService: InfraMonitoringService);
    getBusinesses(page?: number, limit?: number, search?: string, planId?: string, status?: string, country?: string): Promise<{
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
    updateStatus(id: string, status: 'ACTIVE' | 'EXPIRED' | 'CANCELLED' | 'SUSPENDED'): Promise<{
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
    resetTrial(id: string): Promise<{
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
    getPayments(status?: string, businessId?: string, planId?: string, country?: string, currency?: string, dateFrom?: string, dateTo?: string, page?: number, limit?: number, search?: string): Promise<{
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
    getSubscriptions(status?: string, planId?: string, country?: string, page?: number, limit?: number, search?: string): Promise<{
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
    resetSubscriptionTrial(businessId: string): Promise<{
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
    getActivationCodes(search?: string, planId?: string, isActive?: string, page?: number, limit?: number): Promise<{
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
    createActivationCode(body: {
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
    bulkCreateActivationCodes(body: {
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
    getAuditLogs(businessId?: string, action?: string, entity?: string, page?: number, limit?: number): Promise<{
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
    getFeatureFlags(search?: string, level?: string, status?: string, page?: number, limit?: number): Promise<{
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
    upsertFeatureFlag(body: {
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
    sendRemoteCommand(body: {
        command: string;
        businessId: string;
        deviceId?: string;
        payload?: any;
    }): Promise<{
        success: boolean;
        command: string;
        businessId: string;
    }>;
    sendAdminNotification(body: any): Promise<{
        success: boolean;
        message: string;
        sentCount?: undefined;
    } | {
        success: boolean;
        sentCount: number;
        message: string;
    }>;
    executeBulkAction(body: {
        action: string;
        businessIds: string[];
        payload?: any;
    }): Promise<{
        processed: number;
        results: any[];
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
    updateGlobalConfig(body: any): Promise<{
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
    getDevices(businessId?: string, search?: string, connectionStatus?: string, page?: number, limit?: number): Promise<{
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
    remoteLogoutDevice(id: string): Promise<{
        success: boolean;
        deviceId: string;
    }>;
    getInfraOverview(): Promise<{
        backendStatus: string;
        apiStatus: string;
        databaseStatus: string;
        redisStatus: string;
        webSocketStatus: string;
        storageStatus: string;
        sslStatus: string;
        domainStatus: string;
        currentVersion: string;
        latestVersion: string;
        serverTime: string;
    }>;
    getServerHealth(): Promise<{
        cpuUsagePct: number;
        ramUsagePct: number;
        totalRamMb: number;
        usedRamMb: number;
        processHeapUsedMb: number;
        processRssMb: number;
        loadAverage: number[];
        diskUsagePct: number;
        diskSizeBytes: number;
        uploadsFileCount: number;
        networkUploadKbps: number;
        networkDownloadKbps: number;
        uptimeSeconds: number;
        processUptimeSeconds: number;
        cpuCores: number;
        cpuModel: string;
    }>;
    getDbMonitor(): Promise<{
        currentConnections: any;
        maxConnections: number;
        activeQueries: any;
        databaseSize: any;
        databaseSizeBytes: number;
        tablesCount: any;
        deadlocks: number;
        avgQueryTimeMs: number;
        tablesBreakdown: {
            name: string;
            count: number;
        }[];
        slowQueries: any[];
        error?: undefined;
    } | {
        currentConnections: number;
        maxConnections: number;
        activeQueries: number;
        databaseSize: string;
        tablesCount: number;
        deadlocks: number;
        avgQueryTimeMs: number;
        tablesBreakdown: any[];
        slowQueries: any[];
        error: any;
        databaseSizeBytes?: undefined;
    }>;
    getWebSocketMonitor(): Promise<{
        activeConnections: number;
        peakConnections: number;
        reconnectCount: number;
        messagesSent: number;
        messagesReceived: number;
        averageLatencyMs: number;
        status: string;
    }>;
    getApiMonitor(): Promise<{
        requestsPerMinute: number;
        avgResponseTimeMs: number;
        p95ResponseTimeMs: number;
        p99ResponseTimeMs: number;
        successPercentage: number;
        status2xx: number;
        status4xx: number;
        status5xx: number;
        topEndpoints: {
            path: string;
            count: number;
            avgMs: number;
        }[];
        slowestEndpoints: {
            path: string;
            avgMs: number;
        }[];
    }>;
    getBackgroundServices(): Promise<({
        name: string;
        status: string;
        uptime: string;
        lastRun: string;
        pendingQueue: number;
    } | {
        name: string;
        status: string;
        uptime: string;
        lastRun: string;
        pendingQueue?: undefined;
    })[]>;
    getContainers(): Promise<any[]>;
    restartContainer(id: string): Promise<{
        success: boolean;
        id: string;
        message: string;
    }>;
    freeRam(): Promise<{
        success: boolean;
        message: string;
        heapUsedMb: number;
        rssMb: number;
    }>;
    cleanDisk(): Promise<{
        success: boolean;
        message: string;
        freedMb: number;
    }>;
    getRealtimeLogs(filter?: string, search?: string, limit?: number): Promise<{
        time: string;
        level: string;
        service: string;
        message: string;
    }[]>;
    getErrorTracking(): Promise<{
        id: string;
        exception: string;
        message: string;
        frequency: number;
        lastSeen: string;
        status: string;
    }[]>;
    getBackups(): Promise<any[]>;
    createBackup(): Promise<{
        success: boolean;
        filename: string;
        size: string;
        createdAt: string;
    }>;
    getStorageStats(): Promise<{
        totalUsedMb: number;
        fileCount: number;
        breakdown: {
            category: string;
            sizeMb: number;
            fileCount: number;
        }[];
    }>;
    cleanupStorage(): Promise<{
        success: boolean;
        cleanedMb: number;
    }>;
    getSecurityStats(): Promise<{
        failedLoginAttempts: number;
        blockedIpsCount: number;
        activeUserSessions: number;
        activeStaffSessions: number;
        revokedTokensCount: number;
        rateLimitEventsCount: number;
        status: string;
    }>;
    getDeployments(): Promise<{
        currentVersion: string;
        latestVersion: string;
        buildNumber: string;
        releaseDate: string;
        history: {
            version: string;
            releaseDate: string;
            commit: string;
            status: string;
        }[];
    }>;
    getAlerts(): Promise<any[]>;
    getLiveFeed(): Promise<{
        id: string;
        type: string;
        text: string;
        time: string;
    }[]>;
    createBusiness(body: any): Promise<{
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
    updateBusiness(id: string, body: any): Promise<{
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
    createPayment(body: any): Promise<{
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
    updatePayment(id: string, body: any): Promise<{
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
    createSubscription(body: any): Promise<{
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
    updateSubscription(id: string, body: any): Promise<{
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
    getTableList(): Promise<{
        name: string;
        description: string;
    }[]>;
    getTableRows(tableName: string, page?: number, limit?: number, search?: string): Promise<{
        items: any;
        meta: {
            total: any;
            page: number;
            lastPage: number;
        };
    }>;
    updateTableRow(tableName: string, id: string, body: any): Promise<any>;
    deleteTableRow(tableName: string, id: string): Promise<{
        success: boolean;
        id: string;
    }>;
    createTableRow(tableName: string, body: any): Promise<any>;
}
