/**
 * 运行时配置：目前只有网关地址一处，但它决定了"本地能跑、上线白屏"这类事故。
 *
 * 取值优先级（见 .env.development / .env.production）：
 * - 配置了 VITE_GATEWAY => 用绝对地址（跨域场景，需要网关开 CORS）
 * - 留空 / 未配置      => 空串，请求走相对路径 /api/...，由 Vite dev proxy 或 Nginx 反代
 *
 * 注意不能写成 `env.VITE_GATEWAY || 'http://localhost:8080'`：
 * 空串是 falsy，会导致生产构建回落到 localhost。这正是本文件存在的原因。
 *
 * @param {Record<string, unknown>} env 通常是 import.meta.env
 * @returns {string} 不带结尾斜杠；空串表示同源
 */
export function resolveGateway(env) {
  const raw = env?.VITE_GATEWAY
  if (typeof raw !== 'string') return ''
  return raw.trim().replace(/\/+$/, '')
}

/** 应用内统一从这里取网关地址（axios 与 SSE fetch 共用，避免各处重复判断） */
export const GATEWAY = resolveGateway(import.meta.env)

/** 拼出 API 基础路径：`${GATEWAY}/api` */
export const API_BASE = `${GATEWAY}/api`
