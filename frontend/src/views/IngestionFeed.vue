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

    <div v-if="loading" class="feed-status">Loading…</div>

    <div v-else-if="memes.length === 0" class="feed-status">No memes ingested yet</div>

    <div v-else class="meme-grid">
      <MemeCard v-for="meme in memes" :key="meme.id" :meme="meme" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import type { Meme } from '../types'
import { useSse } from '../composables/useSse'
import MemeCard from '../components/MemeCard.vue'

const memes = ref<Meme[]>([])
const loading = ref(true)
const ingestedCount = ref(0)
const postedCount = ref(0)

const { lastEvent: ingestionEvent } = useSse('/api/events/ingestion')
const { lastEvent: postedEvent } = useSse('/api/events/posted')

watch(ingestionEvent, (newMeme) => {
  if (newMeme) {
    memes.value = [newMeme, ...memes.value]
    ingestedCount.value++
  }
})

watch(postedEvent, (postedMeme) => {
  if (postedMeme) {
    postedCount.value++
    const idx = memes.value.findIndex(m => m.id === postedMeme.id)
    if (idx !== -1) {
      memes.value[idx] = { ...memes.value[idx], posted: true }
    }
  }
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
.meme-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }
</style>
