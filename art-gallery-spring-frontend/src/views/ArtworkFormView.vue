<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { artworks, artists, collections, locations } from '../api/resources';
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
  artistId: null,
  yearCreated: null,
  medium: '',
  collectionId: null,
  locationId: null,
  estimatedValue: null,
});

const artistOptions = ref([]);
const collectionOptions = ref([]);
const locationOptions = ref([]);

const fieldErrors = ref({});
const formError = ref(null);
const loading = ref(false);
const initialLoading = ref(true);

const selectedArtist = computed(() =>
  artistOptions.value.find((artist) => Number(artist.id) === Number(form.artistId)),
);

const selectedCollection = computed(() =>
  collectionOptions.value.find((collection) => Number(collection.id) === Number(form.collectionId)),
);

const selectedLocation = computed(() =>
  locationOptions.value.find((location) => Number(location.id) === Number(form.locationId)),
);

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

function formatMoney(value) {
  const numberValue = Number(value || 0);

  if (!numberValue) {
    return 'Not set';
  }

  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(numberValue);
}

async function submit() {
  fieldErrors.value = {};
  formError.value = null;
  loading.value = true;

  const payload = {
    title: form.title?.trim() || null,
    artistId: form.artistId ? Number(form.artistId) : null,
    yearCreated: form.yearCreated ? Number(form.yearCreated) : null,
    medium: form.medium?.trim() || null,
    collectionId: form.collectionId ? Number(form.collectionId) : null,
    locationId: form.locationId ? Number(form.locationId) : null,
    estimatedValue: form.estimatedValue ? Number(form.estimatedValue) : null,
  };

  try {
    if (isEdit.value) {
      await artworks.update(props.id, payload);
    } else {
      await artworks.create(payload);
    }

    router.push({ name: 'artworks' });
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
    const [loadedArtists, loadedCollections, loadedLocations] = await Promise.all([
      artists.all(),
      collections.all(),
      locations.all(),
    ]);

    artistOptions.value = asArray(loadedArtists);
    collectionOptions.value = asArray(loadedCollections);
    locationOptions.value = asArray(loadedLocations);

    if (isEdit.value) {
      const loadedArtwork = await artworks.get(props.id);
      const artwork = asEntity(loadedArtwork);

      Object.assign(form, {
        title: artwork.title || '',
        artistId: artwork.artistId || null,
        yearCreated: artwork.yearCreated || null,
        medium: artwork.medium || '',
        collectionId: artwork.collectionId || null,
        locationId: artwork.locationId || null,
        estimatedValue: artwork.estimatedValue || null,
      });
    }
  } catch (error) {
    formError.value = normalizeError(error).message;
  } finally {
    initialLoading.value = false;
  }
});
</script>

