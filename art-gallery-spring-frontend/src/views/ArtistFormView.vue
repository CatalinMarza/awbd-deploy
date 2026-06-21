<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { artists } from '../api/resources';
import { normalizeError } from '../api/http';

const props = defineProps({ id: { type: String, default: null } });
const router = useRouter();
const isEdit = computed(() => props.id != null);

const form = reactive({ name: '', nationality: '', birthYear: null, deathYear: null });
const fieldErrors = ref({});
const formError = ref(null);
const loading = ref(false);

onMounted(async () => {
  if (isEdit.value) {
    try {
      const a = await artists.get(props.id);
      Object.assign(form, {
        name: a.name,
        nationality: a.nationality,
        birthYear: a.birthYear,
        deathYear: a.deathYear,
      });
    } catch (e) {
      formError.value = normalizeError(e).message;
    }
  }
});

async function submit() {
  fieldErrors.value = {};
  formError.value = null;
  loading.value = true;
  const payload = {
    name: form.name,
    nationality: form.nationality || null,
    birthYear: form.birthYear ? Number(form.birthYear) : null,
    deathYear: form.deathYear ? Number(form.deathYear) : null,
  };
  try {
    if (isEdit.value) await artists.update(props.id, payload);
    else await artists.create(payload);
    router.push({ name: 'artists' });
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
    <h1>{{ isEdit ? 'Edit Artist' : 'New Artist' }}</h1>
    <router-link class="btn" :to="{ name: 'artists' }">← Back</router-link>
  </div>

  <div class="card" style="max-width: 640px">
    <div v-if="formError" class="alert alert-error">{{ formError }}</div>

    <form @submit.prevent="submit">
      <div class="field">
        <label for="name">Name *</label>
        <input id="name" v-model="form.name" :class="{ invalid: fieldErrors.name }" />
        <div v-if="fieldErrors.name" class="field-error">{{ fieldErrors.name }}</div>
      </div>

      <div class="field">
        <label for="nationality">Nationality</label>
        <input id="nationality" v-model="form.nationality" :class="{ invalid: fieldErrors.nationality }" />
        <div v-if="fieldErrors.nationality" class="field-error">{{ fieldErrors.nationality }}</div>
      </div>

      <div class="grid-2">
        <div class="field">
          <label for="birthYear">Birth Year</label>
          <input id="birthYear" v-model="form.birthYear" type="number" :class="{ invalid: fieldErrors.birthYear }" />
          <div v-if="fieldErrors.birthYear" class="field-error">{{ fieldErrors.birthYear }}</div>
        </div>
        <div class="field">
          <label for="deathYear">Death Year</label>
          <input id="deathYear" v-model="form.deathYear" type="number" :class="{ invalid: fieldErrors.deathYear }" />
          <div v-if="fieldErrors.deathYear" class="field-error">{{ fieldErrors.deathYear }}</div>
        </div>
      </div>

      <div class="toolbar" style="margin-top: 0.5rem">
        <button class="btn btn-primary" :disabled="loading">{{ loading ? 'Saving…' : 'Save' }}</button>
        <router-link class="btn" :to="{ name: 'artists' }">Cancel</router-link>
      </div>
    </form>
  </div>
</template>
