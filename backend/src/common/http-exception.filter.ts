import {
  ArgumentsHost,
  Catch,
  ExceptionFilter,
  HttpException,
  HttpStatus,
  Logger,
} from '@nestjs/common';
import type { Response } from 'express';

interface StableErrorBody {
  code: string;
  message: string;
}

/**
 * Emits a stable { code, message } error shape for every thrown exception.
 * Never logs request bodies (may contain health data) — only status + path.
 */
@Catch()
export class HttpExceptionFilter implements ExceptionFilter {
  private readonly logger = new Logger(HttpExceptionFilter.name);

  catch(exception: unknown, host: ArgumentsHost): void {
    const ctx = host.switchToHttp();
    const response = ctx.getResponse<Response>();

    const status =
      exception instanceof HttpException ? exception.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR;

    const body: StableErrorBody = this.toBody(exception, status);

    this.logger.warn(`[${status}] ${body.code}: ${body.message}`);
    response.status(status).json(body);
  }

  private toBody(exception: unknown, status: number): StableErrorBody {
    if (exception instanceof HttpException) {
      const response = exception.getResponse();
      const message =
        typeof response === 'string'
          ? response
          : ((response as { message?: string | string[] }).message ?? exception.message);
      return {
        code: HttpStatus[status] ?? 'ERROR',
        message: Array.isArray(message) ? message.join('; ') : message,
      };
    }

    return { code: 'INTERNAL_ERROR', message: 'Unexpected server error' };
  }
}
