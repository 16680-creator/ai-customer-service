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
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { chatApi } from '../api'

const question = ref('')
const rowsText = ref('')
const answer = ref(null)
const loading = ref(false)
const chartRef = ref(null)
let chartInstance = null

function loadSample() {
  question.value = '各分类销量分布'
  rowsText.value = JSON.stringify([
    { category: '手机', sales: 1200 },
    { category: '平板', sales: 800 },
    { category: '笔记本', sales: 650 },
    { category: '耳机', sales: 420 },
  ])
}

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

function renderChart(data) {
  if (chartInstance) {
    chartInstance.dispose()
    chartInstance = null
  }
  if (!chartRef.value || data.chartType === 'NONE') return
  chartInstance = echarts.init(chartRef.value)
  chartInstance.setOption(data.echartsOption || {})
}

onBeforeUnmount(() => {
  if (chartInstance) chartInstance.dispose()
})
</script>