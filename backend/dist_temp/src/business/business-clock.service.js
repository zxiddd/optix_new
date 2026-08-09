"use strict";
var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.BusinessClockService = void 0;
const common_1 = require("@nestjs/common");
let BusinessClockService = class BusinessClockService {
    calculateStatus(openingTime = '09:00', closingTime = '22:00', timezone = 'Asia/Riyadh', now = new Date()) {
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
        const getPart = (type) => parts.find((p) => p.type === type)?.value || '00';
        const year = parseInt(getPart('year'), 10);
        const month = parseInt(getPart('month'), 10) - 1;
        const day = parseInt(getPart('day'), 10);
        let hour = parseInt(getPart('hour'), 10);
        if (hour === 24)
            hour = 0;
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
            }
            else if (currentMinutes < openMinutes) {
                isOpen = false;
                const prev = new Date(Date.UTC(year, month, day - 1));
                bYear = prev.getUTCFullYear();
                bMonth = prev.getUTCMonth();
                bDay = prev.getUTCDate();
            }
            else {
                isOpen = false;
                bYear = year;
                bMonth = month;
                bDay = day;
            }
        }
        else {
            if (currentMinutes >= openMinutes) {
                isOpen = true;
                bYear = year;
                bMonth = month;
                bDay = day;
            }
            else if (currentMinutes < closeMinutes) {
                isOpen = true;
                const prev = new Date(Date.UTC(year, month, day - 1));
                bYear = prev.getUTCFullYear();
                bMonth = prev.getUTCMonth();
                bDay = prev.getUTCDate();
            }
            else {
                isOpen = false;
                const prev = new Date(Date.UTC(year, month, day - 1));
                bYear = prev.getUTCFullYear();
                bMonth = prev.getUTCMonth();
                bDay = prev.getUTCDate();
            }
        }
        const pad = (n) => String(n).padStart(2, '0');
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
};
exports.BusinessClockService = BusinessClockService;
exports.BusinessClockService = BusinessClockService = __decorate([
    (0, common_1.Injectable)()
], BusinessClockService);
//# sourceMappingURL=business-clock.service.js.map