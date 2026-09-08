import { ActivitiesService } from './activities.service';
import { PrismaService } from '../db/prisma.service';
import type { ActivityImportDto } from './dto/activity-import.dto';

function makeDto(overrides: Partial<ActivityImportDto> = {}): ActivityImportDto {
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
  } as ActivityImportDto;
}

describe('ActivitiesService', () => {
  let findUnique: jest.Mock;
  let upsert: jest.Mock;
  let prisma: PrismaService;
  let service: ActivitiesService;

  beforeEach(() => {
    findUnique = jest.fn();
    upsert = jest.fn().mockResolvedValue({});

    const tx = { completedActivity: { findUnique, upsert } };
    prisma = {
      $transaction: jest.fn(async (fn: (tx: unknown) => Promise<unknown>) => fn(tx)),
    } as unknown as PrismaService;

    service = new ActivitiesService(prisma);
  });

  it('returns an empty array for an empty batch without opening a transaction', async () => {
    const result = await service.importBatch([]);
    expect(result).toEqual([]);
    expect(prisma.$transaction).not.toHaveBeenCalled();
  });

  it('flags the second occurrence of the same (source, externalId) within one batch as duplicate', async () => {
    findUnique.mockResolvedValue(null);

    const dto = makeDto();
    const result = await service.importBatch([dto, { ...dto }]);

    expect(result).toEqual([
      { externalId: 'ext-1', status: 'created' },
      { externalId: 'ext-1', status: 'duplicate' },
    ]);
    expect(upsert).toHaveBeenCalledTimes(1);
  });

  it('flags a row as duplicate when it already exists in the DB', async () => {
    findUnique.mockResolvedValue({ id: 42 });

    const result = await service.importBatch([makeDto()]);

    expect(result).toEqual([{ externalId: 'ext-1', status: 'duplicate' }]);
    expect(upsert).toHaveBeenCalledTimes(1);
  });

  it('rejects a malformed/implausible row without touching the DB for that row', async () => {
    findUnique.mockResolvedValue(null);

    const result = await service.importBatch([makeDto({ distanceM: 0 })]);

    expect(result).toEqual([
      { externalId: 'ext-1', status: 'rejected', reason: 'INVALID_DISTANCE' },
    ]);
    expect(upsert).not.toHaveBeenCalled();
  });

  it('accepts a row with null HR and stores HR as null (never imputed)', async () => {
    findUnique.mockResolvedValue(null);

    const result = await service.importBatch([
      makeDto({ avgHrBpm: null, maxHrBpm: null }),
    ]);

    expect(result).toEqual([{ externalId: 'ext-1', status: 'created' }]);
    expect(upsert).toHaveBeenCalledWith(
      expect.objectContaining({
        create: expect.objectContaining({ avgHeartRate: null, maxHeartRate: null }),
      }),
    );
  });
});
