# -*- coding: utf-8 -*-
"""Probe styles of the 调账 sheet so the filled E cells match the existing table look."""
import sys
import openpyxl

sys.stdout.reconfigure(encoding="utf-8")

SRC = r"C:\Users\16680\Desktop\调账相关.xlsx"
wb = openpyxl.load_workbook(SRC)
ws = wb["Sheet1"]

for coord in ("A1", "C1", "D1", "E1", "F1", "C18", "D18", "E18", "F18", "G18",
              "C37", "E37", "F37", "C59", "D59"):
    c = ws[coord]
    al, f, b, fl = c.alignment, c.font, c.border, c.fill
    print(f"{coord}: value={c.value!r}")
    print(f"    font name={f.name} size={f.size} bold={f.bold} color={f.color.rgb if f.color else None}")
    print(f"    align h={al.horizontal} v={al.vertical} wrap={al.wrap_text} indent={al.indent}")
    print(f"    border L={b.left.style} R={b.right.style} T={b.top.style} B={b.bottom.style}")
    print(f"    fill type={fl.patternType} fg={fl.fgColor.rgb if fl.fgColor else None}")
    print(f"    number_format={c.number_format!r}")
