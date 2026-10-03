import { NavLink, Navigate, Route, Routes } from 'react-router-dom';
import { ReactNode } from 'react';
import { useAuth } from './auth';
import AuthPage from './pages/AuthPage';
import Dashboard from './pages/Dashboard';
import Income from './pages/Income';
import Expenses from './pages/Expenses';
import Debts from './pages/Debts';
import Goals from './pages/Goals';
import Calculator from './pages/Calculator';
import Report from './pages/Report';

function Protected({ children }: { children: ReactNode }) {
  const { token, logout } = useAuth();
  if (!token) return <Navigate to="/login" replace />;
  return (
    <div className="shell">
      <nav className="nav">
        <strong className="brand">FinPlan</strong>
        {[
          ['/', 'Dashboard'], ['/income', 'Income'], ['/expenses', 'Expenses'], ['/debts', 'EMI manager'],
          ['/goals', 'Goals'], ['/calculator', 'Calculator'], ['/report', 'Report'],
        ].map(([to, text]) => (
          <NavLink key={to} to={to} end={to === '/'} className={({ isActive }) => (isActive ? 'active' : '')}>{text}</NavLink>
        ))}
        <button className="link" onClick={logout}>Sign out</button>
      </nav>
      <main className="main">{children}</main>
    </div>
  );
}

export default function App() {
  const { token } = useAuth();
  return (
    <Routes>
      <Route path="/login" element={token ? <Navigate to="/" replace /> : <AuthPage />} />
      <Route path="/" element={<Protected><Dashboard /></Protected>} />
      <Route path="/income" element={<Protected><Income /></Protected>} />
      <Route path="/expenses" element={<Protected><Expenses /></Protected>} />
      <Route path="/debts" element={<Protected><Debts /></Protected>} />
      <Route path="/goals" element={<Protected><Goals /></Protected>} />
      <Route path="/calculator" element={<Protected><Calculator /></Protected>} />
      <Route path="/report" element={<Protected><Report /></Protected>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
