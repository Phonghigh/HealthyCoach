import { Module } from '@nestjs/common';
import { APP_GUARD } from '@nestjs/core';
import { ConfigModule } from '@nestjs/config';
import { configValidationSchema } from './common/config';
import { DbModule } from './db/db.module';
import { HealthController } from './common/health.controller';
import { DeviceKeyGuard } from './common/device-key.guard';
import { ActivitiesModule } from './activities/activities.module';
import { PlansModule } from './plans/plans.module';
import { CheckinsModule } from './checkins/checkins.module';
import { NutritionModule } from './nutrition/nutrition.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      validationSchema: configValidationSchema,
    }),
    DbModule,
    ActivitiesModule,
    PlansModule,
    CheckinsModule,
    NutritionModule,
  ],
  controllers: [HealthController],
  providers: [{ provide: APP_GUARD, useClass: DeviceKeyGuard }],
})
export class AppModule {}
