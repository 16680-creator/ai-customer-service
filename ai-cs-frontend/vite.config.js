import { fileURLToPath, URL } from 'node:url'
import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 第三个参数 '' = 加载全部前缀（默认只加载 VITE_ 前缀）。
  // 这里只用于 dev server 的 proxy 目标，不会打进前端产物。
  const env = loadEnv(mode, process.cwd(), '')

  // 开发期网关地址：.env.development 的 VITE_GATEWAY，缺省 http://localhost:8080
  const gateway = env.VITE_PROXY_TARGET || env.VITE_GATEWAY || 'http://localhost:8080'

  return {
    plugins: [vue()],

    resolve: {
      alias: {
        // 统一用 @ 指向 src，避免 ../../../views 这种相对路径（重构移动目录时尤其疼）
        '@': fileURLToPath(new URL('./src', import.meta.url)),
      },
    },

    server: {
      port: 5173,
      proxy: {
        // 开发期把 /api 代理到网关：前端代码里就能写相对路径，和线上 Nginx 行为一致，
        // 也就不会出现"本地能跑、上线跨域"的经典问题（见 06/09 与 06/16）
        '/api': {
          target: gateway,
          changeOrigin: true,
          // SSE（/api/chat/stream/sse、/api/agent/stream/sse）必须关闭缓冲，
          // 否则 token 会被攒成一坨再返回，打字机效果消失
          configure: (proxy) => {
            proxy.on('proxyRes', (proxyRes) => {
              proxyRes.headers['cache-control'] = 'no-cache, no-transform'
              delete proxyRes.headers['content-encoding']
            })
          },
        },
      },
    },

    build: {
      outDir: 'dist',
      // 生产默认不产出 sourcemap（体积 + 源码泄露）；
      // 需要排查线上白屏时用 `npm run build -- --sourcemap` 临时打开
      sourcemap: false,
      chunkSizeWarningLimit: 800,
      rollupOptions: {
        output: {
          // 把变动频率低的第三方库拆成独立 chunk：发版只让业务代码换 hash，
          // vendor 继续命中浏览器缓存。
          // 注意：Vite 8 底层换成了 rolldown，它只接受函数形式的 manualChunks，
          // 写成对象会直接构建失败：`Invalid type: Expected Function but received Object`。
          manualChunks(id) {
            if (!id.includes('node_modules')) return
            if (id.includes('echarts') || id.includes('zrender')) return 'charts'
            if (id.includes('element-plus') || id.includes('@element-plus')) return 'element'
            if (/[\\/]node_modules[\\/](vue|vue-router|@vue)[\\/]/.test(id)) return 'vue'
          },
        },
      },
    },
  }
})
