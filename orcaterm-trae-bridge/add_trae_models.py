# -*- coding: utf-8 -*-
"""
把 OrcaTerm 内置模型写入 Trae CN 的自定义模型列表（state.vscdb）。

前提：Trae CN 必须处于关闭状态（否则退出时会覆盖本脚本写入的数据）。
用法：python add_trae_models.py [--remove]
  默认添加 8 个模型；--remove 删除本脚本添加的模型（按 name 前缀 orcaterm/ 识别）。
"""
import copy
import json
import os
import shutil
import sqlite3
import sys
import tempfile
import time

DB = os.path.expandvars(r"%APPDATA%\Trae CN\User\globalStorage\state.vscdb")
KEY = "375120640_AI.agent.model.model_list_map"
NAME_PREFIX = "custom_openai_compatible//orcaterm/"
BASE_URL = "http://127.0.0.1:8317/v1/chat/completions"

# (model id -> 显示名)
MODELS = {
    "hy4-preview":       "OrcaTerm Hy4-preview",
    "hy3":               "OrcaTerm Hy3",
    "kimi-k3":           "OrcaTerm Kimi-K3",
    "glm-5.3-flash":     "OrcaTerm GLM-5.3-Flash",
    "glm-5.3":           "OrcaTerm GLM-5.3",
    "glm-5.2":           "OrcaTerm GLM-5.2",
    "deepseek-v4-flash": "OrcaTerm DeepSeek-V4-Flash",
    "deepseek-v4-pro":   "OrcaTerm DeepSeek-V4-Pro",
}


def make_entry(template, model_id, display, seq):
    e = copy.deepcopy(template)
    e["name"] = NAME_PREFIX + model_id
    e["display_name"] = display
    e["base_url"] = BASE_URL
    e["ak"] = None                      # 桥接不校验 Key
    e["sk"] = None
    e["is_default"] = False
    e["status"] = True
    e["selectable"] = True
    e["multimodal"] = False
    e["custom_model_id"] = str(983000 + seq)   # 本地自定义条目的唯一 id
    e["custom_model_type"] = ""
    return e


def main():
    remove = "--remove" in sys.argv
    if not os.path.exists(DB):
        print("找不到 Trae 数据库:", DB)
        return 1
    # 备份
    bak = DB + f".bak-orcaterm-{time.strftime('%Y%m%d-%H%M%S')}"
    shutil.copy2(DB, bak)
    print("已备份到:", bak)

    tmp = os.path.join(tempfile.gettempdir(), "trae_state_write.db")
    shutil.copy2(DB, tmp)
    con = sqlite3.connect(tmp)
    row = con.execute("SELECT value FROM ItemTable WHERE key=?", (KEY,)).fetchone()
    if not row:
        print("未找到 key:", KEY)
        return 1
    data = json.loads(row[0])
    template = None
    for mode, models in data.items():
        if isinstance(models, list):
            for m in models:
                if m.get("name") == "custom_openai_compatible//gpt-5.6-sol":
                    template = m
                    break
        if template:
            break
    if template is None:
        print("未找到可克隆的自定义模型模板(custom_openai_compatible//gpt-5.6-sol)")
        return 1

    changed_modes = []
    for mode, models in data.items():
        if not isinstance(models, list):
            continue
        if not any(isinstance(m, dict) and m.get("provider") == "custom_openai_compatible"
                   for m in models):
            continue  # 只往已有自定义模型的模式里加
        before = len(models)
        if remove:
            models[:] = [m for m in models
                         if not (isinstance(m, dict) and str(m.get("name", "")).startswith(NAME_PREFIX))]
        else:
            existing = {m.get("name") for m in models if isinstance(m, dict)}
            seq = 1
            for mid, disp in MODELS.items():
                name = NAME_PREFIX + mid
                if name not in existing:
                    models.append(make_entry(template, mid, disp, seq))
                seq += 1
        if len(models) != before:
            changed_modes.append(mode)

    if not changed_modes and not remove:
        print("没有需要新增的条目（可能已存在）")
    con.execute("UPDATE ItemTable SET value=? WHERE key=?", (json.dumps(data, ensure_ascii=False), KEY))
    con.commit()
    con.close()
    shutil.copy2(tmp, DB)
    print("完成。变更的模式:", ", ".join(changed_modes) or "(无)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
