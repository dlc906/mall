<template>
  <div class="order-list-page">
    <h2>我的订单</h2>

    <div class="status-tabs">
      <el-button :type="statusFilter === null ? 'primary' : ''" @click="filterOrders(null)">全部</el-button>
      <el-button :type="statusFilter === 0 ? 'warning' : ''" @click="filterOrders(0)">待支付</el-button>
      <el-button :type="statusFilter === 1 ? 'primary' : ''" @click="filterOrders(1)">已支付</el-button>
      <el-button :type="statusFilter === 2 ? 'primary' : ''" @click="filterOrders(2)">已发货</el-button>
      <el-button :type="statusFilter === 3 ? 'success' : ''" @click="filterOrders(3)">已完成</el-button>
    </div>

    <div v-loading="loading">
      <div v-for="order in orders" :key="order.id" class="order-card">
        <div class="order-header">
          <span>订单号: {{ order.orderNo }}</span>
          <span class="order-time">{{ order.createTime }}</span>
          <el-tag :type="statusTagType(order.status)">{{ statusMap[order.status] }}</el-tag>
        </div>
        <div class="order-body" @click="$router.push(`/order/${order.id}`)">
          <div class="order-summary">
            <span>共 1 件商品</span>
            <span class="order-amount">¥{{ order.totalAmount }}</span>
          </div>
          <el-icon><ArrowRight /></el-icon>
        </div>
        <div class="order-actions" v-if="order.status === 0">
          <el-button size="small" type="primary" @click="handlePay(order)">去支付</el-button>
          <el-button size="small" @click="handleCancel(order)">取消订单</el-button>
        </div>
        <div class="order-actions" v-else-if="order.status === 2">
          <el-button size="small" type="success" @click="handleConfirm(order)">确认收货</el-button>
        </div>
      </div>
      <el-empty v-if="!loading && orders.length === 0" description="暂无订单" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const orders = ref([])
const statusFilter = ref(null)
const loading = ref(false)
const statusMap = { 0: '待支付', 1: '已支付', 2: '已发货', 3: '已完成', 4: '已取消', 5: '退款中', 6: '已退款' }

function statusTagType(status) {
  const map = { 0: 'warning', 1: 'primary', 2: 'primary', 3: 'success', 4: 'info', 5: 'warning', 6: 'info' }
  return map[status] || 'info'
}

async function fetchOrders() {
  loading.value = true
  try {
    const res = await request.get('/order/list', {
      params: { pageNum: 1, pageSize: 50, status: statusFilter.value }
    })
    orders.value = res.data.records || []
  } finally {
    loading.value = false
  }
}

function filterOrders(status) {
  statusFilter.value = status
  fetchOrders()
}

async function handleCancel(order) {
  try {
    await ElMessageBox.confirm('确认取消该订单？', '提示', { confirmButtonText: '确定', cancelButtonText: '返回' })
    await request.post(`/order/${order.id}/cancel`, null, { params: { reason: '用户取消' } })
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch { /* cancelled */ }
}

async function handlePay(order) {
  try {
    await request.post('/payment/pay', { orderNo: order.orderNo, amount: order.totalAmount, payMethod: 1 })
    ElMessage.success('支付成功')
    fetchOrders()
  } catch { /* handled */ }
}

async function handleConfirm(order) {
  try {
    await request.post(`/order/${order.id}/confirm`)
    ElMessage.success('确认收货成功')
    fetchOrders()
  } catch { /* handled */ }
}

onMounted(fetchOrders)
</script>

<style scoped>
.order-list-page { max-width: 900px; margin: 20px auto; padding: 20px; }
.status-tabs { display: flex; gap: 8px; margin: 16px 0; }
.order-card { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 12px; }
.order-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; font-size: 14px; color: #666; }
.order-body { display: flex; justify-content: space-between; align-items: center; cursor: pointer; padding: 8px 0; }
.order-amount { font-size: 18px; font-weight: bold; color: #f56c6c; margin-left: 12px; }
.order-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 8px; padding-top: 8px; border-top: 1px solid #f5f5f5; }
</style>
