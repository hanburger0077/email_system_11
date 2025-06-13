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
          <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="80px">
            <el-form-item label="用户名" prop="username">
              <div class="form-item-content">
                <el-input 
                  v-model="userForm.username" 
                  :disabled="!editMode.username"
                  placeholder="请输入用户名"
                />
                <el-button 
                  v-if="!editMode.username"
                  type="text" 
                  @click="toggleEdit('username')"
                  class="edit-btn"
                >
                  修改
                </el-button>
                <div v-else class="edit-actions">
                  <el-button 
                    type="text" 
                    @click="toggleEdit('username')"
                    class="cancel-btn"
                  >
                    取消
                  </el-button>
                  <el-button 
                    type="primary" 
                    size="small"
                    @click="saveUsername"
                    :loading="loading.username"
                  >
                    保存
                  </el-button>
                </div>
              </div>
            </el-form-item>
            
            <el-form-item label="手机号" prop="phone">
              <div class="form-item-content">
                <el-input 
                  v-model="userForm.phone" 
                  :disabled="!editMode.phone"
                  placeholder="请输入手机号"
                />
                <el-button 
                  v-if="!editMode.phone"
                  type="text" 
                  @click="toggleEdit('phone')"
                  class="edit-btn"
                >
                  {{ editMode.phone ? '取消' : '修改' }}
                </el-button>
                <div v-else class="edit-actions">
                  <el-button 
                    type="text" 
                    @click="toggleEdit('phone')"
                    class="cancel-btn"
                  >
                    取消
                  </el-button>
                  <el-button 
                    type="primary" 
                    size="small"
                    @click="savePhone"
                    :loading="loading.phone"
                  >
                    保存
                  </el-button>
                </div>
              </div>
            </el-form-item>
          </el-form>
        </div>

        <!-- 安全设置区块 -->
        <div class="info-section">
          <div class="section-title">安全设置</div>
          <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="80px">
            <el-form-item label="旧密码" prop="oldPassword" v-if="editMode.password">
              <el-input 
                type="password" 
                v-model="passwordForm.oldPassword" 
                placeholder="请输入当前密码"
                show-password
              />
            </el-form-item>
            
            <el-form-item label="新密码" prop="newPassword" v-if="editMode.password">
              <el-input 
                type="password" 
                v-model="passwordForm.newPassword" 
                placeholder="请输入新密码"
                show-password
                @blur="validatePasswordFromAPI"
              />
            </el-form-item>
            
            <el-form-item label="确认密码" prop="confirmPassword" v-if="editMode.password">
              <el-input 
                type="password" 
                v-model="passwordForm.confirmPassword" 
                placeholder="请再次输入新密码"
                show-password
              />
            </el-form-item>
            
            <el-form-item label="登录密码">
              <div class="form-item-content">
                <el-input 
                  type="password" 
                  value="********" 
                  disabled
                  v-if="!editMode.password"
                />
                <el-button 
                  v-if="!editMode.password"
                  type="text" 
                  @click="toggleEdit('password')"
                  class="edit-btn"
                >
                  修改密码
                </el-button>
                <div v-else class="edit-actions">
                  <el-button 
                    type="text" 
                    @click="toggleEdit('password')"
                    class="cancel-btn"
                  >
                    取消
                  </el-button>
                  <el-button 
                    type="primary" 
                    size="small"
                    @click="savePassword"
                    :loading="loading.password"
                  >
                    保存
                  </el-button>
                </div>
              </div>
            </el-form-item>
          </el-form>
        </div>

        
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

// 表单数据
const userForm = reactive({
  username: '当前用户名', // 这里后续从API获取
  phone: '138****8888'    // 这里后续从API获取
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 编辑模式控制
const editMode = reactive({
  username: false,
  phone: false,
  password: false
})

// 加载状态
const loading = reactive({
  username: false,
  phone: false,
  password: false
})

// 表单引用
const userFormRef = ref()
const passwordFormRef = ref()

// 表单验证规则
const userRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在2到20个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ]
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 原始数据备份（用于取消编辑时恢复）
const originalData = {
  username: userForm.username,
  phone: userForm.phone
}

const handleBack = () => {
  router.go(-1)
}

// 切换编辑模式
const toggleEdit = (field) => {
  if (editMode[field]) {
    // 取消编辑，恢复原始数据
    if (field === 'username') {
      userForm.username = originalData.username
    } else if (field === 'phone') {
      userForm.phone = originalData.phone
    } else if (field === 'password') {
      // 清空密码表单
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
    }
  } else {
    // 开始编辑，备份当前数据
    if (field === 'username') {
      originalData.username = userForm.username
    } else if (field === 'phone') {
      originalData.phone = userForm.phone
    }
  }
  editMode[field] = !editMode[field]
}

// 保存用户名
const saveUsername = async () => {
  try {
    await userFormRef.value.validateField('username')
    loading.username = true
    
    // TODO: 调用API接口保存用户名
    // await updateUsername({ username: userForm.username })
    
    // 模拟API调用
    setTimeout(() => {
      loading.username = false
      editMode.username = false
      originalData.username = userForm.username
      ElMessage.success('用户名修改成功')
    }, 1000)
  } catch (error) {
    console.error('用户名验证失败:', error)
  }
}

// 保存手机号
const savePhone = async () => {
  try {
    await userFormRef.value.validateField('phone')
    loading.phone = true
    
    // TODO: 调用API接口保存手机号
    // await updatePhone({ phone: userForm.phone })
    
    // 模拟API调用
    setTimeout(() => {
      loading.phone = false
      editMode.phone = false
      originalData.phone = userForm.phone
      ElMessage.success('手机号修改成功')
    }, 1000)
  } catch (error) {
    console.error('手机号验证失败:', error)
  }
}

// 保存密码
const savePassword = async () => {
  try {
    await passwordFormRef.value.validate()
    loading.password = true
    
    // TODO: 调用API接口修改密码
    // await updatePassword({
    //   oldPassword: passwordForm.oldPassword,
    //   newPassword: passwordForm.newPassword
    // })
    
    // 模拟API调用
    setTimeout(() => {
      loading.password = false
      editMode.password = false
      // 清空表单
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''
      ElMessage.success('密码修改成功')
    }, 1000)
  } catch (error) {
    console.error('密码验证失败:', error)
  }
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
}

.info-header {
  margin-bottom: 24px;
  
  .info-header-back {
    display: flex;
    align-items: center;
    font-size: 14px;
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
      font-size: 24px;
      font-weight: bold;
      color: #303133;
      margin-bottom: 32px;
    }
  }
}

.info-section {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 24px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  
  .section-title {
    font-size: 18px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 2px solid #f0f0f0;
  }
}

.form-item-content {
  display: flex;
  align-items: center;
  gap: 12px;
  
  .el-input {
    flex: 1;
  }
  
  .edit-btn {
    color: #409eff;
    padding: 0;
    min-width: auto;
    
    &:hover {
      color: #66b1ff;
    }
  }
}

.contact-info {
  .contact-item {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
    
    &:last-child {
      margin-bottom: 0;
    }
    
    .contact-label {
      color: #606266;
      min-width: 80px;
    }
    
    .contact-value {
      color: #303133;
      font-weight: 500;
    }
  }
}

:deep(.el-form-item) {
  margin-bottom: 24px;
  
  .el-form-item__label {
    color: #606266;
    font-weight: 500;
  }
}

:deep(.el-button--small) {
  padding: 5px 15px;
}
</style>