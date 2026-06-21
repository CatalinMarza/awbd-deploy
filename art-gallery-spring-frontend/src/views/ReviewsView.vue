<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import DataTable from '../components/DataTable.vue';
import { reviews } from '../api/resources';
import { useAuthStore } from '../stores/auth';
import { normalizeError } from '../api/http';

const auth = useAuthStore();
const router = useRouter();
const table = ref(null);
const actionError = ref(null);

const columns = [
  { key: 'id', label: 'ID', sortable: true },
  { key: 'visitorName', label: 'Visitor', sortable: false },
  { key: 'artworkTitle', label: 'Artwork', sortable: false },
  { key: 'exhibitionTitle', label: 'Exhibition', sortable: false },
  { key: 'rating', label: 'Rating', sortable: true },
  { key: 'reviewDate', label: 'Date', sortable: true },
];

function fetcher(params) {
  return reviews.list(params);
}

function reload() {
  table.value?.reload();
}

async function remove(item) {
  if (!confirm('Delete this review?')) return;
  actionError.value = null;
  try {
    await reviews.remove(item.id);
    reload();
  } catch (e) {
    actionError.value = normalizeError(e).message;
  }
}
</script>

<template>
  <div class="page-header">
    <h1>Reviews</h1>
    <router-link class="btn btn-primary" :to="{ name: 'review-new' }">+ New Review</router-link>
  </div>

  <div v-if="actionError" class="alert alert-error">{{ actionError }}</div>

  <div class="card">
    <DataTable ref="table" :columns="columns" :fetcher="fetcher" default-sort="reviewDate,desc">
      <template #row="{ item }">
        <td>{{ item.id }}</td>
        <td>{{ item.visitorName || '—' }}</td>
        <td>{{ item.artworkTitle || '—' }}</td>
        <td>{{ item.exhibitionTitle || '—' }}</td>
        <td>{{ '★'.repeat(item.rating) }}{{ '☆'.repeat(5 - item.rating) }}</td>
        <td>{{ item.reviewDate }}</td>
      </template>
      <template v-if="auth.isAdmin" #actions="{ item }">
        <button class="btn btn-sm" @click="router.push({ name: 'review-edit', params: { id: item.id } })">Edit</button>
        <button class="btn btn-sm btn-danger" @click="remove(item)">Delete</button>
      </template>
    </DataTable>
  </div>
</template>
