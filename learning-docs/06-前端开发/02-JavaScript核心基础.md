# JavaScript 核心基础

> **前置知识**：[01-HTML与CSS基础](./01-HTML与CSS基础.md)
> **关联篇目**：[00-前端技术全景介绍](./00-前端技术全景介绍.md) · [04-Vue3核心基础](./04-Vue3核心基础.md) · [06-路由守卫与Axios请求封装](./06-路由守卫与Axios请求封装.md)
> **对应项目**：`ai-cs-frontend/src/` 下所有 `.js` 文件——本章是全篇地基，建议精读

---

## 一、JS 与 Java 的关系与差异总览

先说结论：**JavaScript 和 Java 的关系 ≈ 雷锋和雷峰塔**。1995 年 Netscape 为了蹭 Java 热度把 LiveScript 改名 JavaScript，除了同属 C 系语法（大括号、分号、if/for），两者几乎没有共同点：Java 是静态强类型、类继承、多线程语言；JS 是动态弱类型、原型继承、单线程语言。

给一张全景对照表（本章逐节展开）：

| 维度 | Java | JavaScript |
| --- | --- | --- |
| 变量声明 | `String s = "a";`（类型前置） | `let s = 'a'`（动态推断）、`const` 常量 |
| 值相等 | `equals()`，`==` 比引用 | `===`（值+类型全等），永远别用 `==` |
| "空" | `null` 一种 | `null`（有意置空）+ `undefined`（未赋值/不存在）两种 |
| 布尔判断 | 必须显式 boolean | 任意值可隐式转 truthy / falsy |
| 类型系统 | 静态，编译期检查 | 动态，运行期才报错 |
| 继承模型 | class 单继承 + interface | 原型链（class 只是语法糖） |
| 并发模型 | 多线程 + 锁 + 内存模型 | 单线程 + 事件循环（Event Loop） |
| 执行方式 | javac 编译字节码 → JVM 解释 + JIT | 源码直出 → 引擎（V8）解释 + JIT |
| 模块单元 | 类 / 包，import 类 | 文件即模块，import/export 值 |

好消息：**Java 8~21 的现代化让类比越来越自然**——局部 `var` ≈ `let`；lambda / Stream ≈ 一等公民函数；`CompletableFuture` ≈ `Promise`；Java 21 虚拟线程"少量线程扛高并发"的思想 ≈ 事件循环的"线程复用"。你已有的函数式经验可以直接迁移。

---

## 二、变量与类型

### 2.1 基本类型与 typeof

JS 只有 8 种类型：`number / string / boolean / undefined / null / symbol / bigint` 是基本类型，**其余一切都是 `object`（包括数组和函数）**。没有 int/long/float 之分，统一 `number`（64 位浮点，≈ Java 的 double）。

```js
// 01-typeof.js
typeof 1             // 'number'
typeof 'a'           // 'string'
typeof true          // 'boolean'
typeof undefined     // 'undefined'
typeof null          // 'object'  ← 历史遗留 bug，判 null 请用 === null
typeof []            // 'object'  ← 数组要用 Array.isArray([]) 判断
typeof function () {} // 'function' ← 函数是可调用的特殊对象
```

### 2.2 `==` 与 `===`：重灾区

`==` 会先做隐式类型转换再比较，规则鬼畜：

```js
// 02-eq.js
1 == '1'            // true：字符串转数字
[] == false         // true：[]→''→0，false→0
null == undefined   // true：规范规定这对相等

1 === '1'           // false：类型不同直接 false
null === undefined  // false
```

✅ 团队约定：**永远只用 `===` 和 `!==`**（ESLint 规则 `eqeqeq` 强制）。类比：JS 的 `==` ≈ 被写坏的 `equals()`，`===` 才是可靠的 `equals()` 且附带类型校验。

### 2.3 NaN 与显式转换

```js
// 03-convert.js
Number('abc')      // NaN：Not a Number，参与任何运算结果仍是 NaN
NaN === NaN        // false！判断必须用 Number.isNaN(x)
Number('42px')     // NaN    —— 严格
parseInt('42px')   // 42     —— 宽松，解析到非数字字符为止
Boolean('')        // false（见下方 falsy 列表）
```

