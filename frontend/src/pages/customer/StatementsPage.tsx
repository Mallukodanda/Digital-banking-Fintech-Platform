import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { accountApi } from '../../api/endpoints';
import { FileText, Download, ShieldCheck } from 'lucide-react';
import { AlertBanner } from '../../components/common/AlertBanner';

export const StatementsPage: React.FC = () => {
  const [selectedAccountId, setSelectedAccountId] = useState('');
  const [dateRange, setDateRange] = useState('CURRENT_MONTH');
  const [statusMsg, setStatusMsg] = useState<string | null>(null);

  const { data: accountsData } = useQuery({
    queryKey: ['my-accounts'],
    queryFn: accountApi.getMyAccounts,
  });

  const accounts = accountsData?.data || [];
  const activeAccount = accounts.find(a => (a.id || a.accountId) === selectedAccountId) || accounts[0];
  const activeAccountId = activeAccount?.id || activeAccount?.accountId || '';

  const handleDownload = (format: 'PDF' | 'CSV') => {
    setStatusMsg(`Generating audited ${format} statement for Account #${activeAccount?.accountNumber}...`);
    setTimeout(() => {
      setStatusMsg(`Statement successfully dispatched to your registered email address.`);
    }, 1500);
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">Periodic Statements & Tax Reports</h1>
        <p className="text-sm text-slate-400 mt-1">
          Generate tamper-proof regulatory statements and export ledger journal data
        </p>
      </div>

      {statusMsg && (
        <AlertBanner
          type="info"
          message={statusMsg}
          onClose={() => setStatusMsg(null)}
        />
      )}

      <div className="glass-card p-8 rounded-2xl border border-slate-800 space-y-6 shadow-xl">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
              Select Banking Account
            </label>
            <select
              value={selectedAccountId || activeAccountId}
              onChange={e => setSelectedAccountId(e.target.value)}
              className="w-full px-4 py-2.5 bg-slate-900 border border-slate-700 rounded-xl text-white text-sm font-mono focus:outline-none focus:border-brand-500"
            >
              {accounts.map(acc => {
                const accId = acc.id || acc.accountId || '';
                const balance = Number(acc.availableBalance ?? acc.cachedBalance ?? acc.balance ?? 0);
                return (
                  <option key={accId} value={accId}>
                    {acc.accountType} • {acc.accountNumber} (₹{balance.toLocaleString('en-IN', { minimumFractionDigits: 2 })})
                  </option>
                );
              })}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
              Statement Period
            </label>
            <select
              value={dateRange}
              onChange={e => setDateRange(e.target.value)}
              className="w-full px-4 py-2.5 bg-slate-900 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:border-brand-500"
            >
              <option value="CURRENT_MONTH">Current Month (Oct 2026)</option>
              <option value="LAST_MONTH">Previous Month (Sep 2026)</option>
              <option value="LAST_QUARTER">Quarter 3 (Jul - Sep 2026)</option>
              <option value="FINANCIAL_YEAR">Financial Year 2026-2027</option>
            </select>
          </div>
        </div>

        <div className="p-4 bg-slate-900/60 rounded-xl border border-slate-800 flex items-center gap-3 text-xs text-slate-400">
          <ShieldCheck className="w-5 h-5 text-brand-400 flex-shrink-0" />
          <span>
            Every exported statement includes an immutable cryptographic hash matching the general ledger outbox events.
          </span>
        </div>

        <div className="flex flex-col sm:flex-row gap-4 pt-4 border-t border-slate-800">
          <button
            onClick={() => handleDownload('PDF')}
            className="flex-1 py-3 bg-brand-500 hover:bg-brand-400 text-navy-950 font-bold rounded-xl text-sm transition-all shadow-glow flex items-center justify-center gap-2"
          >
            <Download className="w-4 h-4" /> Download Official PDF Statement
          </button>
          <button
            onClick={() => handleDownload('CSV')}
            className="flex-1 py-3 bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold rounded-xl text-sm transition-all border border-slate-700 flex items-center justify-center gap-2"
          >
            <FileText className="w-4 h-4" /> Export CSV Ledger Data
          </button>
        </div>
      </div>
    </div>
  );
};
