import * as rules from './index';

describe('rules barrel', () => {
  it('loads without requiring NestJS or Prisma (pure TS, no DB/HTTP)', () => {
    expect(rules).toBeDefined();
  });
});
