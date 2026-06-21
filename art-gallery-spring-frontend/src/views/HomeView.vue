<script setup>
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();

const sections = [
  { name: 'Artworks', to: 'artworks', desc: 'Central catalogue of artworks and their attributes.' },
  { name: 'Artists', to: 'artists', desc: 'Creators linked to one or more artworks.' },
  { name: 'Exhibitions', to: 'exhibitions', desc: 'Events showcasing many artworks (many-to-many).' },
  { name: 'Visitors', to: 'visitors', desc: 'Registered gallery visitors and memberships.' },
  { name: 'Reviews', to: 'reviews', desc: 'Visitor reviews for artworks and exhibitions.' },
];
</script>

<template>
  <div class="page-header">
    <h1>Welcome, {{ auth.username }}</h1>
    <span v-if="auth.isAdmin" class="badge">Administrator</span>
  </div>

  <p class="muted">
    Art Gallery management — Spring Boot REST API with a Vue 3 single-page client.
    {{ auth.isAdmin ? 'You can create, edit and delete records.' : 'You have read access; reviews can be added.' }}
  </p>

  <div class="grid-2" style="gap: 1rem; margin-top: 1rem">
    <router-link v-for="s in sections" :key="s.to" :to="{ name: s.to }" class="card" style="color: inherit">
      <h3 style="margin: 0 0 0.3rem">{{ s.name }}</h3>
      <span class="muted" style="font-size: 0.88rem">{{ s.desc }}</span>
    </router-link>
  </div>
</template>
