import subprocess, json, os

def run_list(folder_token, page_token=""):
    params = {"folder_token": folder_token, "page_size": 200}
    if page_token:
        params["page_token"] = page_token
    r = subprocess.run(
        ['lark-cli', 'drive', 'files', 'list', '--params', json.dumps(params), '--format', 'json'],
        capture_output=True, text=True, encoding='utf-8'
    )
    if r.returncode != 0:
        return None, (r.stdout + r.stderr).strip()
    out = r.stdout.strip()
    # 若输出是 saved_path 信封，读取落盘文件
    try:
        env = json.loads(out)
        if isinstance(env, dict) and 'saved_path' in env:
            sp = env['saved_path']
            if os.path.exists(sp):
                with open(sp, 'r', encoding='utf-8') as fp:
                    out = fp.read()
            else:
                return None, f"saved_path missing: {sp}"
    except Exception:
        pass
    try:
        return json.loads(out), None
    except Exception as e:
        return None, f"parse error: {e}\n{out[:300]}"

results = []
queue = [{"folder_token": "JeTAfiyp3loQr1dfKdkc1Hi2nbb", "path": "learning-docs"}]
visited = set()
errs = []

while queue:
    item = queue.pop(0)
    ft = item["folder_token"]
    path = item["path"]
    page_token = ""
    while True:
        key = (ft, page_token or "first")
        if key in visited:
            break
        visited.add(key)
        data, err = run_list(ft, page_token)
        if err:
            errs.append(f"{path} :: {err[:200]}")
            break
        if not data or data.get("code") != 0:
            errs.append(f"{path} :: bad resp code {str(data)[:200]}")
            break
        files = data.get("data", {}).get("files", [])
        for f in files:
            rec = {
                "path": path + "/" + f.get("name", ""),
                "name": f.get("name", ""),
                "token": f.get("token", ""),
                "type": f.get("type", ""),
                "created": f.get("created_time", ""),
                "modified": f.get("modified_time", ""),
                "url": f.get("url", ""),
            }
            results.append(rec)
            if f.get("type") == "folder":
                queue.append({"folder_token": f.get("token"), "path": path + "/" + f.get("name", "")})
        if data.get("data", {}).get("has_more"):
            npt = data.get("data", {}).get("next_page_token", "")
            if not npt:
                errs.append(f"{path} :: pagination blocker")
                break
            page_token = npt
        else:
            break

print("TOTAL_ITEMS:", len(results))
print("FOLDERS:", len([r for r in results if r["type"] == "folder"]))
print("FILES:", len([r for r in results if r["type"] != "folder"]))
print("ERRORS:", len(errs))
for e in errs[:10]:
    print("ERR:", e)

with open(r'D:\Projects\Persion\ai-customer-service\_feishu_inventory.json', 'w', encoding='utf-8') as fp:
    json.dump(results, fp, ensure_ascii=False, indent=1)
print("saved inventory")
