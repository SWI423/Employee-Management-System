<template>
  <div class="layout">
    <!-- 侧边栏菜单 -->
    <aside class="sidebar">
      <div class="logo">
        <el-icon :size="28"><FirstAidKit /></el-icon>
        <span>员工管理系统</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#1d2b36"
        text-color="#c0c9d1"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <!-- 主区域 -->
    <div class="main">
      <header class="header">
        <div class="breadcrumb">{{ currentTitle }}</div>
        <div class="user-area">
          <el-tag size="small" :type="roleTagType">{{ roleName }}</el-tag>
          <span class="username">{{ userInfo?.name || userInfo?.username }}</span>
          <el-button type="danger" text size="small" @click="handleLogout">退出</el-button>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { logout } from '@/api/auth'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 菜单配置（与 router children 对应，权限码控制可见性；perm 为空表示登录即可见）
const menus = computed(() => {
  const all = [
    { path: '/emp', title: '员工管理', icon: 'User', perm: 'emp:view' },
    { path: '/dept', title: '部门管理', icon: 'OfficeBuilding', perm: 'dept:view' },
    { path: '/perm', title: '权限管理', icon: 'Lock', perm: 'perm:view' },
    { path: '/ai', title: 'AI 智能助手', icon: 'ChatDotRound', perm: null }
  ]
  return all.filter((m) => !m.perm || userStore.hasPerm(m.perm))
})

const currentTitle = computed(() => {
  const item = menus.value.find((m) => m.path === route.path)
  return item ? item.title : ''
})

const userInfo = computed(() => userStore.userInfo)
const roleName = computed(() => (userInfo.value?.role === 'admin' ? '管理员' : '普通用户'))
const roleTagType = computed(() => (userInfo.value?.role === 'admin' ? 'danger' : 'info'))

async function handleLogout() {
  try {
    await logout()
  } finally {
    userStore.logout()
    ElMessage.success('已安全退出')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout { display: flex; height: 100vh; }
.sidebar {
  width: 220px;
  background: #1d2b36;
  display: flex;
  flex-direction: column;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.el-menu { border-right: none; flex: 1; }
.main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.header {
  height: 56px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}
.breadcrumb { font-size: 15px; font-weight: 500; color: #303133; }
.user-area { display: flex; align-items: center; gap: 12px; }
.username { font-size: 14px; color: #606266; }
.content { flex: 1; padding: 20px; overflow: auto; }
</style>
