# TypeScript 快速上手 —— 给 JS 装上"编译器兜底"

> **前置知识**：[02-JavaScript核心基础](./02-JavaScript核心基础.md)（异步与闭包必须先吃透）。
> **定位**：后端转前端，这一篇是你**安全感的来源**——把 JS 从"运行时才炸"拉回你熟悉的"编译期报错"世界。
> **项目背景**：`ai-cs-frontend` 目前纯 JS（无 tsconfig），本篇最后给出 allowJs 渐进迁移方案，目标是 api 层与 stores 先行 TS 化。

---

## 一、为什么后端转型必须学 TS

### 1.1 纯 JS 的痛，后端人最先感到

```javascript
// ❌ 纯 JS 的日常：这段代码没有 IDE 报错，项目照样构建通过
const user = await getUserById(id)     // 以为返回 { id, name, role }
console.log(user.userName)             // 手滑写了 userName → 运行时打印 undefined
const order = createOrder({ skuId, qty: '-1' })  // qty 传了字符串，接口 500 才发现
```

用 Java 的眼光看，纯 JS 项目相当于：

| 纯 JS 写法 | 等价的 Java 灾难 |
|---|---|
| 到处传普通对象 | 方法签名全是 `Map<String, Object>`，参数全靠猜 |
| 字段名手写 | 没有 IDE 类型提示，重构字段全靠全局搜索 |
| 接口返回值无类型 | 所有远程调用返回 `Object`，用之前先 `instanceof` |
| 没有编译检查 | 改了个字段名，编译通过，上线炸了 |

**TypeScript（TS）= JavaScript + 静态类型系统**。它不改变 JS 运行行为，只在"编译期"做全量类型检查——相当于给 JS 装上了 `javac`。

### 1.2 TS 与 JS 的关系，以及和 Java 的两个重要差异

```text
TS 源码 ──(tsc / Vite 编译)──> 纯 JS ──> 浏览器执行
              ↑
   类型标注在这一步被"擦除"
```

- **类型擦除**：TS 编译成 JS 后，所有类型标注消失。听起来像 Java 泛型擦除？类似，但检查强度天差地别——TS 在编译期把所有类型错误都拦住，Java 泛型擦除后运行时也能靠 ClassCastException 兜底。TS 检查更"全"（不限泛型）。
- **结构化类型（Duck Typing）**：这是和 Java **名义类型**最大的思维差异！

```typescript
// Java：类型匹配看"声明"（名义类型）——没有 implements就不算
// TS：类型匹配看"形状"（结构类型）——字段对上就行

interface Point { x: number; y: number }
const p = { x: 1, y: 2, z: 3 }   // 注意：多了个 z
const point: Point = p            // ✅ 合法！p 的形状"至少"满足 Point
// 类比：Java 里要求"是 IS-A 关系"，TS 只要求"长得像"
```

> ⚠️ 后端直觉迁移提示：TS 的 `interface` 描述"形状"，没有"实现"概念；没有 `extends class` 的强绑定；`private` 也不影响结构判断（`private` 字段除外）。把它当成**可复用的类型声明工具**，而不是 Java 的 interface。

---

## 二、基础类型标注

### 2.1 变量与函数

```typescript
// 基本类型：冒号后面是类型标注，类似 Java 的变量声明放右边
let count: number = 0
let name: string = '张三'
let ok: boolean = true
let ids: number[] = [1, 2, 3]          // 数组
let pair: [string, number] = ['age', 18]  // 元组（Java 没有内置对应物）

// 函数：参数和返回值都标注
function sum(a: number, b: number): number {
  return a + b
}

// 箭头函数同理（02 篇讲过箭头函数）
const fetchUser = async (id: number): Promise<User> => {
  const resp = await request.get<User>(`/api/users/${id}`)
  return resp
}
```

### 2.2 联合类型与字面量类型 —— TS 的招牌能力

```typescript
// union：值可以是几种类型之一 ≈ Java 的 @Nullable / 重载，但更强
let msg: string | null = null        // 显式声明可空，比 JS 的 undefined 猜谜强太多

// 字面量类型：把"值"本身当类型 ≈ 更灵活的枚举
type Status = 'idle' | 'loading' | 'success' | 'error'
let status: Status = 'idle'
// status = 'loaded'   // ❌ 编译期直接报错，运行前就拦截
```

