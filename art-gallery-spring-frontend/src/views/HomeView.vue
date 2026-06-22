<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import {
  artists,
  artworks,
  exhibitions,
  exhibitionArtworks,
  visitors,
  reviews,
} from '../api/resources';
import { normalizeError } from '../api/http';

const router = useRouter();

const loading = ref(true);
const error = ref(null);

const dashboard = ref({
  artists: {
    total: 0,
    items: [],
  },
  artworks: {
    total: 0,
    items: [],
  },
  exhibitions: {
    total: 0,
    items: [],
  },
  visitors: {
    total: 0,
    items: [],
  },
  reviews: {
    total: 0,
    items: [],
  },
});

const enrichedExhibitions = ref([]);

const liveExhibitions = computed(() =>
  enrichedExhibitions.value.filter((exhibition) => getExhibitionStatus(exhibition).key === 'live'),
);

const scheduledExhibitions = computed(() =>
  enrichedExhibitions.value.filter((exhibition) => getExhibitionStatus(exhibition).key === 'scheduled'),
);

const closedExhibitions = computed(() =>
  enrichedExhibitions.value.filter((exhibition) => getExhibitionStatus(exhibition).key === 'closed'),
);

const averageRating = computed(() => {
  const list = dashboard.value.reviews.items;

  if (!list.length) {
    return '—';
  }

  const total = list.reduce((sum, review) => sum + Number(review.rating || 0), 0);
  return (total / list.length).toFixed(1);
});

const estimatedValue = computed(() => {
  const total = dashboard.value.artworks.items.reduce(
    (sum, artwork) => sum + Number(artwork.estimatedValue || 0),
    0,
  );

  return formatMoney(total);
});

const statCards = computed(() => [
  {
    label: 'Artists',
    value: dashboard.value.artists.total,
    description: 'artist profiles',
    icon: '🎨',
    route: 'artists',
  },
  {
    label: 'Artworks',
    value: dashboard.value.artworks.total,
    description: 'catalogue records',
    icon: '🖼️',
    route: 'artworks',
  },
  {
    label: 'Exhibitions',
    value: dashboard.value.exhibitions.total,
    description: 'gallery events',
    icon: '🏛️',
    route: 'exhibitions',
  },
  {
    label: 'Visitors',
    value: dashboard.value.visitors.total,
    description: 'visitor profiles',
    icon: '👥',
    route: 'visitors',
  },
  {
    label: 'Reviews',
    value: dashboard.value.reviews.total,
    description: 'feedback entries',
    icon: '★',
    route: 'reviews',
  },
]);

function asPage(payload) {
  const pagePayload = payload?.data || payload || {};

  return {
    content: Array.isArray(pagePayload.content) ? pagePayload.content : [],
    totalElements: Number(pagePayload.totalElements ?? 0),
  };
}

function asArray(payload) {
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload?.content)) return payload.content;
  if (Array.isArray(payload?.data?.content)) return payload.data.content;
  return [];
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

