<template>
  <div class="header-container">
    <div class="brand-section">
      <img src="@/assets/logo.jpg" alt="华南理工大学" class="logo" />
      <div class="brand-name">
        <span class="brand-flow">Flow</span>
        <span class="brand-mail">mail</span>
      </div>
    </div>
    <div class="header-content">
      <el-input placeholder="邮箱搜索" class="search-input" />
      <div class="header-right" @mouseleave="handleAccountMouseLeave">
        <div class="account-circle" @click="handleAccountClick" @mouseenter="handleAccountMouseEnter">
          <span>账号</span>
          <DropDown v-show="isMenuVisible" class="account-dropdown" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import DropDown from './drop-down.vue';
const isMenuVisible = ref(false)
const handleAccountClick = () => {
  console.log('账号按钮点击');
};

const timer = ref(null)

const handleAccountMouseEnter = () => {
  console.log('账号按钮鼠标进入');
  isMenuVisible.value = true
  clearTimeout(timer.value)
};

const handleAccountMouseLeave = () => {
  console.log('账号按钮鼠标离开');
  clearTimeout(timer.value)
  timer.value = setTimeout(() => {
    isMenuVisible.value = false
  }, 300)
};
</script>

<style lang="scss" scoped>
.header-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 68px;
  background: #fff;
  border-bottom: 2px solid #cce2fa;
  display: flex;
  align-items: center;
  z-index: 100;
  padding: 0 24px;
}

.brand-section {
  display: flex;
  align-items: center;
  margin-right: 16px;
  
  .logo {
    height: 40px;
    margin-right: 12px;
  }
  
  .brand-name {
    display: flex;
    align-items: baseline;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
    
    .brand-flow {
      font-size: 24px;
      font-weight: 300;
      color: #2c3e50;
      letter-spacing: -0.5px;
    }
    
    .brand-mail {
      font-size: 24px;
      font-weight: 600;
      color: #409eff;
      letter-spacing: -0.5px;
      margin-left: 2px;
    }
  }
}

.header-content {
  flex: 1;
  display: flex;
  align-items: center;
  max-width: calc(100vw - 200px); /* 为侧边栏留出空间 */
  margin-left: 10px; /* 与侧边栏宽度对齐 */
}

.search-input {
  width: 320px;
  max-width: 600px;
  height: 36px;
  margin-right: auto; /* 推向左边 */
}

.header-right {
  margin-left: auto;
  margin-right: 50px; /* 与右边保持24px距离 */
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

  user-select: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative; /* 确保定位正确 */
  
  &:hover {
    background-color: #1a5f9e;
  }

  // &:active {
  //   transform: scale(0.95);
  // }
  
  span {
    display: block; /* 确保文本显示 */
    line-height: 1; /* 设置行高 */
  }
  .account-dropdown{
    position: absolute;
    top: 36px;
    right: 0px;
    cursor: auto;
  }
}

@media (max-width: 768px) {
  .brand-section {
    .logo {
      height: 36px;
      margin-right: 8px;
    }
    
    .brand-name {
      .brand-flow,
      .brand-mail {
        font-size: 20px;
      }
    }
  }
}
</style>