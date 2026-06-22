<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { exhibitions, exhibitionArtworks, search } from '../api/resources';
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
const sort = ref('startDate,asc');

const isAdmin = computed(() => authStore.isAdmin);

const liveCount = computed(() =>
  rows.value.filter((exhibition) => getStatus(exhibition).key === 'live').length,
);

const scheduledCount = computed(() =>
  rows.value.filter((exhibition) => getStatus(exhibition).key === 'scheduled').length,
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

function asArray(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data?.content)) return payload.data.content;
  return [];
}

function getExhibitorName(exhibition) {
  return exhibition.exhibitorName || exhibition.exhibitor?.name || 'Unknown exhibitor';
}

function getStatus(exhibition) {
  const now = new Date();
  const start = exhibition.startDate ? new Date(exhibition.startDate) : null;
  const end = exhibition.endDate ? new Date(exhibition.endDate) : null;

  if (start && now < start) {
    return {
      key: 'scheduled',
      label: 'Scheduled',
    };
  }

  if (end && now > end) {
    return {
      key: 'closed',
      label: 'Closed',
    };
  }

  return {
    key: 'live',
    label: 'Live',
  };
}

function getPeriod(exhibition) {
  const start = exhibition.startDate || '—';
  const end = exhibition.endDate || '—';

  return `${start} — ${end}`;
}

async function enrichWithArtworkCounts(exhibitionRows) {
  return Promise.all(
    exhibitionRows.map(async (exhibition) => {
      try {
        const linkedArtworks = await exhibitionArtworks.list(exhibition.id);

        return {
          ...exhibition,
          artworkCount: asArray(linkedArtworks).length,
        };
      } catch {
        return {
          ...exhibition,
          artworkCount: 0,
        };
      }
    }),
  );
}

async function loadExhibitions() {
  loading.value = true;
  error.value = null;

  try {
    const params = {
      page: page.value,
      size: size.value,
      sort: sort.value,
    };

    const result = appliedSearch.value
      ? await search.exhibitions(appliedSearch.value, params)
      : await exhibitions.list(params);

    const parsed = asPage(result);

    rows.value = await enrichWithArtworkCounts(parsed.content);
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
  router.push({ name: 'exhibition-new' });
}

function goToDetails(exhibition) {
  router.push({ name: 'exhibition-details', params: { id: exhibition.id } });
}

function goToEdit(exhibition) {
  router.push({ name: 'exhibition-edit', params: { id: exhibition.id } });
}

async function removeExhibition(exhibition) {
  const confirmed = window.confirm(`Delete exhibition "${exhibition.title}"?`);

  if (!confirmed) {
    return;
  }

  try {
    await exhibitions.remove(exhibition.id);

    if (rows.value.length === 1 && page.value > 0) {
      page.value -= 1;
    }

    await loadExhibitions();
  } catch (err) {
    error.value = normalizeError(err).message;
  }
}

async function applySearch() {
  appliedSearch.value = searchTerm.value.trim();
  page.value = 0;
  await loadExhibitions();
}

async function clearSearch() {
  searchTerm.value = '';
  appliedSearch.value = '';
  page.value = 0;
  await loadExhibitions();
}

async function previousPage() {
  if (!hasPrevious.value) {
    return;
  }

  page.value -= 1;
  await loadExhibitions();
}

async function nextPage() {
  if (!hasNext.value) {
    return;
  }

  page.value += 1;
  await loadExhibitions();
}

async function changeSort(value) {
  sort.value = value;
  page.value = 0;
  await loadExhibitions();
}

onMounted(loadExhibitions);
</script>

<template>
  <div class="exhibitions-page">
    <div class="exhibitions-page-header">
      <div>
        <p class="eyebrow">Exhibition schedule</p>
        <h1>Exhibitions</h1>
        <p class="muted">
          Manage gallery exhibitions and the artworks assigned to each event.
        </p>
      </div>

      <div class="exhibitions-header-actions">
        <button
          v-if="isAdmin"
          type="button"
          class="btn btn-primary"
          @click="goToCreate"
        >
          + New Exhibition
        </button>
      </div>
    </div>

    <div class="exhibition-stats-grid">
      <div class="exhibition-stat-card">
        <span>Total exhibitions</span>
        <strong>{{ totalElements }}</strong>
        <small>Saved exhibition records</small>
      </div>

      <div class="exhibition-stat-card">
        <span>Live on page</span>
        <strong>{{ liveCount }}</strong>
        <small>Currently active events</small>
      </div>

      <div class="exhibition-stat-card">
        <span>Scheduled on page</span>
        <strong>{{ scheduledCount }}</strong>
        <small>Upcoming events</small>
      </div>
    </div>

    <section class="exhibitions-card">
      <div class="exhibitions-toolbar">
        <div class="exhibitions-search">
          <label for="exhibitionSearch">Search exhibitions</label>

          <div class="exhibitions-search-row">
            <input
              id="exhibitionSearch"
              v-model="searchTerm"
              type="search"
              placeholder="Search by exhibition title..."
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

        <div class="exhibitions-sort">
          <label for="exhibitionSort">Sort</label>

          <select
            id="exhibitionSort"
            :value="sort"
            @change="changeSort($event.target.value)"
          >
            <option value="startDate,asc">Start date asc</option>
            <option value="startDate,desc">Start date desc</option>
            <option value="title,asc">Title A-Z</option>
            <option value="title,desc">Title Z-A</option>
            <option value="id,desc">Newest ID</option>
          </select>
        </div>
      </div>

      <div v-if="error" class="alert alert-error">
        {{ error }}
      </div>

      <div v-if="loading" class="empty compact">
        Loading exhibitions...
      </div>

      <div v-else-if="!rows.length" class="exhibitions-empty-state">
        <div class="exhibitions-empty-icon">🏛️</div>
        <h2>No exhibitions found</h2>
        <p>Create an exhibition and assign artworks to it.</p>

        <button
          v-if="isAdmin"
          type="button"
          class="btn btn-primary"
          @click="goToCreate"
        >
          + New Exhibition
        </button>
      </div>

      <div v-else class="exhibition-list">
        <article
          v-for="exhibition in rows"
          :key="exhibition.id"
          class="exhibition-row-card"
        >
          <div class="exhibition-icon-card" :class="getStatus(exhibition).key">
            🏛️
          </div>

          <div class="exhibition-main">
            <div class="exhibition-main-header">
              <div>
                <h2>{{ exhibition.title }}</h2>
                <p>{{ getExhibitorName(exhibition) }}</p>
              </div>

              <span class="exhibition-status-pill" :class="getStatus(exhibition).key">
                {{ getStatus(exhibition).label }}
              </span>
            </div>

            <div class="exhibition-meta-grid">

              <div>
                <span>Period</span>
                <strong>{{ getPeriod(exhibition) }}</strong>
              </div>

              <div>
                <span>Artworks</span>
                <strong>{{ exhibition.artworkCount }}</strong>
              </div>

              <div>
                <span>Exhibitor</span>
                <strong>{{ getExhibitorName(exhibition) }}</strong>
              </div>
            </div>

            <p v-if="exhibition.description" class="exhibition-description">
              {{ exhibition.description }}
            </p>
          </div>

          <div class="exhibition-actions">
            <button
              type="button"
              class="btn btn-secondary"
              @click="goToDetails(exhibition)"
            >
              View
            </button>

            <button
              v-if="isAdmin"
              type="button"
              class="btn btn-secondary"
              @click="goToEdit(exhibition)"
            >
              Edit
            </button>

            <button
              v-if="isAdmin"
              type="button"
              class="btn btn-danger-soft"
              @click="removeExhibition(exhibition)"
            >
              Delete
            </button>
          </div>
        </article>
      </div>

      <div class="exhibitions-pagination">
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
.exhibitions-page {
  width: 100%;
}

.exhibitions-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.25rem;
  margin-bottom: 1.35rem;
}

