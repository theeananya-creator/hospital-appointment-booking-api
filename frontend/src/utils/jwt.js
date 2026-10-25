export function decodeJwtPayload(token) {
  try {
    const payload = token.split(".")[1];
    const base64 = payload.replace(/-/g, "+").replace(/_/g, "/");
    return JSON.parse(window.atob(base64));
  } catch {
    return null;
  }
}

export function userFromToken(token) {
  const claims = decodeJwtPayload(token);
  if (!claims) return null;
  return {
    email: claims.sub,
    role: claims.role,
    id: claims.uid,
  };
}
