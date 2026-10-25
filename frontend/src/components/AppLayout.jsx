import { Outlet } from "react-router-dom";
import Navbar from "./Navbar";
import { usePreferences } from "../context/PreferencesContext";

export default function AppLayout() {
  const { theme, language, t } = usePreferences();
  return (
    <div className="min-h-screen bg-canvas" data-theme={theme} lang={language}>
      <Navbar />
      <main className="mx-auto max-w-7xl px-4 py-8 sm:px-6 sm:py-10">
        <Outlet />
      </main>
      <footer className="border-t border-slate-200 bg-white px-4 py-6 text-center text-xs text-slate-500">
        <p>Carepoint · {t("Your care, thoughtfully scheduled")}</p>
        <p className="mt-2">
          Designed & developed by{" "}
          <a className="font-semibold text-brand hover:underline" href="https://github.com/theeananya-creator"
            target="_blank" rel="noreferrer">Ananya Konda</a> · 2026
        </p>
      </footer>
    </div>
  );
}
