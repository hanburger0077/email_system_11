<template>
  <LoginForm type="login">
    <template #default>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item prop="email">
          <el-input 
            v-model="form.email"
            type="email" 
            placeholder="请输入邮箱账号" 
            size="large"
            clearable
          />
        </el-form-item> 
        <el-form-item prop="password" >
          <el-input 
            v-model="form.password"
            type="password" 
            placeholder="请输入密码" 
            size="large"
            show-password
            clearable
          >
            <template #suffix>
              <span class="email-link forgot-password-suffix" @click="handleForgotPassword">忘记密码？</span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="protocol">
          <div class="protocol">
            <el-checkbox v-model="form.protocol">我已阅读并同意
              <span class="email-link" @click="handleProtocol('serviceterm')">《服务条款》</span>
              和
              <span class="email-link" @click="handleProtocol('privacypolicy')">《隐私政策》</span></el-checkbox>
          </div>
        </el-form-item>
        
          <el-button 
            class="email-button"
            size="large"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
      </el-form>
    </template>
  </LoginForm>
</template>

<script setup>
import LoginForm from '../components/loginForm.vue'
import { loginUser, handleApiError } from '@/utils/api'
import { useUserStore } from '@/stores/user'

const form = ref({
  email: '',
  password: '',
  protocol: false,
})

const rules = ref({
  email: [
    { required: true, message: '请输入邮箱账号', trigger: 'blur' },
    { 
      pattern: /^\d{6,8}@flowmail\.com$/, 
      message: '邮箱格式错误，前6-8位数字，后缀为@flowmail.com', 
      trigger: 'blur' 
    }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  protocol: [
    {
      validator: (rule, value, callback) => {
        if (!value) {
          return callback(new Error('请阅读并同意协议'));
        }
        return callback();
        
      },
    },
  ]
})

const formRef = ref(null)
const loading = ref(false)
const router = useRouter()
const userStore = useUserStore()

const handleProtocol = (type) => {
  window.open(`${window.location.origin}/protocol/${type}`)
}

const handleForgotPassword = () => {
  router.push('/auth/forgotpassword')
}

// 调用 IMAP 登录接口连接邮箱服务器
const connectToIMAPServer = async (username, password) => {
  try {
    // 创建请求数据
    const formData = new FormData()
    formData.append('username', username)
    formData.append('password', password)
    
    // 发送 IMAP 连接请求
    const response = await fetch('/api/mail/connect', {
      method: 'POST',
      body: formData
    })
    
    // 解析响应
    const result = await response.json()
    
    // 检查连接是否成功
    if (result.code === 'code.ok') {
      console.log('IMAP 连接成功:', result)
      return { success: true, data: result.data }
    } else {
      console.error('IMAP 连接失败:', result)
      return { success: false, message: result.message || '邮箱连接失败' }
    }
  } catch (error) {
    console.error('IMAP 连接错误:', error)
    return { success: false, message: '邮箱服务器连接错误' }
  }
}

const handleLogin = async () => {
  if (!formRef.value) return
  
  try {
    // 验证表单
    await formRef.value.validate()
    
    loading.value = true
    
    // 1. 首先调用系统登录API
    const loginResponse = await loginUser(form.value)
    
    // 处理API响应
    handleApiError(loginResponse)
    
    // 2. 系统登录成功后，调用 IMAP 连接接口
    const imapResult = await connectToIMAPServer(form.value.email, form.value.password)
    
    if (!imapResult.success) {
      // IMAP 连接失败，抛出错误
      throw new Error(imapResult.message || 'IMAP 服务器连接失败')
    }
    
    // 3. 登录和IMAP连接都成功后的处理
    ElMessage.success('登录成功')
    
    // 存储用户信息，包括从 IMAP 连接中获取的邮箱相关数据
    userStore.setUserInfo({
      ...loginResponse.data,
      emailInfo: imapResult.data
    })
    
    // 跳转到邮箱主页
    router.push('/main')
    
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error(error.message || '登录失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.protocol{
  font-size: 10px;
  height: 20px;
}

.forgot-password-suffix {
  padding-right: 8px;
  font-size: 12px;
}
</style>