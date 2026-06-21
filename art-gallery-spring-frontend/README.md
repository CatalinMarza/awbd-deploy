# Art Gallery — Spring Boot Frontend (Vue 3)

Minimal single-page client for the Art Gallery Spring Boot REST API. Built fresh
to align 1:1 with the Java backend contract (no .NET / BI legacy code).

## Stack

- **Vue 3.5** (Composition API, `<script setup>`)
- **Pinia 2** — auth state store
- **Vue Router 4** — routing + auth/admin guards
- **Axios** — HTTP client with `withCredentials` (session-cookie auth)
- **Vite 5** — dev server & build

## Prerequisites

- Node.js 18+ and npm
- The Spring Boot backend running on `http://localhost:8080`
  (see `../art-gallery-spring-backend/README.md`)

## Configure

The API base URL is read from `.env`:

```
VITE_API_BASE_URL=http://localhost:8080/api
```

The backend CORS config already allows `http://localhost:5173` with credentials.

## Run

```bash
npm install
npm run dev      # dev server at http://localhost:5173
npm run build    # production bundle in dist/
npm run preview  # serve the production build
```

## Demo credentials

| Username | Password   | Roles                |
|----------|------------|----------------------|
| `admin`  | `admin123` | `ROLE_ADMIN`, `ROLE_USER` |
| `user`   | `user123`  | `ROLE_USER`          |

Admins can create/edit/delete every entity. Regular users have read access and
can submit reviews.

## Structure

```
src/
  api/
    http.js         Axios instance + ApiResponse error normaliser
    resources.js    CRUD resource factory, auth, search, M2M helpers
  stores/
    auth.js         Pinia store (user, isAdmin, login/logout/fetchMe)
  router/
    index.js        Routes + beforeEach guard (auth + admin)
  components/
    DataTable.vue   Reusable table with server-side pagination + sorting
  views/
    LoginView, HomeView, NotFoundView (custom 404)
    Artists / Artworks / Exhibitions / Visitors / Reviews (list + form)
    ExhibitionArtworksView  Manage the artwork ↔ exhibition many-to-many link
  App.vue           Layout (nav + <router-view>)
  main.js           App bootstrap
```

## Features mapped to requirements

- **Views & validation (#5):** entity list/detail/forms; server-side Bean
  Validation errors surfaced per field; JSON error envelope handled centrally;
  custom 404 page.
- **Pagination & sorting (#7):** `DataTable` drives `page`, `size` and `sort`
  query params against the backend `PageResponse`.
- **Security (#8):** session-cookie login, route guards, admin-only write
  actions, automatic redirect to `/login` on unauthenticated access.
