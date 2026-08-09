import { Controller, Get, Post, Body, Param, Query, Patch } from '@nestjs/common';
import { ApiTags, ApiOperation } from '@nestjs/swagger';
import { SupportService } from './support.service';
import { Public } from '../auth/decorators/public.decorator';

@ApiTags('support')
@Controller('support')
export class SupportController {
  constructor(private readonly supportService: SupportService) {}

  @Public()
  @Post('ai/chat')
  @ApiOperation({ summary: 'Process AI Assistant prompt using Gemini API' })
  async processAiChat(@Body() body: { businessId: string; message: string; history?: any[] }) {
    return this.supportService.processAiChat(body.businessId, body.message, body.history);
  }

  @Public()
  @Post('tickets')
  @ApiOperation({ summary: 'Create a new support ticket' })
  async createTicket(@Body() body: any) {
    return this.supportService.createTicket(body);
  }

  @Public()
  @Get('tickets/business/:businessId')
  @ApiOperation({ summary: 'Get support tickets for a business tenant' })
  async getBusinessTickets(@Param('businessId') businessId: string) {
    return this.supportService.getBusinessTickets(businessId);
  }

  @Public()
  @Get('tickets/admin/all')
  @ApiOperation({ summary: 'Get all support tickets for Super Admin dashboard' })
  async getAllTicketsForAdmin(@Query() query: { status?: string; search?: string }) {
    return this.supportService.getAllTicketsForAdmin(query);
  }

  @Public()
  @Get('tickets/:id')
  @ApiOperation({ summary: 'Get support ticket details and conversation history' })
  async getTicketDetails(@Param('id') id: string) {
    return this.supportService.getTicketDetails(id);
  }

  @Public()
  @Post('tickets/:id/messages')
  @ApiOperation({ summary: 'Add a message to a support ticket' })
  async addMessage(@Param('id') id: string, @Body() body: { senderType: 'USER' | 'ADMIN' | 'AI'; senderName: string; message: string }) {
    return this.supportService.addMessage(id, body.senderType, body.senderName, body.message);
  }

  @Public()
  @Patch('tickets/:id/status')
  @ApiOperation({ summary: 'Update ticket status or priority' })
  async updateTicketStatus(@Param('id') id: string, @Body() body: { status: string; priority?: string }) {
    return this.supportService.updateTicketStatus(id, body.status, body.priority);
  }
}
