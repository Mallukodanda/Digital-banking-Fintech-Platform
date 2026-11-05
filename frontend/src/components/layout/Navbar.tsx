import React from 'react';
import { Bell, ShieldCheck, LogOut, User as UserIcon } from 'lucide-react';
import { useAuth } from '../../context/AuthContext';
import { useNotifications } from '../../context/NotificationContext';
import { Link } from 'react-router-dom';

export const Navbar: React.FC<{ isAdmin?: boolean }> = ({ isAdmin = false }) => {
  const { user, logout } = useAuth();
  const { unreadCount } = useNotifications();

  return (
    <header className="h-16 border-b border-slate-800/80 bg-navy-900/60 backdrop-blur-xl sticky top-0 z-30 px-6 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <Link to={isAdmin ? "/admin" : "/dashboard"} className="flex items-center gap-2.5">
          <div className="w-9 h-9 rounded-xl gradient-brand flex items-center justify-center shadow-glow">
            <ShieldCheck className="w-5 h-5 text-navy-950 stroke-[2.5]" />
          </div>
          <div>
            <span className="font-extrabold text-lg tracking-tight text-white">APEX<span className="text-brand-500">PAY</span></span>
            {isAdmin && <span className="ml-2 text-[10px] uppercase font-bold tracking-widest bg-rose-500/20 text-rose-400 px-1.5 py-0.5 rounded border border-rose-500/30">ADMIN</span>}
          </div>
        </Link>
      </div>

      <div className="flex items-center gap-4">
        {!isAdmin && (
          <Link
            to="/notifications"
            className="relative p-2 rounded-xl text-slate-400 hover:text-white hover:bg-slate-800/60 transition-colors"
            title="Notifications"
          >
            <Bell className="w-5 h-5" />
            {unreadCount > 0 && (
              <span className="absolute top-1.5 right-1.5 w-4 h-4 rounded-full bg-rose-500 text-[10px] font-bold text-white flex items-center justify-center animate-pulse">
                {unreadCount > 9 ? '9+' : unreadCount}
              </span>
            )}
          </Link>
        )}

        <div className="h-5 w-[1px] bg-slate-800"></div>

        <div className="flex items-center gap-3">
          <div className="text-right hidden sm:block">
            <p className="text-xs font-semibold text-white">
              {user ? `${user.firstName} ${user.lastName}` : 'Authenticated User'}
            </p>
            <p className="text-[11px] text-slate-400 font-mono">{user?.email}</p>
          </div>
          <Link
            to={isAdmin ? "/admin" : "/profile"}
            className="w-9 h-9 rounded-xl bg-slate-800 border border-slate-700 flex items-center justify-center text-slate-300 hover:text-brand-400 hover:border-brand-500/40 transition-colors"
          >
            <UserIcon className="w-4 h-4" />
          </Link>
          <button
            onClick={logout}
            className="p-2 rounded-xl text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors"
            title="Sign Out"
          >
            <LogOut className="w-5 h-5" />
          </button>
        </div>
      </div>
    </header>
  );
};
