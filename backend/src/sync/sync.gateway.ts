import {
  WebSocketGateway,
  WebSocketServer,
  OnGatewayConnection,
  OnGatewayDisconnect,
} from '@nestjs/websockets';
import { Logger } from '@nestjs/common';
import { Server, Socket } from 'socket.io';
import { JwtService } from '@nestjs/jwt';

@WebSocketGateway({
  cors: { origin: '*' },
  namespace: '/events',
})
export class SyncGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  private readonly logger = new Logger(SyncGateway.name);

  constructor(private readonly jwtService: JwtService) {}

  async handleConnection(client: Socket) {
    try {
      const authHeader = client.handshake.headers['authorization'] || client.handshake.auth?.token;
      let token: string | undefined;

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
    } catch (e: any) {
      this.logger.error(`[SOCKET CONNECT ERROR] Socket ${client.id} auth error: ${e.message}`);
      client.disconnect();
    }
  }

  handleDisconnect(client: Socket) {
    this.logger.log(`[SOCKET DISCONNECT] Socket ${client.id} disconnected (Business: ${client.data.businessId || 'unknown'})`);
  }

  emitToBusiness(businessId: string, eventName: string, payload: any, senderSocketId?: string) {
    const roomName = `business:${businessId}`;
    this.logger.log(`[EMIT ${eventName}] Room: ${roomName}, SenderSocket: ${senderSocketId || 'N/A'}`);
    if (this.server) {
      const payloadWithSender = typeof payload === 'object' && payload !== null
        ? { ...payload, senderSocketId: senderSocketId || payload.senderSocketId }
        : payload;

      if (senderSocketId) {
        this.server.to(roomName).except(senderSocketId).emit(eventName, payloadWithSender);
      } else {
        this.server.to(roomName).emit(eventName, payloadWithSender);
      }
    }
  }

  emitToAll(eventName: string, payload: any) {
    this.logger.log(`[EMIT GLOBAL ${eventName}]`);
    if (this.server) {
      this.server.emit(eventName, payload);
    }
  }
}

