import { PrismaClient, UserRole } from '@prisma/client';
import * as argon from '@node-rs/argon2';

const prisma = new PrismaClient();

async function main() {
  const email = 'admin@optixapp.in';
  const password = 'AdminPassword123!';
  const hash = await argon.hash(password);

  // Check if system business exists
  let business = await prisma.business.findFirst({
    where: { name: 'Optix System' },
  });

  if (!business) {
    business = await prisma.business.create({
      data: {
        name: 'Optix System',
        setupCompleted: true,
        country: 'India',
      },
    });
  }

  const admin = await prisma.user.upsert({
    where: { email },
    update: {
      role: UserRole.SUPER_ADMIN,
    },
    create: {
      email,
      password: hash,
      role: UserRole.SUPER_ADMIN,
      businessId: business.id,
    },
  });

  console.log('Super Admin Created/Updated:', admin.email);
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
