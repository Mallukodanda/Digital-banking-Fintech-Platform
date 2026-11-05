import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { beneficiaryApi } from '../../api/endpoints';
import { AlertBanner } from '../../components/common/AlertBanner';
import { LoadingSkeleton } from '../../components/common/LoadingSkeleton';
import { Badge } from '../../components/common/Badge';
import { Users, UserPlus, CheckCircle, Trash2, Search } from 'lucide-react';

export const BeneficiariesPage: React.FC = () => {
  const queryClient = useQueryClient();
  const [accountNumber, setAccountNumber] = useState('');
  const [nickname, setNickname] = useState('');
  const [verifiedName, setVerifiedName] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const { data: beneficiariesData, isLoading } = useQuery({
    queryKey: ['my-beneficiaries'],
    queryFn: beneficiaryApi.getBeneficiaries,
  });

  const verifyMutation = useMutation({
    mutationFn: beneficiaryApi.verifyBeneficiary,
    onSuccess: (res) => {
      setVerifiedName(res.data.accountHolderName);
      setError(null);
    },
    onError: (err: any) => {
      setVerifiedName(null);
      setError(err.response?.data?.message || 'Beneficiary account number not found.');
    },
  });

  const addMutation = useMutation({
    mutationFn: beneficiaryApi.addBeneficiary,
    onSuccess: (res) => {
      queryClient.invalidateQueries({ queryKey: ['my-beneficiaries'] });
      setSuccess(`Beneficiary "${res.data.beneficiaryName}" added successfully!`);
      setAccountNumber('');
      setNickname('');
      setVerifiedName(null);
    },
    onError: (err: any) => {
      setError(err.response?.data?.message || 'Failed to add beneficiary.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: beneficiaryApi.deleteBeneficiary,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['my-beneficiaries'] });
      setSuccess('Beneficiary removed.');
    },
  });

  const beneficiaries = beneficiariesData?.data || [];

  const handleVerify = (e: React.FormEvent) => {
    e.preventDefault();
    if (!accountNumber) return;
    verifyMutation.mutate(accountNumber);
  };

  const handleAdd = (e: React.FormEvent) => {
    e.preventDefault();
    if (!accountNumber || !nickname) return;
    addMutation.mutate({ destinationAccountNumber: accountNumber, nickname });
  };

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-white">Beneficiary Directory</h1>
        <p className="text-sm text-slate-400 mt-1">
          Verify and store trusted destination accounts with Cache-Aside invalidation
        </p>
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

      {/* Add & Verify Beneficiary Card */}
      <div className="glass-card p-6 sm:p-8 rounded-2xl border border-slate-800 shadow-xl space-y-6">
        <div className="flex items-center gap-2 text-white font-bold text-base">
          <UserPlus className="w-5 h-5 text-brand-400" /> Add New Beneficiary
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Step 1: Verification */}
          <form onSubmit={handleVerify} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                1. Destination Account Number
              </label>
              <div className="flex gap-2">
                <input
                  type="text"
                  required
                  value={accountNumber}
                  onChange={e => {
                    setAccountNumber(e.target.value);
                    setVerifiedName(null);
                  }}
                  placeholder="e.g. 100000000002"
                  className="flex-1 px-4 py-2.5 bg-slate-900 border border-slate-700 rounded-xl text-white text-sm font-mono focus:outline-none focus:border-brand-500"
                />
                <button
                  type="submit"
                  disabled={verifyMutation.isPending || !accountNumber}
                  className="px-4 py-2.5 bg-slate-800 hover:bg-slate-700 disabled:opacity-50 text-slate-200 font-semibold text-xs rounded-xl border border-slate-700 flex items-center gap-1.5"
                >
                  <Search className="w-3.5 h-3.5" />
                  {verifyMutation.isPending ? 'Verifying...' : 'Verify'}
                </button>
              </div>
            </div>

            {verifiedName && (
              <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 rounded-xl flex items-center gap-2 text-xs text-emerald-400">
                <CheckCircle className="w-4 h-4" />
                <span>Verified Account Holder: <strong className="text-white">{verifiedName}</strong></span>
              </div>
            )}
          </form>

          {/* Step 2: Nickname & Confirmation */}
          <form onSubmit={handleAdd} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-2">
                2. Beneficiary Nickname / Label
              </label>
              <input
                type="text"
                required
                disabled={!verifiedName}
                value={nickname}
                onChange={e => setNickname(e.target.value)}
                placeholder="e.g. Mom, Landlord, Vendor HQ"
                className="w-full px-4 py-2.5 bg-slate-900 border border-slate-700 rounded-xl text-white text-sm focus:outline-none focus:border-brand-500 disabled:opacity-50"
              />
            </div>

            <button
              type="submit"
              disabled={addMutation.isPending || !verifiedName || !nickname}
              className="w-full py-2.5 bg-brand-500 hover:bg-brand-400 disabled:opacity-40 text-navy-950 font-bold rounded-xl text-sm transition-all shadow-glow flex items-center justify-center gap-2"
            >
              {addMutation.isPending ? 'Saving...' : 'Save Beneficiary'}
            </button>
          </form>
        </div>
      </div>

      {/* Beneficiaries Table */}
      <div className="space-y-4">
        <h2 className="text-base font-bold text-white">Active Beneficiary Endpoints</h2>
        <div className="glass-card rounded-2xl border border-slate-800 overflow-hidden">
          {isLoading ? (
            <div className="p-6">
              <LoadingSkeleton rows={4} className="h-12" />
            </div>
          ) : beneficiaries.length === 0 ? (
            <div className="p-12 text-center text-slate-500 text-sm">
              <Users className="w-8 h-8 mx-auto mb-2 text-slate-600" />
              No beneficiaries saved yet. Verify an account above to add.
            </div>
          ) : (
            <div className="divide-y divide-slate-800/60">
              {beneficiaries.map(b => (
                <div
                  key={b.id}
                  className="p-4 sm:p-5 flex items-center justify-between hover:bg-slate-800/30 transition-colors"
                >
                  <div className="flex items-center gap-3.5">
                    <div className="p-2.5 rounded-xl bg-slate-800 text-slate-300 border border-slate-700">
                      <Users className="w-5 h-5" />
                    </div>
                    <div>
                      <p className="text-sm font-bold text-white">
                        {b.beneficiaryName}{' '}
                        <span className="text-xs font-normal text-slate-400">({b.nickname})</span>
                      </p>
                      <p className="text-xs font-mono text-slate-400">{b.destinationAccountNumber}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-4">
                    <Badge variant={b.status === 'ACTIVE' ? 'success' : 'neutral'}>
                      {b.status}
                    </Badge>
                    <button
                      onClick={() => deleteMutation.mutate(b.id)}
                      className="p-2 text-slate-500 hover:text-rose-400 hover:bg-rose-500/10 rounded-lg transition-colors"
                      title="Delete Beneficiary"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
