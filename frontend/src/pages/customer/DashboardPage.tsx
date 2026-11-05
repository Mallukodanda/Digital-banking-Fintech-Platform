import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { accountApi, transferApi, beneficiaryApi } from '../../api/endpoints';
import { useAuth } from '../../context/AuthContext';
import { StatCard } from '../../components/common/StatCard';
import { Badge } from '../../components/common/Badge';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';
import { Wallet, ArrowRightLeft, Users, ArrowUpRight, ArrowDownLeft, Plus, ShieldCheck } from 'lucide-react';
import { Link } from 'react-router-dom';

export const DashboardPage: React.FC = () => {
  const { user } = useAuth();

  const { data: accountsData, isLoading: accountsLoading } = useQuery({
    queryKey: ['my-accounts'],
    queryFn: accountApi.getMyAccounts,
  });

  const { data: beneficiariesData } = useQuery({
    queryKey: ['my-beneficiaries'],
    queryFn: beneficiaryApi.getBeneficiaries,
  });

  const accounts = accountsData?.data || [];
  const primaryAccount = accounts[0];
  const primaryAccountId = primaryAccount?.id || primaryAccount?.accountId;

  const { data: recentHistoryData, isLoading: historyLoading } = useQuery({
    queryKey: ['recent-history', primaryAccountId],
    queryFn: () => transferApi.getTransactionHistory(primaryAccountId!, 0, 5),
    enabled: !!primaryAccountId,
  });

  const totalBalance = accounts.reduce((sum, acc) => sum + Number(acc.availableBalance ?? acc.cachedBalance ?? acc.balance ?? 0), 0);
  const activeBeneficiaries = beneficiariesData?.data?.length || 0;
  const recentTransactions = recentHistoryData?.data?.content || [];

  return (
    <div className="space-y-8">
      {/* Welcome Banner */}
      <div className="glass-card rounded-2xl p-6 sm:p-8 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-6 border border-slate-800">
        <div>
          <div className="inline-flex items-center gap-2 px-2.5 py-1 rounded-full bg-emerald-500/10 text-emerald-400 text-xs font-semibold mb-2 border border-emerald-500/20">
            <ShieldCheck className="w-3.5 h-3.5" /> Double-Entry Ledger Synchronized
          </div>
          <h1 className="text-2xl sm:text-3xl font-bold text-white">
            Welcome back, {user?.firstName || 'Customer'}!
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Real-time balance across {accounts.length} active banking accounts.
          </p>
        </div>
        <div className="flex gap-3">
          <Link
            to="/transfer"
            className="px-5 py-2.5 bg-brand-500 hover:bg-brand-400 text-navy-950 font-bold rounded-xl text-sm transition-all shadow-glow flex items-center gap-2"
          >
            <ArrowRightLeft className="w-4 h-4" /> Transfer Funds
          </Link>
          <Link
            to="/accounts"
            className="px-4 py-2.5 bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold rounded-xl text-sm transition-all border border-slate-700 flex items-center gap-1.5"
          >
            <Plus className="w-4 h-4" /> New Account
          </Link>
        </div>
      </div>

      {/* KPI Cards Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
        <StatCard
          title="Total Liquidity Balance"
          value={`₹${totalBalance.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`}
          subtitle="Aggregated available balance"
          icon={Wallet}
          variant="brand"
        />
        <StatCard
          title="Active Accounts"
          value={accounts.length}
          subtitle="Checking & Savings Portfolios"
          icon={Wallet}
          variant="navy"
        />
        <StatCard
          title="Saved Beneficiaries"
          value={activeBeneficiaries}
          subtitle="Verified P2P Transfer Endpoints"
          icon={Users}
          variant="amber"
        />
      </div>

      {/* Main Content Grid: Accounts + Recent Transactions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Account Cards */}
        <div className="lg:col-span-1 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-bold text-white">Your Portfolios</h2>
            <Link to="/accounts" className="text-xs font-semibold text-brand-400 hover:underline">
              View All
            </Link>
          </div>

          {accountsLoading ? (
            <LoadingSkeleton rows={3} className="h-24" />
          ) : accounts.length === 0 ? (
            <div className="glass-card p-6 rounded-2xl text-center border border-slate-800">
              <p className="text-sm text-slate-400">No accounts provisioned yet.</p>
              <Link to="/accounts" className="mt-3 inline-block text-xs font-bold text-brand-400">
                + Provision First Account
              </Link>
            </div>
          ) : (
            accounts.map(acc => {
              const accId = acc.id || acc.accountId;
              const balance = Number(acc.availableBalance ?? acc.cachedBalance ?? acc.balance ?? 0);
              return (
                <div
                  key={accId}
                  className="glass-card glass-card-hover p-5 rounded-2xl border border-slate-800 space-y-3"
                >
                  <div className="flex items-center justify-between">
                    <span className="text-xs font-bold text-slate-400 uppercase tracking-wider">
                      {acc.accountType}
                    </span>
                    <Badge variant={acc.status === 'ACTIVE' ? 'success' : 'danger'}>
                      {acc.status}
                    </Badge>
                  </div>
                  <div>
                    <p className="text-xs font-mono text-slate-500">Account #{acc.accountNumber}</p>
                    <p className="text-2xl font-bold font-mono text-white mt-1">
                      ₹{balance.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    </p>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Recent Ledger Activity */}
        <div className="lg:col-span-2 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-bold text-white">Recent Ledger Transactions</h2>
            <Link to="/transactions" className="text-xs font-semibold text-brand-400 hover:underline">
              Full Statement
            </Link>
          </div>

          <div className="glass-card rounded-2xl border border-slate-800 overflow-hidden">
            {historyLoading ? (
              <div className="p-6">
                <LoadingSkeleton rows={4} className="h-12" />
              </div>
            ) : recentTransactions.length === 0 ? (
              <div className="p-12 text-center text-slate-500 text-sm">
                No recent transactions recorded in general ledger.
              </div>
            ) : (
              <div className="divide-y divide-slate-800/60">
                {recentTransactions.map(tx => {
                  const isCredit = tx.legType === 'CREDIT';
                  return (
                    <div
                      key={tx.legId}
                      className="p-4 sm:p-5 flex items-center justify-between hover:bg-slate-800/30 transition-colors"
                    >
                      <div className="flex items-center gap-3.5">
                        <div
                          className={`p-2.5 rounded-xl ${
                            isCredit
                              ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'
                              : 'bg-rose-500/10 text-rose-400 border border-rose-500/20'
                          }`}
                        >
                          {isCredit ? (
                            <ArrowDownLeft className="w-5 h-5" />
                          ) : (
                            <ArrowUpRight className="w-5 h-5" />
                          )}
                        </div>
                        <div>
                          <p className="text-sm font-semibold text-white">
                            {tx.description || tx.transactionType}
                          </p>
                          <p className="text-xs font-mono text-slate-400">{tx.transactionReference}</p>
                        </div>
                      </div>
                      <div className="text-right">
                        <p
                          className={`text-sm font-bold font-mono ${
                            isCredit ? 'text-emerald-400' : 'text-slate-100'
                          }`}
                        >
                          {isCredit ? '+' : '-'}₹{Number(tx.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                        </p>
                        <p className="text-xs font-mono text-slate-500">
                          Bal: ₹{Number(tx.balanceAfter).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
                        </p>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
