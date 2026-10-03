const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 });

export const money = (n: number): string => inr.format(n);
export const pct = (n: number): string => `${n.toFixed(1)}%`;
export const label = (s: string): string => s.charAt(0) + s.slice(1).toLowerCase().replace(/_/g, ' ');
