import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Home', component: () => import('./views/HomeFeed.vue') },
  { path: '/ingestion', name: 'Ingestion', component: () => import('./views/IngestionFeed.vue') },
  { path: '/admin', name: 'Admin', component: () => import('./views/AdminPanel.vue') },
  { path: '/docs/business', name: 'BusinessDocs', component: () => import('./views/BusinessDocsView.vue') },
  { path: '/docs/services', name: 'ServicesDocs', component: () => import('./views/ServicesDocsView.vue') },
  { path: '/docs/communication', name: 'CommunicationDocs', component: () => import('./views/CommunicationDocsView.vue') },
  { path: '/docs/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

export default router
