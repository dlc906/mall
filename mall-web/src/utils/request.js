import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

/**
 * 将后端/网络错误消息转换为用户友好的提示。
 * 技术性错误（内部异常堆栈、网络错误）展示通用话术，业务提示（库存不足等）直接透传。
 */
function toUserMessage(raw) {
  if (!raw || typeof raw !== 'string') return '请求失败，请稍后再试'
  if (
    raw.includes('服务器内部错误') ||
    raw.includes('Request failed') ||
    raw.includes('Network Error') ||
    raw.includes('timeout') ||
    raw.includes('ECONN')
  ) {
    return '系统繁忙，请稍后再试'
  }
  return raw
}

// Request interceptor: attach token
request.interceptors.request.use(
  config => {
    const token = localStorage.getItem('accessToken')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// Response interceptor
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      // 控制台打印完整错误详情（开发调试用）
      console.error('[API Error]', {
        url: response.config.url,
        method: (response.config.method || '').toUpperCase(),
        code: res.code,
        message: res.message,
        data: res.data,
        timestamp: res.timestamp
      })
      ElMessage.error(toUserMessage(res.message))
      if (res.code === 401) {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  error => {
    // 控制台打印完整错误详情（开发调试用）
    console.error('[HTTP Error]', {
      url: error.config?.url,
      method: (error.config?.method || '').toUpperCase(),
      status: error.response?.status,
      message: error.message,
      response: error.response?.data
    })
    if (error.response?.status === 401) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      router.push('/login')
    }
    ElMessage.error(toUserMessage(error.message))
    return Promise.reject(error)
  }
)

export default request
