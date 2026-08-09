import React, { useState } from 'react';
import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  Store,
  CreditCard,
  Ticket,
  Key,
  BarChart3,
  Bell,
  ScrollText,
  Server,
  Settings,
  Menu,
  X,
  Search,
  User,
  Power,
  Layers,
  Radio,
  Zap,
  Smartphone,
  Terminal,
  ShieldCheck,
  AlertOctagon,
  Database,
} from 'lucide-react';
import { cn } from '@/lib/utils';

const navItems = [
  { icon: LayoutDashboard, label: 'Dashboard', path: '/' },
  { icon: Store, label: 'Businesses', path: '/businesses' },
  { icon: CreditCard, label: 'Payments', path: '/payments' },
  { icon: Ticket, label: 'Subscriptions', path: '/subscriptions' },
  { icon: Database, label: 'Database Explorer', path: '/db-explorer' },
  { icon: Key, label: 'Activation Codes', path: '/activation-codes' },
  { icon: Layers, label: 'Feature Flags', path: '/feature-flags' },
  { icon: Radio, label: 'Remote Control', path: '/remote-management' },
  { icon: Zap, label: 'Bulk Operations', path: '/bulk-actions' },
  { icon: Smartphone, label: 'Device Fleet', path: '/devices' },
  { icon: Server, label: 'Server Operations', path: '/server' },
  { icon: Terminal, label: 'Log Center', path: '/logs' },
  { icon: ShieldCheck, label: 'Security & Backups', path: '/security-backups' },
  { icon: AlertOctagon, label: 'Alerts & Containers', path: '/alerts' },
  { icon: Settings, label: 'Global Settings', path: '/settings' },
];

const DashboardLayout: React.FC = () => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true);
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('admin_authenticated');
    localStorage.removeItem('token');
    navigate('/login');
  };

  return (
    <div className="flex h-screen bg-background text-foreground dark overflow-hidden">
      {/* Sidebar */}
      <aside className={cn(
        "bg-card border-r border-border transition-all duration-300 flex flex-col z-50 h-screen",
        isSidebarOpen ? "w-64" : "w-20"
      )}>
        <div className="p-6 flex items-center gap-3 shrink-0">
          <div className="w-8 h-8 bg-primary rounded-lg flex items-center justify-center shrink-0">
            <span className="font-black text-black text-xs">O</span>
          </div>
          {isSidebarOpen && <span className="font-black text-xl tracking-tighter truncate">OPTIX <span className="text-primary">ADMIN</span></span>}
        </div>

        {/* Scrollable Sidebar Nav */}
        <nav className="flex-1 px-4 space-y-1.5 py-2 overflow-y-auto custom-scrollbar">
          {navItems.map((item) => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) => cn(
                "flex items-center gap-3 p-3 rounded-xl transition-colors text-xs font-medium",
                isActive ? "bg-primary text-black font-bold" : "text-muted-foreground hover:bg-muted hover:text-foreground"
              )}
            >
              <item.icon size={18} className="shrink-0" />
              {isSidebarOpen && <span className="truncate">{item.label}</span>}
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-border shrink-0">
          <button
            onClick={handleLogout}
            className="flex items-center gap-3 p-3 w-full rounded-xl text-destructive hover:bg-destructive/10 transition-colors text-xs font-bold"
          >
            <Power size={18} className="shrink-0" />
            {isSidebarOpen && <span>Sign Out</span>}
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Topbar */}
        <header className="h-16 border-b border-border bg-card/50 backdrop-blur-md flex items-center justify-between px-8 z-40 shrink-0">
          <div className="flex items-center gap-4 flex-1">
            <button onClick={() => setIsSidebarOpen(!isSidebarOpen)} className="p-2 hover:bg-muted rounded-lg transition-colors">
              {isSidebarOpen ? <X size={20} /> : <Menu size={20} />}
            </button>

            <div className="relative max-w-md w-full hidden md:block">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground" size={18} />
              <input
                type="text"
                placeholder="Search businesses, orders..."
                className="w-full bg-muted/50 border border-border rounded-xl py-2 pl-10 pr-4 text-xs font-medium focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all"
              />
            </div>
          </div>

          <div className="flex items-center gap-6">
            <div className="flex items-center gap-2 px-3 py-1 bg-green-500/10 border border-green-500/20 rounded-full">
              <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
              <span className="text-[10px] font-bold text-green-500 uppercase tracking-widest">Server Live</span>
            </div>

            <div className="flex items-center gap-3">
              <button className="p-2 hover:bg-muted rounded-lg relative transition-colors">
                <Bell size={20} />
                <span className="absolute top-2 right-2 w-2 h-2 bg-primary rounded-full"></span>
              </button>
              <div className="w-px h-6 bg-border mx-2"></div>
              <div className="flex items-center gap-3">
                <div className="text-right hidden sm:block">
                  <p className="text-sm font-bold leading-none">Super Admin</p>
                  <p className="text-[10px] text-muted-foreground">admin@optix</p>
                </div>
                <div className="w-10 h-10 bg-muted rounded-xl flex items-center justify-center border border-border">
                  <User size={20} />
                </div>
              </div>
            </div>
          </div>
        </header>

        {/* Page Area */}
        <main className="flex-1 overflow-y-auto p-8 custom-scrollbar">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default DashboardLayout;
