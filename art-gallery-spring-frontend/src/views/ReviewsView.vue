<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { reviews } from '../api/resources';
import { normalizeError } from '../api/http';
import { useAuthStore } from '../stores/auth';

const router = useRouter();
const authStore = useAuthStore();

const rows = ref([]);
const loading = ref(false);
const error = ref(null);

const sortMode = ref('exhibition');
const expandedExhibitions = ref(new Set());
const expandedArtworks = ref(new Set());

const isAdmin = computed(() => authStore.isAdmin);

const totalReviews = computed(() => rows.value.length);

const globalAverageRating = computed(() => {
  if (!rows.value.length) {
    return '—';
  }

  const total = rows.value.reduce((sum, review) => sum + Number(review.rating || 0), 0);
  return (total / rows.value.length).toFixed(1);
});

const exhibitionGroups = computed(() => {
  const exhibitionMap = new Map();

  rows.value.forEach((review) => {
    const exhibitionId = review.exhibitionId || review.exhibition?.id || null;
    const exhibitionTitle = getExhibitionTitle(review);
    const exhibitionKey = exhibitionId
      ? `exhibition-${exhibitionId}`
      : `exhibition-${exhibitionTitle}`;

    const artworkId = review.artworkId || review.artwork?.id || null;
    const artworkTitle = getArtworkTitle(review);
    const artworkKey = artworkId
      ? `${exhibitionKey}-artwork-${artworkId}`
      : `${exhibitionKey}-artwork-${artworkTitle}`;

    if (!exhibitionMap.has(exhibitionKey)) {
      exhibitionMap.set(exhibitionKey, {
        key: exhibitionKey,
        exhibitionId,
        exhibitionTitle,
        reviews: [],
        artworkMap: new Map(),
      });
    }

    const exhibitionGroup = exhibitionMap.get(exhibitionKey);
    exhibitionGroup.reviews.push(review);

    if (!exhibitionGroup.artworkMap.has(artworkKey)) {
      exhibitionGroup.artworkMap.set(artworkKey, {
        key: artworkKey,
        artworkId,
        artworkTitle,
        reviews: [],
      });
    }

    exhibitionGroup.artworkMap.get(artworkKey).reviews.push(review);
  });

  const groups = Array.from(exhibitionMap.values()).map((exhibitionGroup) => {
    const artworkGroups = Array.from(exhibitionGroup.artworkMap.values()).map((artworkGroup) => {
      const artworkRatingTotal = artworkGroup.reviews.reduce(
        (sum, review) => sum + Number(review.rating || 0),
        0,
      );

      const artworkAverageRating = artworkGroup.reviews.length
        ? artworkRatingTotal / artworkGroup.reviews.length
        : 0;

      const artworkLatestDate = getLatestReviewDate(artworkGroup.reviews);

      return {
        ...artworkGroup,
        averageRating: artworkAverageRating,
        latestDate: artworkLatestDate,
      };
    });

    const exhibitionRatingTotal = exhibitionGroup.reviews.reduce(
      (sum, review) => sum + Number(review.rating || 0),
      0,
    );

    const exhibitionAverageRating = exhibitionGroup.reviews.length
      ? exhibitionRatingTotal / exhibitionGroup.reviews.length
      : 0;

    const exhibitionLatestDate = getLatestReviewDate(exhibitionGroup.reviews);

    return {
      key: exhibitionGroup.key,
      exhibitionId: exhibitionGroup.exhibitionId,
      exhibitionTitle: exhibitionGroup.exhibitionTitle,
      reviews: exhibitionGroup.reviews,
      averageRating: exhibitionAverageRating,
      latestDate: exhibitionLatestDate,
      artworkGroups: sortArtworkGroups(artworkGroups),
    };
  });

  return sortExhibitionGroups(groups);
});

const totalExhibitions = computed(() => exhibitionGroups.value.length);

const totalArtworkGroups = computed(() =>
  exhibitionGroups.value.reduce(
    (sum, exhibitionGroup) => sum + exhibitionGroup.artworkGroups.length,
    0,
  ),
);

const bestExhibitionGroup = computed(() => {
  if (!exhibitionGroups.value.length) {
    return null;
  }

  return [...exhibitionGroups.value].sort(
    (a, b) => b.averageRating - a.averageRating,
  )[0];
});

