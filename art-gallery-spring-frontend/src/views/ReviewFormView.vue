<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  reviews,
  visitors,
  exhibitions,
  exhibitionArtworks,
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

const today = () => new Date().toISOString().slice(0, 10);

const form = reactive({
  visitorId: null,
  exhibitionId: null,
  artworkId: null,
  rating: 5,
  reviewText: '',
  reviewDate: today(),
});

const visitorOptions = ref([]);
const exhibitionOptions = ref([]);
const artworkOptions = ref([]);

const fieldErrors = ref({});
const formError = ref(null);

const initialLoading = ref(true);
const loading = ref(false);
const loadingArtworks = ref(false);
const hoverRating = ref(0);

const selectedExhibition = computed(() =>
  exhibitionOptions.value.find(
    (exhibition) => Number(exhibition.id) === Number(form.exhibitionId),
  ),
);

const selectedArtwork = computed(() =>
  artworkOptions.value.find(
    (artwork) => Number(artwork.id) === Number(form.artworkId),
  ),
);

const artworkPlaceholder = computed(() => {
  if (!form.exhibitionId) return '— Select exhibition first —';
  if (loadingArtworks.value) return 'Loading artworks...';
  if (!artworkOptions.value.length) return 'No artworks assigned';
  return '— Select artwork —';
});

const ratingLabel = computed(() => {
  if (form.rating === 1) return 'Very poor';
  if (form.rating === 2) return 'Poor';
  if (form.rating === 3) return 'Good';
  if (form.rating === 4) return 'Very good';
  return 'Excellent';
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
  const nextErrors = { ...fieldErrors.value };

  keys.forEach((key) => {
    delete nextErrors[key];
  });

  fieldErrors.value = nextErrors;
}

async function loadArtworksForExhibition(exhibitionId, resetArtwork = true) {
  artworkOptions.value = [];

  if (resetArtwork) {
    form.artworkId = null;
  }

  if (!exhibitionId) {
    return;
  }

  loadingArtworks.value = true;

  try {
    const loadedArtworks = await exhibitionArtworks.list(exhibitionId);
    artworkOptions.value = asArray(loadedArtworks);
  } catch (error) {
    formError.value = normalizeError(error).message;
  } finally {
    loadingArtworks.value = false;
  }
}

async function onExhibitionChange() {
  formError.value = null;
  clearFieldErrors('exhibitionId', 'artworkId');
  await loadArtworksForExhibition(form.exhibitionId, true);
}

function onArtworkChange() {
  clearFieldErrors('artworkId');
}

function setRating(value) {
  form.rating = value;
  clearFieldErrors('rating');
}

function previewRating(value) {
  hoverRating.value = value;
}

function resetPreviewRating() {
  hoverRating.value = 0;
}

function isStarActive(value) {
  const activeValue = hoverRating.value || form.rating || 0;
  return value <= activeValue;
}

async function submit() {
  fieldErrors.value = {};
  formError.value = null;
  loading.value = true;

  const payload = {
    visitorId: form.visitorId ? Number(form.visitorId) : null,
    exhibitionId: form.exhibitionId ? Number(form.exhibitionId) : null,
    artworkId: form.artworkId ? Number(form.artworkId) : null,
    rating: form.rating ? Number(form.rating) : null,
    reviewText: form.reviewText?.trim() || null,

    // Hidden field kept for compatibility with the existing backend DTO.
    // It is intentionally not displayed in the form.
    reviewDate: form.reviewDate || today(),
  };

  try {
    if (isEdit.value) {
      await reviews.update(props.id, payload);
    } else {
      await reviews.create(payload);
    }

    router.push({ name: 'reviews' });
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
    const [loadedVisitors, loadedExhibitions] = await Promise.all([
      visitors.all(),
      exhibitions.all(),
    ]);

    visitorOptions.value = asArray(loadedVisitors);
    exhibitionOptions.value = asArray(loadedExhibitions);

    if (isEdit.value) {
      const loadedReview = await reviews.get(props.id);
      const review = asEntity(loadedReview);

      Object.assign(form, {
        visitorId: review.visitorId || null,
        exhibitionId: review.exhibitionId || null,
        artworkId: null,
        rating: review.rating || 5,
        reviewText: review.reviewText || '',
        reviewDate: review.reviewDate || today(),
      });

      if (form.exhibitionId) {
        await loadArtworksForExhibition(form.exhibitionId, false);
        form.artworkId = review.artworkId || null;
      }
    }
  } catch (error) {
    formError.value = normalizeError(error).message;
  } finally {
    initialLoading.value = false;
  }
});
</script>

