import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { accountApi, transferApi, beneficiaryApi } from '../../api/endpoints';
import { AlertBanner } from '../../components/common/AlertBanner';
import { TransferReceiptResponse } from '../../types';
import { ArrowRightLeft, ShieldCheck, CheckCircle2, RefreshCw } from 'lucide-react';

export const TransferMoneyPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [sourceAccountId, setSourceAccountId] = useState('');
  const [destinationAccountNumber, setDestinationAccountNumber] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID());
  
  const [error, setError] = useState<string | null>(null);
  const [receipt, setReceipt] = useState<TransferReceiptResponse | null>(null);

  const { data: accountsData } = useQuery({
    queryKey: ['my-accounts'],
    queryFn: accountApi.getMyAccounts,
  });

  const { data: beneficiariesData } = useQuery({
    queryKey: ['my-beneficiaries'],
    queryFn: beneficiaryApi.getBeneficiaries,
  });

  const accounts = accountsData?.data || [];
  const beneficiaries = beneficiariesData?.data || [];

  const selectedAccount = accounts.find(a => (a.id || a.accountId) === sourceAccountId) || accounts[0];
  const selectedAccountId = selectedAccount?.id || selectedAccount?.accountId || '';

  const transferMutation = useMutation({
    mutationFn: () =>
      transferApi.executeTransfer(
        {
          sourceAccountId: selectedAccountId,
          destinationAccountNumber,
          amount: Number(amount),
          currency: selectedAccount.currency,
          description,
        },
        idempotencyKey
      ),
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['my-accounts'] });
      queryClient.invalidateQueries({ queryKey: ['recent-history'] });
      setReceipt(res.data);
      // Generate fresh idempotency key for next transfer
      setIdempotencyKey(crypto.randomUUID());
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || 'Transfer execution failed.');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!selectedAccount) {
      setError('Please select a valid source account.');
      return;
    }
    if (Number(amount) <= 0) {
      setError('Transfer amount must be greater than zero.');
      return;
    }
    if (Number(amount) > Number(selectedAccount.balance)) {
      setError('Insufficient available balance in the selected source account.');
      return;
    }
    if (selectedAccount.accountNumber === destinationAccountNumber) {
      setError('Source and destination account numbers cannot be identical.');
      return;
    }

    transferMutation.mutate();
  };

  const handleReset = () => {
    setReceipt(null);
    setAmount('');
    setDescription('');
    setDestinationAccountNumber('');
    setError(null);
    setIdempotencyKey(crypto.randomUUID());
  };

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white">Transfer Funds</h1>
        <p className="text-sm text-slate-400 mt-1">
          Double-entry ledger transfer with distributed idempotency & pessimistic locks
        </p>
      </div>

      {error && (
        <AlertBanner
          type="danger"
          message={error}
          onClose={() => setError(null)}
        />
      )}

      {receipt ? (
        /* Transfer Receipt Modal / Card */
        <div className="glass-card p-8 rounded-2xl border border-emerald-500/30 space-y-6 shadow-glow">
          <div className="flex items-center gap-3 text-emerald-400">
            <CheckCircle2 className="w-8 h-8" />
            <div>
              <h2 className="text-xl font-bold text-white">Transfer Successful</h2>
              <p className="text-xs text-slate-400">Journal entry committed to general ledger</p>
            </div>
          </div>

          <div className="bg-slate-900/80 p-6 rounded-xl border border-slate-800 space-y-3 font-mono text-sm">
            <div className="flex justify-between">
              <span className="text-slate-500">Transaction Reference:</span>
              <span className="text-white font-bold">{receipt.transactionReference}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Amount Transferred:</span>
              <span className="text-emerald-400 font-bold">
                ₹{Number(receipt.amount).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">From Account:</span>
              <span className="text-slate-200">{receipt.sourceAccountNumber}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">To Account:</span>
              <span className="text-slate-200">{receipt.destinationAccountNumber}</span>
            </div>
            <div className="flex justify-between">
              <span className="text-slate-500">Remaining Balance:</span>
              <span className="text-brand-400 font-bold">
                ₹{Number(receipt.sourceRemainingBalance).toLocaleString('en-IN', { minimumFractionDigits: 2 })}
              </span>
            </div>
            <div className="flex justify-between text-xs pt-2 border-t border-slate-800">
              <span className="text-slate-500">Idempotency-Key:</span>
              <span className="text-slate-400 truncate max-w-[200px]">{receipt.idempotencyKey}</span>
            </div>
          </div>

          <button
            onClick={handleReset}
            className="w-full py-3.5 bg-brand-500 hover:bg-brand-400 text-navy-950 font-bold rounded-xl text-sm transition-all shadow-glow flex items-center justify-center gap-2"
          >
            <RefreshCw className="w-4 h-4" /> Initiate Another Transfer
          </button>
        </div>
      ) : (
        /* Transfer Form */
        <div className="glass-card p-8 rounded-2xl border border-slate-800 shadow-2xl">
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Source Account Selector */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                Source Account (Debited)
              </label>
              <select
                value={sourceAccountId || selectedAccountId}
                onChange={e => setSourceAccountId(e.target.value)}
                className="w-full px-4 py-3 bg-slate-900 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:border-brand-500 font-mono"
              >
                {accounts.map(acc => {
                  const accId = acc.id || acc.accountId || '';
                  const balance = Number(acc.availableBalance ?? acc.cachedBalance ?? acc.balance ?? 0);
                  return (
                    <option key={accId} value={accId}>
                      {acc.accountType} • {acc.accountNumber} (Bal: ₹{balance.toLocaleString('en-IN', { minimumFractionDigits: 2 })})
                    </option>
                  );
                })}
              </select>
            </div>

            {/* Quick Beneficiary Selector */}
            {beneficiaries.length > 0 && (
              <div>
                <label className="block text-xs font-semibold text-slate-400 uppercase tracking-wider mb-2">
                  Quick Select Beneficiary
                </label>
                <div className="flex flex-wrap gap-2">
                  {beneficiaries.map(b => (
                    <button
                      key={b.id}
                      type="button"
                      onClick={() => setDestinationAccountNumber(b.destinationAccountNumber)}
                      className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-all ${
                        destinationAccountNumber === b.destinationAccountNumber
                          ? 'bg-brand-500/20 text-brand-400 border-brand-500/40'
                          : 'bg-slate-900 text-slate-400 border-slate-800 hover:border-slate-700'
                      }`}
                    >
                      {b.beneficiaryName} ({b.nickname})
                    </button>
                  ))}
                </div>
              </div>
            )}

            {/* Destination Account Number */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                Destination Account Number (Credited)
              </label>
              <input
                type="text"
                required
                value={destinationAccountNumber}
                onChange={e => setDestinationAccountNumber(e.target.value)}
                placeholder="100000000002"
                className="w-full px-4 py-3 bg-slate-900 border border-slate-700 rounded-xl text-white placeholder-slate-500 text-sm focus:outline-none focus:border-brand-500 font-mono"
              />
            </div>

            {/* Transfer Amount */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                Amount (INR - ₹)
              </label>
              <div className="relative">
                <span className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 font-bold">₹</span>
                <input
                  type="number"
                  step="0.01"
                  required
                  value={amount}
                  onChange={e => setAmount(e.target.value)}
                  placeholder="5000.00"
                  className="w-full pl-8 pr-4 py-3 bg-slate-900 border border-slate-700 rounded-xl text-white placeholder-slate-500 text-base font-bold font-mono focus:outline-none focus:border-brand-500"
                />
              </div>
            </div>

            {/* Description */}
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                Payment Memo / Description
              </label>
              <input
                type="text"
                value={description}
                onChange={e => setDescription(e.target.value)}
                placeholder="e.g. Invoice payment, rent, vendor settlement"
                className="w-full px-4 py-3 bg-slate-900 border border-slate-700 rounded-xl text-white placeholder-slate-500 text-sm focus:outline-none focus:border-brand-500"
              />
            </div>

            {/* Idempotency Footer Indicator */}
            <div className="p-3.5 bg-slate-900/60 rounded-xl border border-slate-800/80 flex items-center justify-between text-xs text-slate-500 font-mono">
              <span className="flex items-center gap-1.5">
                <ShieldCheck className="w-4 h-4 text-brand-400" /> Idempotent Request Guard Active
              </span>
              <span className="truncate max-w-[150px]">{idempotencyKey.slice(0, 8)}...</span>
            </div>

            <button
              type="submit"
              disabled={transferMutation.isPending}
              className="w-full py-4 bg-brand-500 hover:bg-brand-400 disabled:opacity-50 text-navy-950 font-bold rounded-xl text-base transition-all shadow-glow flex items-center justify-center gap-2"
            >
              {transferMutation.isPending ? (
                <div className="w-5 h-5 border-2 border-navy-950 border-t-transparent rounded-full animate-spin"></div>
              ) : (
                <>
                  <ArrowRightLeft className="w-5 h-5" /> Authorize & Transfer Funds
                </>
              )}
            </button>
          </form>
        </div>
      )}
    </div>
  );
};
