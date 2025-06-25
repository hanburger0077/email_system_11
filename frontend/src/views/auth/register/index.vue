<template>
  <LoginForm type="register">
    <template #default>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item prop="email">
          <el-input 
            v-model="form.email"
            type="text" 
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
          />
        </el-form-item>

        <el-form-item prop="passwordConfirm" >
          <el-input 
            v-model="form.passwordConfirm"
            type="password" 
            placeholder="请确认密码" 
            size="large"
            show-password
            clearable
          />
        </el-form-item>


        <el-form-item prop="username">
          <el-input 
            v-model="form.username"
            placeholder="请输入用户名" 
            size="large"
            clearable
          />
        </el-form-item> 
        <el-form-item prop="phone">
          <el-input
            v-model="form.phone"
            type="phone" 
            placeholder="请输入手机号" 
            size="large"
            clearable
          >

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
          @click="handleRegister"
        >
          注册
        </el-button>
      </el-form>
    </template>
  </LoginForm>
  <RecoveryCode ref="recoveryCodeDialog" :recovery-code="form.recoveryCode" /></template>

<script setup>
import LoginForm from '../components/loginForm.vue'
import { registerUser, handleApiError, loginUser } from '@/utils/api'
import { useUserStore } from '@/stores/user'
import { InfoFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import RecoveryCode from '../components/recoveryCode.vue'
const form = ref({
  email: '',
  password: '',
  passwordConfirm: '',
  username: '',
  phone: '',
  recoveryCode: '',
  protocol: false,
})

const recoveryCodeDialog = ref(null)
// 复制恢复码
const copyRecoveryCode = async () => {
  try {
    await navigator.clipboard.writeText(form.value.recoveryCode)
    ElMessage.success('恢复码已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败，请手动复制')
  }
}

const validatePassword = (rule, value, callback) => {
  if (value !== form.value.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

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
  passwordConfirm: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
    { validator: validatePassword, trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
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

const handleRegister = async () => {
  if (!formRef.value) return
  
  try {
    // 验证表单
    await formRef.value.validate()
    
    loading.value = true
    
    // 1. 先注册
    const registerResponse = await registerUser(form.value)
    handleApiError(registerResponse)
    
    // 2. 从后端响应的 data 中获取恢复码
    if (registerResponse.data && registerResponse.data.recoveryCode) {
      form.value.recoveryCode = registerResponse.data.recoveryCode;
      ElMessage.success('注册成功！请保存您的恢复码');
    } else {
      console.warn('注册成功但未获取到恢复码:', registerResponse);
      ElMessage.warning('注册成功，但未获取到恢复码');
    }
    
    // 3. 自动登录
    const loginResponse = await loginUser({
      email: form.value.email,
      password: form.value.password
    })
    handleApiError(loginResponse)
    
    // 4. 调用 IMAP 连接接口
    const imapResult = await connectToIMAPServer(form.value.email, form.value.password)
    
    if (!imapResult.success) {
      // IMAP 连接失败，记录错误但不中断后续流程
      console.error('IMAP 连接失败:', imapResult.message);
      ElMessage.warning('邮箱服务器连接失败，部分功能可能受限');
    } else {
      console.log('IMAP 连接成功');
    }
    
    // 5. 存储用户信息，包括从 IMAP 连接中获取的邮箱相关数据
    userStore.setUserInfo({
      ...loginResponse.data,
      emailInfo: imapResult.success ? imapResult.data : null
    })

    // 6. 显示恢复码对话框（只有在有恢复码时才显示）
    if (form.value.recoveryCode) {
      await recoveryCodeDialog.value.open()
      ElMessage.success('登录成功')
    }

    // 7. 跳转到邮箱主页
    router.push('/main')
    
  } catch (error) {
    console.error('注册失败:', error)
    ElMessage.error(error.message || '注册失败，请重试')
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

.recovery-code-container {
  width: 100%;
}

.recovery-code-input {
  margin-top: 0px;
  margin-bottom: 0px;
}


/* 恢复码提示样式 */
.recovery-code-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
  margin-bottom: 0px;
  font-size: 12px;
  color: #dc3545;
  font-weight: 400;
  line-height: 1.4;
}

.recovery-code-tip .el-icon {
  color: #dc3545;
  font-size: 14px;
}
</style>