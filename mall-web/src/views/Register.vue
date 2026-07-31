<template>
  <div class="register-page">
    <div class="register-card">
      <h2>用户注册</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名(3-20位)" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码(6-20位)" show-password prefix-icon="Lock" />
        </el-form-item>
        <el-form-item prop="phone">
          <el-input v-model="form.phone" placeholder="手机号(选填)" prefix-icon="Phone" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.inviteCode" placeholder="邀请码(选填)" prefix-icon="Link" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width:100%" @click="handleRegister">注 册</el-button>
        </el-form-item>
      </el-form>
      <div class="bottom-link">
        已有账号？<router-link to="/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '', phone: '', inviteCode: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 3, max: 20, message: '用户名长度3-20位', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }]
}

async function handleRegister() {
  loading.value = true
  try {
    await userStore.register(form)
    ElMessage.success('注册成功')
    router.push('/')
  } catch (e) {
    // Error handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page { display: flex; justify-content: center; align-items: center; min-height: 100vh; background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%); }
.register-card { background: #fff; padding: 40px; border-radius: 12px; width: 400px; box-shadow: 0 10px 40px rgba(0,0,0,0.15); }
.register-card h2 { text-align: center; margin-bottom: 30px; color: #333; }
.bottom-link { text-align: center; font-size: 14px; color: #999; }
.bottom-link a { color: #409eff; }
</style>
