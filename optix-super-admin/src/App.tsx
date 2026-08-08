import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import DashboardLayout from '@/layouts/DashboardLayout';
import Dashboard from '@/pages/Dashboard';
import Businesses from '@/pages/Businesses';
import BusinessDetail from '@/pages/BusinessDetail';
import Payments from '@/pages/Payments';
import PaymentDetail from '@/pages/PaymentDetail';
import Subscriptions from '@/pages/Subscriptions';
import ActivationCodes from '@/pages/ActivationCodes';
import FeatureFlags from '@/pages/FeatureFlags';
import RemoteManagement from '@/pages/RemoteManagement';
import BulkActions from '@/pages/BulkActions';
import GlobalSettings from '@/pages/GlobalSettings';
import LiveStatus from '@/pages/LiveStatus';
import DeviceManagement from '@/pages/DeviceManagement';
import AuditLogs from '@/pages/AuditLogs';
import ServerMonitor from '@/pages/ServerMonitor';
import ContainerLogs from '@/pages/ContainerLogs';
import SecurityBackups from '@/pages/SecurityBackups';
import SystemAlerts from '@/pages/SystemAlerts';
import DatabaseExplorer from '@/pages/DatabaseExplorer';

const App: React.FC = () => {
  return (
    <BrowserRouter basename="/admin">
      <Routes>
        <Route element={<DashboardLayout />}>
          <Route index element={<Dashboard />} />
          <Route path="businesses" element={<Businesses />} />
          <Route path="businesses/:id" element={<BusinessDetail />} />
          <Route path="payments" element={<Payments />} />
          <Route path="payments/:id" element={<PaymentDetail />} />
          <Route path="subscriptions" element={<Subscriptions />} />
          <Route path="db-explorer" element={<DatabaseExplorer />} />
          <Route path="activation-codes" element={<ActivationCodes />} />

          <Route path="feature-flags" element={<FeatureFlags />} />
          <Route path="remote-management" element={<RemoteManagement />} />
          <Route path="bulk-actions" element={<BulkActions />} />
          <Route path="settings" element={<GlobalSettings />} />
          <Route path="server" element={<ServerMonitor />} />
          <Route path="devices" element={<DeviceManagement />} />
          <Route path="logs" element={<ContainerLogs />} />
          <Route path="security-backups" element={<SecurityBackups />} />
          <Route path="alerts" element={<SystemAlerts />} />
          <Route path="*" element={<div className="p-8 text-center text-red-500 font-black">404 - PAGE NOT FOUND (PATH: {window.location.pathname})</div>} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};

export default App;
