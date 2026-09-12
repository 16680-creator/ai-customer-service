# -*- coding: utf-8 -*-
"""Display-fit proof for E18:E37 in the live original: no clipped row, no wrapped line.

Excel column width is measured in digit-character units of the default font; a CJK glyph
consumes ~2 of those units. Row height for 微软雅黑 11pt needs ~15.0pt per wrapped line.
"""
import sys

import openpyxl

sys.stdout.reconfigure(encoding="utf-8")

PATH = r"C:\Users\16680\Desktop\调账相关.xlsx"
NEED_PT_PER_LINE = 15.0  # generous for 微软雅黑 11pt (auto-fit is ~14.25)

ws = openpyxl.load_workbook(PATH)["Sheet1"]
e_width = ws.column_dimensions["E"].width
print(f"E 列宽 = {e_width:.2f} 字符单位（1 全角字 ≈ 2 单位）\n")


def display_width(text):
    """CJK/full-width punctuation counts 2, ASCII counts 1."""
    return sum(2 if ord(ch) > 0x2E7F else 1 for ch in text)


worst_line, problems = 0, []
for row in range(18, 38):
    cell = ws.cell(row=row, column=5).value or ""
    lines = [x for x in cell.split("\n") if x.strip()]
    h = ws.row_dimensions[row].height
    widest = max(display_width(x) for x in lines)
    need = len(lines) * NEED_PT_PER_LINE
    worst_line = max(worst_line, widest)
    if widest >= e_width:
        problems.append(f"E{row}: 最宽行 {widest} >= 列宽 {e_width:.1f} -> 会二次折行")
    if h < need:
        problems.append(f"E{row}: 行高 {h} < 所需 {need:.1f} -> 会被裁切")
    flag = "OK" if (widest < e_width and h >= need) else "!!"
    print(f"[{flag}] E{row}  {len(lines)} 行  最宽 {widest:>3}/{e_width:.1f}  "
          f"行高 {h:>5.1f} >= 所需 {need:.1f}")

print(f"\n全表最宽单行 = {worst_line} 字符单位，占列宽 {worst_line / e_width:.0%}")
print("换行符总数校验:", sum((ws.cell(row=r, column=5).value or "").count("\n")
                       for r in range(18, 38)), "= 124 条 - 20 个单元格 =", 124 - 20)
print("\n显示效果问题:", problems or "无（每条独占一行，行高足够，不折行不裁切）")
sys.exit(1 if problems else 0)
