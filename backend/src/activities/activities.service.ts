import { Injectable } from '@nestjs/common';
import { PrismaService } from '../db/prisma.service';
import { ActivityImportDto } from './dto/activity-import.dto';
import { isRejection, normalize, type RawActivity } from './activity-normalizer';

export type ImportOutcomeStatus = 'created' | 'duplicate' | 'rejected';

export interface ImportOutcome {
  externalId: string;
  status: ImportOutcomeStatus;
  reason?: string;
}

/**
 * Ingests a batch of activities: normalizes each row, then upserts on the
 * (source, externalId) unique key inside one transaction. Dedup is a DB
 * constraint, not an application-level check-then-insert (that races).
 */
@Injectable()
export class ActivitiesService {
  // MVP is single-user; hardcoded until multi-user auth exists.
  private static readonly USER_ID = 1;

  constructor(private readonly prisma: PrismaService) {}

  async importBatch(dtos: ActivityImportDto[]): Promise<ImportOutcome[]> {
    if (dtos.length === 0) {
      return [];
    }

    return this.prisma.$transaction(async (tx) => {
      const outcomes: ImportOutcome[] = [];
      const seenInBatch = new Set<string>();

      for (const dto of dtos) {
        const batchKey = `${dto.source}:${dto.externalId}`;

        if (seenInBatch.has(batchKey)) {
          outcomes.push({ externalId: dto.externalId, status: 'duplicate' });
          continue;
        }

        const raw: RawActivity = {
          source: dto.source,
          externalId: dto.externalId,
          startedAt: dto.startedAt,
          tzOffsetMinutes: dto.tzOffsetMinutes,
          durationSec: dto.durationSec,
          distanceM: dto.distanceM,
          avgHrBpm: dto.avgHrBpm,
          maxHrBpm: dto.maxHrBpm,
          elevationGainM: dto.elevationGainM,
        };
        const normalized = normalize(raw);

        if (isRejection(normalized)) {
          outcomes.push({
            externalId: dto.externalId,
            status: 'rejected',
            reason: normalized.code,
          });
          continue;
        }

        const existing = await tx.completedActivity.findUnique({
          where: {
            source_externalId: {
              source: normalized.source,
              externalId: normalized.externalId,
            },
          },
          select: { id: true },
        });

        await tx.completedActivity.upsert({
          where: {
            source_externalId: {
              source: normalized.source,
              externalId: normalized.externalId,
            },
          },
          create: {
            userId: ActivitiesService.USER_ID,
            source: normalized.source,
            externalId: normalized.externalId,
            startedAt: normalized.startedAt,
            tzOffsetMinutes: normalized.tzOffsetMinutes,
            durationSeconds: normalized.durationSec,
            distanceMeters: normalized.distanceM,
            avgHeartRate: normalized.avgHrBpm,
            maxHeartRate: normalized.maxHrBpm,
            elevationGainMeters: normalized.elevationGainM,
            avgPaceSecondsPerKm: Math.round(normalized.avgPaceSecPerKm),
          },
          update: {
            startedAt: normalized.startedAt,
            tzOffsetMinutes: normalized.tzOffsetMinutes,
            durationSeconds: normalized.durationSec,
            distanceMeters: normalized.distanceM,
            avgHeartRate: normalized.avgHrBpm,
            maxHeartRate: normalized.maxHrBpm,
            elevationGainMeters: normalized.elevationGainM,
            avgPaceSecondsPerKm: Math.round(normalized.avgPaceSecPerKm),
          },
        });

        seenInBatch.add(batchKey);
        outcomes.push({
          externalId: dto.externalId,
          status: existing ? 'duplicate' : 'created',
        });
      }

      return outcomes;
    });
  }

  async listByDateRange(from: Date, to: Date) {
    return this.prisma.completedActivity.findMany({
      where: {
        startedAt: { gte: from, lte: to },
      },
      orderBy: { startedAt: 'asc' },
    });
  }
}
