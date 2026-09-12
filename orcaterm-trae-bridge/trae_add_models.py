# -*- coding: utf-8 -*-
"""通过 CDP 驱动 Trae CN 的 设置→模型管理→添加模型(自定义)，每加一个模型前刷新页面，保证状态干净。"""
import json, sys, time, urllib.request
import websocket

CDP = "http://127.0.0.1:9223"
BASE_URL = "http://127.0.0.1:8317/v1"
API_KEY = "orcaterm-local"
MODELS = [
    ("hy4-preview",        "OrcaTerm Hy4-preview"),
    ("hy3",                "OrcaTerm Hy3"),
    ("kimi-k3",            "OrcaTerm Kimi-K3"),
    ("glm-5.3-flash",      "OrcaTerm GLM-5.3-Flash"),
    ("glm-5.2",            "OrcaTerm GLM-5.2"),
    ("deepseek-v4-flash",  "OrcaTerm DeepSeek-V4-Flash"),
    ("deepseek-v4-pro",    "OrcaTerm DeepSeek-V4-Pro"),
]


def connect():
    targets = json.load(urllib.request.urlopen(CDP + "/json/list"))
    main = [t for t in targets if t.get("type") == "page"
            and "dynamic-island" not in t.get("url", "")]
    ws = websocket.create_connection(main[0]["webSocketDebuggerUrl"],
                                     timeout=300, suppress_origin=True)
    ws.settimeout(0.8)
    return ws


class C:
    def __init__(self):
        self.ws = connect()
        self.mid = 0

    def send(self, method, params=None):
        self.mid += 1
        self.ws.send(json.dumps({"id": self.mid, "method": method,
                                 "params": params or {}}))
        return self.mid

    def drain(self, sec):
        end = time.time() + sec
        while time.time() < end:
            try:
                self.ws.recv()
            except websocket.WebSocketTimeoutException:
                continue
            except Exception:
                break

    def ev(self, expr, timeout=15):
        i = self.send("Runtime.evaluate",
                      {"expression": expr, "returnByValue": True})
        end = time.time() + timeout
        while time.time() < end:
            try:
                r = json.loads(self.ws.recv())
            except websocket.WebSocketTimeoutException:
                continue
            except Exception:
                return None
            if r.get("id") == i:
                res = r.get("result", {})
                if "exceptionDetails" in res:
                    return None
                return res.get("result", {}).get("value")
        return None

    def click(self, x, y):
        for t in ("mousePressed", "mouseReleased"):
            self.send("Input.dispatchMouseEvent",
                      {"type": t, "x": int(x), "y": int(y),
                       "button": "left", "clickCount": 1})

    def reload(self):
        self.send("Page.enable")
        self.send("Page.reload")
        for _ in range(40):
            self.drain(2)
            if self.ev("document.readyState==='complete' && document.querySelectorAll('button').length>3"):
                return True
        return False


c = C()


def nav_to_models():
    """从刷新后的设置页进入 模型管理。"""
    for attempt in range(4):
        if c.ev("""(()=>{const bs=[...document.querySelectorAll('button')]
          .filter(b=>/添加模型/.test((b.textContent||'').trim()));
          return bs.some(b=>{const r=b.getBoundingClientRect();
            if(!(r.width>0)) return false;
            const at=document.elementFromPoint(r.x+r.width/2, r.y+r.height/2);
            return !!(at && (at===b || b.contains(at)));});})()"""):
            return True
        # 展开设置导航
        pos = c.ev("""(()=>{const b=document.querySelector('.icube-settings-sidebar-nav-button-container button');
          if(!b) return null; const r=b.getBoundingClientRect();
          return JSON.stringify({x:Math.round(r.x+r.width/2), y:Math.round(r.y+r.height/2)});})()""")
        if pos:
            p = json.loads(pos)
            c.click(p["x"], p["y"])
            c.drain(1.3)
        # 点 模型 导航项（要求命中测试通过）
        pos = c.ev("""(()=>{const els=[...document.querySelectorAll('span.text')]
          .filter(e=>(e.textContent||'').trim()==='模型');
          for(const e of els){const r=e.getBoundingClientRect();
            if(!(r.width>0 && r.height>0 && r.x<620 && r.y>60 && r.y<900)) continue;
            const at=document.elementFromPoint(r.x+r.width/2, r.y+r.height/2);
            if(at===e || e.contains(at) || at.contains(e))
              return JSON.stringify({x:Math.round(r.x+r.width/2), y:Math.round(r.y+r.height/2)});}
          return null;})()""")
        if pos:
            p = json.loads(pos)
            c.click(p["x"], p["y"])
            c.drain(2.5)
    return False


def url_field_present():
    return c.ev("""(()=>{const f=document.querySelector('.icd-modal-body input[placeholder=\"例如 https://api.openai.com/v1\"]');
      if(!f) return false; const r=f.getBoundingClientRect();
      const at=document.elementFromPoint(r.x+20, r.y+r.height/2);
      return !!(at && (at===f || f.contains(at) || at.contains(f)));})()""") or False


