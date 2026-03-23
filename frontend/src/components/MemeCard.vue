<template>
  <div
    class="meme-card"
    :class="{ 'error-flash': errorFlash }"
    :style="cardStyle"
    @pointerdown="onPointerDown"
    @pointermove="onPointerMove"
    @pointerup="onPointerUp"
    @pointercancel="onPointerUp"
    @click="onCardClick"
  >
    <video
      v-if="isVideo"
      :src="videoSrc"
      class="meme-media"
      muted
      loop
      autoplay
      playsinline
      @error="onMediaError"
    />
    <img
      v-else
      :src="mediaSrc"
      :alt="meme.title"
      class="meme-media"
      loading="lazy"
      @error="onMediaError"
    />
    <div class="meme-info">
      <h3 class="meme-title">{{ meme.title }}</h3>
      <div v-if="!hideStatus" class="meme-meta">
        <span :class="['posted-badge', meme.posted ? 'posted' : 'pending']">
          {{ meme.posted ? 'Posted' : 'Pending' }}
        </span>
        <span v-if="meme.source" class="source-badge">{{ meme.source }}</span>
      </div>
    </div>

    <div v-if="showActions" class="action-buttons">
      <button class="action-btn discard-btn" @click.stop="onDiscard" aria-label="Discard meme">❌</button>
      <button
        class="action-btn like-btn"
        :class="{ liked: localLiked }"
        @click.stop="onLike"
        aria-label="Like meme"
      >{{ localLiked ? '❤️' : '🤍' }}</button>
    </div>

    <Teleport to="body">
      <div v-if="expanded" class="overlay" @click.self="expanded = false">
        <div class="expanded-card">
          <button class="close-btn" @click.stop="expanded = false" aria-label="Close">&times;</button>
          <video
            v-if="isVideo"
            :src="videoSrc"
            class="expanded-media"
            controls
            autoplay
            loop
          />
          <img v-else :src="mediaSrc" :alt="meme.title" class="expanded-media" />
          <div class="expanded-info">
            <h3>{{ meme.title }}</h3>
            <div v-if="!hideStatus" class="expanded-meta">
              <span :class="['posted-badge', meme.posted ? 'posted' : 'pending']">
                {{ meme.posted ? 'Posted' : 'Pending' }}
              </span>
              <span v-if="meme.source" class="source-badge">{{ meme.source }}</span>
            </div>
            <p v-if="meme.description" class="expanded-description">{{ meme.description }}</p>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { Meme } from '../types'

const props = withDefaults(defineProps<{ meme: Meme; hideStatus?: boolean; showActions?: boolean }>(), {
  hideStatus: false,
  showActions: false
})

const emit = defineEmits<{ discard: [id: number] }>()

const expanded = ref(false)
const mediaFailed = ref(false)
const localLiked = ref(props.meme.liked ?? false)
const errorFlash = ref(false)

// Swipe state
const swiping = ref(false)
const startX = ref(0)
const translateX = ref(0)
const hasDragged = ref(false)

const discarding = ref(false)

watch(() => props.meme.liked, (v) => { localLiked.value = v ?? false })

const isVideo = computed(() => {
  const url = props.meme.imageUrl?.toLowerCase() ?? ''
  return url.endsWith('.mp4') || url.endsWith('.gifv') || url.includes('v.redd.it')
})

const videoSrc = computed(() => {
  const url = props.meme.imageUrl ?? ''
  if (url.endsWith('.gifv')) return url.replace('.gifv', '.mp4')
  if (url.includes('v.redd.it') && !url.includes('DASH')) return url + '/DASH_480.mp4'
  return url
})

const mediaSrc = computed(() =>
  mediaFailed.value
    ? 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="300" height="200"%3E%3Crect fill="%231a1a2e" width="300" height="200"/%3E%3Ctext x="50%25" y="50%25" dominant-baseline="middle" text-anchor="middle" fill="%23a0a0b8" font-size="14"%3EMedia unavailable%3C/text%3E%3C/svg%3E'
    : props.meme.imageUrl
)

const cardStyle = computed(() => ({
  transform: translateX.value !== 0 ? `translateX(${translateX.value}px)` : discarding.value ? 'translateX(-120%)' : undefined,
  transition: swiping.value ? 'none' : 'transform 0.3s ease, opacity 0.3s ease',
  opacity: discarding.value ? 0 : translateX.value < -100 ? 0.5 : 1
}))

function onMediaError() { mediaFailed.value = true }

function onPointerDown(e: PointerEvent) {
  if (!props.showActions) return
  // Don't start swipe from action buttons
  const target = e.target as HTMLElement
  if (target.closest('.action-buttons')) return
  swiping.value = true
  startX.value = e.clientX
  hasDragged.value = false
  ;(e.currentTarget as HTMLElement).setPointerCapture(e.pointerId)
}

