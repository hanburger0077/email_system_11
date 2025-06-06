<template>
  <div class="write-content">
    <h2 class="page-title">写邮件</h2>
    <form @submit.prevent="sendEmail">
      <div class="form-field">
        <label>收件人：</label>
        <input type="email" v-model="to" required placeholder="请输入收件人邮箱" class="form-input" />
      </div>
      <div class="form-field">
        <label>主题：</label>
        <input type="text" v-model="subject" required placeholder="请输入邮件主题" class="form-input" />
      </div>
      <div class="form-field">
        <label>正文：</label>
        <textarea v-model="content" required placeholder="请输入邮件正文" rows="8" class="form-textarea"></textarea>
      </div>
      <div class="button-group">
        <button type="submit" class="send-btn">发送邮件</button>
        <button type="button" class="draft-btn-sm" @click="showDraftModal = true">存为草稿</button>
      </div>
    </form>

    <!-- 浮窗和弹窗组件 -->
    <div v-if="showToast" class="toast-message" :class="{ 'toast-success': toastType === 'success', 'toast-error': toastType === 'error' }">
      {{ toastMessage }}
    </div>
    <div v-if="showDraftModal" class="modal-overlay" @click.self="showDraftModal = false">
      <div class="modal-content">
        <h3 class="modal-title">确认保存草稿</h3>
        <p class="modal-message">您确定要将此邮件保存为草稿吗？</p>
        <div class="modal-buttons">
          <button class="modal-cancel-btn" @click="showDraftModal = false">取消</button>
          <button class="modal-confirm-btn" @click="saveAsDraft">确认</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      to: '',
      subject: '',
      content: '',
      showToast: false,
      toastMessage: '',
      toastType: 'success',
      showDraftModal: false
    };
  },
  methods: {
    sendEmail() {
      if (!this.to || !this.subject || !this.content) {
        this.showToastMessage('请填写完整信息', 'error');
        return;
      }
      this.showToastMessage('邮件发送成功', 'success');
      setTimeout(() => this.$router.push('/main'), 1500);
    },
    saveAsDraft() {
      this.showToastMessage('草稿保存成功', 'success');
      this.showDraftModal = false;
    },
    showToastMessage(message, type) {
      this.toastMessage = message;
      this.toastType = type;
      this.showToast = true;
      setTimeout(() => this.showToast = false, 3000);
    }
  }
};
</script>

<style scoped>
.write-content {
  padding: 32px 48px;
  background: #fff;
  height: 100%;
  overflow-y: auto;
}

.page-title {
  color: #1f74c0;
  font-size: 20px;
  font-weight: bold;
  margin-bottom: 24px;
  border-bottom: 2px solid #cce2fa;
  padding-bottom: 8px;
}

.form-field {
  margin-bottom: 16px;
}

.form-label {
  display: block;
  color: #666;
  font-size: 14px;
  margin-bottom: 6px;
}

.form-input,
.form-textarea {
  width: 100%;
  padding: 12px 16px;
  border: 1px solid #cce2fa;
  border-radius: 4px;
  background: #f8faff;
  font-size: 16px;
}

.form-textarea {
  height: 300px;
  resize: vertical;
}

.button-group {
  display: flex;
  gap: 16px;
  align-items: center;
}

.send-btn {
  padding: 14px 28px;
  background: #1f74c0;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-top: 24px;
}

.send-btn:hover {
  background: #1a5f9e;
}

.draft-btn-sm {
  padding: 10px 16px;
  background: #f5f7fa;
  color: #1f74c0;
  border: 1px solid #cce2fa;
  border-radius: 4px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-top: 24px;
}

.draft-btn-sm:hover {
  background: #e6f2fb;
}

/* 浮窗样式 */
.toast-message {
  position: fixed;
  top: 100px;
  right: 30px;
  padding: 12px 24px;
  border-radius: 4px;
  color: white;
  font-size: 16px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  z-index: 100;
  transform: translateY(-20px);
  opacity: 0;
  transition: all 0.3s ease;
}

.toast-message.toast-success {
  background: #4CAF50;
}

.toast-message.toast-error {
  background: #F44336;
}

.toast-message[style*="display: block"] {
  transform: translateY(0);
  opacity: 1;
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
}

.modal-content {
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  padding: 24px;
  width: 400px;
  max-width: 90%;
}

.modal-title {
  color: #1f74c0;
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 12px;
}

.modal-message {
  color: #333;
  font-size: 16px;
  margin-bottom: 24px;
}

.modal-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.modal-cancel-btn {
  padding: 10px 16px;
  background: #f5f7fa;
  color: #1f74c0;
  border: 1px solid #cce2fa;
  border-radius: 4px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
}

.modal-cancel-btn:hover {
  background: #e6f2fb;
}

.modal-confirm-btn {
  padding: 10px 16px;
  background: #1f74c0;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
}

.modal-confirm-btn:hover {
  background: #1a5f9e;
}
</style>