export interface Affordability { bySurplus: number; byFoir: number; maxNewEmi: number; limitedBy: string }
export interface HealthScore { score: number; grade: string; recommendations: string[] }
export interface Summary {
  monthlyGross: number; monthlyTax: number; monthlyNet: number;
  monthlyExpenses: number; monthlyEmis: number; monthlySavingsTarget: number;
  disposableIncome: number; freeCashAfterSavings: number;
  requiredGross: number; grossGap: number;
  emiToIncomePct: number; savingsRatePct: number;
  affordability: Affordability; health: HealthScore;
}
export interface Dashboard {
  summary: Summary;
  expensesByCategory: Record<string, number>;
  incomeByType: Record<string, number>;
  totalDebtRemaining: number;
  activeGoals: number;
  source: string;
}
export interface Profile {
  taxRegime: 'NEW' | 'OLD';
  monthlyFixedDeductions: number;
  annualOldRegimeDeductions: number;
}
export interface ScheduleRow { month: number; payment: number; interest: number; principal: number; balance: number }
export interface ScenarioResult { base: Summary; scenario: Summary; netDelta: number; freeCashDelta: number; scoreDelta: number }
