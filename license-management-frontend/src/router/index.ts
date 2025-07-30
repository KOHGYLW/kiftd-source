import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

// 配置NProgress
NProgress.configure({ showSpinner: false })

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: {
      title: '登录',
      requiresAuth: false
    }
  },
  {
    path: '/',
    component: () => import('@/components/layout/Layout.vue'),
    redirect: '/dashboard',
    meta: {
      requiresAuth: true
    },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: {
          title: '仪表板',
          icon: 'Dashboard'
        }
      },
      {
        path: 'customers',
        name: 'CustomerManagement',
        component: () => import('@/views/CustomerManagement.vue'),
        meta: {
          title: '客户管理',
          icon: 'User'
        }
      },
      {
        path: 'customers/:id',
        name: 'CustomerDetail',
        component: () => import('@/views/CustomerDetail.vue'),
        meta: {
          title: '客户详情',
          icon: 'User'
        }
      },
      {
        path: 'licenses',
        name: 'LicenseManagement',
        component: () => import('@/views/LicenseManagement.vue'),
        meta: {
          title: '授权管理',
          icon: 'Key'
        }
      },
      {
        path: 'licenses/create',
        name: 'LicenseWizard',
        component: () => import('@/views/LicenseWizard.vue'),
        meta: {
          title: '生成授权',
          icon: 'Key'
        }
      },
      {
        path: 'licenses/:id',
        name: 'LicenseDetail',
        component: () => import('@/views/LicenseDetail.vue'),
        meta: {
          title: '授权详情',
          icon: 'Key'
        }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: {
          title: '系统设置',
          icon: 'Setting'
        }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: {
      title: '页面不存在'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  NProgress.start()
  
  // 设置页面标题
  document.title = to.meta?.title 
    ? `${to.meta.title} - ${import.meta.env.VITE_APP_TITLE}`
    : import.meta.env.VITE_APP_TITLE
  
  // 检查是否需要登录
  if (to.meta?.requiresAuth !== false) {
    const token = getToken()
    if (!token) {
      ElMessage.warning('请先登录')
      next('/login')
      return
    }
  }
  
  // 已登录用户访问登录页面，重定向到首页
  if (to.path === '/login' && getToken()) {
    next('/')
    return
  }
  
  next()
})

router.afterEach(() => {
  NProgress.done()
})

export default router