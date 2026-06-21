<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { artworks, artists, collections, locations } from '../api/resources';
import { normalizeError } from '../api/http';

const props = defineProps({ id: { type: String, default: null } });
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

onMounted(async () => {
  try {
    [artistOptions.value, collectionOptions.value, locationOptions.value] = await Promise.all([
      artists.all(),
      collections.all(),
      locations.all(),
    ]);
    if (isEdit.value) {
      const a = await artworks.get(props.id);
      Object.assign(form, {
        title: a.title,
        artistId: a.artistId,
        yearCreated: a.yearCreated,
        medium: a.medium,
        collectionId: a.collectionId,
        locationId: a.locationId,
        estimatedValue: a.estimatedValue,
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
    title: form.title,
    artistId: form.artistId ? Number(form.artistId) : null,
    yearCreated: form.yearCreated ? Number(form.yearCreated) : null,
    medium: form.medium || null,
    collectionId: form.collectionId ? Number(form.collectionId) : null,
    locationId: form.locationId ? Number(form.locationId) : null,
    estimatedValue: form.estimatedValue ? Number(form.estimatedValue) : null,
  };
  try {
    if (isEdit.value) await artworks.update(props.id, payload);
    else await artworks.create(payload);
    router.push({ name: 'artworks' });
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
    <h1>{{ isEdit ? 'Edit Artwork' : 'New Artwork' }}</h1>
    <router-link class="btn" :to="{ name: 'artworks' }">← Back</router-link>
  </div>

  <div class="card" style="max-width: 720px">
    <div v-if="formError" class="alert alert-error">{{ formError }}</div>

    <form @submit.prevent="submit">
      <div class="field">
        <label for="title">Title *</label>
        <input id="title" v-model="form.title" :class="{ invalid: fieldErrors.title }" />
        <div v-if="fieldErrors.title" class="field-error">{{ fieldErrors.title }}</div>
      </div>

      <div class="grid-2">
        <div class="field">
          <label for="artistId">Artist *</label>
          <select id="artistId" v-model="form.artistId" :class="{ invalid: fieldErrors.artistId }">
            <option :value="null" disabled>— Select artist —</option>
            <option v-for="a in artistOptions" :key="a.id" :value="a.id">{{ a.name }}</option>
          </select>
          <div v-if="fieldErrors.artistId" class="field-error">{{ fieldErrors.artistId }}</div>
        </div>
        <div class="field">
          <label for="yearCreated">Year Created</label>
          <input id="yearCreated" v-model="form.yearCreated" type="number" :class="{ invalid: fieldErrors.yearCreated }" />
          <div v-if="fieldErrors.yearCreated" class="field-error">{{ fieldErrors.yearCreated }}</div>
        </div>
      </div>

      <div class="field">
        <label for="medium">Medium</label>
        <input id="medium" v-model="form.medium" :class="{ invalid: fieldErrors.medium }" />
        <div v-if="fieldErrors.medium" class="field-error">{{ fieldErrors.medium }}</div>
      </div>

      <div class="grid-2">
        <div class="field">
          <label for="collectionId">Collection</label>
          <select id="collectionId" v-model="form.collectionId">
            <option :value="null">— None —</option>
            <option v-for="c in collectionOptions" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
        <div class="field">
          <label for="locationId">Location</label>
          <select id="locationId" v-model="form.locationId">
            <option :value="null">— None —</option>
            <option v-for="l in locationOptions" :key="l.id" :value="l.id">{{ l.name }}</option>
          </select>
        </div>
      </div>

      <div class="field">
        <label for="estimatedValue">Estimated Value (USD)</label>
        <input id="estimatedValue" v-model="form.estimatedValue" type="number" step="0.01" :class="{ invalid: fieldErrors.estimatedValue }" />
        <div v-if="fieldErrors.estimatedValue" class="field-error">{{ fieldErrors.estimatedValue }}</div>
      </div>

      <div class="toolbar" style="margin-top: 0.5rem">
        <button class="btn btn-primary" :disabled="loading">{{ loading ? 'Saving…' : 'Save' }}</button>
        <router-link class="btn" :to="{ name: 'artworks' }">Cancel</router-link>
      </div>
    </form>
  </div>
</template>
