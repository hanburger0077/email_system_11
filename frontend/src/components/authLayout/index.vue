<template>
  <div class="layout-wrapper">
    <layout-header class="layout-header"></layout-header>
    <layout-content class="layout-content">
      <slot />
    </layout-content>
    <layout-footer class="layout-footer"></layout-footer>
  </div>
</template>

<script setup>
import layoutHeader from './header.vue';
import layoutFooter from './footer.vue';
import layoutContent from './content.vue';

</script>

<style lang="scss" scoped>
.layout {
  &-wrapper {
    display: flex;
    flex-direction: column;
    min-height: 100%;
    overflow: visible;
  }
  &-header {
    height: 68px;
    position: fixed;
    z-index: 1;
    top: 0;
    right: 0;
    left: 0;
  }
  &-content {
    padding: 64px 0;
    flex: 1;
  }
  &-footer {
    height: 64px;
    position: fixed;
    z-index: 1;
    bottom: 0;
    right: 0;
    left: 0;
  }
}
@include screen-mobile {
  .layout {
    &-footer {
      display: none;
    }
    &-content {
      padding-top: 100px;
      padding-bottom: 0;
      min-height: auto;
    }
  }
}
</style>
