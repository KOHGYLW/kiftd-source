import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  // 侧边栏状态
  const sidebarCollapsed = ref(false)
  
  // 主题设置
  const theme = ref<'light' | 'dark'>('light')
  
  // 设备类型
  const device = ref<'desktop' | 'mobile'>('desktop')
  
  // 切换侧边栏
  const toggleSidebar = () => {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }
  
  // 设置侧边栏状态
  const setSidebarCollapsed = (collapsed: boolean) => {
    sidebarCollapsed.value = collapsed
  }
  
  // 设置主题
  const setTheme = (newTheme: 'light' | 'dark') => {
    theme.value = newTheme
    document.documentElement.setAttribute('data-theme', newTheme)
    localStorage.setItem('theme', newTheme)
  }
  
  // 设置设备类型
  const setDevice = (newDevice: 'desktop' | 'mobile') => {
    device.value = newDevice
  }
  
  // 初始化主题
  const initTheme = () => {
    const savedTheme = localStorage.getItem('theme') as 'light' | 'dark' | null
    if (savedTheme) {
      setTheme(savedTheme)
    }
  }
  
  return {
    sidebarCollapsed: readonly(sidebarCollapsed),
    theme: readonly(theme),
    device: readonly(device),
    toggleSidebar,
    setSidebarCollapsed,
    setTheme,
    setDevice,
    initTheme
  }
})