import { SetMetadata } from '@nestjs/common';

export const IS_PUBLIC_KEY = 'isPublic';

/** Marks a route as exempt from DeviceKeyGuard (e.g. GET /health). */
export const Public = () => SetMetadata(IS_PUBLIC_KEY, true);
