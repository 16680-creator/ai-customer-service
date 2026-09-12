# -*- coding: utf-8 -*-
"""
OrcaTerm -> Trae CN 桥接服务（OpenAI 兼容）

原理：
  腾讯云 OrcaTerm 桌面版的内置 AI 模型（Hy4-preview / Hy3 / Kimi-K3 / GLM-5.3 系列 /
  DeepSeek-V4 系列等，限时免费）只能通过其私有界面/接口使用，无法直接当作
  OpenAI 兼容端点。本服务在本机 127.0.0.1:8317 提供标准 OpenAI 接口
  （/v1/models、/v1/chat/completions），内部通过 WebView2 远程调试协议(CDP)
  驱动 OrcaTerm 的 "OrcaTerm Agent" 面板完成对话：
    新会话 -> 选择模型 -> 填入 prompt -> 发送 -> 等待完成 -> 提取回答文本。

使用前提：
  1. 已登录腾讯云账号的 OrcaTerm（首次需要人工登录）。
  2. OrcaTerm 必须带远程调试端口启动；若检测到未开启，本服务会自动重启它：
     set WEBVIEW2_ADDITIONAL_BROWSER_ARGUMENTS=--remote-debugging-port=9333

Trae CN 侧配置（设置 -> 模型 -> 添加模型）：
    服务商:   OpenAI 兼容
    Base URL: http://127.0.0.1:8317/v1
    API Key:  orcaterm-local（任意非空值）
    模型 ID:  glm-5.3 / glm-5.3-flash / glm-5.2 / deepseek-v4-pro /
              deepseek-v4-flash / kimi-k3 / hy4-preview / hy3
"""

import json
import os
import functools
print = functools.partial(print, flush=True)
import re
import subprocess
import threading
import time
import urllib.request
import uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

# ---------- 配置 ----------
CDP_PORT = 9333
LISTEN_HOST = "127.0.0.1"
LISTEN_PORT = 8317
ORCATERM_EXE = r"D:\DevTools\OrcaTerm\OrcaTerm.exe"
API_KEY = "orcaterm-local"          # Trae 侧随便填，桥接不校验
REPLY_TIMEOUT = 240                  # 单次回答最长等待（秒）
MAX_TURNS_WAIT = int(REPLY_TIMEOUT / 2)

# Trae 侧使用的模型 ID -> OrcaTerm 模型菜单显示名（前缀匹配）
MODELS = {
    "hy4-preview":        "Hy4-preview",
    "hy3":                "Hy3",
    "kimi-k3":            "Kimi-K3",
    "glm-5.3-flash":      "GLM-5.3-Flash",
    "glm-5.3":            "GLM-5.3",
    "glm-5.2":            "GLM-5.2",
    "deepseek-v4-flash":  "DeepSeek-V4-Flash",
    "deepseek-v4-pro":    "Deepseek-V4-Pro",
}
DEFAULT_MODEL = "glm-5.3"

# ---------- CDP 客户端 ----------
try:
    import websocket  # websocket-client
except ImportError:
    raise SystemExit("缺少依赖：请先执行 pip install websocket-client")


