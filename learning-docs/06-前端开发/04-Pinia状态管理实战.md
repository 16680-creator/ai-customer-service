# 04-Pinia 状态管理实战：给前端补上"状态层"

> 2026-09 落地记录：`ai-cs-frontend` 目前**没有安装** Pinia（package.json 无、无 store/ 目录），登录态散在 `src/utils/auth.js` + localStorage，被 axios 拦截器、路由守卫、各视图三处直接消费。本篇以"不推翻现有代码"为前提，演示怎么把状态层补上——所有改动点都对准项目里的真实文件。

---

## 一、现状盘点：没有 store 的日子

登录态目前的三个消费点：

```text
src/utils/auth.js (38 行)          ← 状态真身：localStorage 两个 key
├── TOKEN_KEY = 'aics_token'
├── USER_KEY  = 'aics_user'
└── 导出 getToken/setToken/getUser/setUser/isAuthenticated/logout 六个纯函数

消费点 1: src/api/index.js         ← 14 个 axios 实例的请求拦截器逐个调 getToken()
消费点 2: src/router/index.js      ← 守卫直接调 isAuthenticated()
消费点 3: 各视图（LoginView 等）   ← 登录后 setToken/setUser，界面上的用户名却要自己再读一次
```

这样能跑，但三个已经显现的代价：

1. **状态不是响应式的**：登录成功后，页头那些早已渲染的组件不会自动知道"现在有用户了"，要么刷新要么手动传值。
2. **无处安放的跨视图状态**：WebSocket 连接状态、通知列表（`NotifyView` 里那组 `wsConnected`/`addLog`）只活在单个组件里，换个视图就没了。
3. **测试与演进受限**：纯函数读 localStorage，全局副作用让单测要频繁 mock storage。

## 二、Pinia 是什么、为什么是它

Pinia 是 Vue 官方推荐的状态库（Vuex 的继任者）：一个 store = **state（数据）+ getters（计算）+ actions（同步/异步方法）**，天然按组合式函数风格书写，TypeScript 友好，devtools 可时间旅行。对比当前"模块级纯函数 + localStorage"方案：

| 维度 | 现状（auth.js） | Pinia |
|---|---|---|
| 响应式 | ❌ | ✅ state 变化驱动所有引用组件 |
| 跨组件共享 | 手动传/再读 | `useXxxStore()` 即取 |
| 持久化 | 手写 localStorage | 手写（或 `pinia-plugin-persistedstate` 插件） |
| 与组合式函数差异 | — | 组合式函数管"单组件内的逻辑复用"，store 管"全局单份的共享状态" |

## 三、安装与注册

```bash
cd ai-cs-frontend && npm install pinia
```

`src/main.js`（现文件 19 行，加两行）：

```js
import { createPinia } from 'pinia'
const app = createApp(App)
app.use(createPinia())        // 必须在 router 之前/之后均可，但要在任何 store 被调用之前
app.use(router)
```

## 四、第一个 store：useAuthStore（平移 auth.js）

新建 `src/stores/auth.js`，把 auth.js 六个函数收编为 state + actions（localStorage 持久化行为保持不变，网关侧无感知）：

```js
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

const TOKEN_KEY = 'aics_token'
const USER_KEY = 'aics_user'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem(TOKEN_KEY) || '')
  const user = ref(JSON.parse(localStorage.getItem(USER_KEY) || 'null'))

  const isAuthenticated = computed(() => !!token.value)

  function setToken(t) { token.value = t; localStorage.setItem(TOKEN_KEY, t) }
  function setUser(u)  { user.value = u; localStorage.setItem(USER_KEY, JSON.stringify(u)) }
  function logout() {
    token.value = ''; user.value = null
    localStorage.removeItem(TOKEN_KEY); localStorage.removeItem(USER_KEY)
  }
  return { token, user, isAuthenticated, setToken, setUser, logout }
})
```

组合式写法要点：`defineStore('auth', () => {...})` 第二参就是 setup 函数；`ref` 即 state、`computed` 即 getter、普通函数即 action；**return 什么，组件里就能用什么**。

## 五、改造三个消费点（含一个真实的坑）

**axios 拦截器**（`src/api/index.js`）：把 `getToken()` 换成 store，但**不要在模块顶层调用** `useAuthStore()`：

