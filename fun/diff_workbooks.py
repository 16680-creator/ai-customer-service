# -*- coding: utf-8 -*-
"""Prove the fill is surgically minimal: diff the untouched original against the filled copy.

Any difference outside {E18:E37 value, row height 18-37, E18:E37 alignment} is a regression.
"""
import sys

import openpyxl

sys.stdout.reconfigure(encoding="utf-8")

# pristine copy taken before any write, versus the live original that is now filled
ORIG = r"C:\Users\16680\Desktop\调账相关.backup-20260905-111154.xlsx"
FILLED = r"C:\Users\16680\Desktop\调账相关.xlsx"
ROWS = range(18, 38)

a = openpyxl.load_workbook(ORIG)
b = openpyxl.load_workbook(FILLED)
wa, wb = a["Sheet1"], b["Sheet1"]

print(f"表名: {a.sheetnames} -> {b.sheetnames}")
assert a.sheetnames == b.sheetnames, "工作表清单变了"
print(f"数据区: {wa.dimensions} -> {wb.dimensions}")
assert wa.dimensions == wb.dimensions, "数据区范围变了"

ma = sorted(str(r) for r in wa.merged_cells.ranges)
mb = sorted(str(r) for r in wb.merged_cells.ranges)
print(f"合并区数量: {len(ma)} -> {len(mb)}")
assert ma == mb, f"合并区被破坏: {set(ma) ^ set(mb)}"

value_diffs, style_diffs = [], []
for r in range(1, wa.max_row + 1):
    for c in range(1, wa.max_column + 1):
        ca, cb = wa.cell(row=r, column=c), wb.cell(row=r, column=c)
        if ca.value != cb.value:
            value_diffs.append((ca.coordinate, ca.value, cb.value))
        sig = lambda x: (x.font.name, x.font.size, x.font.bold, x.number_format,
                         x.border.left.style, x.border.right.style,
                         x.border.top.style, x.border.bottom.style,
                         x.fill.patternType, x.alignment.horizontal,
                         x.alignment.vertical, x.alignment.wrap_text)
        if sig(ca) != sig(cb):
            style_diffs.append((ca.coordinate, sig(ca), sig(cb)))

print(f"\n单元格取值差异 {len(value_diffs)} 处:")
for coord, old, new in value_diffs:
    shown = (new[:34].replace("\n", " | ") + " ...") if len(new) > 40 else \
            new.replace("\n", " | ")
    print(f"  {coord}: {old!r} -> {shown}")

print(f"\n单元格样式差异 {len(style_diffs)} 处:")
for coord, old, new in style_diffs:
    changed = [f"{k}:{v}->{new[i]}" for i, (k, v) in enumerate(
        zip(("font", "size", "bold", "numfmt", "L", "R", "T", "B", "fill", "h", "v", "wrap"), old))
        if v != new[i]]
    print(f"  {coord}: " + ", ".join(changed))

# classify: everything must be inside the intended blast radius
bad = [c for c, _o, _n in value_diffs if not (c.startswith("E") and c[1:] in [str(x) for x in ROWS])]
bad += [c for c, _o, _n in style_diffs if not (c.startswith("E") and c[1:] in [str(x) for x in ROWS])]
print("\n越出 E18:E37 的意外改动:", bad or "无")

h_diff = [(r, wa.row_dimensions[r].height, wb.row_dimensions[r].height)
          for r in range(1, wa.max_row + 1)
          if wa.row_dimensions[r].height != wb.row_dimensions[r].height]
print(f"\n行高变化 {len(h_diff)} 行:")
for r, old, new in h_diff:
    print(f"  row {r}: {old} -> {new}")
outside = [r for r, _o, _n in h_diff if r not in ROWS]
print("越出 18-37 的行高改动:", outside or "无")

print("\n公式完整性核查:")
n_formula = 0
for r in ROWS:
    for c in (4, 6, 7):
        va, vb = wa.cell(row=r, column=c).value, wb.cell(row=r, column=c).value
        assert va == vb, f"{wb.cell(row=r, column=c).coordinate}: {va!r} -> {vb!r}"
    assert wb.cell(row=r, column=6).value == f"=D{r}*10000/916/2.5"
    n_formula += 1
print(f"  D 工作量 / F 公式 / G 负责人 共 {n_formula * 3} 格全部逐字相等")
assert wb["D59"].value == wa["D59"].value == "=SUM(D2:D58)", "合计公式变动"
print(f"  D59 合计公式: {wb['D59'].value}")

# openpyxl rewrites the package, so confirm no embedded feature was silently dropped
print("\n工作簿级对象完整性核查:")
pairs = {
    "defined_names": (list(a.defined_names), list(b.defined_names)),
    "条件格式": (len(wa.conditional_formatting._cf_rules), len(wb.conditional_formatting._cf_rules)),
    "数据有效性": (len(wa.data_validations.dataValidation), len(wb.data_validations.dataValidation)),
    "图片": (len(wa._images), len(wb._images)),
    "图表": (len(wa._charts), len(wb._charts)),
    "批注": (sum(1 for row in wa.iter_rows() for c in row if c.comment),
             sum(1 for row in wb.iter_rows() for c in row if c.comment)),
    "冻结窗格": (wa.freeze_panes, wb.freeze_panes),
    "自动筛选": (wa.auto_filter.ref, wb.auto_filter.ref),
    "列宽集合": (sorted(wa.column_dimensions), sorted(wb.column_dimensions)),
}
for label, (old, new) in pairs.items():
    mark = "OK" if old == new else "差异"
    print(f"  [{mark}] {label}: {old} -> {new}")
    assert old == new, f"{label} 在重写中丢失"

print("\n结论: 改动被严格限制在 E18:E37 取值、其水平对齐、以及 18-37 行高")
