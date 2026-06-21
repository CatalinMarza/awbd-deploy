<script setup>
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from './stores/auth';

const auth = useAuthStore();
const router = useRouter();

const userMenuOpen = ref(false);

const showNav = computed(() => auth.isAuthenticated);

const roleLabel = computed(() => {
  return auth.isAdmin ? 'Admin account' : 'User account';
});

function toggleUserMenu() {
  userMenuOpen.value = !userMenuOpen.value;
}

function closeUserMenu() {
  userMenuOpen.value = false;
}

async function onLogout() {
  closeUserMenu();
  await auth.logout();
  router.push({ name: 'login' });
}
</script>

<template>
  <header v-if="showNav" class="nav">
    <div class="nav-inner">
      <RouterLink :to="{ name: 'home' }" class="nav-brand" @click="closeUserMenu">
        Art Gallery
      </RouterLink>

      <nav class="nav-links" aria-label="Main navigation">
        <RouterLink :to="{ name: 'home' }" @click="closeUserMenu">Home</RouterLink>
        <RouterLink :to="{ name: 'artworks' }" @click="closeUserMenu">Artworks</RouterLink>
        <RouterLink :to="{ name: 'artists' }" @click="closeUserMenu">Artists</RouterLink>
        <RouterLink :to="{ name: 'exhibitions' }" @click="closeUserMenu">Exhibitions</RouterLink>
        <RouterLink :to="{ name: 'visitors' }" @click="closeUserMenu">Visitors</RouterLink>
        <RouterLink :to="{ name: 'reviews' }" @click="closeUserMenu">Reviews</RouterLink>
      </nav>

      <span class="spacer"></span>

      <div class="user-menu">
        <button class="user-trigger" type="button" @click="toggleUserMenu">
          <span class="user-name">{{ auth.username }}</span>
          <span class="user-chevron" :class="{ open: userMenuOpen }">⌄</span>
        </button>

        <div v-if="userMenuOpen" class="user-dropdown compact">
          <div class="user-dropdown-header compact">
            <div>
              <strong>{{ auth.username }}</strong>
              <small>{{ roleLabel }}</small>
            </div>
          </div>

          <button class="dropdown-logout" type="button" @click="onLogout">
            Logout
          </button>
        </div>
      </div>
    </div>
  </header>

  <main class="container">
    <RouterView v-if="auth.ready" />
    <div v-else class="empty">Loading application...</div>
  </main>
</template>