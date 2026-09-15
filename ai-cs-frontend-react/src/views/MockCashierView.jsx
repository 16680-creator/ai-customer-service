import { useEffect, useState } from 'react'
import { Alert, Button, Card, Descriptions, Skeleton, Tag, message } from 'antd'
import QRCode from 'qrcode'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { payApi } from '../api'
import { getUser } from '../utils/auth'

const STATUS_TEXT = {
  PENDING_PAY: '待支付',
  PAID: '已支付',
  CANCELLED: '已取消',
  REFUNDED: '已退款',
}

function statusText(s) {
  return STATUS_TEXT[s] || s
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

export default function MockCashierView() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  const orderNo = searchParams.get('orderNo') || ''
  const amount = searchParams.get('amount') || '-'
  const payType = searchParams.get('payType') || 'REDIRECT'
  const codeUrl = searchParams.get('codeUrl') || ''
  const expireTime = searchParams.get('expireTime') || ''
  const userId = getUser()?.userId || ''

  const [remainMs, setRemainMs] = useState(0)
  const [expired, setExpired] = useState(false)
  const [qrDataUrl, setQrDataUrl] = useState('')
  const [status, setStatus] = useState('')
  const [polling, setPolling] = useState(false)
  const [paying, setPaying] = useState(false)
  const [result, setResult] = useState(null)

  const isQrMode = payType === 'QRCODE' && !!codeUrl

  // 缺少订单号时直接回订单列表
  useEffect(() => {
    if (!orderNo) {
      message.warning('缺少订单号')
      navigate('/order', { replace: true })
    }
  }, [orderNo, navigate])

  // 真实渠道：生成支付二维码
  useEffect(() => {
    if (!isQrMode) return
    QRCode.toDataURL(codeUrl, { width: 260, margin: 1 })
      .then(setQrDataUrl)
      .catch(() => message.error('二维码生成失败'))
  }, [isQrMode, codeUrl])

  /** 支付截止倒计时（订单超时后自动提示） */
  useEffect(() => {
    if (!expireTime) return
    const end = new Date(String(expireTime).replace('T', ' ').replace(/-/g, '/')).getTime()
    if (Number.isNaN(end)) return
    const timer = setInterval(() => {
      const remain = end - Date.now()
      setRemainMs(remain > 0 ? remain : 0)
      if (remain <= 0) {
        clearInterval(timer)
        setExpired(true)
        setPolling(false)
        setResult({
          type: 'warning',
          text: '订单已超时，未在有效期内支付。返回订单查看，系统会自动关单并释放库存。',
        })
      }
    }, 1000)
    return () => clearInterval(timer)
  }, [expireTime])

  /** 轮询支付状态：后端 /pay/status 内置"查单兜底"，渠道已支付会自动落库 */
  useEffect(() => {
    if (!isQrMode || !orderNo) return undefined
    let cancelled = false
    ;(async () => {
      setPolling(true)
      for (let i = 0; i < 120; i++) {
        await sleep(2500)
        if (cancelled) return
        try {
          const { data } = await payApi.get(`/status/${orderNo}`, {
            headers: { 'X-User-Id': userId },
          })
          if (data.code === 200) {
            setStatus(data.data.status)
            if (data.data.status === 'PAID') {
              setResult({
                type: 'success',
                text: `支付成功！订单 ${orderNo} 已更新为「已支付」（查单兜底确认）。`,
              })
              setPolling(false)
              return
            }
            if (data.data.status === 'REFUNDED') {
              setResult({ type: 'info', text: '该订单已退款。' })
              setPolling(false)
              return
            }
            if (data.data.status === 'CANCELLED') {
              setResult({ type: 'warning', text: '订单已取消（超时未支付）。' })
              setPolling(false)
              return
            }
          }
        } catch {
          // 忽略单次轮询失败，继续
        }
      }
      if (cancelled) return
      setPolling(false)
      setResult({
        type: 'info',
        text: '等待支付超时，可点击「刷新状态」或返回订单重新发起支付。',
      })
    })()
    return () => {
      cancelled = true
    }
  }, [isQrMode, orderNo, userId])

  const countdownText = (() => {
    const s = Math.max(0, Math.floor(remainMs / 1000))
    const mm = String(Math.floor(s / 60)).padStart(2, '0')
    const ss = String(s % 60).padStart(2, '0')
    return `${mm}:${ss}`
  })()

  async function refreshStatus() {
    try {
      const { data } = await payApi.get(`/status/${orderNo}`, {
        headers: { 'X-User-Id': userId },
      })
      if (data.code === 200) {
        setStatus(data.data.status)
        if (data.data.status === 'PAID') {
          setResult({ type: 'success', text: '支付成功！订单已更新为「已支付」。' })
        } else if (data.data.status === 'PENDING_PAY') {
          setResult({ type: 'info', text: '仍为待支付，请扫码完成支付。' })
        }
      }
    } catch {
      message.error('刷新状态失败')
    }
  }

  async function doPay(payResult) {
    setPaying(true)
    setResult(null)
    try {
      const { data } = await payApi.post(
        '/mock/pay',
        { orderNo, result: payResult },
        { headers: { 'X-User-Id': userId } }
      )
      if (data.code === 200) {
        if (data.data.status === 'PAID') {
          setResult({
            type: 'success',
            text: `支付成功！订单 ${orderNo} 状态已更新为「已支付」（渠道回调 → 验签 → 幂等更新 → 确认）。`,
          })
        } else if (data.data.status === 'REFUNDED') {
          setResult({ type: 'info', text: '该订单已退款。' })
        } else {
          setResult({
            type: 'warning',
            text: '模拟支付失败/取消，订单仍为「待支付」，可返回订单重新发起支付。',
          })
        }
      } else {
        message.error(data.message || '模拟支付失败')
      }
    } catch (e) {
      message.error(e.response?.data?.message || '模拟支付失败')
    } finally {
      setPaying(false)
    }
  }

  return (
    <div className="mock-cashier">
      <Card
        title={
          <div className="cashier-header">
            <span style={{ fontWeight: 600 }}>
              {isQrMode ? '收银台 - 扫码支付' : '模拟收银台（学习完整支付流程）'}
            </span>
            <Tag color={isQrMode ? 'success' : 'warning'}>
              {isQrMode ? '渠道扫码' : '模拟渠道 MOCK'}
            </Tag>
          </div>
        }
      >
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="订单号">{orderNo}</Descriptions.Item>
          <Descriptions.Item label="支付金额">¥{amount}</Descriptions.Item>
          <Descriptions.Item label="流程说明">
            {isQrMode
              ? '请使用支付宝 / 微信 / 云闪付 App 扫码支付。支付完成后，系统通过「查单兜底」自动把订单更新为已支付（后端主动向渠道查询，无需公网回调地址也能闭环）。'
              : '本页等价于微信/支付宝的收银台。点击「模拟支付成功」= 用户在支付 App 完成支付，随后系统走一遍与真实渠道完全相同的链路：渠道通知 → 验签 → 幂等更新订单状态。'}
          </Descriptions.Item>
        </Descriptions>

        {/* 真实渠道：二维码扫码支付 */}
        {isQrMode ? (
          <div className="qr-box">
            {qrDataUrl ? (
              <img src={qrDataUrl} className="qr-img" alt="支付二维码" />
            ) : (
              <Skeleton.Image active style={{ width: 240, height: 240 }} />
            )}
            <div className="qr-tip">请使用 支付宝 / 微信 / 云闪付 App 扫码支付</div>
            {expireTime && !expired && !status && (
              <div className="countdown">支付剩余时间：{countdownText}</div>
            )}
            <div className="qr-actions">
              <Button loading={polling} onClick={refreshStatus}>
                刷新状态
              </Button>
              <Button onClick={() => navigate(`/order/${orderNo}`)}>返回订单</Button>
            </div>
            {status && (
              <Tag
                color={status === 'PAID' ? 'success' : 'warning'}
                className="status-tag"
                style={{ fontSize: 14, padding: '4px 12px' }}
              >
                {statusText(status)}
              </Tag>
            )}
          </div>
        ) : (
          /* 模拟渠道：模拟支付按钮 */
          <div className="cashier-actions">
            <Button type="primary" size="large" loading={paying} onClick={() => doPay('SUCCESS')}>
              模拟支付成功
            </Button>
            <Button size="large" loading={paying} onClick={() => doPay('FAIL')}>
              模拟支付失败 / 取消
            </Button>
            <Button size="large" onClick={() => navigate(`/order/${orderNo}`)}>
              返回订单
            </Button>
          </div>
        )}

        {result && (
          <Alert
            type={result.type}
            showIcon
            message={result.text}
            style={{ marginTop: 16 }}
          />
        )}
      </Card>
    </div>
  )
}
