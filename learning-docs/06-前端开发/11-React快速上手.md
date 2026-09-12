# 11-React 快速上手：从 Vue 心智模型到 React 心智模型

> 前置知识：[04-Vue3核心基础](./04-Vue3核心基础.md)（组合式 API 与响应式原理）、[06-路由守卫与Axios请求封装](./06-路由守卫与Axios请求封装.md)（路由与请求层）、[07-Pinia状态管理实战](./07-Pinia状态管理实战.md)（状态层）、[10-浏览器原理与HTTP跨域](./10-浏览器原理与HTTP跨域.md)（渲染流程）。
> 本篇定位是"概念移植"而非重学前端：把你已建立的 Vue 3 心智模型逐项映射到 React 18，读完能读懂 React 代码、能接手 React 项目。SSR 与 Nuxt 见 [12-Nuxt与SSR入门](./12-Nuxt与SSR入门.md)，移动端跨端见 [13-移动端与小程序开发](./13-移动端与小程序开发.md)。

---

## 一、为什么要会 React

### 1.1 市占率与生态：跳槽面更广

- **市场**：全球使用率第一的前端框架（npm 下载量、State of JS 历年调查均领先），国内大厂（字节、阿里、美团等）核心业务大量使用；招聘市场 React 与 Vue 岗位接近对半，只会 Vue 等于放弃一半面试机会。
- **生态纵深**：React Native（跨端 App）、Next.js（全栈框架）、Taro（React 语法写小程序）、TanStack Query / Zustand / Ant Design 全家桶——学一个 React，喂饱五条产品线。
- **存量资产可复用**：Vite、TypeScript、ESLint、路由与状态分层思想全部平移，唯一要换的是"渲染心智模型"。

### 1.2 核心思想差异一句话

| | Vue 3 | React |
|---|---|---|
| 写法 | 模板（SFC）+ 组合式 API | JSX（函数组件）+ Hooks |
| 更新机制 | 细粒度响应式：`ref` 变了，**只**通知订阅它的组件 | 不可变数据：`setState` 后从该组件起**整棵子树重新执行**，靠 Virtual DOM diff 找差异 |
| 修改数据 | `obj.x = 1` 直接改（Proxy 拦截） | 必须 `setObj({ ...obj, x: 1 })` 造新对象 |
| Java 类比 | 注解驱动的 Spring：框架监听字段变化自动刷新 | Lambda / 纯函数：组件是 `props => UI` 的纯函数，改输入必须传新对象 |

一句话：**Vue 是"改数据，框架自动追着更新"；React 是"我声明新数据，框架整树重算再 diff"。** 理解这句，后面全是语法细节。

## 二、概念映射大表（本篇核心资产）

把 `ai-cs-frontend` 里用过的每个 Vue 概念对应到 React，先总览，后分节展开：

| Vue 3 | React 18 | 说明 |
|---|---|---|
| `.vue` SFC 文件 | `Xxx.jsx` / `Xxx.tsx` 组件文件 | 模板+逻辑+样式合一 → JSX 里全是 JS |
| `ref()` / `reactive()` | `useState()` | React 无响应式代理，只有"值 + setter" |
| `computed()` | `useMemo(() => ..., [deps])` | 缓存计算结果 |
| `watch()` / `watchEffect()` | `useEffect(() => ..., [deps])` | **不完全等价**：effect 跑在渲染之后，用于副作用而非"监听改值" |
| `onMounted()` | `useEffect(fn, [])` | 首次渲染提交后执行 |
| `props` / `emits` | `props` / **回调 props** | React 没有 emit：父传函数下来，子直接调用它 |
| `v-if` / `v-show` | `{cond && ...}` / 三目运算符 | JSX 里条件就是 JS 表达式 |
| `v-for` | `arr.map(x => <li key={x.id} />)` | key 语义完全相同 |
| `v-model` | `value={x}` + `onChange={e => setX(e.target.value)}` | 受控组件组合 |
| `slot`（默认/具名/作用域） | `children` / render props | 把 JSX 当参数传 |
| `provide` / `inject` | `createContext` + `useContext` | 跨层传递 |
| Pinia store | Zustand / Redux Toolkit | 全局状态 |
| Vue Router | React Router 6 | 路由即组件 |
| Element Plus | Ant Design | 企业级组件库 |
| `<router-view />` | `<Outlet />` | 嵌套路由出口 |

