<template>
  <div class="mock-cashier">
    <el-card class="cashier-card">
      <template #header>
        <div class="cashier-header">
          <span style="font-weight: 600">模拟收银台（学习完整支付流程）</span>
          <el-tag type="warning">模拟渠道 MOCK</el-tag>
        </div>
      </template>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单号">{{ orderNo }}</el-descriptions-item>
        <el-descriptions-item label="支付金额">¥{{ amount }}</el-descriptions-item>
        <el-descriptions-item label="流程说明">
          本页等价于微信/支付宝的收银台。点击「模拟支付成功」= 用户在支付 App 完成支付，
          随后系统会走一遍与真实渠道完全相同的链路：渠道通知 → 验签 → 幂等更新订单状态 → 前端轮询确认。
        </el-descriptions-item>
      </el-descriptions>

      <div class="cashier-actions">
        <el-button type="success" size="large" :loading="paying" @click="doPay('SUCCESS')">模拟支付成功</el-button>
        <el-button size="large" :loading="paying" @click="doPay('FAIL')">模拟支付失败 / 取消</el-button>
        <el-button size="large" @click="$router.push(`/order/${orderNo}`)">返回订单</el-button>
      </div>

      <el-alert
        v-if="result"
        :type="result.type"
        :title="result.text"
        show-icon
        style="margin-top: 16px"
        :closable="false"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { payApi } from '../api'
import { getUser } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const orderNo = route.query.orderNo || ''
const amount = route.query.amount || '-'
const userId = ref(getUser()?.userId || '')
const paying = ref(false)
const result = ref(null)

onMounted(() => {
  if (!orderNo) {
    ElMessage.warning('缺少订单号')
    router.replace('/order')
  }
})

async function doPay(payResult) {
  paying.value = true
  result.value = null
  try {
    // 1) 模拟用户在收银台操作：渠道收到支付结果
    const { data } = await payApi.post('/mock/pay', { orderNo, result: payResult }, {
      headers: { 'X-User-Id': userId.value },
    })
    if (data.code === 200) {
      if (data.data.status === 'PAID') {
        // 2) 前端轮询支付状态（真实 Native 扫码场景，前端需轮询确认）
        const status = await pollStatus(5, 1200)
        result.value = {
          type: 'success',
          text: `支付成功！订单 ${orderNo} 状态已更新为「已支付」（渠道回调 → 验签 → 幂等更新 → 前端轮询确认${status ? ' ✓' : '（回调同步完成）'}）。`,
        }
      } else if (data.data.status === 'REFUNDED') {
        result.value = { type: 'info', text: '该订单已退款。' }
      } else {
        result.value = { type: 'warning', text: '模拟支付失败/取消，订单仍为「待支付」，可返回订单重新发起支付。' }
      }
    } else {
      ElMessage.error(data.message || '模拟支付失败')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '模拟支付失败')
  } finally {
    paying.value = false
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function pollStatus(times, interval) {
  for (let i = 0; i < times; i++) {
    await sleep(interval)
    try {
      const { data } = await payApi.get(`/status/${orderNo}`, { headers: { 'X-User-Id': userId.value } })
      if (data.code === 200 && data.data.status === 'PAID') {
        return data.data.status
      }
    } catch (e) {
      // 忽略单次轮询失败，继续
    }
  }
  return null
}
</script>

<style scoped>
.mock-cashier {
  max-width: 640px;
  margin: 40px auto;
  padding: 0 16px;
}
.cashier-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.cashier-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 20px;
  flex-wrap: wrap;
}
</style>