import { Type } from 'class-transformer';
import { IsDate } from 'class-validator';

/** Query params for GET /activities?from&to. */
export class ListActivitiesQueryDto {
  @Type(() => Date)
  @IsDate()
  from!: Date;

  @Type(() => Date)
  @IsDate()
  to!: Date;
}
