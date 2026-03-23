<template>
  <div class="annotation-tooltip">
    <span class="annotation-label">{{ label }}:</span>
    <span class="annotation-text">{{ annotation }}</span>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  label: string
  annotation: string
}>()
</script>

<style scoped>
.annotation-tooltip {
  font-size: 0.8rem;
  color: #a0a0b8;
  font-style: italic;
  line-height: 1.4;
}

.annotation-label {
  font-weight: 600;
}

.annotation-text {
  margin-left: 4px;
}
</style>
