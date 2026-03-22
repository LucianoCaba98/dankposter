<template>
  <nav class="navbar">
    <router-link to="/" class="navbar-brand">🔥 DankPoster</router-link>
    <div class="navbar-links">
      <router-link to="/" exact-active-class="router-link-exact-active">Home</router-link>
      <router-link to="/ingestion">Ingestion</router-link>
      <router-link to="/admin">Admin</router-link>
    </div>
  </nav>
</template>

<script setup lang="ts">
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
</style>