<template>
  <div class="review-form-page">
    <div class="review-page-header">
      <div>
        <p class="eyebrow">Review management</p>
        <h1>{{ isEdit ? 'Edit Review' : 'New Review' }}</h1>
        <p class="muted">
          Select an exhibition first, then choose only an artwork assigned to that exhibition.
        </p>
      </div>

      <div class="review-header-actions">
        <router-link class="btn" :to="{ name: 'reviews' }">
          ← Back
        </router-link>
      </div>
    </div>

    <div class="review-form-shell">
      <div class="review-form-card">
        <div class="review-form-header">
          <div class="review-icon">★</div>

          <div>
            <p class="eyebrow">Form</p>
            <h2>Review details</h2>
            <p>
              Create a review by selecting a visitor, an exhibition and one artwork available in that exhibition.
            </p>
          </div>
        </div>

        <div class="review-flow">
          <div class="review-flow-step" :class="{ active: form.visitorId }">
            <span>1</span>
            <strong>Visitor</strong>
          </div>

          <div class="review-flow-step" :class="{ active: form.exhibitionId }">
            <span>2</span>
            <strong>Exhibition</strong>
          </div>

          <div class="review-flow-step" :class="{ active: form.artworkId }">
            <span>3</span>
            <strong>Artwork</strong>
          </div>

          <div class="review-flow-step" :class="{ active: form.rating }">
            <span>4</span>
            <strong>Review</strong>
          </div>
        </div>

        <div v-if="formError" class="alert alert-error">
          {{ formError }}
        </div>

        <div v-if="initialLoading" class="empty compact">
          Loading form data...
        </div>

        <form v-else class="review-form" @submit.prevent="submit">
          <section class="review-field-section">
            <div class="review-field-section-header">
              <div>
                <h3>Visitor</h3>
                <p>
                  Select the gallery visitor associated with this review.
                </p>
              </div>
            </div>

            <div class="field full-field">
              <label for="visitorId">Visitor *</label>

              <select
                id="visitorId"
                v-model="form.visitorId"
                :class="{ invalid: fieldErrors.visitorId }"
                @change="clearFieldErrors('visitorId')"
              >
                <option :value="null" disabled>
                  — Select visitor —
                </option>

                <option
                  v-for="visitor in visitorOptions"
                  :key="visitor.id"
                  :value="visitor.id"
                >
                  {{ visitor.name }}
                </option>
              </select>

              <div v-if="fieldErrors.visitorId" class="field-error">
                {{ fieldErrors.visitorId }}
              </div>
            </div>
          </section>

          <section class="review-field-section">
            <div class="review-field-section-header">
              <div>
                <h3>Exhibition and artwork</h3>
                <p>
                  The artwork list is filtered by the selected exhibition.
                </p>
              </div>

              <span v-if="selectedExhibition" class="review-select-hint">
                {{ selectedExhibition.title }}
              </span>
            </div>

            <div class="grid-2">
              <div class="field">
                <label for="exhibitionId">Exhibition *</label>

                <select
                  id="exhibitionId"
                  v-model="form.exhibitionId"
                  :class="{ invalid: fieldErrors.exhibitionId }"
                  @change="onExhibitionChange"
                >
                  <option :value="null" disabled>
                    — Select exhibition —
                  </option>

                  <option
                    v-for="exhibition in exhibitionOptions"
                    :key="exhibition.id"
                    :value="exhibition.id"
                  >
                    {{ exhibition.title }}
                  </option>
                </select>

                <div v-if="fieldErrors.exhibitionId" class="field-error">
                  {{ fieldErrors.exhibitionId }}
                </div>
              </div>

              <div class="field">
                <label for="artworkId">Artwork *</label>

                <select
                  id="artworkId"
                  v-model="form.artworkId"
                  :disabled="!form.exhibitionId || loadingArtworks || !artworkOptions.length"
                  :class="{ invalid: fieldErrors.artworkId }"
                  @change="onArtworkChange"
                >
                  <option :value="null" disabled>
                    {{ artworkPlaceholder }}
                  </option>

                  <option
                    v-for="artwork in artworkOptions"
                    :key="artwork.id"
                    :value="artwork.id"
                  >
                    {{ artwork.title }} — {{ artwork.artistName || 'Unknown artist' }}
                  </option>
                </select>

                <div v-if="fieldErrors.artworkId" class="field-error">
                  {{ fieldErrors.artworkId }}
                </div>
              </div>
            </div>

            <div
              v-if="form.exhibitionId && !loadingArtworks && !artworkOptions.length"
              class="alert alert-error review-empty-warning"
            >
              This exhibition has no assigned artworks. Assign artworks first before creating a review.
            </div>

            <div v-if="selectedArtwork" class="review-selected-summary">
              <span>Selected artwork</span>
              <strong>{{ selectedArtwork.title }}</strong>
              <small>{{ selectedArtwork.artistName || 'Unknown artist' }}</small>
            </div>
          </section>

          <section class="review-field-section">
            <div class="review-field-section-header">
              <div>
                <h3>Review content</h3>
                <p>
                  Add a rating and optional review text. The review date is handled automatically.
                </p>
              </div>
            </div>

            <div class="field full-field">
              <label>Rating (1–5) *</label>

              <div
                class="star-rating-card"
                :class="{ invalid: fieldErrors.rating }"
                @mouseleave="resetPreviewRating"
              >
                <div class="star-rating">
                  <button
                    v-for="n in 5"
                    :key="n"
                    type="button"
                    class="star-button"
                    :class="{ active: isStarActive(n) }"
                    :aria-label="`Set rating to ${n}`"
                    @click="setRating(n)"
                    @mouseenter="previewRating(n)"
                    @focus="previewRating(n)"
                  >
                    ★
                  </button>
                </div>

                <div class="star-rating-meta">
                  <span class="star-rating-value">{{ form.rating }}/5</span>
                  <span class="star-rating-caption">{{ ratingLabel }}</span>
                </div>
              </div>

              <div v-if="fieldErrors.rating" class="field-error">
                {{ fieldErrors.rating }}
              </div>
            </div>

            <div class="field full-field">
              <label for="reviewText">Review Text</label>

              <textarea
                id="reviewText"
                v-model="form.reviewText"
                class="review-textarea"
                rows="4"
                :class="{ invalid: fieldErrors.reviewText }"
                placeholder="Optional review text..."
                @input="clearFieldErrors('reviewText')"
              ></textarea>

              <div v-if="fieldErrors.reviewText" class="field-error">
                {{ fieldErrors.reviewText }}
              </div>
            </div>
          </section>

          <div class="review-form-actions">
            <router-link class="btn btn-secondary" :to="{ name: 'reviews' }">
              Cancel
            </router-link>

            <button type="submit" class="btn btn-primary" :disabled="loading || loadingArtworks">
              {{ loading ? 'Saving...' : 'Save Review' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.review-form-page {
  width: 100%;
}

.review-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.25rem;
  margin-bottom: 1.35rem;
}

.review-page-header h1 {
  margin: 0;
  color: var(--ink);
  font-size: clamp(1.75rem, 3vw, 2.45rem);
  line-height: 1.05;
  letter-spacing: -0.055em;
}

.review-page-header p {
  max-width: 760px;
  margin-top: 0.7rem;
  line-height: 1.6;
}

.review-header-actions {
  flex: 0 0 auto;
  padding-top: 0.25rem;
}

.review-form-shell {
  width: min(900px, 100%);
  margin: 0 auto;
}

.review-form-card {
  position: relative;
  overflow: hidden;
  padding: 1.45rem;
  border: 1px solid var(--border);
  border-radius: 26px;
  background:
    radial-gradient(circle at top right, rgba(95, 125, 232, 0.08), transparent 18rem),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.96));
  box-shadow: var(--shadow);
}

