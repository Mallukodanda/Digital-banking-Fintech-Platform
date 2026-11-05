import React from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { notificationApi } from '../../api/endpoints';
import { useNotifications } from '../../context/NotificationContext';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';
import { Badge } from '../../components/common/Badge';
import { Bell, CheckCheck, ShieldAlert, ArrowDownLeft, Info, Check } from 'lucide-react';

export const NotificationsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const { refreshUnreadCount, clearUnreadCount } = useNotifications();

  const { data: notificationsData, isLoading } = useQuery({
    queryKey: ['my-notifications'],
    queryFn: () => notificationApi.getMyNotifications(0, 30),
  });

  const markReadMutation = useMutation({
    mutationFn: notificationApi.markAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-notifications'] });
      refreshUnreadCount();
    },
  });

  const markAllMutation = useMutation({
    mutationFn: notificationApi.markAllAsRead,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-notifications'] });
      clearUnreadCount();
    },
  });

  const notifications = notificationsData?.data?.content || [];
  const unreadExist = notifications.some(n => n.status === 'UNREAD');

  const getIcon = (type: string) => {
    switch (type) {
      case 'FRAUD_ALERT':
        return <ShieldAlert className="w-5 h-5 text-rose-400" />;
      case 'TRANSFER_SUCCESSFUL':
        return <ArrowDownLeft className="w-5 h-5 text-emerald-400" />;
      default:
        return <Info className="w-5 h-5 text-sky-400" />;
    }
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Notifications & Alerts</h1>
          <p className="text-sm text-slate-400 mt-1">
            Real-time event stream dispatched from Kafka transactional consumers
          </p>
        </div>

        {unreadExist && (
          <button
            onClick={() => markAllMutation.mutate()}
            disabled={markAllMutation.isPending}
            className="px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold rounded-xl text-xs border border-slate-700 flex items-center gap-1.5 transition-all"
          >
            <CheckCheck className="w-4 h-4 text-brand-400" /> Mark All as Read
          </button>
        )}
      </div>

      <div className="glass-card rounded-2xl border border-slate-800 overflow-hidden shadow-xl">
        {isLoading ? (
          <div className="p-6">
            <LoadingSkeleton rows={5} className="h-16" />
          </div>
        ) : notifications.length === 0 ? (
          <div className="p-16 text-center text-slate-500 text-sm">
            <Bell className="w-8 h-8 mx-auto mb-2 text-slate-600" />
            No notifications in your inbox.
          </div>
        ) : (
          <div className="divide-y divide-slate-800/60">
            {notifications.map(n => {
              const isUnread = n.status === 'UNREAD';
              return (
                <div
                  key={n.id}
                  className={`p-5 flex items-start justify-between gap-4 transition-colors ${
                    isUnread ? 'bg-slate-800/30' : 'hover:bg-slate-800/20'
                  }`}
                >
                  <div className="flex items-start gap-3.5">
                    <div className="p-2.5 rounded-xl bg-slate-900 border border-slate-700/80 mt-0.5">
                      {getIcon(n.type)}
                    </div>
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <h4 className="text-sm font-bold text-white">{n.title}</h4>
                        {isUnread && (
                          <span className="w-2 h-2 rounded-full bg-brand-400 animate-pulse"></span>
                        )}
                        <Badge
                          variant={
                            n.type === 'FRAUD_ALERT'
                              ? 'danger'
                              : n.type === 'TRANSFER_SUCCESSFUL'
                              ? 'success'
                              : 'info'
                          }
                          size="sm"
                        >
                          {n.type}
                        </Badge>
                      </div>
                      <p className="text-xs text-slate-300 leading-relaxed">{n.message}</p>
                      <p className="text-[11px] font-mono text-slate-500 mt-2">
                        {new Date(n.createdAt).toLocaleString()} • Channel: {n.channel}
                      </p>
                    </div>
                  </div>

                  {isUnread && (
                    <button
                      onClick={() => markReadMutation.mutate(n.id)}
                      className="px-3 py-1.5 bg-slate-800 hover:bg-brand-500 hover:text-navy-950 text-slate-300 font-semibold text-xs rounded-lg border border-slate-700 transition-all flex items-center gap-1"
                    >
                      <Check className="w-3.5 h-3.5" /> Read
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
