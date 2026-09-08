import { Module } from '@nestjs/common';
import { DbModule } from '../db/db.module';
import { ActivitiesController } from './activities.controller';
import { ActivitiesService } from './activities.service';

/** Ingestion + storage of completed runs (Phase 03). */
@Module({
  imports: [DbModule],
  controllers: [ActivitiesController],
  providers: [ActivitiesService],
})
export class ActivitiesModule {}