记忆原则：**Vue 的指令（v-xxx）在 React 里全部退化为原生 JS 语法**——JSX 本来就是 JS，不需要发明新指令。

## 三、JSX 核心语法

### 3.1 组件与表达式插值

```jsx
// src/components/UserCard.jsx —— 一个组件就是一个函数，返回 JSX
function UserCard({ name, role, onLogout }) {   // props 直接解构 ≈ defineProps
  const upper = name.toUpperCase();             // {} 里可写任意 JS 表达式
  return (
    <div className="user-card">                 {/* 注意：是 className，不是 class */}
      <h3>{upper}</h3>                          {/* 插值 ≈ Vue 的 {{ }} */}
      {role === 'admin' && <span>管理员</span>}  {/* 条件渲染 ≈ v-if */}
      <button onClick={() => onLogout()}>退出</button>  {/* 传函数，不是字符串 */}
    </div>
  );
}
export default UserCard;
```

三个高频坑：`className` 不是 `class`；`onClick` 传**函数**不是调用结果；标签必须闭合（`<img />`）。

### 3.2 列表渲染与 key

```jsx
{users.map(u => (
  <UserRow key={u.id} user={u} />   // key 作用与 Vue 完全一致：diff 时复用节点
))}
```

### 3.3 条件渲染与 Fragment

```jsx
{loading ? <Spinner /> : <Table data={rows} />}   {/* 三目 ≈ v-if / v-else */}
{error && <Alert type="error">{error}</Alert>}    {/* 短路 ≈ v-if */}

return (
  <>                    {/* Fragment ≈ template 包裹，不产生真实 DOM */}
    <Header />
    <Main />
  </>
);
```

### 3.4 样式写法

1. **行内对象**：`style={{ color: 'red', fontSize: 14 }}`——对象字面量，属性驼峰。
2. **全局 class**：`className="btn"` 配 CSS 文件引入，最普通。
3. **CSS Modules**：`import s from './UserCard.module.css'` 后写 `s.card`——编译期 hash 隔离作用域，等价 Vue SFC 的 `<style scoped>`，工程化项目首选。

### 3.5 为什么必须不可变更新（React 的第一性原理）

React 判断"要不要重渲染"只做一件事：**用 `Object.is(newState, oldState)` 比较引用**。引用没变 → 认为"什么都没发生" → 跳过更新。这在 Java 里一脉相承：

- `String` 为什么设计成不可变？为了安全地引用共享与比较；
- `HashSet` 判断同一元素靠 `equals/hashCode`——引用相等是最快的短路判定。

```jsx
// ❌ 错误：push 改的是同一个数组，引用没变，React 认为数据没更新
function addBad(user) {
  users.push(user);        // Vue 里天经地义的原地修改
  setUsers(users);         // Object.is(同引用) → 不重渲染，页面"没反应"
}

// ✅ 正确：造新数组、新对象
function addGood(user) {
  setUsers([...users, user]);                    // 新数组
  setUsers(prev => [...prev, user]);             // 并发安全写法，见 4.1
  setUser({ ...user, name: user.name.trim() });  // 新对象替换属性
}
```

同理：删除用 `filter`，改某一项用 `map`，深层嵌套用 immer 库。**把 React 的 state 当成 Java 的不可变对象（record）来操作**，就永远不会踩坑。

## 四、Hooks 详解

Hooks 让函数组件拥有"状态与生命周期"。使用规则只有两条：只在函数组件**顶层**调用；每次渲染按**相同顺序**调用（不能放进 if/loop——React 靠调用顺序识别每个 Hook，类似数组下标寻址）。

