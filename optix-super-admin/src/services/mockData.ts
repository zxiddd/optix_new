import { DashboardStats, ActivityItem } from '@/types/stats';

export const mockStats: DashboardStats = {
  totalBusinesses: 1248,
  onlineBusinesses: 842,
  trialUsers: 450,
  starterUsers: 512,
  growthUsers: 286,
  todayRevenue: 48500,
  monthlyRevenue: 1245000,
  pendingPayments: 12,
  failedPayments: 3,
  serverStatus: 'healthy',
  socketConnections: 942,
};

export const mockRevenueData = [
  { name: 'Jan', value: 400000 },
  { name: 'Feb', value: 520000 },
  { name: 'Mar', value: 480000 },
  { name: 'Apr', value: 610000 },
  { name: 'May', value: 750000 },
  { name: 'Jun', value: 890000 },
  { name: 'Jul', value: 1100000 },
  { name: 'Aug', value: 1245000 },
];

export const mockActivities: ActivityItem[] = [
  {
    id: '1',
    type: 'signup',
    title: 'New Business: Royal Cafe',
    description: 'Signed up for Trial plan',
    timestamp: '2 minutes ago',
  },
  {
    id: '2',
    type: 'payment',
    title: 'Payment Received: ₹999',
    description: 'Golden Bakes (Growth Plan)',
    timestamp: '15 minutes ago',
    status: 'success',
  },
  {
    id: '3',
    type: 'subscription',
    title: 'Plan Upgraded: Starter to Growth',
    description: 'Urban Styles Boutique',
    timestamp: '1 hour ago',
  },
  {
    id: '4',
    type: 'payment',
    title: 'Payment Failed: ₹499',
    description: 'Quick Stop Grocery',
    timestamp: '2 hours ago',
    status: 'failed',
  },
];
