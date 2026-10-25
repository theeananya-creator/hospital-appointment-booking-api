import { lazy, Suspense } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import AppLayout from "./components/AppLayout";
import Loading from "./components/Loading";
import { useAuth } from "./context/AuthContext";
import ProtectedRoute from "./routes/ProtectedRoute";
import { usePreferences } from "./context/PreferencesContext";

const AdminDashboardPage = lazy(() => import("./pages/AdminDashboardPage"));
const AppointmentsPage = lazy(() => import("./pages/AppointmentsPage"));
const DoctorDashboardPage = lazy(() => import("./pages/DoctorDashboardPage"));
const DoctorDetailPage = lazy(() => import("./pages/DoctorDetailPage"));
const DoctorsPage = lazy(() => import("./pages/DoctorsPage"));
const LoginPage = lazy(() => import("./pages/LoginPage"));
const PatientProfilePage = lazy(() => import("./pages/PatientProfilePage"));
const RegisterPage = lazy(() => import("./pages/RegisterPage"));

function LoginRedirect() {
  const { user, loading } = useAuth();
  if (loading) return <Loading label="Restoring your session..." />;
  return <Navigate to={user ? "/doctors" : "/login"} replace />;
}

function NotFoundPage() {
  const { t } = usePreferences();
  return (
    <div className="py-16 text-center">
      <p className="text-sm font-bold uppercase tracking-wider text-brand">404</p>
      <h1 className="mt-2 text-3xl font-bold text-ink">{t("Page not found")}</h1>
      <p className="mt-2 text-sm text-slate-500">{t("The page you requested doesn't exist.")}</p>
      <a className="btn-primary mt-5" href="/doctors">{t("Browse doctors")}</a>
    </div>
  );
}

export default function App() {
  return (
    <Suspense fallback={<Loading label="Opening page..." />}>
      <Routes>
        <Route element={<AppLayout />}>
          <Route index element={<LoginRedirect />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
          <Route path="doctors" element={<DoctorsPage />} />
          <Route path="doctors/:id" element={<DoctorDetailPage />} />
          <Route element={<ProtectedRoute roles={["PATIENT"]} />}>
            <Route path="appointments" element={<AppointmentsPage />} />
            <Route path="profile" element={<PatientProfilePage />} />
          </Route>
          <Route element={<ProtectedRoute roles={["DOCTOR"]} />}>
            <Route path="doctor" element={<DoctorDashboardPage />} />
          </Route>
          <Route element={<ProtectedRoute roles={["ADMIN"]} />}>
            <Route path="admin" element={<AdminDashboardPage />} />
          </Route>
          <Route path="*" element={<NotFoundPage />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
