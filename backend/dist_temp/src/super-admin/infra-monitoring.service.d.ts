import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';
export declare class InfraMonitoringService {
    private prisma;
    private syncGateway;
    private readonly logger;
    constructor(prisma: PrismaService, syncGateway: SyncGateway);
    getOverview(): Promise<{
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
}
