import React from 'react';
import { NavLink } from 'react-router-dom';
import {
  LayoutDashboard,
  Wallet,
  ArrowRightLeft,
  Users,
  History,
  FileText,
  Bell,
  User,
  ShieldAlert,
  FileCheck2,
} from 'lucide-react';
import clsx from 'clsx';
import { useNotifications } from '../../context/NotificationContext';

interface SidebarProps {
  isAdmin?: boolean;
}

interface NavItem {
  to: string;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  badge?: number;
}

export const Sidebar: React.FC<SidebarProps> = ({ isAdmin = false }) => {
  const { unreadCount } = useNotifications();

  const customerLinks: NavItem[] = [
    { to: '/dashboard', label: 'Overview', icon: LayoutDashboard },
    { to: '/accounts', label: 'Accounts', icon: Wallet },
    { to: '/transfer', label: 'Transfer Money', icon: ArrowRightLeft },
    { to: '/beneficiaries', label: 'Beneficiaries', icon: Users },
    { to: '/transactions', label: 'Transactions', icon: History },
    { to: '/statements', label: 'Statements', icon: FileText },
    { to: '/notifications', label: 'Notifications', icon: Bell, badge: unreadCount },
    { to: '/profile', label: 'Profile & Security', icon: User },
  ];

  const adminLinks: NavItem[] = [
    { to: '/admin', label: 'Admin Command', icon: LayoutDashboard },
    { to: '/admin/fraud', label: 'Fraud Alerts', icon: ShieldAlert },
    { to: '/admin/audit', label: 'Audit Trail', icon: FileCheck2 },
  ];

  const links = isAdmin ? adminLinks : customerLinks;

  return (
    <aside className="w-64 border-r border-slate-800/80 bg-navy-900/40 backdrop-blur-md p-4 flex flex-col justify-between hidden md:flex min-h-[calc(100vh-4rem)]">
      <nav className="space-y-1.5">
        <div className="px-3 py-2 text-[11px] font-bold text-slate-500 uppercase tracking-wider">
          {isAdmin ? 'Administration Portal' : 'Banking Operations'}
        </div>
        {links.map(link => {
          const Icon = link.icon;
          return (
            <NavLink
              key={link.to}
              to={link.to}
              end={link.to === '/dashboard' || link.to === '/admin'}
              className={({ isActive }) =>
                clsx(
                  'flex items-center justify-between px-3.5 py-2.5 rounded-xl text-sm font-medium transition-all group',
                  isActive
                    ? 'bg-brand-500/10 text-brand-400 border border-brand-500/20 shadow-glow'
                    : 'text-slate-400 hover:text-slate-100 hover:bg-slate-800/50'
                )
              }
            >
              <div className="flex items-center gap-3">
                <Icon className="w-4 h-4 transition-transform group-hover:scale-110" />
                <span>{link.label}</span>
              </div>
              {link.badge !== undefined && link.badge > 0 && (
                <span className="px-2 py-0.5 text-[10px] font-bold rounded-full bg-rose-500 text-white">
                  {link.badge}
                </span>
              )}
            </NavLink>
          );
        })}
      </nav>

      <div className="p-3 glass-card rounded-xl border border-slate-800 text-xs text-slate-400">
        <div className="flex items-center gap-2 text-emerald-400 font-semibold mb-1">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping"></span>
          Core Banking Live
        </div>
        <p className="text-[11px] text-slate-500">PostgreSQL 16 • Redis 7 • Kafka</p>
      </div>
    </aside>
  );
};
