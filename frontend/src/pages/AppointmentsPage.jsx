import { useCallback, useEffect, useState } from "react";
import { appointmentApi, doctorApi } from "../api";
import Loading from "../components/Loading";
import Modal from "../components/Modal";
import PageHeader from "../components/PageHeader";
import StatePanel from "../components/StatePanel";
import StatusBadge from "../components/StatusBadge";
import { useToast } from "../context/ToastContext";
import { getErrorMessage } from "../utils/errors";
import { dateInputValue } from "../utils/date";
import { usePreferences } from "../context/PreferencesContext";

function formatDate(dateTime) {
  return new Date(dateTime).toLocaleString([], {
    weekday: "short", month: "short", day: "numeric", year: "numeric",
    hour: "numeric", minute: "2-digit",
  });
}

function canChange(appointment) {
  return appointment.status === "BOOKED"
    && new Date(appointment.startTime).getTime() - Date.now() >= 2 * 60 * 60 * 1000;
}

function RescheduleForm({ appointment, onClose, onComplete }) {
  const { t } = usePreferences();
  const [date, setDate] = useState("");
  const [slots, setSlots] = useState([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!date) return;
    let active = true;
    setLoading(true);
    setError("");
    doctorApi.slots(appointment.doctorId, date)
      .then(({ data }) => { if (active) setSlots(data); })
      .catch((requestError) => { if (active) setError(getErrorMessage(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [appointment.doctorId, date]);

  async function chooseSlot(slotId) {
    setSaving(true);
    setError("");
    try {
      await appointmentApi.reschedule(appointment.id, slotId);
      onComplete("Appointment rescheduled.");
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  return (
    <Modal title={`Reschedule with ${appointment.doctorName}`} onClose={onClose}>
      <p className="mb-4 text-sm text-slate-500">{t("Choose a new date and time. Your current time stays booked until the change succeeds.")}</p>
      <label className="label" htmlFor="newDate">{t("New appointment date")}</label>
      <input id="newDate" className="field" type="date" min={dateInputValue()} value={date}
        onChange={(event) => setDate(event.target.value)} />
      {error && <p role="alert" className="mt-4 rounded-xl bg-rose-50 p-3 text-sm text-rose-700">{error}</p>}
      {loading ? <Loading label={t("Loading available times...")} /> : date && (
        slots.length ? <div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-3">
          {slots.map((slot) => <button key={slot.id} className="btn-secondary" disabled={saving}
            onClick={() => chooseSlot(slot.id)}>{new Date(slot.startTime).toLocaleTimeString([], { hour: "numeric", minute: "2-digit" })}</button>)}
        </div> : !error && <p className="mt-4 text-sm text-slate-500">{t("No times are available on this date.")}</p>
      )}
    </Modal>
  );
}

export default function AppointmentsPage() {
  const { t } = usePreferences();
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [history, setHistory] = useState(null);
  const [rescheduling, setRescheduling] = useState(null);
  const [busyId, setBusyId] = useState("");
  const { showToast } = useToast();

  const loadAppointments = useCallback(async (quiet = false) => {
    if (!quiet) {
      setLoading(true);
      setError("");
    }
    try {
      const { data } = await appointmentApi.mine();
      setAppointments(data);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      if (!quiet) setLoading(false);
    }
  }, []);

  useEffect(() => { loadAppointments(); }, [loadAppointments]);

  useEffect(() => {
    const refresh = () => {
      if (document.visibilityState === "visible") loadAppointments(true);
    };
    const timer = window.setInterval(refresh, 15_000);
    document.addEventListener("visibilitychange", refresh);
    return () => {
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", refresh);
    };
  }, [loadAppointments]);

  async function cancel(appointment) {
    if (!window.confirm(`Cancel your appointment with ${appointment.doctorName}?`)) return;
    setBusyId(appointment.id);
    try {
      await appointmentApi.cancel(appointment.id);
      showToast(t("Appointment cancelled."));
      await loadAppointments();
    } catch (requestError) {
      showToast(getErrorMessage(requestError), "error");
    } finally {
      setBusyId("");
    }
  }

  async function openHistory(appointment) {
    setHistory({ appointment, entries: null, error: "" });
    try {
      const { data } = await appointmentApi.history(appointment.id);
      setHistory({ appointment, entries: data, error: "" });
    } catch (requestError) {
      setHistory({ appointment, entries: [], error: getErrorMessage(requestError) });
    }
  }

  const upcoming = appointments.filter((item) => new Date(item.startTime) >= new Date() && item.status === "BOOKED");
  const past = appointments.filter((item) => !upcoming.includes(item));
  const renderAppointment = (appointment) => (
    <article key={appointment.id} className="rounded-2xl bg-white p-5 shadow-card">
      <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="text-lg font-bold text-ink">{appointment.doctorName}</h3>
            <StatusBadge status={appointment.status} />
          </div>
          <p className="mt-1 text-sm text-slate-500">{formatDate(appointment.startTime)}</p>
          <p className="mt-1 text-xs text-slate-400">Appointment ID: {appointment.id}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button className="btn-secondary !px-3 !py-2" onClick={() => openHistory(appointment)}>{t("History")}</button>
          {canChange(appointment) && <button className="btn-secondary !px-3 !py-2" onClick={() => setRescheduling(appointment)}>{t("Reschedule")}</button>}
          {canChange(appointment) && <button className="rounded-xl border border-rose-200 px-3 py-2 text-sm font-semibold text-rose-700 hover:bg-rose-50 disabled:opacity-50"
            disabled={busyId === appointment.id} onClick={() => cancel(appointment)}>{busyId === appointment.id ? "Cancelling..." : t("Cancel")}</button>}
        </div>
      </div>
      {appointment.status === "BOOKED" && new Date(appointment.startTime) < new Date(Date.now() + 2 * 60 * 60 * 1000)
        && <p className="mt-3 text-xs text-amber-700">Cancellation and rescheduling close two hours before the appointment.</p>}
      {appointment.status === "BOOKED" && appointment.rescheduleCount >= 3
        && <p className="mt-2 text-xs text-slate-500">Maximum reschedules reached.</p>}
    </article>
  );

  return (
    <div>
      <PageHeader eyebrow={t("Your appointments")} title={t("Appointments")} description={t("Review visits, manage upcoming bookings, and see appointment history.")} />
      {loading ? <Loading label="Loading appointments..." /> : error ? (
        <StatePanel title="Appointments couldn't be loaded" message={error} action={<button className="btn-secondary" onClick={loadAppointments}>Try again</button>} />
      ) : appointments.length === 0 ? (
        <StatePanel title={t("No appointments yet")} message={t("Once you book a visit, it will appear here.")}
          action={<a className="btn-primary" href="/doctors">{t("Find a doctor")}</a>} />
      ) : (
        <div className="space-y-8">
          <section>
            <h2 className="mb-4 text-xl font-bold text-ink">{t("Upcoming")}</h2>
            <div className="space-y-3">{upcoming.length ? upcoming.map(renderAppointment) : <StatePanel title={t("Nothing scheduled")} message={t("You have no upcoming booked visits.")} />}</div>
          </section>
          <section>
            <h2 className="mb-4 text-xl font-bold text-ink">{t("Past and other appointments")}</h2>
            <div className="space-y-3">{past.length ? past.map(renderAppointment) : <StatePanel title={t("No past visits")} message={t("Completed visits and cancelled bookings will appear here.")} />}</div>
          </section>
        </div>
      )}
      {rescheduling && <RescheduleForm appointment={rescheduling} onClose={() => setRescheduling(null)} onComplete={async (message) => {
        setRescheduling(null);
        showToast(message === "Appointment rescheduled." ? t(message) : message);
        await loadAppointments();
      }} />}
      {history && <Modal title={t("Appointment history")} onClose={() => setHistory(null)}>
        <p className="mb-4 text-sm text-slate-500">{history.appointment.doctorName} · {formatDate(history.appointment.startTime)}</p>
        {history.error ? <p role="alert" className="text-sm text-rose-700">{history.error}</p>
          : history.entries === null ? <Loading label="Loading history..." />
            : history.entries.length ? <ol className="space-y-4">
              {history.entries.map((entry) => <li key={entry.id} className="border-l-2 border-teal-200 pl-4">
                <p className="font-semibold text-ink">{entry.fromStatus || "Created"} → {entry.toStatus}</p>
                <p className="mt-1 text-sm text-slate-500">{entry.note || "Status updated"} · {entry.actorEmail}</p>
                <p className="mt-1 text-xs text-slate-400">{new Date(entry.changedAt).toLocaleString()}</p>
              </li>)}
            </ol> : <StatePanel title="No history available" message="There are no recorded status changes yet." />}
      </Modal>}
    </div>
  );
}
