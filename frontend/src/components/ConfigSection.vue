<template>
  <section class="config-section">
    <h3 class="config-section-title">{{ title }}</h3>
    <div class="config-section-fields">
      <ConfigField
        v-for="[key, value] in fieldEntries"
        :key="key"
        :label="formatLabel(key)"
        :modelValue="value"
        :error="errors[key] || ''"
        @update:modelValue="onFieldUpdate(key, $event)"
      />
    </div>
    <button
      class="save-button"
      :disabled="hasErrors || saving"
      @click="$emit('save')"
    >
      <span v-if="saving" class="saving-indicator">
        <span class="spinner"></span>
        Saving...
      </span>
      <span v-else>Save</span>
    </button>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import ConfigField from './ConfigField.vue'

const props = defineProps<{
  title: string
  fields: Record<string, string | number | boolean>
  errors: Record<string, string>
  saving: boolean
}>()

const emit = defineEmits<{
  'update:fields': [fields: Record<string, string | number | boolean>]
  save: []
}>()

const fieldEntries = computed(() => Object.entries(props.fields))

const hasErrors = computed(() =>
  Object.values(props.errors).some((e) => e !== '')
)

function formatLabel(key: string): string {
  return key.replace(/([A-Z])/g, ' $1').replace(/^./, (s) => s.toUpperCase())
}

function onFieldUpdate(key: string, value: string | number | boolean) {
  emit('update:fields', { ...props.fields, [key]: value })
}
</script>

<style scoped>
.config-section {
  background-color: #1e1e36;
  border: 1px solid #2a2a4a;
  border-radius: 10px;
  padding: 24px;
  margin-bottom: 20px;
}

.config-section-title {
  margin: 0 0 16px 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: #e0e0e0;
  border-bottom: 1px solid #2a2a4a;
  padding-bottom: 12px;
}

.config-section-fields {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.save-button {
  margin-top: 20px;
  padding: 10px 28px;
  background-color: #bb86fc;
  color: #121212;
  border: none;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s, opacity 0.2s;
}

.save-button:hover:not(:disabled) {
  background-color: #ce9efc;
}

.save-button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.saving-indicator {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid #121212;
  border-top-color: transparent;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
