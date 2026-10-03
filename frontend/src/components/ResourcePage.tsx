import { FormEvent, ReactNode, useCallback, useEffect, useState } from 'react';
import { api } from '../api';

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type Row = { id: string } & Record<string, any>;

export interface Field {
  name: string;
  label: string;
  type: 'text' | 'number' | 'select' | 'date';
  options?: string[];
  optional?: boolean;
  step?: string;
  hint?: string;
}

export interface Column {
  header: string;
  render: (row: Row) => ReactNode;
}

interface Props {
  title: string;
  intro?: string;
  endpoint: string;
  fields: Field[];
  columns: Column[];
  initial: Record<string, string>;
  children?: ReactNode;
}

/** One list-plus-form screen for any CRUD resource; income, expenses, loans and goals all use it. */
export default function ResourcePage({ title, intro, endpoint, fields, columns, initial, children }: Props) {
  const [rows, setRows] = useState<Row[]>([]);
  const [form, setForm] = useState<Record<string, string>>(initial);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    try {
      setRows(await api<Row[]>(endpoint));
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not load data');
    } finally {
      setLoading(false);
    }
  }, [endpoint]);

  useEffect(() => { void load(); }, [load]);

  function reset() {
    setForm(initial);
    setEditingId(null);
  }

  function edit(row: Row) {
    setEditingId(row.id);
    setForm(Object.fromEntries(fields.map((f) => [f.name, String(row[f.name] ?? '')])));
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  async function submit(e: FormEvent) {
    e.preventDefault();
    setError('');
    const body: Record<string, unknown> = {};
    for (const f of fields) {
      const v = form[f.name] ?? '';
      if (v === '' && f.optional) continue;
      body[f.name] = f.type === 'number' ? Number(v) : v;
    }
    try {
      if (editingId) await api(`${endpoint}/${editingId}`, { method: 'PUT', body });
      else await api(endpoint, { method: 'POST', body });
      reset();
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed');
    }
  }

  async function remove(row: Row) {
    if (!window.confirm('Delete this entry?')) return;
    try {
      await api(`${endpoint}/${row.id}`, { method: 'DELETE' });
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Delete failed');
    }
  }

  return (
    <section>
      <h1>{title}</h1>
      {intro && <p className="muted">{intro}</p>}
      {children}

      <form className="card form" onSubmit={submit}>
        <h2>{editingId ? 'Edit entry' : 'Add entry'}</h2>
        <div className="grid">
          {fields.map((f) => (
            <label key={f.name}>
              {f.label}{f.optional ? ' (optional)' : ''}
              {f.type === 'select' ? (
                <select value={form[f.name] ?? ''} onChange={(e) => setForm({ ...form, [f.name]: e.target.value })}>
                  {f.options!.map((o) => <option key={o} value={o}>{o.charAt(0) + o.slice(1).toLowerCase().replace(/_/g, ' ')}</option>)}
                </select>
              ) : (
                <input
                  type={f.type}
                  step={f.type === 'number' ? f.step ?? 'any' : undefined}
                  min={f.type === 'number' ? 0 : undefined}
                  required={!f.optional}
                  value={form[f.name] ?? ''}
                  onChange={(e) => setForm({ ...form, [f.name]: e.target.value })}
                />
              )}
              {f.hint && <small className="muted">{f.hint}</small>}
            </label>
          ))}
        </div>
        {error && <p className="error">{error}</p>}
        <div className="row">
          <button type="submit">{editingId ? 'Save changes' : 'Add'}</button>
          {editingId && <button type="button" className="secondary" onClick={reset}>Cancel</button>}
        </div>
      </form>

      <div className="card">
        {loading ? <p className="muted">Loading…</p> : rows.length === 0 ? <p className="muted">Nothing here yet.</p> : (
          <div className="scroll">
            <table>
              <thead><tr>{columns.map((c) => <th key={c.header}>{c.header}</th>)}<th /></tr></thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.id}>
                    {columns.map((c) => <td key={c.header}>{c.render(r)}</td>)}
                    <td className="actions">
                      <button className="link" onClick={() => edit(r)}>Edit</button>
                      <button className="link danger" onClick={() => void remove(r)}>Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  );
}
