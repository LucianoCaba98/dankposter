<template>
  <div class="metrics-panel">
    <div class="metric-card">
      <span class="metric-value">{{ totalCount }}</span>
      <span class="metric-label">Total Messages</span>
    </div>
    <div class="metric-card">
      <span class="metric-value throughput">{{ throughput }}</span>
      <span class="metric-label">Messages / sec</span>
    </div>
    <div class="metric-card">
      <span class="metric-value delivered">{{ deliveredCount }}</span>
      <span class="metric-label">Successful Deliveries</span>
    </div>
    <div class="metric-card">
      <span class="metric-value failed">{{ failedCount }}</span>
      <span class="metric-label">Failed Deliveries</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { KafkaMessageEvent } from '../../types'

const props = defineProps<{
  messages: KafkaMessageEvent[]
}>()

const totalCount = computed(() => props.messages.length)

const throughput = computed(() => {
  const now = Date.now()
  const windowMs = 30_000
  const recentCount = props.messages.filter((m) => {
    const capturedTime = new Date(m.capturedAt).getTime()
    return now - capturedTime <= windowMs
  }).length
  return (recentCount / 30).toFixed(1)
})

const deliveredCount = computed(() =>
  props.messages.filter((m) => m.deliveryStatus === 'delivered').length
)

const failedCount = computed(() =>
  props.messages.filter((m) => m.deliveryStatus === 'failed').length
)
</script>

<style scoped>
.metrics-panel {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.metric-card {
  background-color: #1a1a2e;
  border: 1px solid #2a2a4a;
  border-radius: 10px;
  padding: 20px 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.metric-value {
  font-size: 2rem;
  font-weight: 700;
  color: #e0e0e0;
}

.metric-value.throughput {
  color: #bb86fc;
}

.metric-value.delivered {
  color: #52b788;
}

.metric-value.failed {
  color: #ef5350;
}

.metric-label {
  font-size: 0.85rem;
  color: #a0a0b8;
  text-align: center;
}

@media (max-width: 900px) {
  .metrics-panel {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
