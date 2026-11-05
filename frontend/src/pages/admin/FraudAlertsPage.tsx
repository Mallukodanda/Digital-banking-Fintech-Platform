import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { adminApi } from '../../api/endpoints';
import { FraudStatus, FraudAlertResponse } from '../../types';
import { Badge } from '../../components/common/Badge';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';
import { AlertBanner } from '../../components/common/AlertBanner';
import { ShieldAlert, CheckCircle, XCircle, ChevronLeft, ChevronRight, Eye } from 'lucide-react';

export const FraudAlertsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [statusFilter, setStatusFilter] = useState<FraudStatus | undefined>(undefined);
  const [page, setPage] = useState(0);
  const [selectedAlert, setSelectedAlert] = useState<FraudAlertResponse | null>(null);
  const [reviewerNotes, setReviewerNotes] = useState('');
  const [success, setSuccess] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const { data: alertsData, isLoading } = useQuery({
    queryKey: ['admin-fraud-alerts', statusFilter, page],
    queryFn: () => adminApi.getFraudAlerts(statusFilter, page, 10),
  });

  const reviewMutation = useMutation({
    mutationFn: ({ alertId, status, notes }: { alertId: string; status: FraudStatus; notes: string }) =>
      adminApi.reviewFraudAlert(alertId, { status, reviewerNotes: notes }),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['admin-fraud-alerts'] });
      setSuccess(`Fraud alert ${res.data.id} marked as ${res.data.status}.`);
      setSelectedAlert(null);
      setReviewerNotes('');
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || 'Failed to review fraud alert.');
    },
  });

  const alerts = alertsData?.data?.content || [];
  const totalPages = alertsData?.data?.totalPages || 0;

  const handleReviewAction = (status: FraudStatus) => {
    if (!selectedAlert) return;
    reviewMutation.mutate({
      alertId: selectedAlert.id,
      status,
      notes: reviewerNotes || `Admin reviewed and marked as ${status}`,
    });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Fraud Surveillance & Review</h1>
          <p className="text-sm text-slate-400 mt-1">
            Investigate algorithmic rule violations and resolve high-risk transfers
          </p>
        </div>

        {/* Filter */}
        <select
          value={statusFilter || ''}
          onChange={e => {
            setStatusFilter((e.target.value as FraudStatus) || undefined);
            setPage(0);
          }}
          className="px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:border-rose-500"
        >
          <option value="">All Statuses (All Alerts)</option>
          <option value="FLAGGED">FLAGGED (Requires Attention)</option>
          <option value="INVESTIGATING">INVESTIGATING</option>
          <option value="CONFIRMED_FRAUD">CONFIRMED_FRAUD</option>
          <option value="DISMISSED">DISMISSED (False Positive)</option>
        </select>
      </div>

      {success && (
        <AlertBanner
          type="success"
          message={success}
          onClose={() => setSuccess(null)}
        />
      )}
      {error && (
        <AlertBanner
          type="danger"
          message={error}
          onClose={() => setError(null)}
        />
      )}

      {/* Fraud Table */}
      <div className="glass-card rounded-2xl border border-slate-800 overflow-hidden shadow-xl">
        {isLoading ? (
          <div className="p-6">
            <LoadingSkeleton rows={5} className="h-14" />
          </div>
        ) : alerts.length === 0 ? (
          <div className="p-16 text-center text-slate-500 text-sm">
            <ShieldAlert className="w-8 h-8 mx-auto mb-2 text-slate-600" />
            No fraud alerts match the selected criteria.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-800 bg-slate-900/40 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                  <th className="py-3.5 px-6">Account & Rule</th>
                  <th className="py-3.5 px-6">Risk Score</th>
                  <th className="py-3.5 px-6">Level</th>
                  <th className="py-3.5 px-6">Status</th>
                  <th className="py-3.5 px-6">Timestamp</th>
                  <th className="py-3.5 px-6 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-sm">
                {alerts.map(alert => (
                  <tr key={alert.id} className="hover:bg-slate-800/20 transition-colors">
                    <td className="py-4 px-6">
                      <p className="font-bold text-white text-xs">{alert.triggeredRule}</p>
                      <p className="text-xs font-mono text-slate-400">Acc #{alert.accountNumber}</p>
                    </td>
                    <td className="py-4 px-6 font-mono font-bold text-rose-400">
                      {alert.riskScore} / 100
                    </td>
                    <td className="py-4 px-6">
                      <Badge
                        variant={
                          alert.riskLevel === 'CRITICAL' || alert.riskLevel === 'HIGH'
                            ? 'danger'
                            : 'warning'
                        }
                      >
                        {alert.riskLevel}
                      </Badge>
                    </td>
                    <td className="py-4 px-6">
                      <Badge
                        variant={
                          alert.status === 'CONFIRMED_FRAUD'
                            ? 'danger'
                            : alert.status === 'DISMISSED'
                            ? 'neutral'
                            : 'warning'
                        }
                      >
                        {alert.status}
                      </Badge>
                    </td>
                    <td className="py-4 px-6 text-xs font-mono text-slate-400">
                      {new Date(alert.createdAt).toLocaleString()}
                    </td>
                    <td className="py-4 px-6 text-right">
                      <button
                        onClick={() => setSelectedAlert(alert)}
                        className="px-3 py-1.5 bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-semibold rounded-lg border border-slate-700 transition-colors inline-flex items-center gap-1.5"
                      >
                        <Eye className="w-3.5 h-3.5" /> Review
                      </button>
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
            <span className="text-xs font-mono text-slate-500">
              Page {page + 1} of {totalPages}
            </span>
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

      {/* Review Modal */}
      {selectedAlert && (
        <div className="fixed inset-0 bg-navy-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="glass-card max-w-lg w-full p-6 rounded-2xl border border-slate-700 shadow-2xl space-y-5">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-white flex items-center gap-2">
                <ShieldAlert className="w-5 h-5 text-rose-400" /> Fraud Case Adjudication
              </h3>
              <Badge variant="danger">Score: {selectedAlert.riskScore}</Badge>
            </div>

            <div className="p-4 bg-slate-900/80 rounded-xl border border-slate-800 space-y-2 text-xs font-mono">
              <p><strong className="text-slate-400">Rule Violated:</strong> <span className="text-white">{selectedAlert.triggeredRule}</span></p>
              <p><strong className="text-slate-400">Account:</strong> <span className="text-white">{selectedAlert.accountNumber}</span></p>
              <p><strong className="text-slate-400">Alert ID:</strong> <span className="text-slate-400">{selectedAlert.id}</span></p>
              {selectedAlert.metadata && (
                <div className="mt-2 pt-2 border-t border-slate-800">
                  <strong className="text-slate-400 block mb-1">Metadata:</strong>
                  <pre className="p-2 bg-black/40 rounded text-[11px] text-slate-300 overflow-x-auto font-mono">
                    {selectedAlert.metadata}
                  </pre>
                </div>
              )}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                Reviewer Investigation Notes
              </label>
              <textarea
                rows={3}
                value={reviewerNotes}
                onChange={e => setReviewerNotes(e.target.value)}
                placeholder="Detail the outcome of phone verification or AML check..."
                className="w-full px-3 py-2.5 bg-slate-900 border border-slate-700 rounded-xl text-white text-xs focus:outline-none focus:border-rose-500"
              />
            </div>

            <div className="flex flex-col sm:flex-row gap-3 pt-2">
              <button
                type="button"
                onClick={() => handleReviewAction('DISMISSED')}
                disabled={reviewMutation.isPending}
                className="flex-1 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold text-xs rounded-xl border border-slate-700 transition-all flex items-center justify-center gap-1.5"
              >
                <CheckCircle className="w-4 h-4 text-emerald-400" /> Dismiss (False Positive)
              </button>
              <button
                type="button"
                onClick={() => handleReviewAction('CONFIRMED_FRAUD')}
                disabled={reviewMutation.isPending}
                className="flex-1 py-2.5 bg-rose-600 hover:bg-rose-500 text-white font-bold text-xs rounded-xl transition-all shadow-glow-danger flex items-center justify-center gap-1.5"
              >
                <XCircle className="w-4 h-4" /> Confirm Fraud
              </button>
            </div>

            <button
              onClick={() => setSelectedAlert(null)}
              className="w-full text-center text-xs text-slate-500 hover:text-slate-300 pt-1"
            >
              Cancel
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
