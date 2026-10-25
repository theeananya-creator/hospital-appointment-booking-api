export default function Loading({ label = "Loading..." }) {
  return (
    <div className="flex min-h-48 items-center justify-center gap-3 text-sm text-slate-500" role="status">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-slate-200 border-t-brand" />
      {label}
    </div>
  );
}
