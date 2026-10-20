import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/emp',
    children: [
      {
        path: 'emp',
        name: 'EmpList',
        component: () => import('@/views/EmpList.vue'),
        meta: { title: '员工管理', icon: 'User' }
      },
      {
        path: 'dept',
        name: 'DeptList',
        component: () => import('@/views/DeptList.vue'),
        meta: { title: '部门管理', icon: 'OfficeBuilding' }
      },
      {
        path: 'perm',
        name: 'PermList',
        component: () => import('@/views/PermList.vue'),
        meta: { title: '权限管理', icon: 'Lock' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局前置守卫：未登录一律去登录页
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.path !== '/login' && !userStore.token) {
    next('/login')
  } else if (to.path === '/login' && userStore.token) {
    next('/')
  } else {
    next()
  }
})

export default router
