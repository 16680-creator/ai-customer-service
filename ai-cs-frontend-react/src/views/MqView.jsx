import { useEffect, useState } from 'react'
import { Alert, Button, Card, Col, Descriptions, Empty, Form, Input, Modal, Row, Switch, Table, Tabs, Tag, message } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { mqApi } from '../api'
import { confirmDialog } from '../utils/dialog'

function woStatusType(status) {
  return status === 'DONE' ? 'success' : status === 'FAILED' ? 'error' : 'processing'
}

export default function MqView() {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [activeTab, setActiveTab] = useState('cluster')

  const [overview, setOverview] = useState({})
  const [brokers, setBrokers] = useState([])
  const [topics, setTopics] = useState([])
  const [groups, setGroups] = useState([])

  const [topicDialog, setTopicDialog] = useState(false)
  const [topicDetail, setTopicDetail] = useState({})
  const [groupDialog, setGroupDialog] = useState(false)
  const [groupDetail, setGroupDetail] = useState({})

  // ===== 工单消息闭环演示 =====
  const [woForm, setWoForm] = useState({ title: '', content: '', simulateFail: false })
  const [woSubmitting, setWoSubmitting] = useState(false)
  const [woLoading, setWoLoading] = useState(false)
  const [workOrders, setWorkOrders] = useState([])
  const [frLoading, setFrLoading] = useState(false)
  const [failRecords, setFailRecords] = useState([])

  useEffect(() => {
    loadAll()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // 首次切到演示 Tab 时加载数据
  useEffect(() => {
    if (activeTab === 'workOrder') loadWorkOrders()
    if (activeTab === 'failRecords') loadFailRecords()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [activeTab])

  async function loadAll() {
    setLoading(true)
    setError('')
    try {
      const [o, b, t, g] = await Promise.all([
        mqApi.get('/overview'),
        mqApi.get('/cluster'),
        mqApi.get('/topics'),
        mqApi.get('/groups'),
      ])
      if (o.data.code === 200) setOverview(o.data.data || {})
      if (b.data.code === 200) setBrokers(b.data.data || [])
      if (t.data.code === 200) setTopics(t.data.data || [])
      if (g.data.code === 200) setGroups(g.data.data || [])
    } catch (e) {
      setError(e.response?.data?.message || e.message || 'RocketMQ 调度服务不可用')
    } finally {
      setLoading(false)
    }
  }

  async function openTopic(row) {
    try {
      const { data } = await mqApi.get(`/topic/${encodeURIComponent(row.topic)}`)
      if (data.code === 200) {
        setTopicDetail(data.data || {})
        setTopicDialog(true)
      } else {
        message.error(data.message)
      }
    } catch (e) {
      message.error(e.response?.data?.message || '获取 Topic 详情失败')
    }
  }

  async function openGroup(row) {
    try {
      const { data } = await mqApi.get(`/group/${encodeURIComponent(row.group)}`)
      if (data.code === 200) {
        setGroupDetail(data.data || {})
        setGroupDialog(true)
      } else {
        message.error(data.message)
      }
    } catch (e) {
      message.error(e.response?.data?.message || '获取消费组详情失败')
    }
  }

  async function loadWorkOrders() {
    setWoLoading(true)
    try {
      const { data } = await mqApi.get('/work-order/list')
      if (data.code === 200) setWorkOrders(data.data || [])
    } catch (e) {
      message.error(e.response?.data?.message || '获取工单列表失败')
    } finally {
      setWoLoading(false)
    }
  }

  async function createWorkOrder() {
    if (!woForm.title || !woForm.content) {
      message.warning('请填写标题和内容')
      return
    }
    setWoSubmitting(true)
    try {
      const { data } = await mqApi.post('/work-order/create', woForm)
      if (data.code === 200) {
        message.success(`工单已创建并发送: ${data.data?.ticketNo || ''}`)
        setWoForm({ title: '', content: '', simulateFail: false })
        loadWorkOrders()
      } else {
        message.error(data.message)
      }
    } catch (e) {
      message.error(e.response?.data?.message || '创建工单失败')
    } finally {
      setWoSubmitting(false)
    }
  }

  async function loadFailRecords() {
    setFrLoading(true)
    try {
      const { data } = await mqApi.get('/fail-records/list')
      if (data.code === 200) setFailRecords(data.data || [])
    } catch (e) {
      message.error(e.response?.data?.message || '获取失败记录失败')
    } finally {
      setFrLoading(false)
    }
  }

  async function onRepush(row) {
    try {
      await confirmDialog({
        title: '重新推送',
        content: `确认重新推送工单 ${row.bizKey} 的失败消息？`,
      })
    } catch {
      return // 用户取消
    }
    try {
      const { data } = await mqApi.post(`/fail-records/${row.id}/repush`)
      if (data.code === 200) {
        message.success('已重新投递，消费成功后工单状态将变为 DONE')
        loadFailRecords()
        loadWorkOrders()
      } else {
        message.error(data.message)
      }
    } catch (e) {
      message.error(e.response?.data?.message || '重推失败')
    }
  }

  const stats = [
    { label: 'Broker 数', value: overview.brokerCount ?? '-' },
    { label: 'Topic 数', value: overview.topicCount ?? '-' },
    { label: '消费组数', value: overview.groupCount ?? '-' },
    { label: '总堆积量', value: overview.totalDiff ?? '-' },
  ]

  const brokerColumns = [
    { title: 'Broker', dataIndex: 'brokerName', minWidth: 140 },
    { title: '集群', dataIndex: 'cluster', width: 120 },
    { title: '主节点地址', dataIndex: 'masterAddr', minWidth: 150 },
    {
      title: '从节点',
      minWidth: 160,
      render: (_, row) => (row.slaveAddrs || []).join(', ') || '-',
    },
    { title: '版本', dataIndex: 'version', width: 110 },
    { title: 'CommitLog 磁盘占比', dataIndex: 'commitLogDiskRatio', width: 140 },
    { title: '写消息最大耗时(ms)', dataIndex: 'putMessageEntireTimeMax', width: 150 },
    { title: 'TPS', dataIndex: 'qps', width: 90 },
  ]

  const topicColumns = [
    { title: 'Topic', dataIndex: 'topic', minWidth: 200 },
    { title: 'Broker', minWidth: 160, render: (_, row) => (row.brokers || []).join(', ') || '-' },
    { title: '读队列', dataIndex: 'readQueues', width: 80 },
    { title: '写队列', dataIndex: 'writeQueues', width: 80 },
    { title: '消息量', width: 110, render: (_, row) => row.messageCount ?? '-' },
    {
      title: '操作',
      width: 90,
      render: (_, row) => (
        <Button
          size="small"
          type="link"
          onClick={(e) => {
            e.stopPropagation()
            openTopic(row)
          }}
        >
          详情
        </Button>
      ),
    },
  ]

  const groupColumns = [
    { title: '消费组', dataIndex: 'group', minWidth: 180 },
    { title: '消费 TPS', width: 100, render: (_, row) => row.consumeTps || '-' },
    {
      title: '堆积量',
      width: 110,
      render: (_, row) => <Tag color={row.diff > 0 ? 'error' : 'success'}>{row.diff ?? '-'}</Tag>,
    },
    {
      title: '消费 Topic',
      minWidth: 220,
      render: (_, row) => (row.topics || []).join(', ') || '-',
    },
    {
      title: '操作',
      width: 90,
      render: (_, row) => (
        <Button
          size="small"
          type="link"
          onClick={(e) => {
            e.stopPropagation()
            openGroup(row)
          }}
        >
          详情
        </Button>
      ),
    },
  ]

  const workOrderColumns = [
    { title: '工单号', dataIndex: 'ticketNo', minWidth: 190 },
    { title: '标题', dataIndex: 'title', minWidth: 130 },
    { title: '内容', dataIndex: 'content', minWidth: 200, ellipsis: true },
    {
      title: '状态',
      width: 100,
      render: (_, row) => <Tag color={woStatusType(row.status)}>{row.status}</Tag>,
    },
    { title: '创建时间', dataIndex: 'createTime', width: 170 },
    { title: '更新时间', dataIndex: 'updateTime', width: 170 },
  ]

  const failRecordColumns = [
    { title: 'ID', dataIndex: 'id', width: 70 },
    { title: '工单号', dataIndex: 'bizKey', minWidth: 180 },
    { title: '消息ID', dataIndex: 'msgId', minWidth: 190, ellipsis: true },
    { title: '失败原因', dataIndex: 'failReason', minWidth: 230, ellipsis: true },
    { title: '已重试', dataIndex: 'reconsumeTimes', width: 80 },
    {
      title: '记录状态',
      width: 110,
      render: (_, row) => <Tag color={row.status === 'PENDING' ? 'error' : 'default'}>{row.status}</Tag>,
    },
    { title: '落库时间', dataIndex: 'createTime', width: 170 },
    { title: '重推时间', dataIndex: 'repushTime', width: 170 },
    {
      title: '操作',
      width: 110,
      fixed: 'right',
      render: (_, row) => (
        <Button
          size="small"
          danger
          disabled={row.status !== 'PENDING'}
          onClick={() => onRepush(row)}
        >
          重新推送
        </Button>
      ),
    },
  ]

  return (
    <div className="mq-view">
      <div className="mq-header">
        <h2>RocketMQ 调度中心</h2>
        <Button type="primary" icon={<ReloadOutlined />} loading={loading} onClick={loadAll}>
          刷新
        </Button>
      </div>

      {error && (
        <Alert type="error" showIcon message={error} style={{ marginBottom: 16 }} />
      )}

      {/* 概览卡片 */}
      <Row gutter={16} className="stat-row">
        {stats.map((s) => (
          <Col span={6} key={s.label}>
            <Card hoverable styles={{ body: { padding: 20 } }}>
              <div className="stat-value">{s.value}</div>
              <div className="stat-label">{s.label}</div>
            </Card>
          </Col>
        ))}
      </Row>

      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        style={{ marginTop: 16 }}
        items={[
          {
            key: 'cluster',
            label: '集群 / Broker',
            children: (
              <Table
                rowKey="brokerName"
                columns={brokerColumns}
                dataSource={brokers}
                bordered
                loading={loading}
                pagination={false}
                scroll={{ x: 1100 }}
                locale={{ emptyText: <Empty description="暂无 Broker" /> }}
              />
            ),
          },
          {
            key: 'topics',
            label: 'Topic',
            children: (
              <Table
                rowKey="topic"
                className="clickable"
                columns={topicColumns}
                dataSource={topics}
                bordered
                loading={loading}
                pagination={false}
                scroll={{ x: 800 }}
                onRow={(row) => ({ onClick: () => openTopic(row) })}
                locale={{ emptyText: <Empty description="暂无 Topic" /> }}
              />
            ),
          },
          {
            key: 'groups',
            label: '消费组 / 堆积',
            children: (
              <Table
                rowKey="group"
                className="clickable"
                columns={groupColumns}
                dataSource={groups}
                bordered
                loading={loading}
                pagination={false}
                scroll={{ x: 800 }}
                onRow={(row) => ({ onClick: () => openGroup(row) })}
                locale={{ emptyText: <Empty description="暂无消费组" /> }}
              />
            ),
          },
          {
            key: 'workOrder',
            label: '工单演示',
            children: (
              <>
                <Card style={{ marginBottom: 16 }}>
                  <Form layout="inline">
                    <Form.Item label="标题">
                      <Input
                        value={woForm.title}
                        onChange={(e) => setWoForm((p) => ({ ...p, title: e.target.value }))}
                        placeholder="工单标题"
                        style={{ width: 200 }}
                      />
                    </Form.Item>
                    <Form.Item label="内容">
                      <Input
                        value={woForm.content}
                        onChange={(e) => setWoForm((p) => ({ ...p, content: e.target.value }))}
                        placeholder="工单内容"
                        style={{ width: 280 }}
                      />
                    </Form.Item>
                    <Form.Item label="模拟消费失败">
                      <Switch
                        checked={woForm.simulateFail}
                        onChange={(v) => setWoForm((p) => ({ ...p, simulateFail: v }))}
                      />
                    </Form.Item>
                    <Form.Item>
                      <Button type="primary" loading={woSubmitting} onClick={createWorkOrder}>
                        创建并发送
                      </Button>
                      <Button style={{ marginLeft: 10 }} onClick={loadWorkOrders}>
                        刷新工单
                      </Button>
                    </Form.Item>
                  </Form>
                  <div className="wo-tip">
                    勾选「模拟消费失败」：消费者抛异常 → RocketMQ 自动重试 3 次（约 10s/30s/1m）→ 进入死信队列 →
                    失败记录落库，到「失败记录」Tab 重推后恢复正常消费。
                  </div>
                </Card>
                <Table
                  rowKey="ticketNo"
                  columns={workOrderColumns}
                  dataSource={workOrders}
                  bordered
                  loading={woLoading}
                  pagination={false}
                  scroll={{ x: 1000 }}
                  locale={{ emptyText: <Empty description="暂无工单，创建一条试试" /> }}
                />
              </>
            ),
          },
          {
            key: 'failRecords',
            label: '失败记录',
            children: (
              <>
                <div style={{ marginBottom: 12 }}>
                  <Button icon={<ReloadOutlined />} onClick={loadFailRecords}>
                    刷新失败记录
                  </Button>
                </div>
                <Table
                  rowKey="id"
                  columns={failRecordColumns}
                  dataSource={failRecords}
                  bordered
                  loading={frLoading}
                  pagination={false}
                  scroll={{ x: 1300 }}
                  locale={{
                    emptyText: <Empty description="暂无消费失败记录（勾选「模拟消费失败」创建工单即可触发）" />,
                  }}
                />
              </>
            ),
          },
        ]}
      />

      {/* Topic 详情 */}
      <Modal
        open={topicDialog}
        title={`Topic 详情：${topicDetail.topic || ''}`}
        width={640}
        footer={null}
        onCancel={() => setTopicDialog(false)}
      >
        <Table
          rowKey={(r, i) => r.broker ?? i}
          columns={[
            { title: 'Broker', dataIndex: 'broker', minWidth: 140 },
            { title: '读队列', dataIndex: 'readQueueNums', width: 90 },
            { title: '写队列', dataIndex: 'writeQueueNums', width: 90 },
            { title: '权限(perm)', dataIndex: 'perm', width: 100 },
          ]}
          dataSource={topicDetail.queues || []}
          size="small"
          bordered
          pagination={false}
        />
      </Modal>

      {/* 消费组详情 */}
      <Modal
        open={groupDialog}
        title={`消费组详情：${groupDetail.group || ''}`}
        width={760}
        footer={null}
        onCancel={() => setGroupDialog(false)}
      >
        <Descriptions column={2} bordered size="small" style={{ marginBottom: 12 }}>
          <Descriptions.Item label="消费 TPS">{groupDetail.consumeTps || '-'}</Descriptions.Item>
        </Descriptions>
        <Table
          rowKey={(r, i) => `${r.topic}-${r.queueId}-${i}`}
          columns={[
            { title: 'Topic', dataIndex: 'topic', minWidth: 160 },
            { title: 'Broker', dataIndex: 'broker', minWidth: 120 },
            { title: '队列', dataIndex: 'queueId', width: 60 },
            { title: 'Broker Offset', dataIndex: 'brokerOffset', width: 120 },
            { title: '消费 Offset', dataIndex: 'consumerOffset', width: 120 },
            {
              title: '堆积',
              width: 90,
              render: (_, row) => <Tag color={row.diff > 0 ? 'error' : 'success'}>{row.diff}</Tag>,
            },
          ]}
          dataSource={groupDetail.queues || []}
          size="small"
          bordered
          pagination={false}
          scroll={{ x: 800 }}
        />
      </Modal>
    </div>
  )
}
