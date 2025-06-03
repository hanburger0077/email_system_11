<template>
  <div class="login-page">
    <!-- 顶部导航栏 -->
    <div class="header">
      <img src="@/assets/logo.jpg" alt="华南理工大学" class="logo" />
      <div class="top-buttons">
        <button class="btn-login">登录</button>
        <button class="btn-register" @click="goToRegister">注册</button>
      </div>
    </div>

    <div class="main-container">
      <div class="left-section">
        <h1 class="title">邮箱系统</h1>
        <img src="@/assets/login-bg.jpg" alt="背景图片" class="bg-image" />
      </div>
      <div class="right-section">
        <div class="login-box">
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
      </div>
    </div>

    <div class="footer"></div>
  </div>
</template>

<script>
export default {
  name: 'LoginPage',
  data() {
    return {
      email: '', // 邮箱账号
      password: '' // 密码
    };
  },
  methods: {
    goToRegister() {
      this.$router.push({ name: 'Register' });
    },
    handleLogin() {
      if (this.email && this.password) {
        alert(`欢迎，${this.email}！登录成功。`);
        this.$router.push('/main'); 
      } else {
        alert('账号或密码为空！');
      }
    }
  }
};
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

.login-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f0f0f0;
}

/* 顶部导航栏 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  height: 10vh;
  background-color: #f0f0f0;
  flex-shrink: 0;
}

.logo {
  height: 60%;
  max-height: 50px;
  width: auto;
}

.top-buttons {
  display: flex;
  gap: 1vw;
}

.btn-login,
.btn-register {
  padding: 0.5em 1.2em;
  font-size: 0.9em;
  border-radius: 4px;
  border: none;
  cursor: pointer;
}

.btn-login {
  background-color: white;
  color: #005691;
  border: 1px solid #005691;
}

.btn-register {
  background-color: #005691;
  color: white;
}

/* 主体布局 */
/* .main-container {
  flex: 1;
  display: flex;
  padding: 0 0;
  justify-content: space-between;
  align-items: center;
  overflow: hidden;
  width: 100vw;         
  min-height: 80vh;    
  box-sizing: border-box;
} */

.main-container {
  flex: 1;
  display: flex;
  padding: 0 ;
  justify-content: space-between;
  align-items: center;
  overflow: hidden;
  width:97vw;
}

/* 左侧：标题与图片 */
.left-section,
.right-section {
  flex: 1;
  min-width: 300px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.title {
  font-size: 2.5vw;
  margin-bottom: 1em;
  color: #333;
}

.bg-image {
  width: 60%;
  max-width: 400px;
  height: auto;
  object-fit: contain;
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