def open_dialog():
    for _ in range(3):
        pos = c.ev("""(()=>{const bs=[...document.querySelectorAll('button')]
          .filter(b=>/添加模型/.test((b.textContent||'').trim()));
          for(const b of bs){const r=b.getBoundingClientRect();
            if(!(r.width>0)) continue;
            const at=document.elementFromPoint(r.x+r.width/2, r.y+r.height/2);
            if(at && (at===b || b.contains(at)))
              return JSON.stringify({x:Math.round(r.x+r.width/2), y:Math.round(r.y+r.height/2)});}
          return null;})()""")
        if not pos:
            return False
        p = json.loads(pos)
        c.click(p["x"], p["y"])
        for _ in range(10):
            c.drain(1)
            if url_field_present():
                return True
        c.send("Input.dispatchKeyEvent", {"type": "rawKeyDown", "key": "Escape",
                                          "code": "Escape", "windowsVirtualKeyCode": 27})
        c.send("Input.dispatchKeyEvent", {"type": "keyUp", "key": "Escape",
                                          "code": "Escape", "windowsVirtualKeyCode": 27})
        c.drain(1.5)
    return False


def fill_field(ph, text, tries=4):
    val = None
    for t in range(tries):
        pos = c.ev(f"""(()=>{{const f=[...document.querySelectorAll('.icd-modal-body input,.icd-modal-body textarea')]
          .find(f=>f.placeholder==={json.dumps(ph)}); if(!f) return null;
          f.scrollIntoView({{block:'center'}});
          const r=f.getBoundingClientRect();
          const at=document.elementFromPoint(r.x+Math.min(40,r.width/2), r.y+r.height/2);
          if(!(at && (at===f || f.contains(at)))) return null;
          return JSON.stringify({{x:Math.round(r.x+Math.min(40,r.width/2)), y:Math.round(r.y+r.height/2)}});}})()""")
        if not pos:
            c.drain(0.5)
            continue
        p = json.loads(pos)
        c.click(p["x"], p["y"])
        c.drain(0.5)
        focused = c.ev(f"""(()=>{{const f=[...document.querySelectorAll('.icd-modal-body input,.icd-modal-body textarea')]
          .find(f=>f.placeholder==={json.dumps(ph)}); return !!(f && document.activeElement===f);}})()""")
        if not focused:
            continue
        c.send("Input.dispatchKeyEvent", {"type": "rawKeyDown", "modifiers": 2,
                                          "key": "a", "code": "KeyA",
                                          "windowsVirtualKeyCode": 65})
        c.send("Input.dispatchKeyEvent", {"type": "keyUp", "modifiers": 2,
                                          "key": "a", "code": "KeyA",
                                          "windowsVirtualKeyCode": 65})
        c.drain(0.1)
        c.send("Input.insertText", {"text": text})
        c.drain(0.4)
        val = c.ev(f"""(()=>{{const f=[...document.querySelectorAll('.icd-modal-body input,.icd-modal-body textarea')]
          .find(f=>f.placeholder==={json.dumps(ph)}); return f? (f.value||''):'';}})()""")
        if val == text:
            return "ok"
    return f"MISMATCH({val!r})"


def click_dialog_btn(label):
    pos = c.ev(f"""(()=>{{const bs=[...document.querySelectorAll('.icd-modal-body button')]
      .filter(b=>(b.textContent||'').trim()==={json.dumps(label)} && b.getBoundingClientRect().width>0);
      if(!bs.length) return null; const b=bs[0]; const r=b.getBoundingClientRect();
      const at=document.elementFromPoint(r.x+r.width/2, r.y+r.height/2);
      if(!(at && (at===b || b.contains(at)))) return null;
      return JSON.stringify({{x:Math.round(r.x+r.width/2), y:Math.round(r.y+r.height/2)}});}})()""")
    if not pos:
        return False
    p = json.loads(pos)
    c.click(p["x"], p["y"])
    return True


def add_one(mid_, disp):
    if not url_field_present():
        if not open_dialog():
            return "NO_DIALOG"
    r1 = fill_field("例如 https://api.openai.com/v1", BASE_URL)
    r2 = fill_field("输入模型 ID", mid_)
    r3 = fill_field("请输入模型展示名称", disp)
    r4 = fill_field("请输入 API Key", API_KEY)
    print("   fill:", r1, r2, r3, r4)
    if "ok" not in (r1, r2):
        return "FILL_FAIL"
    # 提交：先试 直接保存（无测试），否则 添加模型（带连通性测试）
    submitted = "direct" if click_dialog_btn("直接保存") else None
    if not submitted:
        if not click_dialog_btn("添加模型"):
            return "NO_SUBMIT"
        for _ in range(50):
            c.drain(2)
            if not c.ev("!!document.querySelector('.icd-modal-body')"):
                submitted = "test-passed"
                break
            if click_dialog_btn("直接保存"):
                submitted = "direct-after-test"
                break
    c.drain(2.5)
    modal_gone = not c.ev("!!document.querySelector('.icd-modal-body')")
    listed = c.ev(f"document.body.innerText.includes({json.dumps(disp)})")
    return f"{submitted} modalGone={modal_gone} listed={listed}"


def main():
    only = sys.argv[1:] if len(sys.argv) > 1 else None
    for mid_, disp in MODELS:
        if only and mid_ not in only:
            continue
        already = c.ev(f"document.body.innerText.includes({json.dumps(disp)})")
        if already:
            print(f"== {mid_}: 已存在，跳过")
            continue
        print(f"== {mid_}")
        c.reload()
        if not nav_to_models():
            print("   无法进入模型管理页")
            continue
        print("  =>", add_one(mid_, disp))


if __name__ == "__main__":
    main()
