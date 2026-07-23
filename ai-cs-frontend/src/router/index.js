import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },
  { path: '/chat', name: 'Chat', component: () => import('../views/ChatView.vue') },
  { path: '/user', name: 'User', component: () => import('../views/UserView.vue') },
  { path: '/knowledge', name: 'Knowledge', component: () => import('../views/KnowledgeView.vue') },
  { path: '/message', name: 'Message', component: () => import('../views/MessageView.vue') },
  { path: '/notify', name: 'Notify', component: () => import('../views/NotifyView.vue') },
  { path: '/search', name: 'Search', component: () => import('../views/SearchView.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
