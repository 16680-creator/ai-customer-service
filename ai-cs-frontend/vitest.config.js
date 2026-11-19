import { defineConfig, mergeConfig } from 'vitest/config'
import viteConfig from './vite.config.js'

// 复用 vite.config.js：alias、define 等构建期解析规则与测试保持一致，
// 否则测试里 `@/xxx` 能过、构建却失败（两套配置漂移是常见事故）。
// vite.config.js 是函数式配置（要读 mode 下的 .env），所以这里显式传一个 mode 求值。
const resolved = typeof viteConfig === 'function'
  ? viteConfig({ mode: 'test', command: 'serve' })
  : viteConfig

export default mergeConfig(
  resolved,
  defineConfig({
    test: {
      environment: 'jsdom',
      include: ['src/**/*.test.js'],
      // 只跑 src 下的单测；E2E 交给 Playwright（见 06/15 文档，尚未落地 ⚠️）
      exclude: ['node_modules/**', 'dist/**'],
      restoreMocks: true,
      clearMocks: true,
    },
  })
)
