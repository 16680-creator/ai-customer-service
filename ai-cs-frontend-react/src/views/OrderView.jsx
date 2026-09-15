import { useEffect, useState } from 'react'
import { Button, Empty, Table, Tag, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { orderApi } from '../api'
import { getUser } from '../utils/auth'
import { formatTime, statusTag, statusText } from '../utils/order'

export default function OrderView() {
  const navigate = useNavigate()
  const userId = getUser()?.userId || localStorage.getItem('userId') || ''
  const [orders, setOrders] = useState([])

  useEffect(() => {
    fetchOrders()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function fetchOrders() {
    try {
      const { data } = await orderApi.get('/list', { headers: { 'X-User-Id': userId } })
      if (data.code === 200) {
        setOrders(data.data || [])
      } else {
        message.error(data.message)
      }
    } catch {
      message.error('获取订单列表失败')
    }
  }

  function goDetail(row) {
    navigate(`/order/${row.orderNo}`)
  }

  const columns = [
    { title: '订单号', dataIndex: 'orderNo', minWidth: 180 },
    {
      title: '状态',
      width: 110,
      render: (_, row) => <Tag color={statusTag(row.status)}>{statusText(row.status)}</Tag>,
    },
    {
      title: '商品',
      minWidth: 200,
      render: (_, row) =>
        (row.items || []).map((i) => `${i.productName}×${i.quantity}`).join('、') || '-',
    },
    {
      title: '应付金额',
      width: 120,
      render: (_, row) => `¥${Number(row.payAmount || 0).toFixed(2)}`,
    },
    {
      title: '下单时间',
      width: 170,
      render: (_, row) => formatTime(row.createTime),
    },
    {
      title: '操作',
      width: 110,
      render: (_, row) => (
        <Button
          type="link"
          size="small"
          onClick={(e) => {
            e.stopPropagation()
            goDetail(row)
          }}
        >
          详情
        </Button>
      ),
    },
  ]

  return (
    <div className="order-view">
      <h2>我的订单</h2>

      <Table
        rowKey="orderNo"
        className="clickable"
        columns={columns}
        dataSource={orders}
        style={{ marginTop: 16 }}
        pagination={false}
        onRow={(row) => ({ onClick: () => goDetail(row) })}
        locale={{ emptyText: <Empty description="暂无订单" /> }}
      />
    </div>
  )
}
