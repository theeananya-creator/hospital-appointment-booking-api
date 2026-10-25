import { useEffect, useState } from "react";
import {
  Bar, BarChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer,
  Tooltip, XAxis, YAxis,
} from "recharts";
import { adminApi, specializationApi } from "../api";
import Loading from "../components/Loading";
import PageHeader from "../components/PageHeader";
import StatePanel from "../components/StatePanel";
import { useToast } from "../context/ToastContext";
import { getErrorMessage } from "../utils/errors";
import { usePreferences } from "../context/PreferencesContext";

const chartColors = ["#0f766e", "#2a9d8f", "#f4a261", "#e76f51", "#457b9d", "#8e7dbe"];

function toRows(data) {
  if (Array.isArray(data)) return data;
  if (data && typeof data === "object") return Object.entries(data).map(([label, value]) => ({ label, value }));
  return [];
}

function findKey(rows, keys) {
  const sample = rows[0] || {};
  return Object.keys(sample).find((key) => keys.some((candidate) => candidate.toLowerCase() === key.toLowerCase()))
    || Object.keys(sample)[0];
}

function ReportChart({ title, rows, labels, values, emptyMessage }) {
  const { t } = usePreferences();
  const labelKey = findKey(rows, labels);
  const valueKey = findKey(rows, values);
  const normalized = rows.map((row, index) => ({
    label: String(row[labelKey] ?? `Item ${index + 1}`),
    value: Number(row[valueKey] ?? 0),
  }));

  return (
    <section className="rounded-2xl bg-white p-5 shadow-card">
      <h2 className="mb-4 font-bold text-ink">{t(title)}</h2>
      {normalized.length ? <div className="h-64">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart data={normalized} margin={{ top: 8, right: 8, bottom: 28, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" vertical={false} />
            <XAxis dataKey="label" tick={{ fontSize: 10 }} angle={-18} textAnchor="end" interval={0} />
            <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
            <Tooltip />
            <Bar dataKey="value" name="Appointments" radius={[6, 6, 0, 0]}>
              {normalized.map((_, index) => <Cell key={index} fill={chartColors[index % chartColors.length]} />)}
            </Bar>
          </BarChart>
        </ResponsiveContainer>
      </div> : <p className="py-10 text-center text-sm text-slate-500">{t(emptyMessage || "No report data is available yet.")}</p>}
    </section>
  );
}

export default function AdminDashboardPage() {
  const { t } = usePreferences();
  const [specializations, setSpecializations] = useState([]);
  const [reports, setReports] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");
  const [specializationForm, setSpecializationForm] = useState({ name: "", description: "" });
  const [doctorForm, setDoctorForm] = useState({
    email: "", password: "", fullName: "", specializationId: "", experienceYears: 0, consultationFee: "", bio: "",
  });
  const { showToast } = useToast();

  async function loadDashboard() {
    setLoading(true);
    setError("");
    try {
      const [specializationResponse, doctorsResponse, cancellationResponse, hoursResponse, topResponse] = await Promise.all([
        specializationApi.list(),
        adminApi.appointmentsPerDoctorMonth(),
        adminApi.cancellationRate(),
        adminApi.peakBookingHours(),
        adminApi.topSpecializations(),
      ]);
      setSpecializations(specializationResponse.data);
      setDoctorForm((current) => ({
        ...current,
        specializationId: current.specializationId || specializationResponse.data[0]?.id || "",
      }));
      setReports({
        monthly: toRows(doctorsResponse.data),
        cancellation: cancellationResponse.data,
        hours: toRows(hoursResponse.data),
        specializations: toRows(topResponse.data),
      });
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadDashboard(); }, []);

  async function createSpecialization(event) {
    event.preventDefault();
    setBusy("specialization");
    try {
      await specializationApi.create(specializationForm);
      setSpecializationForm({ name: "", description: "" });
      showToast(t("Specialization created."));
      const { data } = await specializationApi.list();
      setSpecializations(data);
      setDoctorForm((current) => ({ ...current, specializationId: current.specializationId || data[0]?.id || "" }));
    } catch (requestError) {
      showToast(getErrorMessage(requestError), "error");
    } finally {
      setBusy("");
    }
  }

  async function createDoctor(event) {
    event.preventDefault();
    setBusy("doctor");
    try {
      await adminApi.createDoctor({
        ...doctorForm,
        experienceYears: Number(doctorForm.experienceYears),
        consultationFee: Number(doctorForm.consultationFee),
      });
      setDoctorForm((current) => ({
        ...current, email: "", password: "", fullName: "", experienceYears: 0, consultationFee: "", bio: "",
      }));
      showToast(t("Doctor account created."));
    } catch (requestError) {
      showToast(getErrorMessage(requestError), "error");
    } finally {
      setBusy("");
    }
  }

  if (loading) return <Loading label={t("Loading admin reports...")} />;
  if (error) return <StatePanel title={t("Admin data couldn't be loaded")} message={error}
    action={<button className="btn-secondary" onClick={loadDashboard}>{t("Try again")}</button>} />;

  const monthlyLabels = reports.monthly.map((row) => ({
    ...row,
    chartLabel: `${row.doctorName || row.doctorId} ${row.year}-${String(row.month).padStart(2, "0")}`,
    appointments: Number(row.appointmentCount || 0),
  }));
  const cancellationRate = Number(reports.cancellation.cancellationRate || 0);

  return (
    <div>
      <PageHeader eyebrow="Administration" title="Hospital overview" description="Provision care teams, manage specializations, and review booking trends." />
      <div className="mb-8 grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl bg-white p-5 shadow-card"><p className="text-xs uppercase tracking-wider text-slate-400">{t("Specializations")}</p><p className="mt-2 text-3xl font-bold">{specializations.length}</p></div>
        <div className="rounded-2xl bg-white p-5 shadow-card"><p className="text-xs uppercase tracking-wider text-slate-400">{t("Cancellation rate")}</p><p className="mt-2 text-3xl font-bold">{(cancellationRate * 100).toFixed(1)}%</p><p className="mt-1 text-xs text-slate-500">{reports.cancellation.cancelledAppointments || 0} of {reports.cancellation.totalAppointments || 0} appointments</p></div>
        <div className="rounded-2xl bg-white p-5 shadow-card"><p className="text-xs uppercase tracking-wider text-slate-400">{t("Monthly doctor summaries")}</p><p className="mt-2 text-3xl font-bold">{reports.monthly.length}</p></div>
      </div>

      <div className="mb-8 grid gap-6 lg:grid-cols-2">
        <form onSubmit={createSpecialization} className="space-y-4 rounded-3xl bg-white p-5 shadow-card sm:p-7">
          <div><h2 className="text-xl font-bold text-ink">{t("Create specialization")}</h2><p className="mt-1 text-sm text-slate-500">{t("Add a department category for doctor profiles.")}</p></div>
          <label className="block"><span className="label">{t("Name")}</span><input className="field" required maxLength={120} value={specializationForm.name} onChange={(event) => setSpecializationForm({ ...specializationForm, name: event.target.value })} /></label>
          <label className="block"><span className="label">{t("Description")}</span><textarea className="field min-h-20" maxLength={500} value={specializationForm.description} onChange={(event) => setSpecializationForm({ ...specializationForm, description: event.target.value })} /></label>
          <button className="btn-primary" disabled={busy === "specialization"}>{busy === "specialization" ? t("Creating...") : t("Create specialization")}</button>
        </form>
        <form onSubmit={createDoctor} className="space-y-4 rounded-3xl bg-white p-5 shadow-card sm:p-7">
          <div><h2 className="text-xl font-bold text-ink">{t("Create doctor account")}</h2><p className="mt-1 text-sm text-slate-500">{t("Doctors are provisioned by admins, not self-registered.")}</p></div>
          <label className="block"><span className="label">{t("Full name")}</span><input className="field" required maxLength={160} value={doctorForm.fullName} onChange={(event) => setDoctorForm({ ...doctorForm, fullName: event.target.value })} /></label>
          <label className="block"><span className="label">{t("Email")}</span><input className="field" type="email" required maxLength={254} value={doctorForm.email} onChange={(event) => setDoctorForm({ ...doctorForm, email: event.target.value })} /></label>
          <label className="block"><span className="label">{t("Temporary password (12–72 characters)")}</span><input className="field" type="password" required minLength={12} maxLength={72} value={doctorForm.password} onChange={(event) => setDoctorForm({ ...doctorForm, password: event.target.value })} /></label>
          <label className="block"><span className="label">{t("Specialization")}</span><select className="field" required value={doctorForm.specializationId} onChange={(event) => setDoctorForm({ ...doctorForm, specializationId: event.target.value })}><option value="">{t("Choose specialization")}</option>{specializations.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block"><span className="label">{t("Experience years")}</span><input className="field" type="number" min="0" value={doctorForm.experienceYears} onChange={(event) => setDoctorForm({ ...doctorForm, experienceYears: event.target.value })} /></label>
            <label className="block"><span className="label">{t("Consultation fee")}</span><input className="field" type="number" min="0" step="0.01" required value={doctorForm.consultationFee} onChange={(event) => setDoctorForm({ ...doctorForm, consultationFee: event.target.value })} /></label>
          </div>
          <label className="block"><span className="label">{t("Bio")}</span><textarea className="field min-h-20" maxLength={2000} value={doctorForm.bio} onChange={(event) => setDoctorForm({ ...doctorForm, bio: event.target.value })} /></label>
          <button className="btn-primary" disabled={busy === "doctor" || specializations.length === 0}>{busy === "doctor" ? t("Creating...") : t("Create doctor")}</button>
        </form>
      </div>

      <section className="mb-6 rounded-2xl bg-white p-5 shadow-card">
        <h2 className="mb-4 font-bold text-ink">{t("Appointments per doctor per month")}</h2>
        {monthlyLabels.length ? <div className="h-72">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={monthlyLabels} margin={{ top: 8, right: 8, bottom: 55, left: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="chartLabel" tick={{ fontSize: 10 }} angle={-25} textAnchor="end" interval={0} />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="appointments" fill="#0f766e" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div> : <p className="py-8 text-center text-sm text-slate-500">No monthly appointment data is available yet.</p>}
      </section>
      <div className="grid gap-6 lg:grid-cols-2">
        <section className="rounded-2xl bg-white p-5 shadow-card">
          <h2 className="mb-4 font-bold text-ink">{t("Appointment cancellation rate")}</h2>
          {Number(reports.cancellation.totalAppointments) ? <div className="h-64">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={[
                  { name: "Cancelled", value: Number(reports.cancellation.cancelledAppointments || 0) },
                  { name: "Other appointments", value: Math.max(0, Number(reports.cancellation.totalAppointments || 0) - Number(reports.cancellation.cancelledAppointments || 0)) },
                ]} dataKey="value" nameKey="name" innerRadius={55} outerRadius={85} paddingAngle={3}>
                  {[0, 1].map((index) => <Cell key={index} fill={index === 0 ? "#e76f51" : "#0f766e"} />)}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div> : <p className="py-10 text-center text-sm text-slate-500">No appointment data is available yet.</p>}
        </section>
        <ReportChart title="Peak booking hours" rows={reports.hours}
          labels={["hourOfDay", "bookingHour", "hour", "label"]}
          values={["appointmentCount", "count", "value"]} />
        <ReportChart title="Top specializations" rows={reports.specializations}
          labels={["specialization", "name", "label"]}
          values={["appointmentCount", "count", "value"]} />
      </div>
    </div>
  );
}