class CDP:
    """与 OrcaTerm 主页面（tauri.localhost）的 CDP 会话。"""

    def __init__(self, port=CDP_PORT):
        self.port = port
        self.ws = None
        self.mid = 0
        self.lock = threading.RLock()

    def _http(self, path):
        return json.load(urllib.request.urlopen(
            f"http://127.0.0.1:{self.port}{path}", timeout=10))

    def alive(self):
        try:
            self._http("/json/version")
            return True
        except Exception:
            return False

    def connect(self):
        targets = self._http("/json/list")
        main = [t for t in targets
                if t.get("type") == "page" and t.get("url", "").rstrip("/") == "http://tauri.localhost"]
        if not main:  # 兜底：排除动态岛等子窗口
            main = [t for t in targets
                    if t.get("type") == "page" and "tauri.localhost" in t.get("url", "")
                    and "dynamic-island" not in t.get("url", "")]
        if not main:
            raise RuntimeError("未找到 OrcaTerm 主页面 target")
        print("[bridge] CDP attach:", main[0]["url"][:60])
        self.ws = websocket.create_connection(
            main[0]["webSocketDebuggerUrl"], timeout=300, suppress_origin=True)
        self.ws.settimeout(0.4)
        # 预授权剪贴板（部分场景用得到）
        try:
            ver = self._http("/json/version")
            bw = websocket.create_connection(ver["webSocketDebuggerUrl"],
                                             timeout=10, suppress_origin=True)
            bw.send(json.dumps({"id": 1, "method": "Browser.grantPermissions",
                                "params": {"permissions": ["clipboardReadWrite"]}}))
            time.sleep(0.3)
            bw.close()
        except Exception:
            pass

    def ensure(self):
        with self.lock:
            if self.ws is None:
                self.connect()
            return True

    def _recv_until(self, msg_id, timeout):
        end = time.time() + timeout
        while time.time() < end:
            try:
                r = json.loads(self.ws.recv())
            except websocket.WebSocketTimeoutException:
                continue
            except Exception:
                self.ws = None
                raise ConnectionError("CDP 连接断开")
            if isinstance(r, dict) and r.get("id") == msg_id:
                return r
        return None

    def eval_js(self, expr, timeout=20, await_promise=True):
        with self.lock:
            self.ensure()
            self.mid += 1
            self.ws.send(json.dumps({
                "id": self.mid, "method": "Runtime.evaluate",
                "params": {"expression": expr, "returnByValue": True,
                           "awaitPromise": await_promise}}))
            r = self._recv_until(self.mid, timeout)
            if r is None:
                raise TimeoutError("CDP evaluate 超时")
            res = r.get("result", {})
            if "exceptionDetails" in res:
                raise RuntimeError("JS 异常: %s" %
                                   json.dumps(res["exceptionDetails"])[:300])
            return res.get("result", {}).get("value")

    def insert_text(self, text):
        with self.lock:
            self.ensure()
            self.mid += 1
            self.ws.send(json.dumps({"id": self.mid, "method": "Input.insertText",
                                     "params": {"text": text}}))
            self._recv_until(self.mid, 10)

    def click_at(self, x, y):
        with self.lock:
            self.ensure()
            for t, extra in (("mousePressed", {"clickCount": 1}),
                             ("mouseReleased", {"clickCount": 1})):
                self.mid += 1
                self.ws.send(json.dumps({"id": self.mid, "method": "Input.dispatchMouseEvent",
                                         "params": {"type": t, "x": int(x), "y": int(y),
                                                    "button": "left", **extra}}))
                self._recv_until(self.mid, 10)

    def key_combo(self, key, code, vk, modifiers):
        """modifiers: list of CDP modifier flags, e.g. [2]=Ctrl, [4]=Shift"""
        with self.lock:
            self.ensure()
            seq = [("rawKeyDown", {}), ("keyUp", {})]
            for t, _ in seq:
                self.mid += 1
                self.ws.send(json.dumps({"id": self.mid, "method": "Input.dispatchKeyEvent",
                                         "params": {"modifiers": modifiers, "type": t,
                                                    "key": key, "code": code,
                                                    "windowsVirtualKeyCode": vk,
                                                    "nativeVirtualKeyCode": vk}}))
                self._recv_until(self.mid, 10)

    def reload_page(self):
        with self.lock:
            self.ensure()
            self.mid += 1
            self.ws.send(json.dumps({"id": self.mid, "method": "Page.reload",
                                     "params": {}}))
            self._recv_until(self.mid, 15)


cdp = CDP()

# ---------- OrcaTerm 进程管理 ----------

