import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { appointmentApi, doctorApi } from "../api";
import Loading from "../components/Loading";
import PageHeader from "../components/PageHeader";
import StatePanel from "../components/StatePanel";
import { useAuth } from "../context/AuthContext";
import { useToast } from "../context/ToastContext";
import { getErrorMessage } from "../utils/errors";
import { dateInputValue, tomorrowInputValue } from "../utils/date";
import { usePreferences } from "../context/PreferencesContext";

function localTime(dateTime) {
  return new Date(dateTime).toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
}

export default function DoctorDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const { t } = usePreferences();
  const navigate = useNavigate();
  const { showToast, notify } = useToast();
  const [doctor, setDoctor] = useState(null);
  const [date, setDate] = useState(tomorrowInputValue());
  const [slots, setSlots] = useState([]);
  const [loadingDoctor, setLoadingDoctor] = useState(true);
  const [loadingSlots, setLoadingSlots] = useState(true);
  const [error, setError] = useState("");
  const [bookingId, setBookingId] = useState("");
  const [lastUpdated, setLastUpdated] = useState(null);

  const loadSlots = useCallback(async () => {
    const { data } = await doctorApi.slots(id, date);
    return data;
  }, [id, date]);

  useEffect(() => {
    let active = true;
    setLoadingDoctor(true);
    doctorApi.get(id)
      .then(({ data }) => { if (active) setDoctor(data); })
      .catch((requestError) => { if (active) setError(getErrorMessage(requestError)); })
      .finally(() => { if (active) setLoadingDoctor(false); });
    return () => { active = false; };
  }, [id]);

  useEffect(() => {
    let active = true;
    setLoadingSlots(true);
    setError("");
    loadSlots()
      .then((data) => {
        if (active) {
          setSlots(data);
          setLastUpdated(new Date());
        }
      })
      .catch((requestError) => { if (active) setError(getErrorMessage(requestError)); })
      .finally(() => { if (active) setLoadingSlots(false); });
    return () => { active = false; };
  }, [id, date]);

  useEffect(() => {
    let active = true;
    const refresh = async () => {
      if (document.visibilityState !== "visible") return;
      try {
        const data = await loadSlots();
        if (active) {
          setSlots(data);
          setLastUpdated(new Date());
          setError("");
        }
      } catch (requestError) {
        if (active) setError(getErrorMessage(requestError));
      }
    };
    const timer = window.setInterval(refresh, 10_000);
    document.addEventListener("visibilitychange", refresh);
    return () => {
      active = false;
      window.clearInterval(timer);
      document.removeEventListener("visibilitychange", refresh);
    };
  }, [loadSlots]);

  async function book(slot) {
    if (user?.role !== "PATIENT") {
      showToast(t("Sign in with a patient account to book an appointment."), "error");
      navigate("/login");
      return;
    }
    setBookingId(slot.id);
    try {
      await appointmentApi.book(slot.id);
      showToast(t("Your appointment is booked. Confirmation email queued."));
      notify(t("Appointment confirmed"), `${doctor.fullName} · ${localTime(slot.startTime)}`);
      setSlots((current) => current.filter((item) => item.id !== slot.id));
    } catch (requestError) {
      const message = getErrorMessage(requestError);
      if (requestError.response?.status === 409) {
        showToast(message || t("This slot was just booked. Choose another time."), "error");
        try {
          const refreshedSlots = await loadSlots();
          setSlots(refreshedSlots);
          setLastUpdated(new Date());
        } catch (refreshError) {
          setError(getErrorMessage(refreshError));
        }
      } else {
        showToast(message, "error");
      }
    } finally {
      setBookingId("");
    }
  }

  if (loadingDoctor) return <Loading label="Loading doctor profile..." />;
  if (!doctor) return <StatePanel title="Doctor unavailable" message={error || "We couldn't find this doctor."} action={<Link className="btn-secondary" to="/doctors">Back to doctors</Link>} />;

  return (
    <div>
      <PageHeader eyebrow={doctor.specialization || "Care team"} title={doctor.fullName || "Doctor profile"}
        description={doctor.bio || "Choose an available time to schedule a visit."}>
        <Link className="btn-secondary" to="/doctors">All doctors</Link>
      </PageHeader>
      <div className="mb-7 grid gap-4 sm:grid-cols-3">
        <div className="rounded-2xl bg-white p-5 shadow-card"><p className="text-xs uppercase tracking-wider text-slate-400">{t("Experience")}</p><p className="mt-2 text-xl font-bold">{doctor.experienceYears ?? "—"} years</p></div>
        <div className="rounded-2xl bg-white p-5 shadow-card"><p className="text-xs uppercase tracking-wider text-slate-400">{t("Consultation fee")}</p><p className="mt-2 text-xl font-bold">{doctor.consultationFee ?? "—"}</p></div>
        <div className="rounded-2xl bg-white p-5 shadow-card"><p className="text-xs uppercase tracking-wider text-slate-400">{t("Availability")}</p><p className="mt-2 text-xl font-bold">{doctor.available === false ? t("Not accepting visits") : t("Appointments open")}</p></div>
      </div>
      <section className="rounded-3xl bg-white p-5 shadow-card sm:p-7">
        <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
          <div>
            <h2 className="text-xl font-bold text-ink">{t("Available appointment times")}</h2>
            <p className="mt-1 text-sm text-slate-500">{t("Choose a date to see open slots.")}</p>
            <p className="mt-2 text-xs text-brand" aria-live="polite">● {t("Live availability")} · {t("Updates every 10 seconds")}{lastUpdated && ` · ${lastUpdated.toLocaleTimeString()}`}</p>
          </div>
          <div><label htmlFor="appointmentDate" className="label">{t("Appointment date")}</label><input id="appointmentDate" className="field" type="date" min={dateInputValue()} value={date} onChange={(event) => setDate(event.target.value)} /></div>
        </div>
        {loadingSlots ? <Loading label={t("Loading available times...")} /> : error ? (
          <div className="mt-5 rounded-xl bg-rose-50 p-4 text-sm text-rose-700" role="alert">{error}</div>
        ) : slots.length ? (
          <div className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
            {slots.map((slot) => (
              <button key={slot.id} type="button" onClick={() => book(slot)} disabled={Boolean(bookingId)}
                className="rounded-xl border border-slate-200 px-3 py-3 text-sm font-semibold text-ink hover:border-brand hover:bg-teal-50 disabled:opacity-50">
                {bookingId === slot.id ? t("Booking...") : localTime(slot.startTime)}
              </button>
            ))}
          </div>
        ) : <div className="mt-5"><StatePanel title={t("No open times for this date")} message={t("Choose another date to check availability.")} /></div>}
      </section>
    </div>
  );
}