function asPage(payload) {
  const pagePayload = payload?.data || payload || {};

  return {
    content: Array.isArray(pagePayload.content) ? pagePayload.content : [],
    page: Number(pagePayload.page ?? 0),
    size: Number(pagePayload.size ?? 0),
    totalElements: Number(pagePayload.totalElements ?? 0),
    totalPages: Number(pagePayload.totalPages ?? 1),
  };
}

function getVisitorName(review) {
  return review.visitorName || review.visitor?.name || 'Unknown visitor';
}

function getArtworkTitle(review) {
  return review.artworkTitle || review.artwork?.title || 'General exhibition review';
}

function getExhibitionTitle(review) {
  return review.exhibitionTitle || review.exhibition?.title || 'Unknown exhibition';
}

function getReviewDate(review) {
  return review.reviewDate || '—';
}

function getLatestReviewDate(reviewList) {
  return reviewList
    .map((review) => review.reviewDate)
    .filter(Boolean)
    .sort()
    .at(-1) || null;
}

function averageLabel(value) {
  return Number(value || 0) ? Number(value).toFixed(1) : '—';
}

function ratingLabel(rating) {
  const value = Math.round(Number(rating || 0));

  if (value <= 1) return 'Very poor';
  if (value === 2) return 'Poor';
  if (value === 3) return 'Good';
  if (value === 4) return 'Very good';
  return 'Excellent';
}

function isStarActive(rating, star) {
  return star <= Math.round(Number(rating || 0));
}

function dateValue(value) {
  if (!value) {
    return 0;
  }

  const timestamp = new Date(value).getTime();
  return Number.isNaN(timestamp) ? 0 : timestamp;
}

function sortArtworkGroups(groups) {
  return [...groups].sort((a, b) => {
    if (sortMode.value === 'rating') {
      return b.averageRating - a.averageRating;
    }

    if (sortMode.value === 'count') {
      return b.reviews.length - a.reviews.length;
    }

    if (sortMode.value === 'latest') {
      return dateValue(b.latestDate) - dateValue(a.latestDate);
    }

    return a.artworkTitle.localeCompare(b.artworkTitle);
  });
}

function sortExhibitionGroups(groups) {
  return [...groups].sort((a, b) => {
    if (sortMode.value === 'rating') {
      return b.averageRating - a.averageRating;
    }

    if (sortMode.value === 'count') {
      return b.reviews.length - a.reviews.length;
    }

    if (sortMode.value === 'latest') {
      return dateValue(b.latestDate) - dateValue(a.latestDate);
    }

    return a.exhibitionTitle.localeCompare(b.exhibitionTitle);
  });
}

function isExhibitionExpanded(groupKey) {
  return expandedExhibitions.value.has(groupKey);
}

function isArtworkExpanded(groupKey) {
  return expandedArtworks.value.has(groupKey);
}

function toggleExhibition(groupKey) {
  const next = new Set(expandedExhibitions.value);

  if (next.has(groupKey)) {
    next.delete(groupKey);
  } else {
    next.add(groupKey);
  }

  expandedExhibitions.value = next;
}

function toggleArtwork(groupKey) {
  const next = new Set(expandedArtworks.value);

  if (next.has(groupKey)) {
    next.delete(groupKey);
  } else {
    next.add(groupKey);
  }

  expandedArtworks.value = next;
}

function expandAllExhibitions() {
  expandedExhibitions.value = new Set(exhibitionGroups.value.map((group) => group.key));
}

function collapseAllExhibitions() {
  expandedExhibitions.value = new Set();
  expandedArtworks.value = new Set();
}

async function loadReviews() {
  loading.value = true;
  error.value = null;

  try {
    const pageSize = 200;
    const firstPagePayload = await reviews.list({
      page: 0,
      size: pageSize,
      sort: 'reviewDate,desc',
    });

    const firstPage = asPage(firstPagePayload);
    let loadedReviews = [...firstPage.content];

    if (firstPage.totalPages > 1) {
      const remainingPages = await Promise.all(
        Array.from({ length: firstPage.totalPages - 1 }, (_, index) =>
          reviews.list({
            page: index + 1,
            size: pageSize,
            sort: 'reviewDate,desc',
          }),
        ),
      );

      remainingPages.forEach((payload) => {
        loadedReviews = [...loadedReviews, ...asPage(payload).content];
      });
    }

    rows.value = loadedReviews;

    // Default behavior: everything is collapsed.
    // The user opens an exhibition, then opens an artwork only when details are needed.
    expandedExhibitions.value = new Set();
    expandedArtworks.value = new Set();
  } catch (err) {
    error.value = normalizeError(err).message;
  } finally {
    loading.value = false;
  }
}

