import { FormEvent, ReactNode, useEffect, useState } from 'react';
import { api } from '../api';
import { money, pct } from '../format';
import { Dashboard, Profile, ScenarioResult, ScheduleRow } from '../types';

interface Base {
  gross: number; net: number; expenses: number; emis: number; savings: number;
  regime: Profile['taxRegime']; fixedDeductions: number; oldDeductions: number;
}

const ZERO_BASE: Base = { gross: 0, net: 0, expenses: 0, emis: 0, savings: 0, regime: 'NEW', fixedDeductions: 0, oldDeductions: 0 };

function Num({ label, value, onChange, step = 'any' }: { label: string; value: string; onChange: (v: string) => void; step?: string }) {
  return (
    <label>{label}
      <input type="number" step={step} value={value} onChange={(e) => onChange(e.target.value)} required />
    </label>
  );
}

function Panel({ title, question, children }: { title: string; question: string; children: ReactNode }) {
  return (
    <div className="card">
      <h2>{title}</h2>
      <p className="muted">{question}</p>
      {children}
    </div>
  );
}

const n = (s: string): number => Number(s) || 0;

function RequiredSalary({ base }: { base: Base }) {
  const [expenses, setExpenses] = useState('0');
  const [emis, setEmis] = useState('0');
  const [savings, setSavings] = useState('0');
  const [deductions, setDeductions] = useState('0');
  const [regime, setRegime] = useState<Profile['taxRegime']>('NEW');
  const [result, setResult] = useState<{ requiredMonthlyGross: number; breakdown: { monthlyTax: number; monthlyNet: number } } | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setExpenses(String(base.expenses)); setEmis(String(base.emis)); setSavings(String(base.savings));
    setDeductions(String(base.fixedDeductions)); setRegime(base.regime);
  }, [base]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      setResult(await api('/planner/salary/required', { method: 'POST', body: {
        targetNetMonthly: n(expenses) + n(emis) + n(savings),
        salary: { regime, monthlyFixedDeductions: n(deductions), annualOldRegimeDeductions: base.oldDeductions },
      } }));
    } catch (err) { setError(err instanceof Error ? err.message : 'Calculation failed'); }
  }

  return (
    <Panel title="What salary do I need?" question="Gross pay needed to cover expenses, EMIs and savings after tax.">
      <form onSubmit={submit}>
        <div className="grid">
          <Num label="Monthly expenses (₹)" value={expenses} onChange={setExpenses} />
          <Num label="Monthly EMIs (₹)" value={emis} onChange={setEmis} />
          <Num label="Monthly savings (₹)" value={savings} onChange={setSavings} />
          <Num label="PF and other deductions (₹)" value={deductions} onChange={setDeductions} />
          <label>Tax regime
            <select value={regime} onChange={(e) => setRegime(e.target.value as Profile['taxRegime'])}>
              <option value="NEW">New</option><option value="OLD">Old</option>
            </select>
          </label>
        </div>
        <button type="submit">Calculate</button>
      </form>
      {error && <p className="error">{error}</p>}
      {result && (
        <p className="result">
          You need a gross of <strong>{money(result.requiredMonthlyGross)}</strong> a month ({money(result.requiredMonthlyGross * 12)} a year).
          Tax is about {money(result.breakdown.monthlyTax)} a month, leaving {money(result.breakdown.monthlyNet)} take-home.
        </p>
      )}
    </Panel>
  );
}

