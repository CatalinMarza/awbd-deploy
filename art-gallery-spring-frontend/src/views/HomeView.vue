<script setup>
import { computed, onMounted, ref } from 'vue';
import { useAuthStore } from '../stores/auth';

const auth = useAuthStore();

const exhibitions = ref([]);
const loading = ref(false);
const error = ref('');

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const isAdmin = computed(() => auth.isAdmin);

const dashboardTitle = computed(() => {
  return isAdmin.value ? 'Gallery dashboard' : 'Gallery exhibitions';
});

const dashboardDescription = computed(() => {
  return isAdmin.value
    ? 'Quick overview of upcoming and recent exhibitions.'
    : 'Browse upcoming and recent gallery exhibitions.';
});

const sortedExhibitions = computed(() => {
  return [...exhibitions.value]
    .filter((item) => item.startDate)
    .sort((a, b) => new Date(a.startDate) - new Date(b.startDate));
});

const upcomingExhibition = computed(() => {
  const today = new Date();

  const future = sortedExhibitions.value.filter((item) => {
    return new Date(item.startDate) >= today;
  });

  return future[0] || sortedExhibitions.value[sortedExhibitions.value.length - 1] || null;
});

const visibleExhibitions = computed(() => {
  const today = new Date();

  const future = sortedExhibitions.value.filter((item) => {
    return new Date(item.startDate) >= today;
  });

  const past = sortedExhibitions.value
    .filter((item) => new Date(item.startDate) < today)
    .sort((a, b) => new Date(b.startDate) - new Date(a.startDate));

  const ordered = [...future, ...past];

  return ordered
    .filter((item) => item.id !== upcomingExhibition.value?.id)
    .slice(0, 4);
});

function formatDate(value) {
  if (!value) {
    return 'Not set';
  }

  return new Intl.DateTimeFormat('en-GB', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  }).format(new Date(value));
}

function extractExhibitions(payload) {
  if (Array.isArray(payload)) {
    return payload;
  }

  if (Array.isArray(payload?.content)) {
    return payload.content;
  }

  if (Array.isArray(payload?.data)) {
    return payload.data;
  }

  if (Array.isArray(payload?.data?.content)) {
    return payload.data.content;
  }

  return [];
}

async function loadExhibitions() {
  loading.value = true;
  error.value = '';

  try {
    const response = await fetch(`${apiBaseUrl}/exhibitions?page=0&size=10&sort=startDate,asc`, {
      credentials: 'include',
    });

    if (!response.ok) {
      throw new Error(`Request failed with status ${response.status}`);
    }

    const payload = await response.json();
    exhibitions.value = extractExhibitions(payload);
  } catch (err) {
    error.value = 'Exhibitions could not be loaded from the backend.';
    exhibitions.value = [];
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadExhibitions();
});
</script>

<template>
  <section class="dashboard-hero simple">
    <div>
      <h1>{{ dashboardTitle }}</h1>
      <p class="hero-text">{{ dashboardDescription }}</p>
    </div>
  </section>

  <section class="events-dashboard single-focus">
    <article class="event-feature-card">
      <div class="section-heading">
        <p class="eyebrow">Upcoming exhibition</p>
        <h2>Next gallery event</h2>
      </div>

      <div v-if="loading" class="empty compact">
        Loading exhibition data...
      </div>

      <div v-else-if="error" class="alert alert-error">
        {{ error }}
      </div>

      <RouterLink
        v-else-if="upcomingExhibition"
        class="event-row event-row-featured"
        :to="{ name: 'exhibitions' }"
      >
        <div>
          <strong>{{ upcomingExhibition.title }}</strong>

          <span>
            {{ formatDate(upcomingExhibition.startDate) }}
            —
            {{ formatDate(upcomingExhibition.endDate) }}
          </span>

          <small v-if="upcomingExhibition.exhibitorName">
            {{ upcomingExhibition.exhibitorName }}
          </small>
        </div>
      </RouterLink>

      <div v-else class="empty compact">
        No exhibitions available yet.
      </div>
    </article>

    <article class="events-list-card">
      <div class="section-heading">
        <p class="eyebrow">Schedule</p>
        <h2>Gallery exhibitions</h2>
      </div>

      <div v-if="loading" class="empty compact">
        Loading schedule...
      </div>

      <div v-else-if="visibleExhibitions.length" class="events-list">
        <RouterLink
          v-for="exhibition in visibleExhibitions"
          :key="exhibition.id"
          class="event-row"
          :to="{ name: 'exhibitions' }"
        >
          <div>
            <strong>{{ exhibition.title }}</strong>

            <span>
              {{ formatDate(exhibition.startDate) }}
              —
              {{ formatDate(exhibition.endDate) }}
            </span>

            <small v-if="exhibition.exhibitorName">
              {{ exhibition.exhibitorName }}
            </small>
          </div>
        </RouterLink>
      </div>

      <div v-else class="empty compact">
        No other exhibitions available.
      </div>
    </article>
  </section>
</template>