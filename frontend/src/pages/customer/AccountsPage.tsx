import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { accountApi } from '../../api/endpoints';
import { Badge } from '../../components/common/Badge';
import { AlertBanner } from '../../components/common/AlertBanner';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';
import { Wallet, Plus, ShieldCheck } from 'lucide-react';

export const AccountsPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [accountType, setAccountType] = useState('CHECKING');
  const [currency, setCurrency] = useState('INR');
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const { data: accountsData, isLoading } = useQuery({
    queryKey: ['my-accounts'],
    queryFn: accountApi.getMyAccounts,
  });

  const createMutation = useMutation({
    mutationFn: accountApi.createAccount,
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['my-accounts'] });
      setShowCreateModal(false);
      setSuccessMsg(`Account #${res.data.accountNumber} provisioned successfully!`);
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || 'Failed to provision account.');
    },
  });

  const accounts = accountsData?.data || [];

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccessMsg(null);
    createMutation.mutate({ accountType, currency });
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Banking Accounts</h1>
          <p className="text-sm text-slate-400 mt-1">
            Manage your deposit, savings, and checking portfolios
          </p>
        </div>
        <button
          onClick={() => setShowCreateModal(true)}
          className="px-4 py-2.5 bg-brand-500 hover:bg-brand-400 text-navy-950 font-bold rounded-xl text-sm transition-all shadow-glow flex items-center gap-2"
        >
          <Plus className="w-4 h-4" /> Open New Account
        </button>
      </div>

      {successMsg && (
        <AlertBanner
          type="success"
          message={successMsg}
          onClose={() => setSuccessMsg(null)}
        />
      )}
      {error && (
        <AlertBanner
          type="danger"
          message={error}
          onClose={() => setError(null)}
        />
      )}

      {isLoading ? (
        <LoadingSkeleton rows={4} className="h-32" />
      ) : accounts.length === 0 ? (
        <div className="glass-card p-12 rounded-2xl text-center border border-slate-800">
          <Wallet className="w-12 h-12 text-slate-600 mx-auto mb-3" />
          <h3 className="text-lg font-bold text-white">No Accounts Found</h3>
          <p className="text-sm text-slate-400 mt-1">Open your first checking or savings account.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {accounts.map(acc => {
            const accId = acc.id || acc.accountId;
            const balance = Number(acc.availableBalance ?? acc.cachedBalance ?? acc.balance ?? 0);
            return (
              <div
                key={accId}
                className="glass-card glass-card-hover p-6 rounded-2xl border border-slate-800 flex flex-col justify-between space-y-6"
              >
                <div>
                  <div className="flex items-center justify-between mb-4">
                    <div className="flex items-center gap-2">
                      <div className="p-2 rounded-xl bg-brand-500/10 text-brand-400 border border-brand-500/20">
                        <Wallet className="w-5 h-5" />
                      </div>
                      <div>
                        <h3 className="text-sm font-bold text-white">{acc.accountType}</h3>
                        <p className="text-xs font-mono text-slate-400">{acc.currency}</p>
                      </div>
                    </div>
                    <Badge variant={acc.status === 'ACTIVE' ? 'success' : 'danger'}>
                      {acc.status}
                    </Badge>
                  </div>

                  <div>
                    <p className="text-xs font-mono text-slate-400">ACCOUNT NUMBER</p>
                    <p className="text-base font-bold font-mono text-slate-200 mt-0.5 tracking-wider">
                      {acc.accountNumber}
                    </p>
                  </div>
                </div>

                <div className="pt-4 border-t border-slate-800/80 flex items-center justify-between">
                  <div>
                    <p className="text-xs text-slate-500">Available Balance</p>
                    <p className="text-xl font-bold font-mono text-brand-400 mt-0.5">
                      ₹{balance.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </p>
                  </div>
                  <div className="flex items-center gap-1 text-[11px] text-emerald-400 font-medium">
                    <ShieldCheck className="w-3.5 h-3.5" /> Synchronized
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Account Creation Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-navy-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="glass-card max-w-md w-full p-6 rounded-2xl border border-slate-700 shadow-2xl space-y-5">
            <h3 className="text-lg font-bold text-white">Open New Banking Portfolio</h3>
            <form onSubmit={handleCreate} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                  Account Type
                </label>
                <select
                  value={accountType}
                  onChange={e => setAccountType(e.target.value)}
                  className="w-full px-3 py-2.5 bg-slate-900 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:border-brand-500"
                >
                  <option value="CHECKING">CHECKING (P2P Transfers & Daily Liquidity)</option>
                  <option value="SAVINGS">SAVINGS (High Yield Interest)</option>
                  <option value="BUSINESS">BUSINESS (Corporate Ledger)</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                  Denominated Currency
                </label>
                <select
                  value={currency}
                  onChange={e => setCurrency(e.target.value)}
                  className="w-full px-3 py-2.5 bg-slate-900 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:border-brand-500"
                >
                  <option value="INR">INR (Indian Rupee - ₹)</option>
                  <option value="USD">USD (United States Dollar - $)</option>
                  <option value="EUR">EUR (Euro - €)</option>
                </select>
              </div>

              <div className="flex gap-3 pt-4 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="flex-1 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-300 font-semibold rounded-xl text-sm transition-all"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createMutation.isPending}
                  className="flex-1 py-2.5 bg-brand-500 hover:bg-brand-400 text-navy-950 font-bold rounded-xl text-sm transition-all shadow-glow flex items-center justify-center gap-2"
                >
                  {createMutation.isPending ? 'Provisioning...' : 'Confirm'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
