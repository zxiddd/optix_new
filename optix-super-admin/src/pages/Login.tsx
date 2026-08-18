import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ShieldCheck, Lock, Mail, ArrowRight, AlertCircle } from 'lucide-react';

const Login: React.FC = () => {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    setTimeout(() => {
      const cleanEmail = email.trim().toLowerCase();
      const validPasses = ['zaddy123', 'zaddy123!', 'zaddy@123', 'zaddy@787'];
      if ((cleanEmail === 'admin@optix' || cleanEmail === 'admin@optix.in' || cleanEmail === 'admin') && validPasses.includes(password.trim().toLowerCase())) {
        localStorage.setItem('admin_authenticated', 'true');
        localStorage.setItem('token', 'super_admin_verified_token');
        navigate('/');
      } else {
        setError('Invalid Super Admin credentials. Please check username and password.');
        setLoading(false);
      }
    }, 400);
  };

  return (
    <div className="min-h-screen bg-background text-foreground dark flex items-center justify-center p-4">
      <div className="max-w-md w-full bg-card border border-border rounded-3xl p-8 shadow-2xl space-y-6">
        <div className="text-center space-y-2">
          <div className="w-14 h-14 bg-primary/10 border border-primary/20 rounded-2xl flex items-center justify-center mx-auto text-primary">
            <ShieldCheck size={28} />
          </div>
          <h1 className="text-2xl font-black tracking-tight">OPTIX <span className="text-primary">SUPER ADMIN</span></h1>
          <p className="text-muted-foreground text-xs font-medium">Enterprise Offline-First SaaS Control Center</p>
        </div>

        {error && (
          <div className="p-4 bg-red-500/10 border border-red-500/30 text-red-400 text-xs font-bold rounded-xl flex items-center gap-2">
            <AlertCircle size={16} /> {error}
          </div>
        )}

        <form onSubmit={handleLogin} className="space-y-4">
          <div>
            <label className="text-xs font-bold text-muted-foreground block mb-1.5 uppercase">Admin Username</label>
            <div className="relative">
              <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" size={18} />
              <input
                type="text"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="admin@optix"
                className="w-full bg-muted/40 border border-border rounded-xl py-2.5 pl-10 pr-4 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>
          </div>

          <div>
            <label className="text-xs font-bold text-muted-foreground block mb-1.5 uppercase">Password</label>
            <div className="relative">
              <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted-foreground" size={18} />
              <input
                type="password"
                required
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full bg-muted/40 border border-border rounded-xl py-2.5 pl-10 pr-4 text-sm font-medium focus:outline-none focus:ring-2 focus:ring-primary/20"
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 bg-primary hover:bg-primary/90 text-black font-black text-xs rounded-xl flex items-center justify-center gap-2 transition-colors shadow-lg shadow-primary/20 uppercase tracking-wider"
          >
            {loading ? 'Authenticating...' : 'Sign In to Dashboard'} <ArrowRight size={16} />
          </button>
        </form>

        <div className="text-center text-[11px] text-muted-foreground pt-2">
          Secure Authenticated Session • Enterprise Platform Operations
        </div>
      </div>
    </div>
  );
};

export default Login;
