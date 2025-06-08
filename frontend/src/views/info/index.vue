<template>
  <div>
    <div class="info-header">
        <div class="info-header-back email-link" @click="handleBack">
          <el-icon><ArrowLeft /></el-icon>
          返回邮箱
        </div>
    </div>
    <div class="info-content">
      <div class="info-content-left">
        <div class="info-content-left-title">个人资料卡</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ArrowLeft } from '@element-plus/icons-vue';
const router = useRouter()
const handleBack = () => {
  router.go(-1)
}
</script>

<style lang="scss" scoped>
  .info-header{
    margin-bottom: 16px;
    .info-header-back{
      display: flex;
      align-items: center;
      font-size: 12px;
    }
  }
</style>