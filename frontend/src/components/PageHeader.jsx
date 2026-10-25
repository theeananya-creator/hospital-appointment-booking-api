import { usePreferences } from "../context/PreferencesContext";

export default function PageHeader({ eyebrow, title, description, children }) {
  const { t } = usePreferences();
  return (
    <div className="mb-7 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
      <div>
        {eyebrow && <p className="mb-2 text-xs font-bold uppercase tracking-[0.16em] text-brand">{t(eyebrow)}</p>}
        <h1 className="text-3xl font-bold tracking-tight text-ink sm:text-4xl">{t(title)}</h1>
        {description && <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">{t(description)}</p>}
      </div>
      {children}
    </div>
  );
}
