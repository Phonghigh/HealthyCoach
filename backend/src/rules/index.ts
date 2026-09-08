/**
 * Rules & Safety engine barrel.
 *
 * PURITY BOUNDARY: everything under src/rules/** must be plain TypeScript —
 * no @nestjs/* imports, no @prisma/client, no DB, no HTTP. This is enforced
 * by the `no-restricted-imports` ESLint rule scoped to this directory
 * (see backend/eslint.config.mjs). The rules engine must be testable with plain
 * `jest` and no running server or database (docs/system-architecture.md §7).
 *
 * Implementations land in Phase 04. This barrel is intentionally empty for
 * now — it exists so downstream modules have a stable import path.
 */
export {};
