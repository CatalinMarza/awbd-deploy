<script setup>
import { ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '../stores/auth';
import { normalizeError } from '../api/http';

const auth = useAuthStore();
const route = useRoute();
const router = useRouter();

const username = ref('');
const password = ref('');
const error = ref(null);
const loading = ref(false);

async function submit() {
  error.value = null;
  loading.value = true;
  try {
    await auth.login(username.value, password.value);
    const redirect = route.query.redirect || { name: 'home' };
    router.push(redirect);
  } catch (e) {
    const n = normalizeError(e);
    error.value = n.status === 401 ? 'Invalid username or password.' : n.message;
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div style="max-width: 380px; margin: 4rem auto">
    <div class="card">
      <h1 style="margin-top: 0; font-size: 1.3rem">Sign in</h1>
      <p class="muted" style="margin-top: -0.4rem; font-size: 0.85rem">Art Gallery Management</p>

      <div v-if="error" class="alert alert-error">{{ error }}</div>

      <form @submit.prevent="submit">
        <div class="field">
          <label for="username">Username</label>
          <input id="username" v-model="username" autocomplete="username" required />
        </div>
        <div class="field">
          <label for="password">Password</label>
          <input id="password" v-model="password" type="password" autocomplete="current-password" required />
        </div>
        <button class="btn btn-primary" style="width: 100%" :disabled="loading">
          {{ loading ? 'Signing in…' : 'Sign in' }}
        </button>
      </form>

    </div>
  </div>
</template>
