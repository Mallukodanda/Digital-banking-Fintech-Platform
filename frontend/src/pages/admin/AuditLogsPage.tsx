import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { adminApi } from '../../api/endpoints';
import { AuditLogFilterRequest } from '../../types';
import { Badge } from '../../components/common/Badge';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';
import { FileCheck2, ChevronLeft, ChevronRight, Search } from 'lucide-react';

export const AuditLogsPage: React.FC = () => {
  const [filter, setFilter] = useState<AuditLogFilterRequest>({});
  const [actorUsername, setActorUsername] = useState('');
  const [resourceType, setResourceType] = useState('');
  const [action, setAction] = useState('');
  const [page, setPage] = useState(0);

  const { data: auditData, isLoading } = useQuery({
    queryKey: ['admin-audit-logs', filter, page],
    queryFn: () => adminApi.getAuditLogs(filter, page, 15),
  });

  const auditLogs = auditData?.data?.content || [];
  const totalPages = auditData?.data?.totalPages || 0;
  const totalElements = auditData?.data?.totalElements || 0;

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    setFilter({
      actorUsername: actorUsername || undefined,
      resourceType: resourceType || undefined,
      action: (action as any) || undefined,
    });
  };

  const handleReset = () => {
    setActorUsername('');
    setResourceType('');
    setAction('');
    setPage(0);
    setFilter({});
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">Forensic Audit Trail</h1>
        <p className="text-sm text-slate-400 mt-1">
          Immutable compliance record with multi-criteria dynamic JPA query filtering
        </p>
      </div>

      {/* Dynamic Filter Card */}
      <div className="glass-card p-6 rounded-2xl border border-slate-800 shadow-xl space-y-4">
        <form onSubmit={handleSearch} className="grid grid-cols-1 sm:grid-cols-4 gap-4">
          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
              Actor Username
            </label>
            <input
              type="text"
              value={actorUsername}
              onChange={e => setActorUsername(e.target.value)}
              placeholder="e.g. admin@bank.com"
              className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-white text-xs font-mono focus:outline-none focus:border-brand-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
              Resource Type
            </label>
            <input
              type="text"
              value={resourceType}
              onChange={e => setResourceType(e.target.value)}
              placeholder="ACCOUNT, TRANSFER, USER"
              className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-white text-xs font-mono focus:outline-none focus:border-brand-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
              Audit Action
            </label>
            <input
              type="text"
              value={action}
              onChange={e => setAction(e.target.value)}
              placeholder="TRANSFER_COMPLETED"
              className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-white text-xs font-mono focus:outline-none focus:border-brand-500"
            />
          </div>

          <div className="flex items-end gap-2">
            <button
              type="submit"
              className="flex-1 py-2 bg-brand-500 hover:bg-brand-400 text-navy-950 font-bold rounded-xl text-xs transition-all shadow-glow flex items-center justify-center gap-1.5"
            >
              <Search className="w-3.5 h-3.5" /> Filter
            </button>
            <button
              type="button"
              onClick={handleReset}
              className="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold text-xs rounded-xl border border-slate-700 transition-all"
            >
              Reset
            </button>
          </div>
        </form>
      </div>

      {/* Audit Logs Table */}
      <div className="glass-card rounded-2xl border border-slate-800 overflow-hidden shadow-xl">
        <div className="px-6 py-4 border-b border-slate-800/80 flex items-center justify-between">
          <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
            Total Audited Events: {totalElements}
          </span>
          <span className="text-xs font-mono text-slate-500">
            Page {page + 1} of {Math.max(1, totalPages)}
          </span>
        </div>

        {isLoading ? (
          <div className="p-6">
            <LoadingSkeleton rows={5} className="h-14" />
          </div>
        ) : auditLogs.length === 0 ? (
          <div className="p-16 text-center text-slate-500 text-sm">
            <FileCheck2 className="w-8 h-8 mx-auto mb-2 text-slate-600" />
            No audit records found matching query parameters.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="border-b border-slate-800 bg-slate-900/40 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                  <th className="py-3.5 px-6">Action & Result</th>
                  <th className="py-3.5 px-6">Actor Identity</th>
                  <th className="py-3.5 px-6">Target Resource</th>
                  <th className="py-3.5 px-6">Correlation ID</th>
                  <th className="py-3.5 px-6">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 font-mono">
                {auditLogs.map(log => (
                  <tr key={log.id} className="hover:bg-slate-800/20 transition-colors">
                    <td className="py-4 px-6">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-white text-xs">{log.action}</span>
                        <Badge
                          variant={
                            log.result === 'SUCCESS'
                              ? 'success'
                              : log.result === 'FAILURE'
                              ? 'danger'
                              : 'warning'
                          }
                          size="sm"
                        >
                          {log.result}
                        </Badge>
                      </div>
                    </td>
                    <td className="py-4 px-6 text-slate-300">
                      <span className="block font-bold">{log.actorUsername || 'SYSTEM'}</span>
                      <span className="text-[10px] text-slate-500">{log.actorRole}</span>
                    </td>
                    <td className="py-4 px-6 text-slate-300">
                      <span className="text-white font-bold">{log.resourceType}: </span>
                      <span className="text-slate-400">{log.resourceId}</span>
                    </td>
                    <td className="py-4 px-6 text-slate-500 truncate max-w-[140px]">
                      {log.correlationId || 'N/A'}
                    </td>
                    <td className="py-4 px-6 text-slate-400">
                      {new Date(log.createdAt).toLocaleString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="px-6 py-4 border-t border-slate-800 flex items-center justify-between">
            <button
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3.5 py-1.5 rounded-xl bg-slate-900 border border-slate-700 text-xs font-semibold text-slate-300 hover:text-white disabled:opacity-40 flex items-center gap-1.5"
            >
              <ChevronLeft className="w-4 h-4" /> Previous
            </button>
            <button
              onClick={() => setPage(p => p + 1)}
              disabled={page >= totalPages - 1}
              className="px-3.5 py-1.5 rounded-xl bg-slate-900 border border-slate-700 text-xs font-semibold text-slate-300 hover:text-white disabled:opacity-40 flex items-center gap-1.5"
            >
              Next <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        )}
      </div>
    </div>
  );
};
