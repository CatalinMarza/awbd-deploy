<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { artists, search } from '../api/resources';
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

const livingArtists = computed(() =>
  rows.value.filter((artist) => !artist.deathYear).length,
);

const nationalitiesOnPage = computed(() => {
  const set = new Set(
    rows.value
      .map((artist) => artist.nationality)
      .filter(Boolean),
  );

  return set.size;
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

function artistInitials(artist) {
  return String(artist.name || '?')
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('') || '?';
}

async function loadArtists() {
  loading.value = true;
  error.value = null;

  try {
    const params = {
      page: page.value,
      size: size.value,
      sort: sort.value,
    };

    const result = appliedSearch.value
      ? await search.artists(appliedSearch.value, params)
      : await artists.list(params);

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
  router.push({ name: 'artist-new' });
}

function goToEdit(artist) {
  router.push({ name: 'artist-edit', params: { id: artist.id } });
}

async function removeArtist(artist) {
  const confirmed = window.confirm(`Delete artist "${artist.name}"?`);

  if (!confirmed) {
    return;
  }

  try {
    await artists.remove(artist.id);

    if (rows.value.length === 1 && page.value > 0) {
      page.value -= 1;
    }

    await loadArtists();
  } catch (err) {
    error.value = normalizeError(err).message;
  }
}

async function applySearch() {
  appliedSearch.value = searchTerm.value.trim();
  page.value = 0;
  await loadArtists();
}

async function clearSearch() {
  searchTerm.value = '';
  appliedSearch.value = '';
  page.value = 0;
  await loadArtists();
}

async function previousPage() {
  if (!hasPrevious.value) {
    return;
  }

  page.value -= 1;
  await loadArtists();
}

async function nextPage() {
  if (!hasNext.value) {
    return;
  }

  page.value += 1;
  await loadArtists();
}

async function changeSort(value) {
  sort.value = value;
  page.value = 0;
  await loadArtists();
}

onMounted(loadArtists);
</script>

<template>
  <div class="artists-page">
    <div class="artists-page-header">
      <div>
        <p class="eyebrow">Artist management</p>
        <h1>Artists</h1>
        <p class="muted">
          Manage artist profiles used by the artwork catalogue.
        </p>
      </div>

      <div class="artists-header-actions">
        <button
          v-if="isAdmin"
          type="button"
          class="btn btn-primary"
          @click="goToCreate"
        >
          + New Artist
        </button>
      </div>
    </div>

    <div class="artist-stats-grid">
      <div class="artist-stat-card">
        <span>Total artists</span>
        <strong>{{ totalElements }}</strong>
        <small>Saved artist profiles</small>
      </div>

      <div class="artist-stat-card">
        <span>Living on page</span>
        <strong>{{ livingArtists }}</strong>
        <small>Artists without death year</small>
      </div>

      <div class="artist-stat-card">
        <span>Nationalities</span>
        <strong>{{ nationalitiesOnPage }}</strong>
        <small>Distinct on current page</small>
      </div>
    </div>

    <section class="artists-card">
      <div class="artists-toolbar">
        <div class="artists-search">
          <label for="artistSearch">Search artists</label>

          <div class="artists-search-row">
            <input
              id="artistSearch"
              v-model="searchTerm"
              type="search"
              placeholder="Search by artist name..."
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

        <div class="artists-sort">
          <label for="artistSort">Sort</label>

          <select
            id="artistSort"
            :value="sort"
            @change="changeSort($event.target.value)"
          >
            <option value="name,asc">Name A-Z</option>
            <option value="name,desc">Name Z-A</option>
            <option value="birthYear,asc">Birth year asc</option>
            <option value="birthYear,desc">Birth year desc</option>
            <option value="id,desc">Newest ID</option>
          </select>
        </div>
      </div>

      <div v-if="error" class="alert alert-error">
        {{ error }}
      </div>

      <div v-if="loading" class="empty compact">
        Loading artists...
      </div>

      <div v-else-if="!rows.length" class="artists-empty-state">
        <div class="artists-empty-icon">🎨</div>
        <h2>No artists found</h2>
        <p>Create an artist before adding artworks.</p>

        <button
          v-if="isAdmin"
          type="button"
          class="btn btn-primary"
          @click="goToCreate"
        >
          + New Artist
        </button>
      </div>

      <div v-else class="artist-list">
        <article
          v-for="artist in rows"
          :key="artist.id"
          class="artist-row-card"
        >
          <div class="artist-avatar">
            {{ artistInitials(artist) }}
          </div>

          <div class="artist-main">
            <div class="artist-main-header">
              <div>
                <h2>{{ artist.name }}</h2>
                <p>{{ artist.nationality || 'Unknown nationality' }}</p>
              </div>
            </div>

            <div class="artist-meta-grid">
              <div>
                <span>Birth year</span>
                <strong>{{ artist.birthYear || '—' }}</strong>
              </div>

              <div>
                <span>Death year</span>
                <strong>{{ artist.deathYear || 'Present' }}</strong>
              </div>
            </div>
          </div>

          <div v-if="isAdmin" class="artist-actions">
            <button
              type="button"
              class="btn btn-secondary"
              @click="goToEdit(artist)"
            >
              Edit
            </button>

            <button
              type="button"
              class="btn btn-danger-soft"
              @click="removeArtist(artist)"
            >
              Delete
            </button>
          </div>
        </article>
      </div>

      <div class="artists-pagination">
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
.artists-page {
  width: 100%;
}

.artists-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.25rem;
  margin-bottom: 1.35rem;
}

.artists-page-header h1 {
  margin: 0;
  color: var(--ink);
  font-size: clamp(1.85rem, 3vw, 2.55rem);
  line-height: 1.05;
  letter-spacing: -0.055em;
}

.artists-page-header p {
  max-width: 760px;
  margin-top: 0.7rem;
  line-height: 1.6;
}

.artists-header-actions {
  flex: 0 0 auto;
  padding-top: 0.25rem;
}

.artist-stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.85rem;
  margin-bottom: 1rem;
}

.artist-stat-card {
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.96));
  box-shadow: var(--shadow-soft);
}

