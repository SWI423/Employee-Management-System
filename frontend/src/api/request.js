import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

// axios 实例：统一 baseURL、超时、拦截器
// 开发环境走 Vite 代理（/api → localhost:8080）
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// ---------- 请求拦截器：带 Token ----------
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = 'Bearer ' + token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// ---------- 响应拦截器：统一处理 Result 格式 ----------
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 后端统一返回 {code, msg, data}
    if (res.code === 200) {
      return res
    }
    // 401 未登录/过期 → 清token 跳登录页
    if (res.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      router.push('/login')
      ElMessage.error(res.msg || '登录已过期')
      return Promise.reject(new Error(res.msg))
    }
    // 403 无权限 / 其他业务错误 → 弹出提示
    ElMessage.error(res.msg || '操作失败')
    return Promise.reject(new Error(res.msg))
  },
  (error) => {
    // 网络层错误（断网、超时、500）
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
