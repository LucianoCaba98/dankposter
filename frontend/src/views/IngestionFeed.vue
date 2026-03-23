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

    <div class="section">
      <div class="section-header" @click="queueExpanded = !queueExpanded">
        <span class="chevron">{{ queueExpanded ? '▼' : '▶' }}</span>
        <h2 class="section-title queue-accent">📋 Posting Queue ({{ queueMemes.length }})</h2>
      </div>
      <div v-show="queueExpanded" class="section-content">
        <div v-if="queueMemes.length === 0" class="feed-status">No memes queued for posting</div>
        <div v-else class="meme-row">
          <MemeCard
            v-for="meme in displayQueue"
            :key="'q-' + meme.id"
            :meme="meme"
          />
        </div>
      </div>
    </div>

    <div class="section">
      <div class="section-header" @click="ingestedExpanded = !ingestedExpanded">
        <span class="chevron">{{ ingestedExpanded ? '▼' : '▶' }}</span>
        <h2 class="section-title">📥 Ingested Memes</h2>
      </div>
      <div v-show="ingestedExpanded" class="section-content">
        <div v-if="loading" class="feed-status">Loading…</div>
        <div v-else-if="memes.length === 0" class="feed-status">No memes ingested yet</div>
        <div v-else class="meme-row">
          <MemeCard
            v-for="meme in displayMemes"
            :key="meme.id"
            :meme="meme"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import type { Meme } from '../types'
import { useSse } from '../composables/useSse'
import MemeCard from '../components/MemeCard.vue'

const memes = ref<Meme[]>([])
const queueMemes = ref<Meme[]>([])
const loading = ref(true)
const ingestedCount = ref(0)
const postedCount = ref(0)
const queueExpanded = ref(true)
const ingestedExpanded = ref(true)

const displayQueue = computed(() => queueMemes.value.slice(0, 10))
const displayMemes = computed(() => memes.value.slice(0, 10))

const { lastEvent: ingestionEvent } = useSse('/api/events/ingestion')
const { lastEvent: postedEvent } = useSse('/api/events/posted')

async function fetchQueue() {
  try {
    const res = await fetch('/api/memes/queue')
    if (res.ok) queueMemes.value = await res.json()
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
    if (idx !== -1) memes.value[idx] = { ...memes.value[idx], posted: true }
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
.feed-status { text-align: center; color: #a0a0b8; padding: 32px 0; font-size: 1rem; }

.section { margin-bottom: 24px; border: 1px solid #2a2a4a; border-radius: 10px; background: #0f0f1a; overflow: hidden; }
.section-header { display: flex; align-items: center; gap: 10px; padding: 12px 16px; cursor: pointer; user-select: none; }
.section-header:hover { background: rgba(187, 134, 252, 0.05); }
.chevron { font-size: 0.8rem; color: #a0a0b8; width: 16px; }
.section-title { font-size: 1.2rem; font-weight: 700; color: #e0e0e0; }
.queue-accent { color: #bb86fc; }
.section-content { padding: 0 16px 16px; }

.meme-row {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.meme-row::-webkit-scrollbar { display: none; }
.meme-row > * { flex-shrink: 0; width: 160px; min-width: 160px; }
.meme-row :deep(.meme-media) { height: 120px; object-fit: cover; }
.meme-row :deep(.meme-info) { padding: 4px 8px 6px; }
.meme-row :deep(.meme-title) { font-size: 0.7rem; }
</style>
