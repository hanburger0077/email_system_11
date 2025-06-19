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
  }

  // 清空用户信息
  const clearUserInfo = () => {
    userInfo.id = null
    userInfo.username = ''
    userInfo.email = ''
    userInfo.phone = ''
    isLoggedIn.value = false
  }

  // 更新用户名
  const updateUserName = (newUsername) => {
    userInfo.username = newUsername
  }

  // 更新手机号
  const updateUserPhone = (newPhone) => {
    userInfo.phone = newPhone
  }

  return {
    userInfo,
    isLoggedIn,
    setUserInfo,
    clearUserInfo,
    updateUserName,
    updateUserPhone
  }
}) 