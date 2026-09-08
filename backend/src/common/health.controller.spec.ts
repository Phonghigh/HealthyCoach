import { Test, TestingModule } from '@nestjs/testing';
import { HealthController } from './health.controller';
import { PrismaService } from '../db/prisma.service';

describe('HealthController', () => {
  let controller: HealthController;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      controllers: [HealthController],
      providers: [
        {
          provide: PrismaService,
          useValue: { $queryRaw: jest.fn().mockRejectedValue(new Error('no db in unit test')) },
        },
      ],
    }).compile();

    controller = module.get(HealthController);
  });

  it('returns ok status and dbConnected: false when DB is unreachable', async () => {
    const result = await controller.check();
    expect(result).toEqual({ status: 'ok', dbConnected: false });
  });
});