falsy 值只有 6 个，背下来：`false、0、''、null、undefined、NaN`。其余全是 truthy——**`[]`、`{}`、`'0'` 都是 truthy**，与直觉相反。

类比：JS 隐式转换 ≈ 把 Java 的 `Integer.parseInt`、`String.valueOf` 和自动拆箱混在一起，并且"永不抛异常、失败给 NaN"——所以边界输入务必自己校验。

---

## 三、函数是一等公民

Java 里函数必须依附类（方法），JS 里函数是和 `String` 平级的值：可以赋给变量、当参数传、当返回值。这就是"一等公民"，是整个前端范式的地基。

### 3.1 三种定义方式

```js
// 04-fn.js
function add(a, b) { return a + b; }          // ① 函数声明：有提升，可在定义前调用
const sub = function (a, b) { return a - b; }; // ② 函数表达式：匿名函数赋给变量
const mul = (a, b) => a * b;                  // ③ 箭头函数：现代首选
const square = x => x * x;                    // 单参数可省括号，单表达式可省 return
```

### 3.2 箭头函数的 this：最重要的区别

- **普通函数**：this 由**调用者**决定（谁调用指向谁），无调用者时非严格模式指向 `window`；
- **箭头函数**：**没有自己的 this**，沿用**定义处外层作用域**的 this（词法作用域），且永不变。

类比：普通函数的 this ≈ 运行时动态绑定（看谁在调）；箭头函数的 this ≈ lambda 捕获外部 `this`——定义时就定死。

```js
// 05-this.js
const timer = {
  name: 'chat-timer',
  run() {
    // ❌ 普通函数：setTimeout 的回调由 window 调用，this 丢失指向 window
    setTimeout(function () { console.log(this.name); }, 100);  // undefined
    // ✅ 箭头函数：定义在 run() 里，捕获 run 的 this（即 timer）
    setTimeout(() => { console.log(this.name); }, 100);        // 'chat-timer'
  },
};
```

记忆口诀：**对象方法用普通函数（或方法简写），回调（setTimeout / forEach / 事件处理器）用箭头函数**。

### 3.3 默认参数与剩余参数

```js
// 06-args.js
function request(url, { timeout = 5000, retry = 3 } = {}) {  // 默认参数 ≈ 重载的缺省值
  // ...
}
function sum(...nums) {        // 剩余参数 ≈ Java 可变参数 int... nums
  return nums.reduce((a, b) => a + b, 0);
}
```

### 3.4 函数式方法：一张表对齐 Java Stream

```js
// 07-stream.js
const users = [
  { name: '张三', age: 18, dept: '研发' },
  { name: '李四', age: 35, dept: '销售' },
  { name: '王五', age: 28, dept: '研发' },
];

const names = users
  .filter(u => u.age >= 20)            // 中间操作：过滤
  .map(u => u.name)                    // 中间操作：映射
  .sort((a, b) => a.localeCompare(b)); // 就地排序（toSorted 返回新数组）
```

与 Java Stream 逐个对照（注意：JS 数组方法**即时求值、非惰性**，每步产生新数组）：

| JS 数组方法 | Java Stream | 备注 |
| --- | --- | --- |
| `map(fn)` | `.map(fn)` | 一对一转换 |
| `filter(fn)` | `.filter(fn)` | 返回 true 保留 |
| `reduce(fn, init)` | `.reduce(accum, init)` | 聚合为单值 |
| `sort((a,b)=>a-b)` / `toSorted` | `.sorted(Comparator.comparingInt(...))` | 比较器返回负 / 0 / 正 |
| `some(fn)` | `.anyMatch(fn)` | 任一满足即 true |
| `every(fn)` | `.allMatch(fn)` | 全部满足才 true |
| `find(fn)` | `.filter(fn).findFirst()` | 返回第一个匹配元素（不是 Optional 包装） |
| `flatMap(fn)` | `.flatMap(fn)` | 拍平一层 |
| `forEach(fn)` | `.forEach(fn)` | 终端遍历 |

