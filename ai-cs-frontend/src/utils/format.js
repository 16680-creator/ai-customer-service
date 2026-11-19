/**
 * 展示层格式化工具：只做"看得懂"，不做业务计算。
 * 单独抽出来的原因：这些函数是最容易被复制粘贴到每个 .vue 里的东西，
 * 一旦分叉就会出现"订单页显示 1.5MB、知识库页显示 1536KB"。
 */
const UNITS = ['B', 'KB', 'MB', 'GB', 'TB', 'PB']

/**
 * 字节数转可读体积（1024 进制）。
 * @param {number} bytes
 * @returns {string} 例如 1536 -> "1.5 KB"；非法值统一返回 "-"
 */
export function formatSize(bytes) {
  if (typeof bytes !== 'number' || !Number.isFinite(bytes) || bytes < 0) return '-'
  if (bytes < 1024) return `${bytes} B`

  let value = bytes
  let unitIndex = 0
  while (value >= 1024 && unitIndex < UNITS.length - 1) {
    value /= 1024
    unitIndex += 1
  }
  return `${value.toFixed(1)} ${UNITS[unitIndex]}`
}

/**
 * 超长字符串中间省略（长订单号 / traceId / 文件 hash 在表格里撑破列宽时用）。
 * @param {string} text
 * @param {number} length 结果总长度（含省略号）；<=0 返回空串
 * @returns {string}
 */
export function truncateMiddle(text, length) {
  if (typeof text !== 'string') return ''
  if (!Number.isFinite(length) || length <= 0) return ''
  if (text.length <= length) return text

  const tailLength = Math.floor((length - 1) / 2)
  const headLength = length - 1 - tailLength
  return `${text.slice(0, headLength)}…${text.slice(text.length - tailLength)}`
}
