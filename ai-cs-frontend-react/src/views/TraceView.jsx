import { useState } from 'react'
import { Alert, Button, Card, Descriptions, Empty, Input, Modal, Table, Tag, message } from 'antd'
import { ClearOutlined, SearchOutlined } from '@ant-design/icons'
import { observabilityApiWrappers } from '../api'

function normalizeSpans(data) {
  if (Array.isArray(data.spans)) return data.spans
  if (data.spansJson) {
    try {
      const parsed = typeof data.spansJson === 'string' ? JSON.parse(data.spansJson) : data.spansJson
      return Array.isArray(parsed) ? parsed : []
    } catch {
      return []
    }
  }
  return []
}

function typeTag(t) {
  return { LLM: 'warning', TOOL: 'success', RETRIEVE: 'default', EMBED: 'processing' }[t] || 'default'
}

export default function TraceView() {
  const [requestId, setRequestId] = useState('')
  const [trace, setTrace] = useState(null)
  const [spans, setSpans] = useState([])
  const [loading, setLoading] = useState(false)
  const [notFound, setNotFound] = useState(false)
  const [loadError, setLoadError] = useState('')
  const [activeSpan, setActiveSpan] = useState(null)

  const maxEnd = spans.reduce(
    (m, s) => Math.max(m, (s.startOffsetMs || 0) + (s.durationMs || 0)),
    1
  )

  function barStyle(s) {
    const start = s.startOffsetMs || 0
    const dur = s.durationMs || 0
    const left = (start / maxEnd) * 100
    const width = Math.max((dur / maxEnd) * 100, 1.5)
    return { left: left + '%', width: width + '%' }
  }

  async function queryTrace() {
    if (!requestId) {
      message.warning('请输入 requestId')
      return
    }
    setLoading(true)
    setNotFound(false)
    setLoadError('')
    setTrace(null)
    setSpans([])
    try {
      const resp = await observabilityApiWrappers.getTrace(requestId)
      const data = resp?.data?.data ?? resp?.data
      if (!data) {
        setNotFound(true)
        return
      }
      setTrace(data)
      // spansJson 可能是字符串(JSON)或已解析数组；spans 字段也可能直接是数组
      setSpans(normalizeSpans(data))
    } catch (e) {
      if (e?.response?.status === 404) {
        setNotFound(true)
      } else {
        setLoadError(e.message || '未知错误')
      }
    } finally {
      setLoading(false)
    }
  }

  const columns = [
    { title: '节点', dataIndex: 'name', minWidth: 200 },
    {
      title: '类型',
      dataIndex: 'type',
      width: 110,
      render: (t) => <Tag color={typeTag(t)}>{t}</Tag>,
    },
    { title: '模型', dataIndex: 'model', width: 160 },
    { title: '相对起点(ms)', dataIndex: 'startOffsetMs', width: 120 },
    { title: '耗时(ms)', dataIndex: 'durationMs', width: 100 },
    { title: 'Token', dataIndex: 'tokenUsage', minWidth: 140 },
    {
      title: '费用($)',
      dataIndex: 'costUsd',
      width: 110,
      render: (v) => (v != null ? Number(v).toFixed(6) : '-'),
    },
    {
      title: '操作',
      width: 90,
      render: (_, row) => (
        <Button size="small" type="link" onClick={() => setActiveSpan(row)}>
          详情
        </Button>
      ),
    },
  ]

  return (
    <div className="trace-page">
      <Card hoverable className="query-card">
        <div className="query-bar">
          <Input
            value={requestId}
            onChange={(e) => setRequestId(e.target.value)}
            placeholder="输入 requestId 查询一次请求的完整调用链"
            allowClear
            style={{ maxWidth: 480 }}
            addonBefore="requestId"
            onPressEnter={queryTrace}
          />
          <Button
            type="primary"
            icon={<SearchOutlined />}
            loading={loading}
            onClick={queryTrace}
          >
            查询链路
          </Button>
          <Button icon={<ClearOutlined />} onClick={() => setRequestId('')}>
            清空
          </Button>
        </div>
        {notFound && (
          <Alert
            type="warning"
            showIcon
            style={{ marginTop: 12 }}
            message="未找到该 requestId 的调用链"
            description="可观测性服务仅保留最近一段时间内的 trace；若请求较久或后端未开启采样，可能查不到。"
          />
        )}
        {loadError && (
          <Alert
            type="error"
            showIcon
            style={{ marginTop: 12 }}
            message={'查询失败: ' + loadError}
          />
        )}
      </Card>

      {trace ? (
        <>
          {/* 概览 */}
          <Card hoverable className="overview-card">
            <Descriptions column={4} bordered size="small">
              <Descriptions.Item label="requestId">{trace.requestId}</Descriptions.Item>
              <Descriptions.Item label="总耗时">{trace.totalDurationMs} ms</Descriptions.Item>
              <Descriptions.Item label="span 数">{spans.length}</Descriptions.Item>
              <Descriptions.Item label="预估费用">
                ${(trace.totalCostUsd || 0).toFixed(6)}
              </Descriptions.Item>
            </Descriptions>
            {trace.error && (
              <div className="trace-error">
                <Tag color="error">执行异常</Tag>
                <span className="error-text">{trace.error}</span>
              </div>
            )}
          </Card>

          {/* 调用链甘特图（按 startOffset 排列） */}
          <Card hoverable className="gantt-card" title={<span className="card-title">调用耗时分布（甘特图）</span>}>
            <div className="gantt">
              {spans.map((s, i) => (
                <div className="gantt-row" key={i} title={s.name}>
                  <div className="gantt-label" title={s.name}>
                    {s.name}
                  </div>
                  <div className="gantt-track">
                    <div className={`gantt-bar type-${s.type || 'OTHER'}`} style={barStyle(s)}>
                      <span className="gantt-bar-text">{s.durationMs}ms</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </Card>

          {/* span 明细 */}
          <Card hoverable className="detail-card" title={<span className="card-title">Span 明细</span>}>
            <Table
              rowKey={(r, i) => r.name ?? i}
              columns={columns}
              dataSource={spans}
              size="small"
              bordered
              pagination={false}
              scroll={{ x: 1000 }}
            />
          </Card>
        </>
      ) : (
        !loading &&
        !notFound &&
        !loadError && (
          <Empty description="输入 requestId 开始追踪 LLM 调用链" style={{ marginTop: 40 }} />
        )
      )}

      <Modal
        open={!!activeSpan}
        title="Span 详情"
        width={640}
        footer={null}
        onCancel={() => setActiveSpan(null)}
      >
        <pre className="span-json">{activeSpan ? JSON.stringify(activeSpan, null, 2) : ''}</pre>
      </Modal>
    </div>
  )
}