def ensure_orcaterm():
    """确保 OrcaTerm 正在运行且 CDP 端口可用；否则带参重启。"""
    if cdp.alive():
        return
    # 是否已有不带调试端口实例
    running = subprocess.run(["tasklist", "/FI", "IMAGENAME eq OrcaTerm.exe"],
                             capture_output=True, text=True).stdout.lower()
    if "orcaterm.exe" in running:
        print("[bridge] OrcaTerm 已在运行但无调试端口，重启之…")
        subprocess.run(["taskkill", "/F", "/IM", "OrcaTerm.exe"],
                       capture_output=True)
        time.sleep(3)
    env = dict(os.environ)
    env["WEBVIEW2_ADDITIONAL_BROWSER_ARGUMENTS"] = \
        f"--remote-debugging-port={CDP_PORT}"
    subprocess.Popen(["cmd", "/c", "start", "", ORCATERM_EXE],
                     cwd=os.path.dirname(ORCATERM_EXE), env=env)
    # 等待 CDP 就绪（应用冷启动较慢）
    for _ in range(60):
        time.sleep(2)
        if cdp.alive():
            break
    else:
        raise RuntimeError("OrcaTerm CDP 端口等待超时")
    time.sleep(4)


# ---------- 面板自动化 ----------
JS = {
    "panel_open": "!!document.querySelector('.chat-ui-rich-input__editor')",

    "panel_visible": """!!(document.querySelector('.chat-ui-sender') ||
        document.querySelector('.orcaterm-model-selector__trigger') ||
        document.querySelector('.orcaterm-task-execution-status-message'))""",

    "open_panel": """(()=>{let b=document.querySelector('.ot-header-ai');
        if(b){b.click(); return 'ok-header'}
        const sp=[...document.querySelectorAll('[class*=start-page] div,[class*=start-page] span,button,div,span')]
        .filter(e=>e.children.length<=2 && e.textContent.trim()==='OrcaTerm AI');
        if(sp.length){(sp[0].closest('[role=button],button,li,[class*=item]')||sp[0]).click(); return 'ok-startpage'}
        return 'no-entry'})()""",

    "close_drawer_new_session": """(()=>{const els=[...document.querySelectorAll('div,span,li,button')]
        .filter(e=>{if(e.textContent.trim()!=='新会话'||e.children.length>2) return false;
            const r=e.getBoundingClientRect(); return r.width>0&&r.height>0;});
        if(!els.length) return 'not-found-or-hidden';
        (els[0].closest('[role=button],li,button')||els[0]).click(); return 'ok'})()""",

    "send_escape": """(()=>{return 'skip-escape'})()""",   # Escape 会关闭整个面板，禁用

    "active_is_editor": """(()=>{const a=document.activeElement;
        return !!(a && /chat-ui-rich-input__editor/.test((a.className||'').toString()))})()""",

    "page_healthy": """document.title.includes('OrcaTerm') ||
        !!document.querySelector('.orcaterm-start-page, .ot-header-ai, .chat-ui-rich-input__editor')""",

    "open_history": """(()=>{const bs=[...document.querySelectorAll('button')].filter(b=>{
        const r=b.getBoundingClientRect(); return r.x>820&&r.x<960&&r.y>10&&r.y<70&&r.width>0});
        if(!bs.length) return 'no-btn';
        bs[0].click(); return 'ok'})()""",

    "drawer_rect": """(()=>{const m=document.querySelector('.agent-menu,[class*=left-pane__menu]');
        if(!m) return 'null'; const r=m.getBoundingClientRect();
        return JSON.stringify({x:r.x,y:r.y,w:r.width,h:r.height})})()""",

    "editor_exposed": """(()=>{const e=document.querySelector('.chat-ui-rich-input__editor');
        if(!e) return false; const r=e.getBoundingClientRect();
        const at=document.elementFromPoint(r.x+r.width/2, r.y+r.height/2);
        return !!at && (at===e || e.contains(at) || at.contains(e));})()""",

    "user_msg_count": "document.querySelectorAll('.orcaterm-user-message').length",

    "assistant_msg_count": "document.querySelectorAll('.orcaterm-assistant-message').length",

    "current_model": "(document.querySelector('.orcaterm-model-selector__trigger-label')||{textContent:''}).textContent",

    "open_model_menu": """(()=>{const t=document.querySelector('.orcaterm-model-selector__trigger');
        if(!t) return 'no-trigger';
        (t.closest('button')||t).click(); return 'ok'})()""",

    "pick_model": """(label)=>0""",   # 由 pick_model_js() 动态生成

    "focus_editor": """(()=>{const e=document.querySelector('.chat-ui-rich-input__editor');
        if(!e) return 'no-editor'; e.focus(); return 'ok'})()""",

    "editor_center": """(()=>{const e=document.querySelector('.chat-ui-rich-input__editor');
        if(!e) return 'null'; const r=e.getBoundingClientRect();
        return JSON.stringify({x:Math.round(r.x+r.width/2), y:Math.round(r.y+r.height/2)})})()""",

    "editor_click_point": """(()=>{const e=document.querySelector('.chat-ui-rich-input__editor');
        if(!e) return 'null'; const r=e.getBoundingClientRect();
        const pts=[];
        for(let fy of [0.5,0.3,0.7,0.2,0.8]) for(let fx of [0.5,0.3,0.7,0.15,0.85,0.05,0.95])
            pts.push([r.x+r.width*fx, r.y+r.height*fy]);
        for(const [x,y] of pts){
            const at=document.elementFromPoint(x,y);
            if(at && (at===e || e.contains(at) || at.contains(e)))
                return JSON.stringify({x:Math.round(x), y:Math.round(y)});
        }
        return JSON.stringify({x:Math.round(r.x+8), y:Math.round(r.y+r.height/2)});
    })()""",

    "send_btn_center": """(()=>{const b=document.querySelector('.chat-ui-sender__form-action');
        if(!b) return 'null'; const r=b.getBoundingClientRect();
        const pts=[];
        for(let fy of [0.5,0.4,0.6,0.3,0.7]) for(let fx of [0.5,0.4,0.6,0.3,0.7,0.2,0.8])
            pts.push([r.x+r.width*fx, r.y+r.height*fy]);
        for(const [x,y] of pts){
            const at=document.elementFromPoint(x,y);
            if(at && (at===b || b.contains(at) || at.contains(b)))
                return JSON.stringify({x:Math.round(x), y:Math.round(y)});
        }
        return JSON.stringify({x:Math.round(r.x+r.width/2), y:Math.round(r.y+r.height/2)});
    })()""",

    "clear_editor": """(()=>{const e=document.querySelector('.chat-ui-rich-input__editor');
        if(!e) return 'no-editor'; e.focus();
        document.execCommand('selectAll', false, null);
        document.execCommand('delete', false, null);
        return 'ok'})()""",

    "editor_text": "(document.querySelector('.chat-ui-rich-input__editor')||{textContent:''}).textContent",

    "click_send": """(()=>{const b=document.querySelector('.chat-ui-sender__form-action');
        if(!b) return 'no-btn'; b.click(); return 'ok'})()""",

    "reply_state": """(()=>{const msgs=[...document.querySelectorAll('.orcaterm-assistant-message')];
        if(!msgs.length) return 'none';
        const m=msgs[msgs.length-1];
        if(m.querySelector('.orcaterm-task-execution-status.is-completed')) return 'done';
        return 'streaming'})()""",

    "extract_answer": """(()=>{const msgs=[...document.querySelectorAll('.orcaterm-assistant-message')];
        if(!msgs.length) return '';
        const md=msgs[msgs.length-1].querySelector('.chat-streaming-markdown');
        if(!md) return (msgs[msgs.length-1].textContent||'').trim();
        const kids=[...md.children];
        let parts = kids.length>=2 ? kids.slice(1) : kids;
        return parts.map(c=>c.innerText!==undefined?c.innerText:c.textContent).join('\\n\\n').trim();})()""",
}


