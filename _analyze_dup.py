import json
from collections import defaultdict

with open(r'D:\Projects\Persion\ai-customer-service\_feishu_inventory.json', 'r', encoding='utf-8') as fp:
    items = json.load(fp)

# 只分析文件（非 folder）
files = [it for it in items if it['type'] != 'folder']
folders = [it for it in items if it['type'] == 'folder']

# 按（父目录 + 文件名）分组
groups = defaultdict(list)
for f in files:
    # path 形如 learning-docs/04-中间件/01-Redis缓存实战.md
    parts = f['path'].split('/')
    parent = '/'.join(parts[:-1])
    name = parts[-1]
    f['parent'] = parent
    groups[(parent, name)].append(f)

print("=== 同目录同名重复（file 类型）===")
dups = {k: v for k, v in groups.items() if len(v) > 1}
print("重复组数:", len(dups))
for (parent, name), lst in sorted(dups.items()):
    print(f"\n[{parent}] {name}")
    for it in lst:
        print(f"  token={it['token']} created={it['created']} modified={it['modified']} type={it['type']}")

# 另外检查：跨目录同名（可能无问题，但列出供参考）
print("\n\n=== 跨目录同名文件（供参考，不一定重复）===")
name_groups = defaultdict(list)
for f in files:
    name_groups[f['name']].append(f['path'])
for name, paths in sorted(name_groups.items()):
    if len(paths) > 1:
        print(f"{name}:")
        for p in paths:
            print(f"    {p}")
