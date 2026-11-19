import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it } from 'vitest'

import { permission } from './permission'

/**
 * 按钮级权限指令的最小可用契约：
 * - 有权限：元素正常保留、可点击；
 * - 无权限：元素从 DOM 移除（而不是 display:none —— 改个 DevTools 样式就绕过了）；
 * - v-permission="['admin','agent']" 任一命中即保留。
 */
const Host = {
  directives: { permission },
  props: { role: { type: [String, Array], default: null } },
  template: `
    <div class="toolbar">
      <button v-permission="role" class="guarded">退款审批</button>
    </div>
  `,
}

function mountGuarded(role) {
  return mount(Host, { props: { role } })
}

function seedUser(role) {
  localStorage.setItem('aics_token', 'jwt')
  localStorage.setItem('aics_user', JSON.stringify({ username: 'alice', role }))
}

describe('v-permission：按钮级权限', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('有权限时保留元素', () => {
    seedUser('admin')

    const wrapper = mountGuarded('admin')

    expect(wrapper.find('.guarded').exists()).toBe(true)
  })

  it('无权限时移除元素', () => {
    seedUser('user')

    const wrapper = mountGuarded('admin')

    expect(wrapper.find('.guarded').exists()).toBe(false)
  })

  it('数组形式任一角色命中即保留', () => {
    seedUser('agent')

    const wrapper = mountGuarded(['admin', 'agent'])

    expect(wrapper.find('.guarded').exists()).toBe(true)
  })

  it('数组形式全部不命中则移除', () => {
    seedUser('user')

    const wrapper = mountGuarded(['admin', 'agent'])

    expect(wrapper.find('.guarded').exists()).toBe(false)
  })

  it('未登录时一律移除', () => {
    const wrapper = mountGuarded('admin')

    expect(wrapper.find('.guarded').exists()).toBe(false)
  })

  it('元素没有父节点时退化为隐藏，并留下排查标记', () => {
    seedUser('user')

    // 直接调用钩子：模拟"指令挂在脱离文档的元素上"，此时 removeChild 无从下手
    const el = document.createElement('button')
    el.className = 'guarded'
    permission.mounted(el, { value: 'admin' })

    expect(el.style.display).toBe('none')
    expect(el.getAttribute('data-permission-denied')).toBe('admin')
  })
})
