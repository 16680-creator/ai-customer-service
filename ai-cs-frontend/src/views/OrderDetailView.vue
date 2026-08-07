<template>
  <div class="order-detail-view">
    <el-page-header @back="$router.back()" :content="`订单 ${orderNo}`" />

    <template v-if="order">
      <el-card shadow="hover" class="section">
        <template #header>
          <div class="card-header">
            <span>订单信息</span>
            <el-tag :type="statusTag(order.status)" size="large">{{ statusText(order.status) }}</el-tag>
          </div>
        </template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">{{ statusText(order.status) }}</el-descriptions-item>
          <el-descriptions-item label="商品总额">¥{{ Number(order.totalAmount || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="优惠金额">-¥{{ Number(order.discountAmount || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="应付金额">¥{{ Number(order.payAmount || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ payMethodText(order.paymentMethod) }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ formatTime(order.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="支付截止">{{ formatTime(order.expireTime) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card shadow="hover" class="section">
        <template #header><span>商品清单</span></template>
        <el-table :data="order.items || []" style="width: 100%">
          <el-table-column prop="productName" label="商品名称" min-width="180" />
          <el-table-column prop="productPrice" label="单价" width="110">
            <template #default="{ row }">¥{{ Number(row.productPrice).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" />
          <el-table-column label="小计" width="110">
            <template #default="{ row }">¥{{ Number(row.subtotal).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </el-card>

      <div class="actions">
        <template v-if="order.status === 'PENDING_PAY'">
          <el-button type="danger" :loading="cancelling" @click="cancelOrder">取消订单</el-button>
          <el-button type="primary" :loading="paying" @click="retryPay">立即支付</el-button>
        </template>
      </div>
    </template>
    <el-empty v-else description="加载中或订单不存在" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { orderApi } from '../api'
import { getUser } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const orderNo = route.params.orderNo
const userId = ref(getUser()?.userId || localStorage.getItem('userId') || '')
const order = ref(null)
const cancelling = ref(false)
const paying = ref(false)

onMounted(fetchOrder)

async function fetchOrder() {
  try {
    const { data } = await orderApi.get(`/${orderNo}`, { headers: { 'X-User-Id': userId.value } })
    if (data.code === 200) {
      order.value = data.data
    } else {
      ElMessage.error(data.message)
    }
  } catch (e) {
    ElMessage.error('获取订单详情失败')
  }
}

async function cancelOrder() {
  try {
    await ElMessageBox.confirm('确定取消该订单吗？', '提示', { type: 'warning' })
    cancelling.value = true
    const { data } = await orderApi.put(`/${orderNo}/cancel`, null, { headers: { 'X-User-Id': userId.value } })
    if (data.code === 200) {
      ElMessage.success('订单已取消')
      fetchOrder()
    } else {
      ElMessage.error(data.message)
    }
  } catch (e) {
    // 用户取消或失败
  } finally {
    cancelling.value = false
  }
}

async function retryPay() {
  paying.value = true
  try {
    const { data } = await orderApi.put(`/${orderNo}/retry-pay`, { paymentMethod: order.value.paymentMethod || 'ALIPAY' }, {
      headers: { 'X-User-Id': userId.value }
    })
    if (data.code === 200 && data.data?.payUrl) {
      ElMessage.success('支付链接已生成，正在跳转...')
      window.open(data.data.payUrl, '_blank')
    } else {
      ElMessage.error(data.message)
    }
  } catch (e) {
    ElMessage.error('支付失败：' + (e.response?.data?.message || e.message))
  } finally {
    paying.value = false
  }
}

function statusText(status) {
  const map = { PENDING_PAY: '待支付', PAID: '已支付', CANCELLED: '已取消' }
  return map[status] || status
}

function statusTag(status) {
  const map = { PENDING_PAY: 'warning', PAID: 'success', CANCELLED: 'info' }
  return map[status] || 'info'
}

function payMethodText(m) {
  const map = { ALIPAY: '支付宝', WECHAT: '微信支付', BANK_CARD: '银行卡' }
  return map[m] || m || '-'
}

function formatTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : '-'
}
</script>

<style scoped>
.order-detail-view { padding: 20px; }
.section { margin-top: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px; }
</style>