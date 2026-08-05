import { Controller, Get } from '@nestjs/common';
import { ApiTags, ApiOperation } from '@nestjs/swagger';
import { PrismaService } from '../prisma/prisma.service';
import { Public } from '../auth/decorators';

@ApiTags('Health')
@Controller()
export class HealthController {
  constructor(private prisma: PrismaService) {}

  @Public()
  @Get('health')
  @ApiOperation({ summary: 'General health check' })
  async getHealth() {
    return {
      status: 'SUCCESS',
      data: {
        service: 'optix-backend-api',
        uptime: process.uptime(),
        timestamp: Date.now(),
      },
    };
  }

  @Public()
  @Get('health/db')
  @ApiOperation({ summary: 'Database connectivity health check' })
  async getDbHealth() {
    try {
      await this.prisma.$queryRaw`SELECT 1`;
      return { status: 'UP', database: 'PostgreSQL' };
    } catch (error) {
      return { status: 'DOWN', database: 'PostgreSQL', error: error.message };
    }
  }
}
