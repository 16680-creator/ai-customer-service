import { useState } from 'react'
import { Alert, Button, Card, Divider, Form, Input, Table, Tag, message } from 'antd'
import { knowledgeApi } from '../api'
import { promptDialog } from '../utils/dialog'

/**
 * 知识库运营看板（US6 知识库运营闭环的前端落地）
 * 1. 闭环：聚类主题 → 缺口标记(gapFlag) → 一键收录 FAQ → 触发知识向量更新
 * 2. report.status = INSUFFICIENT_DATA 表示提问样本不足（<20 条），提示而非报错
 * 3. hitRate = 主题代表问题在知识库的检索命中率，低命中=知识库缺口
 */
export default function KnowledgeOpsView() {
  const [period, setPeriod] = useState('2026-08-01~2026-08-12')
  const [questionsText, setQuestionsText] = useState('')
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(false)

  /** 生成 24 条示例提问（两个语义相近的退款问题），便于体验聚类 */
  function loadSample() {
    const samples = []
    for (let i = 1; i <= 24; i++) {
      if (i % 2 === 1) samples.push({ id: i, text: '怎么申请退款？' })
      else samples.push({ id: i, text: '退款多久能到账？' })
    }
    setQuestionsText(JSON.stringify(samples))
  }

  /** 调用后端 /knowledge/ops/cluster 运行聚类 + 缺口分析 */
  async function runCluster() {
    let questions = []
    try {
      questions = questionsText ? JSON.parse(questionsText) : []
    } catch {
      message.warning('提问 JSON 格式不正确')
      return
    }
    setLoading(true)
    try {
      const resp = await knowledgeApi.post('/ops/cluster', { period, questions })
      if (resp.data && resp.data.code === 200) {
        setReport(resp.data.data)
      } else {
        message.error(resp.data?.message || '聚类失败')
      }
    } catch (e) {
      message.error('运营服务不可用：' + (e.message || ''))
    } finally {
      setLoading(false)
    }
  }

  /** 一键收录 FAQ：把缺口主题转成 FAQ 知识文档，自动触发向量化 */
  async function adoptFaq(row) {
    let value
    try {
      value = await promptDialog({
        title: '收录 FAQ',
        placeholder: '请输入 FAQ 答案（留空使用主题问题）',
      })
    } catch {
      return // 用户取消
    }
    try {
      const resp = await knowledgeApi.post('/ops/faq', {
        question: row.topic,
        answer: value || '（待运营补充答案）',
        knowledgeBase: 'faq',
        clusterTopicId: row.topic,
      })
      if (resp.data && resp.data.code === 200) {
        message.success('FAQ 已收录并触发向量更新')
      } else {
        message.error(resp.data?.message || '收录失败')
      }
    } catch (e) {
      message.error('收录失败：' + (e.message || ''))
    }
  }

  const columns = [
    { title: '主题', dataIndex: 'topic', minWidth: 220 },
    { title: '次数', dataIndex: 'count', width: 80 },
    {
      title: '占比',
      dataIndex: 'ratio',
      width: 90,
      render: (ratio) => `${(ratio * 100).toFixed(1)}%`,
    },
    {
      title: '知识库命中率',
      width: 120,
      render: (_, row) =>
        row.hitRate != null ? (
          <Tag color={row.gapFlag ? 'error' : 'success'}>{(row.hitRate * 100).toFixed(0)}%</Tag>
        ) : (
          <span>-</span>
        ),
    },
    {
      title: '缺口',
      width: 80,
      render: (_, row) =>
        row.gapFlag ? <Tag color="error">缺口</Tag> : <Tag color="success">正常</Tag>,
    },
    {
      title: '操作',
      width: 180,
      render: (_, row) => (
        <Button size="small" type="primary" onClick={() => adoptFaq(row)}>
          收录 FAQ
        </Button>
      ),
    },
  ]

  return (
    <div className="knowledge-ops">
      <Card hoverable title={<span style={{ fontWeight: 600 }}>知识库运营看板（高频问题聚类 + 缺口识别）</span>}>
        <Form layout="inline">
          <Form.Item label="周期">
            <Input
              value={period}
              onChange={(e) => setPeriod(e.target.value)}
              placeholder="如 2026-08-01~2026-08-12"
              style={{ width: 220 }}
            />
          </Form.Item>
          <Form.Item label="提问(JSON)">
            <Input.TextArea
              value={questionsText}
              onChange={(e) => setQuestionsText(e.target.value)}
              rows={4}
              style={{ width: 460 }}
              placeholder='[{"id":1,"text":"怎么退款？"},{"id":2,"text":"退款多久到账？"}]'
            />
          </Form.Item>
          <Form.Item>
            <Button type="primary" onClick={runCluster} loading={loading}>
              运行聚类
            </Button>
            <Button onClick={loadSample} style={{ marginLeft: 10 }}>
              示例数据
            </Button>
          </Form.Item>
        </Form>

        {report && (
          <>
            {report.status === 'INSUFFICIENT_DATA' ? (
              <Alert
                type="warning"
                showIcon
                message="提问数据不足（至少 20 条），无法聚类"
                style={{ marginTop: 16 }}
              />
            ) : (
              <>
                <Divider orientation="left">
                  <Tag>
                    主题 {report.topics?.length || 0} 个 · 缺口 {report.gapTopics?.length || 0} 个
                  </Tag>
                </Divider>
                <Table
                  rowKey={(r, i) => r.topic ?? i}
                  columns={columns}
                  dataSource={report.topics || []}
                  bordered
                  pagination={false}
                  scroll={{ x: 900 }}
                />
              </>
            )}
          </>
        )}
      </Card>
    </div>
  )
}
