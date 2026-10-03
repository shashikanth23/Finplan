import { useEffect, useState } from 'react';
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
          <div className="bar"><span style={{ width: `${(v / max) * 100}%` }} /></div>
          <span>{money(v)}</span>
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

  return (
    <section>
      <h1>Dashboard</h1>
      {noIncome && <p className="card notice">Add your income first. Everything below is calculated from your income, expenses, loans and goals.</p>}
      {d.source === 'local-fallback' && <p className="muted">Showing figures calculated locally while the planner service recovers.</p>}

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
          <h2>Financial health</h2>
          <div className="score">
            <strong>{s.health.score}</strong><span>/ 100 · {s.health.grade}</span>
          </div>
          <ul>{s.health.recommendations.map((r) => <li key={r}>{r}</li>)}</ul>
        </div>
        <div className="card">
          <h2>Where your money goes</h2>
          <CategoryBars data={d.expensesByCategory} />
          <p className="muted">Total loan balance outstanding: {money(d.totalDebtRemaining)}</p>
        </div>
      </div>
    </section>
  );
}
