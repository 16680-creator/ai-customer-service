/* 图表渲染库：index.html 与截图页共用 */
/* 淡蓝色系全局配置 */
const PALETTE = ['#2E75B6', '#5B9BD5', '#8FB8E8', '#A9CCE8', '#C6DCF0', '#7FB3E0', '#4A90D9', '#B5D5F0', '#6BA6DD', '#9DC3E6'];
const AXIS = { axisLine: { lineStyle: { color: '#C9DBEF' } }, axisLabel: { color: '#5A7CA6', fontSize: 11 }, axisTick: { show: false } };
const TIP = { backgroundColor: 'rgba(255,255,255,0.96)', borderColor: '#D7E5F4', borderWidth: 1, textStyle: { color: '#1F3A5F', fontSize: 12 }, padding: [8, 12], extraCssText: 'box-shadow:0 4px 14px rgba(46,117,182,.15);border-radius:8px;' };
const GRID = { left: 12, right: 20, top: 40, bottom: 8, containLabel: true };
const TITLE = { textStyle: { color: '#1F3A5F', fontSize: 13, fontWeight: 700 } };

const D = window.REPORT_DATA;
const charts = [];

function mount(id, opt) {
  const el = document.getElementById(id);
  if (!el) return;
  const c = echarts.init(el, null, { renderer: 'svg' });
  c.setOption({ animation: false, ...opt });
  charts.push(c);
  window.addEventListener('resize', () => c.resize());
}

/* ============ 核心指标卡 ============ */
function renderKpi() {
  const s = D.summary;
  const m6 = D.monthly[5], m7 = D.monthly[6];
  const gmvMom = ((m7.gmv - m6.gmv) / m6.gmv * 100).toFixed(1);
  const rateMom = (m7.rate - m6.rate).toFixed(1);
  const cards = [
    { label: '累计 GMV', value: s.gmv, unit: '万元', note: '1-7 月合计', delta: `7月环比 ${gmvMom}%`, up: Number(gmvMom) >= 0 },
    { label: '净销售额', value: s.ns, unit: '万元', note: '扣退款后', delta: `退款率 ${s.refundRate}%`, up: true },
    { label: '税后净利', value: s.np, unit: '万元', note: '扣全部税负', delta: `7月净利率 ${m7.rate}%`, up: Number(rateMom) >= 0 },
    { label: '综合毛利率', value: s.grossRate, unit: '%', note: `毛利 ${s.gross} 万元`, delta: '高毛利品类', up: true },
    { label: '综合净利率', value: s.npRate, unit: '%', note: '税后净利 / 净销售', delta: `Q2呈下滑趋势`, up: false },
    { label: '销量 / 动销SKU', value: (s.qty / 10000).toFixed(1), unit: '万件', note: `动销 SKU ${s.skuCount} 个`, delta: '平均客单约 34 元', up: true }
  ];
  const grid = document.getElementById('kpiGrid');
  if (!grid) return;
  grid.innerHTML = cards.map(c => `
    <div class="kpi-card">
      <div class="label">${c.label}</div>
      <div class="value">${c.value}<span class="unit">${c.unit}</span></div>
      <div class="note">${c.note}</div>
      <div class="delta ${c.up ? 'up' : 'down'}">${c.delta}</div>
    </div>`).join('');
}

