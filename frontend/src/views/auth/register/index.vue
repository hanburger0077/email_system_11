<template>
  <layout>
    <div class="main-container">
      <h2 class="box-title">账号注册</h2>
      <form @submit.prevent="handleRegister">
        <input 
          type="text" 
          placeholder="设置用户名" 
          class="input" 
          v-model="form.username" 
        />
        <input 
          type="password" 
          placeholder="设置密码" 
          class="input" 
          v-model="form.password" 
        />
        <input 
          type="password" 
          placeholder="确认密码" 
          class="input" 
          v-model="form.confirmPassword" 
          @blur="checkPasswordMatch"  
        />
        <p 
          v-if="isPasswordMismatch && form.confirmPassword" 
          class="error"
        >
          两次输入的密码不一致
        </p>
        <input 
          type="text" 
          placeholder="手机号/邮箱" 
          class="input" 
          v-model="form.contact" 
        />
        <div class="captcha-group">
          <input 
            type="text" 
            placeholder="验证码" 
            class="input captcha-input" 
            v-model="form.captcha" 
          />
          <button class="captcha-btn">获取验证码</button>
        </div>
        <div class="terms-group">
          <input 
            type="checkbox" 
            id="terms" 
            v-model="form.agreeTerms" 
          />
          <label for="terms">我已阅读并同意服务条款和隐私政策</label>
        </div>
        <button class="btn-submit">注册</button>
        <p class="login-link">如已有账号？<a @click="goToLogin">去登录</a></p>
      </form>
    </div>
  </layout>
</template>

<script>
import layout from '@/components/authLayout/index.vue';
export default {
  name: 'RegisterPage',
  components: {
    layout,
  },
  data() {
    return {
      form: {
        username: '',
        password: '',
        confirmPassword: '',
        contact: '',
        captcha: '',
        agreeTerms: false,
      },
      isPasswordMismatch: false, // 密码是否不一致
      hasTouchedConfirm: false, // 是否触碰过确认密码输入框
    };
  },
  computed: {
    isFormValid() {
      // 验证所有必填项（含空格处理）
      return (
        this.form.username.trim() !== '' && // 用户名非空（去空格）
        this.form.password.length >= 6 && // 密码≥6位
        this.form.confirmPassword === this.form.password && // 密码一致
        this.form.contact.trim() !== '' && // 联系方式非空（去空格）
        this.form.captcha.trim() !== '' && // 验证码非空（去空格）
        this.form.agreeTerms // 勾选条款
      );
    },
  },
  methods: {
    goToLogin() {
      this.$router.push({ name: 'Login' }); // 跳转到登录页
    },
    // 确认密码失去焦点时验证
    checkPasswordMatch() {
      this.hasTouchedConfirm = true; // 标记已输入确认密码
      this.isPasswordMismatch = this.form.password !== this.form.confirmPassword;
    },
    handleRegister() {
      if (this.isFormValid) {
        // 模拟注册成功（实际调用API）
        console.log('注册数据：', this.form);
        alert('注册成功，请登录！');
        this.$router.push({ name: 'Login' }); // 跳转到登录页
        // 清空表单
        this.form = {
          username: '',
          password: '',
          confirmPassword: '',
          contact: '',
          captcha: '',
          agreeTerms: false,
        };
        this.isPasswordMismatch = false;
      } else {
        // 提示具体验证失败原因
        let errorMsg = '请检查：\n';
        if (this.form.username.trim() === '') errorMsg += '- 用户名不能为空\n';
        if (this.form.password.length < 6) errorMsg += '- 密码至少6位\n';
        if (this.hasTouchedConfirm && this.isPasswordMismatch) errorMsg += '- 密码不一致\n';
        if (this.form.contact.trim() === '') errorMsg += '- 联系方式不能为空\n';
        if (this.form.captcha.trim() === '') errorMsg += '- 验证码不能为空\n';
        if (!this.form.agreeTerms) errorMsg += '- 请勾选服务条款\n';
        alert(errorMsg);
      }
    },
  },
};
</script>

<style scoped>
.error {
  color: red;
  font-size: 12px;
  margin-top: -10px;
  margin-bottom: 15px;
  text-align: left;
}
/* 全局样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  font-family: Arial, sans-serif;
}

.login-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f0f0f0;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 40px;
  height: 80px;
  background-color: #f0f0f0;
}

.logo {
  height: 60px;
  width: auto;
}

.top-buttons {
  display: flex;
  gap: 20px;
}

.btn-login, .btn-register {
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}

.btn-login {
  background: white;
  color: #005691;
  border: 1px solid #005691;
}

.btn-register.active {
  background: #005691;
  color: white;
  border: none;
}

.left-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.right-section {
  flex: 1;
  display: flex;
  justify-content: center;
}

.title {
  font-size: 32px;
  margin-bottom: 20px;
  color: #333;
}

.bg-image {
  width: 400px;
  height: auto;
  margin-bottom: 30px;
}

.register-box {
  width: 90%;
  max-width: 500px;
  min-width: 350px;
  padding: 2.5em;
  background-color: white;
  border-radius: 10px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
}

.box-title {
  font-size: 24px;
  margin-bottom: 20px;
  text-align: center;
  color: #333;
}

.input {
  width: 100%;
  padding: 12px;
  margin-bottom: 15px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.captcha-group {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.captcha-input {
  flex: 1;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.captcha-btn {
  height: 40px;
  padding: 0 20px;
  background: #005691;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.terms-group {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  font-size: 14px;
  color: #666;
}

.btn-submit {
  width: 100%;
  padding: 12px;
  background: #005691;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  margin-bottom: 15px;
}

.login-link {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.login-link a {
  color: #005691;
  text-decoration: none;
}

.footer {
  height: 50px;
  background: #005691;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .main-container {
    flex-direction: column;
    padding: 20px;
  }

  .left-section, .right-section {
    width: 100%;
    margin: 20px 0;
  }

  .bg-image {
    width: 300px;
  }

  .register-box {
    width: 100%;
    padding: 30px;
  }
}
</style>