### 3.5 闭包：函数记住出生地

**闭包 = 函数 + 它定义时所处词法作用域的引用**。即使外层函数已返回，内层函数依然能访问外层变量。

```js
// 08-closure.js
function makeCounter() {
  let count = 0;              // 外层局部变量
  return function () {
    count += 1;               // 闭包"记住"了 count，并且能修改它
    return count;
  };
}
const c1 = makeCounter();     // 每次调用产生独立的作用域
const c2 = makeCounter();
c1(); c1(); // 返回 2
c2();       // 返回 1 —— 与 c1 互不影响
```

类比 Java：匿名内部类 / lambda 只能捕获 effectively final 变量（值拷贝）；**JS 闭包捕获的是变量引用本身**，可以读写——相当于一个活的迷你对象。同时 `count` 被闭包引用、外部拿不到也改不了，天然实现了私有状态。

为什么重要：防抖/节流（动手练习）、模块私有变量、给每个列表项生成"预置参数"的回调，底层全是闭包。

---

## 四、对象与原型

### 4.1 对象字面量：免注册的 DTO

```js
// 09-object.js
const user = {
  id: 1,                          // key 不用引号
  name: '张三',
  greet() { return `我是${this.name}`; },   // 方法简写
};
user.email = 'z@x.com';           // 动态增删字段，没有编译器拦你
```

类比：`Map<String, Object>` + DTO 的混合体——结构像 DTO，约束像 Map。后端返回的 JSON 反序列化成 JS 对象后可以直接用，无缝衔接。

### 4.2 解构赋值与展开运算符

```js
// 10-destructure.js
// 解构：一行拆包 ≈ 同时调多个 getter
const { id, name, dept = '未知' } = user;   // 对象解构 + 默认值（Vue 里天天用）

// 展开运算符：浅拷贝 + 合并 ≈ new 对象 + copyProperties + 覆盖字段
const patch = { name: '李四' };
const updated = { ...user, ...patch };      // ✅ 不可变更新：原对象不动，产生新对象
```

前端状态更新的惯例就是"不修改原对象，用展开产生新对象"——框架靠引用变化感知数据变更（≈ 用引用不等判断 dirty）。

### 4.3 可选链 `?.` 与空值合并 `??`

```js
// 11-optional.js
// Java: Optional.ofNullable(order).map(Order::getUser).map(User::getAddr).orElse("无")
const addr = order?.user?.addr ?? '无';

city = input ?? '默认城市';   // ?? 仅在 null/undefined 时取右值
score = input || 10;          // || 会把 0、''、false 也吞掉 —— ❌ 常见 bug 源
```

`?.` ≈ `Optional.map()` 逐层剥壳，`??` ≈ `orElse()`。关键差异：JS 对象没有类型系统保护，读不存在的属性只会得到 `undefined` 而不是 NPE，`?.` 就是你的防 NPE 工具。

### 4.4 class 与原型链

```js
// 12-class.js
class Animal {
  constructor(name) { this.name = name; }   // ≈ 构造器
  speak() { return `${this.name} 叫了`; }    // 原型方法
}
class Dog extends Animal {                   // extends ≈ extends，必须先 super()
  constructor(name) { super(name); }
  speak() { return `${super.speak()}：汪`; }
}
```

class 只是语法糖，底层是**原型链**：每个对象有隐藏属性 `__proto__` 指向其构造函数的 `prototype` 对象；读属性时沿 `__proto__` 一路向上找，找到即返回，到 `null` 为止。

```
dog ──__proto__──► Dog.prototype ──__proto__──► Animal.prototype ──__proto__──► Object.prototype ──► null
 自身属性: name       speak                        speak（若自身没有则在这里找到）
```

一段话讲透：**Java 的方法查找绑定在编译期（静态类层次），JS 的方法查找发生在运行时（沿可动态修改的原型链上溯）**。好处是同类实例共享一份方法（省内存），运行时给"类"加方法所有实例立刻生效。日常会写 class、会用 `Object.hasOwn(obj, 'key')` 区分自身属性即可；深入原型链主要在看框架源码（如 Vue 响应式）时才需要。