/* ============ 图1 月度 GMV 与净利率 ============ */
function renderMonthly() {
  mount('chartMonthly', {
    tooltip: { trigger: 'axis', ...TIP },
    legend: { data: ['GMV（万元）', '净利率（%）'], top: 0, textStyle: { color: '#5A7CA6', fontSize: 12 } },
    grid: GRID,
    xAxis: { type: 'category', data: D.monthly.map(m => m.month), ...AXIS },
    yAxis: [
      { type: 'value', name: 'GMV（万元）', nameTextStyle: { color: '#5A7CA6' }, ...AXIS, splitLine: { lineStyle: { color: '#EDF4FB' } } },
      { type: 'value', name: '净利率（%）', nameTextStyle: { color: '#5A7CA6' }, min: 40, max: 60, ...AXIS, splitLine: { show: false } }
    ],
    series: [
      { name: 'GMV（万元）', type: 'bar', data: D.monthly.map(m => m.gmv), barWidth: 34, itemStyle: { borderRadius: [6, 6, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#5B9BD5' }, { offset: 1, color: '#A9CCE8' }]) },
        label: { show: true, position: 'top', color: '#2E75B6', fontSize: 11, fontWeight: 600 } },
      { name: '净利率（%）', type: 'line', data: D.monthly.map(m => m.rate), yAxisIndex: 1, smooth: true, symbolSize: 7, lineStyle: { color: '#1F4E79', width: 2.5 }, itemStyle: { color: '#1F4E79' },
        label: { show: true, position: 'top', color: '#1F4E79', fontSize: 11, fontWeight: 700 } }
    ],
    graphic: [
      { type: 'text', left: '62%', top: '78%', style: { text: '2月爆发：跃马鸣霄书签占42.3%', fill: '#C65D5D', fontSize: 11, fontWeight: 700 } },
      { type: 'text', left: '6%', top: '30%', style: { text: '7月回落 -29.0%', fill: '#C65D5D', fontSize: 11, fontWeight: 700 } }
    ]
  });
}

/* ============ 图2 品类净销售额结构 ============ */
function renderCats() {
  mount('chartCats', {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...TIP },
    grid: { left: 10, right: 50, top: 10, bottom: 10, containLabel: true },
    xAxis: { type: 'value', ...AXIS, splitLine: { lineStyle: { color: '#EDF4FB' } } },
    yAxis: { type: 'category', data: D.cats.map(c => c.cat).reverse(), ...AXIS },
    series: [{
      type: 'bar', data: D.cats.slice().reverse().map((c, i) => ({
        value: c.ns,
        itemStyle: { borderRadius: [0, 5, 5, 0], color: PALETTE[i % PALETTE.length] },
        label: { show: true, position: 'right', formatter: () => c.ns.toFixed(1) + '万 · ' + c.rate + '%', color: '#2C4A73', fontSize: 11 }
      })), barWidth: 16
    }]
  });
}

/* ============ 图3 平台 GMV 占比 + 净利率 ============ */
function renderPlats() {
  mount('chartPlats', {
    tooltip: { trigger: 'item', formatter: p => `${p.name}：GMV ${p.value} 万（${p.percent}%）<br/>净利率 ${D.plats[p.dataIndex].rate}%`, ...TIP },
    legend: { orient: 'vertical', right: 6, top: 'middle', textStyle: { color: '#5A7CA6', fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['42%', '70%'], center: ['36%', '50%'],
      itemStyle: { borderColor: '#fff', borderWidth: 2, borderRadius: 4 },
      label: { show: false },
      data: D.plats.map((p, i) => ({ name: p.plat, value: p.gmv, itemStyle: { color: PALETTE[i % PALETTE.length] } }))
    }],
    graphic: D.plats.slice(0, 5).map((p, i) => ({
      type: 'text',
      left: '70%', top: 30 + i * 22,
      style: { text: `${p.plat}  ${p.gmv}万 / 净利率 ${p.rate}%`, fill: i === 0 ? '#1F4E79' : '#5A7CA6', fontSize: 11, fontWeight: i === 0 ? 700 : 400 }
    }))
  });
}

/* ============ 图4 品类×平台 净利率热力图 ============ */
function renderMatrix() {
  const data = [];
  D.matValues.forEach((row, ri) => row.forEach((v, ci) => {
    if (v !== null) data.push([ci, ri, v]);
  }));
  const maxAbs = Math.max(...data.map(d => Math.abs(d[2])), 1);
  mount('chartMatrix', {
    tooltip: { position: 'top', formatter: p => `${D.matCats[p.value[1]]} × ${D.matPlats[p.value[0]]}<br/>净利率 ${p.value[2].toFixed(1)}%`, ...TIP },
    grid: { left: 80, right: 40, top: 8, bottom: 60 },
    xAxis: { type: 'category', data: D.matPlats, ...AXIS, axisLabel: { color: '#2E75B6', fontSize: 11, fontWeight: 600, rotate: 0 } },
    yAxis: { type: 'category', data: D.matCats, ...AXIS, axisLabel: { color: '#2E75B6', fontSize: 11, fontWeight: 600 } },
    visualMap: {
      min: -10, max: maxAbs * 1.05, calculable: true, orient: 'horizontal', left: 'center', bottom: 0,
      inRange: { color: ['#E8F0FA', '#A9CCE8', '#5B9BD5', '#2E75B6', '#1F4E79'] },
      textStyle: { color: '#5A7CA6', fontSize: 10 }
    },
    series: [{
      type: 'heatmap', data,
      label: { show: true, formatter: p => (p.value[2] == null ? '' : p.value[2].toFixed(0)), color: '#FFFFFF', fontSize: 9.5 },
      itemStyle: { borderColor: '#fff', borderWidth: 1.5 }
    }]
  });
}

/* ============ 图5 月度品类结构变化（堆叠面积） ============ */
function renderStruct() {
  const months = D.mCatShare.map(m => m.month);
  const ser = (name, color) => ({
    name, type: 'line', stack: 'total', smooth: true, symbol: 'none',
    areaStyle: { opacity: 0.82 }, lineStyle: { width: 0 }, itemStyle: { color },
    data: D.mCatShare.map(m => m[name])
  });
  mount('chartStruct', {
    tooltip: { trigger: 'axis', ...TIP },
    legend: { top: 0, textStyle: { color: '#5A7CA6', fontSize: 12 } },
    grid: GRID,
    xAxis: { type: 'category', data: months, ...AXIS },
    yAxis: { type: 'value', name: '净销售额占比 %', nameTextStyle: { color: '#5A7CA6' }, ...AXIS, splitLine: { lineStyle: { color: '#EDF4FB' } } },
    series: [
      ser('其他', '#DCEBF7'), ser('图书', '#8FB8E8'), ser('盲盒福袋', '#4A90D9'), ser('金属书签', '#2E75B6')
    ]
  });
}

/* ============ 图6 毛绒团子挂件 分平台 ============ */
function renderTu() {
  mount('chartTu', {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...TIP },
    grid: { left: 10, right: 30, top: 10, bottom: 10, containLabel: true },
    xAxis: { type: 'value', ...AXIS, splitLine: { lineStyle: { color: '#EDF4FB' } } },
    yAxis: { type: 'category', data: D.tuData.plat.map(p => p.plat).reverse(), ...AXIS },
    series: [{
      type: 'bar', barWidth: 18,
      data: D.tuData.plat.slice().reverse().map((p, i) => ({
        value: p.gmv,
        itemStyle: { borderRadius: [0, 5, 5, 0], color: PALETTE[i % PALETTE.length] },
        label: { show: true, position: 'right', formatter: v => v.value.toFixed(1) + '万', color: '#2C4A73', fontSize: 11 }
      }))
    }]
  });
}