function AffordableEmi({ base }: { base: Base }) {
  const [net, setNet] = useState('0');
  const [expenses, setExpenses] = useState('0');
  const [emis, setEmis] = useState('0');
  const [savings, setSavings] = useState('0');
  const [rate, setRate] = useState('9');
  const [months, setMonths] = useState('240');
  const [result, setResult] = useState<{ maxNewEmi: number; limitedBy: string; loan: number } | null>(null);
  const [error, setError] = useState('');

  useEffect(() => {
    setNet(String(base.net)); setExpenses(String(base.expenses)); setEmis(String(base.emis)); setSavings(String(base.savings));
  }, [base]);

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      const a = await api<{ maxNewEmi: number; limitedBy: string }>('/planner/affordability', { method: 'POST', body: {
        netMonthly: n(net), expenses: n(expenses), existingEmis: n(emis), savings: n(savings) } });
      let loan = 0;
      if (a.maxNewEmi > 0) {
        loan = (await api<{ principal: number }>('/planner/emi/affordable-loan', { method: 'POST', body: {
          emi: a.maxNewEmi, annualRatePct: n(rate), months: n(months) } })).principal;
      }
      setResult({ maxNewEmi: a.maxNewEmi, limitedBy: a.limitedBy, loan });
    } catch (err) { setError(err instanceof Error ? err.message : 'Calculation failed'); }
  }

  return (
    <Panel title="What EMI can I afford?" question="The smaller of your monthly surplus and the 40%-of-take-home lender guideline.">
      <form onSubmit={submit}>
        <div className="grid">
          <Num label="Take-home (₹)" value={net} onChange={setNet} />
          <Num label="Expenses (₹)" value={expenses} onChange={setExpenses} />
          <Num label="Existing EMIs (₹)" value={emis} onChange={setEmis} />
          <Num label="Savings (₹)" value={savings} onChange={setSavings} />
          <Num label="Loan rate (% per year)" value={rate} onChange={setRate} step="0.01" />
          <Num label="Loan tenure (months)" value={months} onChange={setMonths} step="1" />
        </div>
        <button type="submit">Calculate</button>
      </form>
      {error && <p className="error">{error}</p>}
      {result && (
        <p className="result">
          {result.maxNewEmi > 0
            ? <>You can afford a new EMI of up to <strong>{money(result.maxNewEmi)}</strong> ({result.limitedBy === 'FOIR' ? 'capped by the 40% guideline' : 'limited by your surplus'}), which supports a loan of about <strong>{money(result.loan)}</strong>.</>
            : <>You have no room for a new EMI right now. Reduce expenses or savings targets first.</>}
        </p>
      )}
    </Panel>
  );
}

function Scenario({ base }: { base: Base }) {
  const [grossPct, setGrossPct] = useState('0');
  const [extraEmi, setExtraEmi] = useState('0');
  const [expensePct, setExpensePct] = useState('0');
  const [result, setResult] = useState<ScenarioResult | null>(null);
  const [error, setError] = useState('');

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      setResult(await api<ScenarioResult>('/planner/scenario', { method: 'POST', body: {
        base: {
          monthlyGross: base.gross,
          salary: { regime: base.regime, monthlyFixedDeductions: base.fixedDeductions, annualOldRegimeDeductions: base.oldDeductions },
          monthlyExpenses: base.expenses, monthlyEmis: base.emis, monthlySavingsTarget: base.savings,
        },
        scenario: { grossChangePct: n(grossPct), extraMonthlyEmi: n(extraEmi), expenseChangePct: n(expensePct) },
      } }));
    } catch (err) { setError(err instanceof Error ? err.message : 'Simulation failed'); }
  }

  const row = (name: string, a: number, b: number, fmt: (x: number) => string = money) => (
    <tr key={name}><td>{name}</td><td>{fmt(a)}</td><td>{fmt(b)}</td></tr>
  );

  return (
    <Panel title="What happens if things change?" question="Try a raise or pay cut, a new loan, or higher living costs against your current numbers.">
      <form onSubmit={submit}>
        <div className="grid">
          <Num label="Salary change (%)" value={grossPct} onChange={setGrossPct} />
          <Num label="New EMI per month (₹)" value={extraEmi} onChange={setExtraEmi} />
          <Num label="Expense change (%)" value={expensePct} onChange={setExpensePct} />
        </div>
        <button type="submit" disabled={base.gross === 0}>Simulate</button>
        {base.gross === 0 && <span className="muted"> Add income first.</span>}
      </form>
      {error && <p className="error">{error}</p>}
      {result && (
        <div className="scroll">
          <table>
            <thead><tr><th /><th>Now</th><th>After change</th></tr></thead>
            <tbody>
              {row('Gross salary', result.base.monthlyGross, result.scenario.monthlyGross)}
              {row('Take-home', result.base.monthlyNet, result.scenario.monthlyNet)}
              {row('Left after savings', result.base.freeCashAfterSavings, result.scenario.freeCashAfterSavings)}
              {row('Required gross salary', result.base.requiredGross, result.scenario.requiredGross)}
              {row('EMI share of take-home', result.base.emiToIncomePct, result.scenario.emiToIncomePct, pct)}
              {row('Health score', result.base.health.score, result.scenario.health.score, (x) => String(x))}
            </tbody>
          </table>
        </div>
      )}
    </Panel>
  );
}