function getExhibitionStatus(exhibition) {
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

function exhibitionStatusRank(exhibition) {
  const status = getExhibitionStatus(exhibition).key;

  if (status === 'live') return 0;
  if (status === 'scheduled') return 1;
  return 2;
}

function exhibitionDateValue(exhibition) {
  const date = exhibition.startDate || exhibition.endDate;

  if (!date) return Number.MAX_SAFE_INTEGER;

  const timestamp = new Date(date).getTime();

  return Number.isNaN(timestamp) ? Number.MAX_SAFE_INTEGER : timestamp;
}

function getExhibitorName(exhibition) {
  return exhibition.exhibitorName || exhibition.exhibitor?.name || 'Unknown exhibitor';
}

function getArtworkTitle(artwork) {
  return artwork.title || 'Untitled artwork';
}

function getArtistName(artistOrArtwork) {
  return artistOrArtwork.artistName || artistOrArtwork.name || artistOrArtwork.artist?.name || 'Unknown artist';
}

function getVisitorName(visitorOrReview) {
  return visitorOrReview.visitorName || visitorOrReview.name || visitorOrReview.visitor?.name || 'Unknown visitor';
}

function getReviewArtworkTitle(review) {
  return review.artworkTitle || review.artwork?.title || 'Unknown artwork';
}

function getReviewExhibitionTitle(review) {
  return review.exhibitionTitle || review.exhibition?.title || 'Unknown exhibition';
}

function ratingStars(rating) {
  const value = Number(rating || 0);
  return '★'.repeat(value) + '☆'.repeat(Math.max(0, 5 - value));
}

function goTo(routeName) {
  router.push({ name: routeName });
}

function goToExhibition(exhibition) {
  router.push({ name: 'exhibition-details', params: { id: exhibition.id } });
}

async function loadDashboard() {
  loading.value = true;
  error.value = null;

  try {
    const [
      loadedArtists,
      loadedArtworks,
      loadedExhibitions,
      loadedVisitors,
      loadedReviews,
    ] = await Promise.all([
      artists.list({ page: 0, size: 5, sort: 'name,asc' }),
      artworks.list({ page: 0, size: 5, sort: 'title,asc' }),
      exhibitions.list({ page: 0, size: 6, sort: 'startDate,asc' }),
      visitors.list({ page: 0, size: 5, sort: 'name,asc' }),
      reviews.list({ page: 0, size: 5, sort: 'reviewDate,desc' }),
    ]);

    const artistsPage = asPage(loadedArtists);
    const artworksPage = asPage(loadedArtworks);
    const exhibitionsPage = asPage(loadedExhibitions);
    const visitorsPage = asPage(loadedVisitors);
    const reviewsPage = asPage(loadedReviews);

    dashboard.value = {
      artists: {
        total: artistsPage.totalElements,
        items: artistsPage.content,
      },
      artworks: {
        total: artworksPage.totalElements,
        items: artworksPage.content,
      },
      exhibitions: {
        total: exhibitionsPage.totalElements,
        items: exhibitionsPage.content,
      },
      visitors: {
        total: visitorsPage.totalElements,
        items: visitorsPage.content,
      },
      reviews: {
        total: reviewsPage.totalElements,
        items: reviewsPage.content,
      },
    };

    const loadedEnrichedExhibitions = await Promise.all(
      exhibitionsPage.content.map(async (exhibition) => {
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

    enrichedExhibitions.value = loadedEnrichedExhibitions.sort((a, b) => {
      const rankDifference = exhibitionStatusRank(a) - exhibitionStatusRank(b);

      if (rankDifference !== 0) {
        return rankDifference;
      }

      return exhibitionDateValue(a) - exhibitionDateValue(b);
    });
  } catch (err) {
    error.value = normalizeError(err).message;
  } finally {
    loading.value = false;
  }
}

onMounted(loadDashboard);
</script>

<template>
  <div class="dashboard-page">
    <section class="dashboard-hero-card">
      <div>
        <p class="eyebrow">Art Gallery dashboard</p>
        <h1>Gallery overview</h1>
        <p>
          Centralized overview for catalogue records, exhibitions, visitors and reviews.
        </p>
      </div>

      <div class="dashboard-hero-actions">
        <button type="button" class="btn btn-primary" @click="goTo('exhibitions')">
          View exhibitions
        </button>

        <button type="button" class="btn btn-secondary" @click="goTo('artworks')">
          View artworks
        </button>
      </div>
    </section>

    <div v-if="error" class="alert alert-error">
      {{ error }}
    </div>

    <div v-if="loading" class="empty compact">
      Loading dashboard...
    </div>

    <template v-else>
      <section class="dashboard-stat-grid">
        <button
          v-for="card in statCards"
          :key="card.label"
          type="button"
          class="dashboard-stat-card"
          @click="goTo(card.route)"
        >
          <span class="dashboard-stat-icon">{{ card.icon }}</span>

          <span>
            <small>{{ card.label }}</small>
            <strong>{{ card.value }}</strong>
            <em>{{ card.description }}</em>
          </span>
        </button>
      </section>

      <section class="dashboard-summary-grid">
        <article class="dashboard-summary-card">
          <span>Live exhibitions</span>
          <strong>{{ liveExhibitions.length }}</strong>
          <small>Currently active on visible schedule</small>
        </article>

        <article class="dashboard-summary-card">
          <span>Scheduled exhibitions</span>
          <strong>{{ scheduledExhibitions.length }}</strong>
          <small>Upcoming on visible schedule</small>
        </article>

        <article class="dashboard-summary-card">
          <span>Closed exhibitions</span>
          <strong>{{ closedExhibitions.length }}</strong>
          <small>Past exhibitions on visible schedule</small>
        </article>

        <article class="dashboard-summary-card">
          <span>Average rating</span>
          <strong>{{ averageRating }}</strong>
          <small>Based on latest reviews</small>
        </article>

        <article class="dashboard-summary-card wide">
          <span>Estimated artwork value</span>
          <strong>{{ estimatedValue }}</strong>
          <small>Based on visible artwork sample</small>
        </article>
      </section>

      <section class="dashboard-grid">
        <article class="dashboard-panel large">
          <div class="dashboard-panel-header">
            <div>
              <p class="eyebrow">Schedule</p>
              <h2>Exhibitions</h2>
            </div>

            <button type="button" class="btn btn-secondary" @click="goTo('exhibitions')">
              View all
            </button>
          </div>

          <div v-if="!enrichedExhibitions.length" class="empty compact">
            No exhibitions found.
          </div>

          <div v-else class="dashboard-exhibition-list">
            <button
              v-for="exhibition in enrichedExhibitions"
              :key="exhibition.id"
              type="button"
              class="dashboard-exhibition-card"
              @click="goToExhibition(exhibition)"
            >
              <span
                class="dashboard-status-dot"
                :class="getExhibitionStatus(exhibition).key"
              ></span>

              <span class="dashboard-exhibition-main">
                <strong>{{ exhibition.title }}</strong>
                <small>{{ getExhibitorName(exhibition) }}</small>
              </span>

              <span class="dashboard-exhibition-meta">
                <em :class="getExhibitionStatus(exhibition).key">
                  {{ getExhibitionStatus(exhibition).label }}
                </em>
                <small>{{ exhibition.artworkCount }} artworks</small>
              </span>
            </button>
          </div>
        </article>

        <article class="dashboard-panel">
          <div class="dashboard-panel-header">
            <div>
              <p class="eyebrow">Catalogue</p>
              <h2>Artworks</h2>
            </div>

            <button type="button" class="btn btn-secondary" @click="goTo('artworks')">
              View all
            </button>
          </div>

          <div v-if="!dashboard.artworks.items.length" class="empty compact">
            No artworks found.
          </div>

          <div v-else class="dashboard-mini-list">
            <div
              v-for="artwork in dashboard.artworks.items"
              :key="artwork.id"
              class="dashboard-mini-row"
            >
              <span class="mini-icon">🖼️</span>

              <span>
                <strong>{{ getArtworkTitle(artwork) }}</strong>
                <small>{{ getArtistName(artwork) }}</small>
              </span>
            </div>
          </div>
        </article>

        <article class="dashboard-panel">
          <div class="dashboard-panel-header">
            <div>
              <p class="eyebrow">Artists</p>
              <h2>Artist profiles</h2>
            </div>

            <button type="button" class="btn btn-secondary" @click="goTo('artists')">
              View all
            </button>
          </div>

          <div v-if="!dashboard.artists.items.length" class="empty compact">
            No artists found.
          </div>

          <div v-else class="dashboard-mini-list">
            <div
              v-for="artist in dashboard.artists.items"
              :key="artist.id"
              class="dashboard-mini-row"
            >
              <span class="mini-icon">🎨</span>

              <span>
                <strong>{{ artist.name }}</strong>
                <small>{{ artist.nationality || 'Unknown nationality' }}</small>
              </span>
            </div>
          </div>
        </article>

        <article class="dashboard-panel">
          <div class="dashboard-panel-header">
            <div>
              <p class="eyebrow">Visitors</p>
              <h2>Recent visitors</h2>
            </div>

            <button type="button" class="btn btn-secondary" @click="goTo('visitors')">
              View all
            </button>
          </div>

          <div v-if="!dashboard.visitors.items.length" class="empty compact">
            No visitors found.
          </div>

          <div v-else class="dashboard-mini-list">
            <div
              v-for="visitor in dashboard.visitors.items"
              :key="visitor.id"
              class="dashboard-mini-row"
            >
              <span class="mini-icon">👥</span>

              <span>
                <strong>{{ visitor.name }}</strong>
                <small>{{ visitor.membershipType || 'Standard' }}</small>
              </span>
            </div>
          </div>
        </article>

        <article class="dashboard-panel">
          <div class="dashboard-panel-header">
            <div>
              <p class="eyebrow">Reviews</p>
              <h2>Latest feedback</h2>
            </div>

            <button type="button" class="btn btn-secondary" @click="goTo('reviews')">
              View all
            </button>
          </div>

          <div v-if="!dashboard.reviews.items.length" class="empty compact">
            No reviews found.
          </div>

          <div v-else class="dashboard-mini-list">
            <div
              v-for="review in dashboard.reviews.items"
              :key="review.id"
              class="dashboard-mini-row"
            >
              <span class="mini-icon gold">★</span>

              <span>
                <strong>{{ getReviewArtworkTitle(review) }}</strong>
                <small>{{ ratingStars(review.rating) }} · {{ getVisitorName(review) }}</small>
                <small>{{ getReviewExhibitionTitle(review) }}</small>
              </span>
            </div>
          </div>
        </article>
      </section>
    </template>
  </div>
</template>

<style scoped>
.dashboard-page {
  width: 100%;
}

.dashboard-hero-card {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.25rem;
  margin-bottom: 1rem;
  padding: 1.5rem;
  border: 1px solid var(--border);
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(95, 125, 232, 0.12), transparent 18rem),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.96));
  box-shadow: var(--shadow);
}

.dashboard-hero-card h1 {
  margin: 0;
  color: var(--ink);
  font-size: clamp(2rem, 4vw, 3rem);
  line-height: 1;
  letter-spacing: -0.06em;
}

.dashboard-hero-card p {
  max-width: 720px;
  margin: 0.75rem 0 0;
  color: var(--muted);
  font-size: 1rem;
  line-height: 1.6;
}

.dashboard-hero-actions {
  display: flex;
  gap: 0.75rem;
  flex: 0 0 auto;
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

.dashboard-stat-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.85rem;
  margin-bottom: 1rem;
}

.dashboard-stat-card {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 0.75rem;
  align-items: center;
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--shadow-soft);
  text-align: left;
}

