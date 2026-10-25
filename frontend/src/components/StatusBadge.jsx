const styles = {
  BOOKED: "bg-sky-50 text-sky-700",
  CANCELLED: "bg-rose-50 text-rose-700",
  COMPLETED: "bg-emerald-50 text-emerald-700",
  NO_SHOW: "bg-amber-50 text-amber-700",
};

export default function StatusBadge({ status }) {
  return (
    <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${styles[status] || "bg-slate-100 text-slate-600"}`}>
      {status?.replaceAll("_", " ") || "UNKNOWN"}
    </span>
  );
}
