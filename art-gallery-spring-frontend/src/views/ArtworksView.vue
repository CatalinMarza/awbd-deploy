<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { artworks, search } from '../api/resources';
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
const sort = ref('title,asc');

const isAdmin = computed(() => authStore.isAdmin);

const uniqueArtistsOnPage = computed(() => {
  const set = new Set(
    rows.value
      .map((artwork) => artwork.artistName)
      .filter(Boolean),
  );

  return set.size;
});

const totalEstimatedValue = computed(() => {
  const total = rows.value.reduce(
    (sum, artwork) => sum + Number(artwork.estimatedValue || 0),
    0,
  );

  return formatMoney(total);
});

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

function formatMoney(value) {
  const numberValue = Number(value || 0);

  if (!numberValue) {
    return '—';
  }

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'EUR',
    maximumFractionDigits: 0,
  }).format(numberValue);
}

function getArtistName(artwork) {
  return artwork.artistName || artwork.artist?.name || 'Unknown artist';
}

function getLocationName(artwork) {
  return artwork.locationName || artwork.location?.name || 'No location';
}

function getCollectionName(artwork) {
  return artwork.collectionName || artwork.collection?.name || 'No collection';
}

async function loadArtworks() {
  loading.value = true;
  error.value = null;

  try {
    const params = {
      page: page.value,
      size: size.value,
      sort: sort.value,
    };

    const result = appliedSearch.value
      ? await search.artworks(appliedSearch.value, params)
      : await artworks.list(params);

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
  router.push({ name: 'artwork-new' });
}

function goToEdit(artwork) {
  router.push({ name: 'artwork-edit', params: { id: artwork.id } });
}

async function removeArtwork(artwork) {
  const confirmed = window.confirm(`Delete artwork "${artwork.title}"?`);

  if (!confirmed) {
    return;
  }

  try {
    await artworks.remove(artwork.id);

    if (rows.value.length === 1 && page.value > 0) {
      page.value -= 1;
    }

    await loadArtworks();
  } catch (err) {
    error.value = normalizeError(err).message;
  }
}

async function applySearch() {
  appliedSearch.value = searchTerm.value.trim();
  page.value = 0;
  await loadArtworks();
}

async function clearSearch() {
  searchTerm.value = '';
  appliedSearch.value = '';
  page.value = 0;
  await loadArtworks();
}

async function previousPage() {
  if (!hasPrevious.value) {
    return;
  }

  page.value -= 1;
  await loadArtworks();
}

async function nextPage() {
  if (!hasNext.value) {
    return;
  }

  page.value += 1;
  await loadArtworks();
}

async function changeSort(value) {
  sort.value = value;
  page.value = 0;
  await loadArtworks();
}

onMounted(loadArtworks);
</script>

<template>
  <div class="artworks-page">
    <div class="artworks-page-header">
      <div>
        <p class="eyebrow">Artwork catalogue</p>
        <h1>Artworks</h1>
        <p class="muted">
          Manage the gallery artwork inventory, artists, collections and locations.
        </p>
      </div>

      <div class="artworks-header-actions">
        <button
          v-if="isAdmin"
          type="button"
          class="btn btn-primary"
          @click="goToCreate"
        >
          + New Artwork
        </button>
      </div>
    </div>

    <div class="artwork-stats-grid">
      <div class="artwork-stat-card">
        <span>Total artworks</span>
        <strong>{{ totalElements }}</strong>
        <small>Saved catalogue records</small>
      </div>

      <div class="artwork-stat-card">
        <span>Artists on page</span>
        <strong>{{ uniqueArtistsOnPage }}</strong>
        <small>Distinct artists visible</small>
      </div>

      <div class="artwork-stat-card">
        <span>Value on page</span>
        <strong>{{ totalEstimatedValue }}</strong>
        <small>Estimated total value</small>
      </div>
    </div>

    <section class="artworks-card">
      <div class="artworks-toolbar">
        <div class="artworks-search">
          <label for="artworkSearch">Search artworks</label>

          <div class="artworks-search-row">
            <input
              id="artworkSearch"
              v-model="searchTerm"
              type="search"
              placeholder="Search by artwork title..."
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

        <div class="artworks-sort">
          <label for="artworkSort">Sort</label>

          <select
            id="artworkSort"
            :value="sort"
            @change="changeSort($event.target.value)"
          >
            <option value="title,asc">Title A-Z</option>
            <option value="title,desc">Title Z-A</option>
            <option value="yearCreated,asc">Year asc</option>
            <option value="yearCreated,desc">Year desc</option>
            <option value="estimatedValue,desc">Highest value</option>
            <option value="estimatedValue,asc">Lowest value</option>
            <option value="id,desc">Newest ID</option>
          </select>
        </div>
      </div>

      <div v-if="error" class="alert alert-error">
        {{ error }}
      </div>

      <div v-if="loading" class="empty compact">
        Loading artworks...
      </div>

      <div v-else-if="!rows.length" class="artworks-empty-state">
        <div class="artworks-empty-icon">🖼️</div>
        <h2>No artworks found</h2>
        <p>Create an artwork after adding artists, collections and locations.</p>

        <button
          v-if="isAdmin"
          type="button"
          class="btn btn-primary"
          @click="goToCreate"
        >
          + New Artwork
        </button>
      </div>

      <div v-else class="artwork-list">
        <article
          v-for="artwork in rows"
          :key="artwork.id"
          class="artwork-row-card"
        >
          <div class="artwork-icon-card">
            🖼️
          </div>

          <div class="artwork-main">
            <div class="artwork-main-header">
              <div>
                <h2>{{ artwork.title }}</h2>
                <p>{{ getArtistName(artwork) }}</p>
              </div>

              <span class="artwork-value-pill">
                {{ formatMoney(artwork.estimatedValue) }}
              </span>
            </div>

            <div class="artwork-meta-grid">

              <div>
                <span>Year</span>
                <strong>{{ artwork.yearCreated || '—' }}</strong>
              </div>

              <div>
                <span>Medium</span>
                <strong>{{ artwork.medium || '—' }}</strong>
              </div>

              <div>
                <span>Location</span>
                <strong>{{ getLocationName(artwork) }}</strong>
              </div>

              <div>
                <span>Collection</span>
                <strong>{{ getCollectionName(artwork) }}</strong>
              </div>
            </div>
          </div>

          <div v-if="isAdmin" class="artwork-actions">
            <button
              type="button"
              class="btn btn-secondary"
              @click="goToEdit(artwork)"
            >
              Edit
            </button>

            <button
              type="button"
              class="btn btn-danger-soft"
              @click="removeArtwork(artwork)"
            >
              Delete
            </button>
          </div>
        </article>
      </div>

      <div class="artworks-pagination">
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
.artworks-page {
  width: 100%;
}

.artworks-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.25rem;
  margin-bottom: 1.35rem;
}

