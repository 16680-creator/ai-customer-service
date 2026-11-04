"""04 · 接口的三种替代：鸭子类型 / Protocol / ABC

运行: python 04_oop/04_interface.py
"""

# ============================================================
# 本节演示：鸭子类型——不定义任何接口，只要有 send 方法就能用
# 为什么重要：这是 Python 最主流的做法（90% 场景够用），零成本、不改已有类；代价是没有静态检查。
# Java 对照：Java 必须抽 NotificationSender 接口 + 三个 implements + 依赖注入，Python 一行都不用写。
# ============================================================

print("=" * 60)
print("1. 鸭子类型：什么协议都不写（90% 场景的正确选择）")
print("=" * 60)

class EmailSender:
    def send(self, msg):
        return f"邮件：{msg}"        # 邮件实现

class SmsSender:
    def send(self, msg):
        return f"短信：{msg}"        # 短信实现

class WechatSender:
    def send(self, msg):
        return f"微信：{msg}"        # 微信实现

def notify(sender, msg):
    """不检查类型，只要有 send 方法就行"""
    return sender.send(msg)          # 运行到这一行才检查有没有 send；没有就抛 AttributeError

for s in [EmailSender(), SmsSender(), WechatSender()]:   # 三种完全不同类型的实例
    print(f"  {notify(s, 'hello')}")                     # 同一个函数处理全部，无需接口/泛型

print("\n  对照 Java：这里必须抽 NotificationSender 接口 + 三个 implements + 注入")


print("=" * 60)
print("2. Protocol：结构化类型（有静态检查，但不用显式实现）")
print("=" * 60)

# ============================================================
# 本节演示：Protocol（结构化类型）+ runtime_checkable
# 为什么重要：既得到 mypy 的静态检查，又不需要实现类继承协议——可以给已有/第三方类"后配"接口。
# Java 对照：等价 interface，但 Java 要求 implements；Protocol 是"隐式实现"，只看结构是否匹配。
# 注意：@runtime_checkable 只让 isinstance 能用于运行时判断方法名，不校验签名/类型。
# ============================================================

from typing import Protocol, runtime_checkable   # Protocol 定义结构协议；runtime_checkable 允许 isinstance

@runtime_checkable                   # 允许 isinstance(obj, Sender) —— 不写只能用类型注解
class Sender(Protocol):
    def send(self, msg: str) -> str: ...   # 只声明签名，方法体用 ... 占位

class SlackSender:                 # 注意：没有 implements！   ← 不继承 Sender，仅结构一致
    def send(self, msg: str) -> str:
        return f"Slack：{msg}"       # 具体实现

def notify_typed(sender: Sender, msg: str) -> str:   # 注解写协议名，mypy 会检查实参是否结构匹配
    return sender.send(msg)          # 运行时仍是普通动态调用

print(f"  notify_typed(SlackSender(), 'hi') -> {notify_typed(SlackSender(), 'hi')}")   # 正常调用
print(f"  isinstance(SlackSender(), Sender) -> {isinstance(SlackSender(), Sender)}")   # True：有 send 方法
print("  runtime_checkable 让 isinstance 也能用（但只检查方法名，不检查签名）")

class NoSend:
    def other(self): ...             # 只有 other，没有 send

print(f"  isinstance(NoSend(), Sender)      -> {isinstance(NoSend(), Sender)}")   # False：结构不匹配


print("\n" + "=" * 60)
print("3. ABC：抽象基类（最接近 Java 的 interface + abstract class）")
print("=" * 60)

# ============================================================
# 本节演示：ABC 抽象基类（强制子类实现 + 可带默认实现）
# 为什么重要：这是唯一"运行时强制"的约束手段：少实现一个抽象方法就无法实例化，直接 TypeError。
# Java 对照：ABC ≈ abstract class；@abstractmethod ≈ abstract 方法；health() ≈ interface default 方法。
# 代价：必须显式继承，无法像 Protocol 那样适配已有类。
# ============================================================

from abc import ABC, abstractmethod   # ABC 提供抽象基类机制；abstractmethod 标记必须实现

class Repository(ABC):               # 继承 ABC，否则 @abstractmethod 不生效
    @abstractmethod
    def save(self, entity: dict) -> None: ...        # 子类必须实现，否则不能实例化

    @abstractmethod
    def find_by_id(self, id: int) -> dict | None: ...   # 同上；dict | None 是联合类型（3.10+ 语法）

    def health(self) -> str:            # 可以有默认实现（= interface default method）
        return "ok"                     # 子类可继承可覆盖

class MemoryRepo(Repository):        # 显式继承，才能获得"必须实现"的检查
    def __init__(self):
        self._data: dict[int, dict] = {}   # 内存表：id → 实体；类型注解只是提示

    def save(self, entity: dict) -> None:
        self._data[entity["id"]] = entity   # 用 id 作为键写入字典

    def find_by_id(self, id: int) -> dict | None:
        return self._data.get(id)           # get：不存在返回 None，而不是抛 KeyError