### 4.1 useState：值 + setter

```jsx
// src/pages/UserList.jsx
const [keyword, setKeyword] = useState('');   // ≈ const keyword = ref('')
const [users, setUsers] = useState([]);       // ≈ reactive 数组

// 函数式更新：新值依赖旧值时必用，拿到的一定是最新值
setUsers(prev => [...prev, newUser]);
```

注意 `setCount(count + 1)` 连续调两次只 +1（两次都基于同一快照），必须 `setCount(prev => prev + 1)`——类比并发下的 `i = i + 1` 不可靠，要用 `AtomicInteger.incrementAndGet()` 基于最新值运算。

### 4.2 useEffect：副作用与依赖数组

心智模型：**useEffect 不是"监听器"，而是"渲染之后要做的同步动作"**——发请求、设定时器、订阅、改标题都属于副作用。

| 依赖数组 | 执行时机 | Vue 近似物 |
|---|---|---|
| `useEffect(fn)` | 每次渲染后 | `watchEffect()` |
| `useEffect(fn, [])` | 仅首次渲染后 | `onMounted()` |
| `useEffect(fn, [id])` | 首次 + id 变化后 | `watch(id, fn)` |
| 返回的函数 | 下一次执行前 / 卸载时 | `onUnmounted()` |

清理函数类比 Java：**`try-with-resources` 的 `close()`、Spring Bean 的 `@PreDestroy`**——谁申请的资源（定时器/订阅/WebSocket），谁在销毁前归还。

```jsx
// ❌ 依赖漏写 → 陈旧闭包：定时器永远看到第一帧的 count
useEffect(() => {
  const timer = setInterval(() => console.log(count), 1000);  // 永远打印 0
  return () => clearInterval(timer);
}, []);                        // 缺 count，闭包捕获了首次渲染的旧值

// ✅ 正确：要么把 count 写进依赖（每次重建定时器），要么用函数式更新
useEffect(() => {
  const timer = setInterval(() => setCount(prev => prev + 1), 1000);  // prev 永远最新
  return () => clearInterval(timer);
}, []);
```

### 4.3 useRef：跨渲染的"盒子"

```jsx
const timerId = useRef(null);        // { current: null }，改 current 不触发渲染
timerId.current = setInterval(...);  // ≈ AtomicReference：可变、跨渲染存活、但不通知 UI
```

选择标准：**值要在 UI 上显示 → useState；只是记个"把手"（DOM 节点、定时器 id、上一次的值）→ useRef**。

### 4.4 useMemo / useCallback：缓存

- `useMemo(() => computeHeavy(list), [list])` ≈ `@Cacheable(key = "list")`：依赖不变就返回上次结果，避免重复计算。
- `useCallback(fn, [deps])` ≈ 缓存**函数引用**：避免"每次渲染都生成新函数"，导致 memo 子组件失效或 useEffect 反复执行。

### 4.5 useContext：跨层传递

```jsx
// 1. 创建（≈ provide 声明）
const ThemeContext = createContext('light');
// 2. 顶层包住并给值
<ThemeContext.Provider value="dark"><App /></ThemeContext.Provider>
// 3. 任意后代直接取用（≈ inject）
const theme = useContext(ThemeContext);
```

类比：prop drilling（参数逐层透传）相当于"方法参数层层转发"，Context 相当于 Spring 的 `@Autowired`——从环境里直接拿，中间层不用关心。

### 4.6 自定义 Hook = 逻辑复用（≈ Vue 组合式函数）

同一个 `useRequest`，两框架对照，逻辑完全同构：

```js
// Vue 版：src/composables/useRequest.js
import { ref, onMounted } from 'vue'
export function useRequest(fetcher) {
  const data = ref(null), loading = ref(true), error = ref(null)
  onMounted(async () => {
    try { data.value = await fetcher() }
    catch (e) { error.value = e }
    finally { loading.value = false }
  })
  return { data, loading, error }
}
```

