import { useEffect, useState } from 'react'
import { Button, Card, Descriptions, Empty, Table, Tag, message } from 'antd'
import { ArrowLeftOutlined } from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import { orderApi, payApi } from '../api'
import { getUser } from '../utils/auth'
import { confirmDialog } from '../utils/dialog'
import { formatTime, payMethodText, statusTag, statusText } from '../utils/order'

export default function OrderDetailView() {
  const navigate = useNavigate()
  const { orderNo } = useParams()
  const userId = getUser()?.userId || localStorage.getItem('userId') || ''

  const [order, setOrder] = useState(null)
  const [cancelling, setCancelling] = useState(false)
  const [paying, setPaying] = useState(false)
  const [refunding, setRefunding] = useState(false)

  useEffect(() => {
    fetchOrder()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderNo])

  async function fetchOrder() {
    try {
      const { data } = await orderApi.get(`/${orderNo}`, { headers: { 'X-User-Id': userId } })
      if (data.code === 200) {
        setOrder(data.data)
      } else {
        message.error(data.message)
      }
    } catch {
      message.error('获取订单详情失败')
    }
  }

  async function cancelOrder() {
    try {
      await confirmDialog({ content: '确定取消该订单吗？' })
    } catch {
      return // 用户取消
    }
    setCancelling(true)
    try {
      const { data } = await orderApi.put(`/${orderNo}/cancel`, null, {
        headers: { 'X-User-Id': userId },
      })
      if (data.code === 200) {
        message.success('订单已取消')
        fetchOrder()
      } else {
        message.error(data.message)
      }
    } catch (e) {
      message.error('取消失败：' + (e.response?.data?.message || e.message))
    } finally {
      setCancelling(false)
    }
  }

  async function retryPay() {
    setPaying(true)
    try {
      const { data } = await payApi.post(
        '/create',
        { orderNo, paymentMethod: order.paymentMethod || 'MOCK' },
        { headers: { 'X-User-Id': userId } }
      )
      if (data.code === 200) {
        const d = data.data
        if (d.payType === 'QRCODE' && d.codeUrl) {
          // 扫码类渠道（支付宝/微信/银联）：进入收银台渲染二维码 + 轮询
          navigate({
            pathname: '/mock-pay',
            search: `?orderNo=${orderNo}&amount=${d.payAmount}&payType=${d.payType}&codeUrl=${encodeURIComponent(
              d.codeUrl
            )}&payUrl=${encodeURIComponent(d.payUrl || '')}&expireTime=${encodeURIComponent(
              d.expireTime || ''
            )}`,
          })
        } else if (d.payUrl) {
          // 跳转型渠道：新窗口打开收银台
          window.open(d.payUrl, '_blank')
        } else {
          message.error('未获取到支付信息')
        }
      } else {
        message.error(data.message)
      }
    } catch (e) {
      message.error('支付失败：' + (e.response?.data?.message || e.message))
    } finally {
      setPaying(false)
    }
  }

  async function refundOrder() {
    try {
      await confirmDialog({ content: '确定要模拟退款吗？退款后会回补库存并更新订单状态。' })
    } catch {
      return // 用户取消
    }
    setRefunding(true)
    try {
      const { data } = await payApi.post(
        '/mock/refund',
        { orderNo },
        { headers: { 'X-User-Id': userId } }
      )
      if (data.code === 200) {
        message.success('退款成功')
        fetchOrder()
      } else {
        message.error(data.message)
      }
    } catch (e) {
      message.error('退款失败：' + (e.response?.data?.message || e.message))
    } finally {
      setRefunding(false)
    }
  }

  const columns = [
    { title: '商品名称', dataIndex: 'productName' },
    {
      title: '单价',
      dataIndex: 'productPrice',
      width: 110,
      render: (v) => `¥${Number(v).toFixed(2)}`,
    },
    { title: '数量', dataIndex: 'quantity', width: 80 },
    {
      title: '小计',
      width: 110,
      render: (_, row) => `¥${Number(row.subtotal).toFixed(2)}`,
    },
  ]

  return (
    <div className="order-detail-view">
      <div className="back-header">
        <Button type="text" icon={<ArrowLeftOutlined />} onClick={() => navigate(-1)} />
        <span>订单 {orderNo}</span>
      </div>

      {order ? (
        <>
          <Card
            hoverable
            className="section"
            title={
              <div className="card-header">
                <span>订单信息</span>
                <Tag color={statusTag(order.status)}>{statusText(order.status)}</Tag>
              </div>
            }
          >
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="订单号">{order.orderNo}</Descriptions.Item>
              <Descriptions.Item label="订单状态">{statusText(order.status)}</Descriptions.Item>
              <Descriptions.Item label="商品总额">
                ¥{Number(order.totalAmount || 0).toFixed(2)}
              </Descriptions.Item>
              <Descriptions.Item label="优惠金额">
                -¥{Number(order.discountAmount || 0).toFixed(2)}
              </Descriptions.Item>
              <Descriptions.Item label="应付金额">
                ¥{Number(order.payAmount || 0).toFixed(2)}
              </Descriptions.Item>
              <Descriptions.Item label="支付方式">
                {payMethodText(order.paymentMethod)}
              </Descriptions.Item>
              <Descriptions.Item label="下单时间">{formatTime(order.createTime)}</Descriptions.Item>
              <Descriptions.Item label="支付截止">{formatTime(order.expireTime)}</Descriptions.Item>
            </Descriptions>
          </Card>

          <Card hoverable className="section" title="商品清单">
            <Table
              rowKey={(r, i) => r.productName ?? i}
              columns={columns}
              dataSource={order.items || []}
              pagination={false}
            />
          </Card>

          <div className="actions">
            {order.status === 'PENDING_PAY' && (
              <>
                <Button danger loading={cancelling} onClick={cancelOrder}>
                  取消订单
                </Button>
                <Button type="primary" loading={paying} onClick={retryPay}>
                  立即支付
                </Button>
              </>
            )}
            {order.status === 'PAID' && (
              <Button type="primary" style={{ background: '#e6a23c' }} loading={refunding} onClick={refundOrder}>
                模拟退款
              </Button>
            )}
          </div>
        </>
      ) : (
        <Empty description="加载中或订单不存在" style={{ marginTop: 40 }} />
      )}
    </div>
  )
}
