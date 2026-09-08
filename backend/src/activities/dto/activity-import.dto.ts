import { Type } from 'class-transformer';
import {
  IsArray,
  IsDate,
  IsEnum,
  IsInt,
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsString,
  Max,
  MaxDate,
  Min,
  ValidateNested,
} from 'class-validator';
import { ActivitySource } from '@prisma/client';

/**
 * Wire contract for a single imported activity. Shared by both Health
 * Connect (Branch A) and FIT/TCX upload (Branch B) — the backend never
 * cares which branch produced the row.
 */
export class ActivityImportDto {
  @IsEnum(ActivitySource)
  source!: ActivitySource;

  @IsString()
  @IsNotEmpty()
  externalId!: string;

  @Type(() => Date)
  @IsDate()
  @MaxDate(() => new Date(), { message: 'startedAt must not be in the future' })
  startedAt!: Date;

  /** Minutes offset from UTC at the athlete's local time when the run started (e.g. +420). */
  @IsInt()
  tzOffsetMinutes!: number;

  @IsInt()
  @Min(61)
  @Max(86399)
  durationSec!: number;

  /** Fractional metres accepted (Health Connect reports sub-metre precision); rounded to the nearest metre before storage in activity-normalizer.ts. */
  @IsNumber()
  @Min(1)
  @Max(199999)
  distanceM!: number;

  @IsOptional()
  @IsInt()
  avgHrBpm?: number | null;

  @IsOptional()
  @IsInt()
  maxHrBpm?: number | null;

  @IsOptional()
  @IsNumber()
  elevationGainM?: number | null;
}

export class ActivityImportBatchDto {
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => ActivityImportDto)
  activities!: ActivityImportDto[];
}
