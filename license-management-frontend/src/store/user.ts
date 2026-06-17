import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getUserInfo as fetchUserInfo } from '@/api/auth'
import { setUserInfo, removeUserInfo, clearAuthInfo } from '@/utils/auth'
import type { User } from '@/types'

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(null)
  const permissions = ref<string[]>([])
  const loading = ref(false)

  // 获取用户信息
  const fetchUser = async () => {
    if (loading.value) return
    
    loading.value = true
    try {
      const userInfo = await fetchUserInfo()
      user.value = userInfo
      permissions.value = userInfo.permissions || []
      setUserInfo(userInfo)
      return userInfo
    } catch (error) {
      console.error('获取用户信息失败:', error)
      logout()
      throw error
    } finally {
      loading.value = false
    }
  }

  // 设置用户信息
  const setUser = (userInfo: User) => {
    user.value = userInfo
    permissions.value = userInfo.permissions || []
    setUserInfo(userInfo)
  }

  // 检查权限
  const hasPermission = (permission: string) => {
    if (!user.value) return false
    if (user.value.role === 'admin') return true
    return permissions.value.includes(permission)
  }

  // 登出
  const logout = () => {
    user.value = null
    permissions.value = []
    clearAuthInfo()
  }

  return {
    user: readonly(user),
    permissions: readonly(permissions),
    loading: readonly(loading),
    fetchUser,
    setUser,
    hasPermission,
    logout
  }
})