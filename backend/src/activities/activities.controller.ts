import { BadRequestException, Body, Controller, Get, Post, Query } from '@nestjs/common';
import { ActivitiesService } from './activities.service';
import { ActivityImportBatchDto } from './dto/activity-import.dto';
import { ListActivitiesQueryDto } from './dto/list-activities-query.dto';

/**
 * Ingestion + read endpoints for completed activities. Both routes are
 * guarded by the global DeviceKeyGuard (not marked @Public()).
 */
@Controller('activities')
export class ActivitiesController {
  constructor(private readonly activitiesService: ActivitiesService) {}

  // Body is { activities: ActivityImportDto[] } — matches Android's
  // ActivityImportRequest wire shape. The global ValidationPipe (whitelist +
  // forbidNonWhitelisted) validates nested items via @ValidateNested/@Type
  // on ActivityImportBatchDto.
  @Post('import')
  async importBatch(@Body() body: ActivityImportBatchDto) {
    const results = await this.activitiesService.importBatch(body.activities);
    return { results };
  }

  @Get()
  async list(@Query() query: ListActivitiesQueryDto) {
    if (query.from.getTime() > query.to.getTime()) {
      throw new BadRequestException('from must be <= to');
    }
    return this.activitiesService.listByDateRange(query.from, query.to);
  }
}
