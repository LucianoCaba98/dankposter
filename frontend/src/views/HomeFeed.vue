<template>
  <div class="home-feed">
    <h2 class="feed-title">Posted Memes</h2>

    <div v-if="loading" class="feed-status">Loading…</div>

    <div v-else-if="memes.length === 0" class="feed-status">No posted memes yet</div>

    <div v-else class="meme-grid">
      <MemeCard v-for="meme in memes" :key="meme.id" :meme="meme" :hide-status="true" />
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

const { lastEvent } = useSse('/api/events/posted')

watch(lastEvent, (newMeme) => {
  if (newMeme) {
    memes.value = [newMeme, ...memes.value]
  }
})

onMounted(async () => {
  try {
    const res = await fetch('/api/memes/posted?page=0&size=50')
    if (res.ok) {
      const page = await res.json()
      memes.value = page.content
    }
  } catch (e) {
    console.error('Failed to fetch posted memes:', e)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.home-feed { padding: 8px 0; }
.feed-title { font-size: 1.4rem; font-weight: 700; color: #e0e0e0; margin-bottom: 20px; }
.feed-status { text-align: center; color: #a0a0b8; padding: 48px 0; font-size: 1rem; }
.meme-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }
</style>
