<script setup>
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from './stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const userMenuOpen = ref(false);

const showNav = computed(() => authStore.isAuthenticated);

const userRoleLabel = computed(() => {
  return authStore.isAdmin ? 'Admin account' : 'User account';
});

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value;
}

function closeUserMenu() {
  userMenuOpen.value = false;
}

async function logout() {
  closeUserMenu();
  await authStore.logout();
  router.push({ name: 'login' });
}
</script>

<template>
  <header v-if="showNav" class="nav">
    <div class="nav-inner">
      <RouterLink :to="{ name: 'home' }" class="brand" @click="closeUserMenu">
        <span class="brand-logo">🎨</span>
        <span>Art Gallery</span>
      </RouterLink>

      <nav class="nav-links" aria-label="Main navigation">
        <RouterLink :to="{ name: 'home' }" @click="closeUserMenu">
          Home
        </RouterLink>

        <RouterLink :to="{ name: 'artworks' }" @click="closeUserMenu">
          Artworks
        </RouterLink>

        <RouterLink :to="{ name: 'artists' }" @click="closeUserMenu">
          Artists
        </RouterLink>

        <RouterLink :to="{ name: 'exhibitions' }" @click="closeUserMenu">
          Exhibitions
        </RouterLink>

        <RouterLink :to="{ name: 'visitors' }" @click="closeUserMenu">
          Visitors
        </RouterLink>

        <RouterLink :to="{ name: 'reviews' }" @click="closeUserMenu">
          Reviews
        </RouterLink>
      </nav>

      <div class="user-menu">
        <button
          type="button"
          class="user-trigger"
          :class="{ admin: authStore.isAdmin }"
          @click="toggleUserMenu"
        >
          <span class="user-avatar" aria-hidden="true">
            <svg viewBox="0 0 24 24" role="img">
              <path
                d="M12 12.4c2.35 0 4.25-1.9 4.25-4.25S14.35 3.9 12 3.9s-4.25 1.9-4.25 4.25S9.65 12.4 12 12.4Zm0 2.1c-3.25 0-6.6 1.65-6.6 4.2v.55c0 .48.39.85.85.85h11.5c.46 0 .85-.37.85-.85v-.55c0-2.55-3.35-4.2-6.6-4.2Z"
              />
            </svg>
          </span>

          <span class="user-name">{{ authStore.username }}</span>
        </button>

        <div v-if="userMenuOpen" class="user-dropdown">
          <div class="user-dropdown-header">
            <span class="user-avatar dropdown-avatar" aria-hidden="true">
              <svg viewBox="0 0 24 24" role="img">
                <path
                  d="M12 12.4c2.35 0 4.25-1.9 4.25-4.25S14.35 3.9 12 3.9s-4.25 1.9-4.25 4.25S9.65 12.4 12 12.4Zm0 2.1c-3.25 0-6.6 1.65-6.6 4.2v.55c0 .48.39.85.85.85h11.5c.46 0 .85-.37.85-.85v-.55c0-2.55-3.35-4.2-6.6-4.2Z"
                />
              </svg>
            </span>

            <div>
              <strong>{{ authStore.username }}</strong>
              <small>{{ userRoleLabel }}</small>
            </div>
          </div>

          <button type="button" class="dropdown-logout" @click="logout">
            Logout
          </button>
        </div>
      </div>
    </div>
  </header>

  <main class="container">
    <RouterView v-if="authStore.ready" />
    <div v-else class="empty">Loading application...</div>
  </main>
</template>

<style scoped>
.brand {
  display: inline-flex;
  align-items: center;
  gap: 0.7rem;
  color: var(--ink);
  font-size: 1.05rem;
  font-weight: 950;
  text-decoration: none;
  white-space: nowrap;
}

.brand-logo {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 13px;
  background: linear-gradient(135deg, #8aa3ff, var(--primary));
  box-shadow: 0 12px 24px rgba(95, 125, 232, 0.22);
  font-size: 1.05rem;
}

.user-menu {
  position: relative;
  margin-left: auto;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
  min-height: 44px;
  padding: 0.4rem 0.85rem 0.4rem 0.5rem;
  border: 1px solid var(--border);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  color: var(--ink);
  font: inherit;
  font-weight: 900;
  cursor: pointer;
  box-shadow: var(--shadow-soft);
  transition:
    background 0.18s ease,
    border-color 0.18s ease,
    box-shadow 0.18s ease,
    transform 0.18s ease;
}

.user-trigger:hover {
  transform: translateY(-1px);
  border-color: #d7e1ff;
  background: #fbfcff;
  box-shadow: 0 14px 30px rgba(40, 55, 92, 0.1);
}

.user-trigger.admin .user-avatar {
  border-color: #f1d48a;
  background: #fff6dc;
  color: #8d6500;
}

.user-avatar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 1px solid #d7e1ff;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary-dark);
  flex: 0 0 auto;
}

.user-avatar svg {
  width: 18px;
  height: 18px;
  fill: currentColor;
}

.user-name {
  max-width: 130px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-dropdown {
  position: absolute;
  top: calc(100% + 0.65rem);
  right: 0;
  z-index: 20;
  min-width: 230px;
  padding: 0.75rem;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: var(--shadow);
}

.user-dropdown-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem;
  border: 1px solid var(--border);
  border-radius: 15px;
  background: rgba(248, 250, 255, 0.86);
}

.dropdown-avatar {
  width: 38px;
  height: 38px;
}

.user-dropdown-header strong {
  display: block;
  color: var(--ink);
  font-size: 0.95rem;
  font-weight: 950;
}

.user-dropdown-header small {
  display: block;
  margin-top: 0.15rem;
  color: var(--muted);
  font-size: 0.8rem;
  font-weight: 750;
}

.dropdown-logout {
  width: 100%;
  min-height: 42px;
  margin-top: 0.65rem;
  border: 1px solid #f4b8b8;
  border-radius: 13px;
  background: #fffafa;
  color: #cf3b3b;
  font: inherit;
  font-weight: 900;
  cursor: pointer;
}

.dropdown-logout:hover {
  background: #fff1f1;
  border-color: #ee9999;
  color: #b62828;
}

@media (max-width: 860px) {
  .nav-inner {
    gap: 0.75rem;
  }

  .nav-links {
    order: 3;
    width: 100%;
    overflow-x: auto;
    padding-bottom: 0.2rem;
  }

  .user-name {
    max-width: 92px;
  }
}
</style>
