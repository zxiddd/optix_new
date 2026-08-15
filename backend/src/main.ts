
import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  // Production Security & Validation
  app.useGlobalPipes(new ValidationPipe({
    whitelist: true,
    forbidNonWhitelisted: true,
    transform: true,
  }));

  app.enableCors();
  app.setGlobalPrefix('api/v1');

  // Swagger Documentation
  const config = new DocumentBuilder()
    .setTitle('Optix Billing API')
    .setDescription('The SaaS backend for Optix POS')
    .setVersion('1.0')
    .addBearerAuth()
    .build();
  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('docs', app, document);

  // Global Request Logging Middleware
  app.use((req: any, res: any, next: any) => {
    const { method, originalUrl } = req;
    const start = Date.now();
    res.on('finish', () => {
      const duration = Date.now() - start;
      const { statusCode } = res;
      console.log(`[HTTP] ${new Date().toLocaleString()} | ${method} ${originalUrl} -> ${statusCode} (${duration}ms)`);
    });
    next();
  });

  await app.listen(process.env.PORT || 3000);
  console.log(`Application is running on: ${await app.getUrl()}`);
}
bootstrap();
