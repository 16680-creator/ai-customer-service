#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
知识库(kb_document) -> Elasticsearch 全文索引 手动同步脚本
==========================================================
原理: 从知识服务(ai-cs-knowledge)分页拉取 kb_document，按文档ID写入 ES 指定索引。
      ES _id 使用知识文档ID，重复执行会覆盖同ID文档，不会产生重复数据。

用法:
  python sync-kb-to-es.py                                          # 默认本地
  python sync-kb-to-es.py --recreate                               # 重建索引(ngram中文分词)后再同步
  python sync-kb-to-es.py --gateway http://123.60.31.79:8080/api   # 指定远程网关

参数:
  --gateway   网关 API 地址，默认 http://localhost:8080/api
  --es        Elasticsearch 地址，默认 http://127.0.0.1:9200
  --index     目标索引名，默认 knowledge
  --username  登录用户名，默认 admin
  --password  登录密码，默认 admin123
  --recreate  重建索引：删除旧索引，用 ngram 中文分词映射新建后同步（推荐，搜索中文更准）
"""
import argparse
import json
import sys
import urllib.request
import urllib.error

def http(method, url, body=None, token=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            raw = resp.read().decode("utf-8")
            return resp.status, json.loads(raw) if raw else None
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", errors="replace")

NGRAM_MAPPING = {
    "settings": {
        "index": {"max_ngram_diff": 50},
        "analysis": {
            "analyzer": {"ngram_analyzer": {"tokenizer": "ngram_tokenizer"}},
            "tokenizer": {"ngram_tokenizer": {
                "type": "ngram", "min_gram": 2, "max_gram": 20,
                "token_chars": ["letter", "digit"]}}
        }
    },
    "mappings": {
        "properties": {
            "title":      {"type": "text", "analyzer": "ngram_analyzer"},
            "content":    {"type": "text", "analyzer": "ngram_analyzer"},
            "summary":    {"type": "text", "analyzer": "ngram_analyzer"},
            "tags":       {"type": "text", "analyzer": "ngram_analyzer"},
            "docType":    {"type": "keyword"},
            "categoryId": {"type": "long"},
            "status":     {"type": "integer"}
        }
    }
}

def ensure_index(es, index, recreate):
    code, _ = http("HEAD", "%s/%s" % (es, index))
    exists = code == 200
    if exists and recreate:
        http("DELETE", "%s/%s" % (es, index))
        print("[ES] 已删除旧索引: %s" % index)
        exists = False
    if not exists:
        code, body = http("PUT", "%s/%s" % (es, index), NGRAM_MAPPING)
        if code in (200, 400):
            # 400 = 已存在(并发), 忽略
            print("[ES] 索引就绪: %s (ngram 中文分词)" % index)
        else:
            raise SystemExit("[ES] 创建索引失败: %s %s" % (code, str(body)[:300]))
    else:
        print("[ES] 索引已存在(未重建): %s" % index)

def main():
    p = argparse.ArgumentParser(description="知识库 -> ES 手动同步")
    p.add_argument("--gateway", default="http://localhost:8080/api")
    p.add_argument("--es", default="http://127.0.0.1:9200")
    p.add_argument("--index", default="knowledge")
    p.add_argument("--username", default="admin")
    p.add_argument("--password", default="admin123")
    p.add_argument("--recreate", action="store_true", help="重建索引(ngram)后再同步")
    args = p.parse_args()

    # 1. 登录拿 token
    code, login = http("POST", args.gateway + "/user/login",
                       {"username": args.username, "password": args.password})
    if code != 200 or not (login or {}).get("success"):
        raise SystemExit("登录失败: %s %s" % (code, str(login)[:200]))
    token = login["data"]["token"]
    print("[AUTH] 登录成功: %s" % args.username)

    # 2. 确保索引存在
    ensure_index(args.es, args.index, args.recreate)

    # 3. 分页拉取知识库文档并写入 ES
    total_indexed = 0
    page = 1
    page_size = 100
    while True:
        code, resp = http("GET", "%s/knowledge/list?page=%d&pageSize=%d" % (args.gateway, page, page_size), token=token)
        if code != 200:
            raise SystemExit("拉取知识库失败: %s %s" % (code, str(resp)[:200]))
        records = (resp or {}).get("data", {}).get("records", [])
        if not records:
            break
        for d in records:
            doc = {
                "title": d.get("title") or "",
                "content": d.get("content") or "",
                "summary": d.get("summary") or "",
                "tags": d.get("tags") or "",
                "docType": d.get("docType") or "",
                "categoryId": d.get("categoryId"),
                "status": d.get("status"),
            }
            code2, _ = http("POST", "%s/%s/_doc/%s?refresh=wait_for" % (args.es, args.index, d["id"]), doc)
            if code2 not in (200, 201):
                print("[FAIL] id=%s -> %s %s" % (d["id"], code2, _))
            else:
                total_indexed += 1
        print("[SYNC] 第%d页: 同步 %d 篇 (累计 %d)" % (page, len(records), total_indexed))
        page += 1

    print("=" * 50)
    print("同步完成: 共写入 %d 篇文档到 ES 索引 [%s]" % (total_indexed, args.index))
    print("验证: curl -X POST '%s/%s/_search' -H 'Content-Type: application/json' -d '{\"query\":{\"match_all\":{}}}'" % (args.es, args.index))

if __name__ == "__main__":
    main()