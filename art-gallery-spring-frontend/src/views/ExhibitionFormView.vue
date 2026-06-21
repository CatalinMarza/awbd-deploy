<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { exhibitions, exhibitors } from '../api/resources';
import { normalizeError } from '../api/http';

const props = defineProps({ id: { type: String, default: null } });
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
const fieldErrors = ref({});
const formError = ref(null);
const loading = ref(false);

onMounted(async () => {
  try {
    exhibitorOptions.value = await exhibitors.all();
    if (isEdit.value) {
      const e = await exhibitions.get(props.id);
      Object.assign(form, {
        title: e.title,
        startDate: e.startDate,
        endDate: e.endDate,
        exhibitorId: e.exhibitorId,
        description: e.description,
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
    startDate: form.startDate || null,
    endDate: form.endDate || null,
    exhibitorId: form.exhibitorId ? Number(form.exhibitorId) : null,
    description: form.description || null,
  };
  try {
    if (isEdit.value) await exhibitions.update(props.id, payload);
    else await exhibitions.create(payload);
    router.push({ name: 'exhibitions' });
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
    <h1>{{ isEdit ? 'Edit Exhibition' : 'New Exhibition' }}</h1>
    <router-link class="btn" :to="{ name: 'exhibitions' }">← Back</router-link>
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
          <label for="startDate">Start Date *</label>
          <input id="startDate" v-model="form.startDate" type="date" :class="{ invalid: fieldErrors.startDate }" />
          <div v-if="fieldErrors.startDate" class="field-error">{{ fieldErrors.startDate }}</div>
        </div>
        <div class="field">
          <label for="endDate">End Date *</label>
          <input id="endDate" v-model="form.endDate" type="date" :class="{ invalid: fieldErrors.endDate }" />
          <div v-if="fieldErrors.endDate" class="field-error">{{ fieldErrors.endDate }}</div>
        </div>
      </div>

      <div class="field">
        <label for="exhibitorId">Exhibitor *</label>
        <select id="exhibitorId" v-model="form.exhibitorId" :class="{ invalid: fieldErrors.exhibitorId }">
          <option :value="null" disabled>— Select exhibitor —</option>
          <option v-for="e in exhibitorOptions" :key="e.id" :value="e.id">{{ e.name }}</option>
        </select>
        <div v-if="fieldErrors.exhibitorId" class="field-error">{{ fieldErrors.exhibitorId }}</div>
      </div>

      <div class="field">
        <label for="description">Description</label>
        <textarea id="description" v-model="form.description" rows="3"></textarea>
      </div>

      <div class="toolbar" style="margin-top: 0.5rem">
        <button class="btn btn-primary" :disabled="loading">{{ loading ? 'Saving…' : 'Save' }}</button>
        <router-link class="btn" :to="{ name: 'exhibitions' }">Cancel</router-link>
      </div>
    </form>
  </div>
</template>