### 4.5 this 四种指向总结

| 场景 | this 指向 | 类比 |
| --- | --- | --- |
| 独立调用 `fn()` | 非严格 `window` / 严格模式 `undefined` | 无上下文的静态调用 |
| 方法调用 `obj.fn()` | `obj`（谁调用指向谁） | 实例方法里的 this |
| `new Fn()` | 新创建的实例 | 构造器里的 this |
| 箭头函数 | 定义处外层的 this（永不变） | lambda 捕获外部 this |
| `fn.call/apply/bind(obj)` | 手动指定为 obj | 手动绑定上下文 |

---

## 五、异步编程（本章核心）

### 5.1 为什么单线程却不卡

JS 生于浏览器：DOM 只允许一个线程操作（否则两个线程同时改样式必然打架），所以 JS 主线程单线程。但它**不阻塞的秘诀**是：耗时操作（网络、定时器、文件）全部交给宿主环境（浏览器 / Node）的其他线程执行，完成后把回调塞进任务队列，主线程忙完了再来取。

类比后端：**单线程的 Netty EventLoop / Redis 命令执行**——一个线程 + IO 多路复用，从不原地等待慢 IO，所以快。JS 主线程同理：遇到耗时任务"登记后走人"，绝不停留。

### 5.2 Event Loop：先来一道口算题

规则只有两条：**微任务（Promise.then、queueMicrotask）优先于宏任务（setTimeout、I/O、事件回调）；每执行完一个宏任务，就清空全部微任务。**

```js
// 13-event-loop.js
console.log('1');                    // ① 同步代码，立即执行
setTimeout(() => {
  console.log('4');                  // ④ 宏任务：排在最后
}, 0);

Promise.resolve().then(() => {
  console.log('3');                  // ③ 微任务：同步代码一结束就执行
});
console.log('2');                    // ② 同步代码

// 输出顺序：1 2 3 4 —— 即使 setTimeout(…, 0) 也不会插队
```

逐行解释：
1. 主线程自上而下同步执行，打印 `1`；
2. 遇到 `setTimeout`：定时器交给宿主线程计时，回调进入**宏任务队列**，主线程继续往下走；
3. 遇到 `Promise.then`：回调进入**微任务队列**，主线程继续；打印 `2`，同步代码执行完、**调用栈空了，先清空全部微任务** → 打印 `3`；
4. 微任务清空后，才取一个宏任务执行 → 打印 `4`；如此循环。

面试考点：`setTimeout(fn, 0)` ≠ 立即执行，它至少要等"当前同步代码 + 全部微任务"跑完。

### 5.3 演进史：Callback → Promise → async/await

**Callback 时代**：异步结果只能靠传入回调接收，多层嵌套形成"回调地狱"：

```js
// 14-callback.js ❌
getUser(id, (err, user) => {
  getOrders(user, (err, orders) => {
    getDetail(orders[0], (err, detail) => { /* 越嵌越深，错误处理重复三遍 */ });
  });
});
```

**Promise**：一个代表"未来结果"的容器，三状态 `pending → fulfilled / rejected`，且**状态一旦改变不可逆**（≈ 一个结果只能设置一次的 `Future`）：

```js
// 15-promise.js
function fetchUser(id) {
  return new Promise((resolve, reject) => {   // executor 会同步立即执行
    setTimeout(() => (id > 0 ? resolve({ id, name: '张三' }) : reject(new Error('id 非法'))), 300);
  });
}
fetchUser(1)
  .then(user => fetchUser(user.id + 1))       // 返回新 Promise → 链继续，天然解决嵌套
  .then(user => console.log(user))
  .catch(err => console.error(err))           // 链上任意一步失败都会到这里
  .finally(() => console.log('收尾'));
```

与 `CompletableFuture` 几乎是镜像设计，方法一一对上：

