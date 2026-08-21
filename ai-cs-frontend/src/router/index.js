import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '../utils/auth'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
  { path: '/', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },
  { path: '/chat', name: 'Chat', component: () => import('../views/ChatView.vue') },
  { path: '/user', name: 'User', component: () => import('../views/UserView.vue') },
  { path: '/knowledge', name: 'Knowledge', component: () => import('../views/KnowledgeView.vue') },
  { path: '/message', name: 'Message', component: () => import('../views/MessageView.vue') },
  { path: '/notify', name: 'Notify', component: () => import('../views/NotifyView.vue') },
  { path: '/search', name: 'Search', component: () => import('../views/SearchView.vue') },
  { path: '/shop', name: 'Shop', component: () => import('../views/ShopView.vue') },
  { path: '/mq', name: 'Mq', component: () => import('../views/MqView.vue') },
  { path: '/product', name: 'Product', component: () => import('../views/ProductView.vue') },
  { path: '/cart', name: 'Cart', component: () => import('../views/CartView.vue') },
  { path: '/checkout', name: 'Checkout', component: () => import('../views/CheckoutView.vue') },
  { path: '/order', name: 'Order', component: () => import('../views/OrderView.vue') },
  { path: '/order/:orderNo', name: 'OrderDetail', component: () => import('../views/OrderDetailView.vue') },
  { path: '/mock-pay', name: 'MockPay', component: () => import('../views/MockCashierView.vue') },
  { path: '/rag-kb', name: 'VectorKb', component: () => import('../views/VectorKbView.vue') },
  { path: '/chat-dashboard', name: 'ChatDashboard', component: () => import('../views/ChatDashboardView.vue') },
  { path: '/knowledge-ops', name: 'KnowledgeOps', component: () => import('../views/KnowledgeOpsView.vue') },
  { path: '/feedback', name: 'Feedback', component: () => import('../views/ChatView.vue') },
  { path: '/trace', name: 'Trace', component: () => import('../views/TraceView.vue') },
  { path: '/prompts', name: 'Prompts', component: () => import('../views/PromptView.vue') },
  { path: '/agent', name: 'Agent', component: () => import('../views/AgentView.vue') },
  { path: '/graph', name: 'Graph', component: () => import('../views/GraphView.vue') },
  { path: '/rag-eval', name: 'RagEval', component: () => import('../views/RagEvalView.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 全局路由守卫：未登录用户只能访问公开页面（登录页），其余一律跳转登录页
router.beforeEach((to) => {
  const isPublic = to.meta?.public === true
  if (!isPublic && !isAuthenticated()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  // 已登录访问登录页，直接进首页
  if (to.path === '/login' && isAuthenticated()) {
    return { path: '/' }
  }
  return true
})

export default router
