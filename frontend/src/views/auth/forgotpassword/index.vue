<template>
  <LoginForm type="forgotpassword">
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
        
        <el-form-item prop="recoveryCode">
          <el-input 
            v-model="form.recoveryCode"
            placeholder="请输入8位恢复码" 
            size="large"
            clearable
            maxlength="8"
          />
        </el-form-item>
        
        <el-form-item prop="newPassword">
          <el-input 
            v-model="form.newPassword"
            type="password" 
            placeholder="请输入新密码" 
            size="large"
            show-password
            clearable
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input 
            v-model="form.confirmPassword"
            type="password" 
            placeholder="请确认新密码" 
            size="large"
            show-password
            clearable
          />
        </el-form-item>

        <el-button 
          class="email-button"
          size="large"
          :loading="loading"
          @click="handleResetPassword"
        >
          重置密码
        </el-button>
      </el-form>
    </template>
  </LoginForm>
</template>

<script setup>
import LoginForm from '../components/loginForm.vue'
import { resetPassword, handleApiError } from '@/utils/api'

const form = ref({
  email: '',
  recoveryCode: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.value.newPassword) {
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
  recoveryCode: [
    { required: true, message: '请输入恢复码', trigger: 'blur' },
    { len: 8, message: '恢复码必须是8位', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9]{8}$/, message: '恢复码只能包含字母和数字', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' },
    { pattern: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d]{6,20}$/, message: '密码必须包含大写字母、小写字母和数字，长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
})

const formRef = ref(null)
const loading = ref(false)
const router = useRouter()

const handleResetPassword = async () => {
  if (!formRef.value) return
  
  try {
    // 验证表单
    await formRef.value.validate()
    
    loading.value = true
    
    // 调用重置密码API
    const response = await resetPassword({
      email: form.value.email,
      recoveryCode: form.value.recoveryCode,
      newPassword: form.value.newPassword,
      confirmPassword: form.value.confirmPassword
    })
    
    // 处理API响应
    handleApiError(response)
    
    // 重置密码成功
    ElMessage.success('密码重置成功！请使用新密码登录')
    
    // 跳转到登录页面
    router.push('/auth/login')
    
  } catch (error) {
    console.error('密码重置失败:', error)
    ElMessage.error(error.message || '密码重置失败，请检查恢复码是否正确')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>


</style> 