```jsx
// React 版：src/hooks/useRequest.js
import { useState, useEffect } from 'react';
export function useRequest(fetcher) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  useEffect(() => {
    let cancelled = false;                          // 防竞态：慢请求晚到覆盖新请求
    fetcher().then(d => !cancelled && setData(d))
      .catch(e => !cancelled && setError(e))
      .finally(() => !cancelled && setLoading(false));
    return () => { cancelled = true; };             // 清理 ≈ @PreDestroy
  }, []);
  return { data, loading, error };
}
```

差别只在响应式容器（ref vs useState）与生命周期入口（onMounted vs useEffect），**复用思想一模一样**。

## 五、生态速览

### 5.1 Vite 创建项目（和 Vue 同一套工具链）

```bash
npm create vite@latest my-react-app -- --template react   # TS 版用 react-ts
npm install && npm run dev    # dev server / proxy 配置与 Vue 项目写法完全一致
```

### 5.2 React Router 6 要点

```jsx
// src/router/index.jsx —— 路由即组件树
import { createBrowserRouter, RouterProvider, Outlet } from 'react-router-dom';
const Layout = () => (          // ≈ 布局组件 + <router-view />
  <>
    <Sidebar />
    <Outlet />                  {/* 子路由出口 */}
  </>
);
const router = createBrowserRouter([
  {
    path: '/', element: <Layout />,
    children: [
      { path: 'users', element: <UserList /> },          // ≈ children 路由配置
      { path: 'users/:id', element: <UserDetail /> },    // useParams() 取参数
    ],
  },
]);
export default function App() { return <RouterProvider router={router} />; }
```

对照：`createBrowserRouter` ≈ `createRouter` 的 routes；`Outlet` ≈ `<router-view>`；`useNavigate()` ≈ `useRouter().push`；`useParams()` ≈ `useRoute().params`；路由守卫用条件渲染组件实现（`<RequireAuth>` 包一层，见动手练习）。

### 5.3 TanStack Query：服务端状态的正确归宿

Pinia 存接口数据的老问题：**数据会脏**（后端改了本地不知道）、loading/error 要手写、刷新要手动调。TanStack Query 把"服务端状态"当作带缓存策略的资源管理（≈ Spring Cache + TTL + 失效广播）：

```jsx
const { data, isLoading, refetch } = useQuery({
  queryKey: ['users', page],                 // 缓存 key（含查询参数）
  queryFn: () => fetchUsers(page),
});
const mutation = useMutation({
  mutationFn: createUser,                    // 写成功后按 key 失效缓存 → 自动重拉
  onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
});
```

自动缓存、窗口聚焦重取、失败重试、失效重拉——"从接口拿数据"从此不进 useState。

### 5.4 Zustand：三行代码的全局状态

```js
// src/store/useUserStore.js —— ≈ Pinia 的 defineStore
import { create } from 'zustand';
export const useUserStore = create(set => ({
  user: null,
  login: user => set({ user }),              // set ≈ $patch
  logout: () => set({ user: null }),
}));
// 组件里：const { user, login } = useUserStore()，无需 Provider，天然按选择器订阅
```

### 5.5 Next.js 是什么

React 官方背书的全栈框架：文件式路由 + SSR/SSG + API Routes（后端代码也写在同工程）。它之于 React ≈ **Nuxt 之于 Vue**，服务端渲染思想在 [12-Nuxt与SSR入门](./12-Nuxt与SSR入门.md) 统一讲透，两边概念完全互通。

## 六、实战对照：用户管理页（搜索 + 表格 + 分页 + 新增弹窗）

### 6.1 Vue 3 版（伪代码）

