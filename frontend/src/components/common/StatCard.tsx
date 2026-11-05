import React from 'react';
import { LucideIcon } from 'lucide-react';
import clsx from 'clsx';

interface StatCardProps {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  trend?: {
    value: string;
    positive: boolean;
  };
  variant?: 'brand' | 'navy' | 'rose' | 'amber';
}

export const StatCard: React.FC<StatCardProps> = ({
  title,
  value,
  subtitle,
  icon: Icon,
  trend,
  variant = 'navy',
}) => {
  const iconVariants = {
    brand: 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20',
    navy: 'bg-sky-500/10 text-sky-400 border border-sky-500/20',
    rose: 'bg-rose-500/10 text-rose-400 border border-rose-500/20',
    amber: 'bg-amber-500/10 text-amber-400 border border-amber-500/20',
  };

  return (
    <div className="glass-card glass-card-hover rounded-2xl p-6 relative overflow-hidden">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-slate-400">{title}</p>
          <h3 className="text-2xl font-bold text-white mt-1.5 font-mono">{value}</h3>
          {subtitle && <p className="text-xs text-slate-500 mt-1">{subtitle}</p>}
          {trend && (
            <div className="flex items-center gap-1.5 mt-2.5">
              <span
                className={clsx(
                  'text-xs font-semibold px-1.5 py-0.5 rounded',
                  trend.positive
                    ? 'text-emerald-400 bg-emerald-500/10'
                    : 'text-rose-400 bg-rose-500/10'
                )}
              >
                {trend.positive ? '+' : ''}
                {trend.value}
              </span>
              <span className="text-xs text-slate-500">vs last month</span>
            </div>
          )}
        </div>
        <div className={clsx('p-3 rounded-xl', iconVariants[variant])}>
          <Icon className="w-6 h-6" />
        </div>
      </div>
    </div>
  );
};