.dashboard-stat-card:hover {
  border-color: #d7e1ff;
  background: #fbfcff;
}

.dashboard-stat-icon {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: 16px;
  background: var(--primary-soft);
  font-size: 1.25rem;
}

.dashboard-stat-card small {
  display: block;
  color: var(--muted);
  font-size: 0.76rem;
  font-weight: 950;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.dashboard-stat-card strong {
  display: block;
  margin-top: 0.15rem;
  color: var(--ink);
  font-size: 1.45rem;
  font-weight: 950;
}

.dashboard-stat-card em {
  display: block;
  color: var(--muted);
  font-size: 0.8rem;
  font-style: normal;
  font-weight: 750;
}

.dashboard-summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 0.85rem;
  margin-bottom: 1rem;
}

.dashboard-summary-card {
  padding: 1rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.96), rgba(248, 251, 255, 0.96));
  box-shadow: var(--shadow-soft);
}

.dashboard-summary-card span {
  display: block;
  color: var(--muted);
  font-size: 0.76rem;
  font-weight: 950;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.dashboard-summary-card strong {
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

.dashboard-summary-card small {
  display: block;
  margin-top: 0.15rem;
  color: var(--muted);
  font-size: 0.82rem;
  font-weight: 750;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1rem;
}

.dashboard-panel {
  padding: 1.1rem;
  border: 1px solid var(--border);
  border-radius: 26px;
  background:
    radial-gradient(circle at top right, rgba(95, 125, 232, 0.08), transparent 18rem),
    linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(248, 251, 255, 0.96));
  box-shadow: var(--shadow);
}

.dashboard-panel.large {
  grid-column: 1 / -1;
}

.dashboard-panel-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.95rem;
}

