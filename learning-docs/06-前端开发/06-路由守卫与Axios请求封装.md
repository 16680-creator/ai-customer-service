# 路由守卫与 Axios 请求封装

> 本项目前端通过 **Vue Router 路由守卫** 做登录拦截，通过 **Axios 拦截器** 统一附加 JWT、处理 401。
> 对应项目文件：`ai-cs-frontend/src/router/index.js`、`ai-cs-frontend/src/api/index.js`、`ai-cs-frontend/src/utils/auth.js`

---

## 一、为什么需要这两层封装？

```
未封装前：
  每个页面都要自己判断"是否登录"
  每个请求都要自己加 Token
  每个 401 都要自己跳登录页
  → 代码重复、易错

封装后：
  路由守卫：统一拦截未登录访问
  Axios 拦截器：统一加 Token、统一处理 401
  → 业务代码只关心数据，不关心鉴权细节
```

---

## 二、Axios 请求封装（api/index.js，本项目实际代码）

```javascript
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, logout } from '../utils/auth'

// 所有请求统一走网关（网关负责鉴权 + 路由转发到各微服务）
const GATEWAY = import.meta.env.VITE_GATEWAY || 'http://localhost:8080'
const BASE_URL = `${GATEWAY}/api`

// 各服务的 API 前缀（网关 stripPrefix(1) 后对应各服务 Controller 路径）
const SERVICES = {
  user: `${BASE_URL}/user`,
  chat: `${BASE_URL}/chat`,
  knowledge: `${BASE_URL}/knowledge`,
  message: `${BASE_URL}/message`,
  notify: `${BASE_URL}/notify`,
  search: `${BASE_URL}/search`,
  order: `${BASE_URL}/order`,
  cart: `${BASE_URL}/cart`,
  product: `${BASE_URL}/product`,
}

function redirectToLogin() {
  if (window.location.pathname !== '/login') {
    window.location.href = '/login'
  }
}

function createClient(baseURL) {
  const client = axios.create({ baseURL, timeout: 30000 })

  // ===== 请求拦截器：自动附加 JWT Token =====
  client.interceptors.request.use((config) => {
    const token = getToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  })

  // ===== 响应拦截器：401 未认证 -> 清除登录态并跳转登录页 =====
  client.interceptors.response.use(
    (response) => response,
    (error) => {
      if (error.response && error.response.status === 401) {
        logout()
        ElMessage.warning(error.response.data?.message || '登录已过期，请重新登录')
        redirectToLogin()
      }
      return Promise.reject(error)
    }
  )

  return client
}

// 为每个服务创建独立的 Axios 实例
export const userApi = createClient(SERVICES.user)
export const chatApi = createClient(SERVICES.chat)
export const knowledgeApi = createClient(SERVICES.knowledge)
export const messageApi = createClient(SERVICES.message)
export const notifyApi = createClient(SERVICES.notify)
export const searchApi = createClient(SERVICES.search)
export const orderApi = createClient(SERVICES.order)
export const cartApi = createClient(SERVICES.cart)
export const productApi = createClient(SERVICES.product)

export default SERVICES
```

### 为什么按服务拆分实例？

```
每个服务对应一个 Axios 实例（baseURL 不同）
好处：不同服务可以有不同的超时、单独拦截器
坏处：配置重复
```

### Token 管理工具（utils/auth.js）

```javascript
// 从 localStorage 读取/写入/清除 Token
const TOKEN_KEY = 'aics_token'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function logout() {
  localStorage.removeItem(TOKEN_KEY)
  // 可加：清空用户信息、跳登录页等
}
```

---

## 三、路由守卫（router/index.js，本项目实际代码）

```javascript
import { createRouter, createWebHistory } from 'vue-router'
import { isAuthenticated } from '../utils/auth'

const routes = [
  // meta.public: true 表示公开页面（无需登录）
  { path: '/login', name: 'Login', component: () => import('../views/LoginView.vue'), meta: { public: true } },
  { path: '/', name: 'Dashboard', component: () => import('../views/Dashboard.vue') },
  { path: '/chat', name: 'Chat', component: () => import('../views/ChatView.vue') },
  { path: '/user', name: 'User', component: () => import('../views/UserView.vue') },
  // ... 其余页面默认需要登录
]

const router = createRouter({
  history: createWebHistory(),   // HTML5 History 模式（无 # 号）
  routes,
})

// ===== 全局前置守卫 =====
router.beforeEach((to) => {
  const isPublic = to.meta?.public === true

  // 1. 访问需要登录的页面，但未登录 -> 跳登录页，并记录来源
  if (!isPublic && !isAuthenticated()) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  // 2. 已登录访问登录页 -> 直接进首页
  if (to.path === '/login' && isAuthenticated()) {
    return { path: '/' }
  }

  return true   // 放行
})

export default router
```

### 守卫逻辑流程图

```
用户访问 /chat
     │
     ▼
to.meta.public? ──是──▶ 放行
     │否
     ▼
已登录? ──是──▶ 放行
     │否
     ▼
跳转 /login?redirect=/chat  （登录后能回到原页面）
```

### `redirect` 实现登录后回跳

```javascript
// LoginView.vue 登录成功后
const { redirect } = useRoute().query
router.replace(redirect || '/')   // 优先回跳原页面
```

---

## 四、isAuthenticated 判定

```javascript
// utils/auth.js
export function isAuthenticated() {
  return !!getToken()   // 简单判断：有 Token 即认为已登录
}
```

> 进阶：可结合 Token 过期时间判断，避免 Token 已过期还放行（最终会被 Axios 的 401 拦截兜底）。

---

## 五、前后端配合的完整鉴权链路

```
① 前端登录 → 后端返回 Token → 存 localStorage
② 路由守卫：未登录访问受保护页面 → 跳登录页
③ 页面发请求 → Axios 请求拦截器自动加 Authorization: Bearer <token>
④ 网关 AuthFilter 校验 Token → 校验通过放行，透传 userId
⑤ 后端返回 401 → Axios 响应拦截器 → 清除登录态 → 跳登录页
```

---

## 六、动手练习

1. 给路由数组新增一个 `meta: { roles: ['admin'] }` 页面，在守卫里加角色校验
2. 在 Axios 请求拦截器里给请求加一个 `X-Request-Id` 头（用于链路追踪）
3. 模拟 Token 过期，观察 401 时页面是否自动跳转登录页
4. 实现登录成功后按 `redirect` 参数回跳

---

## 学习检查清单

- [ ] 理解路由守卫的 `beforeEach` 和 `meta` 机制
- [ ] 理解 `redirect` 参数实现登录回跳
- [ ] 会在 Axios 请求拦截器里附加 Token
- [ ] 会在 Axios 响应拦截器里统一处理 401
- [ ] 理解按服务拆分 Axios 实例的原因
- [ ] 理解前端 + 网关 + 后端的完整鉴权链路

---

## 下一步

→ [09-安全与设计模式/01-JWT鉴权与异常处理](../09-安全与设计模式/01-JWT鉴权与异常处理.md)