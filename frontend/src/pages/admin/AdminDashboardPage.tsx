import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { adminApi } from '../../api/endpoints';
import { StatCard } from '../../components/common/StatCard';
import { Badge } from '../../components/common/Badge';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';
import { ShieldAlert, FileCheck2, Activity, ShieldCheck, AlertTriangle } from 'lucide-react';
import { Link } from 'react-router-dom';

export const AdminDashboardPage: React.FC = () => {
  const { data: fraudData, isLoading: fraudLoading } = useQuery({
    queryKey: ['admin-fraud-recent'],
    queryFn: () => adminApi.getFraudAlerts(undefined, 0, 5),
  });

  const { data: auditData, isLoading: auditLoading } = useQuery({
    queryKey: ['admin-audit-recent'],
    queryFn: () => adminApi.getAuditLogs({}, 0, 5),
  });

  const fraudAlerts = fraudData?.data?.content || [];
  const auditLogs = auditData?.data?.content || [];

  const pendingFraudCount = fraudAlerts.filter(a => a.status === 'FLAGGED' || a.status === 'INVESTIGATING').length;

  return (
    <div className="space-y-8">
      {/* Admin Header */}
      <div className="glass-card rounded-2xl p-6 sm:p-8 border border-slate-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full bg-rose-500/10 text-rose-400 text-xs font-semibold mb-2 border border-rose-500/20">
            <ShieldCheck className="w-3.5 h-3.5" /> High-Assurance Administration Portal
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white">System Command & Risk Center</h1>
          <p className="text-sm text-slate-400 mt-1">
            Real-time fraud surveillance, distributed audit trail, and account freeze operations
          </p>
        </div>

        <div className="flex gap-3">
          <Link
            to="/admin/fraud"
            className="px-4 py-2.5 bg-rose-500 hover:bg-rose-400 text-white font-bold rounded-xl text-sm transition-all shadow-glow-danger flex items-center gap-2"
          >
            <ShieldAlert className="w-4 h-4" /> Fraud Workstation
          </Link>
          <Link
            to="/admin/audit"
            className="px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold rounded-xl text-sm transition-all border border-slate-700 flex items-center gap-2"
          >
            <FileCheck2 className="w-4 h-4" /> Audit Inspector
          </Link>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <StatCard
          title="Active Fraud Alerts"
          value={pendingFraudCount}
          subtitle="Awaiting administrative review"
          icon={ShieldAlert}
          variant="rose"
        />
        <StatCard
          title="Ingested Audit Logs"
          value={auditData?.data?.totalElements || 0}
          subtitle="Immutable compliance records"
          icon={FileCheck2}
          variant="navy"
        />
        <StatCard
          title="Engine Status"
          value="OPERATIONAL"
          subtitle="Kafka consumers & Rule Engine"
          icon={Activity}
          variant="brand"
        />
      </div>

      {/* Grid: Fraud alerts summary + Audit log feed */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Fraud Queue */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-rose-400" /> High-Risk Fraud Queue
            </h2>
            <Link to="/admin/fraud" className="text-xs font-semibold text-rose-400 hover:underline">
              View All Alerts
            </Link>
          </div>

          <div className="glass-card rounded-2xl border border-slate-800 overflow-hidden">
            {fraudLoading ? (
              <div className="p-6">
                <LoadingSkeleton rows={4} className="h-12" />
              </div>
            ) : fraudAlerts.length === 0 ? (
              <div className="p-12 text-center text-slate-500 text-sm">
                No active fraud alerts flagged by rule engine.
              </div>
            ) : (
              <div className="divide-y divide-slate-800/60">
                {fraudAlerts.map(alert => (
                  <div
                    key={alert.id}
                    className="p-4 flex items-center justify-between hover:bg-slate-800/30 transition-colors"
                  >
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-sm font-bold text-white">{alert.triggeredRule}</span>
                        <Badge
                          variant={
                            alert.riskLevel === 'CRITICAL' || alert.riskLevel === 'HIGH'
                              ? 'danger'
                              : 'warning'
                          }
                        >
                          Score: {alert.riskScore}
                        </Badge>
                      </div>
                      <p className="text-xs font-mono text-slate-400 mt-1">
                        Acc #{alert.accountNumber} • {new Date(alert.createdAt).toLocaleTimeString()}
                      </p>
                    </div>
                    <Badge variant={alert.status === 'FLAGGED' ? 'danger' : 'neutral'}>
                      {alert.status}
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Audit Trail */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <FileCheck2 className="w-4 h-4 text-sky-400" /> Recent Security & Banking Audits
            </h2>
            <Link to="/admin/audit" className="text-xs font-semibold text-sky-400 hover:underline">
              Query All Logs
            </Link>
          </div>

          <div className="glass-card rounded-2xl border border-slate-800 overflow-hidden">
            {auditLoading ? (
              <div className="p-6">
                <LoadingSkeleton rows={4} className="h-12" />
              </div>
            ) : auditLogs.length === 0 ? (
              <div className="p-12 text-center text-slate-500 text-sm">
                No audit records recorded yet.
              </div>
            ) : (
              <div className="divide-y divide-slate-800/60">
                {auditLogs.map(log => (
                  <div
                    key={log.id}
                    className="p-4 flex items-center justify-between hover:bg-slate-800/30 transition-colors text-xs"
                  >
                    <div>
                      <p className="font-bold font-mono text-white">{log.action}</p>
                      <p className="text-slate-400 font-mono mt-0.5">
                        Actor: {log.actorUsername || log.actorRole} • Res: {log.resourceType} ({log.resourceId})
                      </p>
                    </div>
                    <Badge variant={log.result === 'SUCCESS' ? 'success' : 'danger'}>
                      {log.result}
                    </Badge>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