def pick_model_js(label):
    """在模型菜单里选模型：优先精确匹配（去掉“新/限时免费”角标后），再退化为前缀匹配。"""
    want = json.dumps(label.replace(" ", ""), ensure_ascii=False)
    return """(()=>{
        const norm=s=>s.replace(/\\s+/g,'').replace(/新$/,'').replace(/限时免费$/,'').replace(/新$/,'');
        const want=%s;
        const items=[...document.querySelectorAll('li,[role=option],[class*=item]')]
        .filter(e=>{const t=e.textContent.trim(); if(!t||t.length>40) return false;
            const n=norm(t); return n.toLowerCase()===want.toLowerCase();});
        if(!items.length){
            items.push(...[...document.querySelectorAll('li,[role=option],[class*=item]')]
            .filter(e=>{const t=e.textContent.trim(); if(!t||t.length>40) return false;
                return norm(t).toLowerCase().startsWith(want.toLowerCase());})
            .sort((a,b)=>norm(a.textContent).length-norm(b.textContent).length));
        }
        if(!items.length) return 'not-found';
        const it=items[0]; (it.closest('[role=option],li')||it).click(); return 'ok';})()""" % want


def panel_ready():
    try:
        return bool(cdp.eval_js(JS["panel_open"], timeout=8))
    except Exception:
        return False


