<template>
  <div class="info-container">
    <div class="info-header">
      <div class="info-header-back email-link" @click="handleBack">
        <el-icon><ArrowLeft /></el-icon>
        返回邮箱
      </div>
    </div>
    
    <div class="info-content">
      <div class="info-content-left">
        <div class="info-content-left-title">个人资料卡</div>
        
        <!-- 基本信息区块 -->
        <div class="info-section">
          <div class="section-title">基本信息</div>
          
          <div class="form-item-row">
            <span class="form-label required">用户名</span>
            <div class="form-item-content">
              <el-input 
                v-model="userForm.username" 
                placeholder="当前用户名"
                class="form-input"
              />
              <div class="edit-actions">
                <el-button 
                  type="text" 
                  @click="cancelUsername"
                  class="action-btn"
                >
                  取消
                </el-button>
                <el-button 
                  type="primary" 
                  size="medium"
                  @click="saveUsername"
                  :loading="loading.username"
                  class="action-btn primary"
                >
                  保存
                </el-button>
              </div>
            </div>
          </div>
          
          <div class="form-item-row">
            <span class="form-label required">手机号</span>
            <div class="form-item-content">
              <el-input 
                v-model="userForm.phone" 
                placeholder="138****8888"
                class="form-input"
              />
              <div class="edit-actions">
                <el-button 
                  type="text" 
                  @click="cancelPhone"
                  class="action-btn"
                >
                  取消
                </el-button>
                <el-button 
                  type="primary" 
                  size="medium"
                  @click="savePhone"
                  :loading="loading.phone"
                  class="action-btn primary"
                >
                  保存
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 安全设置区块 -->
        <div class="info-section">
          <div class="section-title">安全设置</div>
          
          <div class="form-item-row">
            <span class="form-label required">旧密码</span>
            <div class="form-item-content">
              <el-input 
                type="password" 
                v-model="passwordForm.oldPassword" 
                placeholder="请输入当前密码"
                show-password
                class="form-input"
              />
            </div>
          </div>
          
          <div class="form-item-row">
            <span class="form-label required">新密码</span>
            <div class="form-item-content">
              <el-input 
                type="password" 
                v-model="passwordForm.newPassword" 
                placeholder="请输入新密码"
                show-password
                class="form-input"
                @blur="validatePasswordFromAPI"
              />
            </div>
          </div>
          
          <div class="form-item-row">
            <span class="form-label required">确认密码</span>
            <div class="form-item-content">
              <el-input 
                type="password" 
                v-model="passwordForm.confirmPassword" 
                placeholder="请再次输入新密码"
                show-password
                class="form-input"
              />
            </div>
          </div>
          
          <div class="password-actions">
            <el-button 
              type="text" 
              @click="resetPasswordForm"
              class="action-btn"
            >
              取消
            </el-button>
            <el-button 
              type="primary" 
              @click="savePassword"
              :loading="loading.password"
              class="action-btn primary"
            >
              保存
            </el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { updateUsername, updatePhone, updatePassword, handleApiError } from '@/utils/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// 表单数据
const userForm = reactive({
  username: '', 
  phone: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 移除编辑模式控制，所有输入框都是可编辑的

// 加载状态
const loading = reactive({
  username: false,
  phone: false,
  password: false
})

// 移除表单引用，使用简单验证

// 移除表单验证规则，使用简单验证

// 原始数据备份（用于取消编辑时恢复）
const originalData = reactive({
  username: '',
  phone: ''
})

// 初始化用户数据
onMounted(() => {
  if (userStore.userInfo.username) {
    userForm.username = userStore.userInfo.username
    userForm.phone = userStore.userInfo.phone
    originalData.username = userStore.userInfo.username
    originalData.phone = userStore.userInfo.phone
  }
})

const handleBack = () => {
  router.go(-1)
}

// 移除切换编辑模式的方法

// 保存用户名
const saveUsername = async () => {
  try {
    // 简单验证
    if (!userForm.username || userForm.username.trim() === '') {
      ElMessage.error('用户名不能为空')
      return
    }
    
    if (userForm.username.length < 2 || userForm.username.length > 20) {
      ElMessage.error('用户名长度在2到20个字符')
      return
    }
    
    // 弹窗要求输入密码
    const { value: password } = await ElMessageBox.prompt('请输入当前密码以确认修改', '身份验证', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputType: 'password',
      inputValidator: (value) => {
        if (!value) {
          return '密码不能为空'
        }
        return true
      }
    })
    
    loading.username = true
    
    // 调用API接口保存用户名
    const response = await updateUsername({
      email: userStore.userInfo.email,
      password: password,
      oldUsername: originalData.username,
      newUsername: userForm.username
    })
    
    // 处理API响应
    handleApiError(response)
    
    // 更新本地状态
    userStore.updateUserName(userForm.username)
    originalData.username = userForm.username
    ElMessage.success('用户名修改成功')
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('用户名修改失败:', error)
      ElMessage.error(error.message || '用户名修改失败')
    }
  } finally {
    loading.username = false
  }
}

