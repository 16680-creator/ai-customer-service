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

      <!-- 工单消息闭环演示 -->
      <el-tab-pane label="工单演示" name="workOrder">
        <el-card shadow="never" style="margin-bottom: 16px">
          <el-form :inline="true">
            <el-form-item label="标题">
              <el-input v-model="woForm.title" placeholder="工单标题" style="width: 200px" />
            </el-form-item>
            <el-form-item label="内容">
              <el-input v-model="woForm.content" placeholder="工单内容" style="width: 280px" />
            </el-form-item>
            <el-form-item label="模拟消费失败">
              <el-switch v-model="woForm.simulateFail" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="woSubmitting" @click="createWorkOrder">创建并发送</el-button>
              <el-button @click="loadWorkOrders">刷新工单</el-button>
            </el-form-item>
          </el-form>
          <div class="wo-tip">
            勾选「模拟消费失败」：消费者抛异常 → RocketMQ 自动重试 3 次（约 10s/30s/1m）→ 进入死信队列 →
            失败记录落库，到「失败记录」Tab 重推后恢复正常消费。
          </div>
        </el-card>
        <el-table :data="workOrders" border stripe v-loading="woLoading">
          <el-table-column prop="ticketNo" label="工单号" min-width="190" />
          <el-table-column prop="title" label="标题" min-width="130" />
          <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="woStatusType(row.status)" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" />
          <el-table-column prop="updateTime" label="更新时间" width="170" />
        </el-table>
        <el-empty v-if="!workOrders.length && !woLoading" description="暂无工单，创建一条试试" />
      </el-tab-pane>

      <!-- 消费失败记录 -->
      <el-tab-pane label="失败记录" name="failRecords">
        <div style="margin-bottom: 12px">
          <el-button :icon="Refresh" @click="loadFailRecords">刷新失败记录</el-button>
        </div>
        <el-table :data="failRecords" border stripe v-loading="frLoading">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="bizKey" label="工单号" min-width="180" />
          <el-table-column prop="msgId" label="消息ID" min-width="190" show-overflow-tooltip />
          <el-table-column prop="failReason" label="失败原因" min-width="230" show-overflow-tooltip />
          <el-table-column prop="reconsumeTimes" label="已重试" width="80" />
          <el-table-column label="记录状态" width="110">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PENDING' ? 'danger' : 'info'" size="small">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="落库时间" width="170" />
          <el-table-column prop="repushTime" label="重推时间" width="170" />
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="warning" :disabled="row.status !== 'PENDING'" @click="onRepush(row)">重新推送</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!failRecords.length && !frLoading" description="暂无消费失败记录（勾选「模拟消费失败」创建工单即可触发）" />
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
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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

// ===== 工单消息闭环演示 =====
const woForm = ref({ title: '', content: '', simulateFail: false })
const woSubmitting = ref(false)
const woLoading = ref(false)
const workOrders = ref([])
const frLoading = ref(false)
const failRecords = ref([])

function woStatusType(status) {
  return status === 'DONE' ? 'success' : status === 'FAILED' ? 'danger' : 'primary'
}

async function loadWorkOrders() {
  woLoading.value = true
  try {
    const { data } = await mqApi.get('/work-order/list')
    if (data.code === 200) workOrders.value = data.data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '获取工单列表失败')
  } finally {
    woLoading.value = false
  }
}

async function createWorkOrder() {
  if (!woForm.value.title || !woForm.value.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  woSubmitting.value = true
  try {
    const { data } = await mqApi.post('/work-order/create', woForm.value)
    if (data.code === 200) {
      ElMessage.success(`工单已创建并发送: ${data.data?.ticketNo || ''}`)
      woForm.value.title = ''
      woForm.value.content = ''
      woForm.value.simulateFail = false
      loadWorkOrders()
    } else {
      ElMessage.error(data.message)
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '创建工单失败')
  } finally {
    woSubmitting.value = false
  }
}

async function loadFailRecords() {
  frLoading.value = true
  try {
    const { data } = await mqApi.get('/fail-records/list')
    if (data.code === 200) failRecords.value = data.data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '获取失败记录失败')
  } finally {
    frLoading.value = false
  }
}

async function onRepush(row) {
  try {
    await ElMessageBox.confirm(`确认重新推送工单 ${row.bizKey} 的失败消息？`, '重新推送', { type: 'warning' })
  } catch {
    return
  }
  try {
    const { data } = await mqApi.post(`/fail-records/${row.id}/repush`)
    if (data.code === 200) {
      ElMessage.success('已重新投递，消费成功后工单状态将变为 DONE')
      loadFailRecords()
      loadWorkOrders()
    } else {
      ElMessage.error(data.message)
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '重推失败')
  }
}

// 首次切到演示 Tab 时加载数据
watch(activeTab, (tab) => {
  if (tab === 'workOrder') loadWorkOrders()
  if (tab === 'failRecords') loadFailRecords()
})
</script>

<style scoped>
.mq-view { padding: 20px; }
.mq-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.mq-header h2 { margin: 0; font-size: 20px; }
.stat-row { margin-bottom: 4px; }
.stat-value { font-size: 24px; font-weight: 700; color: #409eff; }
.stat-label { color: #909399; font-size: 13px; margin-top: 4px; }
.clickable { cursor: pointer; }
.wo-tip { color: #909399; font-size: 12px; line-height: 1.6; }
</style>