def panel_visible():
    try:
        return bool(cdp.eval_js(JS["panel_visible"], timeout=8))
    except Exception:
        return False


def ensure_panel():
    """确保输入框可用且未被抽屉遮挡。抽屉用“新会话”/点击空白处收起；崩溃页自动重载。"""
    for attempt in range(4):
        # 崩溃自检（“哎呀，出了点问题”错误页）
        try:
            healthy = cdp.eval_js(JS["page_healthy"], timeout=8)
        except Exception:
            healthy = False
        if not healthy:
            print("[bridge] 检测到页面异常，重新加载 …")
            try:
                cdp.reload_page()
            except Exception:
                pass
            for _ in range(40):
                time.sleep(2)
                try:
                    if cdp.eval_js(JS["page_healthy"], timeout=8):
                        break
                except Exception:
                    pass
        if panel_ready():
            if cdp.eval_js(JS["editor_exposed"], timeout=8):
                return
            dismiss_drawer()
            if cdp.eval_js(JS["editor_exposed"], timeout=8):
                return
        if panel_visible():
            # 多半是会话历史抽屉盖住了输入框
            r = cdp.eval_js(JS["close_drawer_new_session"], timeout=10)
            time.sleep(1.8)
            dismiss_drawer()
            if panel_ready() and cdp.eval_js(JS["editor_exposed"], timeout=8):
                print("[bridge] 经“新会话”恢复面板:", r)
                return
        r = cdp.eval_js(JS["open_panel"], timeout=10)
        for _ in range(16):
            time.sleep(0.5)
            if panel_ready() and cdp.eval_js(JS["editor_exposed"], timeout=8):
                print("[bridge] 打开面板:", r)
                return
        print(f"[bridge] 面板未就绪，重试 {attempt+1}/4 (上次入口: {r})")
    raise RuntimeError("无法打开 OrcaTerm AI 面板")


def dismiss_drawer():
    """会话抽屉(左侧菜单)打开时会盖住编辑器；点抽屉右侧的聊天区域把它收起。"""
    for _ in range(3):
        if cdp.eval_js(JS["editor_exposed"], timeout=8):
            return True
        raw = cdp.eval_js(JS["drawer_rect"], timeout=8)
        if not raw or raw == "null":
            return cdp.eval_js(JS["editor_exposed"], timeout=8)
        d = json.loads(raw)
        # 窗口内容宽度，尽量点在抽屉右侧、聊天区中部
        iw = cdp.eval_js("window.innerWidth", timeout=8) or 1280
        x = min(int(d["x"] + d["w"] + 60), int(iw) - 20)
        y = int(min(d["y"] + 150, d["y"] + d["h"] / 2))
        cdp.click_at(x, y)
        time.sleep(1.2)
    return cdp.eval_js(JS["editor_exposed"], timeout=8)


