import { defineStore } from 'pinia';
import { auth } from '../api/resources';

/**
 * Holds the authenticated user. The actual credential lives in the
 * session cookie managed by the browser; here we only mirror identity/roles
 * so the UI can show/hide admin actions and guard routes.
 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    ready: false, // becomes true once the initial /auth/me check completes
  }),
  getters: {
    isAuthenticated: (state) => state.user !== null,
    roles: (state) => state.user?.roles ?? [],
    isAdmin: (state) => (state.user?.roles ?? []).includes('ROLE_ADMIN'),
    username: (state) => state.user?.username ?? null,
  },
  actions: {
    async fetchMe() {
      try {
        this.user = await auth.me();
      } catch {
        this.user = null;
      } finally {
        this.ready = true;
      }
    },
    async login(username, password) {
      this.user = await auth.login(username, password);
      return this.user;
    },
    async logout() {
      try {
        await auth.logout();
      } finally {
        this.user = null;
      }
    },
  },
});
