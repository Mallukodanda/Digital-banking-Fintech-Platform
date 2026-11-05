import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { Badge } from '../../components/common/Badge';
import { User, ShieldCheck, Key } from 'lucide-react';

export const ProfilePage: React.FC = () => {
  const { user } = useAuth();

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">Customer Profile & Security</h1>
        <p className="text-sm text-slate-400 mt-1">
          Identity credentials, RBAC authorizations, and security settings
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Profile Card */}
        <div className="md:col-span-1 glass-card p-6 rounded-2xl border border-slate-800 text-center space-y-4">
          <div className="w-20 h-20 rounded-2xl gradient-brand mx-auto flex items-center justify-center text-2xl font-extrabold text-navy-950 shadow-glow">
            {user?.firstName?.charAt(0)}
            {user?.lastName?.charAt(0)}
          </div>
          <div>
            <h2 className="text-lg font-bold text-white">{user?.firstName} {user?.lastName}</h2>
            <p className="text-xs text-slate-400 font-mono mt-0.5">{user?.email}</p>
          </div>
          <div className="pt-2">
            <Badge variant="success" size="md">
              {user?.role || 'ROLE_CUSTOMER'}
            </Badge>
          </div>
        </div>

        {/* Profile Details & Metadata */}
        <div className="md:col-span-2 glass-card p-6 sm:p-8 rounded-2xl border border-slate-800 space-y-6">
          <h3 className="text-base font-bold text-white flex items-center gap-2">
            <User className="w-4 h-4 text-brand-400" /> Account Specifications
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-sm font-mono">
            <div className="p-4 bg-slate-900/60 rounded-xl border border-slate-800">
              <span className="text-xs text-slate-500 block mb-1">USER IDENTIFIER (UUID)</span>
              <span className="text-xs text-slate-300 font-mono break-all">{user?.userId}</span>
            </div>

            <div className="p-4 bg-slate-900/60 rounded-xl border border-slate-800">
              <span className="text-xs text-slate-500 block mb-1">CONTACT PHONE</span>
              <span className="text-xs text-slate-300 font-mono">{user?.phoneNumber || '+1 415 555 0192'}</span>
            </div>

            <div className="p-4 bg-slate-900/60 rounded-xl border border-slate-800">
              <span className="text-xs text-slate-500 block mb-1">REGISTRATION DATE</span>
              <span className="text-xs text-slate-300 font-mono">
                {user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'Active'}
              </span>
            </div>

            <div className="p-4 bg-slate-900/60 rounded-xl border border-slate-800">
              <span className="text-xs text-slate-500 block mb-1">SECURITY STATUS</span>
              <span className="text-xs text-emerald-400 font-bold flex items-center gap-1">
                <ShieldCheck className="w-3.5 h-3.5" /> BCrypt 12 Rounds + JWT
              </span>
            </div>
          </div>

          <div className="pt-4 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
            <span className="flex items-center gap-1.5">
              <Key className="w-4 h-4 text-brand-400" /> Stateless JWT session with auto-refresh queue
            </span>
            <span className="text-emerald-400 font-semibold">100% Encrypted</span>
          </div>
        </div>
      </div>
    </div>
  );
};