def new_session():
    """经历史抽屉点“新会话”，收起抽屉并校验清空；失败则继续用当前会话。"""
    try:
        for attempt in range(3):
            if not panel_ready():
                ensure_panel()
            cdp.eval_js(JS["open_history"], timeout=10)
            time.sleep(1.2)
            r = cdp.eval_js(JS["close_drawer_new_session"], timeout=10)
            time.sleep(2.0)
            dismiss_drawer()
            time.sleep(0.8)
            n_user = cdp.eval_js(JS["user_msg_count"], timeout=8) or 0
            if r == "ok" and n_user == 0 and panel_ready():
                print("[bridge] 新会话: ok")
                return
            if not panel_ready():
                ensure_panel()
        print(f"[bridge] 新会话未确认(继续当前会话): r={r}, user_msgs={n_user}")
    except Exception as e:
        print("[bridge] 新会话失败(忽略):", e)
        try:
            ensure_panel()
        except Exception:
            pass


def select_model(model_id):
    label = MODELS.get(model_id)
    if not label:
        print(f"[bridge] 未知模型 {model_id}，使用当前选中模型")
        return
    cur = (cdp.eval_js(JS["current_model"], timeout=10) or "").replace(" ", "")
    if cur.lower().startswith(label.replace(" ", "").lower()):
        return  # 已选中
    cdp.eval_js(JS["open_model_menu"], timeout=10)
    time.sleep(1.0)
    r = "not-found"
    for _ in range(3):
        r = cdp.eval_js(pick_model_js(label), timeout=10)
        if r == "ok":
            break
        cdp.eval_js(JS["open_model_menu"], timeout=10)
        time.sleep(1.2)
    time.sleep(1.0)
    cur = (cdp.eval_js(JS["current_model"], timeout=10) or "").replace(" ", "")
    print(f"[bridge] 选模型 {model_id} -> {label}: {r}, 当前: {cur}")
    if r != "ok":
        # 收起可能还开着的菜单，避免遮挡
        cdp.eval_js(JS["open_model_menu"], timeout=8)
        time.sleep(0.8)
        raise RuntimeError(f"无法选择模型 {label}")


def _content_text(content):
    """OpenAI content 可能是字符串或多模态 part 列表，统一取纯文本。"""
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        parts = []
        for p in content:
            if isinstance(p, str):
                parts.append(p)
            elif isinstance(p, dict):
                if p.get("type") == "text" and isinstance(p.get("text"), str):
                    parts.append(p["text"])
                elif isinstance(p.get("text"), str):
                    parts.append(p["text"])
        return "\n".join(parts)
    return str(content) if content is not None else ""


def build_prompt(messages):
    """把 OpenAI messages 数组压成单条 prompt（Agent 面板是单输入框）。
    注意不要用“系统设定”等字眼——内置模型会把外部指令当提示注入拒绝。"""
    turns = [{"role": m.get("role"), "content": _content_text(m.get("content"))}
             for m in messages if m.get("role") in ("user", "assistant")]
    turns = [m for m in turns if m["content"].strip()]
    if not turns:
        return "你好"
    last_user = next((m for m in reversed(turns) if m["role"] == "user"), turns[-1])
    history = [m for m in turns[:-1]] if len(turns) > 1 else []
    if not history and last_user is turns[-1] and len(turns) == 1:
        return turns[0]["content"]
    lines = ["（以下是此前的对话记录，供你参考）"]
    for m in history:
        who = "用户" if m["role"] == "user" else "助手"
        lines.append(f"{who}: {m['content']}")
    lines.append("（以上是历史记录）")
    lines.append("")
    lines.append(last_user["content"])
    return "\n".join(lines)


REQUEST_LOCK = threading.Lock()   # OrcaTerm 面板同一时刻只跑一个对话


