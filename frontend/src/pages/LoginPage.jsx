import { useState } from "react";
import { Link, Navigate, useLocation, useNavigate } from "react-router-dom";
import PageHeader from "../components/PageHeader";
import { useAuth } from "../context/AuthContext";
import { getErrorMessage } from "../utils/errors";
import { usePreferences } from "../context/PreferencesContext";

export default function LoginPage() {
  const { user, login } = useAuth();
  const { t } = usePreferences();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/doctors" replace />;

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      await login(form);
      navigate(location.state?.from?.pathname || "/doctors", { replace: true });
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-md">
      <div className="mb-6 rounded-3xl bg-ink px-7 py-8 text-white">
        <p className="mb-2 text-xs font-bold uppercase tracking-[0.16em] text-teal-200">{t("Welcome back")}</p>
        <h1 className="text-3xl font-bold">{t("Sign in to Carepoint")}</h1>
        <p className="mt-2 text-sm text-slate-300">{t("Manage your care and appointments securely.")}</p>
      </div>
      <form onSubmit={handleSubmit} className="space-y-5 rounded-3xl bg-white p-6 shadow-card sm:p-8">
        {error && <div role="alert" className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</div>}
        <div>
          <label className="label" htmlFor="email">{t("Email address")}</label>
          <input className="field" id="email" type="email" autoComplete="email" required maxLength={254}
            value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} />
        </div>
        <div>
          <label className="label" htmlFor="password">{t("Password")}</label>
          <input className="field" id="password" type="password" autoComplete="current-password" required
            value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} />
        </div>
        <button className="btn-primary w-full" disabled={busy}>{busy ? t("Signing in...") : t("Sign in")}</button>
        <p className="text-center text-sm text-slate-500">
          {t("New to Carepoint?")} <Link className="font-semibold text-brand hover:underline" to="/register">{t("Create a patient account")}</Link>
        </p>
      </form>
    </div>
  );
}
