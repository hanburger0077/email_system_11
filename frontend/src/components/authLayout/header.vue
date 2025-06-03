<template>
  <div class="layout-header" >
    <div class="layout-header-inner">
      <img
        src="@/assets/logo.jpg"
        title="华南理工大学"
        class="layout-header-logo"
      />
      <div class="right-title">十一组邮箱</div>
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
  &-inner {
    height: 100%;
    padding: 0 24px;
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  &-logo {
    display: block;
    height: 50px;
  }
  .right-title {
    font-size: 20px;
    font-weight: bold;
    color: #aaa;
  }
}
</style>
