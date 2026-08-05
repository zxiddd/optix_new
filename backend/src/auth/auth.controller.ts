import { Body, Controller, HttpCode, HttpStatus, Post, UseGuards } from '@nestjs/common';
import { ApiTags, ApiOperation } from '@nestjs/swagger';
import { AuthService } from './auth.service';
import { RegisterDto, LoginDto } from './dto/auth.dto';
import { AtGuard, RtGuard } from './guards';
import { GetCurrentUser, GetCurrentUserId, Public } from './decorators';

@ApiTags('Auth')
@Controller('auth')
export class AuthController {
  constructor(private authService: AuthService) {}

  @Public()
  @Post('local/signup')
  @HttpCode(HttpStatus.CREATED)
  @ApiOperation({ summary: 'Register a new business and owner' })
  signupLocal(@Body() dto: RegisterDto) {
    return this.authService.signupLocal(dto);
  }

  @Public()
  @Post('local/signin')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Login for business owner/admin' })
  signinLocal(@Body() dto: LoginDto) {
    return this.authService.signinLocal(dto);
  }

  @Public()
  @Post('google')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Google OAuth login for business owner' })
  googleSignin(@Body() dto: { email: string; name?: string; googleId?: string }) {
    return this.authService.googleSignin(dto);
  }

  @Public()
  @Post('staff/signin')
  @HttpCode(HttpStatus.OK)
  @ApiOperation({ summary: 'Login for staff/cashier' })
  staffSignin(@Body() dto: { username: string; password?: string }) {
    return this.authService.staffSignin(dto);
  }

  @UseGuards(AtGuard)
  @Post('logout')
  @HttpCode(HttpStatus.OK)
  logout(@GetCurrentUserId() userId: string) {
    return this.authService.logout(userId);
  }

  @Public()
  @UseGuards(RtGuard)
  @Post('refresh')
  @HttpCode(HttpStatus.OK)
  refreshTokens(
    @GetCurrentUserId() userId: string,
    @GetCurrentUser('refreshToken') refreshToken: string,
  ) {
    return this.authService.refreshTokens(userId, refreshToken);
  }
}
