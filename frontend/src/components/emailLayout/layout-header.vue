<template>
  <div class="header-container">
      <img src="@/assets/logo.jpg" alt="华南理工大学" class="logo" />
      <el-input placeholder="邮箱搜索" style="width: 240px"  class="search-input" />
      <el-button class="account-btn">账号中心</el-button>
  </div>
</template>

<script setup>
import { menuList } from './menuList';
import layoutSidebar from './layout-sidebar.vue';
import { ref } from 'vue';

</script>

<style lang="scss" scoped>
.header-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 48px;
  background: #fff;
  border-bottom: 2px solid #cce2fa;
  display: flex;
  align-items: center;
  z-index: 10;
  padding: 0 48px;
  .logo {
    height: 32px;
    margin-right: 16px;
  }
}


</style>