/* ============ 图7 TOP / BOTTOM 组合 ============ */
function renderTopBottom() {
  const top = D.topBottom.top.slice().reverse();
  const bottom = D.topBottom.bottom;
  const names = [...bottom.map(b => b.name), ...top.map(t => t.name)];
  const vals = [...bottom.map(b => -b.np), ...top.map(t => t.np)];
  mount('chartTopBottom', {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, formatter: p => `${p[0].name}<br/>税后净利 ${p[0].value.toFixed(1)} 万`, ...TIP },
    grid: { left: 10, right: 40, top: 10, bottom: 10, containLabel: true },
    xAxis: { type: 'value', ...AXIS, splitLine: { lineStyle: { color: '#EDF4FB' } } },
    yAxis: { type: 'category', data: names, ...AXIS },
    series: [{
      type: 'bar', barWidth: 12,
      data: vals.map(v => ({
        value: v,
        itemStyle: { borderRadius: [0, 4, 4, 0], color: v >= 0 ? '#5B9BD5' : '#C65D5D' },
        label: { show: true, position: 'right', formatter: p => Math.abs(p.value).toFixed(1), color: v >= 0 ? '#2E75B6' : '#C65D5D', fontSize: 10 }
      }))
    }]
  });
}

/* ============ 图8 Q1→Q2 净利率变化 ============ */
function renderQ1Q2() {
  const all = [...D.q1q2.worsen, ...D.q1q2.improve];
  const names = all.map(x => x.name);
  const r1 = all.map(x => x.r1);
  const r2 = all.map(x => x.r2);
  mount('chartQ1Q2', {
    tooltip: { trigger: 'axis', ...TIP },
    legend: { data: ['Q1 净利率 %', 'Q2 净利率 %'], top: 0, textStyle: { color: '#5A7CA6', fontSize: 12 } },
    grid: { left: 10, right: 30, top: 36, bottom: 70, containLabel: true },
    xAxis: { type: 'category', data: names, ...AXIS, axisLabel: { rotate: 38, color: '#2C4A73', fontSize: 10 } },
    yAxis: { type: 'value', name: '净利率 %', ...AXIS, splitLine: { lineStyle: { color: '#EDF4FB' } } },
    series: [
      { name: 'Q1 净利率 %', type: 'bar', data: r1, barWidth: 14, itemStyle: { borderRadius: [3, 3, 0, 0], color: '#A9CCE8' } },
      { name: 'Q2 净利率 %', type: 'bar', data: r2, barWidth: 14, itemStyle: { borderRadius: [3, 3, 0, 0], color: '#2E75B6' } }
    ],
    graphic: all.filter(x => Math.abs(x.d) >= 9).map((x, i) => ({
      type: 'text', left: `${(i + 1) * 6.7 + 2}%`, top: '12%',
      style: { text: (x.d >= 0 ? '+' : '') + x.d.toFixed(1) + 'pp', fill: x.d >= 0 ? '#3E8E5A' : '#C65D5D', fontSize: 10, fontWeight: 700 }
    }))
  });
}

