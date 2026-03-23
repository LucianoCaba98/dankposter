<template>
  <div class="ingestion-feed">
    <div class="feed-header">
      <h2 class="feed-title">Ingested Memes</h2>
      <div class="live-counters">
        <span class="counter ingested-counter">📥 {{ ingestedCount }} ingested</span>
        <span class="counter-divider">·</span>
        <span class="counter posted-counter">📤 {{ postedCount }} posted</span>
      </div>
    </div>

    <div class="queue-section">
      <div class="queue-header">
        <h2 class="queue-title">📋 Posting Queue ({{ queueMemes.length }})</h2>
      </div>
      <div v-if="queueMemes.length === 0" class="feed-status">No memes queued for posting</div>
      <div v-else class="meme-grid">
        <MemeCard v-for="meme in queueMemes" :key="'queue-' + meme.id" :meme="meme" />
      </div>
    </div>

    <div class="ingested-section">
      <div class="ingested-header">
        <h2 class="feed-title">📥 Ingested Memes</h2>
      </div>
      <div v-if="loading" class="feed-status">Loading…</div>
      <div v-else-if="memes.length === 0" class="feed-status">No memes ingested yet</div>
      <div v-else class="meme-grid">
        <MemeCard v-for="meme in memes" :key="meme.id" :meme="meme" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import type { Meme } from '../types'
import { useSse } from '../composables/useSse'
import MemeCard from '../components/MemeCard.vue'

const memes = ref<Meme[]>([])
const queueMemes = ref<Meme[]>([])
const loading = ref(true)
const ingestedCount = ref(0)
const postedCount = ref(0)

const { lastEvent: ingestionEvent } = useSse('/api/events/ingestion')
const { lastEvent: postedEvent } = useSse('/api/events/posted')

async function fetchQueue() {
  try {
    const res = await fetch('/api/memes/queue')
    if (res.ok) {
      queueMemes.value = await res.json()
    }
  } catch (e) {
    console.error('Failed to fetch posting queue:', e)
  }
}

watch(ingestionEvent, (newMeme) => {
  if (newMeme && !memes.value.some(m => m.id === newMeme.id)) {
    memes.value = [newMeme, ...memes.value]
    ingestedCount.value++
  }
  fetchQueue()
})

watch(postedEvent, (postedMeme) => {
  if (postedMeme) {
    postedCount.value++
    const idx = memes.value.findIndex(m => m.id === postedMeme.id)
    if (idx !== -1) {
      memes.value[idx] = { ...memes.value[idx], posted: true }
    }
  }
  fetchQueue()
})

onMounted(async () => {
  try {
    const [allRes, postedRes] = await Promise.all([
      fetch('/api/memes/all?page=0&size=50'),
      fetch('/api/memes/posted?page=0&size=1')
    ])
    if (allRes.ok) {
      const data = await allRes.json()
      memes.value = data.content
      ingestedCount.value = data.totalElements ?? 0
    }
    if (postedRes.ok) {
      const data = await postedRes.json()
      postedCount.value = data.totalElements ?? 0
    }
  } catch (e) {
    console.error('Failed to fetch memes:', e)
  } finally {
    loading.value = false
  }
  fetchQueue()
})
</script>

<style scoped>
.ingestion-feed { padding: 8px 0; }
.feed-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.feed-title { font-size: 1.4rem; font-weight: 700; color: #e0e0e0; }
.live-counters { display: flex; align-items: center; gap: 8px; font-size: 0.85rem; }
.counter { font-weight: 500; }
.ingested-counter { color: #64b5f6; }
.posted-counter { color: #52b788; }
.counter-divider { color: #4a4a6a; }
.feed-status { text-align: center; color: #a0a0b8; padding: 48px 0; font-size: 1rem; }
.meme-grid { columns: 3 300px; column-gap: 20px; }
.meme-grid > * { margin-bottom: 20px; }

.queue-section {
  margin-bottom: 40px;
  padding-bottom: 28px;
  border-bottom: 2px solid #2a2a4a;
}
.ingested-section {
  padding-top: 0;
}
.ingested-header {
  margin-bottom: 20px;
}
.queue-header {
  margin-bottom: 20px;
}
.queue-title {
  font-size: 1.4rem;
  font-weight: 700;
  color: #bb86fc;
}
</style>
