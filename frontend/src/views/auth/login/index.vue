<template>
  <LoginForm type="login">
    <template #default>
      <el-form :model="form" :rules="rules" ref="formRef" label-position="top">
        <el-form-item prop="username">
          <el-input 
            v-model="form.username"
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

const form = ref({
  username: '',
  password: '',
  protocol: false,
})

const rules = ref({
  username: [
    { required: true, message: '请输入邮箱账号', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
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

const handleProtocol = (type) => {
  window.open(`${window.location.origin}/protocol/${type}`)
}
const handleLogin = async () => {
  if (!formRef.value) return
  
  try {
    // 验证表单
    await formRef.value.validate()
    
    loading.value = true
    console.log('login', form.value)
    
    // 这里添加登录逻辑
    // await loginApi(form.value)
    
    // 登录成功后的处理
    ElMessage.success('登录成功！')
    
  } catch (error) {
    console.error('登录失败:', error)
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