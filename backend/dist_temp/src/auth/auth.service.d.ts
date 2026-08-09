import { PrismaService } from '../prisma/prisma.service';
import { RegisterDto, LoginDto } from './dto/auth.dto';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { SyncGateway } from '../sync/sync.gateway';
export declare class AuthService {
    private prisma;
    private jwtService;
    private config;
    private syncGateway;
    constructor(prisma: PrismaService, jwtService: JwtService, config: ConfigService, syncGateway: SyncGateway);
    signupLocal(dto: RegisterDto): Promise<{
        userId: string;
        businessId: string;
        setupCompleted: boolean;
        role: import(".prisma/client").$Enums.UserRole;
        access_token: string;
        refresh_token: string;
    }>;
    signinLocal(dto: LoginDto): Promise<{
        userId: string;
        businessId: string;
        setupCompleted: boolean;
        role: import(".prisma/client").$Enums.UserRole;
        access_token: string;
        refresh_token: string;
    }>;
    googleSignin(dto: {
        email: string;
        name?: string;
        googleId?: string;
    }): Promise<{
        userId: string;
        businessId: string;
        role: import(".prisma/client").$Enums.UserRole;
        isNewAccount: boolean;
        setupCompleted: boolean;
        access_token: string;
        refresh_token: string;
    }>;
    staffSignin(dto: {
        username: string;
        password?: string;
        deviceId?: string;
        deviceName?: string;
    }): Promise<{
        staffId: string;
        name: string;
        businessId: string;
        role: import(".prisma/client").$Enums.UserRole;
        permissions: string[];
        access_token: string;
        refresh_token: string;
    }>;
    logout(userId: string): Promise<void>;
    refreshTokens(userId: string, rt: string): Promise<{
        access_token: string;
        refresh_token: string;
    }>;
    saveRefreshToken(userId: string, rt: string): Promise<void>;
    getTokens(userId: string, email: string, businessId: string, role: string): Promise<{
        access_token: string;
        refresh_token: string;
    }>;
}
