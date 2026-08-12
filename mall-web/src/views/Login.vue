<template>
  <div class="login-page">
    <div class="login-card">
      <h2>用户登录</h2>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password prefix-icon="Lock" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div style="display:flex; gap:10px; width:100%">
            <el-input v-model="form.captchaCode" placeholder="请输入验证码" prefix-icon="Key" @keyup.enter="handleLogin" />
            <img
              v-if="captchaImg"
              :src="captchaImg"
              alt="验证码"
              title="点击刷新"
              style="width:120px; height:40px; cursor:pointer; border-radius:4px; flex-shrink:0"
              @click="fetchCaptcha"
            />
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width:100%" @click="handleLogin">登 录</el-button>
        </el-form-item>
      </el-form>
      <div class="bottom-link">
        还没有账号？<router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const captchaImg = ref('')
const form = reactive({ username: '', password: '', captchaKey: '', captchaCode: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function fetchCaptcha() {
  try {
    const res = await request.get('/auth/captcha')
    form.captchaKey = res.data.captchaKey
    captchaImg.value = res.data.captchaImg
    form.captchaCode = ''
  } catch {
    ElMessage.error('验证码获取失败')
  }
}

async function handleLogin() {
  loading.value = true
  try {
    await userStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    // 验证码一次性使用，登录失败后刷新
    fetchCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(fetchCaptcha)
</script>

<style scoped>
.login-page { display: flex; justify-content: center; align-items: center; min-height: 100vh; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.login-card { background: #fff; padding: 40px; border-radius: 12px; width: 400px; box-shadow: 0 10px 40px rgba(0,0,0,0.15); }
.login-card h2 { text-align: center; margin-bottom: 30px; color: #333; }
.bottom-link { text-align: center; font-size: 14px; color: #999; }
.bottom-link a { color: #409eff; }
</style>
