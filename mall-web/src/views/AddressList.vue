<template>
  <div class="address-page">
    <h2>收货地址</h2>
    <el-button type="primary" @click="showDialog(null)">+ 新增地址</el-button>

    <div class="addr-list" v-loading="loading">
      <div v-for="addr in addresses" :key="addr.id" class="addr-card">
        <div class="addr-info">
          <div><b>{{ addr.receiverName }}</b> {{ addr.receiverPhone }}
            <el-tag v-if="addr.isDefault" type="danger" size="small" style="margin-left:8px">默认</el-tag>
          </div>
          <p>{{ addr.province }}{{ addr.city }}{{ addr.district }} {{ addr.detailAddress }}</p>
        </div>
        <div class="addr-actions">
          <el-button link type="primary" @click="showDialog(addr)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(addr.id)">删除</el-button>
          <el-button v-if="!addr.isDefault" link @click="handleSetDefault(addr.id)">设为默认</el-button>
        </div>
      </div>
      <el-empty v-if="!loading && addresses.length === 0" description="暂无地址" />
    </div>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑地址' : '新增地址'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="收货人">
          <el-input v-model="form.receiverName" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.receiverPhone" />
        </el-form-item>
        <el-form-item label="省份">
          <el-input v-model="form.province" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="form.city" />
        </el-form-item>
        <el-form-item label="区/县">
          <el-input v-model="form.district" />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="form.detailAddress" type="textarea" />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import request from '../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const addresses = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const editId = ref(null)

const form = reactive({
  receiverName: '', receiverPhone: '', province: '', city: '',
  district: '', detailAddress: '', isDefault: 0
})

async function fetchAddresses() {
  loading.value = true
  try {
    const res = await request.get('/user/address')
    addresses.value = res.data || []
  } finally {
    loading.value = false
  }
}

function showDialog(addr) {
  if (addr) {
    editId.value = addr.id
    Object.assign(form, addr)
  } else {
    editId.value = null
    Object.assign(form, { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detailAddress: '', isDefault: 0 })
  }
  dialogVisible.value = true
}

async function handleSave() {
  try {
    if (editId.value) {
      await request.put(`/user/address/${editId.value}`, form)
    } else {
      await request.post('/user/address', form)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchAddresses()
  } catch { /* handled */ }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确认删除该地址？')
    await request.delete(`/user/address/${id}`)
    ElMessage.success('已删除')
    fetchAddresses()
  } catch { /* cancelled */ }
}

async function handleSetDefault(id) {
  try {
    await request.put(`/user/address/${id}/default`)
    ElMessage.success('已设为默认')
    fetchAddresses()
  } catch { /* handled */ }
}

onMounted(fetchAddresses)
</script>

<style scoped>
.address-page { max-width: 800px; margin: 20px auto; padding: 20px; }
.addr-list { margin-top: 16px; }
.addr-card { background: #fff; padding: 16px; border-radius: 8px; margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center; }
.addr-info p { color: #666; font-size: 14px; margin-top: 4px; }
.addr-actions { display: flex; gap: 8px; }
</style>
