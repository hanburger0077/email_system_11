<template>
  <LoginForm type="register">
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
        
        <!-- 恢复码显示 -->
        <el-form-item label="恢复码（请务必保存）">
          <div class="recovery-code-container">
            <el-input 
              v-model="form.recoveryCode"
              placeholder="系统自动生成恢复码" 
              size="large"
              readonly
              class="recovery-code-input"
            >
              <template #append>
                <el-button @click="copyRecoveryCode" type="primary">复制</el-button>
              </template>
            </el-input>
            <div class="recovery-code-tip">
              <el-icon><InfoFilled /></el-icon>
              恢复码用于找回密码，请妥善保管，不要泄露给他人
            </div>
          </div>
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
import { registerUser, handleApiError, loginUser } from '@/utils/api'
import { useUserStore } from '@/stores/user'
import { InfoFilled } from '@element-plus/icons-vue'

const form = ref({
  email: '',
  password: '',
  passwordConfirm: '',
  username: '',
  phone: '',
  recoveryCode: '',
  protocol: false,
})

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

const handleLogin = async () => {
  try {
    // 1. 先注册
    const registerResponse = await registerUser(form.value)
    handleApiError(registerResponse)
    
    // 2. 从后端响应中提取恢复码
    const recoveryCodeMatch = registerResponse.message.match(/恢复码： (\w+)/);
    if (recoveryCodeMatch) {
      form.value.recoveryCode = recoveryCodeMatch[1];
      ElMessage.success('注册成功！请务必保存您的恢复码');
    }
    
    // 3. 自动登录
    const loginResponse = await loginUser({
      email: form.value.email,
      password: form.value.password
    })
    handleApiError(loginResponse)
    
    userStore.setUserInfo(loginResponse.data)
    router.push('/main')
    
  } catch (error) {
    console.error('注册失败:', error)
    ElMessage.error(error.message || '注册失败，请重试')
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