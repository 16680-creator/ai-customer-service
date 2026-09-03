// 网关限流压测：验证 Redis 令牌桶（replenish-rate=5 / burst=10）的拒绝行为。
// 用法：k6 run -e BASE=http://localhost:8080 -e PATH=/api/product/page gateway-rate-limit.js
// 预期：请求速率(30/s) 远超 replenish-rate(5/s)，溢出部分被 429 拒绝。
import http from 'k6/http';
import { check } from 'k6';

const BASE = __ENV.BASE || 'http://localhost:8080';
const PATH = __ENV.PATH || '/api/product/page?current=1&size=10';

export const options = {
  scenarios: {
    burst: {
      // 恒定到达率：无论响应多慢，始终以 30 req/s 施压（比 VU 模型更适合测限流）
      executor: 'constant-arrival-rate',
      rate: 30,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 20,
      maxVUs: 50,
    },
  },
  thresholds: {
    // 限流场景下 429 是"预期产物"，不作为失败；只约束放行请求的时延
    http_req_duration: ['p(95)<500'],
  },
};

export default function () {
  const res = http.get(`${BASE}${PATH}`);
  check(res, {
    '2xx 放行': (r) => r.status >= 200 && r.status < 300,
    '429 限流拒绝': (r) => r.status === 429,
  });
}
