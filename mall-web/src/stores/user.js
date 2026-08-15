import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '../utils/request'
import { useCartStore } from './cart'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref(null)
  const isLoggedIn = ref(!!localStorage.getItem('accessToken'))

  async function fetchUserInfo() {
    try {
      const res = await request.get('/user/info')
      userInfo.value = res.data
      isLoggedIn.value = true
    } catch {
      logout()
    }
  }

  async function login(loginData) {
    const res = await request.post('/auth/login', loginData)
    const { accessToken, refreshToken } = res.data
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
    isLoggedIn.value = true
    await fetchUserInfo()
    return res.data
  }

  async function register(registerData) {
    const res = await request.post('/auth/register', registerData)
    const { accessToken, refreshToken } = res.data
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)
    isLoggedIn.value = true
    return res.data
  }

  function logout() {
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
    userInfo.value = null
    isLoggedIn.value = false
    // 重置购物车缓存，防止切换账号后看到上一用户数据
    useCartStore().reset()
  }

  return { userInfo, isLoggedIn, login, register, logout, fetchUserInfo }
})
