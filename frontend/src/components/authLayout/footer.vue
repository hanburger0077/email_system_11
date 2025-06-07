<template>
  <div class="layout-footer">
    <div class="layout-footer-inner">
     <!-- （可选：） 组员信息：韩启慧 -->
    </div>
  </div>
</template>

<script setup>
</script>

<style lang="scss" scoped>
.layout-footer {
  padding: 12px 0;
  min-width: 1180px;
  display: flex;
  background-color: #fff;
  justify-content: center;
  align-items: center;
  &-inner {
    max-width: 1180px;
    flex: 1;
    display: flex;
    justify-content: space-between;
    align-items: start;
  }
}
@include screen-mobile {
  .layout-footer {
    display: none;
  }
}
</style>