| Promise | CompletableFuture | 说明 |
| --- | --- | --- |
| `then(fn)` 返回普通值 | `thenApply(fn)` | 同步变换结果 |
| `then(fn)` 返回新 Promise | `thenCompose(fn)` | 链式展平，避免 Promise 套 Promise |
| `catch(fn)` | `exceptionally(fn)` / `handle` | 异常兜底 |
| `finally(fn)` | `whenComplete(...)` | 无论成败都执行 |
| `Promise.all([...])` | `allOf(...)` | 全部成功才成功 |
| `Promise.race([...])` | `anyOf(...)` | 任一完成即定胜负 |
| `new Promise(executor)` | `supplyAsync(supplier)` | 创建异步任务 |
| `Promise.resolve(v)` | `completedFuture(v)` | 已完成的实例 |

**async/await**：Promise 的"同步写法糖"。`await` 暂停**当前 async 函数**（注意：不阻塞主线程），把 Promise 的结果直接还给你：

```js
// 16-async.js ✅
async function loadChat() {
  try {
    const user = await fetchUser(1);          // 像 Future.get()，但绝不阻塞线程
    const orders = await fetchOrders(user);   // 顺序逻辑一目了然
    return { user, orders };
  } catch (err) {                             // try/catch 天然接住 rejected
    console.error('加载失败', err);
  }
}
```

类比：写起来像 Spring MVC 里阻塞式的同步 Service 代码，实际运行在事件循环上、单线程扛并发——这正是 Java 21 虚拟线程想给你的"同步的写法、异步的效率"，JS 从语法层面直接实现了。

### 5.4 并发组合：批量接口的正确姿势

```js
// 17-combine.js
// all：全部成功才成功 —— 适合"必须都拿到"的页面初始化
const [user, msgs, kb] = await Promise.all([getUser(), getMsgs(), getKB()]);

// allSettled：部分失败可容忍 —— 适合仪表盘多卡片，挂一张别拖垮全部
const results = await Promise.allSettled([getUser(), getMsgs(), getKB()]);

// race：任一完成即返回 —— 超时控制的经典写法
const result = await Promise.race([
  fetchSlowAPI(),
  new Promise((_, reject) => setTimeout(() => reject(new Error('请求超时')), 3000)),
]);
```

### 5.5 典型错误示范

```js
// ❌ 错误一：for 循环里 await = 串行请求，n 个接口耗时累加
for (const id of ids) { await fetchUser(id); }
// ✅ 先全部发起，再统一等待（并发，总耗时 ≈ 最慢的一个）
const users = await Promise.all(ids.map(id => fetchUser(id)));

// ❌ 错误二：async 函数调用处不接 rejected → Unhandled Rejection，界面静默无反应
loadChat();
// ✅ 返回 Promise 必须有兜底：调用处 await + try/catch，或 .catch 统一上报
loadChat().catch(showErrorToast);

// ❌ 错误三：forEach 里的 await 完全无效——forEach 不等待异步回调
list.forEach(async item => { await save(item); });  // save 们并发乱跑，错误无人接
// ✅ 要顺序：for...of + await；要并发：Promise.all(list.map(...))
```

---

## 六、模块化：ESM 与 CommonJS

```js
// src/api/user.js —— 一个文件 ≈ 一个类：export 的成员 ≈ public，未导出的 ≈ 包私有
const BASE = '/api/user';             // 模块内私有，外部不可见
export function getUser(id) { ... }   // 具名导出：导入时名字必须一致
export const TIMEOUT = 5000;
export default { getUser };           // 默认导出：一个文件最多一个，≈ 类的主入口
```

```js
// src/views/UserView.js
import userApi, { getUser, TIMEOUT } from '../api/user.js';  // 默认导出可自由命名
```

对照 Java：`import` 语义相同，但 JS **导入的是"值"（对象/函数），且路径必须写全**，没有 classpath 自动扫描。**为什么历史上有两套**：Node 2011 年先用 `require / module.exports`（CommonJS）跑起来，ES2015 才标准化出 ESM，浏览器与打包器（Vite）都只认 ESM。**本项目与所有 Vite 项目一律 ESM**；见到 `require` 多半是老的 Node 脚本或 CJS 依赖。

---

## 七、DOM 与事件：了解即可

