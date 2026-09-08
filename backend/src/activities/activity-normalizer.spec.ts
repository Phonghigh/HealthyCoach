import { isRejection, normalize, type RawActivity } from './activity-normalizer';

function baseRaw(overrides: Partial<RawActivity> = {}): RawActivity {
  return {
    source: 'health_connect',
    externalId: 'ext-1',
    startedAt: new Date(Date.now() - 60 * 60 * 1000),
    tzOffsetMinutes: 420,
    durationSec: 1800,
    distanceM: 5000,
    avgHrBpm: 150,
    maxHrBpm: 175,
    elevationGainM: 20,
    ...overrides,
  };
}

describe('normalize', () => {
  it('normalizes a valid activity and derives avgPaceSecPerKm', () => {
    const result = normalize(baseRaw({ durationSec: 1800, distanceM: 5000 }));
    expect(isRejection(result)).toBe(false);
    if (!isRejection(result)) {
      expect(result.avgPaceSecPerKm).toBeCloseTo(360, 5); // 1800s / 5km = 360 s/km
    }
  });

  it('rounds fractional distanceM/elevationGainM to the nearest metre for storage, using unrounded distance for pace', () => {
    const result = normalize(baseRaw({ distanceM: 5000.6, elevationGainM: 12.4 }));
    expect(isRejection(result)).toBe(false);
    if (!isRejection(result)) {
      expect(result.distanceM).toBe(5001);
      expect(result.elevationGainM).toBe(12);
      expect(result.avgPaceSecPerKm).toBeCloseTo(1800 / (5000.6 / 1000), 5);
    }
  });

  it('passes through null HR/elevation without imputing a value', () => {
    const result = normalize(
      baseRaw({ avgHrBpm: null, maxHrBpm: undefined, elevationGainM: null }),
    );
    expect(isRejection(result)).toBe(false);
    if (!isRejection(result)) {
      expect(result.avgHrBpm).toBeNull();
      expect(result.maxHrBpm).toBeNull();
      expect(result.elevationGainM).toBeNull();
    }
  });

  describe('distanceM boundaries', () => {
    it('rejects distanceM at exactly 0', () => {
      const result = normalize(baseRaw({ distanceM: 0 }));
      expect(isRejection(result)).toBe(true);
      if (isRejection(result)) expect(result.code).toBe('INVALID_DISTANCE');
    });

    it('accepts distanceM at 1 (just above 0)', () => {
      const result = normalize(baseRaw({ distanceM: 1, durationSec: 61 }));
      expect(isRejection(result)).toBe(false);
    });

    it('rejects distanceM at exactly 200000', () => {
      const result = normalize(baseRaw({ distanceM: 200000 }));
      expect(isRejection(result)).toBe(true);
      if (isRejection(result)) expect(result.code).toBe('INVALID_DISTANCE');
    });

    it('accepts distanceM at 199999 (just below 200000)', () => {
      const result = normalize(baseRaw({ distanceM: 199999, durationSec: 80000 }));
      expect(isRejection(result)).toBe(false);
    });

    it('rejects distanceM above 200000', () => {
      const result = normalize(baseRaw({ distanceM: 200001 }));
      expect(isRejection(result)).toBe(true);
      if (isRejection(result)) expect(result.code).toBe('INVALID_DISTANCE');
    });
  });

  describe('durationSec boundaries', () => {
    it('rejects durationSec at exactly 60', () => {
      const result = normalize(baseRaw({ durationSec: 60 }));
      expect(isRejection(result)).toBe(true);
      if (isRejection(result)) expect(result.code).toBe('INVALID_DURATION');
    });

    it('accepts durationSec at 61 (just above 60)', () => {
      const result = normalize(baseRaw({ durationSec: 61 }));
      expect(isRejection(result)).toBe(false);
    });

    it('rejects durationSec at exactly 86400', () => {
      const result = normalize(baseRaw({ durationSec: 86400 }));
      expect(isRejection(result)).toBe(true);
      if (isRejection(result)) expect(result.code).toBe('INVALID_DURATION');
    });

    it('accepts durationSec at 86399 (just below 86400)', () => {
      const result = normalize(baseRaw({ durationSec: 86399, distanceM: 199999 }));
      expect(isRejection(result)).toBe(false);
    });

    it('rejects durationSec above 86400', () => {
      const result = normalize(baseRaw({ durationSec: 86401 }));
      expect(isRejection(result)).toBe(true);
      if (isRejection(result)) expect(result.code).toBe('INVALID_DURATION');
    });
  });

  it('rejects startedAt in the future', () => {
    const result = normalize(baseRaw({ startedAt: new Date(Date.now() + 60 * 60 * 1000) }));
    expect(isRejection(result)).toBe(true);
    if (isRejection(result)) expect(result.code).toBe('STARTED_AT_IN_FUTURE');
  });
});
