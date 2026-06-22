<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  artworks,
  exhibitions,
  exhibitionArtworks,
  exhibitors,
} from '../api/resources';
import { normalizeError } from '../api/http';

const props = defineProps({
  id: {
    type: String,
    default: null,
  },
});

const router = useRouter();
const isEdit = computed(() => props.id != null);

const form = reactive({
  title: '',
  startDate: '',
  endDate: '',
  exhibitorId: null,
  description: '',
});

const exhibitorOptions = ref([]);
const artworkOptions = ref([]);
const selectedArtworkIds = ref([]);
const originalArtworkIds = ref([]);
const artworkSearch = ref('');

const fieldErrors = ref({});
const formError = ref(null);
const loading = ref(false);
const initialLoading = ref(true);

const selectedExhibitor = computed(() =>
  exhibitorOptions.value.find(
    (exhibitor) => Number(exhibitor.id) === Number(form.exhibitorId),
  ),
);

const selectedArtworks = computed(() => {
  const ids = selectedArtworkIds.value.map(Number);

  return ids
    .map((id) => artworkOptions.value.find((artwork) => Number(artwork.id) === id))
    .filter(Boolean);
});

const availableArtworks = computed(() => {
  const selectedIds = new Set(selectedArtworkIds.value.map(Number));
  const term = artworkSearch.value.trim().toLowerCase();

  return artworkOptions.value
    .filter((artwork) => !selectedIds.has(Number(artwork.id)))
    .filter((artwork) => {
      if (!term) return true;

      return [
        artwork.title,
        artwork.artistName,
        artwork.medium,
        artwork.yearCreated,
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(term);
    });
});

const selectedCountLabel = computed(() => {
  const count = selectedArtworkIds.value.length;

  if (count === 1) {
    return '1 selected';
  }

  return `${count} selected`;
});

function asArray(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data?.content)) return payload.data.content;
  return [];
}

function asEntity(payload) {
  if (payload?.data) return payload.data;
  return payload || {};
}

function clearFieldErrors(...keys) {
  const next = { ...fieldErrors.value };
  keys.forEach((key) => delete next[key]);
  fieldErrors.value = next;
}

function isSelected(artworkId) {
  return selectedArtworkIds.value.map(Number).includes(Number(artworkId));
}

function toggleArtwork(artworkId) {
  const numericId = Number(artworkId);
  const currentIds = selectedArtworkIds.value.map(Number);

  if (currentIds.includes(numericId)) {
    selectedArtworkIds.value = currentIds.filter((id) => id !== numericId);
  } else {
    selectedArtworkIds.value = [...currentIds, numericId];
  }
}

function selectVisibleArtworks() {
  const current = new Set(selectedArtworkIds.value.map(Number));

  availableArtworks.value.forEach((artwork) => {
    current.add(Number(artwork.id));
  });

  selectedArtworkIds.value = Array.from(current);
}

function clearSelectedArtworks() {
  selectedArtworkIds.value = [];
}

function removeSelectedArtwork(artworkId) {
  const numericId = Number(artworkId);
  selectedArtworkIds.value = selectedArtworkIds.value
    .map(Number)
    .filter((id) => id !== numericId);
}

async function syncArtworks(exhibitionId) {
  const original = new Set(originalArtworkIds.value.map(Number));
  const selected = new Set(selectedArtworkIds.value.map(Number));

  const toAdd = [...selected].filter((id) => !original.has(id));
  const toRemove = [...original].filter((id) => !selected.has(id));

  await Promise.all([
    ...toAdd.map((artworkId) => exhibitionArtworks.add(exhibitionId, artworkId)),
    ...toRemove.map((artworkId) => exhibitionArtworks.remove(exhibitionId, artworkId)),
  ]);

  originalArtworkIds.value = [...selected];
}

async function submit() {
  fieldErrors.value = {};
  formError.value = null;
  loading.value = true;

  const payload = {
    title: form.title?.trim() || null,
    startDate: form.startDate || null,
    endDate: form.endDate || null,
    exhibitorId: form.exhibitorId ? Number(form.exhibitorId) : null,
    description: form.description?.trim() || null,
  };

  try {
    let saved;

    if (isEdit.value) {
      saved = await exhibitions.update(props.id, payload);
    } else {
      saved = await exhibitions.create(payload);
    }

    const savedExhibition = asEntity(saved);
    const exhibitionId = props.id || savedExhibition.id;

    if (exhibitionId) {
      await syncArtworks(exhibitionId);
    }

    router.push({ name: 'exhibitions' });
  } catch (error) {
    const normalized = normalizeError(error);

    if (Object.keys(normalized.fieldErrors).length) {
      fieldErrors.value = normalized.fieldErrors;
    } else {
      formError.value = normalized.message;
    }
  } finally {
    loading.value = false;
  }
}

