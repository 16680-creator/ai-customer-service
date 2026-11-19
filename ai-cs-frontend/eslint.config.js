import js from '@eslint/js'
import pluginVue from 'eslint-plugin-vue'
import globals from 'globals'

// ESLint 9+ 的 flat config（ESLint 10 已完全移除 .eslintrc 支持）。
// 之前前端没有任何 lint 脚本，`npm run build` 是唯一的前端门禁，
// 语法错误能拦住、但未使用变量 / v-for 缺 key / 模板里误用 this 这类问题拦不住
// （见 learning-docs/17-研发效能与工程实践/08-质量门禁与自动化.md:100,107）。
export default [
  {
    // 不检查依赖与产物；dist 里的压缩代码既慢又没意义
    ignores: ['dist/**', 'node_modules/**', 'coverage/**'],
  },

  js.configs.recommended,
  ...pluginVue.configs['flat/recommended'],

  {
    files: ['**/*.{js,vue}'],
    languageOptions: {
      ecmaVersion: 'latest',
      sourceType: 'module',
      globals: {
        ...globals.browser,
        ...globals.node,
      },
    },
    rules: {
      // Vue 3 组合式 API 下这些规则误报率高：
      // 组件名用单词是允许的（views 下都是 XxxView，够清晰）
      'vue/multi-word-component-names': 'off',
      // 项目里大量 el-table / el-form 的可选属性顺序不一致，强制排序只会制造 diff 噪音
      'vue/attributes-order': 'off',
      'vue/max-attributes-per-line': 'off',
      'vue/singleline-html-element-content-newline': 'off',

      // 纯排版规则（换行位置、引号风格、自闭合写法）一律关掉：
      // 本项目没有接 Prettier，这些规则只会把已有文件重排一遍、制造几百行无关 diff，
      // 却抓不到任何 bug。格式化交给后续引入 Prettier 时一次性处理（见 06/15 ⚠️）。
      'vue/html-self-closing': 'off',
      'vue/multiline-html-element-content-newline': 'off',
      'vue/first-attribute-linebreak': 'off',
      'vue/html-closing-bracket-newline': 'off',
      'vue/html-quotes': 'off',

      // 但这些要真拦：
      'vue/no-unused-vars': 'error', // v-for/插槽里声明了没用到的变量，通常是重构残留
      'vue/no-v-html': 'error', // v-html 是 XSS 入口，本项目尚无消毒依赖（见 06/18）
      'no-unused-vars': ['error', { argsIgnorePattern: '^_', caughtErrors: 'none' }],
      // ESLint 10 新规则，会把 `let rows = []` + try 内重新赋值的兜底写法判为"无用赋值"。
      // 那是为了给 catch 分支留一个可用的默认值，属于有意为之，关掉。
      'no-useless-assignment': 'off',
      'no-console': ['warn', { allow: ['warn', 'error'] }],
      'no-debugger': 'error',
    },
  },

  {
    // 测试文件里 describe/it/expect 是全局注入的，需要显式声明，否则 no-undef 会报一片
    files: ['**/*.test.js'],
    languageOptions: {
      globals: {
        ...globals.node,
        describe: 'readonly',
        it: 'readonly',
        expect: 'readonly',
        vi: 'readonly',
        beforeEach: 'readonly',
        afterEach: 'readonly',
      },
    },
    rules: {
      // 测试里断言失败要打印上下文，允许 console 输出
      'no-console': 'off',
    },
  },

  {
    // 构建/配置文件是 Node 环境跑 ESM，用不到浏览器全局
    files: ['*.config.js', 'eslint.config.js'],
    languageOptions: {
      globals: { ...globals.node },
    },
  },
]
