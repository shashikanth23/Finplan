import ResourcePage from '../components/ResourcePage';
import { label, money } from '../format';

export default function Debts() {
  return (
    <ResourcePage
      title="EMI manager"
      intro="Add each loan. Leave EMI blank and it is calculated from the principal, rate and tenure; leave the balance blank for a new loan."
      endpoint="/debts"
      initial={{ loanType: 'HOME', lender: '', principal: '', annualRate: '', tenureMonths: '', emi: '', remainingBalance: '' }}
      fields={[
        { name: 'loanType', label: 'Loan type', type: 'select', options: ['HOME', 'CAR', 'PERSONAL', 'EDUCATION', 'CREDIT_CARD', 'OTHER'] },
        { name: 'lender', label: 'Lender', type: 'text', optional: true },
        { name: 'principal', label: 'Principal (₹)', type: 'number' },
        { name: 'annualRate', label: 'Interest rate (% per year)', type: 'number', step: '0.01' },
        { name: 'tenureMonths', label: 'Tenure (months)', type: 'number', step: '1' },
        { name: 'emi', label: 'Current EMI (₹)', type: 'number', optional: true },
        { name: 'remainingBalance', label: 'Remaining balance (₹)', type: 'number', optional: true },
      ]}
      columns={[
        { header: 'Loan', render: (r) => `${label(r.loanType)}${r.lender ? ` (${r.lender})` : ''}` },
        { header: 'Principal', render: (r) => money(r.principal) },
        { header: 'Rate', render: (r) => `${r.annualRate}%` },
        { header: 'Tenure', render: (r) => `${r.tenureMonths} mo` },
        { header: 'EMI', render: (r) => money(r.emi) },
        { header: 'Balance', render: (r) => money(r.remainingBalance) },
      ]}
    />
  );
}
