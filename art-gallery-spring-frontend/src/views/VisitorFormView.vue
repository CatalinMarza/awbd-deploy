<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { visitors } from '../api/resources';
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

const membershipOptions = [
  {
    value: 'Standard',
    title: 'Standard',
    description: 'Regular visitor profile',
    icon: '🎟️',
  },
  {
    value: 'VIP',
    title: 'VIP',
    description: 'Priority visitor profile',
    icon: '⭐',
  },
  {
    value: 'Student',
    title: 'Student',
    description: 'Student visitor profile',
    icon: '🎓',
  },
];

const form = reactive({
  name: '',
  email: '',
  phone: '',
  membershipType: 'Standard',

  // Hidden field kept for compatibility with the existing backend DTO.
  // It is intentionally not displayed in the form.
  joinDate: today(),
});

const fieldErrors = ref({});
const formError = ref(null);
const loading = ref(false);
const initialLoading = ref(true);

const selectedMembership = computed(() =>
  membershipOptions.find((option) => option.value === form.membershipType),
);

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

function selectMembership(value) {
  form.membershipType = value;
  clearFieldErrors('membershipType');
}

async function submit() {
  fieldErrors.value = {};
  formError.value = null;
  loading.value = true;

  const payload = {
    name: form.name?.trim() || null,
    email: form.email?.trim() || null,
    phone: form.phone?.trim() || null,
    membershipType: form.membershipType || null,
    joinDate: form.joinDate || today(),
  };

  try {
    if (isEdit.value) {
      await visitors.update(props.id, payload);
    } else {
      await visitors.create(payload);
    }

    router.push({ name: 'visitors' });
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
      const loadedVisitor = await visitors.get(props.id);
      const visitor = asEntity(loadedVisitor);

      Object.assign(form, {
        name: visitor.name || '',
        email: visitor.email || '',
        phone: visitor.phone || '',
        membershipType: visitor.membershipType || 'Standard',
        joinDate: visitor.joinDate || today(),
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
  <div class="visitor-form-page">
    <div class="visitor-page-header">
      <div>
        <p class="eyebrow">Visitor management</p>
        <h1>{{ isEdit ? 'Edit Visitor' : 'New Visitor' }}</h1>
        <p class="muted">
          {{ isEdit ? 'Update visitor profile and membership information.' : 'Create a visitor profile for reviews and gallery activity.' }}
        </p>
      </div>

      <div class="visitor-header-actions">
        <router-link class="btn" :to="{ name: 'visitors' }">
          ← Back
        </router-link>
      </div>
    </div>

    <div class="visitor-form-shell">
      <div class="visitor-form-card">
        <div class="visitor-form-header">
          <div class="visitor-icon">👤</div>

          <div>
            <p class="eyebrow">Form</p>
            <h2>Visitor details</h2>
            <p>
              Add identity, contact and membership details. The join date is handled automatically and is not shown in the form.
            </p>
          </div>
        </div>

        <div class="visitor-flow">
          <div class="visitor-flow-step" :class="{ active: form.name }">
            <span>1</span>
            <strong>Identity</strong>
          </div>

          <div class="visitor-flow-step" :class="{ active: form.email || form.phone }">
            <span>2</span>
            <strong>Contact</strong>
          </div>

          <div class="visitor-flow-step" :class="{ active: form.membershipType }">
            <span>3</span>
            <strong>Membership</strong>
          </div>
        </div>

        <div v-if="formError" class="alert alert-error">
          {{ formError }}
        </div>

        <div v-if="initialLoading" class="empty compact">
          Loading visitor data...
        </div>

        <form v-else class="visitor-form" @submit.prevent="submit">
          <section class="visitor-field-section">
            <div class="visitor-field-section-header">
              <div>
                <h3>Identity</h3>
                <p>The visitor name is required.</p>
              </div>
            </div>

            <div class="field full-field">
              <label for="name">Name *</label>

              <input
                id="name"
                v-model="form.name"
                type="text"
                autocomplete="name"
                placeholder="Visitor full name"
                :class="{ invalid: fieldErrors.name }"
                @input="clearFieldErrors('name')"
              />

              <div v-if="fieldErrors.name" class="field-error">
                {{ fieldErrors.name }}
              </div>
            </div>
          </section>

          <section class="visitor-field-section">
            <div class="visitor-field-section-header">
              <div>
                <h3>Contact information</h3>
                <p>Email and phone are optional, but useful for visitor identification.</p>
              </div>
            </div>

            <div class="grid-2">
              <div class="field">
                <label for="email">Email</label>

                <input
                  id="email"
                  v-model="form.email"
                  type="email"
                  autocomplete="email"
                  placeholder="visitor@example.com"
                  :class="{ invalid: fieldErrors.email }"
                  @input="clearFieldErrors('email')"
                />

                <div v-if="fieldErrors.email" class="field-error">
                  {{ fieldErrors.email }}
                </div>
              </div>

              <div class="field">
                <label for="phone">Phone</label>

                <input
                  id="phone"
                  v-model="form.phone"
                  type="tel"
                  autocomplete="tel"
                  placeholder="+40 700 000 000"
                  :class="{ invalid: fieldErrors.phone }"
                  @input="clearFieldErrors('phone')"
                />

                <div v-if="fieldErrors.phone" class="field-error">
                  {{ fieldErrors.phone }}
                </div>
              </div>
            </div>
          </section>

          <section class="visitor-field-section">
            <div class="visitor-field-section-header">
              <div>
                <h3>Membership</h3>
                <p>Select the visitor membership category.</p>
              </div>

              <span v-if="selectedMembership" class="visitor-select-hint">
                {{ selectedMembership.title }}
              </span>
            </div>

            <div class="membership-options">
              <button
                v-for="option in membershipOptions"
                :key="option.value"
                type="button"
                class="membership-option"
                :class="{ active: form.membershipType === option.value }"
                @click="selectMembership(option.value)"
              >
                <span class="membership-option-icon">
                  {{ option.icon }}
                </span>

                <span class="membership-option-content">
                  <strong>{{ option.title }}</strong>
                  <small>{{ option.description }}</small>
                </span>
              </button>
            </div>

            <div v-if="fieldErrors.membershipType" class="field-error">
              {{ fieldErrors.membershipType }}
            </div>
          </section>

          <div class="visitor-form-actions">
            <router-link class="btn btn-secondary" :to="{ name: 'visitors' }">
              Cancel
            </router-link>

            <button type="submit" class="btn btn-primary" :disabled="loading">
              {{ loading ? 'Saving...' : isEdit ? 'Save Visitor' : 'Create Visitor' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.visitor-form-page {
  width: 100%;
}

.visitor-page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1.25rem;
  margin-bottom: 1.35rem;
}

.visitor-page-header h1 {
  margin: 0;
  color: var(--ink);
  font-size: clamp(1.75rem, 3vw, 2.45rem);
  line-height: 1.05;
  letter-spacing: -0.055em;
}

.visitor-page-header p {
  max-width: 760px;
  margin-top: 0.7rem;
  line-height: 1.6;
}

.visitor-header-actions {
  flex: 0 0 auto;
  padding-top: 0.25rem;
}

.visitor-form-shell {
  width: min(900px, 100%);
  margin: 0 auto;
}

.visitor-form-card {
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

.visitor-form-card::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: inherit;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.65), transparent 38%),
    radial-gradient(circle at 8% 12%, rgba(95, 125, 232, 0.08), transparent 12rem);
}

.visitor-form-card > * {
  position: relative;
  z-index: 1;
}

.visitor-form-header {
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

.visitor-form-header h2 {
  margin: 0;
  color: var(--ink);
  font-size: 1.45rem;
  line-height: 1.1;
  letter-spacing: -0.035em;
}

.visitor-form-header p {
  margin: 0.45rem 0 0;
  color: var(--muted);
  line-height: 1.55;
}

.visitor-icon {
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

.visitor-flow {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.65rem;
  margin-bottom: 1rem;
}

.visitor-flow-step {
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

.visitor-flow-step span {
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

.visitor-flow-step strong {
  min-width: 0;
  color: inherit;
  font-size: 0.84rem;
  font-weight: 900;
  white-space: nowrap;
}

.visitor-flow-step.active {
  border-color: #d7e1ff;
  background: var(--primary-soft);
  color: var(--primary-dark);
}

.visitor-flow-step.active span {
  background: var(--primary);
  color: white;
}

.visitor-form {
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
}

.visitor-field-section {
  margin-bottom: 1rem;
  padding: 1.1rem;
  border: 1px solid var(--border);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: var(--shadow-soft);
}

.visitor-field-section:last-of-type {
  margin-bottom: 0;
}

.visitor-field-section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.95rem;
}

.visitor-field-section-header h3 {
  margin: 0;
  color: var(--ink);
  font-size: 1rem;
  letter-spacing: -0.02em;
}

.visitor-field-section-header p {
  margin: 0.25rem 0 0;
  color: var(--muted);
  font-size: 0.9rem;
  line-height: 1.45;
}

.visitor-field-section .field {
  margin-bottom: 0;
}

.visitor-field-section .grid-2 {
  gap: 1rem;
}

.full-field {
  width: 100%;
}

.visitor-select-hint {
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

.membership-options {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0.8rem;
}

.membership-option {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  gap: 0.75rem;
  align-items: center;
  min-height: 92px;
  padding: 0.9rem;
  border: 1px solid var(--border);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  text-align: left;
  cursor: pointer;
  transition:
    transform 0.18s ease,
    border-color 0.18s ease,
    background 0.18s ease,
    box-shadow 0.18s ease;
}

.membership-option:hover {
  transform: translateY(-1px);
  border-color: #d7e1ff;
  background: #fbfcff;
  box-shadow: 0 12px 24px rgba(95, 125, 232, 0.1);
}

.membership-option.active {
  border-color: #c7d5ff;
  background: var(--primary-soft);
  box-shadow: 0 14px 28px rgba(95, 125, 232, 0.13);
}

.membership-option-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 15px;
  background: #f2f5fb;
  font-size: 1.25rem;
}

.membership-option.active .membership-option-icon {
  background: var(--primary);
  color: white;
}

.membership-option-content {
  display: grid;
  gap: 0.18rem;
}

.membership-option-content strong {
  color: var(--ink);
  font-size: 0.95rem;
  font-weight: 950;
}

.membership-option-content small {
  color: var(--muted);
  font-size: 0.8rem;
  font-weight: 750;
  line-height: 1.35;
}

.visitor-form-actions {
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
  .visitor-form-shell {
    width: 100%;
  }

  .visitor-flow,
  .membership-options {
    grid-template-columns: 1fr;
  }

  .visitor-page-header {
    flex-direction: column;
  }

  .visitor-header-actions {
    padding-top: 0;
  }
}

@media (max-width: 640px) {
  .visitor-form-card {
    padding: 1rem;
    border-radius: 20px;
  }

  .visitor-form-header {
    grid-template-columns: 1fr;
  }

  .visitor-icon {
    width: 46px;
    height: 46px;
    border-radius: 16px;
  }

  .visitor-field-section {
    padding: 1rem;
    border-radius: 18px;
  }

  .visitor-field-section-header {
    flex-direction: column;
  }

  .visitor-form-actions {
    flex-direction: column-reverse;
  }

  .visitor-form-actions .btn,
  .visitor-form-actions button {
    width: 100%;
  }
}
</style>
