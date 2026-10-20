import { defineStore } from 'pinia'
import { ref } from 'vue'

// 用户状态：token、角色、权限码
// 权限码驱动按钮级控制：v-if="userStore.hasPerm('emp:add')"
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  function setLogin(data) {
    token.value = data.token
    userInfo.value = data
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  // 是否拥有某个权限码
  function hasPerm(code) {
    if (!userInfo.value) return false
    // admin 角色兜底：拥有全部权限
    if (userInfo.value.role === 'admin') return true
    return (userInfo.value.permissions || []).includes(code)
  }

  return { token, userInfo, setLogin, logout, hasPerm }
})
