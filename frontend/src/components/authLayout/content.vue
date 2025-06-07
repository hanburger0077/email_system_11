<template>
  <div 
    class="layout-content" 
  >
    <div class="layout">
      <div class="layout-left">
        <img src="@/assets/auth-left-bg.png" alt="背景图片" class="bg-image" />
      </div>
      <div class="layout-right">
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script setup>
</script>

<style lang="scss" scoped>
.layout-content {
  display: flex;
  justify-content: space-between;
  width: 100%;
  min-width: 1180px;

  .layout {
    display: flex;
    width: 100vw;
    justify-content: center;
    align-items: center;
    background-color: #fff;
    padding: 40px;
    gap: 60px;
    
    &-left {
      width: 700px;
      min-width: 700px;
      min-height: 480px;
      border-radius: 20px;
      overflow: hidden;
      
      .bg-image{
        width: 100%;
        height: 100%;
        object-fit: cover;
      }
    }
    
    &-right {
      width: 420px;
      min-width: 420px;
      min-height: 480px;
      border-radius: 20px;
      background: #fff;
      box-shadow: 0px 5px 15px 1px rgba(0, 0, 0, 0.05);
      display: flex;
      flex-direction: column;
      padding: 24px;
            
      :deep() & > * {
        flex: 1;
      }
    }
  }
}

@include screen-mobile {
  .layout-content {
    min-width: 0;
    height: auto;
    background-color: #fff;
    
    .layout {
      flex-direction: column;
      background-color: #fff;
      padding: 20px;
      gap: 20px;
      
      &-left {
        display: none;
      }
      
      &-right {
        width: 100%;
        min-width: 0;
      }
    }
  }
}
</style>
