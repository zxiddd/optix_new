import { PrismaService } from '../prisma/prisma.service';
import { SyncGateway } from '../sync/sync.gateway';
import { BusinessClockService } from './business-clock.service';
export declare class BusinessResetService {
    private prisma;
    private syncGateway;
    private businessClockService;
    constructor(prisma: PrismaService, syncGateway: SyncGateway, businessClockService: BusinessClockService);
    resetBusinessDay(businessId: string, targetBusinessDate?: string, senderSocketId?: string): Promise<{
        businessId: string;
        lastResetBusinessDate: string;
        tokenCounter: number;
        resetAt: string;
        alreadyReset: boolean;
    } | {
        alreadyReset: boolean;
        businessId: string;
        lastResetBusinessDate: string;
        tokenCounter: number;
    }>;
}