/* ============ 图9 IP TOP10 ============ */
function renderIp() {
  mount('chartIp', {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...TIP },
    grid: { left: 10, right: 46, top: 10, bottom: 10, containLabel: true },
    xAxis: { type: 'value', ...AXIS, splitLine: { lineStyle: { color: '#EDF4FB' } } },
    yAxis: { type: 'category', data: D.ipData.top.map(i => i.name).reverse(), ...AXIS },
    series: [{
      type: 'bar', barWidth: 14,
      data: D.ipData.top.slice().reverse().map((i, idx) => ({
        value: i.gmv,
        itemStyle: { borderRadius: [0, 5, 5, 0], color: PALETTE[idx % PALETTE.length] },
        label: { show: true, position: 'right', formatter: p => p.value.toFixed(1) + '万 · ' + i.share + '%', color: '#2C4A73', fontSize: 10.5 }
      }))
    }]
  });
}

/* ============ 图10 金属书签月度 ============ */
function renderJs() {
  mount('chartJs', {
    tooltip: { trigger: 'axis', ...TIP },
    grid: GRID,
    xAxis: { type: 'category', data: D.jsMonthly.map(m => m.month), ...AXIS },
    yAxis: { type: 'value', name: 'GMV（万元）', ...AXIS, splitLine: { lineStyle: { color: '#EDF4FB' } } },
    series: [{
      type: 'bar', data: D.jsMonthly.map(m => m.gmv), barWidth: 30,
      itemStyle: { borderRadius: [6, 6, 0, 0], color: p => p.dataIndex === 1 ? '#2E75B6' : '#A9CCE8' },
      label: { show: true, position: 'top', color: '#2E75B6', fontSize: 11, fontWeight: 600 }
    }],
    graphic: [
      { type: 'text', left: '12%', top: '8%', style: { text: '「跃马鸣霄」新年限定 69.7万', fill: '#1F4E79', fontSize: 12, fontWeight: 700 } },
      { type: 'text', left: '40%', top: '76%', style: { text: '4-7月单月仅约0.1万', fill: '#C65D5D', fontSize: 11, fontWeight: 700 } }
    ]
  });
}

/* ============ 图11 退款率：平台 × 品类（新增） ============ */
function renderRefund() {
  const platSorted = D.plats.slice().sort((a, b) => b.refundRate - a.refundRate);
  const catSorted = D.cats.slice().sort((a, b) => b.refundRate - a.refundRate);
  mount('chartRefund', {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' }, ...TIP },
    grid: [
      { left: 12, right: 24, top: 28, bottom: '52%', containLabel: true },
      { left: 12, right: 24, top: '52%', bottom: 6, containLabel: true }
    ],
    xAxis: [
      { type: 'value', ...AXIS, splitLine: { lineStyle: { color: '#EDF4FB' } } },
      { type: 'value', ...AXIS, splitLine: { lineStyle: { color: '#EDF4FB' } } }
    ],
    yAxis: [
      { type: 'category', data: platSorted.map(p => p.plat), ...AXIS },
      { type: 'category', data: catSorted.map(c => c.cat), ...AXIS }
    ],
    series: [
      {
        name: '平台退款率', type: 'bar', barWidth: 13,
        data: platSorted.map(p => ({
          value: p.refundRate,
          itemStyle: { borderRadius: [0, 4, 4, 0], color: p.refundRate >= 12 ? '#C65D5D' : (p.refundRate >= 9 ? '#5B9BD5' : '#8FB8E8') },
          label: { show: true, position: 'right', formatter: v => v.value + '%', color: '#2C4A73', fontSize: 10.5 }
        }))
      },
      {
        name: '品类退款率', type: 'bar', barWidth: 12, xAxisIndex: 1, yAxisIndex: 1,
        data: catSorted.map(c => ({
          value: c.refundRate,
          itemStyle: { borderRadius: [0, 4, 4, 0], color: c.refundRate >= 14 ? '#C65D5D' : (c.refundRate >= 11 ? '#5B9BD5' : '#8FB8E8') },
          label: { show: true, position: 'right', formatter: v => v.value + '%', color: '#2C4A73', fontSize: 10 }
        }))
      }
    ],
    graphic: [
      { type: 'text', left: '1%', top: 0, style: { text: '平台退款率（%）—— 小红书13.6 / 快手14.4 明显偏高', fill: '#1F4E79', fontSize: 12, fontWeight: 700 } },
      { type: 'text', left: '1%', top: '50%', style: { text: '品类退款率（%）—— 盲盒福袋16.2 / 金属书签14.6 为重灾区', fill: '#1F4E79', fontSize: 12, fontWeight: 700 } }
    ]
  });
}
