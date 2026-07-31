<template>
  <div class="order-detail-page" v-loading="loading">
    <h2>订单详情</h2>
    <div class="detail-card" v-if="orderResp">
      <div class="order-status">
        <h3>{{ statusMap[orderResp.order.status] }}</h3>
        <p>订单号: {{ orderResp.order.orderNo }}</p>
        <p>下单时间: {{ orderResp.order.createTime }}</p>
      </div>
      <div class="receiver-info">
        <h4>收货信息</h4>
        <p>{{ orderResp.order.receiverName }} {{ orderResp.order.receiverPhone }}</p>
        <p>{{ orderResp.order.receiverAddress }}</p>
      </div>
      <div class="items">
        <h4>商品信息</h4>
        <div v-for="item in orderResp.items" :key="item.id" class="order-item">
          <el-image :src="item.productImage" fit="cover" style="width:80px;height:80px" />
          <span>{{ item.productName }}</span>
          <span>¥{{ item.productPrice }} × {{ item.quantity }}</span>
          <span class="item-total">¥{{ (item.productPrice * item.quantity).toFixed(2) }}</span>
        </div>
      </div>
      <div class="order-total">
        应付总额: <span class="total-price">¥{{ orderResp.order.totalAmount }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import request from '../utils/request'

const route = useRoute()
const orderResp = ref(null)
const loading = ref(false)
const statusMap = { 0: '待支付', 1: '已支付', 2: '已发货', 3: '已完成', 4: '已取消', 5: '退款中', 6: '已退款' }

async function fetchDetail() {
  loading.value = true
  try {
    const res = await request.get(`/order/${route.params.id}`)
    orderResp.value = res.data
  } finally {
    loading.value = false
  }
}

onMounted(fetchDetail)
</script>

<style scoped>
.order-detail-page { max-width: 900px; margin: 20px auto; padding: 20px; }
.detail-card { background: #fff; border-radius: 8px; padding: 24px; }
.order-status { margin-bottom: 24px; }
.order-status h3 { color: #67c23a; margin-bottom: 8px; }
.order-status p { color: #666; font-size: 14px; }
.receiver-info { margin-bottom: 24px; padding: 16px; background: #f5f7fa; border-radius: 4px; }
.receiver-info p { margin-top: 4px; color: #666; }
.items h4 { margin-bottom: 12px; }
.order-item { display: flex; align-items: center; gap: 16px; padding: 12px 0; border-bottom: 1px solid #f5f5f5; }
.item-total { color: #f56c6c; font-weight: bold; margin-left: auto; }
.order-total { text-align: right; margin-top: 16px; font-size: 16px; }
.total-price { font-size: 24px; color: #f56c6c; font-weight: bold; }
</style>
