<template>
  <nav class="navbar">
    <router-link to="/" class="navbar-brand">🔥 DankPoster</router-link>
    <div class="navbar-links">
      <router-link to="/" exact-active-class="router-link-exact-active">Home</router-link>
      <router-link to="/ingestion">Ingestion</router-link>
      <router-link to="/admin">Admin</router-link>
    </div>
    <button class="burger-btn" @click.stop="toggleMenu" aria-label="Documentation menu">
      <svg width="22" height="18" viewBox="0 0 22 18" fill="none" xmlns="http://www.w3.org/2000/svg">
        <rect y="0" width="22" height="2" rx="1" fill="currentColor" />
        <rect y="8" width="22" height="2" rx="1" fill="currentColor" />
        <rect y="16" width="22" height="2" rx="1" fill="currentColor" />
      </svg>
    </button>
    <div v-if="menuOpen" class="docs-menu" @click.stop>
      <router-link to="/docs/business" @click="closeMenu">Business Model &amp; Design</router-link>
      <router-link to="/docs/services" @click="closeMenu">Services</router-link>
      <router-link to="/docs/communication" @click="closeMenu">Communication</router-link>
    </div>
  </nav>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'

const menuOpen = ref(false)

function toggleMenu() {
  menuOpen.value = !menuOpen.value
}

function closeMenu() {
  menuOpen.value = false
}

function handleClickOutside(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.docs-menu') && !target.closest('.burger-btn')) {
    closeMenu()
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') {
    closeMenu()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  document.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  document.removeEventListener('keydown', handleKeydown)
})
</script>

<style scoped>
.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px;
  background-color: #1a1a2e;
  border-bottom: 1px solid #2a2a4a;
  position: relative;
  z-index: 10000;
}

.navbar-brand {
  font-size: 1.25rem;
  font-weight: 700;
  color: #e0e0e0;
  letter-spacing: 0.5px;
  text-decoration: none;
  transition: color 0.2s;
}

.navbar-brand:hover {
  color: #bb86fc;
}

.navbar-links {
  display: flex;
  gap: 4px;
}

.navbar-links a {
  color: #a0a0b8;
  text-decoration: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 0.9rem;
  font-weight: 500;
  transition: color 0.2s, background-color 0.2s;
}

.navbar-links a:hover {
  color: #e0e0e0;
  background-color: #2a2a4a;
}

.navbar-links a.router-link-active {
  color: #bb86fc;
  background-color: #2a2a4a;
}

.navbar-links a.router-link-exact-active {
  color: #bb86fc;
  background-color: #2a2a4a;
}

.burger-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  cursor: pointer;
  color: #e0e0e0;
  padding: 8px;
  border-radius: 6px;
  transition: color 0.2s, background-color 0.2s;
  margin-left: 8px;
}

.burger-btn:hover {
  color: #bb86fc;
  background-color: #2a2a4a;
}

.docs-menu {
  position: absolute;
  top: 100%;
  right: 24px;
  background-color: #1a1a2e;
  border: 1px solid #2a2a4a;
  border-radius: 8px;
  padding: 6px 0;
  min-width: 200px;
  z-index: 10001;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
}

.docs-menu a {
  display: block;
  padding: 10px 16px;
  color: #e0e0e0;
  text-decoration: none;
  font-size: 0.9rem;
  font-weight: 500;
  transition: color 0.2s, background-color 0.2s;
}

.docs-menu a:hover {
  color: #bb86fc;
  background-color: #2a2a4a;
}
</style>