function goToCreate() {
  router.push({ name: 'review-new' });
}

function goToEdit(review) {
  router.push({ name: 'review-edit', params: { id: review.id } });
}

async function removeReview(review) {
  const confirmed = window.confirm(
    `Delete review for "${getArtworkTitle(review)}"?`,
  );

  if (!confirmed) {
    return;
  }

  try {
    await reviews.remove(review.id);
    await loadReviews();
  } catch (err) {
    error.value = normalizeError(err).message;
  }
}

onMounted(loadReviews);
</script>

<template>
  <div class="reviews-page">
    <div class="reviews-page-header">
      <div>
        <p class="eyebrow">Review management</p>
        <h1>Reviews</h1>
        <p class="muted">
          Reviews are grouped by exhibition. Each exhibition contains its reviewed artworks and calculated averages.
        </p>
      </div>

      <div class="reviews-header-actions">
        <button type="button" class="btn btn-primary" @click="goToCreate">
          + New Review
        </button>
      </div>
    </div>

    <div class="review-stats-grid">
      <div class="review-stat-card">
        <span>Total reviews</span>
        <strong>{{ totalReviews }}</strong>
        <small>Loaded feedback entries</small>
      </div>

      <div class="review-stat-card">
        <span>Exhibitions reviewed</span>
        <strong>{{ totalExhibitions }}</strong>
        <small>Main review groups</small>
      </div>

      <div class="review-stat-card">
        <span>Artwork groups</span>
        <strong>{{ totalArtworkGroups }}</strong>
        <small>Reviewed artworks</small>
      </div>

      <div class="review-stat-card">
        <span>Average rating</span>
        <strong>{{ globalAverageRating }}</strong>
        <small>Across loaded reviews</small>
      </div>

      <div class="review-stat-card wide">
        <span>Best reviewed exhibition</span>
        <strong>{{ bestExhibitionGroup ? bestExhibitionGroup.exhibitionTitle : '—' }}</strong>
        <small>
          {{ bestExhibitionGroup ? `${averageLabel(bestExhibitionGroup.averageRating)}/5 · ${bestExhibitionGroup.reviews.length} reviews` : 'No reviews yet' }}
        </small>
      </div>
    </div>

    <section class="reviews-card">
      <div class="reviews-toolbar">
        <div>
          <h2>Exhibition review list</h2>
          <p>Open an exhibition to see its artworks. Open an artwork to see individual reviews.</p>
        </div>

        <div class="reviews-toolbar-actions">
          <div class="reviews-sort">
            <label for="reviewSort">Sort groups</label>

            <select id="reviewSort" v-model="sortMode">
              <option value="exhibition">Exhibition / artwork</option>
              <option value="rating">Highest average rating</option>
              <option value="count">Most reviews</option>
              <option value="latest">Latest review</option>
            </select>
          </div>

          <div class="expand-actions">
            <button type="button" class="btn btn-secondary" @click="expandAllExhibitions">
              Show exhibitions
            </button>

            <button type="button" class="btn btn-secondary" @click="collapseAllExhibitions">
              Collapse all
            </button>
          </div>
        </div>
      </div>

      <div v-if="error" class="alert alert-error">
        {{ error }}
      </div>

      <div v-if="loading" class="empty compact">
        Loading reviews...
      </div>

      <div v-else-if="!exhibitionGroups.length" class="reviews-empty-state">
        <div class="reviews-empty-icon">★</div>
        <h2>No reviews found</h2>
        <p>Create a review after assigning artworks to exhibitions.</p>

        <button type="button" class="btn btn-primary" @click="goToCreate">
          + New Review
        </button>
      </div>

      <div v-else class="exhibition-review-list">
        <article
          v-for="exhibitionGroup in exhibitionGroups"
          :key="exhibitionGroup.key"
          class="exhibition-review-card"
        >
          <button
            type="button"
            class="exhibition-review-header"
            @click="toggleExhibition(exhibitionGroup.key)"
          >
            <div class="review-rating-panel">
              <div class="review-stars" :aria-label="`${averageLabel(exhibitionGroup.averageRating)} out of 5`">
                <span
                  v-for="star in 5"
                  :key="star"
                  :class="{ active: isStarActive(exhibitionGroup.averageRating, star) }"
                >
                  ★
                </span>
              </div>

              <strong>{{ averageLabel(exhibitionGroup.averageRating) }}/5</strong>
              <small>{{ ratingLabel(exhibitionGroup.averageRating) }}</small>
            </div>

            <div class="exhibition-review-main">
              <p class="eyebrow">Exhibition</p>
              <h2>{{ exhibitionGroup.exhibitionTitle }}</h2>
              <p>
                {{ exhibitionGroup.artworkGroups.length }}
                {{ exhibitionGroup.artworkGroups.length === 1 ? 'artwork group' : 'artwork groups' }}
              </p>
            </div>

            <div class="exhibition-review-summary">
              <span>
                {{ exhibitionGroup.reviews.length }}
                {{ exhibitionGroup.reviews.length === 1 ? 'review' : 'reviews' }}
              </span>

              <small>Latest: {{ exhibitionGroup.latestDate || '—' }}</small>

              <em>
                {{ isExhibitionExpanded(exhibitionGroup.key) ? 'Hide artworks' : 'Show artworks' }}
              </em>
            </div>
          </button>

          <div
            v-if="isExhibitionExpanded(exhibitionGroup.key)"
            class="artwork-review-list"
          >
            <article
              v-for="artworkGroup in exhibitionGroup.artworkGroups"
              :key="artworkGroup.key"
              class="artwork-review-card"
            >
              <button
                type="button"
                class="artwork-review-header"
                @click="toggleArtwork(artworkGroup.key)"
              >
                <div class="artwork-review-title">
                  <p class="eyebrow">Artwork</p>
                  <h3>{{ artworkGroup.artworkTitle }}</h3>
                </div>

                <div class="artwork-review-average">
                  <div class="review-stars small-stars" :aria-label="`${averageLabel(artworkGroup.averageRating)} out of 5`">
                    <span
                      v-for="star in 5"
                      :key="star"
                      :class="{ active: isStarActive(artworkGroup.averageRating, star) }"
                    >
                      ★
                    </span>
                  </div>

                  <strong>{{ averageLabel(artworkGroup.averageRating) }}/5</strong>
                </div>

                <div class="artwork-review-summary">
                  <span>
                    {{ artworkGroup.reviews.length }}
                    {{ artworkGroup.reviews.length === 1 ? 'review' : 'reviews' }}
                  </span>

                  <small>Latest: {{ artworkGroup.latestDate || '—' }}</small>

                  <em>
                    {{ isArtworkExpanded(artworkGroup.key) ? 'Hide reviews' : 'Show reviews' }}
                  </em>
                </div>
              </button>

              <div
                v-if="isArtworkExpanded(artworkGroup.key)"
                class="review-items"
              >
                <div
                  v-for="review in artworkGroup.reviews"
                  :key="review.id"
                  class="review-item"
                >
                  <div class="review-item-rating">
                    <div class="review-stars small-stars" :aria-label="`${review.rating} out of 5`">
                      <span
                        v-for="star in 5"
                        :key="star"
                        :class="{ active: isStarActive(review.rating, star) }"
                      >
                        ★
                      </span>
                    </div>

                    <strong>{{ review.rating }}/5</strong>
                  </div>

                  <div class="review-item-main">
                    <div class="review-item-title">
                      <strong>{{ getVisitorName(review) }}</strong>
                      <span>{{ getReviewDate(review) }}</span>
                    </div>

                    <p v-if="review.reviewText">
                      {{ review.reviewText }}
                    </p>

                    <p v-else class="muted">
                      No review text.
                    </p>
                  </div>

                  <div v-if="isAdmin" class="review-item-actions">
                    <button
                      type="button"
                      class="btn btn-secondary"
                      @click="goToEdit(review)"
                    >
                      Edit
                    </button>

                    <button
                      type="button"
                      class="btn btn-danger-soft"
                      @click="removeReview(review)"
                    >
                      Delete
                    </button>
                  </div>
                </div>
              </div>
            </article>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.reviews-page {
  width: 100%;
}

