import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import Loading from "../components/Loading";
import PageHeader from "../components/PageHeader";
import StatePanel from "../components/StatePanel";
import { doctorApi, specializationApi } from "../api";
import { getErrorMessage } from "../utils/errors";
import { usePreferences } from "../context/PreferencesContext";

function readError(error) {
  return getErrorMessage(error);
}

export default function DoctorsPage() {
  const { t } = usePreferences();
  const [filters, setFilters] = useState({ name: "", specializationId: "", minFee: "", maxFee: "" });
  const [specializations, setSpecializations] = useState([]);
  const [result, setResult] = useState(null);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [searchKey, setSearchKey] = useState(0);

  useEffect(() => {
    specializationApi.list()
      .then(({ data }) => setSpecializations(data))
      .catch((requestError) => setError(readError(requestError)));
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    setError("");
    const params = { page, size: 9, sort: "fullName,asc" };
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== "") params[key] = value;
    });
    doctorApi.search(params)
      .then(({ data }) => { if (active) setResult(data); })
      .catch((requestError) => { if (active) setError(readError(requestError)); })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [filters, page, searchKey]);

  function submitSearch(event) {
    event.preventDefault();
    setPage(0);
    setSearchKey((current) => current + 1);
  }

  function clearFilters() {
    setFilters({ name: "", specializationId: "", minFee: "", maxFee: "" });
    setPage(0);
    setSearchKey((current) => current + 1);
  }

  return (
    <div>
      <PageHeader eyebrow={t("Find your care team")} title={t("Meet our doctors")} description={t("Search trusted specialists and find a time that works for you.")} />
      <form onSubmit={submitSearch} className="mb-8 grid gap-3 rounded-2xl bg-white p-4 shadow-card md:grid-cols-6">
        <input className="field md:col-span-2" aria-label={t("Doctor name")} placeholder={t("Doctor name")}
          value={filters.name} onChange={(event) => setFilters({ ...filters, name: event.target.value })} />
        <select className="field md:col-span-2" aria-label="Specialization" value={filters.specializationId}
          onChange={(event) => setFilters({ ...filters, specializationId: event.target.value })}>
          <option value="">{t("All specializations")}</option>
          {specializations.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}
        </select>
        <input className="field" aria-label={t("Min fee")} type="number" min="0" step="0.01" placeholder={t("Min fee")}
          value={filters.minFee} onChange={(event) => setFilters({ ...filters, minFee: event.target.value })} />
        <input className="field" aria-label={t("Max fee")} type="number" min="0" step="0.01" placeholder={t("Max fee")}
          value={filters.maxFee} onChange={(event) => setFilters({ ...filters, maxFee: event.target.value })} />
        <div className="flex gap-2 md:col-span-6">
          <button className="btn-primary" type="submit">{t("Search doctors")}</button>
          <button className="btn-secondary" type="button" onClick={clearFilters}>{t("Clear")}</button>
        </div>
      </form>

      {loading ? <Loading label={t("Finding doctors...")} /> : error ? (
        <StatePanel title={t("We couldn't load doctors")} message={error} action={<button className="btn-secondary" onClick={() => setSearchKey((key) => key + 1)}>{t("Try again")}</button>} />
      ) : result?.content?.length ? (
        <>
          <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {result.content.map((doctor) => (
              <article key={doctor.id} className="flex flex-col rounded-2xl bg-white p-5 shadow-card">
                <div className="flex items-start gap-4">
                  <div className="grid h-12 w-12 shrink-0 place-items-center rounded-2xl bg-teal-50 font-bold text-brand">
                    {doctor.fullName?.split(" ").map((part) => part[0]).slice(0, 2).join("") || "Dr"}
                  </div>
                  <div className="min-w-0">
                    <h2 className="truncate text-lg font-bold text-ink">{doctor.fullName}</h2>
                    <p className="text-sm text-brand">{doctor.specialization}</p>
                  </div>
                </div>
                {doctor.bio && <p className="mt-4 line-clamp-3 text-sm leading-6 text-slate-500">{doctor.bio}</p>}
                <div className="mt-5 flex flex-wrap gap-2 text-xs text-slate-500">
                  <span className="rounded-full bg-slate-100 px-2.5 py-1">{doctor.experienceYears} years experience</span>
                  <span className="rounded-full bg-slate-100 px-2.5 py-1">Consultation {doctor.consultationFee}</span>
                </div>
                <Link className="btn-primary mt-5 w-full" to={`/doctors/${doctor.id}`}>{t("View profile & slots")}</Link>
              </article>
            ))}
          </div>
          <div className="mt-7 flex items-center justify-between">
            <p className="text-sm text-slate-500">
              Page {(result.number ?? page) + 1} of {Math.max(result.totalPages || 1, 1)} · {result.totalElements ?? result.content.length} doctors
            </p>
            <div className="flex gap-2">
              <button className="btn-secondary" disabled={result.first || page === 0} onClick={() => setPage((current) => Math.max(0, current - 1))}>{t("Previous")}</button>
              <button className="btn-secondary" disabled={result.last || page + 1 >= (result.totalPages || 0)} onClick={() => setPage((current) => current + 1)}>{t("Next")}</button>
            </div>
          </div>
        </>
      ) : <StatePanel title={t("No doctors found")} message={t("Try changing your search or clearing the filters.")} action={<button className="btn-secondary" onClick={clearFilters}>{t("Clear filters")}</button>} />}
    </div>
  );
}
