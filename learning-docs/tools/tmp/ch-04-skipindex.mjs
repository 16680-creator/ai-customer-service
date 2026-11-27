// 跳数索引（minmax）有效性模型：一个 granule 内某列的取值范围是否真的能剪掉数据
const G = 8192;
const card = { scenario: 5, model: 3, provider: 2 };
console.log('granule 行数 =', G, ' (index_granularity 默认值)');
for (const [col, C] of Object.entries(card)) {
  // 写入顺序随机时，单个 granule 里"完全没有某个取值"的概率
  const pMissing = Math.exp(G * Math.log((C - 1) / C));
  const distinctInGranule = C * (1 - pMissing);
  console.log(col.padEnd(10), '基数 ' + C, '| 一个 granule 内期望出现取值数 =', distinctInGranule.toFixed(4),
    '| min/max 覆盖全部取值的概率 =', (1 - pMissing).toFixed(6));
}
// 反过来：如果按该列排序，一个 granule 只装 1 个取值
console.log('按列排序后单个 granule 内取值数 = 1（min=max），minmax 索引退化为无用，因为排序本身已经完成剪枝');
