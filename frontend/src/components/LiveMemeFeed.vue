<template>
  <div ref="feedRoot" class="live-feed">
    <h3 class="feed-heading">
      <span class="pulse-dot"></span>
      Live Meme Feed
    </h3>

    <p v-if="memes.length === 0" class="feed-placeholder">
      Waiting for memes to stream in...
    </p>

    <div v-else class="feed-scroll">
      <TransitionGroup name="meme-slide">
        <MemeCard
          v-for="meme in memes"
          :key="meme.id"
          :meme="meme"
          :hide-status="true"
          class="feed-meme"
        />
      </TransitionGroup>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onUnmounted } from 'vue'
import { useSse } from '../composables/useSse'
import MemeCard from './MemeCard.vue'
import type { Meme } from '../types'

const memes = ref<Meme[]>([])
const { lastEvent } = useSse('/api/events/posted')
const feedRoot = ref<HTMLElement | null>(null)

watch(lastEvent, (newMeme) => {
  if (newMeme && !memes.value.some(m => m.id === newMeme.id)) {
    memes.value = [newMeme, ...memes.value]
    nextTick(() => trimOverflow())
  }
})

function trimOverflow() {
  const el = feedRoot.value
  if (!el || memes.value.length <= 1) return
  // The element has max-height from CSS (grid cell constraint).
  // If scrollHeight > clientHeight, content overflows — trim oldest.
  while (memes.value.length > 1 && el.scrollHeight > el.clientHeight) {
    memes.value = memes.value.slice(0, -1)
  }
}

// Re-trim when the grid cell shrinks (docs sections collapse)
let observer: ResizeObserver | null = null

onMounted(() => {
  if (feedRoot.value) {
    observer = new ResizeObserver(() => {
      nextTick(() => trimOverflow())
    })
    observer.observe(feedRoot.value)
  }
})

onUnmounted(() => {
  observer?.disconnect()
})
</script>

<style scoped>
.live-feed {
  background-color: #1a1a2e;
  border: 1px solid #2a2a4a;
  border-radius: 10px;
  padding: 16px;
  max-height: 100%;
  overflow: hidden;
}

.feed-heading {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 1.1rem;
  font-weight: 700;
  color: #e0e0e0;
  margin: 0 0 12px;
}

.pulse-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #4caf50;
  display: inline-block;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.85); }
}

.feed-placeholder {
  color: #a0a0b8;
  text-align: center;
  padding: 32px 0;
  font-size: 0.9rem;
}

.feed-scroll {
  overflow: hidden;
}

.feed-meme {
  margin-bottom: 12px;
}

/* TransitionGroup animations */
.meme-slide-enter-active {
  transition: all 0.4s ease-out;
}

.meme-slide-leave-active {
  transition: all 0.3s ease-in;
}

.meme-slide-enter-from {
  opacity: 0;
  transform: translateY(-20px);
}

.meme-slide-leave-to {
  opacity: 0;
}
</style>
