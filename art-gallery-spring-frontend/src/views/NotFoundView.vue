<script setup>
import { useRouter } from 'vue-router';
const router = useRouter();
</script>

<template>
  <div style="text-align: center; padding: 4rem 1rem">
    <div style="font-size: 3.5rem; font-weight: 700; color: var(--primary)">404</div>
    <h1 style="margin: 0.3rem 0">Page not found</h1>
    <p class="muted">The page you are looking for does not exist or has been moved.</p>
    <button class="btn btn-primary" @click="router.push({ name: 'home' })">Back to Home</button>
  </div>
</template>
