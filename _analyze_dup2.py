import json, os
from collections import defaultdict

with open(r'D:\Projects\Persion\ai-customer-service\_feishu_inventory.json', 'r', encoding='utf-8') as fp:
    items = json.load(fp)

for it in items:
    parts = it['path'].split('/')
    it['parent'] = '/'.join(parts[:-1])
    it['name'] = parts[-1]

files = [it for it in items if it['type'] != 'folder']
folders = [it for it in items if it['type'] == 'folder']

print("=== 文件夹清单 ===")
for f in sorted(folders, key=lambda x: x['path']):
    print(f"  {f['path']}  token={f['token']}")

print("\n=== 1. 基名相同（忽略扩展名）===")
base_groups = defaultdict(list)
for f in files:
    base = os.path.splitext(f['name'])[0]
    base_groups[(f['parent'], base)].append(f)
for (parent, base), lst in sorted(base_groups.items()):
    if len(lst) > 1:
        print(f"\n[{parent}] 基名: {base}")
        for it in lst:
            print(f"  token={it['token']} name={it['name']} modified={it['modified']}")

print("\n=== 2. 全库同名文件（含不同目录）===")
name_groups = defaultdict(list)
for f in files:
    name_groups[f['name']].append(f)
for name, lst in sorted(name_groups.items()):
    if len(lst) > 1:
        print(f"\n{name}  x{len(lst)}")
        for it in lst:
            print(f"  {it['path']}  token={it['token']} modified={it['modified']}")

print("\n=== 3. 可疑历史遗留：10-项目开发计划 ===")
for f in files:
    if '项目开发计划' in f['path'] or '项目开发' in f['path']:
        print(f"  {f['path']}  token={f['token']} modified={f['modified']}")
for fd in folders:
    if '项目开发' in fd['path']:
        print(f"  FOLDER {fd['path']}  token={fd['token']}")

print("\n=== 4. 00-学习路线总览 下的文件（历史迁移过）===")
for f in files:
    if f['parent'] == 'learning-docs/00-学习路线总览':
        print(f"  {f['name']}  token={f['token']} modified={f['modified']}")

print("\n=== 5. 文件名相似度检查（去掉序号前缀）===")
def strip_prefix(name):
    import re
    n = re.sub(r'^\d+[-_]', '', name)
    return os.path.splitext(n)[0]

norm_groups = defaultdict(list)
for f in files:
    norm_groups[(f['parent'], strip_prefix(f['name']))].append(f)
for (parent, key), lst in sorted(norm_groups.items()):
    if len(lst) > 1 and key:
        print(f"\n[{parent}] {key}")
        for it in lst:
            print(f"  token={it['token']} name={it['name']} modified={it['modified']}")
