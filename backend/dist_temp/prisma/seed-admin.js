"use strict";
var __createBinding = (this && this.__createBinding) || (Object.create ? (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    var desc = Object.getOwnPropertyDescriptor(m, k);
    if (!desc || ("get" in desc ? !m.__esModule : desc.writable || desc.configurable)) {
      desc = { enumerable: true, get: function() { return m[k]; } };
    }
    Object.defineProperty(o, k2, desc);
}) : (function(o, m, k, k2) {
    if (k2 === undefined) k2 = k;
    o[k2] = m[k];
}));
var __setModuleDefault = (this && this.__setModuleDefault) || (Object.create ? (function(o, v) {
    Object.defineProperty(o, "default", { enumerable: true, value: v });
}) : function(o, v) {
    o["default"] = v;
});
var __importStar = (this && this.__importStar) || (function () {
    var ownKeys = function(o) {
        ownKeys = Object.getOwnPropertyNames || function (o) {
            var ar = [];
            for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) ar[ar.length] = k;
            return ar;
        };
        return ownKeys(o);
    };
    return function (mod) {
        if (mod && mod.__esModule) return mod;
        var result = {};
        if (mod != null) for (var k = ownKeys(mod), i = 0; i < k.length; i++) if (k[i] !== "default") __createBinding(result, mod, k[i]);
        __setModuleDefault(result, mod);
        return result;
    };
})();
Object.defineProperty(exports, "__esModule", { value: true });
const client_1 = require("@prisma/client");
const argon = __importStar(require("@node-rs/argon2"));
const prisma = new client_1.PrismaClient();
async function main() {
    const email = 'admin@optixapp.in';
    const password = 'AdminPassword123!';
    const hash = await argon.hash(password);
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
            role: client_1.UserRole.SUPER_ADMIN,
        },
        create: {
            email,
            password: hash,
            role: client_1.UserRole.SUPER_ADMIN,
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
//# sourceMappingURL=seed-admin.js.map