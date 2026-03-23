<template>
  <div class="message-inspector">
    <TransitionGroup name="message-fade" tag="div" class="message-list">
      <MessageCard
        v-for="(msg, index) in messages"
        :key="msg.capturedAt + '-' + msg.key + '-' + index"
        :event="msg"
      />
    </TransitionGroup>

    <div v-if="messages.length === 0" class="placeholder">
      Waiting for Kafka messages...
    </div>
  </div>
</template>

<script setup lang="ts">
import type { KafkaMessageEvent } from '../../types'
import MessageCard from './MessageCard.vue'

defineProps<{
  messages: KafkaMessageEvent[]
}>()
</script>

<style scoped>
.message-inspector {
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  min-height: 200px;
  max-height: 600px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #a0a0b8;
  font-size: 0.95rem;
  padding: 48px 16px;
  text-align: center;
}

/* Fade-in transition for new messages */
.message-fade-enter-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.message-fade-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}

.message-fade-leave-active {
  transition: opacity 0.2s ease;
}

.message-fade-leave-to {
  opacity: 0;
}
</style>
