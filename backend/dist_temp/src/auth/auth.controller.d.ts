import { AuthService } from './auth.service';
import { RegisterDto, LoginDto } from './dto/auth.dto';
export declare class AuthController {
    private authService;
    constructor(authService: AuthService);
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
    refreshTokens(userId: string, refreshToken: string): Promise<{
        access_token: string;
        refresh_token: string;
    }>;
}