r = MemoryRepo()
r.save({"id": 1, "name": "张三"})            # 写入一条
print(f"  r.find_by_id(1) = {r.find_by_id(1)}")   # 读回来
print(f"  r.health()      = {r.health()}")       # 继承来的默认实现 → ok

try:
    Repository()                     # 抽象类不能实例化，运行时报 TypeError
except TypeError as e:
    print(f"\n  Repository() -> TypeError: {e}")

class Incomplete(Repository):
    def save(self, entity): pass        # 少了 find_by_id

try:
    Incomplete()                     # 缺一个抽象方法，依然不能实例化
except TypeError as e:
    print(f"  Incomplete() -> TypeError: {e}")


print("=" * 60)
print("4. 三者对比")
print("=" * 60)

# ============================================================
# 本节演示：三者对比（只是打印说明文字，三引号内是文本不是代码）
# 结论速记：内部协作/脚本 → 鸭子类型；要静态检查/适配三方类 → Protocol；框架扩展点 → ABC。
# Java 对照：从"到处抽接口"的 Java 思维切换过来，Python 里先不抽接口，真的需要类型安全时再加 Protocol。
# ============================================================

print("""
  鸭子类型
    + 零成本，最 Pythonic
    + 不改已有类就能适配
    - 无静态检查（mypy 不管）
    - 文档要写清楚需要什么方法
    适用：内部协作、脚本、已有代码库

  Protocol
    + 有静态检查（mypy/pyright）
    + 不需要显式继承，适配第三方类无压力
    - 运行时不强约束（除非 runtime_checkable）
    适用：需要类型安全的公共库、团队协作

  ABC
    + 强制子类实现，运行时就报错
    + 可以有共享实现（模板方法模式）
    - 必须显式继承，无法适配已有类
    适用：框架扩展点、明确的类层次
""")


print("=" * 60)
print("5. 实战：用 Protocol 让代码可测试")
print("=" * 60)

# ============================================================
# 本节演示：实战——用 Protocol 做可测试设计（生产用真实 LLM，测试注入 Fake）
# 为什么重要：这是项目 ai-cs-py-chat 的测试策略：LLM 调用全部 mock 掉，跑得快、不花钱、结果稳定。
# Java 对照：LlmClient 协议 ≈ 接口；ChatService 构造注入 ≈ 构造器注入；FakeLlm ≈ Mockito 的 mock/stub。
# 注意 async/await 是 Python 的协程语法，对应 Java 的 CompletableFuture，先按"异步函数"理解即可。
# ============================================================

class LlmClient(Protocol):           # 协议：任何有 async chat(history, message) 的对象都满足
    async def chat(self, history: list[dict], message: str) -> str: ...   # 只有签名

class RealLlm:
    async def chat(self, history, message):
        return "真实调用（会花钱）"    # 生产实现：真实网络请求（这里简化成固定字符串）

class FakeLlm:
    """测试替身：不联网、不花钱"""
    async def chat(self, history, message):
        return f"echo: {message}"     # 测试实现：原样回显，断言方便

import asyncio                        # 标准库异步事件循环

class ChatService:
    def __init__(self, llm: LlmClient | None = None):
        self.llm = llm or RealLlm()   # 没传就用真实实现；传了就用传入的（依赖注入，等价 Java 构造器注入）

    async def reply(self, message: str) -> str:
        return await self.llm.chat([], message)   # await 等待协程结果；history 这里传空列表

async def main():
    real = ChatService()              # 默认注入 RealLlm
    print(f"  生产：{await real.reply('你好')}")

    test = ChatService(llm=FakeLlm())      # 构造注入替身
    print(f"  测试：{await test.reply('你好')}")   # 输出 echo: 你好，可断言

asyncio.run(main())                   # 启动事件循环并执行 main()，等价 Java 里提交任务给 Executor
print("  这就是 ai-cs-py-chat 的测试策略：LLM 全 mock，跑得快又不花钱")


print("=" * 60)
print("6. 反模式：不是所有东西都要抽接口")
print("=" * 60)

# ============================================================
# 本节演示：反模式——Java 式"每个类都配一个 Interface + Impl"在 Python 里是过度设计
# 为什么重要：Python 靠鸭子类型天然可替换；只有真需要类型安全时才加 Protocol，不要预防性抽接口。
# Java 对照：Java 的 interface/impl 分层常由 Spring 生态和编译期类型决定；Python 里这层通常是纯负担。
# ============================================================

print("""
  Java 思维（过度设计）：
    class UserServiceInterface
    class UserServiceImpl implements UserServiceInterface
    class UserRepositoryInterface
    class UserRepositoryImpl implements UserRepositoryInterface
    → Python 里这么写会被同事吐槽

  Python 思维：
    class UserService:
        def __init__(self, repo=None):
            self.repo = repo or UserRepo()

    → 需要替换时传参
    → 需要接口时用 Protocol（只在真的需要类型安全时才加）
""")