// 保存手机号
const savePhone = async () => {
  try {
    // 简单验证
    if (!userForm.phone || userForm.phone.trim() === '') {
      ElMessage.error('手机号不能为空')
      return
    }
    
    const phoneRegex = /^1[3-9]\d{9}$/
    if (!phoneRegex.test(userForm.phone)) {
      ElMessage.error('请输入正确的手机号')
      return
    }
    
    // 弹窗要求输入密码
    const { value: password } = await ElMessageBox.prompt('请输入当前密码以确认修改', '身份验证', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      inputType: 'password',
      inputValidator: (value) => {
        if (!value) {
          return '密码不能为空'
        }
        return true
      }
    })
    
    loading.phone = true
    
    // 调用API接口保存手机号
    const response = await updatePhone({
      email: userStore.userInfo.email,
      password: password,
      oldPhone: originalData.phone,
      newPhone: userForm.phone
    })
    
    // 处理API响应
    handleApiError(response)
    
    // 更新本地状态
    userStore.updateUserPhone(userForm.phone)
    originalData.phone = userForm.phone
    ElMessage.success('手机号修改成功')
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('手机号修改失败:', error)
      ElMessage.error(error.message || '手机号修改失败')
    }
  } finally {
    loading.phone = false
  }
}

// 保存密码
const savePassword = async () => {
  try {
    // 简单验证
    if (!passwordForm.oldPassword || passwordForm.oldPassword.trim() === '') {
      ElMessage.error('请输入当前密码')
      return
    }
    
    if (!passwordForm.newPassword || passwordForm.newPassword.trim() === '') {
      ElMessage.error('请输入新密码')
      return
    }
    
    if (passwordForm.newPassword.length < 6 || passwordForm.newPassword.length > 20) {
      ElMessage.error('密码长度在6到20个字符')
      return
    }
    
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      ElMessage.error('两次输入的密码不一致')
      return
    }
    
    loading.password = true
    
    // 调用API接口修改密码
    const response = await updatePassword({
      email: userStore.userInfo.email,
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    
    // 处理API响应
    handleApiError(response)
    
    // 清空表单
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    ElMessage.success('密码修改成功')
    
  } catch (error) {
    console.error('密码修改失败:', error)
    ElMessage.error(error.message || '密码修改失败')
  } finally {
    loading.password = false
  }
}

// 重置密码表单
const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

// 取消用户名修改
const cancelUsername = () => {
  userForm.username = originalData.username
}

// 取消手机号修改
const cancelPhone = () => {
  userForm.phone = originalData.phone
}

// 验证密码合法性（调用后端API）
const validatePasswordFromAPI = async () => {
  if (!passwordForm.newPassword) return
  
  try {
    // TODO: 调用后端API验证密码合法性
    // const response = await validatePassword({ password: passwordForm.newPassword })
    // 
    // 示例API调用结构：
    // if (!response.data.isValid) {
    //   ElMessage.error(response.data.message || '密码不符合要求')
    //   return false
    // }
    
    // 模拟API调用
    console.log('验证密码:', passwordForm.newPassword)
    
    // 这里可以添加更多的密码复杂度检查
    const passwordRegex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{6,20}$/
    if (!passwordRegex.test(passwordForm.newPassword)) {
      ElMessage.error('密码必须包含大写字母、小写字母和数字，长度6-20位')
      return false
    }
    
    return true
  } catch (error) {
    console.error('密码验证API调用失败:', error)
    ElMessage.error('密码验证失败，请重试')
    return false
  }
}
</script>

<style lang="scss" scoped>
.info-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  background: #fff;
  min-height: 100vh;
}

