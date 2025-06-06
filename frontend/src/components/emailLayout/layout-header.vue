<template>
  <div class="header-container">
    <img src="@/assets/logo.jpg" alt="华南理工大学" class="logo" />
    <div class="header-spacer"></div>
    <el-input placeholder="邮箱搜索" class="search-input" />
    <div class="header-spacer"></div>
    <div class="account-circle" @click="handleAccountClick">
      <span>账号</span>
    </div>
  </div>
</template>

<script setup>
const handleAccountClick = () => {
  console.log('账号按钮点击');
};
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
  z-index: 100; /* 提高z-index确保在其他元素上方 */
  padding: 0 24px;

  .logo {
    height: 32px;
    margin-right: 16px;
  }
}

.header-spacer {
  flex: 1;
}

.search-input {
  width: 320px;
  max-width: 600px;
}

.account-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #1f74c0;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.1s;
  margin-left: 16px;
  user-select: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative; /* 确保定位正确 */
  
  &:hover {
    background-color: #1a5f9e;
  }

  &:active {
    transform: scale(0.95);
  }
  
  span {
    display: block; /* 确保文本显示 */
    line-height: 1; /* 设置行高 */
  }
}
</style>