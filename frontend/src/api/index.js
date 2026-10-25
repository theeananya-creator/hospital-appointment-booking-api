import { api } from "./client";

export const authApi = {
  register: (payload) => api.post("/api/auth/register", payload),
  login: (payload) => api.post("/api/auth/login", payload),
};

export const doctorApi = {
  search: (params) => api.get("/api/doctors", { params }),
  get: (id) => api.get(`/api/doctors/${id}`),
  slots: (doctorId, date) => api.get(`/api/doctors/${doctorId}/slots`, { params: { date } }),
  updateMine: (payload) => api.patch("/api/doctors/me", payload),
  setWorkingHours: (payload) => api.put("/api/doctors/me/working-hours", payload),
  addLeave: (payload) => api.post("/api/doctors/me/leaves", payload),
};

export const specializationApi = {
  list: () => api.get("/api/specializations"),
  create: (payload) => api.post("/api/specializations", payload),
};

export const patientApi = {
  getMine: () => api.get("/api/patients/me"),
  updateMine: (payload) => api.patch("/api/patients/me", payload),
};

export const appointmentApi = {
  book: (slotId) => api.post("/api/appointments", { slotId }),
  mine: () => api.get("/api/appointments/mine"),
  cancel: (id) => api.post(`/api/appointments/${id}/cancel`),
  reschedule: (id, newSlotId) => api.post(`/api/appointments/${id}/reschedule`, { newSlotId }),
  history: (id) => api.get(`/api/appointments/${id}/history`),
  updateStatus: (id, status, note) => api.patch(`/api/appointments/${id}/status`, { status, note }),
};

export const adminApi = {
  createDoctor: (payload) => api.post("/api/admin/doctors", payload),
  appointmentsPerDoctorMonth: () => api.get("/api/admin/reports/appointments-per-doctor-month"),
  cancellationRate: () => api.get("/api/admin/reports/cancellation-rate"),
  peakBookingHours: () => api.get("/api/admin/reports/peak-booking-hours"),
  topSpecializations: () => api.get("/api/admin/reports/top-specializations"),
};
