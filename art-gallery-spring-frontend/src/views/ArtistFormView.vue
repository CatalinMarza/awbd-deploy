<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { artists } from '../api/resources';
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
  name: '',
  nationality: '',
  birthYear: null,
  deathYear: null,
});

const fieldErrors = ref({});
const formError = ref(null);
const loading = ref(false);
const initialLoading = ref(true);

function clearFieldErrors(...keys) {
  const next = { ...fieldErrors.value };
  keys.forEach((key) => delete next[key]);
  fieldErrors.value = next;
}

function asEntity(payload) {
  if (payload?.data) return payload.data;
  return payload || {};
}

async function submit() {
  fieldErrors.value = {};
  formError.value = null;
  loading.value = true;

  const payload = {
    name: form.name?.trim() || null,
    nationality: form.nationality?.trim() || null,
    birthYear: form.birthYear ? Number(form.birthYear) : null,
    deathYear: form.deathYear ? Number(form.deathYear) : null,
  };

  try {
    if (isEdit.value) {
      await artists.update(props.id, payload);
    } else {
      await artists.create(payload);
    }

    router.push({ name: 'artists' });
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
    if (isEdit.value) {
      const loadedArtist = await artists.get(props.id);
      const artist = asEntity(loadedArtist);

      Object.assign(form, {
        name: artist.name || '',
        nationality: artist.nationality || '',
        birthYear: artist.birthYear || null,
        deathYear: artist.deathYear || null,
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
  <div class="form-page artist-form-page">
    <div class="form-page-header">
      <div>
        <p class="eyebrow">Artist management</p>
        <h1>{{ isEdit ? 'Edit Artist' : 'New Artist' }}</h1>
        <p class="muted">
          {{ isEdit ? 'Update artist profile information.' : 'Create an artist profile before adding artworks.' }}
        </p>
      </div>

      <router-link class="btn btn-secondary" :to="{ name: 'artists' }">
        ← Back
      </router-link>
    </div>

    <div class="form-shell artist-form-shell">
      <section class="entity-form-card">
        <div class="entity-form-card-header">
          <div class="entity-form-icon">🎨</div>

          <div>
            <p class="eyebrow">Form</p>
            <h2>Artist details</h2>
            <p>
              Keep the artist data concise and consistent with the artwork catalogue.
            </p>
          </div>
        </div>

        <div v-if="formError" class="alert alert-error">
          {{ formError }}
        </div>

        <div v-if="initialLoading" class="empty compact">
          Loading artist data...
        </div>

        <form v-else class="entity-form" @submit.prevent="submit">
          <section class="form-section">
            <div class="form-section-header">
              <div>
                <h3>Identity</h3>
                <p>Name is required. Nationality is optional.</p>
              </div>
            </div>

            <div class="field full-field">
              <label for="name">Name *</label>

              <input
                id="name"
                v-model="form.name"
                type="text"
                autocomplete="name"
                placeholder="Artist full name"
                :class="{ invalid: fieldErrors.name }"
                @input="clearFieldErrors('name')"
              />

              <div v-if="fieldErrors.name" class="field-error">
                {{ fieldErrors.name }}
              </div>
            </div>

            <div class="field full-field">
              <label for="nationality">Nationality</label>

              <input
                id="nationality"
                v-model="form.nationality"
                type="text"
                placeholder="Nationality"
                :class="{ invalid: fieldErrors.nationality }"
                @input="clearFieldErrors('nationality')"
              />

              <div v-if="fieldErrors.nationality" class="field-error">
                {{ fieldErrors.nationality }}
              </div>
            </div>
          </section>

          <section class="form-section">
            <div class="form-section-header">
              <div>
                <h3>Timeline</h3>
                <p>Use years only. Leave death year empty for living artists.</p>
              </div>
            </div>

            <div class="grid-2">
              <div class="field">
                <label for="birthYear">Birth Year</label>

                <input
                  id="birthYear"
                  v-model="form.birthYear"
                  type="number"
                  inputmode="numeric"
                  placeholder="e.g. 1840"
                  :class="{ invalid: fieldErrors.birthYear }"
                  @input="clearFieldErrors('birthYear')"
                />

                <div v-if="fieldErrors.birthYear" class="field-error">
                  {{ fieldErrors.birthYear }}
                </div>
              </div>

              <div class="field">
                <label for="deathYear">Death Year</label>

                <input
                  id="deathYear"
                  v-model="form.deathYear"
                  type="number"
                  inputmode="numeric"
                  placeholder="Optional"
                  :class="{ invalid: fieldErrors.deathYear }"
                  @input="clearFieldErrors('deathYear')"
                />

                <div v-if="fieldErrors.deathYear" class="field-error">
                  {{ fieldErrors.deathYear }}
                </div>
              </div>
            </div>
          </section>

          <div class="form-actions">
            <router-link class="btn btn-secondary" :to="{ name: 'artists' }">
              Cancel
            </router-link>

            <button type="submit" class="btn btn-primary" :disabled="loading">
              {{ loading ? 'Saving...' : 'Save Artist' }}
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

.form-shell {
  width: min(760px, 100%);
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
  background: linear-gradient(135deg, #a78bfa, var(--primary));
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

.form-section:last-of-type {
  margin-bottom: 0;
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
