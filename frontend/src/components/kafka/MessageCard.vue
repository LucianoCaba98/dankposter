<template>
  <div class="message-card">
    <div class="summary-row" @click="expanded = !expanded">
      <span
        class="status-badge"
        :style="{ color: statusColors[event.deliveryStatus].text, backgroundColor: statusColors[event.deliveryStatus].bg }"
      >
        {{ event.deliveryStatus }}
      </span>
      <span class="message-key">{{ event.key }}</span>
      <span class="message-topic">{{ event.topic }}</span>
      <span class="message-time">{{ relativeTime }}</span>
    </div>

    <div v-if="expanded" class="detail-view">
      <div class="detail-field">
        <div class="field-row">
          <span class="field-label">topic:</span>
          <span class="field-value">{{ event.topic }}</span>
        </div>
        <AnnotationTooltip label="topic" annotation="A topic is a named channel for categorizing messages" />
      </div>

      <div class="detail-field">
        <div class="field-row">
          <span class="field-label">partition:</span>
          <span class="field-value">{{ event.partition ?? 'N/A' }}</span>
        </div>
        <AnnotationTooltip label="partition" annotation="Partitions enable parallel processing — messages with the same key go to the same partition" />
      </div>

      <div class="detail-field">
        <div class="field-row">
          <span class="field-label">offset:</span>
          <span class="field-value">{{ event.offset ?? 'N/A' }}</span>
        </div>
        <AnnotationTooltip label="offset" annotation="A sequential ID tracking the message position within a partition" />
      </div>

      <div class="detail-field">
        <div class="field-row">
          <span class="field-label">key:</span>
          <span class="field-value">{{ event.key }}</span>
        </div>
        <AnnotationTooltip label="key" annotation="Determines partition assignment — DankPoster uses memeId as the key" />
      </div>

      <div class="detail-field">
        <div class="field-row">
          <span class="field-label">timestamp:</span>
          <span class="field-value">{{ event.timestamp ?? 'N/A' }}</span>
        </div>
        <AnnotationTooltip label="timestamp" annotation="Kafka records when the message was produced" />
      </div>

      <div class="detail-field">
        <div class="field-row">
          <span class="field-label">payload:</span>
        </div>
        <pre class="payload-block">{{ formattedPayload }}</pre>
        <AnnotationTooltip label="payload" annotation="The serialized MemeDeliveryEvent JSON" />
      </div>

      <div class="detail-field">
        <div class="field-row">
          <span class="field-label">deliveryStatus:</span>
          <span
            class="field-value"
            :style="{ color: statusColors[event.deliveryStatus].text }"
          >
            {{ event.deliveryStatus }}
          </span>
        </div>
        <AnnotationTooltip label="deliveryStatus" annotation="Lifecycle stages: produced → consumed → delivered / failed" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { KafkaMessageEvent } from '../../types'
import AnnotationTooltip from './AnnotationTooltip.vue'

const props = defineProps<{
  event: KafkaMessageEvent
}>()

const expanded = ref(false)

const statusColors: Record<KafkaMessageEvent['deliveryStatus'], { text: string; bg: string }> = {
  produced: { text: '#64b5f6', bg: '#1a2a3a' },
  consumed: { text: '#ffd54f', bg: '#2a2a1a' },
  delivered: { text: '#52b788', bg: '#1b4332' },
  failed: { text: '#ef5350', bg: '#3a1a1a' },
}

const relativeTime = computed(() => {
  const now = Date.now()
  const captured = new Date(props.event.capturedAt).getTime()
  const diffSeconds = Math.floor((now - captured) / 1000)

  if (diffSeconds < 60) return `${diffSeconds}s ago`
  if (diffSeconds < 3600) return `${Math.floor(diffSeconds / 60)}m ago`
  return `${Math.floor(diffSeconds / 3600)}h ago`
})

const formattedPayload = computed(() => {
  try {
    return JSON.stringify(JSON.parse(props.event.payload), null, 2)
  } catch {
    return props.event.payload
  }
})
</script>

<style scoped>
.message-card {
  background-color: #1a1a2e;
  border: 1px solid #2a2a4a;
  border-radius: 8px;
  overflow: hidden;
}

.summary-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.summary-row:hover {
  background-color: #22223a;
}

.status-badge {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 4px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.message-key {
  color: #e0e0e0;
  font-family: monospace;
  font-size: 0.9rem;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  min-width: 0;
}

.message-topic {
  color: #a0a0b8;
  font-size: 0.85rem;
  white-space: nowrap;
}

.message-time {
  color: #a0a0b8;
  font-size: 0.8rem;
  margin-left: auto;
  white-space: nowrap;
}

.detail-view {
  border-top: 1px solid #2a2a4a;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.detail-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.field-row {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.field-label {
  color: #bb86fc;
  font-size: 0.85rem;
  font-weight: 600;
  font-family: monospace;
}

.field-value {
  color: #e0e0e0;
  font-size: 0.9rem;
  font-family: monospace;
  word-break: break-all;
}

.payload-block {
  background-color: #12121e;
  border: 1px solid #2a2a4a;
  border-radius: 6px;
  padding: 12px;
  margin: 4px 0 0 0;
  color: #e0e0e0;
  font-family: monospace;
  font-size: 0.8rem;
  line-height: 1.5;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
