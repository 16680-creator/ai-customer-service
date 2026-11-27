// ORDER BY 候选排序键的剪枝效益模型（纯算术，输入是假设规模档位，非实测）
const G = 8192, DAYS = 31, WIN = 7, SCEN = 5;
const pad = (s, n) => String(s).padEnd(n);
const sizes = [200000, 2000000, 20000000];

for (const N of sizes) {
  const total = Math.ceil(N / G), perDay = N / DAYS;
  console.log('=== 单月分区 N = ' + N.toLocaleString('en-US') + ' 行, 全表 granule = ' + total + ' ===');
  console.log(pad('ORDER BY 前缀', 30) + pad('命中行', 12) + pad('扫 granule', 12) + '读放大占比');
  const rows = [
    ['create_time, scenario, model', Math.round(perDay * WIN)],
    ['scenario, create_time, model', Math.round(perDay * WIN / SCEN)],
    ['request_id (唯一键/无排序语义)', N],
  ];
  for (const [key, hit] of rows) {
    const gran = Math.min(total, Math.ceil(hit / G));
    console.log(pad(key, 30) + pad(hit.toLocaleString('en-US'), 12) + pad(gran, 12) + (gran / total * 100).toFixed(1) + '%');
  }
  console.log('');
}

// 行宽估算：列类型来自 deploy/mysql/llm-observability-init.sql 的 model_usage 定义
const cols = [
  ['request_id', 'String', 36], ['user_id', 'Int64', 8], ['session_id', 'Int64', 8],
  ['scenario', 'LowCardinality(String)', 8], ['provider', 'LowCardinality(String)', 10],
  ['model', 'LowCardinality(String)', 16], ['input_tokens', 'Int32', 4],
  ['output_tokens', 'Int32', 4], ['total_tokens', 'Int32', 4],
  ['estimated_cost', 'Decimal(12,6)', 16], ['estimated', 'UInt8', 1],
  ['status', 'LowCardinality(String)', 8], ['error_summary', 'String', 0],
  ['create_time', 'DateTime', 4],
];
const sum = cols.reduce((a, c) => a + c[2], 0);
console.log('=== model_usage 映射到 ClickHouse 后的平均行宽（未压缩上界估算） ===');
for (const [n, t, b] of cols) console.log(pad(n, 18) + pad(t, 26) + b + ' B');
console.log(pad('合计', 44) + sum + ' B/行');
for (const N of sizes) {
  const raw = sum * N / 1024 / 1024;
  console.log('N=' + N.toLocaleString('en-US') + ' 行: 原始 ' + raw.toFixed(1) + ' MiB/月, 压缩 5x 后 ' + (raw / 5).toFixed(1) + ' MiB/月, 一年 ' + (raw / 5 * 12).toFixed(0) + ' MiB');
}
console.log('一个 granule 的原始字节 = ' + sum + ' B x 8192 = ' + (sum * 8192 / 1024).toFixed(1) + ' KiB（跳数索引的最小剪枝粒度）');

// 成本算术：单价来自 tools/nacos-config/ai-cs-chat.yml:119-128（元/百万 Token）
const price = { 'deepseek-chat': [2.0, 8.0], 'siliconflow-qwen3-32b': [1.0, 2.0], 'siliconflow-qwen3-8b': [0.2, 0.6] };
const inTok = 1200, outTok = 400, calls = 10000;
console.log('');
console.log('=== 成本算术（单价真实，调用量为假设） ===');
for (const [m, [pi, po]] of Object.entries(price)) {
  const per = inTok / 1e6 * pi + outTok / 1e6 * po;
  console.log(pad(m, 26) + '单次 ' + per.toFixed(6) + ' 元 × ' + calls + ' 次 = ' + (per * calls).toFixed(2) + ' 元');
}
