import { useEffect, useState } from 'react';
import { api } from '../api';
import { label, money, pct } from '../format';
import { Dashboard, Profile } from '../types';
import { CategoryBars } from './Dashboard';

interface Loan { id: string; loanType: string; lender?: string; emi: number; remainingBalance: number; annualRate: number }
interface Goal { id: string; name: string; targetAmount: number; savedAmount: number; targetDate: string; requiredMonthly: number; reached: boolean }

export default function Report() {
  const [d, setD] = useState<Dashboard | null>(null);
  const [profile, setProfile] = useState<Profile | null>(null);
  const [loans, setLoans] = useState<Loan[]>([]);
  const [goals, setGoals] = useState<Goal[]>([]);
  const [error, setError] = useState('');

  useEffect(() => {
    Promise.all([api<Dashboard>('/dashboard'), api<Profile>('/profile'), api<Loan[]>('/debts'), api<Goal[]>('/goals')])
      .then(([dash, p, l, g]) => { setD(dash); setProfile(p); setLoans(l); setGoals(g); })
      .catch((e) => setError(e instanceof Error ? e.message : 'Could not build the report'));
  }, []);

  if (error) return <p className="error">{error}</p>;
  if (!d || !profile) return <p className="muted">Loading…</p>;
  const s = d.summary;

  return (
    <section className="report">
      <div className="row no-print">
        <h1>Financial report</h1>
        <button onClick={() => window.print()}>Print or save as PDF</button>
      </div>
      <p className="muted">Generated {new Date().toLocaleDateString('en-IN', { dateStyle: 'long' })} · {profile.taxRegime === 'NEW' ? 'New' : 'Old'} tax regime</p>

      <h2>Summary</h2>
      <table>
        <tbody>
          <tr><td>Gross salary</td><td>{money(s.monthlyGross)} / month</td></tr>
          <tr><td>Estimated tax</td><td>{money(s.monthlyTax)} / month</td></tr>
          <tr><td>Take-home pay</td><td>{money(s.monthlyNet)} / month</td></tr>
          <tr><td>Living expenses</td><td>{money(s.monthlyExpenses)} / month</td></tr>
          <tr><td>EMIs</td><td>{money(s.monthlyEmis)} / month ({pct(s.emiToIncomePct)} of take-home)</td></tr>
          <tr><td>Savings to set aside</td><td>{money(s.monthlySavingsTarget)} / month</td></tr>
          <tr><td>Left after everything</td><td>{money(s.freeCashAfterSavings)} / month</td></tr>
          <tr><td>Required gross salary</td><td>{money(s.requiredGross)} / month</td></tr>
          <tr><td>Largest new EMI you can take</td><td>{money(s.affordability.maxNewEmi)} / month</td></tr>
          <tr><td>Financial health</td><td>{s.health.score} / 100 ({s.health.grade})</td></tr>
        </tbody>
      </table>

      <h2>Recommendations</h2>
      <ul>{s.health.recommendations.map((r) => <li key={r}>{r}</li>)}</ul>

      <h2>Expenses by category</h2>
      <CategoryBars data={d.expensesByCategory} />

      <h2>Loans</h2>
      {loans.length === 0 ? <p className="muted">No loans recorded.</p> : (
        <table>
          <thead><tr><th>Loan</th><th>Rate</th><th>EMI</th><th>Balance</th></tr></thead>
          <tbody>{loans.map((l) => (
            <tr key={l.id}><td>{label(l.loanType)}{l.lender ? ` (${l.lender})` : ''}</td><td>{l.annualRate}%</td><td>{money(l.emi)}</td><td>{money(l.remainingBalance)}</td></tr>
          ))}</tbody>
        </table>
      )}

      <h2>Savings goals</h2>
      {goals.length === 0 ? <p className="muted">No goals recorded.</p> : (
        <table>
          <thead><tr><th>Goal</th><th>Saved</th><th>Target</th><th>Due</th><th>Per month</th></tr></thead>
          <tbody>{goals.map((g) => (
            <tr key={g.id}><td>{g.name}</td><td>{money(g.savedAmount)}</td><td>{money(g.targetAmount)}</td><td>{g.targetDate}</td><td>{g.reached ? 'Reached' : money(g.requiredMonthly)}</td></tr>
          ))}</tbody>
        </table>
      )}
      <p className="muted">Estimates only. Tax uses FY 2026-27 slabs without surcharge, HRA or other exemptions. Not financial or tax advice.</p>
    </section>
  );
}
