<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { visitors, search } from '../api/resources';
import { normalizeError } from '../api/http';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const rows = ref([]);
const loading = ref(false);
const error = ref(null);

const searchTerm = ref('');
const appliedSearch = ref('');

const page = ref(0);
const size = ref(10);
const totalElements = ref(0);
const totalPages = ref(0);
const sort = ref('name,asc');

const isAdmin = computed(() => authStore.isAdmin);

const vipCount = computed(() =>
  rows.value.filter((visitor) => String(visitor.membershipType || '').toLowerCase() === 'vip').length,
);

const studentCount = computed(() =>
  rows.value.filter((visitor) => String(visitor.membershipType || '').toLowerCase() === 'student').length,
);

const pageLabel = computed(() => {
  const current = totalPages.value ? page.value + 1 : 1;
  const total = totalPages.value || 1;
  return `Page ${current} of ${total} · ${totalElements.value} total`;
});

const hasPrevious = computed(() => page.value > 0);
const hasNext = computed(() => page.value + 1 < totalPages.value);

function asPage(payload) {
  const pagePayload = payload?.data || payload || {};

  return {
    content: Array.isArray(pagePayload.content) ? pagePayload.content : [],
    page: Number(pagePayload.page ?? page.value ?? 0),
    size: Number(pagePayload.size ?? size.value ?? 10),
    totalElements: Number(pagePayload.totalElements ?? 0),
    totalPages: Number(pagePayload.totalPages ?? 0),
  };
}

function membershipClass(type) {
  const normalized = String(type || 'Standard').toLowerCase();

  return {
    vip: normalized === 'vip',
    student: normalized === 'student',
    standard: normalized !== 'vip' && normalized !== 'student',
  };
}

function membershipIcon(type) {
  const normalized = String(type || 'Standard').toLowerCase();

  if (normalized === 'vip') return '⭐';
  if (normalized === 'student') return '🎓';
  return '🎟️';
}

async function loadVisitors() {
  loading.value = true;
  error.value = null;

  try {
    const params = {
      page: page.value,
      size: size.value,
      sort: sort.value,
    };

    const result = appliedSearch.value
      ? await search.visitors(appliedSearch.value, params)
      : await visitors.list(params);

    const parsed = asPage(result);

    rows.value = parsed.content;
    page.value = parsed.page;
    size.value = parsed.size;
    totalElements.value = parsed.totalElements;
    totalPages.value = parsed.totalPages;
  } catch (err) {
    error.value = normalizeError(err).message;
  } finally {
    loading.value = false;
  }
}

function goToCreate() {
  router.push({ name: 'visitor-new' });
}

function goToEdit(visitor) {
  router.push({ name: 'visitor-edit', params: { id: visitor.id } });
}

async function removeVisitor(visitor) {
  const confirmed = window.confirm(`Delete visitor "${visitor.name}"?`);

  if (!confirmed) return;

  try {
    await visitors.remove(visitor.id);

    if (rows.value.length === 1 && page.value > 0) {
      page.value -= 1;
    }

    await loadVisitors();
  } catch (err) {
    error.value = normalizeError(err).message;
  }
}

async function applySearch() {
  appliedSearch.value = searchTerm.value.trim();
  page.value = 0;
  await loadVisitors();
}

async function clearSearch() {
  searchTerm.value = '';
  appliedSearch.value = '';
  page.value = 0;
  await loadVisitors();
}

async function previousPage() {
  if (!hasPrevious.value) return;
  page.value -= 1;
  await loadVisitors();
}

async function nextPage() {
  if (!hasNext.value) return;
  page.value += 1;
  await loadVisitors();
}

async function changeSort(value) {
  sort.value = value;
  page.value = 0;
  await loadVisitors();
}

onMounted(loadVisitors);
</script>

