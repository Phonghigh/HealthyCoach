import { Controller, Get } from '@nestjs/common';
import { PrismaService } from '../db/prisma.service';
import { Public } from './public.decorator';

interface HealthResponse {
  status: 'ok';
  dbConnected: boolean;
}

@Controller('health')
export class HealthController {
  constructor(private readonly prisma: PrismaService) {}

  @Public()
  @Get()
  async check(): Promise<HealthResponse> {
    const dbConnected = await this.prisma
      .$queryRaw`SELECT 1`
      .then(() => true)
      .catch(() => false);

    return { status: 'ok', dbConnected };
  }
}
