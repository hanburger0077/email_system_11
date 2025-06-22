<template>
  <div class="mail-detail">
    <div v-if="isLoading" class="loading-indicator">
      <div class="loader"></div>
      <p>加载中，请稍候...</p>
    </div>

    <template v-else>
      <!-- 邮件操作栏 -->
      <div class="mail-actions">
        <button class="action-btn" @click="goBack">
          <span class="action-icon">←</span> 返回
        </button>
        
        <!-- 回复按钮 -->
        <button class="action-btn reply-btn" @click="replyMail">
          <span class="action-icon">↩️</span> 回复
        </button>
        
        <!-- 根据文件夹类型显示不同操作按钮 -->
        <template v-if="mailbox === 'INBOX' || mailbox === 'JUNK'">
          <button class="action-btn" @click="moveToTrash">
            <span class="action-icon">🗑</span> 移至回收站
          </button>
        </template>
        
        <template v-if="mailbox === 'TRASH'">
          <button class="action-btn" @click="restoreMail">
            <span class="action-icon">↩</span> 还原邮件
          </button>
        </template>
        
        <!-- 永久删除按钮 -->
        <button class="action-btn delete" @click="confirmDelete">
          <span class="action-icon">❌</span> 永久删除
        </button>
        
        <!-- 星标按钮 -->
        <button 
          class="action-btn" 
          @click="toggleStar" 
          :title="isStarred ? '取消星标' : '加注星标'"
        >
          <span class="action-icon" :class="{ 'star-active': isStarred }">
            {{ isStarred ? '★' : '☆' }}
          </span>
          {{ isStarred ? '取消星标' : '加注星标' }}
        </button>
        
        <!-- 收件箱才显示已读/未读切换 -->
        <template v-if="mailbox === 'INBOX'">
          <button 
            class="action-btn" 
            @click="toggleReadStatus"
            :title="mail.read === 1 ? '标为未读' : '标为已读'"
          >
            <span class="action-icon">{{ mail.read === 1 ? '📖' : '📕' }}</span>
            {{ mail.read === 1 ? '标为未读' : '标为已读' }}
          </button>
        </template>
      </div>
      
      <!-- 邮件主体内容 -->
      <div class="mail-header-info">
        <h1 class="mail-subject">{{ mail.subject }}</h1>
        <div class="mail-meta">
          <div class="sender-info">
            <span class="label">发件人:</span>
            <span class="value">{{ mail.sender_email }}</span>
          </div>
          <div class="receiver-info">
            <span class="label">收件人:</span>
            <span class="value">{{ mail.receiver_email }}</span>
          </div>
          <div class="time-info">
            <span class="label">时间:</span>
            <span class="value">{{ formatTime(mail.create_at) }}</span>
          </div>
        </div>
      </div>
      
      <!-- 邮件内容 -->
      <div class="mail-content" v-html="mail.content"></div>
      
      <!-- 邮件附件 -->
      <div class="attachments-section" v-if="attachments && attachments.length > 0">
        <h3 class="attachments-title">附件 ({{ attachments.length }})</h3>
        <div class="attachments-list">
          <div v-for="attachment in attachments" :key="attachment.id" class="attachment-item">
            <a 
              :href="`/attachments/download/${attachment.id}`" 
              target="_blank"
              class="attachment-link"
              @click.prevent="downloadAttachment(attachment)"
            >
              {{ attachment.name || `附件-${attachment.id}` }}
              <span v-if="attachment.downloading" class="downloading-indicator">
                <span class="downloading-spinner"></span> 下载中...
              </span>
              <span v-else>
                ({{ formatFileSize(attachment.size) }})
              </span>
            </a>
          </div>
        </div>
      </div>
    </template>
    
    <!-- 确认删除模态框 -->
    <div class="modal-overlay" v-if="showDeleteModal">
      <div class="modal-content">
        <h3 class="modal-title">确认删除</h3>
        <p class="modal-message">您确定要永久删除此邮件吗？此操作无法撤销。</p>
        <div class="modal-buttons">
          <button class="modal-cancel-btn" @click="showDeleteModal = false">取消</button>
          <button class="modal-confirm-btn" @click="deleteMail">确认删除</button>
        </div>
      </div>
    </div>
    
    <!-- Toast消息提示 -->
    <div 
      class="toast-message" 
      :class="toastType" 
      v-if="showToast"
    >
      {{ toastMessage }}
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      mail: {
        mail_id: null,
        subject: '',
        sender_email: '',
        receiver_email: '',
        create_at: '',
        content: '',
        attachmentIds: [],
        read: 0,
        receiver_star: 0,
        sender_star: 0
      },
      isLoading: true,
      currentFolder: 'INBOX',
      attachments: [],
      showDeleteModal: false,
      showToast: false,
      toastMessage: '',
      toastType: 'success',
      toastTimeout: null
    };
  },
  computed: {
    isStarred() {
      if (this.mailbox === 'INBOX') {
        return this.mail.receiver_star === 1;
      } else if (this.mailbox === 'SENT') {
        return this.mail.sender_star === 1;
      }
      return false;
    },
    mailId() {
      return this.$route.query.id;
    },
    mailbox() {
      return this.$route.query.mailbox || 'INBOX';
    }
  },
  created() {
    this.initMailData();
  },
  methods: {
    // 初始化邮件数据
    initMailData() {
      this.currentFolder = this.mailbox;
      this.fetchMailData(this.mailbox, this.mailId);
    },
    
    // 获取邮件数据
    async fetchMailData(mailbox, mailId) {
      if (!mailId) {
        this.showToastMessage('邮件ID无效', 'error');
        this.isLoading = false;
        return;
      }
      
      this.isLoading = true;
      
      try {
        const response = await fetch(`/api/mail/${mailbox}/mails/${mailId}`);
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          this.mail = result.data;
          
          // 更新sessionStorage缓存
          sessionStorage.setItem('currentMail', JSON.stringify(this.mail));
          
          // 获取附件信息
          if (this.mail.attachmentIds && this.mail.attachmentIds.length > 0) {
            await this.fetchAttachmentsInfo();
          }
        } else if (result.code === 'code.error') {
          this.showToastMessage(`获取邮件失败: ${result.message}${result.reason ? ': ' + result.reason : ''}`, 'error');
        } else {
          this.showToastMessage(`获取邮件失败: ${result.message}`, 'error');
        }
      } catch (error) {
        console.error('获取邮件数据出错:', error);
        this.showToastMessage('获取邮件数据失败，请检查网络连接', 'error');
      } finally {
        this.isLoading = false;
      }
    },
    
    // 回复邮件
    replyMail() {
      if (!this.mail.mail_id) {
        this.showToastMessage('无法回复，邮件数据无效', 'error');
        return;
      }

      try {
        const originalSender = this.mail.sender_email || '';
        const formattedTime = this.formatTime(this.mail.create_at);
        const originalSubject = this.mail.subject || '';
        const originalContent = this.mail.content || '';
        
        let replySubject = originalSubject;
        if (!replySubject.startsWith('回复:')) {
          replySubject = '回复: ' + replySubject;
        }
        
        const quotedContent = `<br><br><hr>
          <div style="color: #666; font-size: 0.9em; padding: 10px; background-color: #f9f9f9; border-left: 3px solid #ccc;">
            <p><strong>原始邮件</strong></p>
            <p><strong>发件人:</strong> ${originalSender}</p>
            <p><strong>时间:</strong> ${formattedTime}</p>
            <p><strong>主题:</strong> ${originalSubject}</p>
            <div style="margin-top: 10px;">${originalContent}</div>
          </div>
        `;
        
        const replyData = {
          to: originalSender,
          subject: replySubject,
          content: "",
          quotedContent: quotedContent
        };
        
        sessionStorage.setItem('replyMailData', JSON.stringify(replyData));
        
        this.$router.push({
          path: '/edit',
          query: { reply: 'true' }
        });
      } catch (error) {
        console.error('准备回复邮件时出错:', error);
        this.showToastMessage('回复邮件失败，请稍后再试', 'error');
      }
    },
    
    // 格式化时间显示
    formatTime(dateStr) {
      if (!dateStr) return '未知时间';
      
      try {
        const date = new Date(dateStr);
        
        if (isNaN(date.getTime())) {
          console.warn('无法解析的时间:', dateStr);
          return '未知时间';
        }
        
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
      } catch (error) {
        console.error('格式化时间错误:', error);
        return '未知时间';
      }
    },
    
    // 显示提示信息
    showToastMessage(message, type = 'success') {
      if (this.toastTimeout) {
        clearTimeout(this.toastTimeout);
      }
      
      this.toastMessage = message;
      this.toastType = type;
      this.showToast = true;
      
      this.toastTimeout = setTimeout(() => {
        this.showToast = false;
      }, 3000);
    },
    
    // 获取附件信息
    async fetchAttachmentsInfo() {
      try {
        if (!this.mail.attachmentIds || this.mail.attachmentIds.length === 0) {
          this.attachments = [];
          return;
        }
        
        const attachmentPromises = this.mail.attachmentIds.map(async attachmentId => {
          try {
            const response = await fetch(`/attachments/${attachmentId}`);
            const result = await response.json();
            
            if (result.code === 'code.ok' && result.data) {
              return { 
                id: attachmentId,
                name: result.data.fileName || `附件-${attachmentId}`,
                size: result.data.size || 0
              };
            } else {
              console.warn(`获取附件 ${attachmentId} 信息失败:`, result.message);
              return { id: attachmentId, name: `附件-${attachmentId}`, size: 0 };
            }
          } catch (error) {
            console.error(`获取附件 ${attachmentId} 信息出错:`, error);
            return { id: attachmentId, name: `附件-${attachmentId}`, size: 0 };
          }
        });
        
        this.attachments = await Promise.all(attachmentPromises);
      } catch (error) {
        console.error('获取附件信息失败:', error);
        this.showToastMessage('获取附件信息失败', 'error');
      }
    },
    
    // 格式化文件大小
    formatFileSize(bytes) {
      if (bytes === 0 || bytes === undefined) return '0 B';
      const k = 1024;
      const sizes = ['B', 'KB', 'MB', 'GB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]);
    },
    
    // 下载附件
    async downloadAttachment(attachment) {
      // 标记为下载中
      this.$set(attachment, 'downloading', true);
      
      try {
        const link = document.createElement('a');
        link.href = `/attachments/download/${attachment.id}`;
        link.setAttribute('download', attachment.name);
        link.style.display = 'none';
        document.body.appendChild(link);
        link.click();
        setTimeout(() => {
          document.body.removeChild(link);
          this.$set(attachment, 'downloading', false);
        }, 100);
        console.log(`开始下载附件: ${attachment.name}`);
      } catch (error) {
        console.error('下载附件出错:', error);
        this.showToastMessage('下载附件失败', 'error');
        this.$set(attachment, 'downloading', false);
      }
    },
    
    // 返回上一页
    goBack() {
      const from = this.$route.query.from;
      if (from && window.history.length > 1) {
        this.$router.back();
        return;
      }
      if (from === 'star') {
        this.$router.push('/star');
      } else {
        this.$router.push({
          path: '/main',
          query: { folder: this.mailbox }
        });
      }
    },
    
    // 切换星标状态
    async toggleStar() {
      if (!this.mail.mail_id) {
        this.showToastMessage('无法操作，邮件ID无效', 'error');
        return;
      }
      
      try {
        let starSign;
        if (this.mailbox === 'INBOX') {
          starSign = 'R_STAR';
        } else if (this.mailbox === 'SENT') {
          starSign = 'S_STAR';
        } else {
          this.showToastMessage('当前文件夹不支持星标操作', 'error');
          return;
        }
        
        const operation = this.isStarred ? '-FLAG' : '+FLAG';
        const isAdding = !this.isStarred;
        
        const response = await fetch(`/api/mail/${this.mailbox}/mails/${this.mail.mail_id}/change/${starSign}/${operation}`, {
          method: 'POST'
        });
        
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          if (this.mailbox === 'INBOX') {
            this.mail.receiver_star = isAdding ? 1 : 0;
          } else {
            this.mail.sender_star = isAdding ? 1 : 0;
          }
          sessionStorage.setItem('currentMail', JSON.stringify(this.mail));
          this.showToastMessage(isAdding ? '已添加星标' : '已取消星标');
        } else {
          this.showToastMessage('修改星标状态失败: ' + (result.reason || result.message), 'error');
        }
      } catch (error) {
        console.error('修改星标状态出错:', error);
        this.showToastMessage('修改星标状态失败，请检查网络连接', 'error');
      }
    },
    
    // 切换已读/未读状态
    async toggleReadStatus() {
      if (!this.mail.mail_id || this.mailbox !== 'INBOX') {
        this.showToastMessage('无法操作，邮件ID无效或不在收件箱', 'error');
        return;
      }
      
      try {
        const operation = this.mail.read === 1 ? '-FLAG' : '+FLAG';
        const endpoint = `/api/mail/${this.mailbox}/mails/${this.mail.mail_id}/change/READ/${operation}`;
        const response = await fetch(endpoint, { method: 'POST' });
        const result = await response.json();
        if (result.code === 'code.ok') {
          this.mail.read = this.mail.read === 1 ? 0 : 1;
          sessionStorage.setItem('currentMail', JSON.stringify(this.mail));
          this.showToastMessage(this.mail.read === 1 ? '已标为已读' : '已标为未读');
        } else {
          this.showToastMessage('修改已读状态失败', 'error');
        }
      } catch (error) {
        console.error('修改已读状态出错:', error);
        this.showToastMessage('修改已读状态失败', 'error');
      }
    },
    
    // 移至回收站
    async moveToTrash() {
      if (!this.mail.mail_id) {
        this.showToastMessage('无法操作，邮件ID无效', 'error');
        return;
      }
      
      try {
        // 修改为使用 URL query 中的 mailbox
        const endpoint = `/api/mail/${this.mailbox}/mails/${this.mail.mail_id}/change/TRASH/+FLAG`;
        const response = await fetch(endpoint, { method: 'POST' });
        const result = await response.json();
        if (result.code === 'code.ok') {
          this.showToastMessage('邮件已移至回收站');
          setTimeout(() => this.goBack(), 1500);
        } else {
          this.showToastMessage('移动邮件失败', 'error');
        }
      } catch (error) {
        console.error('移动邮件出错:', error);
        this.showToastMessage('移动邮件失败', 'error');
      }
    },
    
    // 从回收站还原邮件
    async restoreMail() {
      if (!this.mail.mail_id || this.mailbox !== 'TRASH') {
        this.showToastMessage('无法操作，邮件ID无效或不在回收站', 'error');
        return;
      }
      
      try {
        const endpoint = `/api/mail/${this.mailbox}/mails/${this.mail.mail_id}/change/TRASH/-FLAG`;
        const response = await fetch(endpoint, { method: 'POST' });
        const result = await response.json();
        if (result.code === 'code.ok') {
          this.showToastMessage('邮件已还原');
          setTimeout(() => this.goBack(), 1500);
        } else {
          this.showToastMessage('还原邮件失败', 'error');
        }
      } catch (error) {
        console.error('还原邮件出错:', error);
        this.showToastMessage('还原邮件失败', 'error');
      }
    },
    
    // 显示删除确认模态框
    confirmDelete() {
      this.showDeleteModal = true;
    },
    
    // 永久删除邮件
    async deleteMail() {
      if (!this.mail.mail_id) {
        this.showToastMessage('无法操作，邮件ID无效', 'error');
        this.showDeleteModal = false;
        return;
      }
      
      try {
        // 修改为使用URL query中的 mailbox
        const response = await fetch(`/api/mail/${this.mailbox}/mails/${this.mail.mail_id}/delete`, {
          method: 'DELETE'
        });
        const result = await response.json();
        if (result.code === 'code.ok') {
          this.showDeleteModal = false;
          this.showToastMessage('邮件已永久删除');
          setTimeout(() => this.goBack(), 1500);
        } else {
          this.showToastMessage('删除邮件失败', 'error');
          this.showDeleteModal = false;
        }
      } catch (error) {
        console.error('删除邮件出错:', error);
        this.showToastMessage('删除邮件失败', 'error');
        this.showDeleteModal = false;
      }
    }
  },
  beforeDestroy() {
    if (this.toastTimeout) {
      clearTimeout(this.toastTimeout);
    }
  }
};
</script>

