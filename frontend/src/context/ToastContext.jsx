import { createContext, useCallback, useContext, useMemo, useState } from "react";

const ToastContext = createContext(null);

export function ToastProvider({ children }) {
  const [toast, setToast] = useState(null);

  const showToast = useCallback((message, type = "success") => {
    setToast({ message, type });
    window.setTimeout(() => setToast(null), 4500);
  }, []);

  const enableNotifications = useCallback(async () => {
    if (!("Notification" in window)) {
      showToast("Browser notifications are not supported.", "error");
      return;
    }
    const permission = await Notification.requestPermission();
    showToast(permission === "granted" ? "Browser alerts are enabled." : "Browser alerts were not enabled.",
      permission === "granted" ? "success" : "error");
  }, [showToast]);

  const notify = useCallback((title, body) => {
    if ("Notification" in window && Notification.permission === "granted") {
      new Notification(title, { body });
    }
  }, []);

  const value = useMemo(() => ({ showToast, enableNotifications, notify }),
    [showToast, enableNotifications, notify]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      {toast && (
        <div
          role="status"
          className={`fixed bottom-5 right-5 z-50 max-w-sm rounded-xl px-4 py-3 text-sm font-medium text-white shadow-card ${
            toast.type === "error" ? "bg-rose-700" : "bg-ink"
          }`}
        >
          {toast.message}
        </div>
      )}
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (!context) throw new Error("useToast must be used inside ToastProvider");
  return context;
}
