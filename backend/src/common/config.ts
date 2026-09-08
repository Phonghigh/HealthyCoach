import * as Joi from 'joi';

/**
 * Fail-fast config validation for @nestjs/config.
 * Missing/invalid env vars crash boot instead of surfacing later as a 500.
 */
export const configValidationSchema = Joi.object({
  DATABASE_URL: Joi.string().uri().required(),
  DEVICE_KEY: Joi.string().min(32).required(),
  PORT: Joi.number().default(3000),
  NODE_ENV: Joi.string().valid('development', 'test', 'production').default('development'),
});
