import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Home', component: () => import('./views/HomeFeed.vue') },
  { path: '/ingestion', name: 'Ingestion', component: () => import('./views/IngestionFeed.vue') },
  { path: '/admin', name: 'Admin', component: () => import('./views/AdminPanel.vue') },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

export default router
