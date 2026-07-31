<template>
  <div class="order-confirm">
    <h2>确认订单</h2>

    <!-- Address Selection -->
    <div class="section">
      <h3>收货地址</h3>
      <div class="address-list">
        <div v-for="addr in addresses" :key="addr.id"
             :class="['address-item', { selected: selectedAddress?.id === addr.id }]"
             @click="selectedAddress = addr">
          <div class="addr-info">
            <b>{{ addr.receiverName }}</b> {{ addr.receiverPhone }}
            <p>{{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detailAddress }}</p>
          </div>
          <el-tag v-if="addr.isDefault" type="danger" size="small">默认</el-tag>
        </div>
        <el-button @click="$router.push('/user/address')">+ 管理地址</el-button>
      </div>
    </div>

    <!-- Order Items -->
    <div class="section">
      <h3>商品信息</h3>
      <div v-for="item in cartStore.checkedItems" :key="item.productId" class="confirm-item">
        <el-image :src="item.image" fit="cover" style="width:80px;height:80px" />
        <span class="item-name">{{ item.name }}</span>
        <span class="item-price">¥{{ item.price }} × {{ item.quantity }}</span>
        <span class="item-total">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
      </div>
    </div>

    <!-- Summary -->
    <div class="section summary">
      <div>商品合计: <span class="price">¥{{ cartStore.totalPrice }}</span></div>
      <div>运费: <span>免运费</span></div>
      <div class="total">应付金额: <span class="price big">¥{{ cartStore.totalPrice }}</span></div>
    </div>

    <div class="submit-area">
      <el-button type="danger" size="large" :loading="submitting" @click="submitOrder">提交订单</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const cartStore = useCartStore()
const addresses = ref([])
const selectedAddress = ref(null)
const submitting = ref(false)

async function fetchAddresses() {
  try {
    const res = await request.get('/user/address')
    addresses.value = res.data || []
    selectedAddress.value = addresses.value.find(a => a.isDefault) || addresses.value[0]
  } catch (e) { /* ignore */ }
}

async function submitOrder() {
  if (!selectedAddress.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  submitting.value = true
  try {
    const items = cartStore.checkedItems.map(i => ({ productId: i.productId, quantity: i.quantity }))
    await request.post('/order', { addressId: selectedAddress.value.id, items })
    ElMessage.success('下单成功')
    cartStore.clearCart()
    router.push('/order/list')
  } catch (e) {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  fetchAddresses()
  cartStore.init()
})
</script>

<style scoped>
.order-confirm { max-width: 900px; margin: 20px auto; padding: 20px; }
.section { background: #fff; padding: 20px; border-radius: 8px; margin-bottom: 16px; }
.section h3 { margin-bottom: 12px; font-size: 16px; }
.address-list { display: flex; flex-wrap: wrap; gap: 12px; }
.address-item { border: 2px solid #eee; border-radius: 8px; padding: 12px; cursor: pointer; min-width: 220px; transition: border-color 0.2s; }
.address-item.selected { border-color: #409eff; }
.addr-info p { font-size: 13px; color: #666; margin-top: 4px; }
.confirm-item { display: flex; align-items: center; gap: 16px; padding: 12px 0; border-bottom: 1px solid #f5f5f5; }
.item-name { flex: 1; }
.item-price { color: #666; }
.item-total { color: #f56c6c; font-weight: bold; }
.summary { text-align: right; line-height: 2; }
.price { color: #f56c6c; font-weight: bold; }
.price.big { font-size: 24px; }
.submit-area { text-align: right; }
</style>