onMounted(async () => {
  try {
    const [loadedExhibitors, loadedArtworks] = await Promise.all([
      exhibitors.all(),
      artworks.all(),
    ]);

    exhibitorOptions.value = asArray(loadedExhibitors);
    artworkOptions.value = asArray(loadedArtworks);

    if (isEdit.value) {
      const [loadedExhibition, linkedArtworks] = await Promise.all([
        exhibitions.get(props.id),
        exhibitionArtworks.list(props.id),
      ]);

      const exhibition = asEntity(loadedExhibition);
      const linkedIds = asArray(linkedArtworks).map((artwork) => Number(artwork.id));

      Object.assign(form, {
        title: exhibition.title || '',
        startDate: exhibition.startDate || '',
        endDate: exhibition.endDate || '',
        exhibitorId: exhibition.exhibitorId || null,
        description: exhibition.description || '',
      });

      selectedArtworkIds.value = linkedIds;
      originalArtworkIds.value = linkedIds;
    }
  } catch (error) {
    formError.value = normalizeError(error).message;
  } finally {
    initialLoading.value = false;
  }
});
</script>

<template>
  <div class="form-page exhibition-form-page">
    <div class="form-page-header exhibition-header">
      <div>
        <p class="eyebrow">Exhibition schedule</p>
        <h1>{{ isEdit ? 'Edit Exhibition' : 'New Exhibition' }}</h1>
        <p class="muted">
          Create an exhibition and assign artworks in the same screen.
        </p>
      </div>

      <router-link class="btn btn-secondary" :to="{ name: 'exhibitions' }">
        ← Back
      </router-link>
    </div>

    <div class="form-shell exhibition-form-shell">
      <section class="entity-form-card exhibition-form-card">
        <div class="entity-form-card-header">
          <div class="entity-form-icon">🏛️</div>

          <div>
            <p class="eyebrow">Form</p>
            <h2>Exhibition details</h2>
            <p>
              Complete the event details and choose the artworks that will be displayed.
            </p>
          </div>
        </div>

        <div v-if="formError" class="alert alert-error">
          {{ formError }}
        </div>

        <div v-if="initialLoading" class="empty compact">
          Loading exhibition data...
        </div>

        <form v-else class="entity-form exhibition-layout" @submit.prevent="submit">
          <div class="exhibition-left">
            <section class="form-section">
              <div class="form-section-header">
                <div>
                  <h3>Event information</h3>
                  <p>Title, period and exhibitor define the exhibition schedule.</p>
                </div>
              </div>

              <div class="field full-field">
                <label for="title">Title *</label>

                <input
                  id="title"
                  v-model="form.title"
                  type="text"
                  placeholder="Exhibition title"
                  :class="{ invalid: fieldErrors.title }"
                  @input="clearFieldErrors('title')"
                />

                <div v-if="fieldErrors.title" class="field-error">
                  {{ fieldErrors.title }}
                </div>
              </div>

              <div class="grid-2">
                <div class="field">
                  <label for="startDate">Start Date *</label>

                  <input
                    id="startDate"
                    v-model="form.startDate"
                    type="date"
                    :class="{ invalid: fieldErrors.startDate }"
                    @input="clearFieldErrors('startDate')"
                  />

                  <div v-if="fieldErrors.startDate" class="field-error">
                    {{ fieldErrors.startDate }}
                  </div>
                </div>

                <div class="field">
                  <label for="endDate">End Date *</label>

                  <input
                    id="endDate"
                    v-model="form.endDate"
                    type="date"
                    :class="{ invalid: fieldErrors.endDate }"
                    @input="clearFieldErrors('endDate')"
                  />

                  <div v-if="fieldErrors.endDate" class="field-error">
                    {{ fieldErrors.endDate }}
                  </div>
                </div>
              </div>

              <div class="field full-field">
                <label for="exhibitorId">Exhibitor *</label>

                <select
                  id="exhibitorId"
                  v-model="form.exhibitorId"
                  :class="{ invalid: fieldErrors.exhibitorId }"
                  @change="clearFieldErrors('exhibitorId')"
                >
                  <option :value="null" disabled>— Select exhibitor —</option>
                  <option
                    v-for="exhibitor in exhibitorOptions"
                    :key="exhibitor.id"
                    :value="exhibitor.id"
                  >
                    {{ exhibitor.name }}
                  </option>
                </select>

                <div v-if="fieldErrors.exhibitorId" class="field-error">
                  {{ fieldErrors.exhibitorId }}
                </div>
              </div>

              <div class="field full-field">
                <label for="description">Description</label>

                <textarea
                  id="description"
                  v-model="form.description"
                  rows="4"
                  placeholder="Short exhibition description..."
                  :class="{ invalid: fieldErrors.description }"
                  @input="clearFieldErrors('description')"
                ></textarea>

                <div v-if="fieldErrors.description" class="field-error">
                  {{ fieldErrors.description }}
                </div>
              </div>
            </section>

            <aside class="form-summary-card">
              <span>Preview</span>
              <strong>{{ form.title || 'Untitled exhibition' }}</strong>
              <small>{{ selectedExhibitor?.name || 'No exhibitor selected' }}</small>

              <div class="summary-chips">
                <em>{{ form.startDate || 'No start date' }}</em>
                <em>{{ form.endDate || 'No end date' }}</em>
                <em>{{ selectedCountLabel }}</em>
              </div>
            </aside>
          </div>

          <aside class="artwork-assignment-card">
            <div class="artwork-assignment-header">
              <div>
                <h3>Artworks</h3>
                <p>Select artworks assigned to this exhibition.</p>
              </div>

              <span>{{ selectedCountLabel }}</span>
            </div>

            <div class="artwork-search-row">
              <input
                v-model="artworkSearch"
                type="search"
                placeholder="Search available artworks..."
              />

              <button type="button" class="btn btn-secondary" @click="selectVisibleArtworks">
                Select visible
              </button>
            </div>

            <div class="available-artwork-list">
              <label
                v-for="artwork in availableArtworks"
                :key="artwork.id"
                class="artwork-option"
              >
                <input
                  type="checkbox"
                  :checked="isSelected(artwork.id)"
                  @change="toggleArtwork(artwork.id)"
                />

                <span>
                  <strong>{{ artwork.title }}</strong>
                  <small>{{ artwork.artistName || 'Unknown artist' }}</small>
                </span>
              </label>

              <div v-if="!availableArtworks.length" class="empty compact">
                No available artworks.
              </div>
            </div>

            <div class="selected-artwork-panel">
              <div class="selected-artwork-header">
                <h4>Selected artworks</h4>

                <button
                  type="button"
                  class="btn btn-secondary small-action"
                  :disabled="!selectedArtworkIds.length"
                  @click="clearSelectedArtworks"
                >
                  Clear
                </button>
              </div>

              <div v-if="!selectedArtworks.length" class="empty compact">
                No artworks selected yet.
              </div>

              <div v-else class="selected-artwork-list">
                <div
                  v-for="artwork in selectedArtworks"
                  :key="artwork.id"
                  class="selected-artwork"
                >
                  <span>
                    <strong>{{ artwork.title }}</strong>
                    <small>{{ artwork.artistName || 'Unknown artist' }}</small>
                  </span>

                  <button
                    type="button"
                    class="remove-artwork-btn"
                    @click="removeSelectedArtwork(artwork.id)"
                  >
                    Remove
                  </button>
                </div>
              </div>
            </div>
          </aside>

          <div class="form-actions exhibition-actions">
            <router-link class="btn btn-secondary" :to="{ name: 'exhibitions' }">
              Cancel
            </router-link>

            <button type="submit" class="btn btn-primary" :disabled="loading">
              {{ loading ? 'Saving...' : 'Save Exhibition' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  </div>
</template>

<style scoped>
.form-page {
  width: 100%;
}

.exhibition-form-page {
  display: block;
}

.form-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.25rem;
  width: min(980px, 100%);
  margin: 0 auto 1.35rem;
}

.form-page-header h1 {
  margin: 0;
  color: var(--ink);
  font-size: clamp(1.9rem, 3vw, 2.6rem);
  line-height: 1.05;
  letter-spacing: -0.055em;
}

.form-page-header p {
  max-width: 680px;
  margin-top: 0.7rem;
  line-height: 1.6;
}

.exhibition-form-shell {
  width: min(980px, 100%);
  margin: 0 auto;
}

.entity-form-card {
  overflow: hidden;
  padding: 1.45rem;
  border: 1px solid var(--border);
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(95, 125, 232, 0.1), transparent 18rem),
    linear-gradient(145deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.96));
  box-shadow: var(--shadow);
}