.artist-stat-card span {
  display: block;
  color: var(--muted);
  font-size: 0.76rem;
  font-weight: 950;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.artist-stat-card strong {
  display: block;
  margin-top: 0.35rem;
  color: var(--ink);
  font-size: 1.35rem;
  font-weight: 950;
}

.artist-stat-card small {
  display: block;
  margin-top: 0.15rem;
  color: var(--muted);
  font-size: 0.82rem;
  font-weight: 750;
}

.artists-card {
  padding: 1.1rem;
  border: 1px solid var(--border);
  border-radius: 26px;
  background:
    radial-gradient(circle at top right, rgba(95, 125, 232, 0.08), transparent 18rem),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.96));
  box-shadow: var(--shadow);
}

.artists-toolbar {
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

.artists-toolbar label {
  display: block;
  margin-bottom: 0.45rem;
  color: var(--ink);
  font-size: 0.83rem;
  font-weight: 950;
}

.artists-search-row {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}

.artists-search-row input {
  flex: 1 1 auto;
}

.artists-sort select {
  width: 100%;
  min-height: 46px;
}

.artist-list {
  display: grid;
  gap: 0.85rem;
}

.artist-row-card {
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

.artist-avatar {
  display: grid;
  place-items: center;
  width: 58px;
  height: 58px;
  border-radius: 20px;
  background: linear-gradient(135deg, #a78bfa, var(--primary));
  color: white;
  font-size: 1.05rem;
  font-weight: 950;
  box-shadow: 0 12px 26px rgba(95, 125, 232, 0.2);
}

.artist-main {
  min-width: 0;
}

.artist-main-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 0.9rem;
}

.artist-main-header h2 {
  margin: 0;
  color: var(--ink);
  font-size: 1.06rem;
  font-weight: 950;
  letter-spacing: -0.025em;
}

.artist-main-header p {
  margin: 0.2rem 0 0;
  color: var(--muted);
  font-size: 0.9rem;
  font-weight: 750;
}

.artist-meta-grid {
  display: grid;
  grid-template-columns: 160px 160px;
  gap: 0.75rem;
  margin-top: 0.85rem;
}

.artist-meta-grid div {
  min-width: 0;
  padding: 0.7rem 0.8rem;
  border: 1px solid var(--border);
  border-radius: 15px;
  background: rgba(248, 250, 255, 0.82);
}

.artist-meta-grid span {
  display: block;
  color: var(--muted);
  font-size: 0.72rem;
  font-weight: 950;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.artist-meta-grid strong {
  display: block;
  margin-top: 0.18rem;
  color: var(--ink);
  font-size: 0.9rem;
  font-weight: 850;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.artist-actions {
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

.artists-empty-state {
  display: grid;
  justify-items: center;
  gap: 0.65rem;
  padding: 3rem 1rem;
  text-align: center;
}

.artists-empty-icon {
  display: grid;
  place-items: center;
  width: 62px;
  height: 62px;
  border-radius: 22px;
  background: var(--primary-soft);
  font-size: 1.55rem;
}

.artists-empty-state h2 {
  margin: 0;
  color: var(--ink);
  font-size: 1.25rem;
}

.artists-empty-state p {
  margin: 0;
  color: var(--muted);
}

.artists-pagination {
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
  .artist-row-card {
    grid-template-columns: 64px minmax(0, 1fr);
  }

  .artist-actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }
}

@media (max-width: 900px) {
  .artists-page-header {
    flex-direction: column;
  }

  .artists-header-actions {
    padding-top: 0;
  }

  .artist-stats-grid,
  .artists-toolbar,
  .artist-meta-grid {
    grid-template-columns: 1fr;
  }

  .artists-search-row {
    flex-direction: column;
    align-items: stretch;
  }
}

@media (max-width: 640px) {
  .artists-card {
    padding: 0.9rem;
    border-radius: 20px;
  }

  .artist-row-card {
    grid-template-columns: 1fr;
  }

  .artist-avatar {
    width: 50px;
    height: 50px;
    border-radius: 17px;
  }

  .artist-main-header {
    flex-direction: column;
  }

  .artist-actions {
    flex-direction: column;
  }

  .artist-actions .btn,
  .artist-actions button {
    width: 100%;
  }

  .artists-pagination {
    flex-direction: column;
  }
}
</style>