<template>
  <div class="visitors-page">
    <div class="visitors-page-header">
      <div>
        <p class="eyebrow">Visitor management</p>
        <h1>Visitors</h1>
        <p class="muted">
          Manage gallery visitors used for reviews and visitor-related records.
        </p>
      </div>

      <div class="visitors-header-actions">
        <button
          v-if="isAdmin"
          type="button"
          class="btn btn-primary"
          @click="goToCreate"
        >
          + New Visitor
        </button>
      </div>
    </div>

    <div class="visitor-stats-grid">
      <div class="visitor-stat-card">
        <span>Total visitors</span>
        <strong>{{ totalElements }}</strong>
        <small>Registered visitor profiles</small>
      </div>

      <div class="visitor-stat-card">
        <span>VIP on page</span>
        <strong>{{ vipCount }}</strong>
        <small>Premium visitor profiles</small>
      </div>

      <div class="visitor-stat-card">
        <span>Students on page</span>
        <strong>{{ studentCount }}</strong>
        <small>Student visitor profiles</small>
      </div>
    </div>

    <section class="visitors-card">
      <div class="visitors-toolbar">
        <div class="visitors-search">
          <label for="visitorSearch">Search visitors</label>

          <div class="visitors-search-row">
            <input
              id="visitorSearch"
              v-model="searchTerm"
              type="search"
              placeholder="Search by name or email..."
              @keyup.enter="applySearch"
            />

            <button type="button" class="btn btn-primary" @click="applySearch">
              Search
            </button>

            <button type="button" class="btn btn-secondary" @click="clearSearch">
              Clear
            </button>
          </div>
        </div>

        <div class="visitors-sort">
          <label for="visitorSort">Sort</label>

          <select
            id="visitorSort"
            :value="sort"
            @change="changeSort($event.target.value)"
          >
            <option value="name,asc">Name A-Z</option>
            <option value="name,desc">Name Z-A</option>
            <option value="id,asc">ID ascending</option>
            <option value="id,desc">ID descending</option>
          </select>
        </div>
      </div>

      <div v-if="error" class="alert alert-error">
        {{ error }}
      </div>

      <div v-if="loading" class="empty compact">
        Loading visitors...
      </div>

      <div v-else-if="!rows.length" class="visitors-empty-state">
        <div class="visitors-empty-icon">👤</div>
        <h2>No visitors found</h2>
        <p>Create a visitor profile or adjust the search filter.</p>

        <button
          v-if="isAdmin"
          type="button"
          class="btn btn-primary"
          @click="goToCreate"
        >
          + New Visitor
        </button>
      </div>

      <div v-else class="visitor-list">
        <article
          v-for="visitor in rows"
          :key="visitor.id"
          class="visitor-row-card"
        >
          <div class="visitor-main">
            <div class="visitor-title-line">
              <span
                class="membership-name-icon"
                :class="membershipClass(visitor.membershipType)"
                :title="visitor.membershipType || 'Standard'"
              >
                {{ membershipIcon(visitor.membershipType) }}
              </span>

              <h2>{{ visitor.name }}</h2>
            </div>

            <p>{{ visitor.email || 'No email provided' }}</p>

            <div class="visitor-meta-grid">
              <div>
                <span>Phone</span>
                <strong>{{ visitor.phone || '—' }}</strong>
              </div>

              <div>
                <span>Email</span>
                <strong>{{ visitor.email || '—' }}</strong>
              </div>
            </div>
          </div>

          <div v-if="isAdmin" class="visitor-actions">
            <button
              type="button"
              class="btn btn-secondary"
              @click="goToEdit(visitor)"
            >
              Edit
            </button>

            <button
              type="button"
              class="btn btn-danger-soft"
              @click="removeVisitor(visitor)"
            >
              Delete
            </button>
          </div>
        </article>
      </div>

      <div class="visitors-pagination">
        <button
          type="button"
          class="btn btn-secondary"
          :disabled="!hasPrevious"
          @click="previousPage"
        >
          ← Prev
        </button>

        <span>{{ pageLabel }}</span>

        <button
          type="button"
          class="btn btn-secondary"
          :disabled="!hasNext"
          @click="nextPage"
        >
          Next →
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.visitors-page {
  width: 100%;
}

.visitors-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.25rem;
  margin-bottom: 1.35rem;
}

.visitors-page-header h1 {
  margin: 0;
  color: var(--ink);
  font-size: clamp(1.85rem, 3vw, 2.55rem);
  line-height: 1.05;
  letter-spacing: -0.055em;
}

.visitors-page-header p {
  max-width: 760px;
  margin-top: 0.7rem;
  line-height: 1.6;
}

.visitors-header-actions {
  flex: 0 0 auto;
  padding-top: 0.25rem;
}

.visitor-stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
  margin-bottom: 1rem;
}

.visitor-stat-card {
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.96));
  box-shadow: var(--shadow-soft);
}

.visitor-stat-card span {
  display: block;
  color: var(--muted);
  font-size: 0.76rem;
  font-weight: 950;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.visitor-stat-card strong {
  display: block;
  max-width: 100%;
  margin-top: 0.35rem;
  color: var(--ink);
  font-size: 1.35rem;
  font-weight: 950;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.visitor-stat-card small {
  display: block;
  margin-top: 0.15rem;
  color: var(--muted);
  font-size: 0.82rem;
  font-weight: 750;
}

.visitors-card {
  padding: 1.1rem;
  border: 1px solid var(--border);
  border-radius: 26px;
  background:
    radial-gradient(circle at top right, rgba(95, 125, 232, 0.08), transparent 18rem),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.96));
  box-shadow: var(--shadow);
}