| TS | Java 对应物 |
|---|---|
| `string \| null` | `@Nullable String`（但 TS 会**强制**你先判空才能用） |
| `'a' \| 'b' \| 'c'` 字面量联合 | `enum`（且可以用于任意原始值） |
| `Record<string, number>` | `Map<String, Integer>` |
| `unknown` | 待收窄的 `Object` |
| `never` | 底类型（不可能的分支，用于穷尽性检查） |

### 2.3 interface vs type —— 怎么选

```typescript
// interface：描述对象形状（可被 extends / declaration merging）
interface User {
  id: number
  name: string
  role?: Role            // ? 表示可选字段 ≈ 可空字段
  readonly createdAt: string   // 只读 ≈ final
}

// type：别名，能表达 interface 表达不了的东西（联合、元组、工具类型计算）
type ID = number | string
type StatusMap = Record<Status, string>
type UserOrError = User | ApiError
```

| 维度 | interface | type |
|---|---|---|
| 描述对象/函数形状 | ✅ | ✅ |
| 联合/元组/映射类型 | ❌ | ✅ |
| extends 继承 | ✅（接口继承） | ✅（交叉类型 &） |
| 同名自动合并 | ✅（declaration merging，慎用） | ❌ 报重复 |
| **推荐** | 对象形状、公共 API | 联合类型、别名、计算类型 |

> ✅ **后端习惯迁移**：把每个后端 DTO 都在前端建一个对应的 `interface`，放在 `src/types/` 下。这一个习惯能消掉 80% 的联调事故。

---

## 三、类型收窄与守卫

TS 的类型检查是**流敏感**的：`if` 判断之后，编译器自动收窄类型。这是它和 Java 注解式判空（@Nullable 只是文档）的本质区别。

```typescript
function render(msg: string | null) {
  // console.log(msg.length)      // ❌ 编译报错：msg 可能是 null
  if (msg === null) return
  console.log(msg.length)         // ✅ 收窄后合法，和 Java 17 pattern matching 一样舒服
}
```

### 3.1 可辨识联合（Discriminated Union）—— 本篇最重要的一节

这正好和 Java 17+ 的 **sealed interface + switch 模式匹配**同构，后端人一秒懂：

```typescript
// TS 版：请求状态机
type RequestState =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: User[] }      // success 才有 data
  | { status: 'error'; message: string }     // error 才有 message

function view(state: RequestState): string {
  switch (state.status) {          // 用公共字面量字段"辨识"
    case 'idle':      return '请发起请求'
    case 'loading':   return '加载中…'
    case 'success':   return `共 ${state.data.length} 条`   // ✅ 这里才有 data，编译器保证
    case 'error':     return state.message
  }
}
```

```java
// Java 17+ 等价实现（对照记忆）
sealed interface RequestState permits Idle, Loading, Success, Error {}
record Idle() implements RequestState {}
record Loading() implements RequestState {}
record Success(List<User> data) implements RequestState {}
record Error(String message) implements RequestState {}

String view(RequestState state) {
    return switch (state) {
        case Idle i      -> "请发起请求";
        case Loading l   -> "加载中…";
        case Success s   -> "共 " + s.data().size() + " 条";
        case Error e     -> e.message();
    };
}
```

**区别**：Java 靠 `sealed + record` 类体系辨识；TS 靠一个字面量字段（约定叫 `status`/`kind`/`type`）辨识。后端返回体里的 `code + message + data` 结构天然适合建模成可辨识联合。

---

## 四、泛型

和 Java 泛型几乎同构，写法上尖括号一样，约束用 `extends`：

```typescript
// 函数泛型
function first<T>(arr: T[]): T | undefined { return arr[0] }

// 约束：T 必须有 id 字段 ≈ <T extends HasId>
interface HasId { id: number }
function findById<T extends HasId>(list: T[], id: number): T | undefined {
  return list.find(item => item.id === id)
}

// 接口泛型：对接后端统一返回体（重点！第六节实战展开）
interface Result<T> {
  code: number
  message: string
  data: T
}
```

