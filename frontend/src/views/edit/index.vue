<template>
  <div class="write-content">
    <h2 class="page-title">{{ isReply ? '回复邮件' : '写邮件' }}</h2>
    <form @submit.prevent="sendEmail">
      <div class="form-field">
        <label>收件人：</label>
        <input type="email" v-model="to" required placeholder="请输入收件人邮箱" class="form-input" />
      </div>
      <div class="form-field">
        <label>主题：</label>
        <input type="text" v-model="subject" required placeholder="请输入邮件主题" class="form-input" />
      </div>

      <!-- 附件上传区域 -->
      <div class="form-field attachment-section">
        <div class="attachment-header">
          <label>附件：</label>
          <button type="button" class="attachment-btn" @click="triggerFileInput">
            <i class="attachment-icon">📎</i> 添加附件
          </button>
          <input 
            type="file" 
            ref="fileInput" 
            @change="handleFileUpload" 
            multiple
            style="display: none" 
          />
        </div>
        
        <div v-if="attachments.length > 0" class="attachment-list">
          <div v-for="(file, index) in attachments" :key="index" class="attachment-item">
            <span class="attachment-name">{{ file.name }}</span>
            <span class="attachment-size">({{ formatFileSize(file.size) }})</span>
            <button type="button" class="attachment-remove" @click="removeAttachment(index)">×</button>
          </div>
        </div>
      </div>

      <div class="form-field">
        <label>正文：</label>
        <textarea v-model="content" required placeholder="请输入邮件正文" rows="12" class="form-textarea"></textarea>
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
      showDraftModal: false,
      attachments: [], // 存储上传的附件
      isReply: false,
      originalMail: null // 存储原始邮件信息
    };
  },
  created() {
    // 检查路由参数，确定是否为回复邮件
    const { reply, to, subject, originalMail } = this.$route.query;
    
    if (reply === 'true') {
      this.isReply = true;
      this.to = to || '';
      
      // 尝试从sessionStorage获取被回复的邮件信息
      const storedMail = sessionStorage.getItem('currentMail');
      if (storedMail) {
        try {
          this.originalMail = JSON.parse(storedMail);
          // 构建回复邮件的正文，包含原邮件信息
          this.formatReplyContent();
        } catch (e) {
          console.error('解析原始邮件数据失败:', e);
        }
      }
    }
  },
  methods: {
    // 格式化回复邮件的内容，加入原始邮件信息
    formatReplyContent() {
      if (!this.originalMail) return;
      
      const original = this.originalMail;
      const separator = '\n\n' + '-'.repeat(60) + '\n';
      const quotePrefix = '> ';
      
      let replyContent = '\n\n' + separator;
      replyContent += `发件人: ${original.sender}\n`;
      replyContent += `时间: ${original.time}\n`;
      replyContent += `主题: ${original.subject}\n\n`;
      
      // 处理原邮件内容，每行前添加引用符号
      const originalLines = original.content.split('\n');
      const quotedContent = originalLines.map(line => quotePrefix + line).join('\n');
      
      replyContent += quotedContent;
      
      this.content = replyContent;
    },
    triggerFileInput() {
      // 触发文件选择框
      this.$refs.fileInput.click();
    },
    handleFileUpload(event) {
      // 处理文件上传
      const files = event.target.files;
      if (!files.length) return;
      
      // 将文件添加到附件列表
      for (let i = 0; i < files.length; i++) {
        if (files[i].size > 10 * 1024 * 1024) { // 10MB 大小限制
          this.showToastMessage(`文件 ${files[i].name} 超过10MB，无法上传`, 'error');
          continue;
        }
        this.attachments.push(files[i]);
      }
      
      // 清空文件输入以便再次选择相同文件
      event.target.value = '';
    },
    removeAttachment(index) {
      // 从列表中移除附件
      this.attachments.splice(index, 1);
    },
    formatFileSize(bytes) {
      // 格式化文件大小显示
      if (bytes < 1024) return bytes + ' B';
      else if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
      else return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    },
    sendEmail() {
      if (!this.to || !this.subject || !this.content) {
        this.showToastMessage('请填写完整信息', 'error');
        return;
      }
      
      // 这里可以进行附件的上传处理，比如创建FormData并发送到服务器
      const formData = new FormData();
      formData.append('to', this.to);
      formData.append('subject', this.subject);
      formData.append('content', this.content);
      formData.append('isReply', this.isReply);
      
      if (this.originalMail) {
        formData.append('originalMailId', this.originalMail.id);
      }
      
      this.attachments.forEach((file, index) => {
        formData.append(`attachment_${index}`, file);
      });
      
      // 模拟发送请求
      console.log('准备发送邮件，包含附件数量:', this.attachments.length);
      
      this.showToastMessage('邮件发送成功', 'success');
      setTimeout(() => this.$router.push('/main'), 1500);
    },
    saveAsDraft() {
      // 保存草稿逻辑，同样可以包含附件
      console.log('保存草稿，包含附件数量:', this.attachments.length);
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

/* 附件上传样式 */
.attachment-section {
  margin-bottom: 20px;
}

.attachment-header {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.attachment-btn {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  background-color: #f5f7fa;
  border: 1px dashed #cce2fa;
  border-radius: 4px;
  color: #1f74c0;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  margin-left: 8px;
}

.attachment-btn:hover {
  background-color: #e6f2fb;
  border-color: #1f74c0;
}

.attachment-icon {
  margin-right: 6px;
  font-style: normal;
}

.attachment-list {
  margin-top: 10px;
  max-height: 150px;
  overflow-y: auto;
  border: 1px solid #eee;
  border-radius: 4px;
  padding: 8px;
  background-color: #f9f9f9;
}

.attachment-item {
  display: flex;
  align-items: center;
  padding: 6px 8px;
  margin-bottom: 4px;
  background: white;
  border-radius: 3px;
  border: 1px solid #eee;
}

.attachment-name {
  flex: 1;
  font-size: 14px;
  margin-right: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-size {
  font-size: 12px;
  color: #999;
  margin-right: 8px;
}

.attachment-remove {
  background: none;
  border: none;
  color: #f44336;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
  padding: 0 4px;
}

.attachment-remove:hover {
  color: #d32f2f;
}

/* 原有按钮样式 */
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