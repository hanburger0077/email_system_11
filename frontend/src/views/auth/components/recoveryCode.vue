<template>
  <div>
    <el-dialog v-model="dialogVisible" :title="title || '注册成功'" :show-close="false">
      <div class="recovery-code-container">
        <div class="recovery-code-text">
          恢复码用于找回密码，请妥善保管，不要泄露给他人 <br>
          <div class="recovery-code-text-code">
            {{ recoveryCode || 'error!' }}
            <el-icon @click="copyRecoveryCode" class="recovery-code-text-code-icon"><CopyDocument /></el-icon>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button  class="email-button" @click="handleConfirm">确定</el-button>
      </template>
    </el-dialog>  
  </div>  
  </template>
  
  <script setup>
  import { ref } from 'vue'
  import { ElMessage, ElDialog } from 'element-plus'
  import { CopyDocument } from '@element-plus/icons-vue'
  
  const props = defineProps({
    title: {
      type: String,
      default: ''
    },
    recoveryCode: {
      type: String,
      default: ''
    },
  })
  
  const dialogVisible = ref(false)
  const copyRecoveryCode = () => {
    navigator.clipboard.writeText(props.recoveryCode)
    ElMessage.success('复制成功')
  }
  
  let _resolve = null
  let _reject = null
  const open = () => {
    dialogVisible.value = true
    // 点击确定后，关闭对话框
    return new Promise((resolve, reject) => {
      _resolve = resolve
      _reject = reject
    }
  )
  }
  const handleConfirm = () => {
    dialogVisible.value = false
    _resolve()
  }
  
  defineExpose({
    open
  })
  </script>
  
  <style lang="scss" scoped>
  .recovery-code-text-code{
    display: flex;
    align-items: center;
    justify-content: center;
    margin-top: 10px;
    font-size: 16px;
    font-weight: bold;
    color: #333;
    background-color: #f0f0f0;
    padding: 10px;
    border-radius: 5px;
    .recovery-code-text-code-icon{
      margin-left: 10px;
      cursor: pointer;
    }
  }
  </style>