<style scoped>
.mail-detail {
  max-width: 900px;
  margin: 0 auto;
  padding: 20px;
  position: relative;
  min-height: calc(100vh - 120px);
}

.mail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eaeaea;
}

.action-btn {
  display: flex;
  align-items: center;
  padding: 8px 15px;
  background-color: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.action-btn:hover {
  background-color: #ecf5ff;
  color: #409EFF;
  border-color: #c6e2ff;
}

.reply-btn {
  background-color: #ecf5ff;
  color: #409EFF;
  border-color: #c6e2ff;
}

.reply-btn:hover {
  background-color: #409EFF;
  color: white;
  border-color: #409EFF;
}

.action-icon {
  margin-right: 5px;
  font-size: 16px;
}

.star-active {
  color: #f1c40f;
}

.mail-header-info {
  margin-bottom: 30px;
}

.mail-subject {
  font-size: 24px;
  margin-bottom: 15px;
  color: #303133;
  word-break: break-word;
}

.mail-meta {
  color: #606266;
  font-size: 14px;
  margin-bottom: 20px;
}

.sender-info,
.receiver-info,
.time-info {
  margin-bottom: 8px;
  display: flex;
}

.label {
  font-weight: bold;
  min-width: 80px;
}

.value {
  word-break: break-all;
}

.mail-content {
  line-height: 1.6;
  color: #303133;
  word-wrap: break-word;
  overflow-wrap: break-word;
  padding-bottom: 20px;
}

.mail-content pre {
  white-space: pre-wrap;
  background-color: #f9f9f9;
  padding: 10px;
  border-radius: 4px;
  overflow: auto;
}

.loading-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 60vh;
}