```js
// 18-dom.js
const btn = document.querySelector('#submit-btn');   // 用 CSS 选择器找元素
btn.addEventListener('click', (e) => {               // 注册事件 ≈ 观察者模式订阅
  e.target;        // 实际被点击的元素（可能是 btn 的子元素）
  e.currentTarget; // 当前绑定监听的元素（btn 本身）
});
```

- **事件冒泡**：子元素的事件会逐级向父元素传播。**事件委托**：利用冒泡，把监听挂在父容器上统一处理所有子项（列表项增删后无需重新绑事件）。
- 类比：事件委托 ≈ 网关统一鉴权，而不是每个微服务各自实现一遍。

一句话定位 Vue：**Vue 的响应式 + 模板系统就是"帮你跳过手写 DOM"**——你只声明"状态长什么样"，框架负责增删改 DOM。但事件冒泡概念必须懂：Element Plus 的事件处理、`@click.stop` 阻止冒泡都基于它。

---

## 八、ES6+ 常用语法速查表

| 语法 | 示例 | Java 类比 / 说明 |
| --- | --- | --- |
| 模板字符串 | `` `你好 ${name}` `` | `String.format`，支持多行 |
| 解构（对象/数组） | `const { a } = obj` | 一行拆多个 getter |
| 展开 / 剩余 | `{ ...a, ...b }` / `(...rest)` | copyProperties / 可变参数 |
| 可选链 | `a?.b?.c` | `Optional.map` 链 |
| 空值合并 | `a ?? '默认'` | `orElse`，但只认 null/undefined |
| 箭头函数 | `x => x * 2` | lambda 表达式 |
| 默认参数 | `function f(a = 1)` | 重载的缺省参数 |
| 属性简写 | `{ name }`（即 `name: name`） | 构造器赋值省写 |
| 数组链式 | `[1,2].map().filter()` | Stream（注意非惰性） |
| async / await | `await fetchUser(1)` | 不阻塞线程的 `Future.get()` |
| 模块 | `import` / `export` | import（路径必须写全） |

## 本章小结

- JS ≠ Java：动态弱类型、原型继承、单线程事件循环；`===` 永远替代 `==`；falsy 只有 6 个值，`[]` 和 `'0'` 是 truthy；
- 函数是一等公民：箭头函数的 this 是"定义时"而非"调用时"；数组方法 ≈ Stream 但即时求值；闭包 = 函数 + 词法作用域引用，能读写外部变量；
- 对象 = 免注册 DTO：解构 / 展开是日常惯用语，`?.` + `??` 是防 NPE 组合拳，class 只是原型链的语法糖，this 只有四种指向；
- **异步是灵魂**：Event Loop 两规则（微任务优先、每宏任务清空微任务）；Promise ≈ CompletableFuture；async/await = 同步写法、异步运行、绝不阻塞主线程；
- 并发组合：`Promise.all` 必须全成、`allSettled` 容忍失败、`race` 做超时；三大坑——for 内串行 await、async 不接 rejected、forEach 里 await；
- 模块只用 ESM：export ≈ public，未导出 ≈ 包私有；手写 DOM 只存在于框架之外，但事件冒泡与委托必须懂。

## 动手练习

1. **口算 Event Loop**：先不用浏览器，写出下面代码的输出顺序，再运行验证：同步 `log('A')`；`setTimeout(() => log('B'))`；`Promise.resolve().then(() => { log('C'); setTimeout(() => log('D')); })`；同步 `log('E')`。（关键：微任务先于所有宏任务；C 里新排的 setTimeout 排在 B 之后。）
2. **手写 debounce**：实现 `debounce(fn, delay)`——连续触发只在停止 delay 毫秒后执行一次（搜索框输入场景），要求用闭包保存 timer，并解释为什么箭头函数能让 `this` 和参数透传不丢。
3. **reduce 实现 groupBy**：用 `reduce` 把 `[{dept:'研发'},{dept:'销售'},{dept:'研发'}]` 聚合成 `{ 研发: [...], 销售: [...] }`，并写出等价的 Java `Collectors.groupingBy` 实现作对照。
4. **回调改造**：把 14-callback.js 的三层回调地狱改写成 `async/await` + `try/catch` 版本，并说明为什么改造后错误处理只需要一份。