def ask_orcaterm(messages, model_id):
    with REQUEST_LOCK:
        last_err = None
        for attempt in range(3):
            try:
                return _ask_orcaterm(messages, model_id)
            except (RuntimeError, TimeoutError) as e:
                last_err = e
                print(f"[bridge] 第 {attempt+1} 次尝试失败: {e}；重置面板后重试…")
                try:
                    cdp.eval_js(JS["open_panel"], timeout=8)
                except Exception:
                    pass
                time.sleep(2)
        raise last_err


def _ask_orcaterm(messages, model_id):
    ensure_orcaterm()
    ensure_panel()
    new_session()
    select_model(model_id)

    prompt = build_prompt(messages)

    # 真实点击输入框（新会话后编辑器可能被左菜单短暂遮挡，网格找可点区域 + 多试几次）
    focused = False
    for _ in range(4):
        raw = cdp.eval_js(JS["editor_click_point"], timeout=10)
        if not raw or raw == "null":
            time.sleep(1.0)
            continue
        pos = json.loads(raw)
        cdp.click_at(pos["x"], pos["y"])
        time.sleep(0.5)
        if cdp.eval_js(JS["active_is_editor"], timeout=8):
            focused = True
            break
    if not focused:
        raise RuntimeError("点击后焦点未进入输入框")
    # Ctrl+A 全选（覆盖残留内容），再输入
    cdp.key_combo("a", "KeyA", 65, [2])
    time.sleep(0.2)
    cdp.insert_text(prompt)
    time.sleep(0.8)
    text = (cdp.eval_js(JS["editor_text"], timeout=10) or "").strip()
    if text.strip() != prompt.strip():
        raise RuntimeError(f"输入注入失败(内容不匹配): {text[:60]!r}...")
    # 真实点击发送按钮（若编辑器内容未被清掉说明没发出去，换点重试）
    sent = False
    for si in range(4):
        spos_raw = cdp.eval_js(JS["send_btn_center"], timeout=10)
        if not spos_raw or spos_raw == "null":
            break
        spos = json.loads(spos_raw)
        cdp.click_at(spos["x"], spos["y"])
        time.sleep(1.5)
        cur = (cdp.eval_js(JS["editor_text"], timeout=8) or "").strip()
        if cur != prompt.strip():
            sent = True
            break
        print(f"[bridge] 发送点击疑似未生效({si+1}/4)，重试…")
    if not sent:
        print("[bridge] 发送状态未确认，继续等待新消息（可能已发出）")

    # 1) 先等“新的”assistant 消息出现（避免读到上一轮的完成标记）
    before_user = cdp.eval_js(JS["user_msg_count"], timeout=10) or 0
    before_asst = cdp.eval_js(JS["assistant_msg_count"], timeout=10) or 0
    new_msg_deadline = time.time() + 30
    saw_new = False
    while time.time() < new_msg_deadline:
        time.sleep(1.5)
        try:
            n = cdp.eval_js(JS["assistant_msg_count"], timeout=8) or 0
        except Exception:
            continue
        if n > before_asst:
            saw_new = True
            break
        n_user = cdp.eval_js(JS["user_msg_count"], timeout=8) or 0
        if n_user <= before_user and time.time() > new_msg_deadline - 20:
            print("[bridge] 警告: 消息可能未发出，继续等待…")
    if not saw_new:
        raise TimeoutError("发送后未生成新的回答消息（可能未发出）")

    # 2) 再等这条新消息完成
    deadline = time.time() + REPLY_TIMEOUT
    while time.time() < deadline:
        time.sleep(2.5)
        try:
            state = cdp.eval_js(JS["reply_state"], timeout=10)
        except Exception:
            state = "streaming"
        if state == "done":
            time.sleep(0.8)
            answer = cdp.eval_js(JS["extract_answer"], timeout=15) or ""
            if answer:
                return answer
    raise TimeoutError(f"等待 OrcaTerm 回答超时({REPLY_TIMEOUT}s)")


# ---------- OpenAI 兼容 HTTP 服务 ----------

