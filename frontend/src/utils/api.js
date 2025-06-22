// API 基础配置
const API_BASE_URL = '/api'

// 通用请求函数
const request = async (url, options = {}) => {
  const config = {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
    },
    credentials: 'include', // 包含cookies
    ...options,
  }

  if (config.body && typeof config.body === 'object') {
    config.body = JSON.stringify(config.body)
  }

  try {
    const response = await fetch(`${API_BASE_URL}${url}`, config)
    
    // 检查响应状态
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    // 获取响应文本
    const responseText = await response.text()
    
    // 检查响应是否为空
    if (!responseText || responseText.trim() === '') {
      throw new Error('Empty response from server')
    }
    
    // 尝试解析JSON
    try {
      const data = JSON.parse(responseText)
      return data
    } catch (jsonError) {
      console.error('JSON解析失败:', jsonError)
      console.error('响应内容:', responseText)
      throw new Error('Invalid JSON response from server')
    }
    
  } catch (error) {
    console.error('API请求失败:', error)
    throw error
  }
}

// 用户注册
export const registerUser = (userData) => {
  return request('/user/register', {
    method: 'POST',
    body: {
      username: userData.username,
      password: userData.password,
      confirmPassword: userData.passwordConfirm,
      email: userData.email,
      phone: userData.phone,
      recoveryCode: userData.recoveryCode
    }
  })
}

// 用户登录
export const loginUser = (userData) => {
  return request('/user/login', {
    method: 'POST',
    body: {
      email: userData.email,
      password: userData.password
    }
  })
}

// 修改用户名
export const updateUsername = (userData) => {
  return request('/user/updateUsername', {
    method: 'POST',
    body: {
      newUsername: userData.newUsername
    }
  })
}

// 修改手机号
export const updatePhone = (userData) => {
  return request('/user/updatePhone', {
    method: 'POST',
    body: {
      newPhone: userData.newPhone
    }
  })
}

// 修改密码
export const updatePassword = (userData) => {
  return request('/user/updatePassword', {
    method: 'POST',
    body: {
      //email: userData.email,
      oldPassword: userData.oldPassword,
      newPassword: userData.newPassword,
      confirmNewPassword: userData.confirmNewPassword || userData.newPassword // ✅ 新增这行
    }
  })
}

// 退出登录
export const exitUser = () => {
  return request('/user/exit', {
    method: 'POST',
    body: {}
  })
}

// 注销账户
export const logoutUser = (userData) => {
  return request('/user/logout', {
    method: 'POST',
    body: {
      email: userData.email,
      password: userData.password
    }
  })
}

// 重置密码
export const resetPassword = (userData) => {
  return request('/user/recoverPassword', {
    method: 'POST',
    body: {
      email: userData.email,
      recoveryCode: userData.recoveryCode,
      newPassword: userData.newPassword,
      confirmNewPassword: userData.confirmPassword
    }
  })
}

// 统一错误处理
export const handleApiError = (response) => {
 if (response.code === 'code.ok') {
    return response
  }
  
  // 所有非成功的响应都视为错误
  throw new Error(response.reason || response.message || '操作失败')
} 