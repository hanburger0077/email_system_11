<template>
  <div class="write-content">
    <h2 class="page-title">{{ isReply ? '回复邮件' : '写邮件' }}</h2>
    <form @submit.prevent="sendEmail">
      <div class="form-field">
        <label>收件人：</label>
        <input type="email" v-model="to" required placeholder="请输入收件人邮箱" class="form-input" />
        <div v-if="isSendingToSelf" class="error-hint">不能给自己发送邮件</div>
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
        <button type="submit" class="send-btn" :disabled="isSending || isSavingDraft || isSendingToSelf">
          {{ isSending ? '发送中...' : '发送邮件' }}
        </button>
        <button type="button" class="draft-btn" @click="saveAsDraft" :disabled="isSending || isSavingDraft">
          {{ isSavingDraft ? '保存中...' : '存为草稿' }}
        </button>
        <button type="button" class="cancel-btn" @click="cancelCompose">取消</button>
      </div>
    </form>

    <!-- 浮窗和弹窗组件 -->
    <div v-show="showToast" class="toast-message" :class="{ 'toast-success': toastType === 'success', 'toast-error': toastType === 'error', 'active': showToast }">
      {{ toastMessage }}
    </div>
    
    <div v-if="showDraftModal" class="modal-overlay" @click.self="showDraftModal = false">
      <div class="modal-content">
        <h3 class="modal-title">确认保存草稿</h3>
        <p class="modal-message">您确定要将此邮件保存为草稿吗？</p>
        <div class="modal-buttons">
          <button class="modal-cancel-btn" @click="showDraftModal = false">取消</button>
          <button class="modal-confirm-btn" @click="confirmSaveAsDraft">确认</button>
        </div>
      </div>
    </div>
    
    <div v-if="showCancelModal" class="modal-overlay" @click.self="showCancelModal = false">
      <div class="modal-content">
        <h3 class="modal-title">确认取消写信</h3>
        <p class="modal-message">确定要取消写信吗？未保存的内容将会丢失。</p>
        <div class="modal-buttons">
          <button class="modal-cancel-btn" @click="showCancelModal = false">返回编辑</button>
          <button class="modal-confirm-btn" @click="confirmCancel">确认取消</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { useUserStore } from '@/stores/user';

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
      showCancelModal: false,
      showLoading: false,
      attachments: [], // 存储上传的附件
      isReply: false,
      originalMail: null, // 存储原始邮件信息
      isSubmitting: false, // 保留用于兼容性，不再使用
      isSending: false,    // 邮件发送状态
      isSavingDraft: false, // 草稿保存状态
      draftId: null, // 草稿ID
      currentUserEmail: '', // 当前用户邮箱
    };
  },
  computed: {
    // 判断是否在给自己发送邮件
    isSendingToSelf() {
      return this.to && this.currentUserEmail && 
             this.to.trim().toLowerCase() === this.currentUserEmail.toLowerCase();
    }
  },
  created() {
    // 检查路由参数，确定是否为回复邮件
    const { reply, draftId } = this.$route.query;
    
    // 获取当前用户邮箱
    this.getCurrentUserEmail();
    
    if (reply === 'true') {
      this.isReply = true;
      
      // 尝试从sessionStorage获取回复邮件的信息
      const replyData = sessionStorage.getItem('replyMailData');
      if (replyData) {
        try {
          const parsedData = JSON.parse(replyData);
          this.to = parsedData.to || '';
          this.content = parsedData.content || '';
          
          // 检查是否有引用内容
          if (parsedData.quotedContent) {
            this.content = (this.content || '') + parsedData.quotedContent;
          }
        } catch (e) {
          console.error('解析回复邮件数据失败:', e);
        }
      } else {
        console.warn('没有找到回复邮件数据');
      }
    }
    
    // 从URL参数获取草稿ID
    if (draftId) {
      this.draftId = draftId;
      // 加载草稿内容
      this.loadDraft();
    }
  },
  watch: {
    // 移除监听收件人变化时的弹窗提示
    to() {
      // 不再调用showToastMessage
    }
  },
  methods: {
    // 获取当前用户邮箱
    getCurrentUserEmail() {
      const userStore = useUserStore();
      if (userStore.userInfo && userStore.userInfo.email) {
        this.currentUserEmail = userStore.userInfo.email;
        console.log('当前用户邮箱:', this.currentUserEmail);
      } else {
        console.warn('未能获取当前用户邮箱');
      }
    },
    
    triggerFileInput() {
      // 触发文件选择框
      this.$refs.fileInput.click();
    },
    
    handleFileUpload(event) {
      // 参考send-mail.html中的附件处理方法
      if (event.target.files.length > 0) {
        const file = event.target.files[0];
        // 添加文件大小限制检查
        if (file.size > 10 * 1024 * 1024) { // 10MB 大小限制
          this.showToastMessage(`文件 ${file.name} 超过10MB，无法上传`, 'error');
          event.target.value = ''; // 清空文件输入以便再次选择
          return;
        }
        this.attachments.push(file);
        event.target.value = ''; // 清空文件输入以便选择相同的文件
      }
    },
    
    removeAttachment(index) {
      // 从列表中移除附件，与send-mail.html保持一致
      this.attachments.splice(index, 1);
    },
    
    formatFileSize(bytes) {
      // 格式化文件大小显示
      if (bytes < 1024) return bytes + ' B';
      else if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
      else return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    },
    
    async loadDraft() {
      this.showLoading = true;
      try {
        const response = await fetch(`/api/mail/DRAFT/mails/${this.draftId}`);
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          const draft = result.data;
          this.to = draft.receiver_email;
          this.subject = draft.subject;
          this.content = draft.content;
          
          // 加载草稿的附件信息（如果有）
          if (draft.attachments && draft.attachments.length > 0) {
            // 这里需要根据实际API返回的附件格式处理
            // 由于附件需要实际文件对象，这里可能需要单独加载附件
            this.showToastMessage('草稿中包含附件，但需要重新添加', 'info');
          }
          
          this.showToastMessage('草稿加载成功', 'success');
        } else if (result.code === 'code.error') {
          this.showToastMessage(`草稿加载失败: ${result.message}: ${result.reason}`, 'error');
        }
      } catch (error) {
        console.error('Error loading draft:', error);
        this.showToastMessage('加载草稿失败', 'error');
      } finally {
        this.showLoading = false;
      }
    },
    
    async sendEmail() {
      // 参考send-mail.html中的邮件发送方法
      if (!this.to || !this.subject || !this.content) {
        this.showToastMessage('请填写完整信息', 'error');
        return;
      }
      
      // 检查是否在给自己发送邮件，仅做验证，不弹窗提示
      if (this.isSendingToSelf) {
        return; // 不再调用showToastMessage，直接返回
      }
      
      if (this.isSending) {
        return; // 防止重复提交
      }
      
      this.isSending = true;
      
      try {
        console.log('准备发送邮件...');
        // 创建FormData对象，用于发送邮件内容和附件，与send-mail.html保持一致
        const formData = new FormData();
        formData.append('to', this.to.trim());
        formData.append('subject', this.subject.trim());
        formData.append('content', this.content.trim());
        
        // 添加附件到FormData - 使用与send-mail.html相同的参数名
        if (this.attachments.length > 0) {
          this.attachments.forEach(file => {
            formData.append('attachmentFiles', file);
          });
        }
        
        // 发送请求到后端API
        const response = await fetch('/api/mail/send', {
          method: 'POST',
          body: formData
        });
        
        console.log('邮件发送响应状态:', response.status);
        
        if (!response.ok) {
          const errorData = await response.json();
          throw new Error(errorData.message || '发送邮件失败');
        }
        
        const result = await response.json();
        console.log('邮件发送响应结果:', result);
        
        if (result.code === 'code.ok') {
          // 如果邮件是从草稿发送的，则删除草稿
          if (this.draftId) {
            try {
              await fetch(`/api/mail/DRAFT/mails/${this.draftId}/delete`, {
                method: 'DELETE'
              });
              console.log('已删除发送成功的草稿');
            } catch (error) {
              console.warn('删除草稿失败:', error);
            }
          }
          
          // 清理回复邮件数据
          if (this.isReply) {
            sessionStorage.removeItem('replyMailData');
          }
          
          // 清空表单，与send-mail.html保持一致
          this.to = '';
          this.subject = '';
          this.content = '';
          this.attachments = [];
          
          this.showToastMessage('邮件发送成功', 'success');
          // 发送成功后延迟跳转
          setTimeout(() => this.$router.push('/main'), 2000);
        } else {
          this.showToastMessage(`邮件发送失败: ${result.message}`, 'error');
        }
      } catch (error) {
        console.error('发送邮件出错:', error);
        this.showToastMessage(`邮件发送失败: ${error.message}`, 'error');
      } finally {
        this.isSending = false;
      }
    },
    
    // 显示保存草稿确认框
    saveAsDraft() {
      // 如果内容为空，不保存草稿
      if (!this.to && !this.subject && !this.content.trim() && this.attachments.length === 0) {
        this.showToastMessage('邮件内容为空，无需保存草稿', 'error');
        return;
      }
      
      this.showDraftModal = true;
    },
    
    // 确认保存草稿
    async confirmSaveAsDraft() {
      if (this.isSavingDraft) {
        return; // 防止重复提交
      }
      
      this.isSavingDraft = true;
      this.showDraftModal = false; // 立即关闭确认对话框
      
      try {
        // 处理回复邮件草稿的内容 - 解决NumberFormatException问题
        let processedContent = this.content;
        
        // 检查是否为回复邮件，如果是回复邮件且内容包含原始邮件引用，则进行处理
        if (this.isReply && processedContent.includes('------------------ 原始邮件 ------------------')) {
          // 确保引用内容前至少有一个换行，如果没有则添加
          if (!processedContent.match(/\n\s*------------------/)) {
            processedContent = processedContent.replace(
              /------------------ 原始邮件 ------------------/, 
              '\n\n------------------ 原始邮件 ------------------'
            );
          }
        }
        
        // 创建FormData对象，用于保存草稿内容
        const formData = new FormData();
        formData.append('to', this.to);
        formData.append('subject', this.subject);
        formData.append('content', processedContent);
        
        // 添加附件代码，与send-mail.html中的草稿保存保持一致
        if (this.attachments.length > 0) {
          this.attachments.forEach(file => {
            formData.append('attachmentFiles', file);
          });
        }
        
        let response;
        if (this.draftId) {
          // 更新现有草稿
          response = await fetch(`/api/mail/drafts/${this.draftId}`, {
            method: 'PUT',
            body: formData
          });
        } else {
          // 创建新草稿
          response = await fetch('/api/mail/drafts', {
            method: 'POST',
            body: formData
          });
        }
        
        const result = await response.json();
        console.log('草稿保存结果:', result);
        
        if (result.code === 'code.ok') {
          // 更新草稿ID
          if (!this.draftId && result.data) {
            this.draftId = result.data.id || result.data;
          }
          this.showToastMessage('草稿保存成功', 'success');
        } else if (result.code === 'code.error') {
          this.showToastMessage(`草稿保存失败: ${result.message}: ${result.reason || ''}`, 'error');
        }
      } catch (error) {
        console.error('保存草稿出错:', error);
        this.showToastMessage(`草稿保存失败: ${error.message}`, 'error');
      } finally {
        this.isSavingDraft = false;
      }
    },
    
    // 取消写信
    cancelCompose() {
      // 如果内容为空或只填写了很少内容，直接关闭
      if (!this.content && !this.subject && !this.to && this.attachments.length === 0) {
        this.confirmCancel();
        return;
      }
      
      // 显示确认对话框
      this.showCancelModal = true;
    },
    
    // 确认取消写信
    confirmCancel() {
      // 关闭确认对话框
      this.showCancelModal = false;
      
      // 如果是编辑草稿但未保存更改，询问是否先保存草稿
      if (this.draftId && (this.to || this.subject || this.content)) {
        // 这里可以根据需要添加保存草稿的逻辑
      }
      
      // 清理回复邮件数据
      if (this.isReply) {
        sessionStorage.removeItem('replyMailData');
      }
      
      // 返回到邮箱主页面
      this.$router.push('/main?folder=INBOX');
    },
    
    showToastMessage(message, type) {
      console.log('显示提示:', message, type);
      this.toastMessage = message;
      this.toastType = type;
      this.showToast = true;
      
      // 3秒后隐藏提示
      setTimeout(() => {
        this.showToast = false;
      }, 3000);
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
  position: relative;
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

/* 错误提示样式 */
.error-hint {
  color: #F44336;
  font-size: 12px;
  margin-top: 4px;
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

/* 按钮样式 */
.button-group {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-top: 24px;
  width: auto;
  max-width: 600px;
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
  width: 180px;
}

.send-btn:hover {
  background: #1a5f9e;
}

.send-btn:disabled {
  background: #cccccc;
  cursor: not-allowed;
}

.draft-btn {
  padding: 14px 28px;
  background: #f5f7fa;
  color: #1f74c0;
  border: 1px solid #cce2fa;
  border-radius: 4px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
  width: 140px;
}

.draft-btn:hover {
  background: #e6f2fb;
}

.draft-btn:disabled {
  background: #f5f5f5;
  color: #999999;
  cursor: not-allowed;
}

.cancel-btn {
  padding: 14px 28px;
  background: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
  width: 120px;
}

.cancel-btn:hover {
  background: #d32f2f;
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
  z-index: 9999;
  opacity: 0;
  transform: translateY(-20px);
  transition: all 0.3s ease;
  pointer-events: none;
}

.toast-message.active {
  opacity: 1;
  transform: translateY(0);
}

.toast-success {
  background: #4CAF50;
}

.toast-error {
  background: #F44336;
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