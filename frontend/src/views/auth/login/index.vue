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
          />
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
      pattern: /^\d{6,8}@hh\.com$/, 
      message: '邮箱格式错误，前6-8位数字，后缀为@hh.com', 
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
const handleLogin = async () => {
  if (!formRef.value) return
  
  try {
    // 验证表单
    await formRef.value.validate()
    
    loading.value = true
    
    // 调用登录API
    const response = await loginUser(form.value)
    
    // 处理API响应
    handleApiError(response)
    
    // 登录成功后的处理
    ElMessage.success('登录成功！')
    
    // 存储用户信息
    userStore.setUserInfo(response.data)
    
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

</style>