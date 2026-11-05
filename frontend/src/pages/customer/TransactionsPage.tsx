import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { accountApi, transferApi } from '../../api/endpoints';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';
import { ArrowDownLeft, ArrowUpRight, History, ChevronLeft, ChevronRight, Filter } from 'lucide-react';

export const TransactionsPage: React.FC = () => {
  const [selectedAccountId, setSelectedAccountId] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const pageSize = 10;

  const { data: accountsData } = useQuery({
    queryKey: ['my-accounts'],
    queryFn: accountApi.getMyAccounts,
  });

  const accounts = accountsData?.data || [];
  const currentAccountId = selectedAccountId || accounts[0]?.id || accounts[0]?.accountId || '';

  const { data: historyData, isLoading } = useQuery({
    queryKey: ['account-history', currentAccountId, page],
    queryFn: () => transferApi.getTransactionHistory(currentAccountId, page, pageSize),
    enabled: !!currentAccountId,
  });

  const transactions = historyData?.data?.content || [];
  const totalPages = historyData?.data?.totalPages || 0;
  const totalElements = historyData?.data?.totalElements || 0;

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">General Ledger Statement</h1>
          <p className="text-sm text-slate-400 mt-1">
            Immutable transaction history with running balance audit trail
          </p>
        </div>

        {/* Account Selector Filter */}
        {accounts.length > 0 && (
          <div className="flex items-center gap-2">
            <Filter className="w-4 h-4 text-slate-400" />
            <select
              value={currentAccountId}
              onChange={e => {
                setSelectedAccountId(e.target.value);
                setPage(0);
              }}
              className="px-3 py-2 bg-slate-900 border border-slate-700 rounded-xl text-white text-sm font-mono focus:outline-none focus:border-brand-500"
            >
              {accounts.map(acc => {
                const accId = acc.id || acc.accountId;
                return (
                  <option key={accId} value={accId}>
                    {acc.accountType} • {acc.accountNumber}
                  </option>
                );
              })}
            </select>
          </div>
        )}
      </div>

      {/* Transactions Table */}
      <div className="glass-card rounded-2xl border border-slate-800 overflow-hidden shadow-xl">
        <div className="px-6 py-4 border-b border-slate-800/80 flex items-center justify-between">
          <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
            Total Records: {totalElements}
          </span>
          <span className="text-xs font-mono text-slate-500">
            Page {page + 1} of {Math.max(1, totalPages)}
          </span>
        </div>

        {isLoading ? (
          <div className="p-6">
            <LoadingSkeleton rows={5} className="h-14" />
          </div>
        ) : transactions.length === 0 ? (
          <div className="p-16 text-center text-slate-500 text-sm">
            <History className="w-8 h-8 mx-auto mb-2 text-slate-600" />
            No transaction records found for this account.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-slate-800 bg-slate-900/40 text-[11px] font-semibold text-slate-400 uppercase tracking-wider">
                  <th className="py-3.5 px-6">Type & Ref</th>
                  <th className="py-3.5 px-6">Description</th>
                  <th className="py-3.5 px-6">Timestamp</th>
                  <th className="py-3.5 px-6 text-right">Amount</th>
                  <th className="py-3.5 px-6 text-right">Balance After</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/60 text-sm">
                {transactions.map(tx => {
                  const isCredit = tx.legType === 'CREDIT';
                  return (
                    <tr key={tx.legId} className="hover:bg-slate-800/20 transition-colors">
                      <td className="py-4 px-6">
                        <div className="flex items-center gap-2.5">
                          <div
                            className={`p-1.5 rounded-lg ${
                              isCredit
                                ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                                : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                            }`}
                          >
                            {isCredit ? (
                              <ArrowDownLeft className="w-4 h-4" />
                            ) : (
                              <ArrowUpRight className="w-4 h-4" />
                            )}
                          </div>
                          <div>
                            <span className="font-bold text-xs text-white block">
                              {tx.transactionType}
                            </span>
                            <span className="text-xs font-mono text-slate-400">
                              {tx.transactionReference}
                            </span>
                          </div>
                        </div>
                      </td>
                      <td className="py-4 px-6 text-slate-300">
                        {tx.description || 'P2P Transfer Settlement'}
                      </td>
                      <td className="py-4 px-6 text-xs font-mono text-slate-400">
                        {new Date(tx.timestamp).toLocaleString()}
                      </td>
                      <td
                        className={`py-4 px-6 text-right font-mono font-bold ${
                          isCredit ? 'text-emerald-400' : 'text-slate-100'
                        }`}
                      >
                        {isCredit ? '+' : '-'}₹{Number(tx.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </td>
                      <td className="py-4 px-6 text-right font-mono font-semibold text-slate-400">
                        ₹{Number(tx.balanceAfter).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Navigation */}
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
