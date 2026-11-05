import React from 'react';
import { Link } from 'react-router-dom';
import { Shield, Zap, Lock, Database, ArrowRight, RefreshCw } from 'lucide-react';

export const LandingPage: React.FC = () => {
  return (
    <div className="min-h-screen bg-navy-950 text-slate-100 flex flex-col selection:bg-brand-500 selection:text-black">
      {/* Header */}
      <header className="border-b border-slate-800/80 bg-navy-900/40 backdrop-blur-xl sticky top-0 z-40 px-6 py-4 flex items-center justify-between max-w-7xl mx-auto w-full">
        <div className="flex items-center gap-2.5">
          <div className="w-10 h-10 rounded-xl gradient-brand flex items-center justify-center shadow-glow">
            <Shield className="w-6 h-6 text-navy-950 stroke-[2.5]" />
          </div>
          <span className="font-extrabold text-xl tracking-tight text-white">APEX<span className="text-brand-500">PAY</span></span>
        </div>
        <div className="flex items-center gap-4">
          <Link
            to="/login"
            className="text-sm font-semibold text-slate-300 hover:text-white px-4 py-2 rounded-xl transition-colors"
          >
            Sign In
          </Link>
          <Link
            to="/register"
            className="text-sm font-semibold text-navy-950 bg-brand-500 hover:bg-brand-400 px-5 py-2.5 rounded-xl transition-all shadow-glow hover:scale-105"
          >
            Open Account
          </Link>
        </div>
      </header>

      {/* Hero Section */}
      <section className="flex-1 flex flex-col items-center justify-center text-center px-6 py-20 max-w-5xl mx-auto">
        <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-brand-500/10 border border-brand-500/20 text-brand-400 text-xs font-semibold uppercase tracking-wider mb-8">
          <Zap className="w-3.5 h-3.5" /> High-Performance Double-Entry Core Banking
        </div>
        <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight text-white leading-tight">
          Next-Generation <span className="text-transparent bg-clip-text bg-gradient-to-r from-emerald-400 via-teal-300 to-cyan-400">Digital Banking</span> Infrastructure
        </h1>
        <p className="mt-6 text-lg sm:text-xl text-slate-400 max-w-3xl leading-relaxed">
          Built with deterministic pessimism, strict ACID double-entry general ledger, distributed Redis caching, and real-time Kafka event streaming.
        </p>
        <div className="mt-10 flex flex-col sm:flex-row gap-4 w-full sm:w-auto">
          <Link
            to="/register"
            className="flex items-center justify-center gap-2 text-base font-bold text-navy-950 bg-brand-500 hover:bg-brand-400 px-8 py-3.5 rounded-2xl transition-all shadow-glow hover:scale-105"
          >
            Get Started Free <ArrowRight className="w-5 h-5" />
          </Link>
          <Link
            to="/login"
            className="flex items-center justify-center gap-2 text-base font-semibold text-slate-300 hover:text-white bg-slate-900 border border-slate-700 hover:border-slate-500 px-8 py-3.5 rounded-2xl transition-all"
          >
            Access Portal
          </Link>
        </div>

        {/* Feature Cards Grid */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-20 w-full text-left">
          <div className="glass-card p-6 rounded-2xl border border-slate-800">
            <div className="p-3 w-fit rounded-xl bg-emerald-500/10 text-emerald-400 mb-4 border border-emerald-500/20">
              <Lock className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-white mb-2">Pessimistic Concurrency</h3>
            <p className="text-sm text-slate-400 leading-relaxed">
              Deterministic row-level locking eliminates race conditions, lost updates, and guarantees zero negative balances.
            </p>
          </div>

          <div className="glass-card p-6 rounded-2xl border border-slate-800">
            <div className="p-3 w-fit rounded-xl bg-sky-500/10 text-sky-400 mb-4 border border-sky-500/20">
              <Database className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-white mb-2">Immutable Ledger</h3>
            <p className="text-sm text-slate-400 leading-relaxed">
              Double-entry bookkeeping where every debit strictly matches a credit, verified with append-only audit verification.
            </p>
          </div>

          <div className="glass-card p-6 rounded-2xl border border-slate-800">
            <div className="p-3 w-fit rounded-xl bg-amber-500/10 text-amber-400 mb-4 border border-amber-500/20">
              <RefreshCw className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-white mb-2">Distributed Idempotency</h3>
            <p className="text-sm text-slate-400 leading-relaxed">
              UUID request fingerprinting and atomic distributed locks completely protect against double-debit network timeouts.
            </p>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-slate-800 py-8 px-6 text-center text-xs text-slate-500">
        <p>© 2026 ApexPay Platform • Spring Boot 3.2.4 • PostgreSQL 16 • Apache Kafka • Redis 7</p>
      </footer>
    </div>
  );
};
