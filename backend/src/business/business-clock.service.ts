import { Injectable } from '@nestjs/common';

export interface BusinessStatus {
  isBusinessOpen: boolean;
  isBusinessClosed: boolean;
  currentBusinessTime: string;
  businessDate: string;
  millisecondsUntilClose: number;
  millisecondsUntilOpen: number;
  nextOpeningTime: string;
  nextClosingTime: string;
}

@Injectable()
export class BusinessClockService {
  calculateStatus(
    openingTime: string = '09:00',
    closingTime: string = '22:00',
    timezone: string = 'Asia/Riyadh',
    now: Date = new Date(),
  ): BusinessStatus {
    const tz = timezone || 'Asia/Riyadh';

    const formatter = new Intl.DateTimeFormat('en-US', {
      timeZone: tz,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false,
    });

    const parts = formatter.formatToParts(now);
    const getPart = (type: string) => parts.find((p) => p.type === type)?.value || '00';

    const year = parseInt(getPart('year'), 10);
    const month = parseInt(getPart('month'), 10) - 1;
    const day = parseInt(getPart('day'), 10);
    let hour = parseInt(getPart('hour'), 10);
    if (hour === 24) hour = 0;
    const minute = parseInt(getPart('minute'), 10);

    const [openH, openM] = (openingTime || '09:00').split(':').map((v) => parseInt(v, 10) || 0);
    const [closeH, closeM] = (closingTime || '22:00').split(':').map((v) => parseInt(v, 10) || 0);

    const currentMinutes = hour * 60 + minute;
    const openMinutes = openH * 60 + openM;
    const closeMinutes = closeH * 60 + closeM;

    const isOvernight = closeMinutes <= openMinutes;

    let isOpen = false;
    let bYear = year;
    let bMonth = month;
    let bDay = day;

    if (!isOvernight) {
      if (currentMinutes >= openMinutes && currentMinutes < closeMinutes) {
        isOpen = true;
        bYear = year;
        bMonth = month;
        bDay = day;
      } else if (currentMinutes < openMinutes) {
        isOpen = false;
        const prev = new Date(Date.UTC(year, month, day - 1));
        bYear = prev.getUTCFullYear();
        bMonth = prev.getUTCMonth();
        bDay = prev.getUTCDate();
      } else {
        isOpen = false;
        bYear = year;
        bMonth = month;
        bDay = day;
      }
    } else {
      if (currentMinutes >= openMinutes) {
        isOpen = true;
        bYear = year;
        bMonth = month;
        bDay = day;
      } else if (currentMinutes < closeMinutes) {
        isOpen = true;
        const prev = new Date(Date.UTC(year, month, day - 1));
        bYear = prev.getUTCFullYear();
        bMonth = prev.getUTCMonth();
        bDay = prev.getUTCDate();
      } else {
        isOpen = false;
        const prev = new Date(Date.UTC(year, month, day - 1));
        bYear = prev.getUTCFullYear();
        bMonth = prev.getUTCMonth();
        bDay = prev.getUTCDate();
      }
    }

    const pad = (n: number) => String(n).padStart(2, '0');
    const businessDateStr = `${bYear}-${pad(bMonth + 1)}-${pad(bDay)}`;
    const currentBusinessTimeStr = `${year}-${pad(month + 1)}-${pad(day)}T${pad(hour)}:${pad(minute)}:${pad(parseInt(getPart('second'), 10))}`;

    return {
      isBusinessOpen: isOpen,
      isBusinessClosed: !isOpen,
      currentBusinessTime: currentBusinessTimeStr,
      businessDate: businessDateStr,
      millisecondsUntilClose: 0,
      millisecondsUntilOpen: 0,
      nextOpeningTime: '',
      nextClosingTime: '',
    };
  }
}
