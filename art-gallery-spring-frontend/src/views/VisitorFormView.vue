<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
import { visitors } from '../api/resources';
import { normalizeError } from '../api/http';

const props = defineProps({ id: { type: String, default: null } });
const router = useRouter();
const isEdit = computed(() => props.id != null);

const form = reactive({ name: '', email: '', phone: '', membershipType: '', joinDate: '' });
const fieldErrors = ref({});
const formError = ref(null);
const loading = ref(false);

onMounted(async () => {
  if (isEdit.value) {
    try {
      const v = await visitors.get(props.id);
      Object.assign(form, {
        name: v.name,
        email: v.email,
        phone: v.phone,
        membershipType: v.membershipType,
        joinDate: v.joinDate,
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
    email: form.email || null,
    phone: form.phone || null,
    membershipType: form.membershipType || null,
    joinDate: form.joinDate || null,
  };
  try {
    if (isEdit.value) await visitors.update(props.id, payload);
    else await visitors.create(payload);
    router.push({ name: 'visitors' });
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
    <h1>{{ isEdit ? 'Edit Visitor' : 'New Visitor' }}</h1>
    <router-link class="btn" :to="{ name: 'visitors' }">← Back</router-link>
  </div>

  <div class="card" style="max-width: 720px">
    <div v-if="formError" class="alert alert-error">{{ formError }}</div>

    <form @submit.prevent="submit">
      <div class="field">
        <label for="name">Name *</label>
        <input id="name" v-model="form.name" :class="{ invalid: fieldErrors.name }" />
        <div v-if="fieldErrors.name" class="field-error">{{ fieldErrors.name }}</div>
      </div>

      <div class="grid-2">
        <div class="field">
          <label for="email">Email</label>
          <input id="email" v-model="form.email" type="email" :class="{ invalid: fieldErrors.email }" />
          <div v-if="fieldErrors.email" class="field-error">{{ fieldErrors.email }}</div>
        </div>
        <div class="field">
          <label for="phone">Phone</label>
          <input id="phone" v-model="form.phone" :class="{ invalid: fieldErrors.phone }" />
          <div v-if="fieldErrors.phone" class="field-error">{{ fieldErrors.phone }}</div>
        </div>
      </div>

      <div class="grid-2">
        <div class="field">
          <label for="membershipType">Membership Type</label>
          <input id="membershipType" v-model="form.membershipType" :class="{ invalid: fieldErrors.membershipType }" />
          <div v-if="fieldErrors.membershipType" class="field-error">{{ fieldErrors.membershipType }}</div>
        </div>
        <div class="field">
          <label for="joinDate">Join Date</label>
          <input id="joinDate" v-model="form.joinDate" type="date" :class="{ invalid: fieldErrors.joinDate }" />
          <div v-if="fieldErrors.joinDate" class="field-error">{{ fieldErrors.joinDate }}</div>
        </div>
      </div>

      <div class="toolbar" style="margin-top: 0.5rem">
        <button class="btn btn-primary" :disabled="loading">{{ loading ? 'Saving…' : 'Save' }}</button>
        <router-link class="btn" :to="{ name: 'visitors' }">Cancel</router-link>
      </div>
    </form>
  </div>
</template>
