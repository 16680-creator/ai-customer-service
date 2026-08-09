<template>
  <div class="mq-view">
    <div class="mq-header">
      <h2>RocketMQ 调度中心</h2>
      <el-button type="primary" :icon="Refresh" :loading="loading" @click="loadAll">刷新</el-button>
    </div>

    <el-alert
      v-if="error"
      :title="error"
      type="error"
      show-icon
      :closable="false"
      style="margin-bottom: 16px"
    />

    <!-- 概览卡片 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="s in stats" :key="s.label">
        <el-card shadow="hover" :body-style="{ padding: '20px' }">
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" style="margin-top: 16px">
      <!-- Broker 集群 -->
      <el-tab-pane label="集群 / Broker" name="cluster">
        <el-table :data="brokers" border stripe size="default" v-loading="loading">
          <el-table-column prop="brokerName" label="Broker" min-width="140" />
          <el-table-column prop="cluster" label="集群" width="120" />
          <el-table-column prop="masterAddr" label="主节点地址" min-width="150" />
          <el-table-column label="从节点" min-width="160">
            <template #default="{ row }">{{ (row.slaveAddrs || []).join(', ') || '-' }}</template>
          </el-table-column>
          <el-table-column prop="version" label="版本" width="110" />
          <el-table-column prop="commitLogDiskRatio" label="CommitLog 磁盘占比" width="140" />
          <el-table-column prop="putMessageEntireTimeMax" label="写消息最大耗时(ms)" width="150" />
          <el-table-column prop="qps" label="TPS" width="90" />
        </el-table>
        <el-empty v-if="!brokers.length && !loading" description="暂无 Broker" />
      </el-tab-pane>

      <!-- Topic -->
      <el-tab-pane label="Topic" name="topics">
        <el-table :data="topics" border stripe v-loading="loading" @row-click="openTopic" row-class-name="clickable">
          <el-table-column prop="topic" label="Topic" min-width="200" />
          <el-table-column label="Broker" min-width="160">
            <template #default="{ row }">{{ (row.brokers || []).join(', ') || '-' }}</template>
          </el-table-column>
          <el-table-column prop="readQueues" label="读队列" width="80" />
          <el-table-column prop="writeQueues" label="写队列" width="80" />
          <el-table-column label="消息量" width="110">
            <template #default="{ row }">{{ row.messageCount ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click.stop="openTopic(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!topics.length && !loading" description="暂无 Topic" />
      </el-tab-pane>

      <!-- 消费组 -->
      <el-tab-pane label="消费组 / 堆积" name="groups">
        <el-table :data="groups" border stripe v-loading="loading" @row-click="openGroup" row-class-name="clickable">
          <el-table-column prop="group" label="消费组" min-width="180" />
          <el-table-column label="消费 TPS" width="100">
            <template #default="{ row }">{{ row.consumeTps || '-' }}</template>
          </el-table-column>
          <el-table-column label="堆积量" width="110">
            <template #default="{ row }">
              <el-tag :type="row.diff > 0 ? 'danger' : 'success'" size="small">{{ row.diff ?? '-' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="消费 Topic" min-width="220">
            <template #default="{ row }">{{ (row.topics || []).join(', ') || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ row }">
              <el-button size="small" link type="primary" @click.stop="openGroup(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!groups.length && !loading" description="暂无消费组" />
      </el-tab-pane>
    </el-tabs>

    <!-- Topic 详情 -->
    <el-dialog v-model="topicDialog" :title="`Topic 详情：${topicDetail.topic || ''}`" width="640px">
      <el-table :data="topicDetail.queues || []" border size="small">
        <el-table-column prop="broker" label="Broker" min-width="140" />
        <el-table-column prop="readQueueNums" label="读队列" width="90" />
        <el-table-column prop="writeQueueNums" label="写队列" width="90" />
        <el-table-column prop="perm" label="权限(perm)" width="100" />
      </el-table>
    </el-dialog>

    <!-- 消费组详情 -->
    <el-dialog v-model="groupDialog" :title="`消费组详情：${groupDetail.group || ''}`" width="760px">
      <el-descriptions :column="2" border style="margin-bottom: 12px">
        <el-descriptions-item label="消费 TPS">{{ groupDetail.consumeTps || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="groupDetail.queues || []" border size="small">
        <el-table-column prop="topic" label="Topic" min-width="160" />
        <el-table-column prop="broker" label="Broker" min-width="120" />
        <el-table-column prop="queueId" label="队列" width="60" />
        <el-table-column prop="brokerOffset" label="Broker Offset" width="120" />
        <el-table-column prop="consumerOffset" label="消费 Offset" width="120" />
        <el-table-column label="堆积" width="90">
          <template #default="{ row }">
            <el-tag :type="row.diff > 0 ? 'danger' : 'success'" size="small">{{ row.diff }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { mqApi } from '../api'

const loading = ref(false)
const error = ref('')
const activeTab = ref('cluster')

const overview = ref({})
const brokers = ref([])
const topics = ref([])
const groups = ref([])

const topicDialog = ref(false)
const topicDetail = ref({})
const groupDialog = ref(false)
const groupDetail = ref({})

const stats = computed(() => [
  { label: 'Broker 数', value: overview.value.brokerCount ?? '-' },
  { label: 'Topic 数', value: overview.value.topicCount ?? '-' },
  { label: '消费组数', value: overview.value.groupCount ?? '-' },
  { label: '总堆积量', value: overview.value.totalDiff ?? '-' },
])

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  error.value = ''
  try {
    const [o, b, t, g] = await Promise.all([
      mqApi.get('/overview'),
      mqApi.get('/cluster'),
      mqApi.get('/topics'),
      mqApi.get('/groups'),
    ])
    if (o.data.code === 200) overview.value = o.data.data || {}
    if (b.data.code === 200) brokers.value = b.data.data || []
    if (t.data.code === 200) topics.value = t.data.data || []
    if (g.data.code === 200) groups.value = g.data.data || []
  } catch (e) {
    error.value = e.response?.data?.message || e.message || 'RocketMQ 调度服务不可用'
  } finally {
    loading.value = false
  }
}

async function openTopic(row) {
  try {
    const { data } = await mqApi.get(`/topic/${encodeURIComponent(row.topic)}`)
    if (data.code === 200) {
      topicDetail.value = data.data || {}
      topicDialog.value = true
    } else {
      ElMessage.error(data.message)
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '获取 Topic 详情失败')
  }
}

async function openGroup(row) {
  try {
    const { data } = await mqApi.get(`/group/${encodeURIComponent(row.group)}`)
    if (data.code === 200) {
      groupDetail.value = data.data || {}
      groupDialog.value = true
    } else {
      ElMessage.error(data.message)
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '获取消费组详情失败')
  }
}
</script>

<style scoped>
.mq-view { padding: 20px; }
.mq-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.mq-header h2 { margin: 0; font-size: 20px; }
.stat-row { margin-bottom: 4px; }
.stat-value { font-size: 24px; font-weight: 700; color: #409eff; }
.stat-label { color: #909399; font-size: 13px; margin-top: 4px; }
.clickable { cursor: pointer; }
</style>