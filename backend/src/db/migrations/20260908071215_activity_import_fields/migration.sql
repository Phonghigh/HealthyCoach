-- AlterTable: add tz offset, max HR, elevation gain to completed_activities.
-- (source, external_id) unique constraint already exists from the init migration.
ALTER TABLE "completed_activities" ADD COLUMN "tz_offset_minutes" INTEGER NOT NULL DEFAULT 0;
ALTER TABLE "completed_activities" ALTER COLUMN "tz_offset_minutes" DROP DEFAULT;
ALTER TABLE "completed_activities" ADD COLUMN "max_heart_rate" INTEGER;
ALTER TABLE "completed_activities" ADD COLUMN "elevation_gain_meters" INTEGER;