.review-form-card::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: inherit;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.65), transparent 38%),
    radial-gradient(circle at 8% 12%, rgba(95, 125, 232, 0.08), transparent 12rem);
}

.review-form-card > * {
  position: relative;
  z-index: 1;
}

.review-form-header {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 1rem;
  align-items: flex-start;
  margin-bottom: 1.15rem;
  padding: 1.1rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.76);
}

.review-form-header h2 {
  margin: 0;
  color: var(--ink);
  font-size: 1.45rem;
  line-height: 1.1;
  letter-spacing: -0.035em;
}

.review-form-header p {
  margin: 0.45rem 0 0;
  color: var(--muted);
  line-height: 1.55;
}

.review-icon {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 18px;
  background: linear-gradient(135deg, #8aa3ff, var(--primary));
  color: white;
  font-size: 1.25rem;
  font-weight: 950;
  box-shadow: 0 12px 26px rgba(95, 125, 232, 0.24);
}

.review-flow {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 0.65rem;
  margin-bottom: 1rem;
}

.review-flow-step {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  min-height: 48px;
  padding: 0.65rem 0.75rem;
  border: 1px solid var(--border);
  border-radius: 15px;
  background: rgba(255, 255, 255, 0.78);
  color: var(--muted);
}

.review-flow-step span {
  display: grid;
  place-items: center;
  flex: 0 0 auto;
  width: 26px;
  height: 26px;
  border-radius: 999px;
  background: #f2f5fb;
  color: var(--muted);
  font-size: 0.78rem;
  font-weight: 950;
}

.review-flow-step strong {
  min-width: 0;
  color: inherit;
  font-size: 0.84rem;
  font-weight: 900;
  white-space: nowrap;
}

.review-flow-step.active {
  border-color: #d7e1ff;
  background: var(--primary-soft);
  color: var(--primary-dark);
}

.review-flow-step.active span {
  background: var(--primary);
  color: white;
}

.review-form {
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.review-field-section {
  margin-bottom: 1rem;
  padding: 1.1rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--shadow-soft);
}

.review-field-section:last-of-type {
  margin-bottom: 0;
}

.review-field-section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.95rem;
}

