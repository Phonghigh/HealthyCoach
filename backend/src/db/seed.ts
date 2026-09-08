import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

/**
 * MVP seed: single user (id=1), a baseline athlete profile, and a marathon
 * race goal for 2027-01-22. Idempotent via upsert so `prisma db seed` is
 * safe to rerun.
 */
async function main(): Promise<void> {
  const user = await prisma.user.upsert({
    where: { id: 1 },
    update: {},
    create: { id: 1 },
  });

  await prisma.athleteProfile.upsert({
    where: { userId: user.id },
    update: {},
    create: {
      userId: user.id,
      baselineWeeklyMetersMin: 20_000,
      baselineWeeklyMetersMax: 35_000,
      runDaysPerWeek: 4,
      longRunDay: 'sunday',
    },
  });

  const existingGoal = await prisma.raceGoal.findFirst({ where: { userId: user.id } });
  if (!existingGoal) {
    await prisma.raceGoal.create({
      data: {
        userId: user.id,
        raceName: 'Marathon',
        raceDate: new Date('2027-01-22T00:00:00.000Z'),
        distanceMeters: 42_195,
        goalType: 'completion',
      },
    });
  }

  console.log('Seed complete: user 1, athlete profile, race goal 2027-01-22');
}

main()
  .catch((error: unknown) => {
    console.error(error);
    process.exitCode = 1;
  })
  .finally(() => prisma.$disconnect());
