import { useState } from "react";
import { Link, Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { getErrorMessage } from "../utils/errors";
import { dateInputValue } from "../utils/date";
import { usePreferences } from "../context/PreferencesContext";

const initialForm = { fullName: "", email: "", password: "", phone: "", dateOfBirth: "" };

export default function RegisterPage() {
  const { user, register } = useAuth();
  const { t } = usePreferences();
  const navigate = useNavigate();
  const [form, setForm] = useState(initialForm);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  if (user) return <Navigate to="/doctors" replace />;

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    if (form.password.length < 12 || form.password.length > 72) {
      setError(t("Password must be between 12 and 72 characters."));
      return;
    }
    setBusy(true);
    try {
      const payload = { ...form, dateOfBirth: form.dateOfBirth || null };
      await register(payload);
      navigate("/doctors", { replace: true });
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="mx-auto max-w-xl">
      <div className="mb-6">
        <p className="mb-2 text-xs font-bold uppercase tracking-[0.16em] text-brand">{t("Patient registration")}</p>
        <h1 className="text-3xl font-bold tracking-tight text-ink">{t("Create your account")}</h1>
        <p className="mt-2 text-sm text-slate-500">{t("New registrations are patient accounts. Doctor accounts are created by an administrator.")}</p>
      </div>
      <form onSubmit={handleSubmit} className="grid gap-5 rounded-3xl bg-white p-6 shadow-card sm:grid-cols-2 sm:p-8">
        {error && <div role="alert" className="rounded-xl bg-rose-50 px-4 py-3 text-sm text-rose-700 sm:col-span-2">{error}</div>}
        <div className="sm:col-span-2">
          <label className="label" htmlFor="fullName">{t("Full name")}</label>
          <input className="field" id="fullName" required maxLength={160} autoComplete="name" value={form.fullName}
            onChange={(event) => setForm({ ...form, fullName: event.target.value })} />
        </div>
        <div className="sm:col-span-2">
          <label className="label" htmlFor="email">{t("Email address")}</label>
          <input className="field" id="email" type="email" required maxLength={254} autoComplete="email" value={form.email}
            onChange={(event) => setForm({ ...form, email: event.target.value })} />
        </div>
        <div className="sm:col-span-2">
          <label className="label" htmlFor="password">{t("Password")} <span className="font-normal text-slate-400">({t("12–72 characters")})</span></label>
          <input className="field" id="password" type="password" required minLength={12} maxLength={72} autoComplete="new-password" value={form.password}
            onChange={(event) => setForm({ ...form, password: event.target.value })} />
        </div>
        <div>
          <label className="label" htmlFor="phone">{t("Phone")} <span className="font-normal text-slate-400">({t("optional")})</span></label>
          <input className="field" id="phone" type="tel" maxLength={30} autoComplete="tel" value={form.phone}
            onChange={(event) => setForm({ ...form, phone: event.target.value })} />
        </div>
        <div>
          <label className="label" htmlFor="dateOfBirth">{t("Date of birth")} <span className="font-normal text-slate-400">({t("optional")})</span></label>
          <input className="field" id="dateOfBirth" type="date" max={dateInputValue()} value={form.dateOfBirth}
            onChange={(event) => setForm({ ...form, dateOfBirth: event.target.value })} />
        </div>
        <button className="btn-primary w-full sm:col-span-2" disabled={busy}>{busy ? t("Creating account...") : t("Create patient account")}</button>
        <p className="text-center text-sm text-slate-500 sm:col-span-2">
          {t("Already have an account?")} <Link className="font-semibold text-brand hover:underline" to="/login">{t("Sign in")}</Link>
        </p>
      </form>
    </div>
  );
}