.loader {
  border: 5px solid #f3f3f3;
  border-top: 5px solid #409EFF;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin-bottom: 15px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 附件样式 */
.attachments-section {
  margin-top: 30px;
  padding: 15px;
  background-color: #f0f8ff;
  border-radius: 8px;
  border: 1px solid #e0e0e0;
}

.attachments-title {
  font-size: 16px;
  margin-bottom: 15px;
  color: #606266;
  font-weight: bold;
}

.attachments-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.attachment-item {
  padding: 5px 0;
}

.attachment-link {
  color: #0066cc;
  text-decoration: none;
  cursor: pointer;
  display: block;
  font-size: 14px;
  padding: 5px 0;
}

.attachment-link:hover {
  text-decoration: underline;
}

.downloading-indicator {
  color: #666;
  font-size: 0.9em;
  display: inline-flex;
  align-items: center;
  margin-left: 5px;
}

.downloading-spinner {
  display: inline-block;
  width: 12px;
  height: 12px;
  border: 2px solid rgba(0,0,0,0.1);
  border-radius: 50%;
  border-top-color: #409EFF;
  animation: spin 1s linear infinite;
  margin-right: 5px;
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background-color: #fff;
  padding: 20px;
  border-radius: 8px;
  width: 400px;
  max-width: 90%;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
}

.modal-title {
  font-size: 18px;
  margin-bottom: 15px;
  color: #303133;
  position: relative;
}

.modal-message {
  margin-bottom: 20px;
  color: #606266;
}

.modal-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 15px;
}

.modal-cancel-btn {
  padding: 8px 20px;
  background-color: #f5f7fa;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s;
}

.modal-cancel-btn:hover {
  color: #409EFF;
  border-color: #c6e2ff;
  background-color: #ecf5ff;
}

.modal-confirm-btn {
  padding: 8px 20px;
  background-color: #f56c6c;
  color: white;
  border: 1px solid #f56c6c;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.modal-confirm-btn:hover {
  background-color: #f78989;
  border-color: #f78989;
}

/* Toast提示样式 */
.toast-message {
  position: fixed;
  top: 60px;
  left: 50%;
  transform: translateX(-50%);
  padding: 10px 20px;
  border-radius: 4px;
  color: white;
  font-size: 14px;
  z-index: 2000;
  animation: fadeIn 0.3s;
}

.toast-message.success {
  background-color: #67C23A;
}

.toast-message.error {
  background-color: #F56C6C;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translate(-50%, -20px); }
  to { opacity: 1; transform: translate(-50%, 0); }
}

@media (max-width: 600px) {
  .mail-actions {
    flex-direction: column;
    gap: 8px;
  }
  
  .action-btn {
    width: 100%;
  }
  
  .mail-subject {
    font-size: 20px;
  }
}
</style>