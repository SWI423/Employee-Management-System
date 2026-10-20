import request from './request'

// 登录
export function login(data) {
  return request.post('/auth/login', data)
}

// 注销
export function logout() {
  return request.post('/auth/logout')
}

// 当前用户信息
export function getMe() {
  return request.get('/auth/me')
}
