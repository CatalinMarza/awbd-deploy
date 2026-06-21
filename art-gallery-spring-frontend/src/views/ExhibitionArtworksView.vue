<script setup>
import { ref, computed, onMounted } from 'vue';
import { exhibitions, artworks, exhibitionArtworks } from '../api/resources';
import { useAuthStore } from '../stores/auth';
import { normalizeError } from '../api/http';

const props = defineProps({ id: { type: String, required: true } });
const auth = useAuthStore();

const exhibition = ref(null);
const linked = ref([]);
const allArtworks = ref([]);
const selectedId = ref(null);
const error = ref(null);
const loading = ref(true);

const available = computed(() => {
  const linkedIds = new Set(linked.value.map((a) => a.id));
  return allArtworks.value.filter((a) => !linkedIds.has(a.id));
});

async function refresh() {
  linked.value = await exhibitionArtworks.list(props.id);
}

onMounted(async () => {
  try {
    [exhibition.value, allArtworks.value] = await Promise.all([
      exhibitions.get(props.id),
      artworks.all(),
    ]);
    await refresh();
  } catch (e) {
    error.value = normalizeError(e).message;
  } finally {
    loading.value = false;
  }
});

async function add() {
  if (!selectedId.value) return;
  error.value = null;
  try {
    await exhibitionArtworks.add(props.id, selectedId.value);
    selectedId.value = null;
    await refresh();
  } catch (e) {
    error.value = normalizeError(e).message;
  }
}

async function remove(artworkId) {
  error.value = null;
  try {
    await exhibitionArtworks.remove(props.id, artworkId);
    await refresh();
  } catch (e) {
    error.value = normalizeError(e).message;
  }
}
</script>

<template>
  <div class="page-header">
    <h1>Exhibition Artworks</h1>
    <router-link class="btn" :to="{ name: 'exhibitions' }">← Back</router-link>
  </div>

  <p v-if="exhibition" class="muted">
    Managing artworks for <strong>{{ exhibition.title }}</strong> (many-to-many).
  </p>

  <div v-if="error" class="alert alert-error">{{ error }}</div>

  <div v-if="auth.isAdmin" class="card" style="margin-bottom: 1rem">
    <div class="toolbar">
      <select v-model="selectedId" style="max-width: 320px">
        <option :value="null" disabled>— Select an artwork to add —</option>
        <option v-for="a in available" :key="a.id" :value="a.id">{{ a.title }}</option>
      </select>
      <button class="btn btn-primary" :disabled="!selectedId" @click="add">Add to exhibition</button>
    </div>
  </div>

  <div class="card">
    <div v-if="loading" class="empty">Loading…</div>
    <table v-else>
      <thead>
        <tr>
          <th>ID</th><th>Title</th><th>Artist</th><th>Year</th>
          <th v-if="auth.isAdmin" style="text-align: right">Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="a in linked" :key="a.id">
          <td>{{ a.id }}</td>
          <td>{{ a.title }}</td>
          <td>{{ a.artistName || '—' }}</td>
          <td>{{ a.yearCreated || '—' }}</td>
          <td v-if="auth.isAdmin" class="row-actions">
            <button class="btn btn-sm btn-danger" @click="remove(a.id)">Remove</button>
          </td>
        </tr>
        <tr v-if="linked.length === 0">
          <td :colspan="auth.isAdmin ? 5 : 4" class="empty">No artworks linked yet.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
