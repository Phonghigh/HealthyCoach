-- CreateEnum
CREATE TYPE "ActivitySource" AS ENUM ('health_connect', 'fit_upload', 'manual');

-- CreateEnum
CREATE TYPE "GoalType" AS ENUM ('completion', 'time_goal');

-- CreateEnum
CREATE TYPE "PlanStatus" AS ENUM ('draft', 'active', 'completed', 'abandoned');

-- CreateEnum
CREATE TYPE "CheckinType" AS ENUM ('post_run', 'pain');

-- CreateTable
CREATE TABLE "users" (
    "id" SERIAL NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "users_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "athlete_profiles" (
    "id" SERIAL NOT NULL,
    "user_id" INTEGER NOT NULL,
    "baseline_weekly_meters_min" INTEGER NOT NULL,
    "baseline_weekly_meters_max" INTEGER NOT NULL,
    "run_days_per_week" INTEGER NOT NULL,
    "long_run_day" TEXT NOT NULL,
    "sweat_rate_ml_per_hour" INTEGER,
    "dietary_notes" TEXT,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "athlete_profiles_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "race_goals" (
    "id" SERIAL NOT NULL,
    "user_id" INTEGER NOT NULL,
    "race_name" TEXT,
    "race_date" TIMESTAMP(3) NOT NULL,
    "distance_meters" INTEGER NOT NULL,
    "goal_type" "GoalType" NOT NULL,
    "goal_time_seconds" INTEGER,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "race_goals_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "training_plans" (
    "id" SERIAL NOT NULL,
    "user_id" INTEGER NOT NULL,
    "race_goal_id" INTEGER NOT NULL,
    "status" "PlanStatus" NOT NULL DEFAULT 'draft',
    "start_date" TIMESTAMP(3) NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "training_plans_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "training_weeks" (
    "id" SERIAL NOT NULL,
    "training_plan_id" INTEGER NOT NULL,
    "week_index" INTEGER NOT NULL,
    "week_start_date" TIMESTAMP(3) NOT NULL,
    "target_meters" INTEGER NOT NULL,
    "phase" TEXT NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "training_weeks_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "planned_workouts" (
    "id" SERIAL NOT NULL,
    "training_week_id" INTEGER NOT NULL,
    "scheduled_date" TIMESTAMP(3) NOT NULL,
    "workout_type" TEXT NOT NULL,
    "target_meters" INTEGER,
    "target_seconds" INTEGER,
    "target_pace_seconds_per_km" INTEGER,
    "notes" TEXT,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updated_at" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "planned_workouts_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "completed_activities" (
    "id" SERIAL NOT NULL,
    "user_id" INTEGER NOT NULL,
    "planned_workout_id" INTEGER,
    "source" "ActivitySource" NOT NULL,
    "external_id" TEXT NOT NULL,
    "started_at" TIMESTAMP(3) NOT NULL,
    "duration_seconds" INTEGER NOT NULL,
    "distance_meters" INTEGER NOT NULL,
    "avg_heart_rate" INTEGER,
    "avg_pace_seconds_per_km" INTEGER,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "completed_activities_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "subjective_checkins" (
    "id" SERIAL NOT NULL,
    "completed_activity_id" INTEGER,
    "user_id" INTEGER NOT NULL,
    "checkin_type" "CheckinType" NOT NULL,
    "rpe" INTEGER,
    "soreness_level" INTEGER,
    "mood_notes" TEXT,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "subjective_checkins_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "fueling_plans" (
    "id" SERIAL NOT NULL,
    "planned_workout_id" INTEGER,
    "user_id" INTEGER NOT NULL,
    "carb_grams_per_hour" INTEGER NOT NULL,
    "fluid_ml_per_hour" INTEGER NOT NULL,
    "sodium_mg_per_hour" INTEGER,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "fueling_plans_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "fueling_logs" (
    "id" SERIAL NOT NULL,
    "fueling_plan_id" INTEGER,
    "completed_activity_id" INTEGER,
    "user_id" INTEGER NOT NULL,
    "carb_grams_consumed" INTEGER,
    "fluid_ml_consumed" INTEGER,
    "notes" TEXT,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "fueling_logs_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "injury_flags" (
    "id" SERIAL NOT NULL,
    "completed_activity_id" INTEGER,
    "user_id" INTEGER NOT NULL,
    "body_part" TEXT NOT NULL,
    "pain_level" INTEGER NOT NULL,
    "resolved" BOOLEAN NOT NULL DEFAULT false,
    "notes" TEXT,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "injury_flags_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "plan_adjustments" (
    "id" SERIAL NOT NULL,
    "training_plan_id" INTEGER NOT NULL,
    "rule_version" TEXT NOT NULL,
    "input_snapshot" JSONB NOT NULL,
    "reason" TEXT NOT NULL,
    "status" TEXT NOT NULL DEFAULT 'proposed',
    "actor" TEXT NOT NULL,
    "created_at" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "applied_at" TIMESTAMP(3),

    CONSTRAINT "plan_adjustments_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "athlete_profiles_user_id_key" ON "athlete_profiles"("user_id");

-- CreateIndex
CREATE UNIQUE INDEX "training_weeks_training_plan_id_week_index_key" ON "training_weeks"("training_plan_id", "week_index");

-- CreateIndex
CREATE UNIQUE INDEX "completed_activities_source_external_id_key" ON "completed_activities"("source", "external_id");

-- AddForeignKey
ALTER TABLE "athlete_profiles" ADD CONSTRAINT "athlete_profiles_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "race_goals" ADD CONSTRAINT "race_goals_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "training_plans" ADD CONSTRAINT "training_plans_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "users"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "training_plans" ADD CONSTRAINT "training_plans_race_goal_id_fkey" FOREIGN KEY ("race_goal_id") REFERENCES "race_goals"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "training_weeks" ADD CONSTRAINT "training_weeks_training_plan_id_fkey" FOREIGN KEY ("training_plan_id") REFERENCES "training_plans"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "planned_workouts" ADD CONSTRAINT "planned_workouts_training_week_id_fkey" FOREIGN KEY ("training_week_id") REFERENCES "training_weeks"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "completed_activities" ADD CONSTRAINT "completed_activities_planned_workout_id_fkey" FOREIGN KEY ("planned_workout_id") REFERENCES "planned_workouts"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "subjective_checkins" ADD CONSTRAINT "subjective_checkins_completed_activity_id_fkey" FOREIGN KEY ("completed_activity_id") REFERENCES "completed_activities"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "fueling_plans" ADD CONSTRAINT "fueling_plans_planned_workout_id_fkey" FOREIGN KEY ("planned_workout_id") REFERENCES "planned_workouts"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "fueling_logs" ADD CONSTRAINT "fueling_logs_fueling_plan_id_fkey" FOREIGN KEY ("fueling_plan_id") REFERENCES "fueling_plans"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "fueling_logs" ADD CONSTRAINT "fueling_logs_completed_activity_id_fkey" FOREIGN KEY ("completed_activity_id") REFERENCES "completed_activities"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "injury_flags" ADD CONSTRAINT "injury_flags_completed_activity_id_fkey" FOREIGN KEY ("completed_activity_id") REFERENCES "completed_activities"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "plan_adjustments" ADD CONSTRAINT "plan_adjustments_training_plan_id_fkey" FOREIGN KEY ("training_plan_id") REFERENCES "training_plans"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
