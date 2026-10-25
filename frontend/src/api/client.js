import axios from "axios";

const configuredBaseURL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
// Dev requests use Vite's same-origin proxy; production requests use the configured API URL.
const baseURL = import.meta.env.DEV ? "" : configuredBaseURL.replace(/\/$/, "");

export const api = axios.create({
  baseURL,
  headers: { "Content-Type": "application/json" },
});

let accessToken = null;
let refreshPromise = null;
let onAuthenticationFailure = () => {};

export function setAccessToken(token) {
  accessToken = token;
}

export function setAuthenticationFailureHandler(handler) {
  onAuthenticationFailure = handler;
}

function refreshAccessToken() {
  if (!refreshPromise) {
    const refreshToken = window.localStorage.getItem("refreshToken");
    if (!refreshToken) {
      onAuthenticationFailure();
      return Promise.reject(new Error("Your session has expired. Please sign in again."));
    }

    refreshPromise = axios
      .post(`${baseURL}/api/auth/refresh`, { refreshToken })
      .then(({ data }) => {
        accessToken = data.accessToken;
        // Refresh secrets rotate on every use, so persist the replacement immediately.
        window.localStorage.setItem("refreshToken", data.refreshToken);
        return data.accessToken;
      })
      .catch((error) => {
        accessToken = null;
        window.localStorage.removeItem("refreshToken");
        onAuthenticationFailure();
        throw error;
      })
      .finally(() => {
        refreshPromise = null;
      });
  }
  return refreshPromise;
}

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const isAuthRequest = originalRequest?.url?.includes("/api/auth/");

    if (error.response?.status !== 401 || !originalRequest || originalRequest._retry || isAuthRequest) {
      if (error.response?.status === 401 && originalRequest?._retry) {
        accessToken = null;
        window.localStorage.removeItem("refreshToken");
        onAuthenticationFailure();
      }
      return Promise.reject(error);
    }

    originalRequest._retry = true;
    try {
      const newToken = await refreshAccessToken();
      originalRequest.headers.Authorization = `Bearer ${newToken}`;
      return api(originalRequest);
    } catch (refreshError) {
      return Promise.reject(refreshError);
    }
  },
);

export async function restoreSession() {
  if (!window.localStorage.getItem("refreshToken")) return null;
  const token = await refreshAccessToken();
  return token;
}
