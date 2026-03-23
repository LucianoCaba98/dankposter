import { ref, onUnmounted } from 'vue'
import type { KafkaMessageEvent } from '../types'

export function useKafkaMetricsSse() {
  const lastEvent = ref<KafkaMessageEvent | null>(null)
  let eventSource: EventSource | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  const connect = () => {
    eventSource = new EventSource('/api/events/kafka-metrics')

    eventSource.onmessage = (event) => {
      try {
        lastEvent.value = JSON.parse(event.data) as KafkaMessageEvent
      } catch (e) {
        console.error('Failed to parse Kafka metrics SSE event:', e)
      }
    }

    eventSource.onerror = () => {
      if (eventSource) {
        eventSource.close()
        eventSource = null
      }
      reconnectTimer = setTimeout(connect, 3000)
    }
  }

  const close = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer)
      reconnectTimer = null
    }
    if (eventSource) {
      eventSource.close()
      eventSource = null
    }
  }

  connect()

  onUnmounted(() => {
    close()
  })

  return { lastEvent, close }
}
