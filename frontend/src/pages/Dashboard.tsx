import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { label, money, pct } from '../format';
import { Dashboard as DashboardData } from '../types';

function Stat({ title, value, note, tone }: { title: string; value: string; note?: string; tone?: 'good' | 'bad' }) {
  return (
    <div className={`card stat ${tone ?? ''}`}>
      <span className="muted">{title}</span>
      <strong>{value}</strong>
      {note && <small className="muted">{note}</small>}
    </div>
  );
}

export function CategoryBars({ data }: { data: Record<string, number> }) {
  const entries = Object.entries(data).sort((a, b) => b[1] - a[1]);
  const max = Math.max(1, ...entries.map(([, v]) => v));
  if (entries.length === 0) return <p className="muted">No expenses added yet.</p>;
  return (
    <div className="bars">
      {entries.map(([k, v]) => (
        <div key={k} className="bar-row">
          <span>{label(k)}</span>
          <div className="bar"><span style={{ width: `${Math.max(0, (v / max) * 100)}%` }} /></div>
          <strong>{money(v)}</strong>
        </div>
      ))}
    </div>
  );
}

export default function Dashboard() {
  const [d, setD] = useState<DashboardData | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api<DashboardData>('/dashboard').then(setD).catch((e) => setError(e instanceof Error ? e.message : 'Could not load dashboard'));
  }, []);

  if (error) return <p className="error">{error}</p>;
  if (!d) return <p className="muted">Loading…</p>;
  const s = d.summary;
  const noIncome = s.monthlyGross === 0;
  const healthProgress = Math.min(100, Math.max(0, s.health.score));

  return (
    <section>
      <div className="dashboard-heading">
        <div>
          <p className="eyebrow">YOUR OVERVIEW</p>
          <h1>Your finances, at a glance.</h1>
          <p className="page-intro">A clearer view of where you are — and what you can do next.</p>
        </div>
        <Link className="button-link" to="/report"><span aria-hidden="true">▥</span> View report</Link>
      </div>
      {noIncome && <div className="notice"><span aria-hidden="true">✦</span><p><strong>Start with your income</strong><br />Add your income to see a personalized view of your expenses, loans, and savings goals.</p><Link to="/income">Add income <span aria-hidden="true">→</span></Link></div>}
      {d.source === 'local-fallback' && <p className="card notice">Showing figures calculated locally while the planner service recovers.</p>}

      <div className="dashboard-feature">
        <div className="feature-main">
          <span className="feature-label"><span className="status-dot" /> MONTHLY SNAPSHOT</span>
          <p className="feature-title">Take-home pay</p>
          <strong className="feature-amount">{money(s.monthlyNet)}</strong>
          <p className="feature-note">Your estimated monthly income after tax</p>
          <div className="feature-breakdown">
            <div><span>Gross income</span><strong>{money(s.monthlyGross)}</strong></div>
            <div><span>Estimated tax</span><strong>{money(s.monthlyTax)}</strong></div>
          </div>
        </div>
        <div className="feature-health">
          <div className="health-heading"><span>FINANCIAL HEALTH</span><span className="health-grade">{s.health.grade}</span></div>
          <div className="health-score">{s.health.score}<span> / 100</span></div>
          <div className="health-track"><span style={{ width: `${healthProgress}%` }} /></div>
          <p>{s.health.recommendations[0] ?? 'Keep tracking your finances to build a stronger financial picture.'}</p>
          <Link to="/report">See your full report <span aria-hidden="true">→</span></Link>
        </div>
      </div>

      <div className="stats">
        <Stat title="Current salary (gross)" value={money(s.monthlyGross)} note={`Take-home ${money(s.monthlyNet)} after ${money(s.monthlyTax)} tax`} />
        <Stat title="Monthly expenses" value={money(s.monthlyExpenses)} />
        <Stat title="EMI burden" value={money(s.monthlyEmis)} note={`${pct(s.emiToIncomePct)} of take-home`} tone={s.emiToIncomePct > 40 ? 'bad' : undefined} />
        <Stat title="Savings to set aside" value={money(s.monthlySavingsTarget)} note={`${d.activeGoals} active goal${d.activeGoals === 1 ? '' : 's'}`} />
        <Stat title="Disposable income" value={money(s.disposableIncome)} note="After expenses and EMIs" />
        <Stat title="Left after savings" value={money(s.freeCashAfterSavings)} tone={s.freeCashAfterSavings < 0 ? 'bad' : 'good'} />
        <Stat title="Required salary (gross)" value={money(s.requiredGross)}
          note={s.grossGap > 0 ? `${money(s.grossGap)} above your current pay` : 'Your pay covers this'} tone={s.grossGap > 0 ? 'bad' : 'good'} />
        <Stat title="Max new EMI" value={money(s.affordability.maxNewEmi)} note={s.affordability.limitedBy === 'FOIR' ? 'Capped at 40% of take-home' : 'Limited by your monthly surplus'} />
      </div>

      <div className="two">
        <div className="card">
          <div className="card-heading">
            <div><p className="eyebrow">PERSONALIZED INSIGHTS</p><h2>Your next best steps</h2></div>
            <Link className="subtle-link" to="/report">Full report <span aria-hidden="true">→</span></Link>
          </div>
          {s.health.recommendations.length > 0 && (
            <ul className="recommendations">
              {s.health.recommendations.map((recommendation) => (
                <li key={recommendation}>{recommendation}</li>
              ))}
            </ul>
          )}
          {s.health.recommendations.length === 0 && <p className="muted">Your finances are looking balanced. Keep your entries up to date to get relevant recommendations.</p>}
        </div>
        <Link to="/expenses" className="card spending-card">
          <div className="card-heading">
            <div><p className="eyebrow">SPENDING BREAKDOWN</p><h2>Where your money goes</h2></div>
            <span className="card-arrow" aria-hidden="true">↗</span>
          </div>
          <CategoryBars data={d.expensesByCategory} />
          <div className="spending-footer"><span>Total loan balance</span><strong>{money(d.totalDebtRemaining)}</strong></div>
        </Link>
      </div>
      <div className="quick-actions">
        <div><p className="eyebrow">KEEP THINGS UP TO DATE</p><h2>Quick actions</h2></div>
        <Link to="/income"><span className="quick-icon income-icon">↗</span><span><strong>Update income</strong><small>Keep your take-home estimate current</small></span><span className="quick-arrow">→</span></Link>
        <Link to="/expenses"><span className="quick-icon expense-icon">↘</span><span><strong>Track an expense</strong><small>See where your money is going</small></span><span className="quick-arrow">→</span></Link>
        <Link to="/goals"><span className="quick-icon goals-icon">◎</span><span><strong>Set a savings goal</strong><small>Turn your plans into monthly steps</small></span><span className="quick-arrow">→</span></Link>
      </div>
    </section>
  );
}
