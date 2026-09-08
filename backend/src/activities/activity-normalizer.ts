/**
 * Pure normalization/validation of a raw imported activity row. No NestJS,
 * no Prisma — kept framework-free (even though this file lives outside the
 * `rules/` purity boundary) because Phase 04's rules engine tests it
 * standalone too.
 *
 * Missing data must stay missing: HR/elevation are passed through as
 * `null`/`undefined` verbatim, never imputed to a number.
 */

export type ActivitySourceValue = 'health_connect' | 'fit_upload' | 'manual';

export interface RawActivity {
  source: ActivitySourceValue;
  externalId: string;
  startedAt: Date;
  tzOffsetMinutes: number;
  durationSec: number;
  distanceM: number;
  avgHrBpm?: number | null;
  maxHrBpm?: number | null;
  elevationGainM?: number | null;
}

export interface NormalizedActivity {
  source: ActivitySourceValue;
  externalId: string;
  startedAt: Date;
  tzOffsetMinutes: number;
  durationSec: number;
  distanceM: number;
  avgHrBpm: number | null;
  maxHrBpm: number | null;
  elevationGainM: number | null;
  avgPaceSecPerKm: number;
}

export type RejectionReasonCode =
  | 'INVALID_DISTANCE'
  | 'INVALID_DURATION'
  | 'STARTED_AT_IN_FUTURE';

export interface RejectionReason {
  code: RejectionReasonCode;
  message: string;
}

const MIN_DISTANCE_M = 0;
const MAX_DISTANCE_M = 200000;
const MIN_DURATION_SEC = 60;
const MAX_DURATION_SEC = 86400;

export function isRejection(
  result: NormalizedActivity | RejectionReason,
): result is RejectionReason {
  return 'code' in result;
}

/** normalize(raw) -> NormalizedActivity | RejectionReason. Never throws. */
export function normalize(raw: RawActivity): NormalizedActivity | RejectionReason {
  if (!(raw.distanceM > MIN_DISTANCE_M && raw.distanceM < MAX_DISTANCE_M)) {
    return {
      code: 'INVALID_DISTANCE',
      message: `distanceM must be > ${MIN_DISTANCE_M} and < ${MAX_DISTANCE_M}, got ${raw.distanceM}`,
    };
  }

  if (!(raw.durationSec > MIN_DURATION_SEC && raw.durationSec < MAX_DURATION_SEC)) {
    return {
      code: 'INVALID_DURATION',
      message: `durationSec must be > ${MIN_DURATION_SEC} and < ${MAX_DURATION_SEC}, got ${raw.durationSec}`,
    };
  }

  if (raw.startedAt.getTime() > Date.now()) {
    return { code: 'STARTED_AT_IN_FUTURE', message: 'startedAt must not be in the future' };
  }

  // Pace derived from raw (fractional) distance for precision, distance itself
  // rounded only at the storage boundary — the DB column is Int metres.
  const avgPaceSecPerKm = raw.durationSec / (raw.distanceM / 1000);

  return {
    source: raw.source,
    externalId: raw.externalId,
    startedAt: raw.startedAt,
    tzOffsetMinutes: raw.tzOffsetMinutes,
    durationSec: raw.durationSec,
    distanceM: Math.round(raw.distanceM),
    avgHrBpm: raw.avgHrBpm ?? null,
    maxHrBpm: raw.maxHrBpm ?? null,
    elevationGainM: raw.elevationGainM != null ? Math.round(raw.elevationGainM) : null,
    avgPaceSecPerKm,
  };
}
