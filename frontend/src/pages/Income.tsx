import { FormEvent, useEffect, useState } from 'react';
import { api } from '../api';
import ResourcePage from '../components/ResourcePage';
import { label, money } from '../format';
import { Profile } from '../types';

function TaxSettings() {
  const [p, setP] = useState<Profile | null>(null);
  const [msg, setMsg] = useState('');

  useEffect(() => { api<Profile>('/profile').then(setP).catch(() => setMsg('Could not load tax settings')); }, []);

  async function save(e: FormEvent) {
    e.preventDefault();
    if (!p) return;
    try {
      setP(await api<Profile>('/profile', { method: 'PUT', body: p }));
      setMsg('Saved');
    } catch (err) {
      setMsg(err instanceof Error ? err.message : 'Save failed');
    }
  }

  if (!p) return null;
  return (
    <form className="card form" onSubmit={save}>
      <h2>Tax and deductions</h2>
      <div className="grid">
        <label>Tax regime
          <select value={p.taxRegime} onChange={(e) => setP({ ...p, taxRegime: e.target.value as Profile['taxRegime'] })}>
            <option value="NEW">New regime</option>
            <option value="OLD">Old regime</option>
          </select>
        </label>
        <label>Monthly PF and other deductions
          <input type="number" min={0} value={p.monthlyFixedDeductions} onChange={(e) => setP({ ...p, monthlyFixedDeductions: Number(e.target.value) })} />
        </label>
        {p.taxRegime === 'OLD' && (
          <label>Yearly 80C / 80D etc. claimed
            <input type="number" min={0} value={p.annualOldRegimeDeductions} onChange={(e) => setP({ ...p, annualOldRegimeDeductions: Number(e.target.value) })} />
          </label>
        )}
      </div>
      <div className="row"><button type="submit">Save</button><span className="muted">{msg}</span></div>
    </form>
  );
}

export default function Income() {
  return (
    <ResourcePage
      title="Income"
      intro="Enter your gross pay components. Annual items such as bonuses are spread over 12 months."
      endpoint="/incomes"
      initial={{ type: 'BASIC', label: '', amount: '', frequency: 'MONTHLY' }}
      fields={[
        { name: 'type', label: 'Type', type: 'select', options: ['BASIC', 'ALLOWANCE', 'BONUS', 'OTHER'] },
        { name: 'label', label: 'Description', type: 'text' },
        { name: 'amount', label: 'Amount (₹)', type: 'number' },
        { name: 'frequency', label: 'How often', type: 'select', options: ['MONTHLY', 'ANNUAL'] },
      ]}
      columns={[
        { header: 'Type', render: (r) => label(r.type) },
        { header: 'Description', render: (r) => r.label },
        { header: 'Amount', render: (r) => `${money(r.amount)} ${r.frequency === 'ANNUAL' ? '/ year' : '/ month'}` },
        { header: 'Per month', render: (r) => money(r.monthlyAmount) },
      ]}
    >
      <TaxSettings />
    </ResourcePage>
  );
}
