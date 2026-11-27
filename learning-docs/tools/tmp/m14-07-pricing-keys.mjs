#!/usr/bin/env node
/*
 * m14-07-pricing-keys.mjs —— 单价键覆盖率检查（14/07 篇 C 级证据，可直接当巡检脚本）
 *
 * 做什么：从 tools/nacos-config/ai-cs-chat.yml 抽出 aics.usage.pricing 的所有键，
 *        再抽出路由模型的 id，并检查聊天侧写入的 pricingKey 是否都能命中价格表。
 * 判读：任何 model 在价格表里查不到键 → estimateCost 走 defaultPricing（0/0）→ 费用恒 0 且无告警。
 */
import { readFileSync } from 'node:fs';

const ymlPath = 'tools/nacos-config/ai-cs-chat.yml';
const lines = readFileSync(ymlPath, 'utf8').split(/\r?\n/);

// 1) pricing 键：位于 '    pricing:' 之后、缩进为 6 空格的 'xxx:' 行
const pricing = [];
let inPricing = false;
for (const line of lines) {
  if (/^\s{4}pricing:\s*$/.test(line)) {
    inPricing = true;
    continue;
  }
  if (inPricing) {
    if (/^\s{6}([A-Za-z0-9._/-]+):\s*$/.test(line)) {
      pricing.push(line.trim().replace(/:$/, ''));
    } else if (/^\s{0,4}\S/.test(line)) {
      inPricing = false; // 缩进退回到 4 空格以内 → pricing 块结束
    }
  }
}

// 2) 路由模型的 id 与真实 model 名
const modelIds = [...new Set([...lines.join('\n').matchAll(/^\s{6}- id: (.+)$/gm)].map((m) => m[1].trim()))];

// 3) 聊天侧写入的两类 pricingKey 来源（8 参重载传 id；7 参重载传展示名）
const visionSrc = readFileSync(
  'ai-cs-chat/src/main/java/com/aics/chat/service/impl/VisionModelClient.java',
  'utf8'
);
const visionModel = /visionProperties\.getModel\(\)/.test(visionSrc)
  ? lines.join('\n').match(/vision:[\s\S]*?model:\s*(\S+)/)?.[1] ?? '<未在 yml 中找到>'
  : '<未知>';

console.log('价格表键（aics.usage.pricing）:');
for (const k of pricing) console.log(`  - ${k}`);
console.log('\n路由模型 id（8 参重载用 id 作 pricingKey）:');
for (const k of modelIds) console.log(`  - ${k}  ${pricing.includes(k) ? '命中' : '未命中 → 走 defaultPricing'}`);
console.log('\n视觉模型展示名（7 参重载 → pricingKey = 展示名）:');
console.log(`  - ${visionModel}  ${pricing.includes(visionModel) ? '命中' : '未命中 → 走 defaultPricing（0/0）'}`);
console.log('\n默认兜底单价（ModelUsageProperties.ModelPrice 初值）: input=0 output=0');
