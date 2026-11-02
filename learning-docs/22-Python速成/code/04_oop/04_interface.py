"""04 · 接口的三种替代：鸭子类型 / Protocol / ABC

运行: python 04_oop/04_interface.py
"""

print("=" * 60)
print("1. 鸭子类型：什么协议都不写（90% 场景的正确选择）")
print("=" * 60)

class EmailSender:
    def send(self, msg):
        return f"邮件：{msg}"

class SmsSender:
    def send(self, msg):
        return f"短信：{msg}"

class WechatSender:
    def send(self, msg):
        return f"微信：{msg}"

def notify(sender, msg):
    """不检查类型，只要有 send 方法就行"""
    return sender.send(msg)

for s in [EmailSender(), SmsSender(), WechatSender()]:
    print(f"  {notify(s, 'hello')}")

print("\n  对照 Java：这里必须抽 NotificationSender 接口 + 三个 implements + 注入")


print("=" * 60)
print("2. Protocol：结构化类型（有静态检查，但不用显式实现）")
print("=" * 60)

from typing import Protocol, runtime_checkable

@runtime_checkable
class Sender(Protocol):
    def send(self, msg: str) -> str: ...

class SlackSender:                 # 注意：没有 implements！
    def send(self, msg: str) -> str:
        return f"Slack：{msg}"

def notify_typed(sender: Sender, msg: str) -> str:
    return sender.send(msg)

print(f"  notify_typed(SlackSender(), 'hi') -> {notify_typed(SlackSender(), 'hi')}")
print(f"  isinstance(SlackSender(), Sender) -> {isinstance(SlackSender(), Sender)}")
print("  runtime_checkable 让 isinstance 也能用（但只检查方法名，不检查签名）")

class NoSend:
    def other(self): ...

print(f"  isinstance(NoSend(), Sender)      -> {isinstance(NoSend(), Sender)}")


print("\n" + "=" * 60)
print("3. ABC：抽象基类（最接近 Java 的 interface + abstract class）")
print("=" * 60)

from abc import ABC, abstractmethod

class Repository(ABC):
    @abstractmethod
    def save(self, entity: dict) -> None: ...

    @abstractmethod
    def find_by_id(self, id: int) -> dict | None: ...

    def health(self) -> str:            # 可以有默认实现（= interface default method）
        return "ok"

class MemoryRepo(Repository):
    def __init__(self):
        self._data: dict[int, dict] = {}

    def save(self, entity: dict) -> None:
        self._data[entity["id"]] = entity

    def find_by_id(self, id: int) -> dict | None:
        return self._data.get(id)

r = MemoryRepo()
r.save({"id": 1, "name": "张三"})
print(f"  r.find_by_id(1) = {r.find_by_id(1)}")
print(f"  r.health()      = {r.health()}")

try:
    Repository()
except TypeError as e:
    print(f"\n  Repository() -> TypeError: {e}")

class Incomplete(Repository):
    def save(self, entity): pass        # 少了 find_by_id

try:
    Incomplete()
except TypeError as e:
    print(f"  Incomplete() -> TypeError: {e}")


print("=" * 60)
print("4. 三者对比")
print("=" * 60)

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

class LlmClient(Protocol):
    async def chat(self, history: list[dict], message: str) -> str: ...

class RealLlm:
    async def chat(self, history, message):
        return "真实调用（会花钱）"

class FakeLlm:
    """测试替身：不联网、不花钱"""
    async def chat(self, history, message):
        return f"echo: {message}"

import asyncio

class ChatService:
    def __init__(self, llm: LlmClient | None = None):
        self.llm = llm or RealLlm()

    async def reply(self, message: str) -> str:
        return await self.llm.chat([], message)

async def main():
    real = ChatService()
    print(f"  生产：{await real.reply('你好')}")

    test = ChatService(llm=FakeLlm())      # 构造注入替身
    print(f"  测试：{await test.reply('你好')}")

asyncio.run(main())
print("  这就是 ai-cs-py-chat 的测试策略：LLM 全 mock，跑得快又不花钱")


print("=" * 60)
print("6. 反模式：不是所有东西都要抽接口")
print("=" * 60)

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
