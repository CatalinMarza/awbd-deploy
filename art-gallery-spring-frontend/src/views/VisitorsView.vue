<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import DataTable from '../components/DataTable.vue';
import { visitors, search } from '../api/resources';
import { useAuthStore } from '../stores/auth';
import { normalizeError } from '../api/http';

const auth = useAuthStore();
const router = useRouter();
const table = ref(null);
const query = ref('');
const actionError = ref(null);

const columns = [
  { key: 'id', label: 'ID', sortable: true },
  { key: 'name', label: 'Name', sortable: true },
  { key: 'email', label: 'Email', sortable: true },
  { key: 'phone', label: 'Phone', sortable: false },
  { key: 'membershipType', label: 'Membership', sortable: true },
  { key: 'joinDate', label: 'Joined', sortable: true },
];

function fetcher(params) {
  return query.value.trim()
    ? search.visitors(query.value.trim(), params)
    : visitors.list(params);
}

function reload() {
  table.value?.reload();
}

async function remove(item) {
  if (!confirm(`Delete visitor "${item.name}"?`)) return;
  actionError.value = null;
  try {
    await visitors.remove(item.id);
    reload();
  } catch (e) {
    actionError.value = normalizeError(e).message;
  }
}
</script>

<template>
  <div class="page-header">
    <h1>Visitors</h1>
    <router-link v-if="auth.isAdmin" class="btn btn-primary" :to="{ name: 'visitor-new' }">+ New Visitor</router-link>
  </div>

  <div v-if="actionError" class="alert alert-error">{{ actionError }}</div>

  <div class="card">
    <DataTable ref="table" :columns="columns" :fetcher="fetcher" default-sort="name,asc">
      <template #toolbar>
        <input v-model="query" placeholder="Search visitors…" style="max-width: 260px" @keyup.enter="reload" />
        <button class="btn" @click="reload">Search</button>
        <button v-if="query" class="btn" @click="query = ''; reload()">Clear</button>
      </template>
      <template #row="{ item }">
        <td>{{ item.id }}</td>
        <td>{{ item.name }}</td>
        <td>{{ item.email || '—' }}</td>
        <td>{{ item.phone || '—' }}</td>
        <td>{{ item.membershipType || '—' }}</td>
        <td>{{ item.joinDate || '—' }}</td>
      </template>
      <template v-if="auth.isAdmin" #actions="{ item }">
        <button class="btn btn-sm" @click="router.push({ name: 'visitor-edit', params: { id: item.id } })">Edit</button>
        <button class="btn btn-sm btn-danger" @click="remove(item)">Delete</button>
      </template>
    </DataTable>
  </div>
</template>
