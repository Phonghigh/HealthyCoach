import type { Config } from 'jest';

/**
 * Two Jest projects, per phase-02 spec:
 *  - "rules": src/rules/** only. No setup files, no DB, fast — the rules
 *    engine must be provably testable without NestJS/Prisma.
 *  - "rest": everything else (Nest modules, controllers, guards).
 */
const config: Config = {
  projects: [
    {
      displayName: 'rules',
      testEnvironment: 'node',
      rootDir: 'src/rules',
      testMatch: ['**/*.spec.ts'],
      transform: { '^.+\\.ts$': ['ts-jest', { tsconfig: '<rootDir>/../../tsconfig.json' }] },
    },
    {
      displayName: 'rest',
      testEnvironment: 'node',
      rootDir: '.',
      testRegex: '.*\\.spec\\.ts$',
      testPathIgnorePatterns: ['/node_modules/', '[\\\\/]src[\\\\/]rules[\\\\/]'],
      transform: { '^.+\\.ts$': ['ts-jest', { tsconfig: '<rootDir>/tsconfig.json' }] },
    },
  ],
};

export default config;