.entity-form-card-header {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  gap: 1rem;
  align-items: flex-start;
  margin-bottom: 1rem;
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.78);
}

.entity-form-card-header h2 {
  margin: 0;
  color: var(--ink);
  font-size: 1.35rem;
  letter-spacing: -0.03em;
}

.entity-form-card-header p {
  margin: 0.45rem 0 0;
  color: var(--muted);
  line-height: 1.5;
}

.entity-form-icon {
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  border-radius: 18px;
  background: linear-gradient(135deg, #8aa3ff, var(--primary));
  box-shadow: 0 12px 26px rgba(95, 125, 232, 0.2);
  font-size: 1.35rem;
}

.entity-form {
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.exhibition-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 1rem;
  align-items: start;
}

.exhibition-left {
  min-width: 0;
}

.form-section,
.artwork-assignment-card,
.form-summary-card {
  border: 1px solid var(--border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--shadow-soft);
}

.form-section {
  padding: 1.1rem;
}

.form-section-header {
  margin-bottom: 0.9rem;
}

.form-section-header h3,
.artwork-assignment-header h3 {
  margin: 0;
  color: var(--ink);
  font-size: 1rem;
  letter-spacing: -0.02em;
}

.form-section-header p,
.artwork-assignment-header p {
  margin: 0.25rem 0 0;
  color: var(--muted);
  font-size: 0.9rem;
  line-height: 1.45;
}

.full-field {
  width: 100%;
}

.form-section .field {
  margin-bottom: 0.9rem;
}

.form-section .field:last-child {
  margin-bottom: 0;
}

.form-section .grid-2 {
  gap: 1rem;
}

.form-summary-card {
  display: grid;
  gap: 0.2rem;
  margin-top: 1rem;
  padding: 1rem;
  background:
    linear-gradient(135deg, rgba(238, 243, 255, 0.95), rgba(255, 255, 255, 0.96));
}

.form-summary-card span {
  color: var(--muted);
  font-size: 0.74rem;
  font-weight: 950;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.form-summary-card strong {
  color: var(--ink);
  font-size: 1.1rem;
  font-weight: 950;
}

.form-summary-card small {
  color: var(--muted);
  font-size: 0.88rem;
  font-weight: 750;
}

.summary-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem;
  margin-top: 0.65rem;
}

.summary-chips em {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 0.65rem;
  border: 1px solid #d7e1ff;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary-dark);
  font-size: 0.78rem;
  font-style: normal;
  font-weight: 900;
}

.artwork-assignment-card {
  position: sticky;
  top: 6rem;
  max-height: calc(100vh - 8rem);
  overflow: hidden;
  display: grid;
  grid-template-rows: auto auto minmax(220px, 1fr) auto;
  padding: 1rem;
}

.artwork-assignment-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.9rem;
}

