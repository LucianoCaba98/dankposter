<template>
  <div class="docs-section">
    <div
      class="section-header"
      role="button"
      tabindex="0"
      @click="emit('toggle', sectionId)"
      @keydown.enter="emit('toggle', sectionId)"
      @keydown.space.prevent="emit('toggle', sectionId)"
    >
      <span class="section-title">{{ title }}</span>
      <svg
        class="chevron"
        :class="{ expanded }"
        width="20"
        height="20"
        viewBox="0 0 20 20"
        fill="none"
        aria-hidden="true"
      >
        <path
          d="M6 8l4 4 4-4"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </div>
    <div class="section-body" :class="{ expanded }">
      <div class="section-content">
        <slot />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const { title, sectionId, expanded } = defineProps<{
  title: string
  sectionId: string
  expanded: boolean
}>()

const emit = defineEmits<{
  toggle: [sectionId: string]
}>()
</script>

<style scoped>
.docs-section {
  background-color: #1a1a2e;
  border: 1px solid #2a2a4a;
  border-radius: 10px;
  margin-bottom: 16px;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.2s;
}

.section-header:hover {
  background-color: rgba(187, 134, 252, 0.08);
}

.section-header:hover .section-title {
  color: #bb86fc;
}

.section-title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #e0e0e0;
  transition: color 0.2s;
}

.chevron {
  color: #a0a0b8;
  transition: transform 0.3s ease;
  flex-shrink: 0;
}

.chevron.expanded {
  transform: rotate(180deg);
}

.section-body {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.35s ease;
}

.section-body.expanded {
  max-height: 4000px;
}

.section-content {
  padding: 0 20px 20px;
  color: #c0c0d8;
  line-height: 1.7;
  font-size: 0.95rem;
}
</style>