.review-field-section-header h3 {
  margin: 0;
  color: var(--ink);
  font-size: 1rem;
  letter-spacing: -0.02em;
}

.review-field-section-header p {
  margin: 0.25rem 0 0;
  color: var(--muted);
  font-size: 0.9rem;
  line-height: 1.45;
}

.review-select-hint {
  display: inline-flex;
  align-items: center;
  max-width: 260px;
  min-height: 30px;
  padding: 0 0.75rem;
  border: 1px solid #d7e1ff;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary-dark);
  font-size: 0.8rem;
  font-weight: 900;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.review-field-section .field {
  margin-bottom: 0;
}

.review-field-section .grid-2 {
  gap: 1rem;
}

.full-field {
  width: 100%;
}

.review-textarea {
  min-height: 118px;
  resize: vertical;
}

.review-selected-summary {
  display: grid;
  gap: 0.12rem;
  margin-top: 0.95rem;
  padding: 0.85rem 0.95rem;
  border: 1px solid #d7e1ff;
  border-radius: 16px;
  background:
    linear-gradient(135deg, rgba(238, 243, 255, 0.95), rgba(255, 255, 255, 0.95));
}

.review-selected-summary span {
  color: var(--muted);
  font-size: 0.74rem;
  font-weight: 950;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.review-selected-summary strong {
  color: var(--ink);
  font-size: 1rem;
  font-weight: 950;
}

.review-selected-summary small {
  color: var(--muted);
  font-size: 0.86rem;
  font-weight: 750;
}

.review-empty-warning {
  margin-top: 0.95rem;
  margin-bottom: 0;
}

.star-rating-card {
  display: flex;
  flex-direction: column;
  gap: 0.55rem;
  max-width: 470px;
  min-height: 90px;
  padding: 0.9rem 1rem;
  border: 1px solid var(--border);
  border-radius: 16px;
  background: linear-gradient(135deg, #fffdf7, #ffffff);
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.star-rating-card:hover {
  border-color: #f2d27c;
  box-shadow: 0 10px 24px rgba(226, 180, 55, 0.12);
}

.star-rating-card.invalid {
  border-color: #e36d6d;
}

.star-rating {
  display: flex;
  align-items: center;
  gap: 0.35rem;
}

.star-button {
  border: 0;
  background: transparent;
  padding: 0;
  margin: 0;
  cursor: pointer;
  font-size: 2.05rem;
  line-height: 1;
  color: #d6dbe8;
  transition: transform 0.16s ease, color 0.16s ease, text-shadow 0.16s ease;
}

.star-button:hover,
.star-button:focus-visible {
  transform: translateY(-1px) scale(1.06);
  outline: none;
}

.star-button.active {
  color: #f4c542;
  text-shadow: 0 2px 10px rgba(244, 197, 66, 0.28);
}

.star-rating-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.8rem;
  flex-wrap: wrap;
}

.star-rating-value {
  color: var(--ink);
  font-size: 0.95rem;
  font-weight: 900;
}

.star-rating-caption {
  color: #9a7a18;
  font-size: 0.85rem;
  font-weight: 800;
}

.review-form-actions {
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

@media (max-width: 900px) {
  .review-form-shell {
    width: 100%;
  }

  .review-flow {
    grid-template-columns: repeat(2, 1fr);
  }

  .review-page-header {
    flex-direction: column;
  }

  .review-header-actions {
    padding-top: 0;
  }
}

@media (max-width: 640px) {
  .review-form-card {
    padding: 1rem;
    border-radius: 20px;
  }

  .review-form-header {
    grid-template-columns: 1fr;
  }

  .review-icon {
    width: 46px;
    height: 46px;
    border-radius: 16px;
  }

  .review-flow {
    grid-template-columns: 1fr;
  }

  .review-field-section {
    padding: 1rem;
    border-radius: 18px;
  }

  .review-field-section-header {
    flex-direction: column;
  }

  .review-select-hint {
    max-width: 100%;
  }

  .star-rating {
    justify-content: center;
    flex-wrap: wrap;
  }

  .star-rating-meta {
    justify-content: center;
    text-align: center;
  }

  .review-form-actions {
    flex-direction: column-reverse;
  }

  .review-form-actions .btn,
  .review-form-actions button {
    width: 100%;
  }
}
</style>
