import { ref, reactive } from 'vue'
import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', () => {
  // 用户信息
  const userInfo = reactive({
    id: null,
    username: '',
    email: '',
    phone: ''
  })

  
  // 登录状态
  const isLoggedIn = ref(false)

  // 设置用户信息
  const setUserInfo = (info) => {
    userInfo.id = info.id
    userInfo.username = info.username
    userInfo.email = info.email
    userInfo.phone = info.phone
    isLoggedIn.value = true
    
    // 保存到localStorage以便恢复
    localStorage.setItem('userInfo', JSON.stringify({
      id: info.id,
      username: info.username,
      email: info.email,
      phone: info.phone
    }))
  }

  // 清空用户信息
  const clearUserInfo = () => {
    userInfo.id = null
    userInfo.username = ''
    userInfo.email = ''
    userInfo.phone = ''
    isLoggedIn.value = false
    
    // 清空localStorage
    localStorage.removeItem('userInfo')
  }

  // 更新用户名
  const updateUserName = (newUsername) => {
    userInfo.username = newUsername
  }

  // 更新手机号
  const updateUserPhone = (newPhone) => {
    userInfo.phone = newPhone
  }

  // 从Cookie恢复用户状态（使用localStorage作为备份）
  const initFromCookie = () => {
    // 读取Cookie中的邮箱
    const cookies = document.cookie.split(';')
    const loginCookie = cookies.find(cookie => cookie.trim().startsWith('loginUserEmail='))
    
    if (loginCookie) {
      const email = loginCookie.split('=')[1]
      if (email && email.trim()) {
        // 尝试从localStorage恢复完整用户信息
        const savedUserInfo = localStorage.getItem('userInfo')
        if (savedUserInfo) {
          try {
            const parsedUserInfo = JSON.parse(savedUserInfo)
            if (parsedUserInfo.email === email) {
              setUserInfo(parsedUserInfo)
              console.log('从localStorage恢复用户状态:', parsedUserInfo)
              return true
            }
          } catch (error) {
            console.log('localStorage数据解析失败')
          }
        }
        
        // 如果localStorage没有数据，使用基本信息
        userInfo.email = email
        userInfo.username = email.split('@')[0]
        isLoggedIn.value = true
        console.log('使用基本信息恢复用户状态:', email)
        return true
      }
    }
    
    console.log('未找到有效的登录Cookie')
    return false
  }

  return {
    userInfo,
    isLoggedIn,
    setUserInfo,
    clearUserInfo,
    updateUserName,
    updateUserPhone,
    initFromCookie
  }
}) 