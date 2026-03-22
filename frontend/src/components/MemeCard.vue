<template>
  <div class="meme-card" @click="expanded = true">
    <h3 class="meme-title">{{ meme.title }}</h3>
    <img
      :src="imageSrc"
      :alt="imageAlt"
      class="meme-image"
      @error="onImageError"
    />
    <div class="meme-meta">
      <span class="dankness-score">🔥 {{ meme.danknessScore.toFixed(1) }}</span>
      <span v-if="!hideStatus" :class="['posted-badge', meme.posted ? 'posted' : 'pending']">
        {{ meme.posted ? 'Posted' : 'Pending' }}
      </span>
    </div>
    <p v-if="truncatedDescription" class="meme-description">{{ truncatedDescription }}</p>

    <!-- Expanded overlay -->
    <Teleport to="body">
      <div v-if="expanded" class="overlay" @click.self="expanded = false">
        <div class="expanded-card">
          <button class="close-btn" @click.stop="expanded = false" aria-label="Close">&times;</button>
          <img :src="imageSrc" :alt="imageAlt" class="expanded-image" />
          <div class="expanded-info">
            <h3>{{ meme.title }}</h3>
            <div class="expanded-meta">
              <span class="dankness-score">🔥 {{ meme.danknessScore.toFixed(1) }}</span>
              <span v-if="!hideStatus" :class="['posted-badge', meme.posted ? 'posted' : 'pending']">
                {{ meme.posted ? 'Posted' : 'Pending' }}
              </span>
            </div>
            <p v-if="meme.description" class="expanded-description">{{ meme.description }}</p>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { Meme } from '../types'

const props = withDefaults(defineProps<{ meme: Meme; hideStatus?: boolean }>(), {
  hideStatus: false
})
const expanded = ref(false)
const imageFailed = ref(false)

const imageSrc = computed(() =>
  imageFailed.value
    ? 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="300" height="200"%3E%3Crect fill="%231a1a2e" width="300" height="200"/%3E%3Ctext x="50%25" y="50%25" dominant-baseline="middle" text-anchor="middle" fill="%23a0a0b8" font-size="14"%3EImage unavailable%3C/text%3E%3C/svg%3E'
    : props.meme.imageUrl
)

const imageAlt = computed(() =>
  imageFailed.value ? 'Meme image unavailable' : props.meme.title
)

function onImageError() {
  imageFailed.value = true
}

const truncatedDescription = computed(() => {
  const desc = props.meme.description
  if (!desc) return ''
  return desc.length > 140 ? desc.slice(0, 140) + '...' : desc
})
</script>

<style scoped>
.meme-card {
  background-color: #1a1a2e;
  border: 1px solid #2a2a4a;
  border-radius: 10px;
  overflow: hidden;
  transition: border-color 0.2s;
  cursor: pointer;
}

.meme-card:hover {
  border-color: #3a3a5a;
}

.meme-title {
  padding: 14px 16px 0;
  font-size: 1rem;
  font-weight: 600;
  color: #e0e0e0;
  line-height: 1.4;
}

.meme-image {
  width: 100%;
  display: block;
  margin-top: 10px;
  max-height: 400px;
  object-fit: cover;
}

.meme-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
}

.dankness-score {
  font-size: 0.9rem;
  color: #f0a040;
  font-weight: 600;
}

.posted-badge {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.posted-badge.posted {
  background-color: #1b4332;
  color: #52b788;
}

.posted-badge.pending {
  background-color: #2a2a4a;
  color: #a0a0b8;
}

.meme-description {
  padding: 0 16px 14px;
  font-size: 0.85rem;
  color: #a0a0b8;
  line-height: 1.5;
}

/* Overlay */
.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.2s ease-out;
}

.expanded-card {
  position: relative;
  background-color: #1a1a2e;
  border: 1px solid #2a2a4a;
  border-radius: 12px;
  max-width: 600px;
  max-height: 90vh;
  width: 90vw;
  overflow-y: auto;
  animation: scaleIn 0.2s ease-out;
}

.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  background: rgba(0, 0, 0, 0.6);
  color: #e0e0e0;
  border: none;
  border-radius: 50%;
  width: 32px;
  height: 32px;
  font-size: 1.2rem;
  cursor: pointer;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s;
}

.close-btn:hover {
  background: rgba(0, 0, 0, 0.8);
}

.expanded-image {
  width: 100%;
  display: block;
  border-radius: 12px 12px 0 0;
  max-height: 70vh;
  object-fit: contain;
  background: #0f0f1a;
}

.expanded-info {
  padding: 16px 20px 20px;
}

.expanded-info h3 {
  font-size: 1.1rem;
  font-weight: 600;
  color: #e0e0e0;
  margin-bottom: 10px;
  line-height: 1.4;
}

.expanded-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.expanded-description {
  font-size: 0.9rem;
  color: #a0a0b8;
  line-height: 1.6;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes scaleIn {
  from { transform: scale(0.9); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
</style>