.reviews-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.25rem;
  margin-bottom: 1.35rem;
}

.reviews-page-header h1 {
  margin: 0;
  color: var(--ink);
  font-size: clamp(1.85rem, 3vw, 2.55rem);
  line-height: 1.05;
  letter-spacing: -0.055em;
}

.reviews-page-header p {
  max-width: 760px;
  margin-top: 0.7rem;
  line-height: 1.6;
}

.reviews-header-actions {
  flex: 0 0 auto;
  padding-top: 0.25rem;
}

.review-stats-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.85rem;
  margin-bottom: 1rem;
}

.review-stat-card {
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.96));
  box-shadow: var(--shadow-soft);
}

.review-stat-card span {
  display: block;
  color: var(--muted);
  font-size: 0.76rem;
  font-weight: 950;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.review-stat-card strong {
  display: block;
  max-width: 100%;
  margin-top: 0.35rem;
  color: var(--ink);
  font-size: 1.28rem;
  font-weight: 950;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-stat-card small {
  display: block;
  margin-top: 0.15rem;
  color: var(--muted);
  font-size: 0.82rem;
  font-weight: 750;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reviews-card {
  padding: 1.1rem;
  border: 1px solid var(--border);
  border-radius: 26px;
  background:
    radial-gradient(circle at top right, rgba(244, 197, 66, 0.08), transparent 18rem),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.96));
  box-shadow: var(--shadow);
}

.reviews-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
}

