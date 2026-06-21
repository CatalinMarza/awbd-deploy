<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import DataTable from '../components/DataTable.vue';
import { artworks, search } from '../api/resources';
import { useAuthStore } from '../stores/auth';
import { normalizeError } from '../api/http';

const auth = useAuthStore();
const router = useRouter();
const table = ref(null);
const query = ref('');
const actionError = ref(null);

const columns = [
  { key: 'id', label: 'ID', sortable: true },
  { key: 'title', label: 'Title', sortable: true },
  { key: 'artistName', label: 'Artist', sortable: false },
  { key: 'yearCreated', label: 'Year', sortable: true },
  { key: 'medium', label: 'Medium', sortable: true },
  { key: 'estimatedValue', label: 'Est. Value', sortable: true },
];

function fetcher(params) {
  return query.value.trim()
    ? search.artworks(query.value.trim(), params)
    : artworks.list(params);
}

function reload() {
  table.value?.reload();
}

function money(v) {
  return v == null ? '—' : new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(v);
}

async function remove(item) {
  if (!confirm(`Delete artwork "${item.title}"?`)) return;
  actionError.value = null;
  try {
    await artworks.remove(item.id);
    reload();
  } catch (e) {
    actionError.value = normalizeError(e).message;
  }
}
</script>

<template>
  <div class="page-header">
    <h1>Artworks</h1>
    <router-link v-if="auth.isAdmin" class="btn btn-primary" :to="{ name: 'artwork-new' }">+ New Artwork</router-link>
  </div>

  <div v-if="actionError" class="alert alert-error">{{ actionError }}</div>

  <div class="card">
    <DataTable ref="table" :columns="columns" :fetcher="fetcher" default-sort="title,asc">
      <template #toolbar>
        <input v-model="query" placeholder="Search artworks…" style="max-width: 260px" @keyup.enter="reload" />
        <button class="btn" @click="reload">Search</button>
        <button v-if="query" class="btn" @click="query = ''; reload()">Clear</button>
      </template>
      <template #row="{ item }">
        <td>{{ item.id }}</td>
        <td>{{ item.title }}</td>
        <td>{{ item.artistName || '—' }}</td>
        <td>{{ item.yearCreated || '—' }}</td>
        <td>{{ item.medium || '—' }}</td>
        <td>{{ money(item.estimatedValue) }}</td>
      </template>
      <template v-if="auth.isAdmin" #actions="{ item }">
        <button class="btn btn-sm" @click="router.push({ name: 'artwork-edit', params: { id: item.id } })">Edit</button>
        <button class="btn btn-sm btn-danger" @click="remove(item)">Delete</button>
      </template>
    </DataTable>
  </div>
</template>
