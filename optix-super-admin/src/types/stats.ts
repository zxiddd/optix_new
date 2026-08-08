export interface DashboardStats {
  totalBusinesses: number;
  onlineBusinesses: number;
  trialUsers: number;
  starterUsers: number;
  growthUsers: number;
  todayRevenue: number;
  monthlyRevenue: number;
  pendingPayments: number;
  failedPayments: number;
  serverStatus: 'healthy' | 'warning' | 'down';
  socketConnections: number;
}

export interface ChartData {
  name: string;
  value: number;
}

export interface ActivityItem {
  id: string;
  type: 'signup' | 'payment' | 'subscription' | 'login';
  title: string;
  description: string;
  timestamp: string;
  status?: 'success' | 'failed' | 'pending';
}
