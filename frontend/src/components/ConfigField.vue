<template>
  <div class="config-field">
    <label class="config-field-label">{{ label }}</label>
    <div class="config-field-input">
      <!-- Boolean toggle -->
      <label v-if="typeof modelValue === 'boolean'" class="toggle-switch">
        <input
          type="checkbox"
          :checked="modelValue"
          @change="$emit('update:modelValue', ($event.target as HTMLInputElement).checked)"
        />
        <span class="toggle-slider"></span>
      </label>

      <!-- Number input -->
      <input
        v-else-if="typeof modelValue === 'number'"
        type="number"
        class="field-input"
        :class="{ 'field-input-error': error }"
        :value="modelValue"
        @input="$emit('update:modelValue', Number(($event.target as HTMLInputElement).value))"
      />

      <!-- Text input (default) -->
      <input
        v-else
        type="text"
        class="field-input"
        :class="{ 'field-input-error': error }"
        :value="modelValue"
        @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      />
    </div>
    <span v-if="error" class="field-error">{{ error }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  modelValue: string | number | boolean
  error: string
}>()

defineEmits<{
  'update:modelValue': [value: string | number | boolean]
}>()
</script>

<style scoped>
.config-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 10px 0;
}

.config-field-label {
  font-size: 0.85rem;
  font-weight: 500;
  color: #a0a0b8;
  text-transform: capitalize;
}

.config-field-input {
  display: flex;
  align-items: center;
}

.field-input {
  width: 100%;
  padding: 8px 12px;
  background-color: #1a1a2e;
  border: 1px solid #2a2a4a;
  border-radius: 6px;
  color: #e0e0e0;
  font-size: 0.9rem;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s;
}

.field-input:focus {
  border-color: #bb86fc;
}

.field-input-error {
  border-color: #cf6679;
}

.field-input-error:focus {
  border-color: #cf6679;
}

.field-input[type='number']::-webkit-inner-spin-button,
.field-input[type='number']::-webkit-outer-spin-button {
  opacity: 1;
}

.toggle-switch {
  position: relative;
  display: inline-block;
  width: 44px;
  height: 24px;
  cursor: pointer;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-slider {
  position: absolute;
  inset: 0;
  background-color: #2a2a4a;
  border-radius: 12px;
  transition: background-color 0.2s;
}

.toggle-slider::before {
  content: '';
  position: absolute;
  height: 18px;
  width: 18px;
  left: 3px;
  bottom: 3px;
  background-color: #a0a0b8;
  border-radius: 50%;
  transition: transform 0.2s, background-color 0.2s;
}

.toggle-switch input:checked + .toggle-slider {
  background-color: #3a2a6e;
}

.toggle-switch input:checked + .toggle-slider::before {
  transform: translateX(20px);
  background-color: #bb86fc;
}

.field-error {
  font-size: 0.78rem;
  color: #cf6679;
  line-height: 1.3;
}
</style>