### 4.1 常用工具类型（内置"泛型方法库"）

| 工具类型 | 作用 | 使用场景 |
|---|---|---|
| `Partial<T>` | 所有字段变可选 | **表单/局部更新**：`Partial<User>` 作为 update 接口入参，只传变更字段 |
| `Required<T>` | 所有字段变必填 | 校验完整性 |
| `Pick<T, K>` | 挑选部分字段 | 列表页只要 `Pick<User, 'id' \| 'name'>` |
| `Omit<T, K>` | 排除部分字段 | 新增时不要 id：`Omit<User, 'id' \| 'createdAt'>` |
| `Record<K, V>` | 构造键值映射 | `Record<Role, string>` 枚举 → 中文文案映射表 |
| `ReturnType<F>` | 取函数返回类型 | 复用他人函数的返回结构，不想手写一遍 |

```typescript
// 典型实战：一套 User 派生四种类型，后端 DTO 一处定义处处使用
interface User { id: number; name: string; password: string; createdAt: string }
type UserVO      = Omit<User, 'password'>                  // 列表展示
type UserCreate  = Omit<User, 'id' | 'createdAt'>          // 新增入参
type UserUpdate  = Partial<Omit<User, 'id' | 'createdAt'>> // 局部更新
const roleLabel: Record<string, string> = { ADMIN: '管理员', USER: '用户' }
```

> 这套"派生"能力是 Java 望尘莫及的（Java 里你得写 UserCreateDTO/UserUpdateDTO/UserVO 三个类）——**TS 里 DTO 一处定义，VO 按需投影**。

---

## 五、any / unknown / never / void

| 类型 | 含义 | Java 类比 | 什么时候用 |
|---|---|---|---|
| `any` | 关闭检查，什么都不查 | `Object` + 强转全免（危险） | ❌ 尽量不用；迁移过渡期临时用 |
| `unknown` | 安全版 any：用之前**必须收窄** | 待判型的 `Object` | ✅ 反序列化/第三方库返回值的默认选择 |
| `never` | 不可能有值 | 底类型 | switch 穷尽性检查、抛错函数 |
| `void` | 无返回值 | `void` | 事件处理函数 |

```typescript
// any vs unknown 的区别（面试高频）
const a: any = JSON.parse(raw)
console.log(a.foo.bar)          // ❌ 不检查，运行时炸

const u: unknown = JSON.parse(raw)
// console.log(u.foo)           // ❌ 编译报错，逼你先收窄
if (typeof u === 'object' && u !== null && 'foo' in u) {
  // 收窄后才能用
}
```

**类型断言 `as`**：你比编译器懂的时候用，但它是"豁免检查"，慎用：

```typescript
const el = document.getElementById('app') as HTMLDivElement  // ✅ 合理：DOM 场景你确定
const user = resp.data as User                                // ⚠️ 建议换成"校验函数"再断言
```

---

## 六、实战：三个立即可用的场景

### 6.1 给 axios 包一层泛型，对接后端统一返回体

后端返回 `R<T>`（`{ code, message, data }`），前端在拦截器里剥壳、泛型向上传递：

```typescript
// src/api/request.ts
import axios from 'axios'

interface Result<T> {          // 对应后端 R<T>
  code: number
  message: string
  data: T
}

const request = axios.create({ baseURL: '/api', timeout: 15000 })

request.interceptors.response.use((resp) => {
  const body = resp.data as Result<unknown>
  if (body.code !== 200) {
    ElMessage.error(body.message)
    return Promise.reject(new Error(body.message))
  }
  resp.data = body.data        // 拦截器剥壳：调用方直接拿到 data
  return resp
})

// 关键：泛型方法 —— 调用方声明"这次接口返回什么"，IDE 全程提示
export function get<T>(url: string, params?: object): Promise<T> {
  return request.get(url, { params })
}
```

```typescript
// src/api/user.ts —— 每个 API 一行，类型即文档
import { get, post } from './request'
import type { User, UserCreate, PageResult } from '@/types/user'

export const listUsers = (params: { page: number; size: number; keyword?: string }) =>
  get<PageResult<User>>('/users', params)

export const createUser = (data: UserCreate) => post<void>('/users', data)
```

