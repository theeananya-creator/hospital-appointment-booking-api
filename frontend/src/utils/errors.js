export function getErrorMessage(error) {
  const response = error?.response;
  const data = response?.data;

  if (data?.message) return data.message;
  if (data?.error) return data.error;
  if (response?.status === 409) return "This appointment conflicts with another booking. Please choose a different time.";
  if (error?.code === "ERR_NETWORK") {
    return "Cannot reach the API. Check that the backend is running and the API URL is correct.";
  }
  return error?.message || "Something went wrong. Please try again.";
}
