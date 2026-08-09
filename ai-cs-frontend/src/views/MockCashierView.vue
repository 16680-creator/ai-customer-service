<template>
  <div class="mock-cashier">
    <el-card class="cashier-card">
      <template #header>
        <div class="cashier-header">
          <span style="font-weight: 600">{{ isQrMode ? '收银台 - 扫码支付' : '模拟收银台（学习完整支付流程）' }}</span>
          <el-tag :type="isQrMode ? 'success' : 'warning'">{{ isQrMode ? '渠道扫码' : '模拟渠道 MOCK' }}</el-tag>
        </div>
      </template>

      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单号">{{ orderNo }}</el-descriptions-item>
        <el-descriptions-item label="支付金额">¥{{ amount }}</el-descriptions-item>
        <el-descriptions-item label="流程说明">
          <template v-if="isQrMode">
            请使用支付宝 / 微信 / 云闪付 App 扫码支付。支付完成后，系统通过「查单兜底」自动把订单更新为已支付
            （后端主动向渠道查询，无需公网回调地址也能闭环）。
          </template>
          <template v-else>
            本页等价于微信/支付宝的收银台。点击「模拟支付成功」= 用户在支付 App 完成支付，
            随后系统走一遍与真实渠道完全相同的链路：渠道通知 → 验签 → 幂等更新订单状态。
          </template>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 真实渠道：二维码扫码支付 -->
      <div v-if="isQrMode" class="qr-box">
        <img v-if="qrDataUrl" :src="qrDataUrl" class="qr-img" alt="支付二维码" />
        <el-skeleton v-else animated style="width: 240px; height: 240px" />
        <div class="qr-tip">请使用 支付宝 / 微信 / 云闪付 App 扫码支付</div>
        <div v-if="expireTime && !expired && !status" class="countdown">支付剩余时间：{{ countdownText }}</div>
        <div class="qr-actions">
          <el-button :loading="polling" @click="refreshStatus">刷新状态</el-button>
          <el-button @click="$router.push(`/order/${orderNo}`)">返回订单</el-button>
        </div>
        <el-tag v-if="status" :type="status === 'PAID' ? 'success' : 'warning'" size="large" class="status-tag">
          {{ statusText(status) }}
        </el-tag>
      </div>

      <!-- 模拟渠道：模拟支付按钮 -->
      <div v-else class="cashier-actions">
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import { payApi } from '../api'
import { getUser } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const orderNo = route.query.orderNo || ''
const amount = route.query.amount || '-'
const payType = route.query.payType || 'REDIRECT'
const codeUrl = route.query.codeUrl || ''
const payUrl = route.query.payUrl || ''
const expireTime = route.query.expireTime || ''
const remainMs = ref(0)
const expired = ref(false)
const userId = ref(getUser()?.userId || '')

const qrDataUrl = ref('')
const status = ref('')
const polling = ref(false)
const paying = ref(false)
const result = ref(null)

const isQrMode = computed(() => payType === 'QRCODE' && !!codeUrl)

onMounted(async () => {
  if (!orderNo) {
    ElMessage.warning('缺少订单号')
    router.replace('/order')
    return
  }
  if (isQrMode.value) {
    try {
      qrDataUrl.value = await QRCode.toDataURL(codeUrl, { width: 260, margin: 1 })
    } catch (e) {
      ElMessage.error('二维码生成失败')
    }
    startPolling()
  }
  startCountdown()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

let timer = null

/** 支付截止倒计时（订单超时后自动提示） */
function startCountdown() {
  if (!expireTime) return
  const end = new Date(String(expireTime).replace('T', ' ').replace(/-/g, '/')).getTime()
  if (Number.isNaN(end)) return
  timer = setInterval(() => {
    remainMs.value = end - Date.now()
    if (remainMs.value <= 0) {
      remainMs.value = 0
      expired.value = true
      if (timer) clearInterval(timer)
      polling.value = false
      result.value = { type: 'warning', text: '订单已超时，未在有效期内支付。返回订单查看，系统会自动关单并释放库存。' }
    }
  }, 1000)
}

const countdownText = computed(() => {
  const s = Math.max(0, Math.floor(remainMs.value / 1000))
  const mm = String(Math.floor(s / 60)).padStart(2, '0')
  const ss = String(s % 60).padStart(2, '0')
  return ${mm}:
})

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/** 轮询支付状态：后端 /pay/status 内置"查单兜底"，渠道已支付会自动落库 */
async function startPolling(times = 120, interval = 2500) {
  polling.value = true
  for (let i = 0; i < times; i++) {
    await sleep(interval)
    try {
      const { data } = await payApi.get(`/status/${orderNo}`, { headers: { 'X-User-Id': userId.value } })
      if (data.code === 200) {
        status.value = data.data.status
        if (data.data.status === 'PAID') {
          result.value = { type: 'success', text: `支付成功！订单 ${orderNo} 已更新为「已支付」（查单兜底确认）。` }
          polling.value = false
          return
        }
        if (data.data.status === 'REFUNDED') {
          result.value = { type: 'info', text: '该订单已退款。' }
          polling.value = false
          return
        }
        if (data.data.status === 'CANCELLED') {
          result.value = { type: 'warning', text: '订单已取消（超时未支付）。' }
          polling.value = false
          return
        }
      }
    } catch (e) {
      // 忽略单次轮询失败，继续
    }
  }
  polling.value = false
  result.value = { type: 'info', text: '等待支付超时，可点击「刷新状态」或返回订单重新发起支付。' }
}

async function refreshStatus() {
  try {
    const { data } = await payApi.get(`/status/${orderNo}`, { headers: { 'X-User-Id': userId.value } })
    if (data.code === 200) {
      status.value = data.data.status
      if (data.data.status === 'PAID') {
        result.value = { type: 'success', text: '支付成功！订单已更新为「已支付」。' }
      } else if (data.data.status === 'PENDING_PAY') {
        result.value = { type: 'info', text: '仍为待支付，请扫码完成支付。' }
      }
    }
  } catch (e) {
    ElMessage.error('刷新状态失败')
  }
}

async function doPay(payResult) {
  paying.value = true
  result.value = null
  try {
    const { data } = await payApi.post('/mock/pay', { orderNo, result: payResult }, {
      headers: { 'X-User-Id': userId.value },
    })
    if (data.code === 200) {
      if (data.data.status === 'PAID') {
        result.value = {
          type: 'success',
          text: `支付成功！订单 ${orderNo} 状态已更新为「已支付」（渠道回调 → 验签 → 幂等更新 → 确认）。`,
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

function statusText(s) {
  const map = { PENDING_PAY: '待支付', PAID: '已支付', CANCELLED: '已取消', REFUNDED: '已退款' }
  return map[s] || s
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
.qr-box {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.qr-img {
  width: 260px;
  height: 260px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
}
.qr-tip {
  color: #909399;
  font-size: 13px;
}
.qr-actions {
  display: flex;
  gap: 10px;
}
.status-tag {
  margin-top: 4px;
}
.countdown {
  color: #e6a23c;
  font-size: 14px;
  font-weight: 600;
}
</style>