import { Input, Modal } from 'antd'

/**
 * 二次确认弹窗（对应原 ElMessageBox.confirm）
 * 用户确认 -> resolve(true)；取消 -> reject（调用方 catch 后直接 return 即可）
 */
export function confirmDialog({
  title = '提示',
  content,
  okText = '确定',
  cancelText = '取消',
  danger = false,
}) {
  return new Promise((resolve, reject) => {
    Modal.confirm({
      title,
      content,
      okText,
      cancelText,
      okButtonProps: danger ? { danger: true } : undefined,
      onOk: () => resolve(true),
      onCancel: () => reject(new Error('cancel')),
    })
  })
}

/**
 * 文本输入弹窗（对应原 ElMessageBox.prompt）
 * 确认 -> resolve(输入值)；取消 -> reject
 */
export function promptDialog({
  title = '请输入',
  placeholder = '',
  initialValue = '',
  okText = '提交',
  cancelText = '取消',
  rows = 3,
}) {
  return new Promise((resolve, reject) => {
    let value = initialValue
    Modal.confirm({
      title,
      okText,
      cancelText,
      width: 460,
      content: (
        <Input.TextArea
          rows={rows}
          defaultValue={initialValue}
          placeholder={placeholder}
          onChange={(e) => {
            value = e.target.value
          }}
          style={{ marginTop: 12 }}
        />
      ),
      onOk: () => resolve(value),
      onCancel: () => reject(new Error('cancel')),
    })
  })
}
