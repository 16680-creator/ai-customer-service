import { useState } from 'react'
import { Alert, Button, Card, Col, Empty, Form, Input, InputNumber, Row, Select, Table, Tag, message } from 'antd'
import { PlayCircleOutlined } from '@ant-design/icons'
import { ragApi } from '../api'

function fmt(v) {
  return v == null ? '-' : Number(v).toFixed(2)
}

export default function RagEvalView() {
  const [req, setReq] = useState({
    knowledgeBase: 'knowledge',
    mode: 'VECTOR',
    goldenSetPath: 'classpath:eval/golden-set.json',
    hitRateThreshold: 0.6,
    llmScoreThreshold: 3.5,
  })
  const [running, setRunning] = useState(false)
  const [report, setReport] = useState(null)
  const [error, setError] = useState('')

  function setField(key, value) {
    setReq((prev) => ({ ...prev, [key]: value }))
  }

  async function runEval() {
    if (!req.knowledgeBase.trim()) {
      message.warning('请填写知识库')
      return
    }
    setRunning(true)
    setReport(null)
    setError('')
    try {
      const { data } = await ragApi.post('/eval/run', {
        knowledgeBase: req.knowledgeBase.trim(),
        mode: req.mode,
        goldenSetPath: req.goldenSetPath.trim(),
        hitRateThreshold: req.hitRateThreshold,
        llmScoreThreshold: req.llmScoreThreshold,
      })
      if (data.code === 200 || data.success) {
        setReport(data.data || data)
      } else {
        setError(data.message || '未知错误')
      }
    } catch (e) {
      setError(e.response?.data?.message || e.message || '未知错误')
    } finally {
      setRunning(false)
    }
  }

  const columns = [
    { title: '#', width: 50, render: (_, __, i) => i + 1 },
    { title: '问题', dataIndex: 'question', minWidth: 220 },
    {
      title: '命中',
      dataIndex: 'hit',
      width: 80,
      render: (hit) => <Tag color={hit ? 'success' : 'error'}>{hit ? '是' : '否'}</Tag>,
    },
    { title: '评分', dataIndex: 'score', width: 90, render: (v) => fmt(v) },
    {
      title: '通过',
      dataIndex: 'passed',
      width: 80,
      render: (passed) => <Tag color={passed ? 'success' : 'default'}>{passed ? '是' : '否'}</Tag>,
    },
    { title: '回答', dataIndex: 'answer', minWidth: 260, ellipsis: true },
    { title: '判定理由', dataIndex: 'reason', minWidth: 200, ellipsis: true },
  ]

  const metrics = report
    ? [
        { val: report.total, label: '用例总数' },
        { val: report.passed, label: '通过' },
        { val: report.failed, label: '失败' },
        { val: fmt(report.averageScore), label: '平均评分' },
        { val: fmt(report.hitRate), label: '命中率' },
        {
          val: report.passed ? 'PASS' : 'FAIL',
          label: '门禁',
          cls: report.passed ? 'ok' : 'bad',
        },
      ]
    : []

  return (
    <div className="eval-page">
      <Card
        hoverable
        title={
          <div className="header">
            <span className="title">RAG 评估（golden 回归测试 + LLM-as-Judge）</span>
            <Tag>结果低于阈值即视为质量回退</Tag>
          </div>
        }
      >
        <Form layout="inline">
          <Form.Item label="知识库">
            <Input
              value={req.knowledgeBase}
              onChange={(e) => setField('knowledgeBase', e.target.value)}
              placeholder="knowledge"
              style={{ width: 180 }}
            />
          </Form.Item>
          <Form.Item label="检索模式">
            <Select
              value={req.mode}
              onChange={(v) => setField('mode', v)}
              style={{ width: 150 }}
              options={[
                { label: 'VECTOR', value: 'VECTOR' },
                { label: 'HYBRID', value: 'HYBRID' },
                { label: 'RERANK', value: 'RERANK' },
              ]}
            />
          </Form.Item>
          <Form.Item label="golden 集路径">
            <Input
              value={req.goldenSetPath}
              onChange={(e) => setField('goldenSetPath', e.target.value)}
              placeholder="classpath:eval/golden-set.json"
              style={{ width: 240 }}
            />
          </Form.Item>
          <Form.Item label="命中率阈值">
            <InputNumber
              min={0}
              max={1}
              step={0.05}
              value={req.hitRateThreshold}
              onChange={(v) => setField('hitRateThreshold', v ?? 0)}
            />
          </Form.Item>
          <Form.Item label="评分阈值">
            <InputNumber
              min={0}
              max={5}
              step={0.1}
              value={req.llmScoreThreshold}
              onChange={(v) => setField('llmScoreThreshold', v ?? 0)}
            />
          </Form.Item>
          <Form.Item>
            <Button type="primary" icon={<PlayCircleOutlined />} loading={running} onClick={runEval}>
              运行评估
            </Button>
          </Form.Item>
        </Form>

        {error && (
          <Alert type="error" showIcon message={'评估失败: ' + error} style={{ marginTop: 12 }} />
        )}
      </Card>

      {report ? (
        <>
          {/* 汇总 */}
          <Card hoverable style={{ marginTop: 16 }} title="评估汇总">
            <Row gutter={16}>
              {metrics.map((m) => (
                <Col span={4} key={m.label}>
                  <div className="metric">
                    <div className={`m-val${m.cls ? ' ' + m.cls : ''}`}>{m.val}</div>
                    <div className="m-label">{m.label}</div>
                  </div>
                </Col>
              ))}
            </Row>
          </Card>

          {/* 逐条明细 */}
          <Card hoverable style={{ marginTop: 16 }} title="逐条明细">
            <Table
              rowKey={(r, i) => r.question ?? i}
              columns={columns}
              dataSource={report.items || []}
              size="small"
              bordered
              pagination={false}
              scroll={{ x: 1000 }}
            />
          </Card>
        </>
      ) : (
        !running &&
        !error && (
          <Empty description="配置参数后运行评估（基于固定 golden 集量化检索与回答质量）" style={{ marginTop: 40 }} />
        )
      )}
    </div>
  )
}
