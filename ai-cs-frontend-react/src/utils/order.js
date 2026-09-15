/** 订单相关的展示映射（订单列表 / 订单详情共用） */
const STATUS_TEXT = {
  PENDING_PAY: '待支付',
  PAID: '已支付',
  REFUNDING: '退款中',
  CANCELLED: '已取消',
  REFUNDED: '已退款',
}

const STATUS_COLOR = {
  PENDING_PAY: 'warning',
  PAID: 'success',
  REFUNDING: 'warning',
  CANCELLED: 'default',
  REFUNDED: 'default',
}

const PAY_METHOD_TEXT = {
  MOCK: '模拟支付',
  ALIPAY: '支付宝',
  WECHAT: '微信支付',
  BANK_CARD: '银行卡',
  UNIONPAY: '银联云闪付',
}

export function statusText(status) {
  return STATUS_TEXT[status] || status
}

export function statusTag(status) {
  return STATUS_COLOR[status] || 'default'
}

export function payMethodText(method) {
  return PAY_METHOD_TEXT[method] || method || '-'
}

export function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : '-'
}
