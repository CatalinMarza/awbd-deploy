<script setup>
import { ref, watch, onMounted } from 'vue';
import { normalizeError } from '../api/http';

const props = defineProps({
  columns: { type: Array, required: true }, // [{ key, label, sortable }]
  fetcher: { type: Function, required: true }, // (params) => Promise<PageResponse>
  defaultSort: { type: String, default: 'id,asc' },
  pageSize: { type: Number, default: 10 },
});

const items = ref([]);
const pageInfo = ref({ page: 0, totalPages: 0, totalElements: 0, first: true, last: true });
const sort = ref(props.defaultSort);
const page = ref(0);
const loading = ref(false);
const error = ref(null);

async function load() {
  loading.value = true;
  error.value = null;
  try {
    const data = await props.fetcher({ page: page.value, size: props.pageSize, sort: sort.value });
    items.value = data.content;
    pageInfo.value = data;
  } catch (e) {
    error.value = normalizeError(e).message;
    items.value = [];
  } finally {
    loading.value = false;
  }
}

function toggleSort(col) {
  if (!col.sortable) return;
  const [field, dir] = sort.value.split(',');
  const nextDir = field === col.key && dir === 'asc' ? 'desc' : 'asc';
  sort.value = `${col.key},${nextDir}`;
  page.value = 0;
}

function arrow(col) {
  const [field, dir] = sort.value.split(',');
  if (field !== col.key) return '';
  return dir === 'asc' ? ' ▲' : ' ▼';
}

function goTo(p) {
  if (p < 0 || p >= pageInfo.value.totalPages) return;
  page.value = p;
}

watch([page, sort], load);
onMounted(load);
defineExpose({ reload: load });
</script>

<template>
  <div>
    <div v-if="$slots.toolbar" class="toolbar" style="margin-bottom: 0.8rem">
      <slot name="toolbar" />
    </div>

    <div v-if="error" class="alert alert-error">{{ error }}</div>

    <table>
      <thead>
        <tr>
          <th
            v-for="col in columns"
            :key="col.key"
            :class="{ sortable: col.sortable }"
            @click="toggleSort(col)"
          >
            {{ col.label }}<span>{{ arrow(col) }}</span>
          </th>
          <th v-if="$slots.actions" style="text-align: right">Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <slot name="row" :item="item" />
          <td v-if="$slots.actions" class="row-actions">
            <slot name="actions" :item="item" />
          </td>
        </tr>
        <tr v-if="!loading && items.length === 0">
          <td :colspan="columns.length + ($slots.actions ? 1 : 0)" class="empty">No records found.</td>
        </tr>
      </tbody>
    </table>

    <div class="pagination">
      <button class="btn btn-sm" :disabled="pageInfo.first" @click="goTo(page - 1)">← Prev</button>
      <span class="muted" style="font-size: 0.85rem">
        Page {{ pageInfo.page + 1 }} of {{ Math.max(pageInfo.totalPages, 1) }}
        · {{ pageInfo.totalElements }} total
      </span>
      <button class="btn btn-sm" :disabled="pageInfo.last" @click="goTo(page + 1)">Next →</button>
      <span v-if="loading" class="muted" style="font-size: 0.85rem">Loading…</span>
    </div>
  </div>
</template>