.artworks-page-header h1 {
  margin: 0;
  color: var(--ink);
  font-size: clamp(1.85rem, 3vw, 2.55rem);
  line-height: 1.05;
  letter-spacing: -0.055em;
}

.artworks-page-header p {
  max-width: 760px;
  margin-top: 0.7rem;
  line-height: 1.6;
}

.artworks-header-actions {
  flex: 0 0 auto;
  padding-top: 0.25rem;
}

.artwork-stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
  margin-bottom: 1rem;
}

.artwork-stat-card {
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.96));
  box-shadow: var(--shadow-soft);
}

.artwork-stat-card span {
  display: block;
  color: var(--muted);
  font-size: 0.76rem;
  font-weight: 950;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.artwork-stat-card strong {
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

.artwork-stat-card small {
  display: block;
  margin-top: 0.15rem;
  color: var(--muted);
  font-size: 0.82rem;
  font-weight: 750;
}

.artworks-card {
  padding: 1.1rem;
  border: 1px solid var(--border);
  border-radius: 26px;
  background:
    radial-gradient(circle at top right, rgba(95, 125, 232, 0.08), transparent 18rem),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.96));
  box-shadow: var(--shadow);
}

.artworks-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 240px;
  gap: 1rem;
  margin-bottom: 1rem;
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
}

.artworks-toolbar label {
  display: block;
  margin-bottom: 0.45rem;
  color: var(--ink);
  font-size: 0.83rem;
  font-weight: 950;
}

.artworks-search-row {
  display: flex;
  gap: 0.6rem;
}

.artworks-search-row input {
  flex: 1 1 auto;
}

.artwork-list {
  display: grid;
  gap: 0.85rem;
}

.artwork-row-card {
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

.artwork-icon-card {
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

.artwork-main {
  min-width: 0;
}

.artwork-main-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.9rem;
}

.artwork-main-header h2 {
  margin: 0;
  color: var(--ink);
  font-size: 1.06rem;
  font-weight: 950;
  letter-spacing: -0.025em;
}

.artwork-main-header p {
  margin: 0.2rem 0 0;
  color: var(--muted);
  font-size: 0.9rem;
  font-weight: 750;
}

.artwork-value-pill {
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

.artwork-meta-grid {
  display: grid;
  grid-template-columns: 110px 170px minmax(0, 1fr) minmax(0, 1fr);
  gap: 0.75rem;
  margin-top: 0.85rem;
}

.artwork-meta-grid div {
  min-width: 0;
  padding: 0.7rem 0.8rem;
  border: 1px solid var(--border);
  border-radius: 15px;
  background: rgba(248, 250, 255, 0.82);
}

.artwork-meta-grid span {
  display: block;
  color: var(--muted);
  font-size: 0.72rem;
  font-weight: 950;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.artwork-meta-grid strong {
  display: block;
  margin-top: 0.18rem;
  color: var(--ink);
  font-size: 0.9rem;
  font-weight: 850;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.artwork-actions {
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

.artworks-empty-state {
  display: grid;
  justify-items: center;
  gap: 0.65rem;
  padding: 3rem 1rem;
  text-align: center;
}

.artworks-empty-icon {
  display: grid;
  place-items: center;
  width: 62px;
  height: 62px;
  border-radius: 22px;
  background: var(--primary-soft);
  font-size: 1.55rem;
}

.artworks-empty-state h2 {
  margin: 0;
  color: var(--ink);
  font-size: 1.25rem;
}

.artworks-empty-state p {
  margin: 0;
  color: var(--muted);
}

.artworks-pagination {
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
  .artwork-meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1050px) {
  .artwork-row-card {
    grid-template-columns: 64px minmax(0, 1fr);
  }

  .artwork-actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }
}

@media (max-width: 900px) {
  .artworks-page-header {
    flex-direction: column;
  }

  .artworks-header-actions {
    padding-top: 0;
  }

  .artwork-stats-grid,
  .artworks-toolbar,
  .artwork-meta-grid {
    grid-template-columns: 1fr;
  }

  .artworks-search-row {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .artworks-card {
    padding: 0.9rem;
    border-radius: 20px;
  }

  .artwork-row-card {
    grid-template-columns: 1fr;
  }

  .artwork-icon-card {
    width: 50px;
    height: 50px;
    border-radius: 17px;
  }

  .artwork-main-header {
    flex-direction: column;
  }

  .artwork-actions {
    flex-direction: column;
  }

  .artwork-actions .btn,
  .artwork-actions button {
    width: 100%;
  }

  .artworks-pagination {
    flex-direction: column;
  }
}
</style>
