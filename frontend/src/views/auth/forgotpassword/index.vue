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
  <RecoveryCode ref="recoveryCodeDialog" :recovery-code="form.newRecoveryCode" :title="'密码重置成功'" />
</template>

<script setup>
import LoginForm from '../components/loginForm.vue'
import RecoveryCode from '../components/recoveryCode.vue'
import { resetPassword, handleApiError, loginUser } from '@/utils/api'
import { useUserStore } from '@/stores/user'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const form = ref({
  email: '',
  recoveryCode: '',
  newPassword: '',
  confirmPassword: '',
  newRecoveryCode: '', // 用于存储新生成的恢复码
})

const recoveryCodeDialog = ref(null)

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
const userStore = useUserStore()

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
    
    // 检查后端是否返回了新的恢复码（注意字段名是 newRecoveryCode）
    if (response.data && response.data.newRecoveryCode) {
      form.value.newRecoveryCode = response.data.newRecoveryCode
      ElMessage.success('密码重置成功！请保存您的新恢复码')
      
      // 显示恢复码对话框
      await recoveryCodeDialog.value.open()
      
      // 自动登录
      try {
        const loginResponse = await loginUser({
          email: form.value.email,
          password: form.value.newPassword
        })
        handleApiError(loginResponse)
        
        userStore.setUserInfo(loginResponse.data)
        ElMessage.success('登录成功')
        
        // 跳转到主页面
        router.push('/main')
      } catch (loginError) {
        console.error('自动登录失败:', loginError)
        ElMessage.warning('密码重置成功，但自动登录失败，请手动登录')
        // 跳转到登录页面
        router.push('/auth/login')
      }
    } else {
      // 如果后端没有返回恢复码，报错
      console.error('密码重置成功但后端未返回新恢复码:', response)
      ElMessage.error('密码重置成功，但获取新恢复码失败，请联系管理员')
      // 跳转到登录页面
      router.push('/auth/login')
    }
    
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