调用处体验（对比纯 JS）：

```typescript
const page = await listUsers({ page: 1, size: 10 })
page.records.forEach(u => console.log(u.name))   // ✅ IDE 自动补全 records/User 字段
// page.recordz                                  // ❌ 拼错立即红线
```

### 6.2 Vue 3 组件的 TS 写法

```vue
<!-- UserForm.vue -->
<script setup lang="ts">
import type { User } from '@/types/user'

// props 类型化（编译宏，不需要 import）
const props = defineProps<{
  user?: User                 // 可选
  mode: 'create' | 'edit'     // 字面量联合：mode 只能是这两个值
}>()

// 带默认值用 withDefaults
const withDefaults2 = defineProps<{
  size?: 'small' | 'large'
}>(), { size: 'small' }

// emits 类型化：事件名 + 载荷签名
const emit = defineEmits<{
  (e: 'saved', user: User): void
  (e: 'cancel'): void
}>()

// 响应式数据
const form = ref<Partial<User>>({ ...props.user })
</script>
```

### 6.3 本项目渐进迁移方案（allowJs 混跑）

不需要一口气改完。四步走：

```jsonc
// ai-cs-frontend/tsconfig.json —— 混跑期配置
{
  "compilerOptions": {
    "target": "ES2020",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,            // 全开严格模式：新代码直接高质量
    "allowJs": true,           // ✅ 允许 .js 与 .ts 共存（迁移核心开关）
    "checkJs": false,          // 暂不检查旧 .js 文件，避免一次性红海
    "noEmit": true,            // 类型检查交给 vue-tsc，编译由 Vite 做
    "baseUrl": ".",
    "paths": { "@/*": ["src/*"] },
    "types": ["vite/client", "element-plus/global"]
  },
  "include": ["src/**/*.ts", "src/**/*.d.ts", "src/**/*.vue"]
}
```

1. **装依赖**：`npm i -D typescript vue-tsc`；在 `package.json` 加 `"type-check": "vue-tsc --noEmit"`。
2. **建 `src/types/`**：先为 User、Message 等核心实体写 `interface`，对应后端 DTO。
3. **新代码全 TS**：api 层（`src/api/`）与 stores（`src/stores/`）优先改 `.ts`——它们是类型收益最大、改动最小的地方。
4. **旧 .vue 不动**：`<script setup>` 逐步加 `lang="ts"`，跑通一屏再迁下一屏。

> 📌 配合 [09-前端工程化：Vite构建与规范](./09-前端工程化：Vite构建与规范.md) 把 `vue-tsc` 挂进 lint 流水线，类型检查就是你的"提交门禁"。

---

## 本章小结

- TS = JS + 静态类型，编译期全量检查、运行前擦除——给 JS 装上 `javac`。
- 结构化类型看"形状"不看"血统"，和 Java 名义类型是最大思维差异。
- 收窄是 TS 的灵魂：判空/typeof/switch 字面量后自动缩类型；**可辨识联合 ≈ Java 17 sealed + 模式匹配**。
- 工具类型 `Partial/Pick/Omit/Record` 让 DTO 一处定义、四处投影，替代 Java 手写多个 DTO。
- `any` 是逃逸舱（少用），`unknown` 是安全舱（收窄后用）。
- 落地三板斧：`Result<T>` 泛型封装 axios、组件 `defineProps<T>` 类型化、allowJs 渐进迁移。

## 动手练习

1. 给本项目的 `Message`（消息：id、conversationId、role、content、createdAt）建模 `interface`，并用工具类型派生出 `MessageCreate` 和 `MessageVO`。
2. 把 `request.ts` 的 `Result<T>` 封装抄进一个空 Vue 项目（或 ai-cs-frontend 的副本），写一个 `listUsers` 并验证：把 `records` 拼错，观察 IDE 报错。
3. 用可辨识联合建模「SSE 推送事件」：`connected` / `delta`（含增量文本）/ `done` / `error`（含 message），写一个 `handleEvent(ev)` 函数，要求 switch 穷尽所有分支（漏分支让编译器报错）。
