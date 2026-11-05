import React from 'react';
import { AlertCircle, CheckCircle2, Info, AlertTriangle, X } from 'lucide-react';
import clsx from 'clsx';

interface AlertBannerProps {
  type?: 'success' | 'danger' | 'warning' | 'info';
  title?: string;
  message: string;
  onClose?: () => void;
  className?: string;
}

export const AlertBanner: React.FC<AlertBannerProps> = ({
  type = 'info',
  title,
  message,
  onClose,
  className,
}) => {
  const styles = {
    success: 'bg-emerald-950/40 border-emerald-500/30 text-emerald-300',
    danger: 'bg-rose-950/40 border-rose-500/30 text-rose-300',
    warning: 'bg-amber-950/40 border-amber-500/30 text-amber-300',
    info: 'bg-sky-950/40 border-sky-500/30 text-sky-300',
  };

  const icons = {
    success: CheckCircle2,
    danger: AlertCircle,
    warning: AlertTriangle,
    info: Info,
  };

  const Icon = icons[type];

  return (
    <div
      className={clsx(
        'flex items-start gap-3 p-4 rounded-xl border backdrop-blur-md transition-all',
        styles[type],
        className
      )}
    >
      <Icon className="w-5 h-5 flex-shrink-0 mt-0.5" />
      <div className="flex-1 text-sm">
        {title && <h4 className="font-semibold text-white mb-0.5">{title}</h4>}
        <p className="leading-relaxed opacity-90">{message}</p>
      </div>
      {onClose && (
        <button
          onClick={onClose}
          className="text-slate-400 hover:text-white transition-colors p-1"
        >
          <X className="w-4 h-4" />
        </button>
      )}
    </div>
  );
};
