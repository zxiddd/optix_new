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
Object.defineProperty(exports, "__esModule", { value: true });
exports.AuthService = void 0;
const common_1 = require("@nestjs/common");
const prisma_service_1 = require("../prisma/prisma.service");
const argon = __importStar(require("@node-rs/argon2"));
const jwt_1 = require("@nestjs/jwt");
const config_1 = require("@nestjs/config");
const sync_gateway_1 = require("../sync/sync.gateway");
let AuthService = class AuthService {
    constructor(prisma, jwtService, config, syncGateway) {
        this.prisma = prisma;
        this.jwtService = jwtService;
        this.config = config;
        this.syncGateway = syncGateway;
    }
    async signupLocal(dto) {
        const existing = await this.prisma.user.findUnique({ where: { email: dto.email } });
        if (existing) {
            throw new common_1.ForbiddenException('Account already exists. Please login.');
        }
        const hash = await argon.hash(dto.password);
        const user = await this.prisma.$transaction(async (tx) => {
            const business = await tx.business.create({
                data: {
                    name: dto.businessName || 'Optix Store',
                    phone: dto.phone || '',
                    address: dto.address || '',
                    email: dto.email,
                    setupCompleted: true,
                },
            });
            return tx.user.create({
                data: {
                    email: dto.email,
                    password: hash,
                    businessId: business.id,
                    role: 'OWNER',
                },
            });
        });
        const tokens = await this.getTokens(user.id, user.email, user.businessId, user.role);
        await this.saveRefreshToken(user.id, tokens.refresh_token);
        return { ...tokens, userId: user.id, businessId: user.businessId, setupCompleted: true, role: user.role };
    }
    async signinLocal(dto) {
        const user = await this.prisma.user.findUnique({
            where: { email: dto.email },
        });
        if (!user)
            throw new common_1.ForbiddenException('Account not found. Please register first.');
        const passwordMatches = await argon.verify(user.password, dto.password);
        if (!passwordMatches)
            throw new common_1.ForbiddenException('Invalid credentials');
        const business = await this.prisma.business.findUnique({ where: { id: user.businessId } });
        const setupCompleted = !!(business && business.setupCompleted && business.name && business.name.trim().length > 0);
        const tokens = await this.getTokens(user.id, user.email, user.businessId, user.role);
        await this.saveRefreshToken(user.id, tokens.refresh_token);
        return { ...tokens, userId: user.id, businessId: user.businessId, setupCompleted, role: user.role };
    }
    async googleSignin(dto) {
        let user = await this.prisma.user.findUnique({
            where: { email: dto.email },
        });
        const isNew = !user;
        if (!user) {
            const randomPassword = await argon.hash(dto.googleId || dto.email + Date.now());
            user = await this.prisma.$transaction(async (tx) => {
                const business = await tx.business.create({
                    data: {
                        name: '',
                        email: dto.email,
                        setupCompleted: false,
                    },
                });
                return tx.user.create({
                    data: {
                        email: dto.email,
                        password: randomPassword,
                        businessId: business.id,
                        role: 'OWNER',
                    },
                });
            });
        }
        const business = await this.prisma.business.findUnique({ where: { id: user.businessId } });
        const isSetupCompleted = !!(business && business.setupCompleted && business.name && business.name.trim().length > 0);
        const tokens = await this.getTokens(user.id, user.email, user.businessId, user.role);
        await this.saveRefreshToken(user.id, tokens.refresh_token);
        return {
            ...tokens,
            userId: user.id,
            businessId: user.businessId,
            role: user.role,
            isNewAccount: isNew,
            setupCompleted: isSetupCompleted
        };
    }
    async staffSignin(dto) {
        const trimmedUsername = dto.username.trim();
        const staff = await this.prisma.staff.findFirst({
            where: {
                OR: [
                    { username: trimmedUsername },
                    { username: { startsWith: `${trimmedUsername}@` } },
                ],
            },
            include: { permissions: true },
        });
        if (!staff || staff.isDisabled)
            throw new common_1.ForbiddenException('Staff account disabled or not found');
        if (dto.password && staff.password) {
            const passwordMatches = await argon.verify(staff.password, dto.password);
            if (!passwordMatches) {
                const updated = await this.prisma.staff.update({
                    where: { id: staff.id },
                    data: { failedLoginCount: { increment: 1 }, lastFailedLoginAt: new Date() },
                });
                if (updated.failedLoginCount >= 5) {
                    await this.prisma.staffActivityLog.create({
                        data: {
                            staffId: staff.id,
                            businessId: staff.businessId,
                            action: 'SUSPICIOUS_FAILED_LOGINS',
                            entityType: 'AUTH',
                            metadata: { failedCount: updated.failedLoginCount },
                            isSuspicious: true,
                        },
                    });
                }
                throw new common_1.ForbiddenException('Invalid credentials');
            }
        }
        await this.prisma.staff.update({
            where: { id: staff.id },
            data: { failedLoginCount: 0, lastActivityAt: new Date() },
        });
        const session = await this.prisma.staffSession.create({
            data: {
                staffId: staff.id,
                businessId: staff.businessId,
                deviceId: dto.deviceId || 'Android',
                deviceName: dto.deviceName || 'Android Terminal',
                isActive: true,
            },
        });
        this.syncGateway.emitToBusiness(staff.businessId, 'staff.session.started', session);
        const actLog = await this.prisma.staffActivityLog.create({
            data: {
                staffId: staff.id,
                businessId: staff.businessId,
                action: 'STAFF_LOGIN',
                entityType: 'SESSION',
                entityId: session.id,
                deviceId: dto.deviceId,
                isSuspicious: false,
                severity: 'NORMAL',
            },
        });
        this.syncGateway.emitToBusiness(staff.businessId, 'staff.activity.created', actLog);
        const tokens = await this.getTokens(staff.id, staff.username, staff.businessId, staff.role);
        return {
            ...tokens,
            staffId: staff.id,
            name: staff.name,
            businessId: staff.businessId,
            role: staff.role,
            permissions: staff.permissions.map((p) => p.action),
        };
    }
    async logout(userId) {
        await this.prisma.refreshToken.updateMany({
            where: { userId, revokedAt: null },
            data: { revokedAt: new Date() },
        });
    }
    async refreshTokens(userId, rt) {
        const user = await this.prisma.user.findUnique({ where: { id: userId } });
        if (!user)
            throw new common_1.ForbiddenException('Access Denied');
        const tokens = await this.getTokens(user.id, user.email, user.businessId, user.role);
        await this.saveRefreshToken(user.id, tokens.refresh_token);
        return tokens;
    }
    async saveRefreshToken(userId, rt) {
        const hash = await argon.hash(rt);
        await this.prisma.refreshToken.create({
            data: {
                userId,
                tokenHash: hash,
                expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
            },
        });
    }
    async getTokens(userId, email, businessId, role) {
        const secret = this.config.get('JWT_ACCESS_SECRET') || this.config.get('JWT_SECRET') || 'optix_secret';
        const refreshSecret = this.config.get('JWT_REFRESH_SECRET') || secret;
        const [at, rt] = await Promise.all([
            this.jwtService.signAsync({ sub: userId, email, businessId, role }, { secret, expiresIn: '1d' }),
            this.jwtService.signAsync({ sub: userId, email, businessId, role }, { secret: refreshSecret, expiresIn: '7d' }),
        ]);
        return {
            access_token: at,
            refresh_token: rt,
        };
    }
};
exports.AuthService = AuthService;
exports.AuthService = AuthService = __decorate([
    (0, common_1.Injectable)(),
    __metadata("design:paramtypes", [prisma_service_1.PrismaService,
        jwt_1.JwtService,
        config_1.ConfigService,
        sync_gateway_1.SyncGateway])
], AuthService);
//# sourceMappingURL=auth.service.js.map