function onPointerMove(e: PointerEvent) {
  if (!swiping.value) return
  const dx = e.clientX - startX.value
  if (Math.abs(dx) > 5) hasDragged.value = true
  translateX.value = Math.min(0, dx) // left only
}

async function onPointerUp() {
  if (!swiping.value) return
  swiping.value = false
  if (translateX.value < -100) {
    await doDiscard()
  } else {
    translateX.value = 0
  }
}

function onCardClick(e: MouseEvent) {
  if (hasDragged.value) return
  // Don't expand if clicking action buttons
  const target = e.target as HTMLElement
  if (target.closest('.action-buttons')) return
  expanded.value = true
}

function flashError() {
  errorFlash.value = true
  setTimeout(() => { errorFlash.value = false }, 1000)
}

async function doDiscard() {
  try {
    const res = await fetch(`/api/memes/${props.meme.id}`, { method: 'DELETE' })
    if (res.ok || res.status === 204) {
      discarding.value = true
      setTimeout(() => emit('discard', props.meme.id), 300)
    } else {
      translateX.value = 0
      flashError()
    }
  } catch {
    translateX.value = 0
    flashError()
  }
}

async function onDiscard() {
  await doDiscard()
}

async function onLike() {
  const prev = localLiked.value
  localLiked.value = !prev
  try {
    const res = await fetch(`/api/memes/${props.meme.id}/like`, { method: 'PATCH' })
    if (!res.ok) {
      localLiked.value = prev
      flashError()
    }
  } catch {
    localLiked.value = prev
    flashError()
  }
}
</script>

<style scoped>
.meme-card {
  background-color: #1a1a2e;
  border: 1px solid #2a2a4a;
  border-radius: 10px;
  overflow: hidden;
  cursor: pointer;
  break-inside: avoid;
  touch-action: pan-y;
  user-select: none;
  transition: border-color 0.2s;
}
.meme-card:hover { border-color: #3a3a5a; }
.meme-card.error-flash { border-color: #e53935; }
.meme-media { width: 100%; display: block; }
.meme-info { padding: 8px 12px 6px; }
.meme-title { font-size: 0.8rem; font-weight: 600; color: #e0e0e0; line-height: 1.3; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.meme-meta { display: flex; align-items: center; gap: 6px; margin-top: 6px; }
.posted-badge { font-size: 0.65rem; font-weight: 600; padding: 2px 6px; border-radius: 10px; text-transform: uppercase; letter-spacing: 0.5px; }
.posted-badge.posted { background-color: #1b4332; color: #52b788; }
.posted-badge.pending { background-color: #2a2a4a; color: #a0a0b8; }
.source-badge { font-size: 0.6rem; font-weight: 500; padding: 2px 5px; border-radius: 8px; background-color: #2a2a4a; color: #8080a0; text-transform: uppercase; letter-spacing: 0.5px; }

.action-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 6px 10px 8px;
  border-top: 1px solid #2a2a4a;
}
.action-btn {
  background: none;
  border: 1px solid #2a2a4a;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  font-size: 0.85rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s, border-color 0.2s;
}
.action-btn:hover { transform: scale(1.15); }
.discard-btn:hover { border-color: #e53935; }
.like-btn:hover { border-color: #bb86fc; }
.like-btn.liked { border-color: #52b788; background-color: rgba(82, 183, 136, 0.15); }

/* Overlay */
.overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.85); z-index: 9999; display: flex; align-items: center; justify-content: center; animation: fadeIn 0.2s ease-out; }
.expanded-card { position: relative; background-color: #1a1a2e; border: 1px solid #2a2a4a; border-radius: 12px; max-width: 600px; max-height: 90vh; width: 90vw; overflow-y: auto; animation: scaleIn 0.2s ease-out; }
.close-btn { position: absolute; top: 12px; right: 12px; background: rgba(0,0,0,0.6); color: #e0e0e0; border: none; border-radius: 50%; width: 32px; height: 32px; font-size: 1.2rem; cursor: pointer; z-index: 1; display: flex; align-items: center; justify-content: center; }
.close-btn:hover { background: rgba(0,0,0,0.8); }
.expanded-media { width: 100%; display: block; border-radius: 12px 12px 0 0; max-height: 70vh; object-fit: contain; background: #0f0f1a; }
.expanded-info { padding: 16px 20px 20px; }
.expanded-info h3 { font-size: 1.1rem; font-weight: 600; color: #e0e0e0; margin-bottom: 10px; line-height: 1.4; }
.expanded-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.expanded-description { font-size: 0.9rem; color: #a0a0b8; line-height: 1.6; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes scaleIn { from { transform: scale(0.9); opacity: 0; } to { transform: scale(1); opacity: 1; } }
</style>
