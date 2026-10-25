import { useEffect, useState } from "react";
import { patientApi } from "../api";
import Loading from "../components/Loading";
import PageHeader from "../components/PageHeader";
import StatePanel from "../components/StatePanel";
import { useToast } from "../context/ToastContext";
import { getErrorMessage } from "../utils/errors";
import { dateInputValue } from "../utils/date";
import { usePreferences } from "../context/PreferencesContext";

export default function PatientProfilePage() {
  const { t } = usePreferences();
  const [form, setForm] = useState({ fullName: "", phone: "", dateOfBirth: "" });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const { showToast } = useToast();

  async function loadProfile() {
    setLoading(true);
    setError("");
    try {
      const { data } = await patientApi.getMine();
      setForm({
        fullName: data.fullName || "",
        phone: data.phone || "",
        dateOfBirth: data.dateOfBirth || "",
      });
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadProfile(); }, []);

  async function save(event) {
    event.preventDefault();
    setError("");
    setSaving(true);
    try {
      const { data } = await patientApi.updateMine({
        fullName: form.fullName,
        phone: form.phone || null,
        dateOfBirth: form.dateOfBirth || null,
      });
      setForm({ fullName: data.fullName || "", phone: data.phone || "", dateOfBirth: data.dateOfBirth || "" });
      showToast(t("Profile saved."));
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader eyebrow="Your account" title="Patient profile" description="Keep your contact details accurate so your care team can reach you." />
      {loading ? <Loading label={t("Loading your profile...")} /> : error && !form.fullName ? (
        <StatePanel title={t("Profile couldn't be loaded")} message={error} action={<button className="btn-secondary" onClick={loadProfile}>{t("Try again")}</button>} />
      ) : (
        <form onSubmit={save} className="space-y-5 rounded-3xl bg-white p-6 shadow-card sm:p-8">
          {error && <p role="alert" className="rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
          <div>
            <label className="label" htmlFor="fullName">{t("Full name")}</label>
            <input className="field" id="fullName" maxLength={160} required value={form.fullName}
              onChange={(event) => setForm({ ...form, fullName: event.target.value })} />
          </div>
          <div>
            <label className="label" htmlFor="phone">{t("Phone number")}</label>
            <input className="field" id="phone" type="tel" maxLength={30} value={form.phone}
              onChange={(event) => setForm({ ...form, phone: event.target.value })} />
          </div>
          <div>
            <label className="label" htmlFor="dateOfBirth">{t("Date of birth")}</label>
            <input className="field" id="dateOfBirth" type="date" max={dateInputValue()} value={form.dateOfBirth}
              onChange={(event) => setForm({ ...form, dateOfBirth: event.target.value })} />
          </div>
          <button className="btn-primary" disabled={saving}>{saving ? t("Saving...") : t("Save profile")}</button>
        </form>
      )}
    </div>
  );
}
