import { useEffect, useState } from "react";
import { appointmentApi, doctorApi, specializationApi } from "../api";
import Loading from "../components/Loading";
import PageHeader from "../components/PageHeader";
import StatePanel from "../components/StatePanel";
import StatusBadge from "../components/StatusBadge";
import { useToast } from "../context/ToastContext";
import { getErrorMessage } from "../utils/errors";
import { dateInputValue } from "../utils/date";
import { usePreferences } from "../context/PreferencesContext";

const days = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
const emptyHour = { dayOfWeek: "MONDAY", startTime: "09:00", endTime: "17:00", slotDurationMinutes: 30 };

function InputField({ label, ...props }) {
  return <label className="block"><span className="label">{label}</span><input className="field" {...props} /></label>;
}

export default function DoctorDashboardPage() {
  const { t } = usePreferences();
  const [appointments, setAppointments] = useState([]);
  const [specializations, setSpecializations] = useState([]);
  const [hours, setHours] = useState([{ ...emptyHour }]);
  const [profile, setProfile] = useState({
    fullName: "", specializationId: "", experienceYears: 0, consultationFee: "", bio: "", available: true,
  });
  const [leave, setLeave] = useState({ startDate: "", endDate: "", reason: "" });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState("");
  const { showToast } = useToast();

  async function loadData() {
    setLoading(true);
    setError("");
    try {
      const [scheduleResponse, specializationResponse] = await Promise.all([
        appointmentApi.mine(),
        specializationApi.list(),
      ]);
      setAppointments(scheduleResponse.data);
      setSpecializations(specializationResponse.data);
      setProfile((current) => ({
        ...current,
        specializationId: current.specializationId || specializationResponse.data[0]?.id || "",
      }));
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { loadData(); }, []);

  function updateHour(index, key, value) {
    setHours((current) => current.map((hour, itemIndex) =>
      itemIndex === index ? { ...hour, [key]: value } : hour));
  }

  async function saveHours(event) {
    event.preventDefault();
    setBusy("hours");
    try {
      const payload = hours.map((hour) => ({
        ...hour,
        startTime: `${hour.startTime}:00`,
        endTime: `${hour.endTime}:00`,
        slotDurationMinutes: Number(hour.slotDurationMinutes),
      }));
      await doctorApi.setWorkingHours(payload);
      showToast(t("Working hours saved. Future slots are being generated."));
    } catch (requestError) {
      showToast(getErrorMessage(requestError), "error");
    } finally {
      setBusy("");
    }
  }

  async function addLeave(event) {
    event.preventDefault();
    setBusy("leave");
    try {
      await doctorApi.addLeave({ ...leave, reason: leave.reason || null });
      setLeave({ startDate: "", endDate: "", reason: "" });
      showToast(t("Leave day added. Open slots during this period were removed."));
    } catch (requestError) {
      showToast(getErrorMessage(requestError), "error");
    } finally {
      setBusy("");
    }
  }

  async function saveProfile(event) {
    event.preventDefault();
    setBusy("profile");
    try {
      await doctorApi.updateMine({
        ...profile,
        experienceYears: Number(profile.experienceYears),
        consultationFee: Number(profile.consultationFee),
      });
      showToast(t("Doctor profile saved."));
    } catch (requestError) {
      showToast(getErrorMessage(requestError), "error");
    } finally {
      setBusy("");
    }
  }

  async function updateAppointmentStatus(appointmentId, status) {
    setBusy(appointmentId);
    try {
      await appointmentApi.updateStatus(appointmentId, status, `Marked ${status.toLowerCase()}`);
      showToast(`Appointment marked ${status.toLowerCase().replace("_", " ")}.`);
      await loadData();
    } catch (requestError) {
      showToast(getErrorMessage(requestError), "error");
    } finally {
      setBusy("");
    }
  }

  return (
    <div>
      <PageHeader eyebrow="Doctor workspace" title="Your dashboard" description="Manage your availability, profile, leave dates, and patient schedule." />
      {loading ? <Loading label={t("Loading your dashboard...")} /> : error ? (
        <StatePanel title={t("Dashboard data couldn't be loaded")} message={error} action={<button className="btn-secondary" onClick={loadData}>{t("Try again")}</button>} />
      ) : (
        <div className="space-y-7">
          <section className="rounded-3xl bg-white p-5 shadow-card sm:p-7">
            <h2 className="text-xl font-bold text-ink">{t("My appointment schedule")}</h2>
            <p className="mb-5 mt-1 text-sm text-slate-500">{t("Update the visit status after an appointment.")}</p>
            {appointments.length ? <div className="space-y-3">
              {appointments.map((appointment) => <article key={appointment.id} className="flex flex-col justify-between gap-4 rounded-xl border border-slate-100 p-4 sm:flex-row sm:items-center">
                <div>
                  <p className="font-semibold text-ink">{appointment.patientName}</p>
                  <p className="mt-1 text-sm text-slate-500">{new Date(appointment.startTime).toLocaleString()}</p>
                </div>
                <div className="flex items-center gap-3">
                  <StatusBadge status={appointment.status} />
                  {appointment.status === "BOOKED" && <select className="field !w-auto" aria-label={t("Set appointment status")}
                    disabled={busy === appointment.id} value="" onChange={(event) => updateAppointmentStatus(appointment.id, event.target.value)}>
                    <option value="" disabled>{t("Update status")}</option>
                    <option value="COMPLETED">{t("Completed")}</option>
                    <option value="NO_SHOW">{t("No show")}</option>
                  </select>}
                </div>
              </article>)}
            </div> : <StatePanel title={t("No appointments scheduled")} message={t("Patient bookings will appear here.")} />}
          </section>

          <div className="grid gap-7 lg:grid-cols-2">
            <form onSubmit={saveHours} className="space-y-4 rounded-3xl bg-white p-5 shadow-card sm:p-7">
              <div><h2 className="text-xl font-bold text-ink">{t("Weekly working hours")}</h2><p className="mt-1 text-sm text-slate-500">{t("Define when patients can book you.")}</p></div>
              {hours.map((hour, index) => <div key={index} className="grid gap-3 rounded-xl bg-slate-50 p-3 sm:grid-cols-2">
                <label className="block"><span className="label">{t("Day")}</span><select className="field" value={hour.dayOfWeek} onChange={(event) => updateHour(index, "dayOfWeek", event.target.value)}>
                  {days.map((day) => <option key={day}>{day}</option>)}
                </select></label>
                <InputField label={t("Slot length (minutes)")} type="number" min="5" value={hour.slotDurationMinutes} onChange={(event) => updateHour(index, "slotDurationMinutes", event.target.value)} />
                <InputField label={t("Starts")} type="time" required value={hour.startTime} onChange={(event) => updateHour(index, "startTime", event.target.value)} />
                <div className="flex items-end gap-2">
                  <div className="flex-1"><InputField label={t("Ends")} type="time" required value={hour.endTime} onChange={(event) => updateHour(index, "endTime", event.target.value)} /></div>
                  {hours.length > 1 && <button type="button" className="mb-0.5 rounded-lg px-3 py-2 text-sm text-rose-600 hover:bg-rose-50" onClick={() => setHours((current) => current.filter((_, i) => i !== index))}>{t("Remove")}</button>}
                </div>
              </div>)}
              <div className="flex flex-wrap gap-2">
                <button type="button" className="btn-secondary" onClick={() => setHours((current) => [...current, { ...emptyHour }])}>{t("Add hours")}</button>
                <button className="btn-primary" disabled={busy === "hours"}>{busy === "hours" ? t("Saving...") : t("Save hours")}</button>
              </div>
            </form>

            <div className="space-y-7">
              <form onSubmit={addLeave} className="space-y-4 rounded-3xl bg-white p-5 shadow-card sm:p-7">
                <div><h2 className="text-xl font-bold text-ink">{t("Add leave dates")}</h2><p className="mt-1 text-sm text-slate-500">{t("Slots during leave won't be offered to patients.")}</p></div>
                <div className="grid gap-4 sm:grid-cols-2">
                  <InputField label={t("First day")} type="date" min={dateInputValue()} required value={leave.startDate} onChange={(event) => setLeave({ ...leave, startDate: event.target.value })} />
                  <InputField label={t("Last day")} type="date" min={leave.startDate || dateInputValue()} required value={leave.endDate} onChange={(event) => setLeave({ ...leave, endDate: event.target.value })} />
                </div>
                <InputField label={t("Reason (optional)")} maxLength={500} value={leave.reason} onChange={(event) => setLeave({ ...leave, reason: event.target.value })} />
                <button className="btn-primary" disabled={busy === "leave"}>{busy === "leave" ? t("Adding...") : t("Add leave")}</button>
              </form>

              <form onSubmit={saveProfile} className="space-y-4 rounded-3xl bg-white p-5 shadow-card sm:p-7">
                <div><h2 className="text-xl font-bold text-ink">{t("Doctor profile")}</h2><p className="mt-1 text-sm text-slate-500">{t("The backend doesn't expose a GET /doctors/me route, so enter the profile values you want to save.")}</p></div>
                <InputField label={t("Full name")} required maxLength={160} value={profile.fullName} onChange={(event) => setProfile({ ...profile, fullName: event.target.value })} />
                <label className="block"><span className="label">{t("Specialization")}</span><select className="field" required value={profile.specializationId} onChange={(event) => setProfile({ ...profile, specializationId: event.target.value })}>
                  <option value="">{t("Choose specialization")}</option>{specializations.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}
                </select></label>
                <div className="grid gap-4 sm:grid-cols-2">
                  <InputField label={t("Experience (years)")} type="number" min="0" required value={profile.experienceYears} onChange={(event) => setProfile({ ...profile, experienceYears: event.target.value })} />
                  <InputField label={t("Consultation fee")} type="number" min="0" step="0.01" required value={profile.consultationFee} onChange={(event) => setProfile({ ...profile, consultationFee: event.target.value })} />
                </div>
                <label className="block"><span className="label">{t("Bio")}</span><textarea className="field min-h-24" maxLength={2000} value={profile.bio} onChange={(event) => setProfile({ ...profile, bio: event.target.value })} /></label>
                <label className="flex items-center gap-2 text-sm text-slate-700"><input type="checkbox" checked={profile.available} onChange={(event) => setProfile({ ...profile, available: event.target.checked })} /> {t("Accept new appointments")}</label>
                <button className="btn-primary" disabled={busy === "profile"}>{busy === "profile" ? t("Saving...") : t("Save profile")}</button>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