function EmiSchedule() {
  const [principal, setPrincipal] = useState('1000000');
  const [rate, setRate] = useState('8.5');
  const [months, setMonths] = useState('240');
  const [result, setResult] = useState<{ emi: number; totalPayable: number; totalInterest: number; schedule: ScheduleRow[] } | null>(null);
  const [error, setError] = useState('');

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError('');
    try {
      setResult(await api('/planner/emi', { method: 'POST', body: {
        principal: n(principal), annualRatePct: n(rate), months: n(months), includeSchedule: true } }));
    } catch (err) { setError(err instanceof Error ? err.message : 'Calculation failed'); }
  }

  return (
    <Panel title="EMI calculator" question="EMI, total interest and the month-by-month repayment schedule for any loan.">
      <form onSubmit={submit}>
        <div className="grid">
          <Num label="Loan amount (₹)" value={principal} onChange={setPrincipal} />
          <Num label="Rate (% per year)" value={rate} onChange={setRate} step="0.01" />
          <Num label="Tenure (months)" value={months} onChange={setMonths} step="1" />
        </div>
        <button type="submit">Calculate</button>
      </form>
      {error && <p className="error">{error}</p>}
      {result && (
        <>
          <p className="result">
            EMI <strong>{money(result.emi)}</strong> a month. You repay {money(result.totalPayable)} in total, of which {money(result.totalInterest)} is interest.
          </p>
          <div className="scroll">
            <table>
              <thead><tr><th>Month</th><th>Payment</th><th>Interest</th><th>Principal</th><th>Balance</th></tr></thead>
              <tbody>
                {result.schedule.slice(0, 12).map((r) => (
                  <tr key={r.month}><td>{r.month}</td><td>{money(r.payment)}</td><td>{money(r.interest)}</td><td>{money(r.principal)}</td><td>{money(r.balance)}</td></tr>
                ))}
              </tbody>
            </table>
          </div>
          {result.schedule.length > 12 && <p className="muted">First 12 of {result.schedule.length} months shown.</p>}
        </>
      )}
    </Panel>
  );
}

export default function Calculator() {
  const [base, setBase] = useState<Base>(ZERO_BASE);

  useEffect(() => {
    Promise.all([api<Dashboard>('/dashboard'), api<Profile>('/profile')])
      .then(([d, p]) => setBase({
        gross: d.summary.monthlyGross, net: d.summary.monthlyNet, expenses: d.summary.monthlyExpenses,
        emis: d.summary.monthlyEmis, savings: d.summary.monthlySavingsTarget,
        regime: p.taxRegime, fixedDeductions: p.monthlyFixedDeductions, oldDeductions: p.annualOldRegimeDeductions,
      }))
      .catch(() => { /* calculators still work with manual input */ });
  }, []);

  return (
    <section>
      <h1>Salary calculator</h1>
      <p className="muted">Fields start with your saved numbers; change any of them to explore.</p>
      <RequiredSalary base={base} />
      <AffordableEmi base={base} />
      <Scenario base={base} />
      <EmiSchedule />
    </section>
  );
}
