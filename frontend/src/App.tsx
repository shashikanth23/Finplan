import { NavLink, Navigate, Route, Routes, useLocation } from 'react-router-dom';
import { ReactNode, useEffect, useState } from 'react';
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
  const location = useLocation();
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => setMenuOpen(false), [location.pathname]);

  if (!token) return <Navigate to="/login" replace />;

  const navigation = [
    { to: '/', text: 'Overview', icon: '◫', end: true },
    { to: '/income', text: 'Income', icon: '↗' },
    { to: '/expenses', text: 'Expenses', icon: '↘' },
    { to: '/debts', text: 'EMI manager', icon: '▤' },
    { to: '/goals', text: 'Savings goals', icon: '◎' },
    { to: '/calculator', text: 'Calculator', icon: '⌗' },
    { to: '/report', text: 'Reports', icon: '▥' },
  ];

  return (
    <div className={`shell${menuOpen ? ' menu-open' : ''}`}>
      <button className="sidebar-scrim" aria-label="Close navigation" onClick={() => setMenuOpen(false)} />
      <aside className="sidebar">
        <NavLink className="brand" to="/" aria-label="FinPlan home">
          <span className="brand-mark">f</span>
          <span>finplan<small>YOUR MONEY, IN FOCUS</small></span>
        </NavLink>
        <p className="nav-caption">WORKSPACE</p>
        <nav className="nav" aria-label="Main navigation">
          {navigation.map(({ to, text, icon, end }) => (
            <NavLink key={to} to={to} end={end} className={({ isActive }) => (isActive ? 'active' : '')}>
              <span className="nav-icon" aria-hidden="true">{icon}</span>
              <span>{text}</span>
              {to === '/' && <span className="nav-current" aria-hidden="true" />}
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <div className="sidebar-tip">
            <span className="tip-icon">✦</span>
            <strong>Small steps add up</strong>
            <p>A clearer picture starts with keeping your numbers up to date.</p>
          </div>
          <button className="sign-out" onClick={logout}><span aria-hidden="true">↪</span> Sign out</button>
        </div>
      </aside>
      <div className="app-content">
        <header className="topbar">
          <button className="menu-toggle" onClick={() => setMenuOpen(true)} aria-label="Open navigation" aria-expanded={menuOpen}>
            <span /><span /><span />
          </button>
          <div className="topbar-context"><span>PERSONAL FINANCE</span><strong>Make your money make sense.</strong></div>
          <div className="topbar-date"><span className="status-dot" /> YOUR FINANCIAL OVERVIEW</div>
        </header>
        <main className="main">{children}</main>
      </div>
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
