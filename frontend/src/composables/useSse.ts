import { ref, onUnmounted } from 'vue'
import type { Meme } from '../types'

export function useSse(url: string) {
  const lastEvent = ref<Meme | null>(null)
  let eventSource: EventSource | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null

  const connect = () => {
    eventSource = new EventSource(url)

    eventSource.onmessage = (event) => {
      try {
        lastEvent.value = JSON.parse(event.data) as Meme
      } catch (e) {
        console.error('Failed to parse SSE event:', e)
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
