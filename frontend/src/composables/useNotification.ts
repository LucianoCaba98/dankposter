import { ref } from 'vue'

interface Notification {
  id: number
  type: 'success' | 'error'
  message: string
}

let nextId = 0

const notifications = ref<Notification[]>([])

export function useNotification() {
  const notify = (type: 'success' | 'error', message: string, duration = type === 'success' ? 3000 : 0) => {
    const id = nextId++
    notifications.value.push({ id, type, message })

    if (duration > 0) {
      setTimeout(() => {
        dismiss(id)
      }, duration)
    }
  }

  const dismiss = (id: number) => {
    notifications.value = notifications.value.filter(n => n.id !== id)
  }

  return { notifications, notify, dismiss }
}
