<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { reviews, visitors, artworks, exhibitions } from '../api/resources';
import { normalizeError } from '../api/http';

const props = defineProps({ id: { type: String, default: null } });
const router = useRouter();
const isEdit = computed(() => props.id != null);

const form = reactive({
  visitorId: null,
  artworkId: null,
  exhibitionId: null,
  rating: 5,
  reviewText: '',
  reviewDate: new Date().toISOString().slice(0, 10),
});

const visitorOptions = ref([]);
const artworkOptions = ref([]);
const exhibitionOptions = ref([]);
const fieldErrors = ref({});
const formError = ref(null);
const loading = ref(false);

onMounted(async () => {
  try {
    [visitorOptions.value, artworkOptions.value, exhibitionOptions.value] = await Promise.all([
      visitors.all(),
      artworks.all(),
      exhibitions.all(),
    ]);
    if (isEdit.value) {
      const r = await reviews.get(props.id);
      Object.assign(form, {
        visitorId: r.visitorId,
        artworkId: r.artworkId,
        exhibitionId: r.exhibitionId,
        rating: r.rating,
        reviewText: r.reviewText,
        reviewDate: r.reviewDate,
      });
    }
  } catch (e) {
    formError.value = normalizeError(e).message;
  }
});

async function submit() {
  fieldErrors.value = {};
  formError.value = null;
  loading.value = true;
  const payload = {
    visitorId: form.visitorId ? Number(form.visitorId) : null,
    artworkId: form.artworkId ? Number(form.artworkId) : null,
    exhibitionId: form.exhibitionId ? Number(form.exhibitionId) : null,
    rating: form.rating ? Number(form.rating) : null,
    reviewText: form.reviewText || null,
    reviewDate: form.reviewDate || null,
  };
  try {
    if (isEdit.value) await reviews.update(props.id, payload);
    else await reviews.create(payload);
    router.push({ name: 'reviews' });
  } catch (e) {
    const n = normalizeError(e);
    if (Object.keys(n.fieldErrors).length) fieldErrors.value = n.fieldErrors;
    else formError.value = n.message;
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="page-header">
    <h1>{{ isEdit ? 'Edit Review' : 'New Review' }}</h1>
    <router-link class="btn" :to="{ name: 'reviews' }">← Back</router-link>
  </div>

  <div class="card" style="max-width: 720px">
    <div v-if="formError" class="alert alert-error">{{ formError }}</div>
    <p class="muted" style="font-size: 0.85rem; margin-top: 0">
      A review must reference an artwork, an exhibition, or both.
    </p>

    <form @submit.prevent="submit">
      <div class="field">
        <label for="visitorId">Visitor *</label>
        <select id="visitorId" v-model="form.visitorId" :class="{ invalid: fieldErrors.visitorId }">
          <option :value="null" disabled>— Select visitor —</option>
          <option v-for="v in visitorOptions" :key="v.id" :value="v.id">{{ v.name }}</option>
        </select>
        <div v-if="fieldErrors.visitorId" class="field-error">{{ fieldErrors.visitorId }}</div>
      </div>

      <div class="grid-2">
        <div class="field">
          <label for="artworkId">Artwork</label>
          <select id="artworkId" v-model="form.artworkId">
            <option :value="null">— None —</option>
            <option v-for="a in artworkOptions" :key="a.id" :value="a.id">{{ a.title }}</option>
          </select>
        </div>
        <div class="field">
          <label for="exhibitionId">Exhibition</label>
          <select id="exhibitionId" v-model="form.exhibitionId">
            <option :value="null">— None —</option>
            <option v-for="e in exhibitionOptions" :key="e.id" :value="e.id">{{ e.title }}</option>
          </select>
        </div>
      </div>

      <div class="grid-2">
        <div class="field">
          <label for="rating">Rating (1–5) *</label>
          <select id="rating" v-model="form.rating" :class="{ invalid: fieldErrors.rating }">
            <option v-for="n in 5" :key="n" :value="n">{{ n }}</option>
          </select>
          <div v-if="fieldErrors.rating" class="field-error">{{ fieldErrors.rating }}</div>
        </div>
        <div class="field">
          <label for="reviewDate">Review Date *</label>
          <input id="reviewDate" v-model="form.reviewDate" type="date" :class="{ invalid: fieldErrors.reviewDate }" />
          <div v-if="fieldErrors.reviewDate" class="field-error">{{ fieldErrors.reviewDate }}</div>
        </div>
      </div>

      <div class="field">
        <label for="reviewText">Review Text</label>
        <textarea id="reviewText" v-model="form.reviewText" rows="3"></textarea>
      </div>

      <div class="toolbar" style="margin-top: 0.5rem">
        <button class="btn btn-primary" :disabled="loading">{{ loading ? 'Saving…' : 'Save' }}</button>
        <router-link class="btn" :to="{ name: 'reviews' }">Cancel</router-link>
      </div>
    </form>
  </div>
</template>