.exhibitions-page-header h1 {
  margin: 0;
  color: var(--ink);
  font-size: clamp(1.85rem, 3vw, 2.55rem);
  line-height: 1.05;
  letter-spacing: -0.055em;
}

.exhibitions-page-header p {
  max-width: 760px;
  margin-top: 0.7rem;
  line-height: 1.6;
}

.exhibitions-header-actions {
  flex: 0 0 auto;
  padding-top: 0.25rem;
}

.exhibition-stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
  margin-bottom: 1rem;
}

.exhibition-stat-card {
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.96));
  box-shadow: var(--shadow-soft);
}

.exhibition-stat-card span {
  display: block;
  color: var(--muted);
  font-size: 0.76rem;
  font-weight: 950;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.exhibition-stat-card strong {
  display: block;
  margin-top: 0.35rem;
  color: var(--ink);
  font-size: 1.35rem;
  font-weight: 950;
}

.exhibition-stat-card small {
  display: block;
  margin-top: 0.15rem;
  color: var(--muted);
  font-size: 0.82rem;
  font-weight: 750;
}

.exhibitions-card {
  padding: 1.1rem;
  border: 1px solid var(--border);
  border-radius: 26px;
  background:
    radial-gradient(circle at top right, rgba(95, 125, 232, 0.08), transparent 18rem),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.96));
  box-shadow: var(--shadow);
}

.exhibitions-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 1rem;
  margin-bottom: 1rem;
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
}

.exhibitions-toolbar label {
  display: block;
  margin-bottom: 0.45rem;
  color: var(--ink);
  font-size: 0.83rem;
  font-weight: 950;
}

.exhibitions-search-row {
  display: flex;
  gap: 0.6rem;
}

.exhibitions-search-row input {
  flex: 1 1 auto;
}

