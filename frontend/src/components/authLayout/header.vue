<template>
  <div class="layout-header" >
    <div class="layout-header-inner">
      <div class="brand-section">
        <img
          src="@/assets/logo.jpg"
          title="华南理工大学"
          class="layout-header-logo"
        />
        <div class="brand-name">
          <span class="brand-flow">Flow</span>
          <span class="brand-mail">mail</span>
        </div>
      </div>
      <!-- <div class="right-title">十一组邮箱</div> -->
    </div>
  </div>
</template>

<script setup>
</script>

<style lang="scss" scoped>
.layout-header {
  height: 64px;
  display: flex;
  background-color: #fff;
  justify-content: flex-start;
  //border-bottom: 1px solid #f0f0f0;
  
  &-inner {
    height: 100%;
    padding: 0 32px;
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  
  &-logo {
    display: block;
    height: 40px;
    margin-right: 16px;
  }
  
  .brand-section {
    display: flex;
    align-items: center;
  }
  
  .brand-name {
    display: flex;
    align-items: baseline;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
    
    .brand-flow {
      font-size: 28px;
      font-weight: 300;
      color: #2c3e50;
      letter-spacing: -0.5px;
    }
    
    .brand-mail {
      font-size: 28px;
      font-weight: 600;
      color: #409eff;
      letter-spacing: -0.5px;
      margin-left: 2px;
    }
  }
  
  .right-title {
    font-size: 20px;
    font-weight: bold;
    color: #aaa;
  }
}

@media (max-width: 768px) {
  .layout-header {
    &-inner {
      padding: 0 16px;
    }
    
    &-logo {
      height: 36px;
      margin-right: 12px;
    }
    
    .brand-name {
      .brand-flow,
      .brand-mail {
        font-size: 24px;
      }
    }
  }
}
</style>
