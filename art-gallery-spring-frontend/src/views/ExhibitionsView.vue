<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import DataTable from '../components/DataTable.vue';
import { exhibitions, search } from '../api/resources';
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
  { key: 'exhibitorName', label: 'Exhibitor', sortable: false },
  { key: 'startDate', label: 'Start', sortable: true },
  { key: 'endDate', label: 'End', sortable: true },
];

function fetcher(params) {
  return query.value.trim()
    ? search.exhibitions(query.value.trim(), params)
    : exhibitions.list(params);
}

function reload() {
  table.value?.reload();
}

async function remove(item) {
  if (!confirm(`Delete exhibition "${item.title}"?`)) return;
  actionError.value = null;
  try {
    await exhibitions.remove(item.id);
    reload();
  } catch (e) {
    actionError.value = normalizeError(e).message;
  }
}
</script>

<template>
  <div class="page-header">
    <h1>Exhibitions</h1>
    <router-link v-if="auth.isAdmin" class="btn btn-primary" :to="{ name: 'exhibition-new' }">+ New Exhibition</router-link>
  </div>

  <div v-if="actionError" class="alert alert-error">{{ actionError }}</div>

  <div class="card">
    <DataTable ref="table" :columns="columns" :fetcher="fetcher" default-sort="startDate,desc">
      <template #toolbar>
        <input v-model="query" placeholder="Search exhibitions…" style="max-width: 260px" @keyup.enter="reload" />
        <button class="btn" @click="reload">Search</button>
        <button v-if="query" class="btn" @click="query = ''; reload()">Clear</button>
      </template>
      <template #row="{ item }">
        <td>{{ item.id }}</td>
        <td>{{ item.title }}</td>
        <td>{{ item.exhibitorName || '—' }}</td>
        <td>{{ item.startDate }}</td>
        <td>{{ item.endDate }}</td>
      </template>
      <template #actions="{ item }">
        <button class="btn btn-sm" @click="router.push({ name: 'exhibition-artworks', params: { id: item.id } })">Artworks</button>
        <template v-if="auth.isAdmin">
          <button class="btn btn-sm" @click="router.push({ name: 'exhibition-edit', params: { id: item.id } })">Edit</button>
          <button class="btn btn-sm btn-danger" @click="remove(item)">Delete</button>
        </template>
      </template>
    </DataTable>
  </div>
</template>
