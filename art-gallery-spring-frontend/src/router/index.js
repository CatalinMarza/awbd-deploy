import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../stores/auth';

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
  { path: '/', name: 'home', component: () => import('../views/HomeView.vue') },

  { path: '/artworks', name: 'artworks', component: () => import('../views/ArtworksView.vue') },
  { path: '/artworks/new', name: 'artwork-new', component: () => import('../views/ArtworkFormView.vue'), meta: { admin: true } },
  { path: '/artworks/:id/edit', name: 'artwork-edit', component: () => import('../views/ArtworkFormView.vue'), props: true, meta: { admin: true } },

  { path: '/artists', name: 'artists', component: () => import('../views/ArtistsView.vue') },
  { path: '/artists/new', name: 'artist-new', component: () => import('../views/ArtistFormView.vue'), meta: { admin: true } },
  { path: '/artists/:id/edit', name: 'artist-edit', component: () => import('../views/ArtistFormView.vue'), props: true, meta: { admin: true } },

  { path: '/exhibitions', name: 'exhibitions', component: () => import('../views/ExhibitionsView.vue') },
  { path: '/exhibitions/new', name: 'exhibition-new', component: () => import('../views/ExhibitionFormView.vue'), meta: { admin: true } },
  { path: '/exhibitions/:id/edit', name: 'exhibition-edit', component: () => import('../views/ExhibitionFormView.vue'), props: true, meta: { admin: true } },
  { path: '/exhibitions/:id/artworks', name: 'exhibition-artworks', component: () => import('../views/ExhibitionArtworksView.vue'), props: true, meta: { admin: true } },

  { path: '/visitors', name: 'visitors', component: () => import('../views/VisitorsView.vue') },
  { path: '/visitors/new', name: 'visitor-new', component: () => import('../views/VisitorFormView.vue'), meta: { admin: true } },
  { path: '/visitors/:id/edit', name: 'visitor-edit', component: () => import('../views/VisitorFormView.vue'), props: true, meta: { admin: true } },

  { path: '/reviews', name: 'reviews', component: () => import('../views/ReviewsView.vue') },
  { path: '/reviews/new', name: 'review-new', component: () => import('../views/ReviewFormView.vue') },
  { path: '/reviews/:id/edit', name: 'review-edit', component: () => import('../views/ReviewFormView.vue'), props: true, meta: { admin: true } },

  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../views/NotFoundView.vue'), meta: { public: true } },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
});

/**
 * Global guard. Waits for the one-time session check (/auth/me) to finish,
 * then enforces authentication and the admin role where required.
 */
router.beforeEach(async (to) => {
  const authStore = useAuthStore();
  if (!authStore.ready) {
    await authStore.fetchMe();
  }

  if (to.meta.public) return true;

  if (!authStore.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }

  if (to.meta.admin && !authStore.isAdmin) {
    return { name: 'home' };
  }

  return true;
});

export default router;
