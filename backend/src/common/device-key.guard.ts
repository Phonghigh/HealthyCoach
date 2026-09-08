import { CanActivate, ExecutionContext, Injectable, UnauthorizedException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { Reflector } from '@nestjs/core';
import { timingSafeEqual } from 'crypto';
import type { Request } from 'express';
import { IS_PUBLIC_KEY } from './public.decorator';

const DEVICE_KEY_HEADER = 'x-device-key';

/**
 * Single-user shared-secret guard. Compares X-Device-Key header against
 * DEVICE_KEY env var with a constant-time compare to avoid timing attacks.
 * Registered globally in app.module.ts; routes marked @Public() (e.g. /health) are exempt.
 */
@Injectable()
export class DeviceKeyGuard implements CanActivate {
  constructor(
    private readonly config: ConfigService,
    private readonly reflector: Reflector,
  ) {}

  canActivate(context: ExecutionContext): boolean {
    const isPublic = this.reflector.getAllAndOverride<boolean>(IS_PUBLIC_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);
    if (isPublic) {
      return true;
    }

    const request = context.switchToHttp().getRequest<Request>();
    const provided = request.headers[DEVICE_KEY_HEADER];
    const expected = this.config.get<string>('DEVICE_KEY') ?? '';

    if (typeof provided !== 'string' || !this.safeCompare(provided, expected)) {
      throw new UnauthorizedException('Invalid or missing X-Device-Key header');
    }

    return true;
  }

  private safeCompare(a: string, b: string): boolean {
    const bufferA = Buffer.from(a);
    const bufferB = Buffer.from(b);
    if (bufferA.length !== bufferB.length) {
      return false;
    }
    return timingSafeEqual(bufferA, bufferB);
  }
}