.exhibition-list {
  display: grid;
  gap: 0.85rem;
}

.exhibition-row-card {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr) auto;
  gap: 1rem;
  align-items: center;
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.91);
  box-shadow: var(--shadow-soft);
}

.exhibition-icon-card {
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  border-radius: 20px;
  background: linear-gradient(135deg, #8aa3ff, var(--primary));
  color: white;
  font-size: 1.35rem;
  box-shadow: 0 12px 26px rgba(95, 125, 232, 0.2);
}

.exhibition-icon-card.live {
  background: linear-gradient(135deg, #38d996, #16a46f);
  box-shadow: 0 12px 26px rgba(22, 164, 111, 0.2);
}

.exhibition-icon-card.scheduled {
  background: linear-gradient(135deg, #8aa3ff, var(--primary));
}

.exhibition-icon-card.closed {
  background: linear-gradient(135deg, #aeb7ca, #6f7b92);
  box-shadow: 0 12px 26px rgba(111, 123, 146, 0.16);
}

.exhibition-main {
  min-width: 0;
}

.exhibition-main-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.9rem;
}

.exhibition-main-header h2 {
  margin: 0;
  color: var(--ink);
  font-size: 1.06rem;
  font-weight: 950;
  letter-spacing: -0.025em;
}

.exhibition-main-header p {
  margin: 0.2rem 0 0;
  color: var(--muted);
  font-size: 0.9rem;
  font-weight: 750;
}

.exhibition-status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 0.75rem;
  border: 1px solid #d7e1ff;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary-dark);
  font-size: 0.8rem;
  font-weight: 900;
  white-space: nowrap;
}

.exhibition-status-pill.live {
  border-color: #afe8d0;
  background: #e9fbf3;
  color: #08734d;
}

.exhibition-status-pill.closed {
  border-color: #dce2ec;
  background: #f2f5fa;
  color: #607087;
}

.exhibition-meta-grid {
  display: grid;
  grid-template-columns: 230px 120px minmax(0, 1fr);
  gap: 0.75rem;
  margin-top: 0.85rem;
}

.exhibition-meta-grid div {
  min-width: 0;
  padding: 0.7rem 0.8rem;
  border: 1px solid var(--border);
  border-radius: 15px;
  background: rgba(248, 250, 255, 0.82);
}

.exhibition-meta-grid span {
  display: block;
  color: var(--muted);
  font-size: 0.72rem;
  font-weight: 950;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.exhibition-meta-grid strong {
  display: block;
  margin-top: 0.18rem;
  color: var(--ink);
  font-size: 0.9rem;
  font-weight: 850;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.exhibition-description {
  margin: 0.85rem 0 0;
  padding: 0.85rem 0.95rem;
  border-left: 4px solid var(--primary);
  border-radius: 14px;
  background: rgba(248, 250, 255, 0.82);
  color: var(--ink-soft);
  font-size: 0.9rem;
  line-height: 1.5;
}

.exhibition-actions {
  display: flex;
  gap: 0.6rem;
  align-items: center;
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

.exhibitions-empty-state {
  display: grid;
  justify-items: center;
  gap: 0.65rem;
  padding: 3rem 1rem;
  text-align: center;
}

.exhibitions-empty-icon {
  display: grid;
  place-items: center;
  width: 62px;
  height: 62px;
  border-radius: 22px;
  background: var(--primary-soft);
  font-size: 1.55rem;
}

.exhibitions-empty-state h2 {
  margin: 0;
  color: var(--ink);
  font-size: 1.25rem;
}

.exhibitions-empty-state p {
  margin: 0;
  color: var(--muted);
}

.exhibitions-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.75rem;
  margin-top: 1rem;
  color: var(--muted);
  font-size: 0.9rem;
  font-weight: 800;
}

@media (max-width: 1200px) {
  .exhibition-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1050px) {
  .exhibition-row-card {
    grid-template-columns: 64px minmax(0, 1fr);
  }

  .exhibition-actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }
}

@media (max-width: 900px) {
  .exhibitions-page-header {
    flex-direction: column;
  }

  .exhibitions-header-actions {
    padding-top: 0;
  }

  .exhibition-stats-grid,
  .exhibitions-toolbar,
  .exhibition-meta-grid {
    grid-template-columns: 1fr;
  }

  .exhibitions-search-row {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .exhibitions-card {
    padding: 0.9rem;
    border-radius: 20px;
  }

  .exhibition-row-card {
    grid-template-columns: 1fr;
  }

  .exhibition-icon-card {
    width: 50px;
    height: 50px;
    border-radius: 17px;
  }

  .exhibition-main-header {
    flex-direction: column;
  }

  .exhibition-actions {
    flex-direction: column;
  }

  .exhibition-actions .btn,
  .exhibition-actions button {
    width: 100%;
  }

  .exhibitions-pagination {
    flex-direction: column;
  }
}
</style>
