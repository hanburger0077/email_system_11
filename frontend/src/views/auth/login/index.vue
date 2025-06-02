<template>
  <layout>
    <div class="main-container">
      <h2 class="box-title">账号登录</h2>
      <form>
        <!-- 新增 v-model 绑定 -->
        <input 
          type="text" 
          placeholder="邮箱账号" 
          class="input" 
          v-model="email" 
        />
        <input 
          type="password" 
          placeholder="输入密码" 
          class="input" 
          v-model="password" 
        />
        <button 
          type="button"
          class="btn-submit" 
          @click="handleLogin"
        >登录</button>
        <p v-if="!email || !password" class="error">请填写完整账号和密码</p>
      </form>
      <p class="register-link" @click="goToRegister">注册新账号</p>
    </div>
  </layout>
</template>

<script setup>
import layout from '@/components/authLayout/index.vue';
</script>

<style scoped>

.error {
  color: red;
  font-size: 12px;
  margin-top: 8px;
  text-align: center;
}

/* 全局填满并禁止滚动 */
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100vh;
  font-family: Arial, sans-serif;
}

/* 右侧：登录框 */
.login-box {
  width: 90%;           /* 原来是70%，改大一点 */
  max-width: 500px;     /* 原来是400px，改大一点 */
  min-width: 350px;     /* 新增，防止太窄 */
  padding: 2.5em;       /* 可适当加大 */
  background-color: white;
  border-radius: 10px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.box-title {
  font-size: 1.5em;
  margin-bottom: 1em;
  text-align: center;
}

.input {
  width: 100%;
  padding: 0.75em;
  margin-bottom: 1em;
  font-size: 1em;
  border: 1px solid #ccc;
  border-radius: 4px;
  box-sizing: border-box; /* 新增，统一盒模型 */
}

.btn-submit {
  display: block;
  width: 100%; /* 新增，与输入框等宽 */
  box-sizing: border-box; /* 新增，统一盒模型 */
  padding: 0.75em;
  font-size: 1em;
  background-color: #005691;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.register-link {
  margin-top: 1em;
  text-align: center;
  color: #666;
  font-size: 0.9em;
  cursor: pointer;
}

/* 底部横条 */
.footer {
  height: 6vh;
  background-color: #005691;
  flex-shrink: 0;
}

/* ✅ 响应式处理：适配小屏 */
@media (max-width: 768px) {
  .main-container {
    flex-direction: column;
    padding: 0 0 }

  .left-section,
  .right-section {
    flex: none;
    width: 100%;
    padding: 2vh 0;
  }

  .bg-image {
    width: 80%;
  }

  .title {
    font-size: 6vw;
  }

  .login-box {
    width: 90%;
    padding: 1.5em;
  }
}
</style>