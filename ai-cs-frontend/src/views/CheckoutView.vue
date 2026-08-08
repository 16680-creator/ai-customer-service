<template>
  <div class="checkout-view">
    <h2>确认订单</h2>

    <el-card shadow="hover" class="section">
      <template #header><span>商品清单</span></template>
      <el-table :data="confirmInfo.items || []" style="width: 100%">
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

    <el-card shadow="hover" class="section">
      <template #header><span>优惠与支付</span></template>
      <el-form label-width="110px">
        <el-form-item label="商品总额">
          <span>¥{{ Number(confirmInfo.totalAmount || 0).toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="满减优惠" v-if="confirmInfo.fullReduction?.applied">
          <span class="discount">-¥{{ Number(confirmInfo.fullReduction.amount || 0).toFixed(2) }}</span>
          <span class="tip">（{{ confirmInfo.fullReduction.ruleName }}）</span>
        </el-form-item>
        <el-form-item label="优惠券">
          <el-select v-model="selectedCouponId" placeholder="选择优惠券" clearable style="width: 320px">
            <el-option
              v-for="c in usableCoupons"
              :key="c.id"
              :label="`${c.couponName}（满${Number(c.minOrderAmount).toFixed(0)}减${Number(c.amount).toFixed(2)}）`"
              :value="c.id"
            />
          </el-select>
          <el-tag v-if="!usableCoupons.length" type="info" size="small" style="margin-left: 10px">暂无可用优惠券</el-tag>
        </el-form-item>
        <el-form-item label="支付方式">
          <el-radio-group v-model="paymentMethod">
            <el-radio value="MOCK">模拟支付（演示）</el-radio>
            <el-radio value="ALIPAY">支付宝（扫码）</el-radio>
            <el-radio value="WECHAT">微信支付（扫码）</el-radio>
            <el-radio value="UNIONPAY">银联云闪付（扫码）</el-radio>
          </el-radio-group>
          <div class="pay-tip">支付宝/微信/银联渠道代码已实现，配置商户参数（Nacos pay.*）后即可使用；本地演示请选「模拟支付」。</div>
        </el-form-item>
        <el-form-item label="应付金额">
          <span class="pay-amount">¥{{ Number(confirmInfo.payAmount || 0).toFixed(2) }}</span>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="actions">
      <el-button @click="$router.back()">返回</el-button>
      <el-button type="primary" size="large" :loading="submitting" @click="submitOrder">提交订单</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { cartApi, orderApi } from '../api'
import { getUser } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const userId = ref(getUser()?.userId || localStorage.getItem('userId') || '')

const confirmInfo = ref({})
const selectedCouponId = ref(null)
const paymentMethod = ref('MOCK')
const submitting = ref(false)

const usableCoupons = computed(() => (confirmInfo.value.availableCoupons || []).filter(c => c.usable))

onMounted(() => {
  loadConfirm()
})

async function loadConfirm() {
  const ids = (route.query.ids || '').split(',').filter(Boolean).map(Number)
  if (!ids.length) {
    ElMessage.warning('请先选择要结算的商品')
    router.replace('/cart')
    return
  }
  try {
    const { data } = await cartApi.post('/checkout/confirm', { cartItemIds: ids }, {
      headers: { 'X-User-Id': userId.value }
    })
    if (data.code === 200) {
      confirmInfo.value = data.data
    } else {
      ElMessage.error(data.message)
    }
  } catch (e) {
    ElMessage.error('获取结算信息失败')
  }
}

async function submitOrder() {
  const ids = (route.query.ids || '').split(',').filter(Boolean).map(Number)
  submitting.value = true
  try {
    const { data } = await orderApi.post('/create', {
      cartItemIds: ids,
      couponId: selectedCouponId.value,
      paymentMethod: paymentMethod.value
    }, {
      headers: { 'X-User-Id': userId.value }
    })
    if (data.code === 200) {
      ElMessage.success('下单成功，请尽快完成支付')
      router.push(`/order/${data.data.orderNo}`)
    } else {
      ElMessage.error(data.message)
    }
  } catch (e) {
    ElMessage.error('下单失败：' + (e.response?.data?.message || e.message))
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.checkout-view { padding: 20px; }
.section { margin-bottom: 16px; }
.pay-tip { font-size: 12px; color: #909399; line-height: 1.6; }
.discount { color: #e6a23c; font-weight: 600; }
.tip { color: #909399; font-size: 12px; margin-left: 8px; }
.pay-amount { color: #e6a23c; font-size: 22px; font-weight: 700; }
.actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 16px; }
</style>