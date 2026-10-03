import ResourcePage from '../components/ResourcePage';
import { label, money } from '../format';

export default function Expenses() {
  return (
    <ResourcePage
      title="Expenses"
      intro="Monthly living costs. Fixed costs rarely change (rent, insurance); variable ones can be trimmed."
      endpoint="/expenses"
      initial={{ category: 'RENT', kind: 'FIXED', label: '', monthlyAmount: '' }}
      fields={[
        { name: 'category', label: 'Category', type: 'select', options: ['RENT', 'FOOD', 'UTILITIES', 'TRANSPORT', 'INSURANCE', 'EDUCATION', 'LIFESTYLE', 'OTHER'] },
        { name: 'kind', label: 'Fixed or variable', type: 'select', options: ['FIXED', 'VARIABLE'] },
        { name: 'label', label: 'Description', type: 'text' },
        { name: 'monthlyAmount', label: 'Per month (₹)', type: 'number' },
      ]}
      columns={[
        { header: 'Category', render: (r) => label(r.category) },
        { header: 'Kind', render: (r) => label(r.kind) },
        { header: 'Description', render: (r) => r.label },
        { header: 'Per month', render: (r) => money(r.monthlyAmount) },
      ]}
    />
  );
}
