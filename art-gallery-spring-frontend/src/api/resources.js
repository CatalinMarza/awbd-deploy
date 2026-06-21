import http from './http';

/**
 * Unwraps the `ApiResponse` envelope and returns the `data` payload.
 */
const unwrap = (response) => response.data?.data;

/**
 * Factory for a standard CRUD resource backed by a `/api/{path}` controller.
 * List endpoints return a Spring `PageResponse`
 * ({ content, page, size, totalElements, totalPages, first, last }).
 */
export function createResource(path) {
  return {
    /** Paginated list. `params` accepts page, size and sort (e.g. "name,asc"). */
    list: (params = {}) => http.get(`/${path}`, { params }).then(unwrap),
    /** Convenience: fetch every row (used to populate FK dropdowns). */
    all: () =>
      http
        .get(`/${path}`, { params: { page: 0, size: 1000, sort: 'id,asc' } })
        .then((r) => unwrap(r).content),
    get: (id) => http.get(`/${path}/${id}`).then(unwrap),
    create: (body) => http.post(`/${path}`, body).then(unwrap),
    update: (id, body) => http.put(`/${path}/${id}`, body).then(unwrap),
    remove: (id) => http.delete(`/${path}/${id}`).then((r) => r.data),
  };
}

export const artworks = createResource('artworks');
export const artists = createResource('artists');
export const collections = createResource('collections');
export const locations = createResource('locations');
export const exhibitions = createResource('exhibitions');
export const exhibitors = createResource('exhibitors');
export const visitors = createResource('visitors');
export const staff = createResource('staff');
export const loans = createResource('loans');
export const insurances = createResource('insurances');
export const insurancePolicies = createResource('insurance-policies');
export const restorations = createResource('restorations');
export const reviews = createResource('reviews');
export const acquisitions = createResource('acquisitions');

/** Search helpers (only some controllers expose /search?q=). */
export const search = {
  artworks: (q, params = {}) =>
    http.get('/artworks/search', { params: { q, ...params } }).then(unwrap),
  exhibitions: (q, params = {}) =>
    http.get('/exhibitions/search', { params: { q, ...params } }).then(unwrap),
  artists: (q, params = {}) =>
    http.get('/artists/search', { params: { q, ...params } }).then(unwrap),
  visitors: (q, params = {}) =>
    http.get('/visitors/search', { params: { q, ...params } }).then(unwrap),
  staff: (q, params = {}) =>
    http.get('/staff/search', { params: { q, ...params } }).then(unwrap),
  exhibitors: (q, params = {}) =>
    http.get('/exhibitors/search', { params: { q, ...params } }).then(unwrap),
};

/** Manage the @ManyToMany link between exhibitions and artworks. */
export const exhibitionArtworks = {
  list: (exhibitionId) => http.get(`/exhibitions/${exhibitionId}/artworks`).then(unwrap),
  add: (exhibitionId, artworkId) =>
    http.post(`/exhibitions/${exhibitionId}/artworks/${artworkId}`).then((r) => r.data),
  remove: (exhibitionId, artworkId) =>
    http.delete(`/exhibitions/${exhibitionId}/artworks/${artworkId}`).then((r) => r.data),
};

/** Authentication endpoints (session-cookie based). */
export const auth = {
  /** Form-encoded login matching Spring Security's formLogin processing URL. */
  login: (username, password) => {
    const body = new URLSearchParams();
    body.append('username', username);
    body.append('password', password);
    return http
      .post('/auth/login', body, {
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      })
      .then(unwrap);
  },
  logout: () => http.post('/auth/logout').then((r) => r.data),
  me: () => http.get('/auth/me').then(unwrap),
};