.artwork-assignment-header span {
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

.artwork-search-row {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.65rem;
  margin-bottom: 0.85rem;
}

.available-artwork-list {
  display: grid;
  align-content: start;
  gap: 0.55rem;
  max-height: 340px;
  overflow: auto;
  padding-right: 0.35rem;
}

.artwork-option {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  gap: 0.65rem;
  align-items: center;
  padding: 0.7rem;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: rgba(248, 250, 255, 0.78);
  cursor: pointer;
}

.artwork-option:hover {
  border-color: #d7e1ff;
  background: #fbfcff;
}

.artwork-option strong,
.selected-artwork strong {
  display: block;
  color: var(--ink);
  font-size: 0.9rem;
  font-weight: 900;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.artwork-option small,
.selected-artwork small {
  display: block;
  margin-top: 0.15rem;
  color: var(--muted);
  font-size: 0.8rem;
  font-weight: 750;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.selected-artwork-panel {
  margin-top: 0.9rem;
  padding-top: 0.9rem;
  border-top: 1px solid var(--border);
}

.selected-artwork-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.75rem;
  margin-bottom: 0.65rem;
}

.selected-artwork-header h4 {
  margin: 0;
  color: var(--ink);
  font-size: 0.95rem;
  font-weight: 950;
}

.small-action {
  min-height: 34px;
  padding: 0 0.75rem;
  font-size: 0.82rem;
}

.selected-artwork-list {
  display: grid;
  gap: 0.5rem;
  max-height: 180px;
  overflow: auto;
  padding-right: 0.35rem;
}

.selected-artwork {
  display: grid;
  grid-template-columns: 1fr;
  gap: 0.65rem;
  align-items: center;
  padding: 0.65rem 0.7rem;
  border: 1px solid #d7e1ff;
  border-radius: 15px;
  background: var(--primary-soft);
}

.remove-artwork-btn {
  border: 0;
  background: transparent;
  color: #cf3b3b;
  font-size: 0.82rem;
  font-weight: 900;
  cursor: pointer;
}

.remove-artwork-btn:hover {
  color: #9e1f1f;
}

.exhibition-actions {
  grid-column: 1 / -1;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 0.15rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border);
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

@media (max-width: 1100px) {
  .exhibition-layout {
    grid-template-columns: 1fr;
  }

  .artwork-assignment-card {
    position: static;
    max-height: none;
  }

  .available-artwork-list,
  .selected-artwork-list {
    max-height: 320px;
  }
}

@media (max-width: 760px) {
  .form-page-header {
    flex-direction: column;
  }

  .entity-form-card {
    padding: 1rem;
    border-radius: 22px;
  }

  .entity-form-card-header {
    grid-template-columns: 1fr;
  }

  .artwork-search-row,
  .selected-artwork {
    grid-template-columns: 1fr;
  }

  .form-actions {
    flex-direction: column-reverse;
  }

  .form-actions .btn,
  .form-actions button {
    width: 100%;
  }
}
</style>
