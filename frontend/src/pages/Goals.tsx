import ResourcePage from '../components/ResourcePage';
import { money, pct } from '../format';

export default function Goals() {
  return (
    <ResourcePage
      title="Savings goals"
      intro="Each goal turns into a monthly amount to set aside. The total feeds your required salary."
      endpoint="/goals"
      initial={{ name: '', targetAmount: '', savedAmount: '0', targetDate: '' }}
      fields={[
        { name: 'name', label: 'Goal', type: 'text' },
        { name: 'targetAmount', label: 'Target (₹)', type: 'number' },
        { name: 'savedAmount', label: 'Saved so far (₹)', type: 'number' },
        { name: 'targetDate', label: 'Target date', type: 'date' },
      ]}
      columns={[
        { header: 'Goal', render: (r) => r.name },
        { header: 'Target', render: (r) => money(r.targetAmount) },
        { header: 'Progress', render: (r) => (
          <div className="bar" title={pct(r.progressPct)}><span style={{ width: `${r.progressPct}%` }} /></div>
        ) },
        { header: 'Due', render: (r) => r.targetDate },
        { header: 'Save per month', render: (r) => (r.reached ? 'Reached' : money(r.requiredMonthly)) },
      ]}
    />
  );
}
