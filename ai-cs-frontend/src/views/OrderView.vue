<template>
  <div class="order-view">
    <h2>我的订单</h2>

    <el-table :data="orders" style="width: 100%" @row-click="goDetail" row-class-name="clickable">
      <el-table-column prop="orderNo" label="订单号" min-width="180" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="商品" min-width="200">
        <template #default="{ row }">
          <span>{{ (row.items || []).map(i => `${i.productName}×${i.quantity}`).join('、') || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="应付金额" width="120">
        <template #default="{ row }">¥{{ Number(row.payAmount || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="下单时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="110">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click.stop="goDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!orders.length" description="暂无订单" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { orderApi } from '../api'
import { getUser } from '../utils/auth'

const router = useRouter()
const userId = ref(getUser()?.userId || localStorage.getItem('userId') || '')
const orders = ref([])

onMounted(fetchOrders)

async function fetchOrders() {
  try {
    const { data } = await orderApi.get('/list', { headers: { 'X-User-Id': userId.value } })
    if (data.code === 200) {
      orders.value = data.data || []
    } else {
      ElMessage.error(data.message)
    }
  } catch (e) {
    ElMessage.error('获取订单列表失败')
  }
}

function goDetail(row) {
  router.push(`/order/${row.orderNo}`)
}

function statusText(status) {
  const map = { PENDING_PAY: '待支付', PAID: '已支付', REFUNDING: '退款中', CANCELLED: '已取消', REFUNDED: '已退款' }
  return map[status] || status
}

function statusTag(status) {
  const map = { PENDING_PAY: 'warning', PAID: 'success', REFUNDING: 'warning', CANCELLED: 'info', REFUNDED: 'info' }
  return map[status] || 'info'
}

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : '-'
}
</script>

<style scoped>
.order-view { padding: 20px; }
.clickable { cursor: pointer; }
</style>