```js
// ❌ 顶层调用：main.js 里 createClient 先于 app.use(createPinia()) 执行，直接报
//    "getActivePinia was called with no active Pinia"
// ✅ 在拦截器回调内调用：请求发生时 Pinia 早已就绪
client.interceptors.request.use((config) => {
  const { token } = useAuthStore()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
client.interceptors.response.use(null, (error) => {
  if (error.response?.status === 401) {
    useAuthStore().logout()          // 401 统一登出，状态变化自动驱动 UI
    ElMessage.warning('登录已过期，请重新登录')
    redirectToLogin()
  }
  return Promise.reject(error)
})
```

这个"延迟调用"是**前端接 store 最常见的坑**，原因在模块加载顺序：`api/index.js` 在 import 阶段就创建实例，而 Pinia 在 `main.js` 的 `app.use` 才激活。

**路由守卫**（`src/router/index.js`，现 37-48 行的守卫逻辑保持不变，只换数据源）：

```js
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta?.public !== true && !auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && auth.isAuthenticated) return { path: '/' }
  return true
})
```

**视图层**：`LoginView` 登录成功后 `authStore.setToken/setUser`，页头组件从 `storeToRefs(authStore)` 拿 user——登录后页头用户名**自动出现**，这就是响应式状态的第一手体感。旧 `auth.js` 可保留为 store 内部的实现细节或直接删除（全仓只有上述三类消费点）。

## 六、第二个 store：useNotifyStore（顺手补上重连/心跳）

`NotifyView.vue` 现状：手动按钮 `connectWs()` 连 `ws://localhost:8085/ws/notify`，`onopen/onmessage/onclose` 更新本地 ref——**无自动重连、无心跳**，页面一关状态全丢。把它升级为全局 store，顺带补齐这两个可靠性缺口：

```js
// src/stores/notify.js
export const useNotifyStore = defineStore('notify', () => {
  const connected = ref(false)
  const logs = ref([])
  let ws = null, retry = 0

  function connect() {
    ws = new WebSocket('ws://localhost:8085/ws/notify')
    ws.onopen = () => { connected.value = true; retry = 0 }
    ws.onmessage = (e) => logs.value.push({ at: Date.now(), text: e.data })
    ws.onclose = () => {
      connected.value = false
      if (retry++ < 5) setTimeout(connect, Math.min(1000 * 2 ** retry, 15000))  // 指数退避重连
    }
  }
  function disconnect() { ws?.close(); retry = 5 }   // 主动关闭不再重连
  return { connected, logs, connect, disconnect }
})
```

（更完整的 STOMP 方案——`@stomp/stompjs` + 心跳——见 [02-Spring微服务/11-STOMP实时通知与用户目的地](../02-Spring微服务/11-STOMP实时通知与用户目的地.md)；前端原生 WebSocket 版先用重连 + 指数退避兜底。）

## 七、动手练习

1. 按第四节建 `useAuthStore`，跑通"登录 → 页头自动显示用户名 → 401 自动登出回登录页"全链路。
2. 把 `ChatDashboardView`（echarts 图表，全项目唯一 import echarts 的视图）的图表数据源抽成 `useDashboardStore`，action 里封装请求 + loading 状态。
3. （进阶）引入 `pinia-plugin-persistedstate`，删掉手写 localStorage 三行，对比两种持久化方式的取舍。

## 八、面试要点总结

> 前端原有登录态是模块级纯函数 + localStorage，被 axios 拦截器、路由守卫、视图三处直接消费，缺响应式与跨视图共享；引入 Pinia 后以组合式 store 收编（ref 即 state、computed 即 getter、函数即 action），关键改造点是 axios 拦截器内延迟调用 useAuthStore() 规避"Pinia 未激活"的模块加载时序坑；再以 notify store 演示把页面内 WebSocket 状态升级为全局共享并补上指数退避重连。

```text
关键词：defineStore + setup 写法 · storeToRefs 保持响应性
拦截器顶层调 store = 时序坑 · 401 统一登出 → 状态驱动 UI
组合式函数管局部复用，store 管全局单份 · 持久化 = 手写 localStorage 或 persistedstate 插件
```

## 学习检查清单

- [ ] 能说清现状三个消费点与各自的改造方式
- [ ] 能解释为什么 `useAuthStore()` 不能写在 `api/index.js` 模块顶层
- [ ] 完成练习 1，用 devtools 观察一次完整的 state 时间线
- [ ] 完成 notify store 的指数退避重连
