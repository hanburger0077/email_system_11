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
        
        <el-form-item prop="protocol">
          <div class="protocol">
            <el-checkbox v-model="protocol">我已阅读并同意
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

const form = ref({
  email: '',
  password: '',
  passwordConfirm: '',
  username: '',
  phone: ''
})

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
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
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
    { required: true, message: '请阅读并同意服务条款和隐私政策', trigger: 'blur' }
  ]
})

const formRef = ref(null)
const loading = ref(false)

const handleProtocol = (type) => {
  window.open(`${window.location.origin}/protocol/${type}`)
}

const handleLogin = async () => {
  if (!formRef.value) return
  
  try {
    // 验证表单
    await formRef.value.validate()
    
    loading.value = true
    console.log('register', form.value)
    
    // 这里添加登录逻辑
    // await loginApi(form.value)
    
    // 登录成功后的处理
    ElMessage.success('注册成功！')
    
  } catch (error) {
    console.error('注册失败:', error)
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