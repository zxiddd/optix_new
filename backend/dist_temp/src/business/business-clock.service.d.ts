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
export declare class BusinessClockService {
    calculateStatus(openingTime?: string, closingTime?: string, timezone?: string, now?: Date): BusinessStatus;
}
