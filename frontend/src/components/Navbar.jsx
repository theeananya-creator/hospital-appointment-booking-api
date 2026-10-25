import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { usePreferences } from "../context/PreferencesContext";
import { useToast } from "../context/ToastContext";

const linkStyle = ({ isActive }) =>
  `rounded-lg px-3 py-2 text-sm font-medium transition ${
    isActive ? "bg-teal-50 text-brand" : "text-slate-600 hover:bg-slate-100 hover:text-ink"
  }`;

export default function Navbar() {
  const { user, logout } = useAuth();
  const { theme, toggleTheme, language, setLanguage, t } = usePreferences();
  const { enableNotifications } = useToast();
  const navigate = useNavigate();
  const role = user?.role;

  function signOut() {
    logout();
    navigate("/login");
  }

  return (
    <header className="sticky top-0 z-30 border-b border-slate-200 bg-white/95 backdrop-blur">
      <nav className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-3 px-4 py-3 sm:px-6">
        <Link to="/doctors" className="flex items-center gap-2 text-lg font-bold tracking-tight text-ink">
          <span className="grid h-9 w-9 place-items-center rounded-xl bg-brand text-sm text-white">C+</span>
          Carepoint
        </Link>
        <div className="flex flex-wrap items-center gap-1">
          <NavLink to="/doctors" className={linkStyle}>{t("Find doctors")}</NavLink>
          {role === "PATIENT" && <>
            <NavLink to="/appointments" className={linkStyle}>{t("Appointments")}</NavLink>
            <NavLink to="/profile" className={linkStyle}>{t("My profile")}</NavLink>
          </>}
          {role === "DOCTOR" && <NavLink to="/doctor" className={linkStyle}>{t("Doctor dashboard")}</NavLink>}
          {role === "ADMIN" && <NavLink to="/admin" className={linkStyle}>{t("Admin dashboard")}</NavLink>}
          <label className="sr-only" htmlFor="languagePicker">{t("Language")}</label>
          <select id="languagePicker" className="field !w-auto !py-2" value={language}
            onChange={(event) => setLanguage(event.target.value)}>
            <option value="en">English</option>
            <option value="hi">हिन्दी</option>
            <option value="te">తెలుగు</option>
          </select>
          <button type="button" className="btn-secondary !px-3 !py-2" onClick={toggleTheme}>
            {t(theme === "light" ? "Dark theme" : "Light theme")}
          </button>
          {user?.role === "PATIENT" && "Notification" in window && Notification.permission !== "granted" && (
            <button type="button" className="btn-secondary !px-3 !py-2" onClick={enableNotifications}>
              {t("Enable alerts")}
            </button>
          )}
          {user ? (
            <>
              <span className="hidden px-2 text-xs text-slate-500 sm:inline">{user.email}</span>
              <button className="btn-secondary !px-3 !py-2" onClick={signOut}>{t("Sign out")}</button>
            </>
          ) : (
            <>
              <NavLink to="/login" className={linkStyle}>{t("Sign in")}</NavLink>
              <Link to="/register" className="btn-primary !px-3 !py-2">{t("Create account")}</Link>
            </>
          )}
        </div>
      </nav>
    </header>
  );
}
