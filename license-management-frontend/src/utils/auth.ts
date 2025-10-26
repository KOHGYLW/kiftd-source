import { ElMessage } from 'element-plus'

const TOKEN_KEY = 'license-token'
const USER_KEY = 'license-user'

/**
 * 存储token
 */
export const setToken = (token: string) => {
  localStorage.setItem(TOKEN_KEY, token)
}

/**
 * 获取token
 */
export const getToken = (): string | null => {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 移除token
 */
export const removeToken = () => {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * 存储用户信息
 */
export const setUserInfo = (user: any) => {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

/**
 * 获取用户信息
 */
export const getUserInfo = () => {
  const userStr = localStorage.getItem(USER_KEY)
  return userStr ? JSON.parse(userStr) : null
}

/**
 * 移除用户信息
 */
export const removeUserInfo = () => {
  localStorage.removeItem(USER_KEY)
}

/**
 * 清除所有认证信息
 */
export const clearAuthInfo = () => {
  removeToken()
  removeUserInfo()
}

/**
 * 检查是否已登录
 */
export const isLoggedIn = (): boolean => {
  return !!getToken()
}

/**
 * 检查用户权限
 */
export const hasPermission = (permission: string): boolean => {
  const user = getUserInfo()
  if (!user) return false
  
  // 管理员拥有所有权限
  if (user.role === 'admin') return true
  
  // 检查用户是否有特定权限
  return user.permissions?.includes(permission) || false
}

/**
 * 登出处理
 */
export const logout = () => {
  clearAuthInfo()
  ElMessage.success('已退出登录')
  // 刷新页面或跳转到登录页
  window.location.href = '/login'
}