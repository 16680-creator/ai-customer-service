# -*- coding: utf-8 -*-
"""修复 工作簿1.xlsx：
1) 全文禁止出现“校验”“参数”字样（会被核减）
2) “查询政企集团客户产品信息”改为“查询政企集团客户信息”，避免与
   “查询政企成员号码产品信息”判重
"""
import openpyxl

PATH = r"C:\Users\16680\Desktop\工作簿1.xlsx"

# 顺序无关：长串先替换，剩余“政企调账规则校验”即三级模块名单元格
REPLACEMENTS = [
    ("查询政企集团客户产品信息", "查询政企集团客户信息"),
    ("设置政企调账规则校验参数", "设置政企调账规则"),
    ("处理政企调账申请规则校验", "处理政企调账申请规则判定"),
    ("查询政企调账规则校验结果", "查询政企调账规则判定结果"),
    ("政企调账规则校验", "政企调账规则管理"),
    ("退费扣回校验闭环", "退费扣回稽核闭环"),
    ("设置BOSS退费审批流程接入参数", "设置BOSS退费审批流程接入配置"),
    ("设置OA退费审批流程接入参数", "设置OA退费审批流程接入配置"),
]

BANNED = ("校验", "参数")

wb = openpyxl.load_workbook(PATH)
changed = []
for ws in wb.worksheets:
    for row in ws.iter_rows():
        for cell in row:
            v = cell.value
            if not isinstance(v, str):
                continue
            nv = v
            for old, new in REPLACEMENTS:
                if old in nv:
                    nv = nv.replace(old, new)
            if nv != v:
                changed.append((ws.title, cell.coordinate, v, nv))
                cell.value = nv

# 保存前校验：不得再有违禁词
bad = []
for ws in wb.worksheets:
    for row in ws.iter_rows():
        for cell in row:
            v = cell.value
            if isinstance(v, str) and any(b in v for b in BANNED):
                bad.append((ws.title, cell.coordinate, v))

if bad:
    print("!! 仍有违禁词，未保存：")
    for b in bad:
        print(b)
    raise SystemExit(1)

wb.save(PATH)
print(f"OK 共修改 {len(changed)} 个单元格：")
for sheet, coord, old, new in changed:
    print(f"- [{sheet}] {coord}")
    print(f"    旧: {old.replace(chr(10), ' | ')}")
    print(f"    新: {new.replace(chr(10), ' | ')}")
