<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from './stores/auth';

const auth = useAuthStore();
const router = useRouter();
const showNav = computed(() => auth.isAuthenticated);

async function onLogout() {
  await auth.logout();
  router.push({ name: 'login' });
}
</script>

<template>
  <header v-if="showNav" class="nav">
    <div class="nav-inner">
      <span class="nav-brand">🎨 Art Gallery</span>
      <nav class="nav-links">
        <router-link :to="{ name: 'home' }">Home</router-link>
        <router-link :to="{ name: 'artworks' }">Artworks</router-link>
        <router-link :to="{ name: 'artists' }">Artists</router-link>
        <router-link :to="{ name: 'exhibitions' }">Exhibitions</router-link>
        <router-link :to="{ name: 'visitors' }">Visitors</router-link>
        <router-link :to="{ name: 'reviews' }">Reviews</router-link>
      </nav>
      <span class="spacer"></span>
      <span class="muted" style="font-size: 0.85rem">
        {{ auth.username }}
        <span v-if="auth.isAdmin" class="badge">ADMIN</span>
      </span>
      <button class="btn btn-sm" @click="onLogout">Logout</button>
    </div>
  </header>

  <main class="container">
    <router-view v-if="auth.ready" />
    <div v-else class="empty">Loading…</div>
  </main>
</template>
