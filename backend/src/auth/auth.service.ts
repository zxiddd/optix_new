import { ForbiddenException, Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { RegisterDto, LoginDto } from './dto/auth.dto';
import * as argon from '@node-rs/argon2';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { SyncGateway } from '../sync/sync.gateway';

@Injectable()
export class AuthService {
  constructor(
    private prisma: PrismaService,
    private jwtService: JwtService,
    private config: ConfigService,
    private syncGateway: SyncGateway,
  ) {}

  async signupLocal(dto: RegisterDto) {
    // Check if account already exists
    const existing = await this.prisma.user.findUnique({ where: { email: dto.email } });
    if (existing) {
      throw new ForbiddenException('Account already exists. Please login.');
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

  async signinLocal(dto: LoginDto) {
    const user = await this.prisma.user.findUnique({
      where: { email: dto.email },
    });

    if (!user) throw new ForbiddenException('Account not found. Please register first.');

    const passwordMatches = await argon.verify(user.password, dto.password);
    if (!passwordMatches) throw new ForbiddenException('Invalid credentials');

    // Query business to return setupCompleted status
    const business = await this.prisma.business.findUnique({ where: { id: user.businessId } });
    const setupCompleted = !!(business && business.setupCompleted && business.name && business.name.trim().length > 0);

    const tokens = await this.getTokens(user.id, user.email, user.businessId, user.role);
    await this.saveRefreshToken(user.id, tokens.refresh_token);
    return { ...tokens, userId: user.id, businessId: user.businessId, setupCompleted, role: user.role };
  }

  async googleSignin(dto: { email: string; name?: string; googleId?: string }) {
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

  async staffSignin(dto: { username: string; password?: string; deviceId?: string; deviceName?: string }) {
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

    if (!staff || staff.isDisabled) throw new ForbiddenException('Staff account disabled or not found');

    if (dto.password && staff.password) {
      const passwordMatches = await argon.verify(staff.password, dto.password);
      if (!passwordMatches) {
        // Track failed login
        const updated = await this.prisma.staff.update({
          where: { id: staff.id },
          data: { failedLoginCount: { increment: 1 }, lastFailedLoginAt: new Date() },
        });
        // Suspicious activity threshold
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
        throw new ForbiddenException('Invalid credentials');
      }
    }

    // Reset failed login count on success
    await this.prisma.staff.update({
      where: { id: staff.id },
      data: { failedLoginCount: 0, lastActivityAt: new Date() },
    });

    // Create session record
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

    // Create Activity Log
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

  async logout(userId: string) {
    await this.prisma.refreshToken.updateMany({
      where: { userId, revokedAt: null },
      data: { revokedAt: new Date() },
    });
  }

  async refreshTokens(userId: string, rt: string) {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user) throw new ForbiddenException('Access Denied');

    const tokens = await this.getTokens(user.id, user.email, user.businessId, user.role);
    await this.saveRefreshToken(user.id, tokens.refresh_token);
    return tokens;
  }

  async saveRefreshToken(userId: string, rt: string) {
    const hash = await argon.hash(rt);
    await this.prisma.refreshToken.create({
      data: {
        userId,
        tokenHash: hash,
        expiresAt: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000),
      },
    });
  }

  async getTokens(userId: string, email: string, businessId: string, role: string) {
    const secret = this.config.get<string>('JWT_ACCESS_SECRET') || this.config.get<string>('JWT_SECRET') || 'optix_secret';
    const refreshSecret = this.config.get<string>('JWT_REFRESH_SECRET') || secret;

    const [at, rt] = await Promise.all([
      this.jwtService.signAsync(
        { sub: userId, email, businessId, role },
        { secret, expiresIn: '1d' },
      ),
      this.jwtService.signAsync(
        { sub: userId, email, businessId, role },
        { secret: refreshSecret, expiresIn: '7d' },
      ),
    ]);

    return {
      access_token: at,
      refresh_token: rt,
    };
  }
}