```vue
<!-- src/views/UserManage.vue -->
<script setup>
import { ref, onMounted, watch } from 'vue'
const keyword = ref(''); const page = ref(1)
const rows = ref([]); const total = ref(0)
const dialogVisible = ref(false); const form = ref({ name: '', role: 'user' })

async function load() {                      // 搜索与翻页都要靠 watch 触发
  const { data } = await getUsers({ q: keyword.value, page: page.value })
  rows.value = data.list; total.value = data.total
}
onMounted(load)
watch([keyword, page], load)                 // 监听变化重查
async function submit() {
  await createUser(form.value); dialogVisible.value = false; load()
}
</script>
<template>
  <el-input v-model="keyword" placeholder="搜索用户" clearable />
  <el-table :data="rows"> ... </el-table>
  <el-pagination v-model:current-page="page" :total="total" />
  <el-dialog v-model="dialogVisible" title="新增用户">
    <el-form :model="form">
      <el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item>
    </el-form>
    <el-button @click="submit">提交</el-button>
  </el-dialog>
</template>
```

### 6.2 React 版（伪代码）

```jsx
// src/views/UserManage.jsx
import { useState, useEffect } from 'react';
function UserManage() {
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(1);
  const [rows, setRows] = useState([]);
  const [total, setTotal] = useState(0);
  const [dialogOpen, setDialogOpen] = useState(false);   // ≈ dialogVisible
  const [form, setForm] = useState({ name: '', role: 'user' });

  useEffect(() => { load(keyword, page); }, [keyword, page]);  // ≈ onMounted + watch 合体
  async function load(q, p) {
    const { data } = await getUsers({ q, page: p });
    setRows(data.list); setTotal(data.total);            // 不可变更新
  }
  async function submit() {
    await createUser(form); setDialogOpen(false); load(keyword, page);
  }
  return (
    <>
      <Input value={keyword} onChange={e => setKeyword(e.target.value)} placeholder="搜索用户" />
      <Table dataSource={rows} />
      <Pagination current={page} total={total} onChange={setPage} />
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} title="新增用户">
        <Input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
        <Button onClick={submit}>提交</Button>
      </Dialog>
    </>
  );
}
```

### 6.3 对应关系标注

| 片段 | Vue | React |
|---|---|---|
| 搜索词 | `v-model="keyword"` | `value + onChange` 受控组合 |
| 首次加载 + 监听 | `onMounted(load)` + `watch(...)` | 一个 `useEffect(fn, [keyword, page])` |
| 弹窗开关 | `el-dialog v-model` | `open` prop + `onClose` 回调 prop |
| 表单双向 | `v-model="form.name"` | `setForm({ ...form, name })` 整对象替换 |

体感差异：React 的"绑定"更啰嗦，但全是显式 JS、没有指令语法糖；**数据流方向两者一致——状态在上，事件从下用回调传上来**。

## 本章小结

- 心智模型一句话：Vue 改数据自动追踪；React 造新对象整树重渲染 + diff。
- 概念迁移靠第二章映射大表；所有指令退化为原生 JS 语法。
- Hooks 四件套：useState（状态，记得函数式更新）、useEffect（副作用，小心陈旧闭包）、useRef（不触发渲染的盒子）、useMemo/useCallback（缓存计算与函数引用）。
- 生态对位：Zustand ≈ Pinia、React Router ≈ Vue Router、Ant Design ≈ Element Plus、TanStack Query 管"服务端状态"。
- 组件 = props 进、回调 props 出——与 Vue 的 props/emits 同一设计，只是 emit 变成"调用父传下来的函数"。

## 动手练习

1. 用 `npm create vite@latest -- --template react-ts` 建项目，把 `ai-cs-frontend` 的登录页用 React + Ant Design 重写：受控表单 + 调登录接口 + token 存入 Zustand。
2. 给 5.2 的路由表加 `<RequireAuth>`：读取 localStorage 里的 token，未登录渲染 `<Navigate to="/login" />`——对应 Vue 的全局路由守卫。
3. 写一个 `useDebounce(value, delay)` 自定义 Hook（useEffect + 清理函数实现），接到搜索框上，验证输入停顿 500ms 才发起请求。
