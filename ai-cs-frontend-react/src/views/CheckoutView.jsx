import { useEffect, useState } from 'react'
import { Button, Card, Form, Radio, Select, Table, Tag, message } from 'antd'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { cartApi, orderApi } from '../api'
import { getUser } from '../utils/auth'

export default function CheckoutView() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const userId = getUser()?.userId || localStorage.getItem('userId') || ''

  const [confirmInfo, setConfirmInfo] = useState({})
  const [selectedCouponId, setSelectedCouponId] = useState(null)
  const [paymentMethod, setPaymentMethod] = useState('MOCK')
  const [submitting, setSubmitting] = useState(false)

  const idsStr = searchParams.get('ids') || ''

  useEffect(() => {
    loadConfirm()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const usableCoupons = (confirmInfo.availableCoupons || []).filter((c) => c.usable)

  async function loadConfirm() {
    const ids = idsStr.split(',').filter(Boolean).map(Number)
    if (!ids.length) {
      message.warning('请先选择要结算的商品')
      navigate('/cart', { replace: true })
      return
    }
    try {
      const { data } = await cartApi.post(
        '/checkout/confirm',
        { cartItemIds: ids },
        { headers: { 'X-User-Id': userId } }
      )
      if (data.code === 200) {
        setConfirmInfo(data.data)
      } else {
        message.error(data.message)
      }
    } catch {
      message.error('获取结算信息失败')
    }
  }

  async function submitOrder() {
    const ids = idsStr.split(',').filter(Boolean).map(Number)
    setSubmitting(true)
    try {
      const { data } = await orderApi.post(
        '/create',
        {
          cartItemIds: ids,
          couponId: selectedCouponId,
          paymentMethod,
        },
        { headers: { 'X-User-Id': userId } }
      )
      if (data.code === 200) {
        message.success('下单成功，请尽快完成支付')
        navigate(`/order/${data.data.orderNo}`)
      } else {
        message.error(data.message)
      }
    } catch (e) {
      message.error('下单失败：' + (e.response?.data?.message || e.message))
    } finally {
      setSubmitting(false)
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
    <div className="checkout-view">
      <h2>确认订单</h2>

      <Card hoverable className="section" title="商品清单" style={{ marginTop: 16 }}>
        <Table
          rowKey={(r, i) => r.productName ?? i}
          columns={columns}
          dataSource={confirmInfo.items || []}
          pagination={false}
        />
      </Card>

      <Card hoverable className="section" title="优惠与支付">
        <Form labelCol={{ span: 4 }} wrapperCol={{ span: 20 }}>
          <Form.Item label="商品总额">
            <span>¥{Number(confirmInfo.totalAmount || 0).toFixed(2)}</span>
          </Form.Item>

          {confirmInfo.fullReduction?.applied && (
            <Form.Item label="满减优惠">
              <span className="discount">
                -¥{Number(confirmInfo.fullReduction.amount || 0).toFixed(2)}
              </span>
              <span className="discount-tip">（{confirmInfo.fullReduction.ruleName}）</span>
            </Form.Item>
          )}

          <Form.Item label="优惠券">
            <Select
              value={selectedCouponId}
              onChange={(v) => setSelectedCouponId(v ?? null)}
              placeholder="选择优惠券"
              allowClear
              style={{ width: 320 }}
              options={usableCoupons.map((c) => ({
                label: `${c.couponName}（满${Number(c.minOrderAmount).toFixed(0)}减${Number(c.amount).toFixed(2)}）`,
                value: c.id,
              }))}
            />
            {!usableCoupons.length && (
              <Tag style={{ marginLeft: 10 }}>暂无可用优惠券</Tag>
            )}
          </Form.Item>

          <Form.Item label="支付方式">
            <Radio.Group value={paymentMethod} onChange={(e) => setPaymentMethod(e.target.value)}>
              <Radio value="MOCK">模拟支付（演示）</Radio>
              <Radio value="ALIPAY">支付宝（扫码）</Radio>
              <Radio value="WECHAT">微信支付（扫码）</Radio>
              <Radio value="UNIONPAY">银联云闪付（扫码）</Radio>
            </Radio.Group>
            <div className="pay-tip">
              支付宝/微信/银联渠道代码已实现，配置商户参数（Nacos pay.*）后即可使用；本地演示请选「模拟支付」。
            </div>
          </Form.Item>

          <Form.Item label="应付金额">
            <span className="pay-amount">¥{Number(confirmInfo.payAmount || 0).toFixed(2)}</span>
          </Form.Item>
        </Form>
      </Card>

      <div className="actions">
        <Button onClick={() => navigate(-1)}>返回</Button>
        <Button type="primary" size="large" loading={submitting} onClick={submitOrder}>
          提交订单
        </Button>
      </div>
    </div>
  )
}
