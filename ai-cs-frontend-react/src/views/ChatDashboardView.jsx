import { useEffect, useRef, useState } from 'react'
import { Alert, Button, Card, Empty, Form, Input, message } from 'antd'
import * as echarts from 'echarts'
import { chatApi } from '../api'

/**
 * AI 数据看板（US5 结构化问答增强的前端落地）
 * 1. 问数图表 = 后端生成「自然语言结论 + ECharts 配置(echartsOption)」，前端只负责渲染
 * 2. echarts.init(dom) 后 setOption(json) 即可画图；数据变化先 dispose 旧实例再重建
 * 3. chartType=NONE（单行/空数据）时不渲染图表，避免误导
 */
export default function ChatDashboardView() {
  const [question, setQuestion] = useState('')
  const [rowsText, setRowsText] = useState('')
  const [answer, setAnswer] = useState(null)
  const [loading, setLoading] = useState(false)

  const chartRef = useRef(null)
  const chartInstanceRef = useRef(null)

  /** 组件卸载时释放图表实例，防止内存泄漏 */
  useEffect(
    () => () => {
      if (chartInstanceRef.current) {
        chartInstanceRef.current.dispose()
        chartInstanceRef.current = null
      }
    },
    []
  )

  // answer 变化（DOM 就绪后）再渲染图表
  useEffect(() => {
    if (!answer) return
    // 复用实例前先销毁，避免多次 setOption 叠加状态
    if (chartInstanceRef.current) {
      chartInstanceRef.current.dispose()
      chartInstanceRef.current = null
    }
    if (!chartRef.current || answer.chartType === 'NONE') return
    chartInstanceRef.current = echarts.init(chartRef.current)
    chartInstanceRef.current.setOption(answer.echartsOption || {})
  }, [answer])

  /** 填充示例数据，便于快速体验 */
  function loadSample() {
    setQuestion('各分类销量分布')
    setRowsText(
      JSON.stringify([
        { category: '手机', sales: 1200 },
        { category: '平板', sales: 800 },
        { category: '笔记本', sales: 650 },
        { category: '耳机', sales: 420 },
      ])
    )
  }

  /** 调用后端 /chat/chart 生成结论 + 图表配置，再渲染 */
  async function generate() {
    let rows = []
    try {
      rows = rowsText ? JSON.parse(rowsText) : []
    } catch {
      message.warning('数据 JSON 格式不正确')
      return
    }
    setLoading(true)
    try {
      const resp = await chatApi.post('/chart', { question, rows })
      if (resp.data && resp.data.code === 200 && resp.data.data) {
        setAnswer(resp.data.data)
      } else {
        message.error(resp.data?.message || '生成失败')
      }
    } catch (e) {
      message.error('图表服务不可用：' + (e.message || ''))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="chat-dashboard">
      <Card hoverable title={<span style={{ fontWeight: 600 }}>AI 数据看板（问数图表）</span>}>
        <Form labelCol={{ span: 5 }} wrapperCol={{ span: 19 }}>
          <Form.Item label="问题">
            <Input
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="如：各分类销量分布"
            />
          </Form.Item>
          <Form.Item label="数据(JSON)">
            <Input.TextArea
              value={rowsText}
              onChange={(e) => setRowsText(e.target.value)}
              rows={5}
              placeholder='[{"category":"手机","sales":1200},{"category":"平板","sales":800}]'
            />
          </Form.Item>
          <Form.Item wrapperCol={{ offset: 5, span: 19 }}>
            <Button type="primary" onClick={generate} loading={loading}>
              生成图表
            </Button>
            <Button onClick={loadSample} style={{ marginLeft: 10 }}>
              示例数据
            </Button>
          </Form.Item>
        </Form>

        {answer && (
          <>
            <Alert
              type="success"
              showIcon
              message={answer.conclusion}
              style={{ marginBottom: 12 }}
            />
            {answer.chartType !== 'NONE' ? (
              <div ref={chartRef} className="chart-box" />
            ) : (
              <Empty description="当前数据不生成图表（单行/无分布维度）" />
            )}
          </>
        )}
      </Card>
    </div>
  )
}
