#!/usr/bin/env node
/**
 * 只读对账脚本骨架（静态检查层）—— 作者 agent-213 临时脚本
 *
 * 定位：真正的对账要连 MySQL / Chroma 才能跑；但"对账脚本自己会不会静默失效"这件事
 * 可以在离线状态下先用 schema 校验掉。四类检查：
 *   1) 三条对账 SQL 引用的表 / 列是否仍存在于 deploy/mysql/init.sql
 *   2) 实体 @TableName 与实体字段 ⇄ DDL 列的双向漂移
 *   3) 向量元数据契约：写入侧 put 的键 vs 检索侧读的键；三服务 collection-name 是否一致
 *   4) 对账任务的调度存在性：@Scheduled / @XxlJob / 对账关键词命中
 *
 * 只读：仅 readFileSync + existsSync，不连库、不发 HTTP、不写任何文件。
 * 用法：node reconcile-lint-213.mjs
 */
import { readFileSync, existsSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

const HERE = dirname(fileURLToPath(import.meta.url));
const ROOT = join(HERE, '..', '..', '..');
const read = (rel) => readFileSync(join(ROOT, rel), 'utf8');

const findings = [];
const add = (level, check, detail) => findings.push({ level, check, detail });

/** 解析 CREATE TABLE 块 → Map<table, {cols:Set, pk:Set}> */
function parseDdl(sql) {
  const tables = new Map();
  const re = /CREATE TABLE (?:IF NOT EXISTS )?`?([A-Za-z0-9_]+)`?\s*\(([\s\S]*?)\n\)\s*ENGINE/g;
  let m;
  while ((m = re.exec(sql)) !== null) {
    const name = m[1];
    const cols = new Set();
    const pk = new Set();
    for (const line of m[2].split('\n')) {
      const col = /^\s{4}([a-z_0-9]+)\s+[A-Z]/.exec(line);
      if (col) cols.add(col[1]);
      const key = /^\s{4}PRIMARY KEY \(([^)]+)\)/.exec(line);
      if (key) key[1].split(',').forEach((c) => pk.add(c.trim().replace(/`/g, '')));
    }
    tables.set(name, { cols, pk });
  }
  return tables;
}

/** Java 实体 → @TableName + 实例字段（驼峰转下划线） */
function parseEntity(rel) {
  const src = read(rel);
  const table = /@TableName\("([^"]+)"\)/.exec(src)?.[1] ?? null;
  const fields = [];
  const re = /^\s{4}private (?!static)([A-Za-z<>\[\]]+) ([A-Za-z0-9_]+);/gm;
  let m;
  while ((m = re.exec(src)) !== null) {
    fields.push(m[2].replace(/[A-Z]/g, (c) => '_' + c.toLowerCase()));
  }
  return { table, fields };
}

const DDL_REL = 'deploy/mysql/init.sql';
const ALL_DDL_REL = 'deploy/mysql/all-init.sql';
const ddl = parseDdl(read(DDL_REL));
const allDdl = parseDdl(read(ALL_DDL_REL));

// ---------- 1) 三条对账 SQL 的 schema 校验 ----------
const sqlChecks = [
  {
    id: 'R1-孤儿向量',
    sql: 'SELECT id FROM kb_document WHERE id IN (:vectorDocumentIds) AND deleted = 0',
    tables: { kb_document: ['id', 'deleted'] },
  },
  {
    id: 'R2-缺向量文档',
    sql: 'SELECT id, title, update_time FROM kb_document WHERE deleted = 0 AND status IN (0, 2)',
    tables: { kb_document: ['id', 'title', 'update_time', 'deleted', 'status'] },
  },
  {
    id: 'R3-用量双写差集',
    sql: 'SELECT u.request_id FROM model_usage u LEFT JOIN llm_trace t ON t.request_id = u.request_id WHERE t.request_id IS NULL',
    tables: { model_usage: ['request_id'], llm_trace: ['request_id'] },
  },
];

const usageDdl = parseDdl(read('deploy/mysql/llm-observability-init.sql'));
const catalog = new Map([...ddl, ...usageDdl]);

console.log('=== 1) 三条对账 SQL 的 schema 存在性检查 ===');
for (const c of sqlChecks) {
  for (const [table, cols] of Object.entries(c.tables)) {
    const t = catalog.get(table);
    if (!t) {
      add('ERR', c.id, `表不存在: ${table}`);
      console.log(`[ERR ] ${c.id}  表不存在 → ${table}`);
      continue;
    }
    const missing = cols.filter((col) => !t.cols.has(col));
    if (missing.length) {
      add('ERR', c.id, `列不存在: ${table}.${missing.join(',')}`);
      console.log(`[ERR ] ${c.id}  列不存在 → ${table}.${missing.join(',')}`);
    } else {
      console.log(`[OK  ] ${c.id}  ${table}(${cols.join(',')}) 全部存在`);
    }
  }
}

// ---------- 2) 实体 ⇄ DDL 漂移 ----------
console.log('\n=== 2) 实体 ⇄ DDL 漂移检查 ===');
const entities = [
  'ai-cs-knowledge/src/main/java/com/aics/knowledge/entity/KnowledgeDocument.java',
  'ai-cs-knowledge/src/main/java/com/aics/knowledge/entity/KnowledgeFaq.java',
];
for (const rel of entities) {
  const { table, fields } = parseEntity(rel);
  const inInit = ddl.has(table);
  const inAll = allDdl.has(table);
  const tag = inInit ? 'OK  ' : inAll ? 'WARN' : 'ERR ';
  console.log(`[${tag}] ${table}  init.sql=${inInit ? '有' : '无'}  all-init.sql=${inAll ? '有' : '无'}  实体字段=${fields.length}`);
  if (!inInit) add(inAll ? 'WARN' : 'ERR', table, '@TableName 指向的表不在主初始化脚本里');
  const t = ddl.get(table);
  if (t) {
    const onlyDdl = [...t.cols].filter((c) => !fields.includes(c));
    const onlyEntity = fields.filter((f) => !t.cols.has(f));
    console.log(`        仅 DDL 有（实体不映射）: ${onlyDdl.join(',') || '(无)'}`);
    console.log(`        仅实体有（DDL 无此列）: ${onlyEntity.join(',') || '(无)'}`);
    if (onlyEntity.length) add('ERR', table, `实体字段无对应列: ${onlyEntity.join(',')}`);
    if (onlyDdl.length) add('WARN', table, `DDL 列实体未映射: ${onlyDdl.join(',')}`);
  }
}

// ---------- 3) 向量元数据契约 ----------
console.log('\n=== 3) 向量元数据契约检查 ===');
const vectorSvc = read('ai-cs-knowledge/src/main/java/com/aics/knowledge/service/KnowledgeVectorService.java');
const written = [...vectorSvc.matchAll(/\.put\("([^"]+)"/g)].map((m) => m[1]);
const chatEval = read('ai-cs-chat/src/main/java/com/aics/chat/rag/eval/RagEvalServiceImpl.java');
const readKeys = [...chatEval.matchAll(/getOrDefault\("([^"]+)"/g)].map((m) => m[1]);
console.log(`[INFO] 写入侧 metadata 键: ${written.join(', ') || '(未解析到)'}`);
console.log(`[INFO] 检索侧读取键:     ${readKeys.join(', ') || '(未解析到)'}`);
for (const k of readKeys) {
  const ok = written.includes(k);
  console.log(`[${ok ? 'OK  ' : 'ERR '}] 读取键 "${k}" ${ok ? '写入侧同键存在' : '写入侧无此键 → 引用回指会退化为 chunk id'}`);
  if (!ok) add('ERR', 'metadata-contract', `读取键 ${k} 写入侧缺失`);
}
const collections = [
  'ai-cs-knowledge/src/main/resources/application.yml',
  'ai-cs-chat/src/main/resources/application.yml',
  'ai-cs-search/src/main/resources/application.yml',
];
const names = collections.map((rel) => ({
  rel: rel.split('/')[0],
  name: /collection-name:\s*(\S+)/.exec(read(rel))?.[1] ?? '(未配置)',
}));
const distinct = new Set(names.map((n) => n.name));
console.log(`[${distinct.size === 1 ? 'OK  ' : 'ERR '}] 三服务 collection-name: ${names.map((n) => `${n.rel}=${n.name}`).join(' | ')}`);
if (distinct.size !== 1) add('ERR', 'collection', '三服务不是同一个 Chroma 集合');

// ---------- 4) 对账任务的调度存在性 ----------
console.log('\n=== 4) 对账调度存在性扫描（全仓 java）===');
const javaFiles = [];
const { readdirSync, statSync } = await import('node:fs');
function walk(dir, out = []) {
  for (const e of readdirSync(dir)) {
    if (e === 'node_modules' || e === 'target' || e === '.git' || e === 'learning-docs') continue;
    const p = join(dir, e);
    const st = statSync(p);
    if (st.isDirectory()) walk(p, out);
    else if (e.endsWith('.java')) out.push(p);
  }
  return out;
}
javaFiles.push(...walk(ROOT));
const hit = (re) => javaFiles.filter((f) => re.test(readFileSync(f, 'utf8')));
const scheduled = hit(/@Scheduled/);
const xxlJob = hit(/@XxlJob/);
const reconcile = hit(/对账|[Rr]econcile/);
console.log(`[INFO] 含 @Scheduled 的文件 ${scheduled.length}: ${scheduled.map((f) => f.replace(ROOT, '').replace(/\\/g, '/')).join(', ')}`);
console.log(`[INFO] 含 @XxlJob   的文件 ${xxlJob.length}: ${xxlJob.map((f) => f.replace(ROOT, '').replace(/\\/g, '/')).join(', ')}`);
console.log(`[INFO] 提及 对账/reconcile 的文件 ${reconcile.length}`);
const scheduledReconcile = reconcile.filter((f) => /@Scheduled|@XxlJob/.test(readFileSync(f, 'utf8')));
console.log(`[${scheduledReconcile.length ? 'OK  ' : 'WARN'}] 同时对账 + 调度的文件: ${scheduledReconcile.length} 个`);
if (!scheduledReconcile.length) add('WARN', 'scheduling', '仓库内没有"调度触发"的对账任务');

// ---------- 汇总 ----------
const errs = findings.filter((f) => f.level === 'ERR').length;
const warns = findings.filter((f) => f.level === 'WARN').length;
console.log('\n=== 汇总 ===');
console.log(`检查项结论: ERR=${errs}  WARN=${warns}  明细条数=${findings.length}`);
for (const f of findings) console.log(`  - [${f.level}] ${f.check}: ${f.detail}`);
process.exitCode = errs ? 1 : 0;
