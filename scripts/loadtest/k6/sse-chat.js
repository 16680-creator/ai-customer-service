// SSE 流式对话压测：测"首 token 时延"与"整条流完成时延"，用 VU 保持长连接模拟并发会话。
// 用法：
//   直连 Python 服务（推荐，绕过网关变量）：k6 run -e BASE=http://localhost:8000 sse-chat.js
//   走网关打 Java 链路：                 k6 run -e BASE=http://localhost:8080 -e PATH=/api/chat/stream sse-chat.js
// 说明：k6 http 把整条流读完才返回，http_req_waiting ≈ 首 token 时延（TTFB 近似），
//       http_req_duration = 流完成总时长；需要逐帧级指标可换 xk6-sse 扩展。
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE || 'http://localhost:8000';
const PATH = __ENV.PATH || '/api/chat/stream';

export const options = {
  scenarios: {
    hold_streams: {
      // 阶梯加压：SSE 是长连接，用 VU 数模拟"同时挂着的会话数"
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '30s', target: 20 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_waiting: ['p(95)<2000'],    // 首 token 时延 p95 < 2s（先给个保守基线，跑通后再校准）
    http_req_duration: ['p(95)<15000'],  // 整条流 p95 < 15s
    http_req_failed: ['rate<0.05'],
  },
};

export default function () {
  const payload = JSON.stringify({ message: '用一句话介绍这个项目' });
  const params = { headers: { 'Content-Type': 'application/json' } };
  const res = http.post(`${BASE}${PATH}`, payload, params);
  check(res, {
    'SSE 200': (r) => r.status === 200,
    '包含 [DONE] 哨兵': (r) => r.body !== null && r.body.indexOf('[DONE]') !== -1,
  });
  sleep(1);
}
