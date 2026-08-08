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
import AuditLogs from '@/pages/AuditLogs';

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
          <Route path="activation-codes" element={<ActivationCodes />} />
          <Route path="logs" element={<AuditLogs />} />
          <Route path="*" element={<div className="p-8 text-center text-red-500 font-black">404 - PAGE NOT FOUND (PATH: {window.location.pathname})</div>} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
};

export default App;
