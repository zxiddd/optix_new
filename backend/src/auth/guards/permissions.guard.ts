import { CanActivate, ExecutionContext, ForbiddenException, Injectable } from '@nestjs/common';
import { Reflector } from '@nestjs/core';
import { PERMISSIONS_KEY } from '../decorators/permissions.decorator';
import { PrismaService } from '../../prisma/prisma.service';

@Injectable()
export class PermissionsGuard implements CanActivate {
  constructor(
    private reflector: Reflector,
    private prisma: PrismaService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const requiredPermissions = this.reflector.getAllAndOverride<string[]>(PERMISSIONS_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);

    if (!requiredPermissions || requiredPermissions.length === 0) {
      return true;
    }

    const { user } = context.switchToHttp().getRequest();
    if (!user) {
      throw new ForbiddenException('User context missing');
    }

    const role = (user.role || '').toLowerCase();
    if (role === 'admin' || role === 'owner') {
      return true;
    }

    const staff = await this.prisma.staff.findUnique({
      where: { id: user.sub },
      include: { permissions: true },
    });

    if (!staff || staff.isDisabled) {
      throw new ForbiddenException('Staff account is disabled or missing');
    }

    const staffPermissions = staff.permissions.map((p) => p.action);
    const hasPermission = requiredPermissions.some((perm) => staffPermissions.includes(perm));

    if (!hasPermission) {
      throw new ForbiddenException(`Permission Denied: Missing required permission [${requiredPermissions.join(', ')}]`);
    }

    return true;
  }
}
