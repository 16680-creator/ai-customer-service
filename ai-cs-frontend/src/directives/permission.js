import { hasRole } from '../utils/auth'

/**
 * v-permission：按钮级权限指令。
 *
 * 用法：
 *   <el-button v-permission="'admin'">删除用户</el-button>
 *   <el-button v-permission="['admin', 'agent']">退款审批</el-button>
 *
 * 设计取舍：
 * - 无权限时直接 `removeChild`，而不是 `display:none` —— 后者在 DevTools 里改一行样式就能点；
 * - 前端隐藏只是"不给看"，真正的越权拦截必须在后端（网关 JWT + 方法级 @PreAuthorize）；
 * - 仅做初始渲染判断：本项目角色在登录时一次性写入 localStorage，切换角色必然重新登录，
 *   所以不需要 watch/updated 钩子（若将来支持权限热更新，要补 updated）。
 */
export const permission = {
  mounted(el, binding) {
    const required = binding.value
    if (!required) return

    const list = Array.isArray(required) ? required : [required]
    const allowed = list.some((role) => hasRole(role))
    if (allowed) return

    if (el.parentNode) {
      el.parentNode.removeChild(el)
      return
    }

    // 兜底：指令直接挂在组件根节点上时还没有父节点，removeChild 无从下手。
    // 不能悄悄放过（否则按钮可见 = 越权入口），退化为隐藏 + 标记原因，方便排查：
    //   控制台查询 document.querySelectorAll('[data-permission-denied]')
    el.style.display = 'none'
    el.setAttribute('data-permission-denied', list.join(','))
  },
}

/** 供 main.js 批量注册 */
export default {
  install(app) {
    app.directive('permission', permission)
  },
}