.visitors-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  align-items: end;
  gap: 1rem;
  margin-bottom: 1rem;
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
}

.visitors-toolbar label {
  display: block;
  margin-bottom: 0.45rem;
  color: var(--ink);
  font-size: 0.83rem;
  font-weight: 950;
}

.visitors-search-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.visitors-search-row input {
  flex: 1 1 auto;
}

.visitors-sort select {
  width: 100%;
  min-height: 46px;
}

.visitor-list {
  display: grid;
  gap: 0.85rem;
}

.visitor-row-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 1rem;
  align-items: center;
  padding: 1rem 1.15rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.91);
  box-shadow: var(--shadow-soft);
}

.visitor-main {
  min-width: 0;
}

.visitor-title-line {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  flex-wrap: wrap;
}

.visitor-title-line h2 {
  margin: 0;
  color: var(--ink);
  font-size: 1.06rem;
  font-weight: 950;
  letter-spacing: -0.025em;
}

.visitor-main p {
  margin: 0.35rem 0 0;
  color: var(--muted);
  font-size: 0.9rem;
  font-weight: 750;
}

.membership-name-icon {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border: 1px solid #d7e1ff;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary-dark);
  font-size: 0.94rem;
  line-height: 1;
}

.membership-name-icon.vip {
  border-color: #f3d487;
  background: #fff5d6;
  color: #9a6a00;
}

.membership-name-icon.student {
  border-color: #ded4ff;
  background: #f3efff;
  color: #6043c7;
}

.visitor-meta-grid {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 0.75rem;
  margin-top: 0.85rem;
}

.visitor-meta-grid div {
  min-width: 0;
  padding: 0.7rem 0.8rem;
  border: 1px solid var(--border);
  border-radius: 15px;
  background: rgba(248, 250, 255, 0.82);
}

.visitor-meta-grid span {
  display: block;
  color: var(--muted);
  font-size: 0.72rem;
  font-weight: 950;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.visitor-meta-grid strong {
  display: block;
  margin-top: 0.18rem;
  color: var(--ink);
  font-size: 0.9rem;
  font-weight: 850;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.visitor-actions {
  display: flex;
  gap: 0.6rem;
}

.btn-secondary {
  background: #f8faff;
  border-color: var(--border);
  color: var(--ink-soft);
}

.btn-secondary:hover {
  background: var(--primary-lighter);
  border-color: var(--border-strong);
  color: var(--primary-dark);
}

.btn-danger-soft {
  border-color: #f4b8b8;
  background: #fffafa;
  color: #cf3b3b;
}

.btn-danger-soft:hover {
  background: #fff1f1;
  border-color: #ee9999;
  color: #b62828;
}

.visitors-empty-state {
  display: grid;
  justify-items: center;
  gap: 0.65rem;
  padding: 3rem 1rem;
  text-align: center;
}

.visitors-empty-icon {
  display: grid;
  place-items: center;
  width: 62px;
  height: 62px;
  border-radius: 22px;
  background: var(--primary-soft);
  font-size: 1.55rem;
}

.visitors-empty-state h2 {
  margin: 0;
  color: var(--ink);
  font-size: 1.25rem;
}

.visitors-empty-state p {
  margin: 0;
  color: var(--muted);
}

.visitors-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin-top: 1rem;
  color: var(--muted);
  font-size: 0.9rem;
  font-weight: 800;
}

@media (max-width: 1050px) {
  .visitor-row-card {
    grid-template-columns: 1fr;
  }

  .visitor-actions {
    justify-content: flex-end;
  }
}

@media (max-width: 900px) {
  .visitors-page-header {
    flex-direction: column;
  }

  .visitors-header-actions {
    padding-top: 0;
  }

  .visitor-stats-grid,
  .visitors-toolbar,
  .visitor-meta-grid {
    grid-template-columns: 1fr;
  }

  .visitors-search-row {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 640px) {
  .visitors-card {
    padding: 0.9rem;
    border-radius: 20px;
  }

  .visitor-actions {
    flex-direction: column;
  }

  .visitor-actions .btn,
  .visitor-actions button {
    width: 100%;
  }

  .visitors-pagination {
    flex-direction: column;
  }
}
</style>
