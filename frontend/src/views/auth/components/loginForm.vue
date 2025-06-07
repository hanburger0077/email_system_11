<template>
  <div class="login-form">
    <div class="login-form-title">
      {{ title }}
    </div>
    <div class="login-form-content">
      <slot/>
    </div>
    
    <div class="login-form-footer">
      <div v-if="type === 'login'">
        没有账号？
        <span class="email-link" @click="clickHandle('register')">免费注册</span>
      </div>
      <div v-else>
        已有账号？
        <span class="email-link" @click="clickHandle('login')">立即登录</span>
      </div>
    </div>
  </div>
</template>

<script setup>

const props = defineProps({
  type: {
    type: String,
    default: 'login'
  }
})

const title = computed(() => {
  return props.type === 'login' ? '账号登录' : '账号注册'
})

const router = useRouter()
const clickHandle = (type) => {
  router.push({
    name: type === 'login' ? 'auth.login' : 'auth.register'
  })
}

</script>

<style lang="scss" scoped>
.login-form {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 20px;
  .login-form-title { 
    font-size: 20px;
    font-weight: 600;
    color: #333;
    text-align: center;
  }
  .login-form-content {
    width: 100%;
    flex: 1;
  }
  .login-form-footer {
    width: 100%;
    text-align: center;
    font-size: 14px;
    color: #666;
    
  }
}
</style>