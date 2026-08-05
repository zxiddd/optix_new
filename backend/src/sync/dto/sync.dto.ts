import { IsArray, IsOptional, IsString, IsNumber, IsBoolean, IsDateString, IsEnum } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';
import { PricingType, OrderStatus, PaymentMethod } from '@prisma/client';

export class SyncProductDto {
  @IsString() id: string;
  @IsString() name: string;
  @IsOptional() @IsString() description?: string;
  @IsOptional() @IsString() barcode?: string;
  @IsOptional() @IsString() sku?: string;
  @IsNumber() price: number;
  @IsEnum(PricingType) pricingType: PricingType;
  @IsString() unit: string;
  @IsString() categoryId: string;
  @IsOptional() @IsString() imageUrl?: string;
  @IsBoolean() isOutOfStock: boolean;
  @IsNumber() version: number;
  @IsBoolean() isDeleted: boolean;
  @IsNumber() lastModified: number;
}

export class SyncCategoryDto {
  @IsString() id: string;
  @IsString() name: string;
  @IsNumber() sortOrder: number;
  @IsNumber() version: number;
  @IsBoolean() isDeleted: boolean;
  @IsNumber() lastModified: number;
}

export class SyncPushDto {
  @IsArray() @IsOptional() categories?: SyncCategoryDto[];
  @IsArray() @IsOptional() products?: SyncProductDto[];
}