.info-header {
  margin-bottom: 24px;
  
  .info-header-back {
    display: flex;
    align-items: center;
    font-size: 16px;
    color: #409eff;
    cursor: pointer;
    
    &:hover {
      color: #66b1ff;
    }
    
    .el-icon {
      margin-right: 4px;
    }
  }
}

.info-content {
  .info-content-left {
    .info-content-left-title {
      font-size: 28px;
      font-weight: bold;
      color: #303133;
      margin-bottom: 32px;
      text-align: center;
    }
  }
}

.info-section {
  background: #fff;
  border-radius: 8px;
  padding: 32px;
  margin-bottom: 24px;
  box-shadow: 0 2px 8px 0 rgba(0, 0, 0, 0.1);
  
  .section-title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 32px;
    text-align: left;
  }
}

.form-item-row {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.form-label {
  min-width: 80px;
  font-size: 15.5px;
  color: #606266;
  font-weight: 500;
  margin-right: 16px;
  
  &.required::before {
    content: '*';
    color: #f56c6c;
    margin-right: 4px;
  }
}

.form-item-content {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  
  .form-input {
    flex: 1;
    max-width: 300px;
  }
  
  .action-btn {
    padding: 8px 16px;
    font-size: 14px;
    border-radius: 4px;
    border: 1px solid #dcdfe6;
    background: #fff;
    color: #606266;
    cursor: pointer;
    transition: all 0.3s;
    width: 56px;
    text-align: center;
    
    &:hover:not(:disabled) {
      color: #409eff;
      border-color: #c6e2ff;
      background-color: #ecf5ff;
    }
    
    &:disabled {
      cursor: not-allowed;
      opacity: 0.5;
      background: #f5f7fa;
      border-color: #e4e7ed;
      color: #c0c4cc;
    }
    
    &.primary {
      background: #409eff;
      border-color: #409eff;
      color: #fff;
      
      &:hover:not(:disabled) {
        background: #66b1ff;
        border-color: #66b1ff;
      }
      
      &:disabled {
        background: #a0cfff;
        border-color: #a0cfff;
        color: #fff;
      }
    }
  }
}

.edit-actions {
  display: flex;
  gap: 0px;
  justify-content: flex-start;
  width: 120px;
}

.password-actions {
  display: flex;
  justify-content: flex-start;
  gap: 0px;
  margin-top: 32px;
  margin-left: 96px;
  width: 120px;
  
  .action-btn {
    padding: 8px 16px;
    font-size: 14px;
    border-radius: 4px;
    border: 1px solid #dcdfe6;
    background: #fff;
    color: #606266;
    cursor: pointer;
    transition: all 0.3s;
    width: 56px;
    text-align: center;
    
    &:hover:not(:disabled) {
      color: #409eff;
      border-color: #c6e2ff;
      background-color: #ecf5ff;
    }
    
    &:disabled {
      cursor: not-allowed;
      opacity: 0.5;
      background: #f5f7fa;
      border-color: #e4e7ed;
      color: #c0c4cc;
    }
    
    &.primary {
      background: #409eff;
      border-color: #409eff;
      color: #fff;
      
      &:hover:not(:disabled) {
        background: #66b1ff;
        border-color: #66b1ff;
      }
      
      &:disabled {
        background: #a0cfff;
        border-color: #a0cfff;
        color: #fff;
      }
    }
  }
}

:deep(.el-input) {
  .el-input__wrapper {
    border-radius: 4px;
    box-shadow: 0 0 0 1px #dcdfe6 inset;
    
    &:hover {
      box-shadow: 0 0 0 1px #c0c4cc inset;
    }
    
    &.is-focus {
      box-shadow: 0 0 0 1px #409eff inset;
    }
  }
}

:deep(.el-button) {
  border-radius: 4px;
  font-weight: 500;
  
  &.el-button--text {
    border: 1px solid #dcdfe6;
    background: #fff;
    color: #606266;
    padding: 8px 16px;
    
    &:hover {
      color: #409eff;
      border-color: #c6e2ff;
      background-color: #ecf5ff;
    }
  }
  
  &.el-button--primary {
    background: #409eff;
    border-color: #409eff;
    
    &:hover {
      background: #66b1ff;
      border-color: #66b1ff;
    }
  }
}
</style>