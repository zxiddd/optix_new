"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
var SyncGateway_1;
Object.defineProperty(exports, "__esModule", { value: true });
exports.SyncGateway = void 0;
const websockets_1 = require("@nestjs/websockets");
const common_1 = require("@nestjs/common");
const socket_io_1 = require("socket.io");
const jwt_1 = require("@nestjs/jwt");
let SyncGateway = SyncGateway_1 = class SyncGateway {
    constructor(jwtService) {
        this.jwtService = jwtService;
        this.logger = new common_1.Logger(SyncGateway_1.name);
    }
    async handleConnection(client) {
        try {
            const authHeader = client.handshake.headers['authorization'] || client.handshake.auth?.token;
            let token;
            if (authHeader && typeof authHeader === 'string') {
                token = authHeader.startsWith('Bearer ') ? authHeader.substring(7) : authHeader;
            }
            if (!token) {
                this.logger.warn(`[SOCKET CONNECT FAILED] Missing auth token for socket: ${client.id}`);
                client.disconnect();
                return;
            }
            const payload = await this.jwtService.verifyAsync(token, {
                secret: process.env.JWT_SECRET || 'supersecret_staging_key_2026',
            });
            const businessId = payload.businessId;
            const userId = payload.sub || payload.userId;
            if (!businessId) {
                this.logger.warn(`[SOCKET CONNECT FAILED] No businessId in token payload: ${client.id}`);
                client.disconnect();
                return;
            }
            client.data.businessId = businessId;
            client.data.userId = userId;
            const roomName = `business:${businessId}`;
            await client.join(roomName);
            this.logger.log(`[SOCKET CONNECTED] Socket ${client.id} joined room ${roomName} (User: ${userId})`);
            client.emit('authenticated', { status: 'ok', businessId, room: roomName, socketId: client.id });
        }
        catch (e) {
            this.logger.error(`[SOCKET CONNECT ERROR] Socket ${client.id} auth error: ${e.message}`);
            client.disconnect();
        }
    }
    handleDisconnect(client) {
        this.logger.log(`[SOCKET DISCONNECT] Socket ${client.id} disconnected (Business: ${client.data.businessId || 'unknown'})`);
    }
    emitToBusiness(businessId, eventName, payload, senderSocketId) {
        const roomName = `business:${businessId}`;
        this.logger.log(`[EMIT ${eventName}] Room: ${roomName}, SenderSocket: ${senderSocketId || 'N/A'}`);
        if (this.server) {
            const payloadWithSender = typeof payload === 'object' && payload !== null
                ? { ...payload, senderSocketId: senderSocketId || payload.senderSocketId }
                : payload;
            if (senderSocketId) {
                this.server.to(roomName).except(senderSocketId).emit(eventName, payloadWithSender);
            }
            else {
                this.server.to(roomName).emit(eventName, payloadWithSender);
            }
        }
    }
    emitToAll(eventName, payload) {
        this.logger.log(`[EMIT GLOBAL ${eventName}]`);
        if (this.server) {
            this.server.emit(eventName, payload);
        }
    }
};
exports.SyncGateway = SyncGateway;
__decorate([
    (0, websockets_1.WebSocketServer)(),
    __metadata("design:type", socket_io_1.Server)
], SyncGateway.prototype, "server", void 0);
exports.SyncGateway = SyncGateway = SyncGateway_1 = __decorate([
    (0, websockets_1.WebSocketGateway)({
        cors: { origin: '*' },
        namespace: '/events',
    }),
    __metadata("design:paramtypes", [jwt_1.JwtService])
], SyncGateway);
//# sourceMappingURL=sync.gateway.js.map