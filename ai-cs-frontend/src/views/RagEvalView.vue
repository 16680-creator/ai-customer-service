<template>
  <div class="eval-page">
    <el-card shadow="hover">
      <template #header>
        <div class="header">
          <span class="title">RAG 评估（golden 回归测试 + LLM-as-Judge）</span>
          <el-tag size="small" type="info" effect="plain">结果低于阈值即视为质量回退</el-tag>
        </div>
      </template>

      <el-form :inline="true" label-width="110px">
        <el-form-item label="知识库">
          <el-input v-model="req.knowledgeBase" placeholder="knowledge" style="width: 180px" />
        </el-form-item>
        <el-form-item label="检索模式">
          <el-select v-model="req.mode" style="width: 150px">
            <el-option label="VECTOR" value="VECTOR" />
            <el-option label="HYBRID" value="HYBRID" />
            <el-option label="RERANK" value="RERANK" />
          </el-select>
        </el-form-item>
        <el-form-item label="golden 集路径">
          <el-input v-model="req.goldenSetPath" placeholder="classpath:eval/golden-set.json" style="width: 240px" />
        </el-form-item>
        <el-form-item label="命中率阈值">
          <el-input-number v-model="req.hitRateThreshold" :min="0" :max="1" :step="0.05" />
        </el-form-item>
        <el-form-item label="评分阈值">
          <el-input-number v-model="req.llmScoreThreshold" :min="0" :max="5" :step="0.1" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="VideoPlay" :loading="running" @click="runEval">运行评估</el-button>
        </el-form-item>
      </el-form>

      <el-alert v-if="error" type="error" :closable="false" :title="'评估失败: ' + error" show-icon />
    </el-card>

    <template v-if="report">
      <!-- 汇总 -->
      <el-card shadow="hover" style="margin-top: 16px">
        <template #header><span>评估汇总</span></template>
        <el-row :gutter="16">
          <el-col :span="4"><div class="metric"><div class="m-val">{{ report.total }}</div><div class="m-label">用例总数</div></div></el-col>
          <el-col :span="4"><div class="metric"><div class="m-val">{{ report.passed }}</div><div class="m-label">通过</div></div></el-col>
          <el-col :span="4"><div class="metric"><div class="m-val">{{ report.failed }}</div><div class="m-label">失败</div></div></el-col>
          <el-col :span="4"><div class="metric"><div class="m-val">{{ fmt(report.averageScore) }}</div><div class="m-label">平均评分</div></div></el-col>
          <el-col :span="4"><div class="metric"><div class="m-val">{{ fmt(report.hitRate) }}</div><div class="m-label">命中率</div></div></el-col>
          <el-col :span="4">
            <div class="metric">
              <div class="m-val" :class="report.passed ? 'ok' : 'bad'">{{ report.passed ? 'PASS' : 'FAIL' }}</div>
              <div class="m-label">门禁</div>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 逐条明细 -->
      <el-card shadow="hover" style="margin-top: 16px">
        <template #header><span>逐条明细</span></template>
        <el-table :data="report.items || []" stripe size="small" border>
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="question" label="问题" min-width="220" />
          <el-table-column prop="hit" label="命中" width="80">
            <template #default="{ row }"><el-tag :type="row.hit ? 'success' : 'danger'" size="small">{{ row.hit ? '是' : '否' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="score" label="评分" width="90">
            <template #default="{ row }">{{ fmt(row.score) }}</template>
          </el-table-column>
          <el-table-column prop="passed" label="通过" width="80">
            <template #default="{ row }"><el-tag :type="row.passed ? 'success' : 'info'" size="small">{{ row.passed ? '是' : '否' }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="answer" label="回答" min-width="260" show-overflow-tooltip />
          <el-table-column prop="reason" label="判定理由" min-width="200" show-overflow-tooltip />
        </el-table>
      </el-card>
    </template>

    <el-empty v-else-if="!running && !error" description="配置参数后运行评估（基于固定 golden 集量化检索与回答质量）" />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ragApi } from '../api'
import { ElMessage } from 'element-plus'
import { VideoPlay } from '@element-plus/icons-vue'

const req = ref({
  knowledgeBase: 'knowledge',
  mode: 'VECTOR',
  goldenSetPath: 'classpath:eval/golden-set.json',
  hitRateThreshold: 0.6,
  llmScoreThreshold: 3.5,
})
const running = ref(false)
const report = ref(null)
const error = ref('')

function fmt(v) {
  return v == null ? '-' : Number(v).toFixed(2)
}

async function runEval() {
  if (!req.value.knowledgeBase.trim()) return ElMessage.warning('请填写知识库')
  running.value = true
  report.value = null
  error.value = ''
  try {
    const { data } = await ragApi.post('/eval/run', {
      knowledgeBase: req.value.knowledgeBase.trim(),
      mode: req.value.mode,
      goldenSetPath: req.value.goldenSetPath.trim(),
      hitRateThreshold: req.value.hitRateThreshold,
      llmScoreThreshold: req.value.llmScoreThreshold,
    })
    if (data.code === 200 || data.success) {
      report.value = data.data || data
    } else {
      error.value = data.message || '未知错误'
    }
  } catch (e) {
    error.value = e.response?.data?.message || e.message || '未知错误'
  } finally {
    running.value = false
  }
}
</script>

<style scoped>
.eval-page { padding: 16px; }
.header { display: flex; align-items: center; gap: 12px; }
.title { font-weight: 600; color: #303133; }
.metric { text-align: center; padding: 12px; background: #f5f7fa; border-radius: 8px; }
.m-val { font-size: 22px; font-weight: 700; color: #303133; }
.m-val.ok { color: #67c23a; }
.m-val.bad { color: #f56c6c; }
.m-label { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