<template>
  <div class="form-page artwork-form-page">
    <div class="form-page-header">
      <div>
        <p class="eyebrow">Artwork catalogue</p>
        <h1>{{ isEdit ? 'Edit Artwork' : 'New Artwork' }}</h1>
        <p class="muted">
          {{ isEdit ? 'Update artwork catalogue information.' : 'Add a new artwork to the gallery inventory.' }}
        </p>
      </div>

      <router-link class="btn btn-secondary" :to="{ name: 'artworks' }">
        ← Back
      </router-link>
    </div>

    <div class="form-shell artwork-form-shell">
      <section class="entity-form-card">
        <div class="entity-form-card-header">
          <div class="entity-form-icon">🖼️</div>

          <div>
            <p class="eyebrow">Form</p>
            <h2>Artwork details</h2>
            <p>
              Connect the artwork with an artist, a collection and a gallery location.
            </p>
          </div>
        </div>

        <div v-if="formError" class="alert alert-error">
          {{ formError }}
        </div>

        <div v-if="initialLoading" class="empty compact">
          Loading artwork data...
        </div>

        <form v-else class="entity-form" @submit.prevent="submit">
          <section class="form-section">
            <div class="form-section-header">
              <div>
                <h3>Main information</h3>
                <p>Title and artist are required for catalogue consistency.</p>
              </div>
            </div>

            <div class="field full-field">
              <label for="title">Title *</label>

              <input
                id="title"
                v-model="form.title"
                type="text"
                placeholder="Artwork title"
                :class="{ invalid: fieldErrors.title }"
                @input="clearFieldErrors('title')"
              />

              <div v-if="fieldErrors.title" class="field-error">
                {{ fieldErrors.title }}
              </div>
            </div>

            <div class="grid-2">
              <div class="field">
                <label for="artistId">Artist *</label>

                <select
                  id="artistId"
                  v-model="form.artistId"
                  :class="{ invalid: fieldErrors.artistId }"
                  @change="clearFieldErrors('artistId')"
                >
                  <option :value="null" disabled>— Select artist —</option>
                  <option
                    v-for="artist in artistOptions"
                    :key="artist.id"
                    :value="artist.id"
                  >
                    {{ artist.name }}
                  </option>
                </select>

                <div v-if="fieldErrors.artistId" class="field-error">
                  {{ fieldErrors.artistId }}
                </div>
              </div>

              <div class="field">
                <label for="yearCreated">Year Created</label>

                <input
                  id="yearCreated"
                  v-model="form.yearCreated"
                  type="number"
                  inputmode="numeric"
                  placeholder="e.g. 1937"
                  :class="{ invalid: fieldErrors.yearCreated }"
                  @input="clearFieldErrors('yearCreated')"
                />

                <div v-if="fieldErrors.yearCreated" class="field-error">
                  {{ fieldErrors.yearCreated }}
                </div>
              </div>
            </div>

            <div class="field full-field">
              <label for="medium">Medium</label>

              <input
                id="medium"
                v-model="form.medium"
                type="text"
                placeholder="e.g. Oil on Canvas"
                :class="{ invalid: fieldErrors.medium }"
                @input="clearFieldErrors('medium')"
              />

              <div v-if="fieldErrors.medium" class="field-error">
                {{ fieldErrors.medium }}
              </div>
            </div>
          </section>

          <section class="form-section">
            <div class="form-section-header">
              <div>
                <h3>Placement and value</h3>
                <p>Select where the artwork belongs and estimate its value.</p>
              </div>
            </div>

            <div class="grid-2">
              <div class="field">
                <label for="collectionId">Collection</label>

                <select
                  id="collectionId"
                  v-model="form.collectionId"
                  :class="{ invalid: fieldErrors.collectionId }"
                  @change="clearFieldErrors('collectionId')"
                >
                  <option :value="null">— No collection —</option>
                  <option
                    v-for="collection in collectionOptions"
                    :key="collection.id"
                    :value="collection.id"
                  >
                    {{ collection.name }}
                  </option>
                </select>

                <div v-if="fieldErrors.collectionId" class="field-error">
                  {{ fieldErrors.collectionId }}
                </div>
              </div>

              <div class="field">
                <label for="locationId">Location</label>

                <select
                  id="locationId"
                  v-model="form.locationId"
                  :class="{ invalid: fieldErrors.locationId }"
                  @change="clearFieldErrors('locationId')"
                >
                  <option :value="null">— No location —</option>
                  <option
                    v-for="location in locationOptions"
                    :key="location.id"
                    :value="location.id"
                  >
                    {{ location.name }}
                  </option>
                </select>

                <div v-if="fieldErrors.locationId" class="field-error">
                  {{ fieldErrors.locationId }}
                </div>
              </div>
            </div>

            <div class="field full-field">
              <label for="estimatedValue">Estimated Value (USD)</label>

              <input
                id="estimatedValue"
                v-model="form.estimatedValue"
                type="number"
                min="0"
                step="0.01"
                inputmode="decimal"
                placeholder="Estimated value"
                :class="{ invalid: fieldErrors.estimatedValue }"
                @input="clearFieldErrors('estimatedValue')"
              />

              <div v-if="fieldErrors.estimatedValue" class="field-error">
                {{ fieldErrors.estimatedValue }}
              </div>
            </div>
          </section>

          <aside class="form-summary-card">
            <span>Preview</span>
            <strong>{{ form.title || 'Untitled artwork' }}</strong>
            <small>{{ selectedArtist?.name || 'No artist selected' }}</small>

            <div class="summary-chips">
              <em>{{ selectedCollection?.name || 'No collection' }}</em>
              <em>{{ selectedLocation?.name || 'No location' }}</em>
              <em>{{ formatMoney(form.estimatedValue) }}</em>
            </div>
          </aside>

          <div class="form-actions">
            <router-link class="btn btn-secondary" :to="{ name: 'artworks' }">
              Cancel
            </router-link>

            <button type="submit" class="btn btn-primary" :disabled="loading">
              {{ loading ? 'Saving...' : 'Save Artwork' }}
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

.form-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.25rem;
  width: min(1040px, 100%);
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

.form-shell {
  width: min(880px, 100%);
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

.form-section {
  margin-bottom: 1rem;
  padding: 1.1rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--shadow-soft);
}

.form-section-header {
  margin-bottom: 0.9rem;
}

.form-section-header h3 {
  margin: 0;
  color: var(--ink);
  font-size: 1rem;
  letter-spacing: -0.02em;
}

.form-section-header p {
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
  border: 1px solid #d7e1ff;
  border-radius: 20px;
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

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.75rem;
  margin-top: 1.15rem;
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

  .form-actions {
    flex-direction: column-reverse;
  }

  .form-actions .btn,
  .form-actions button {
    width: 100%;
  }
}
</style>
