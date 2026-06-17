import { http } from './request'
import type { LoginForm, LoginResponse, User } from '@/types'

// 用户登录
export const login = (data: LoginForm): Promise<LoginResponse> => {
  return http.post('/auth/login', data)
}

// 用户登出
export const logout = (): Promise<void> => {
  return http.post('/auth/logout')
}

// 获取用户信息
export const getUserInfo = (): Promise<User> => {
  return http.get('/auth/user')
}

// 修改密码
export const changePassword = (data: {
  oldPassword: string
  newPassword: string
}): Promise<void> => {
  return http.post('/auth/change-password', data)
}

// 刷新token
export const refreshToken = (): Promise<{ token: string; expiresIn: number }> => {
  return http.post('/auth/refresh')
}