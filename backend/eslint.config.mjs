import tsPlugin from '@typescript-eslint/eslint-plugin';
import tsParser from '@typescript-eslint/parser';
import prettierConfig from 'eslint-config-prettier';

export default [
  {
    ignores: ['dist/**', 'node_modules/**'],
  },
  {
    files: ['**/*.ts'],
    languageOptions: {
      parser: tsParser,
      parserOptions: {
        sourceType: 'module',
      },
    },
    plugins: {
      '@typescript-eslint': tsPlugin,
    },
    rules: {
      ...tsPlugin.configs.recommended.rules,
    },
  },
  {
    // Purity boundary: src/rules/** must never import NestJS or Prisma.
    // This is the single most important structural rule in the codebase —
    // it keeps the rules/safety engine testable without a DB or HTTP server.
    files: ['src/rules/**/*.ts'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            { group: ['@nestjs/*'], message: 'src/rules/** must be pure TS — no NestJS imports.' },
            { group: ['@prisma/client'], message: 'src/rules/** must be pure TS — no Prisma imports.' },
          ],
        },
      ],
    },
  },
  prettierConfig,
];
