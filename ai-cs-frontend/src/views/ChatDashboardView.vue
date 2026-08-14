<!--
  AI 数据看板（US5 结构化问答增强的前端落地）
  学习要点：
  1. 问数图表 = 后端生成「自然语言结论 + ECharts 配置(echartsOption)」，前端只负责渲染
  2. echarts.init(dom) 后 setOption(json) 即可画图；数据变化先 dispose 旧实例再重建
  3. chartType=NONE（单行/空数据）时不渲染图表，避免误导
-->
<template>
  <div class="chat-dashboard">
    <el-card shadow="hover">
      <template #header>
        <span style="font-weight: 600">AI 数据看板（问数图表）</span>
      </template>
      <el-form label-width="100px">
        <el-form-item label="问题">
          <el-input v-model="question" placeholder="如：各分类销量分布" />
        </el-form-item>
        <el-form-item label="数据(JSON)">
          <el-input v-model="rowsText" type="textarea" :rows="5"
                    placeholder='[{"category":"手机","sales":1200},{"category":"平板","sales":800}]' />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="generate" :loading="loading">生成图表</el-button>
          <el-button @click="loadSample">示例数据</el-button>
        </el-form-item>
      </el-form>

      <template v-if="answer">
        <el-alert :title="answer.conclusion" type="success" :closable="false" style="margin-bottom: 12px" />
        <div v-if="answer.chartType !== 'NONE'" ref="chartRef" style="height: 380px"></div>
        <el-empty v-else description="当前数据不生成图表（单行/无分布维度）" />
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { ref, nextTick, onBeforeUnmount } from 'vue'
import * as echarts from 'echarts'   // ECharts 图表库
import { ElMessage } from 'element-plus'
import { chatApi } from '../api'

const question = ref('')      // 用户问题（如"各分类销量分布"）
const rowsText = ref('')      // 查询结果 JSON 文本（NL2SQL 的 rows）
const answer = ref(null)      // 后端返回的 ChartAnswer（结论+图表类型+option）
const loading = ref(false)
const chartRef = ref(null)    // 图表容器 DOM 引用
let chartInstance = null      // ECharts 实例（需手动管理生命周期）

/** 填充示例数据，便于快速体验 */
function loadSample() {
  question.value = '各分类销量分布'
  rowsText.value = JSON.stringify([
    { category: '手机', sales: 1200 },
    { category: '平板', sales: 800 },
    { category: '笔记本', sales: 650 },
    { category: '耳机', sales: 420 },
  ])
}

/** 调用后端 /chat/chart 生成结论 + 图表配置，再渲染 */
async function generate() {
  let rows = []
  try {
    rows = rowsText.value ? JSON.parse(rowsText.value) : []
  } catch (e) {
    ElMessage.warning('数据 JSON 格式不正确')
    return
  }
  loading.value = true
  try {
    const resp = await chatApi.post('/chart', { question: question.value, rows })
    if (resp.data && resp.data.code === 200 && resp.data.data) {
      answer.value = resp.data.data
      await nextTick()
      renderChart(answer.value)
    } else {
      ElMessage.error(resp.data?.message || '生成失败')
    }
  } catch (e) {
    ElMessage.error('图表服务不可用：' + (e.message || ''))
  } finally {
    loading.value = false
  }
}

/** 用后端返回的 ECharts option 渲染图表；NONE 类型不渲染 */
function renderChart(data) {
  // 复用实例前先销毁，避免多次 setOption 叠加状态
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  if (!chartRef.value || data.chartType === 'NONE') return
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption(data.echartsOption || {})
}

// 组件卸载时释放图表实例，防止内存泄漏
onBeforeUnmount(() => {
  if (chartInstance) chartInstance.dispose()
})
</script>