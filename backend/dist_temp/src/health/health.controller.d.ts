import { PrismaService } from '../prisma/prisma.service';
export declare class HealthController {
    private prisma;
    constructor(prisma: PrismaService);
    getHealth(): Promise<{
        status: string;
        data: {
            service: string;
            uptime: number;
            timestamp: number;
        };
    }>;
    getDbHealth(): Promise<{
        status: string;
        database: string;
        error?: undefined;
    } | {
        status: string;
        database: string;
        error: any;
    }>;
}