.dashboard-panel-header h2 {
  margin: 0;
  color: var(--ink);
  font-size: 1.15rem;
  font-weight: 950;
  letter-spacing: -0.025em;
}

.dashboard-exhibition-list,
.dashboard-mini-list {
  display: grid;
  gap: 0.75rem;
}

.dashboard-exhibition-card {
  display: grid;
  grid-template-columns: 14px minmax(0, 1fr) auto;
  gap: 0.85rem;
  align-items: center;
  padding: 0.9rem;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  text-align: left;
  box-shadow: var(--shadow-soft);
}

.dashboard-status-dot {
  width: 12px;
  height: 12px;
  border-radius: 999px;
  background: #7f8da5;
}

.dashboard-status-dot.live {
  background: #18b978;
}

.dashboard-status-dot.scheduled {
  background: var(--primary);
}

.dashboard-status-dot.closed {
  background: #7f8da5;
}

.dashboard-exhibition-main {
  min-width: 0;
}

.dashboard-exhibition-main strong,
.dashboard-mini-row strong {
  display: block;
  color: var(--ink);
  font-size: 0.95rem;
  font-weight: 950;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dashboard-exhibition-main small,
.dashboard-mini-row small {
  display: block;
  margin-top: 0.18rem;
  color: var(--muted);
  font-size: 0.82rem;
  font-weight: 750;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dashboard-exhibition-meta {
  display: grid;
  justify-items: end;
  gap: 0.18rem;
}

.dashboard-exhibition-meta em {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 0.65rem;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary-dark);
  font-size: 0.78rem;
  font-style: normal;
  font-weight: 900;
}

.dashboard-exhibition-meta em.live {
  background: #e9fbf3;
  color: #08734d;
}

.dashboard-exhibition-meta em.closed {
  background: #f2f5fa;
  color: #607087;
}

.dashboard-exhibition-meta small {
  color: var(--muted);
  font-size: 0.78rem;
  font-weight: 800;
}

.dashboard-mini-row {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 0.75rem;
  align-items: center;
  padding: 0.85rem;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--shadow-soft);
}

.mini-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 15px;
  background: var(--primary-soft);
  font-size: 1.15rem;
}

.mini-icon.gold {
  background: #fff6d8;
  color: #c99812;
}

@media (max-width: 1150px) {
  .dashboard-stat-grid,
  .dashboard-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .dashboard-summary-card.wide {
    grid-column: 1 / -1;
  }
}

@media (max-width: 900px) {
  .dashboard-hero-card {
    flex-direction: column;
  }

  .dashboard-hero-actions {
    flex-wrap: wrap;
  }

  .dashboard-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .dashboard-hero-card,
  .dashboard-panel {
    border-radius: 20px;
    padding: 1rem;
  }

  .dashboard-stat-grid,
  .dashboard-summary-grid {
    grid-template-columns: 1fr;
  }

  .dashboard-panel-header,
  .dashboard-exhibition-card {
    grid-template-columns: 1fr;
  }

  .dashboard-exhibition-meta {
    justify-items: start;
  }

  .dashboard-hero-actions,
  .dashboard-hero-actions .btn,
  .dashboard-hero-actions button {
    width: 100%;
  }
}
</style>
