import axios from 'axios';

/**
 * Axios instance pointed at the Spring Boot API.
 *
 * `withCredentials: true` is essential: the backend authenticates with a
 * session cookie (JSESSIONID) plus an optional remember-me cookie, so the
 * browser must send credentials on every cross-origin request. The backend
 * CORS config allows http://localhost:5173 with credentials.
 */
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api',
  withCredentials: true,
  timeout: 15000,
  headers: {
    Accept: 'application/json',
  },
});

/**
 * Normalises an Axios error into a plain object using the backend's
 * `ApiResponse` envelope: { success, status, message, data, errors }.
 *  - `fieldErrors` maps a field name to its validation message (from `data`).
 *  - `messages` is the flat list of human-readable errors.
 */
export function normalizeError(error) {
  const res = error.response;
  const body = res?.data;
  const fieldErrors =
    body?.data && typeof body.data === 'object' && !Array.isArray(body.data)
      ? body.data
      : {};
  return {
    status: res?.status ?? 0,
    message: body?.message || error.message || 'Unexpected error',
    messages: Array.isArray(body?.errors) ? body.errors : [],
    fieldErrors,
  };
}

export default http;