.reviews-toolbar h2 {
  margin: 0;
  color: var(--ink);
  font-size: 1rem;
  font-weight: 950;
}

.reviews-toolbar p {
  margin: 0.25rem 0 0;
  color: var(--muted);
  font-size: 0.9rem;
}

.reviews-toolbar-actions {
  display: flex;
  align-items: flex-end;
  gap: 0.75rem;
}

.reviews-sort {
  min-width: 230px;
}

.reviews-sort label {
  display: block;
  margin-bottom: 0.45rem;
  color: var(--ink);
  font-size: 0.83rem;
  font-weight: 950;
}

.expand-actions {
  display: flex;
  gap: 0.55rem;
}

.exhibition-review-list {
  display: grid;
  gap: 0.95rem;
}

.exhibition-review-card {
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--shadow-soft);
}

.exhibition-review-header {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr) 160px;
  gap: 1rem;
  align-items: center;
  width: 100%;
  padding: 1rem;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.exhibition-review-header:hover {
  background: rgba(248, 250, 255, 0.7);
}

.review-rating-panel {
  display: grid;
  justify-items: center;
  gap: 0.22rem;
  min-height: 112px;
  padding: 0.9rem;
  border: 1px solid #f2dfaa;
  border-radius: 18px;
  background:
    radial-gradient(circle at top, rgba(244, 197, 66, 0.14), transparent 70%),
    linear-gradient(135deg, #fffdf7, #ffffff);
}

.review-stars {
  display: flex;
  gap: 0.12rem;
  color: #d6dbe8;
  font-size: 1.18rem;
  line-height: 1;
}

.review-stars span.active {
  color: #f4c542;
  text-shadow: 0 2px 8px rgba(244, 197, 66, 0.24);
}

.review-rating-panel strong {
  color: var(--ink);
  font-size: 1.15rem;
  font-weight: 950;
}

.review-rating-panel small {
  color: #9a7a18;
  font-size: 0.8rem;
  font-weight: 900;
}

.exhibition-review-main {
  min-width: 0;
}

.exhibition-review-main h2 {
  margin: 0.2rem 0 0;
  color: var(--ink);
  font-size: 1.15rem;
  font-weight: 950;
  letter-spacing: -0.025em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.exhibition-review-main p:not(.eyebrow) {
  margin: 0.35rem 0 0;
  color: var(--muted);
  font-size: 0.88rem;
  font-weight: 800;
}

.exhibition-review-summary {
  display: grid;
  justify-items: end;
  gap: 0.22rem;
}

.exhibition-review-summary span,
.artwork-review-summary span {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 0.75rem;
  border: 1px solid #d7e1ff;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary-dark);
  font-size: 0.82rem;
  font-weight: 900;
}

.exhibition-review-summary small,
.artwork-review-summary small {
  color: var(--muted);
  font-size: 0.78rem;
  font-weight: 800;
}

.exhibition-review-summary em,
.artwork-review-summary em {
  color: var(--primary-dark);
  font-size: 0.8rem;
  font-style: normal;
  font-weight: 900;
}

.artwork-review-list {
  display: grid;
  gap: 0.75rem;
  padding: 0 1rem 1rem;
}

.artwork-review-card {
  overflow: hidden;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: rgba(248, 250, 255, 0.72);
}

.artwork-review-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 150px 150px;
  gap: 0.85rem;
  align-items: center;
  width: 100%;
  padding: 0.85rem;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.artwork-review-header:hover {
  background: rgba(255, 255, 255, 0.68);
}

.artwork-review-title {
  min-width: 0;
}

.artwork-review-title h3 {
  margin: 0.2rem 0 0;
  color: var(--ink);
  font-size: 1rem;
  font-weight: 950;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.artwork-review-average {
  display: grid;
  justify-items: center;
  gap: 0.16rem;
}

.small-stars {
  font-size: 0.92rem;
}

.artwork-review-average strong {
  color: var(--ink);
  font-size: 0.9rem;
  font-weight: 950;
}

.artwork-review-summary {
  display: grid;
  justify-items: end;
  gap: 0.18rem;
}

.review-items {
  display: grid;
  gap: 0.65rem;
  padding: 0 0.85rem 0.85rem;
}

.review-item {
  display: grid;
  grid-template-columns: 110px minmax(0, 1fr) auto;
  gap: 0.85rem;
  align-items: center;
  padding: 0.85rem;
  border: 1px solid var(--border);
  border-radius: 17px;
  background: rgba(255, 255, 255, 0.86);
}

.review-item-rating {
  display: grid;
  justify-items: center;
  gap: 0.18rem;
}

.review-item-rating strong {
  color: var(--ink);
  font-size: 0.86rem;
  font-weight: 900;
}

.review-item-main {
  min-width: 0;
}

.review-item-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.7rem;
}

.review-item-title strong {
  color: var(--ink);
  font-size: 0.92rem;
  font-weight: 950;
}

.review-item-title span {
  color: var(--muted);
  font-size: 0.8rem;
  font-weight: 800;
  white-space: nowrap;
}

.review-item-main p {
  margin: 0.35rem 0 0;
  color: var(--ink-soft);
  font-size: 0.88rem;
  line-height: 1.45;
}

.review-item-actions {
  display: flex;
  gap: 0.55rem;
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

.reviews-empty-state {
  display: grid;
  justify-items: center;
  gap: 0.65rem;
  padding: 3rem 1rem;
  text-align: center;
}

.reviews-empty-icon {
  display: grid;
  place-items: center;
  width: 62px;
  height: 62px;
  border-radius: 22px;
  background: #fff6d8;
  color: #c99812;
  font-size: 1.55rem;
}

.reviews-empty-state h2 {
  margin: 0;
  color: var(--ink);
  font-size: 1.25rem;
}

.reviews-empty-state p {
  margin: 0;
  color: var(--muted);
}

@media (max-width: 1200px) {
  .review-stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .review-stat-card.wide {
    grid-column: 1 / -1;
  }

  .exhibition-review-header {
    grid-template-columns: 140px minmax(0, 1fr);
  }

  .exhibition-review-summary {
    grid-column: 1 / -1;
    justify-items: start;
  }

  .artwork-review-header {
    grid-template-columns: minmax(0, 1fr) 140px;
  }

  .artwork-review-summary {
    grid-column: 1 / -1;
    justify-items: start;
  }

  .review-item {
    grid-template-columns: 100px minmax(0, 1fr);
  }

  .review-item-actions {
    grid-column: 1 / -1;
    justify-content: flex-end;
  }
}

@media (max-width: 900px) {
  .reviews-page-header,
  .reviews-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .reviews-toolbar-actions,
  .expand-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .reviews-sort {
    min-width: 0;
  }
}

@media (max-width: 640px) {
  .review-stats-grid {
    grid-template-columns: 1fr;
  }

  .reviews-card {
    padding: 0.9rem;
    border-radius: 20px;
  }

  .exhibition-review-header,
  .artwork-review-header,
  .review-item {
    grid-template-columns: 1fr;
  }

  .review-rating-panel,
  .artwork-review-average,
  .review-item-rating {
    justify-items: start;
  }

  .review-item-title {
    flex-direction: column;
    align-items: flex-start;
  }

  .review-item-actions {
    flex-direction: column;
  }

  .review-item-actions .btn,
  .review-item-actions button {
    width: 100%;
  }
}
</style>
