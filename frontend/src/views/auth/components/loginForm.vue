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
      <div v-else-if="type === 'register'">
        已有账号？
        <span class="email-link" @click="clickHandle('login')">立即登录</span>
      </div>
      <div v-else-if="type === 'forgotpassword'">
        想起密码了？
        <span class="email-link" @click="clickHandle('login')">返回登录</span>
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
  if (props.type === 'login') return '账号登录'
  if (props.type === 'register') return '账号注册'
  if (props.type === 'forgotpassword') return '忘记密码'
  return '账号登录'
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
    font-size: 12px;
    color: #666;
    
  }
  
}
</style>