class Handler(BaseHTTPRequestHandler):
    server_version = "OrcatermBridge/1.0"

    def log_message(self, fmt, *args):
        print("[http]", self.address_string(), fmt % args)

    def _json(self, code, obj):
        body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _sse(self, obj):
        self.wfile.write(b"data: " + json.dumps(obj, ensure_ascii=False).encode("utf-8") + b"\n\n")
        self.wfile.flush()

    def do_GET(self):
        if self.path.rstrip("/") in ("/v1/models", ""):
            now = int(time.time())
            data = [{"id": mid, "object": "model", "created": now, "owned_by": "orcaterm",
                     "permission": [], "root": mid, "parent": None}
                    for mid in MODELS]
            self._json(200, {"object": "list", "data": data})
        elif self.path.rstrip("/") == "/health":
            ok = cdp.alive()
            self._json(200, {"ok": ok, "orcaterm_cdp": ok})
        else:
            self._json(404, {"error": {"message": "not found"}})

    def do_POST(self):
        if self.path.split("?")[0] != "/v1/chat/completions":
            self._json(404, {"error": {"message": "not found"}})
            return
        try:
            length = int(self.headers.get("Content-Length", 0))
            req = json.loads(self.rfile.read(length) or b"{}")
        except Exception as e:
            self._json(400, {"error": {"message": f"bad json: {e}"}})
            return

        # 本桥接只监听 127.0.0.1，不校验 API Key（Trae 里随便填即可）
        model_id = req.get("model") or DEFAULT_MODEL
        if model_id not in MODELS:
            model_id = DEFAULT_MODEL
        messages = req.get("messages") or [{"role": "user", "content": "hi"}]
        stream = bool(req.get("stream"))
        cid = f"chatcmpl-{uuid.uuid4().hex[:24]}"
        created = int(time.time())

        print(f"[bridge] 请求 model={model_id} stream={stream} msgs={len(messages)}")
        try:
            answer = ask_orcaterm(messages, model_id)
        except Exception as e:
            self._json(502, {"error": {"message": f"OrcaTerm 调用失败: {e}",
                                       "type": "bridge_error"}})
            return

        if stream:
            self.send_response(200)
            self.send_header("Content-Type", "text/event-stream; charset=utf-8")
            self.send_header("Cache-Control", "no-cache")
            self.end_headers()
            def chunk(delta):
                return {"id": cid, "object": "chat.completion.chunk", "created": created,
                        "model": model_id,
                        "choices": [{"index": 0, "delta": delta, "finish_reason": None}]}
            self._sse(chunk({"role": "assistant", "content": ""}))
            # 按小段流式吐出（桥接本身是“整段后发”）
            piece = 48
            for i in range(0, len(answer), piece):
                self._sse(chunk({"content": answer[i:i + piece]}))
            self._sse({"id": cid, "object": "chat.completion.chunk", "created": created,
                       "model": model_id,
                       "choices": [{"index": 0, "delta": {}, "finish_reason": "stop"}]})
            self.wfile.write(b"data: [DONE]\n\n")
            self.wfile.flush()
        else:
            self._json(200, {
                "id": cid, "object": "chat.completion", "created": created,
                "model": model_id,
                "choices": [{"index": 0,
                             "message": {"role": "assistant", "content": answer},
                             "finish_reason": "stop"}],
                "usage": {"prompt_tokens": 0, "completion_tokens": 0, "total_tokens": 0},
            })


def main():
    ensure_orcaterm()
    print(f"[bridge] OrcaTerm CDP 就绪 (port {CDP_PORT})")
    srv = ThreadingHTTPServer((LISTEN_HOST, LISTEN_PORT), Handler)
    print(f"[bridge] OpenAI 兼容服务: http://{LISTEN_HOST}:{LISTEN_PORT}/v1")
    print(f"[bridge] 模型: {', '.join(MODELS)}")
    srv.serve_forever()


if __name__ == "__main__":
    main()
