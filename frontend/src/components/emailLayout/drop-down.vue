<template>
  <div class="dropdown-container">
    <div class="dropdown-content">
      <div class="dropdown-content-left">
        <div class="dropdown-icon"></div>
      </div>
      <div class="dropdown-content-right">
        <div class="name">{{ userName || '张三' }}</div>
        <div class="info-item" v-for="item in userInfo" :key="item.id">
          {{ item.text }}：{{ item.value }}
        </div>
      </div>
    </div>

    <div class="dropdown-footer">
      <div class="email-link" @click="handleInfo">个人资料卡</div>
      <div class="email-link" @click="handleLogout">退出登录</div>
      <div class="email-link" @click="handleCancel">账号注销</div>
    </div>
  </div>
</template>

<script setup>
import { exitUser, logoutUser, handleApiError } from '@/utils/api'
import { useUserStore } from '@/stores/user'
import { ElMessageBox } from 'element-plus'

const isMenuVisible = ref(false)
const router = useRouter()
const userStore = useUserStore()

const userInfo = computed(() => [
  {
    text: '用户名',
    value: userStore.userInfo.username || '未设置'
  },
  {
    text: '邮箱',
    value: userStore.userInfo.email || '未设置'
  }
])

const userName = computed(() => userStore.userInfo.username || '用户')

const handleInfo = () => {
  router.push('/info')
}

const handleLogout = async () => {
  try {
    // 调用退出登录API
    const response = await exitUser()
    
    // 处理API响应
    handleApiError(response)
    
    // 清空本地用户信息
    userStore.clearUserInfo()
    
    ElMessage.success('退出登录成功')
    
    // 跳转到登录页
    router.push('/auth/login')
    
  } catch (error) {
    console.error('退出登录失败:', error)
    ElMessage.error(error.message || '退出登录失败')
  }
}

const handleCancel = async () => {
  try {
    // 二次确认
    await ElMessageBox.confirm(
      '账号注销后将永久删除您的所有数据，此操作不可恢复。确定要注销账号吗？',
      '账号注销确认',
      {
        confirmButtonText: '确定注销',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )
    
    // 要求输入密码确认
    const { value: password } = await ElMessageBox.prompt('请输入当前密码以确认注销', '身份验证', {
      confirmButtonText: '确认注销',
      cancelButtonText: '取消',
      inputType: 'password',
      inputValidator: (value) => {
        if (!value) {
          return '密码不能为空'
        }
        return true
      }
    })
    
    // 调用注销API
    const response = await logoutUser({
      email: userStore.userInfo.email,
      password: password
    })
    
    // 处理API响应
    handleApiError(response)
    
    // 清空本地用户信息
    userStore.clearUserInfo()
    
    ElMessage.success('账号注销成功')
    
    // 跳转到登录页
    router.push('/auth/login')
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('账号注销失败:', error)
      ElMessage.error(error.message || '账号注销失败')
    }
  }
}
</script>

<style lang="scss" scoped>
.dropdown-container{
  box-shadow: 0 4px 16px #00000029;
  font-size: 12px;
  color: #333;
  line-height: 20px;
  width: 300px;
  background-color: #fff;
  border-radius: 4px;
  padding: 10px 16px;

  color: #333;
  font-weight: 400;

  .dropdown-content{
    display: flex;
    align-items: center;
    .dropdown-icon{
      width: 90px;
      height: 90px;
      margin: 20px;
      margin-left: 0px;
      background-image: url("data:image/svg+xml;charset=utf-8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 64 64' stroke='%23707070'%3E%3Cg class='mectrl_stroke' fill='none'%3E%3Ccircle cx='32' cy='32' r='30.25' stroke-width='1.5'/%3E%3Cg transform='matrix(.9 0 0 .9 10.431 10.431)' stroke-width='2'%3E%3Ccircle cx='24.25' cy='18' r='9'/%3E%3Cpath d='M11.2 40a1 1 0 1126.1 0'/%3E%3C/g%3E%3C/g%3E%3C/svg%3E")
    }

    .name{
      font-size: 18px;
      font-weight: 600;
      color: #333;
      margin-bottom: 10px;
    }
  }
  .dropdown-footer{
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 20px;
    font-size: 12px;
  }
}
</style>