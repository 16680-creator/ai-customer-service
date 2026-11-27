// 告警配置静态自检：rule 文件结构 + Alertmanager 路由树交叉引用（本机无 promtool/amtool）
// 用法: node tools/tmp/alert-config-selfcheck-203.mjs
// 局限: 手写缩进解析，只校验「结构完整 + receiver 引用可达 + 分级有落点 + 无 tab」，不校验 PromQL 语义。

const rulesYaml = `groups:
  - name: ai-cs-alerting
    rules:
      - alert: AiCsServiceDown
        expr: up{job="ai-cs-services"} == 0
        for: 2m
        labels:
          severity: P0
          kind: symptom
        annotations:
          summary: "ai-cs 某实例已不可抓取"
          runbook: "deploy/observability/alertmanager/README.md#aics_servicedown"
      - alert: AiCsHttp5xxBudgetBurn1h
        expr: (sum by (instance) (rate(http_server_requests_seconds_count{job="ai-cs-services",status=~"5.."}[5m])) / sum by (instance) (rate(http_server_requests_seconds_count{job="ai-cs-services"}[5m]))) > (14.4 * 0.01)
        for: 5m
        labels:
          severity: P1
          kind: budget
        annotations:
          summary: "1h 窗口 5xx 燃烧率超 14.4 倍"
      - alert: AiCsJvmHeapWillExhaust
        expr: predict_linear(jvm_memory_used_bytes{job="ai-cs-services",area="heap"}[30m], 3600) > jvm_memory_max_bytes{job="ai-cs-services",area="heap"} * 0.95
        for: 10m
        labels:
          severity: P2
          kind: trend
        annotations:
          summary: "堆内存按当前斜率 1 小时内打满"
`;

const amYaml = `route:
  receiver: p2-daily
  group_by: [alertname, severity]
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  routes:
    - matchers: ['severity="P0"']
      receiver: p0-phone
      group_wait: 10s
      repeat_interval: 30m
    - matchers: ['severity="P1"']
      receiver: p1-im
    - matchers: ['severity="P2"']
      receiver: p2-daily
receivers:
  - name: p0-phone
    webhook_configs:
      - url: http://127.0.0.1:9095/hooks/p0
  - name: p1-im
    webhook_configs:
      - url: http://127.0.0.1:9095/hooks/p1
  - name: p2-daily
    webhook_configs:
      - url: http://127.0.0.1:9095/hooks/p2
`;

const indent = (l) => l.length - l.trimStart().length;
const lines = (s) => s.split('\n').filter((l) => l.trim() !== '' && !l.trim().startsWith('#'));

let errs = 0;
const err = (m) => { errs++; console.log('  ERROR ' + m); };

console.log('=== 1) 缩进/制表符检查 ===');
for (const [tag, doc] of [['rules.yaml', rulesYaml], ['alertmanager.yml', amYaml]]) {
  const tabs = lines(doc).filter((l) => l.includes('\t'));
  const odd = lines(doc).filter((l) => indent(l) % 2 !== 0);
  console.log(`${tag}: 行数=${lines(doc).length} tab=${tabs.length} 奇数缩进=${odd.length}` + (tabs.length || odd.length ? '  <-- 需修' : '  OK'));
  if (tabs.length || odd.length) errs++;
}

console.log('\n=== 2) rule 文件必填字段 ===');
for (const k of ['alert:', 'expr:', 'for:', 'severity:', 'kind:', 'summary:', 'runbook:']) {
  const n = lines(rulesYaml).filter((l) => l.includes(k)).length;
  console.log(`  ${k.padEnd(12)} 出现 ${n} 次` + (n === 0 ? '  <-- 缺失' : '  OK'));
  if (n === 0) errs++;
}
const alertCount = lines(rulesYaml).filter((l) => l.trim().startsWith('- alert:')).length;
console.log(`  规则条数 = ${alertCount}`);

console.log('\n=== 3) Alertmanager 路由树（按缩进还原层级）===');
for (const l of lines(amYaml)) {
  if (/^\s*- matchers|^\s*(route|receivers):|^\s*- name:|^\s*receiver:|^\s*(group_by|group_wait|group_interval|repeat_interval):/.test(l)) {
    console.log('  ' + '  '.repeat(Math.floor(indent(l) / 2)) + l.trim());
  }
}

console.log('\n=== 4) receiver 交叉引用（route 引用的名字必须在 receivers 里定义）===');
const defined = new Set([...amYaml.matchAll(/^\s*-\s*name:\s*(\S+)/gm)].map((m) => m[1]));
const referenced = [...amYaml.matchAll(/receiver:\s*(\S+)/g)].map((m) => m[1]);
console.log('  已定义 receivers: ' + [...defined].join(', '));
for (const r of referenced) {
  const ok = defined.has(r);
  console.log(`  route -> ${r.padEnd(10)} ${ok ? 'OK' : 'UNRESOLVED'}`);
  if (!ok) err++;
}

console.log('\n=== 5) 分级覆盖检查（P0/P1/P2 是否都有落点）===');
for (const sev of ['P0', 'P1', 'P2']) {
  const inRules = rulesYaml.includes(`severity: ${sev}`);
  const inRoute = amYaml.includes(`severity="${sev}"`);
  const ok = inRules && inRoute;
  console.log(`  ${sev}: rule 里产出=${inRules ? 'Y' : 'N'}  route 里有去处=${inRoute ? 'Y' : 'N'}` + (ok ? '  OK' : '  <-- 断链'));
  if (!ok) err++;
}

console.log('\n=== 6) 结论 ===');
console.log(errs === 0 ? 'ALL PASS：结构与引用自洽（真实语法仍需 promtool check rules / amtool check-config）' : `${errs} 处问题`);
process.exit(errs === 0 ? 0 : 1);
