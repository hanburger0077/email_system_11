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
        <template v-if="currentFolder === 'INBOX' || currentFolder === 'JUNK'">
          <button class="action-btn" @click="moveToTrash">
            <span class="action-icon">🗑</span> 移至回收站
          </button>
        </template>
        
        <template v-if="currentFolder === 'TRASH'">
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
        <template v-if="currentFolder === 'INBOX'">
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
      
      <!-- 邮件附件 - 参考view-mail.html的方式 -->
      <div class="attachments-section" v-if="attachments && attachments.length > 0">
        <h3 class="attachments-title">附件 ({{ attachments.length }})</h3>
        <div class="attachments-list">
          <div v-for="attachment in attachments" :key="attachment.id" class="attachment-item">
            <a 
              :href="`/attachments/download/${attachment.id}`" 
              target="_blank"
              class="attachment-link"
            >
              {{ attachment.name || `附件-${attachment.id}` }}
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
      if (this.currentFolder === 'INBOX') {
        return this.mail.receiver_star === 1;
      } else if (this.currentFolder === 'SENT') {
        return this.mail.sender_star === 1;
      }
      return false;
    }
  },
  created() {
    this.fetchMailData();
  },
  methods: {
    // 回复邮件
    replyMail() {
      if (!this.mail.mail_id) {
        this.showToastMessage('无法回复，邮件数据无效', 'error');
        return;
      }

      try {
        // 确保发件人信息正确
        const originalSender = this.mail.sender_email || '';
        
        // 确保时间格式正确
        const formattedTime = this.formatTime(this.mail.create_at);
        
        // 确保主题正确
        const originalSubject = this.mail.subject || '';
        
        // 确保邮件内容正确
        const originalContent = this.mail.content || '';
        
        // 生成回复邮件的主题，如果原邮件主题不以"回复:"开头，则添加
        let replySubject = originalSubject;
        if (!replySubject.startsWith('回复:')) {
          replySubject = '回复: ' + replySubject;
        }
        
        // 生成引用的原始邮件内容
        const quotedContent = `



------------------ 原始邮件 ------------------
  发件人: ${originalSender}
  发送时间: ${formattedTime}
  主题: ${originalSubject}
  内容: ${originalContent}
`;
        
        // 将回复数据存储到sessionStorage
        const replyData = {
          to: originalSender,
          subject: replySubject,
          content: "",  // 初始内容为空
          quotedContent: quotedContent
        };
        
        sessionStorage.setItem('replyMailData', JSON.stringify(replyData));
        
        // 导航到写信页面
        this.$router.push({
          path: '/edit',
          query: { reply: 'true' }
        });
      } catch (error) {
        console.error('准备回复邮件时出错:', error);
        this.showToastMessage('回复邮件失败，请稍后再试', 'error');
      }
    },
    
    // 格式化时间显示，始终显示完整的年月日和时间
    formatTime(dateStr) {
      if (!dateStr) return '未知时间';
      
      try {
        // 解析日期
        const date = new Date(dateStr);
        
        // 检查日期是否有效
        if (isNaN(date.getTime())) {
          console.warn('无法解析的时间:', dateStr);
          return '未知时间';
        }
        
        // 获取年月日时分
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        
        // 返回完整格式的日期时间
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
    
    // 获取邮件数据
    async fetchMailData() {
      this.isLoading = true;
      
      try {
        const urlParams = new URLSearchParams(window.location.search);
        const mailId = this.$route.query.id;
        const mailbox = this.$route.query.mailbox || 'INBOX';
        this.currentFolder = mailbox;
        
        // 尝试从sessionStorage获取邮件数据
        const storedMail = sessionStorage.getItem('currentMail');
        
        if (storedMail) {
          this.mail = JSON.parse(storedMail);
          console.log('Using stored mail data:', this.mail);
        }
        
        if (mailId && (!this.mail.mail_id || parseInt(mailId) !== this.mail.mail_id)) {
          // 从API获取邮件数据
          const response = await fetch(`/api/mail/${this.currentFolder}/mails/${mailId}`);
          const result = await response.json();
          
          if (result.code === 'code.ok') {
            this.mail = result.data;
            console.log('Fetched mail data:', this.mail);
            // 更新sessionStorage
            sessionStorage.setItem('currentMail', JSON.stringify(this.mail));
          } else {
            this.showToastMessage(`获取邮件失败: ${result.message}`, 'error');
          }
        }
        
        // 获取附件信息
        if (this.mail.attachmentIds && this.mail.attachmentIds.length > 0) {
          await this.fetchAttachmentsInfo();
        }
      } catch (error) {
        console.error('获取邮件数据出错:', error);
        this.showToastMessage('获取邮件数据失败，请检查网络连接', 'error');
      } finally {
        this.isLoading = false;
      }
    },
    
    // 参考view-mail.html处理附件的方式
    async fetchAttachmentsInfo() {
      try {
        // 如果没有附件ID，则直接返回
        if (!this.mail.attachmentIds || this.mail.attachmentIds.length === 0) {
          this.attachments = [];
          return;
        }
        
        // 使用Promise.all并行获取所有附件信息
        const attachmentPromises = this.mail.attachmentIds.map(async attachmentId => {
          try {
            const response = await fetch(`/attachments/${attachmentId}`);
            const result = await response.json();
            
            if (result.code === 'code.ok' && result.data) {
              return { 
                id: attachmentId,
                name: result.data.fileName || `附件-${attachmentId}`
              };
            } else {
              console.warn(`获取附件 ${attachmentId} 信息失败:`, result.message);
              return { id: attachmentId, name: `附件-${attachmentId}` };
            }
          } catch (error) {
            console.error(`获取附件 ${attachmentId} 信息出错:`, error);
            return { id: attachmentId, name: `附件-${attachmentId}` };
          }
        });
        
        this.attachments = await Promise.all(attachmentPromises);
        console.log('附件信息:', this.attachments);
      } catch (error) {
        console.error('获取附件信息失败:', error);
        this.showToastMessage('获取附件信息失败', 'error');
      }
    },
    
    // 返回上一页
    goBack() {
      const from = this.$route.query.from;

      // 如果有来源信息，并且浏览器历史记录存在，最简单的就是直接返回上一页
      if (from && window.history.length > 1) {
        this.$router.back();
        return;
      }

      // 如果无法使用 history.back()，则根据 from 参数进行硬跳转
      if (from === 'star') {
        this.$router.push('/star');
      } else {
        // 默认的回退逻辑，返回到主邮箱页面
        this.$router.push({
          path: '/main',
          query: { folder: this.currentFolder || 'INBOX' }
        });
      }
    },
    
    // 切换星标状态 - 修改后与主页面保持一致
    async toggleStar() {
      if (!this.mail.mail_id) {
        this.showToastMessage('无法操作，邮件ID无效', 'error');
        return;
      }
      
      try {
        // 构建API请求参数
        let starSign;
        if (this.currentFolder === 'INBOX') {
          starSign = 'R_STAR'; // 收件人星标
        } else if (this.currentFolder === 'SENT') {
          starSign = 'S_STAR'; // 发件人星标
        } else {
          this.showToastMessage('当前文件夹不支持星标操作', 'error');
          return;
        }
        
        // 确定操作类型：添加或移除星标
        const operation = this.isStarred ? '-FLAG' : '+FLAG';
        
        // 存储当前操作（添加或移除星标）
        const isAdding = !this.isStarred;
        
        // 使用新的API格式修改星标状态
        const response = await fetch(`/api/mail/${this.currentFolder}/mails/${this.mail.mail_id}/change/${starSign}/${operation}`, {
          method: 'POST'
        });
        
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          // 更新本地邮件的星标状态
          if (this.currentFolder === 'INBOX') {
            this.mail.receiver_star = isAdding ? 1 : 0;
          } else {
            this.mail.sender_star = isAdding ? 1 : 0;
          }
          
          // 更新sessionStorage中的邮件数据
          sessionStorage.setItem('currentMail', JSON.stringify(this.mail));
          
          // 显示成功提示 - 使用存储的操作类型而非计算属性
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
      if (!this.mail.mail_id || this.currentFolder !== 'INBOX') {
        this.showToastMessage('无法操作，邮件ID无效或不在收件箱', 'error');
        return;
      }
      
      try {
        // 确定操作类型：标记已读或未读
        const operation = this.mail.read === 1 ? '-FLAG' : '+FLAG';
        const endpoint = `/api/mail/${this.currentFolder}/mails/${this.mail.mail_id}/change/READ/${operation}`;
        
        const response = await fetch(endpoint, {
          method: 'POST'
        });
        
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          // 更新本地邮件的已读状态
          this.mail.read = this.mail.read === 1 ? 0 : 1;
          
          // 更新sessionStorage中的邮件数据
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
        const endpoint = `/api/mail/${this.currentFolder}/mails/${this.mail.mail_id}/change/TRASH/+FLAG`;
        
        const response = await fetch(endpoint, {
          method: 'POST'
        });
        
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          this.showToastMessage('邮件已移至回收站');
          // 延迟返回，让用户看到提示
          setTimeout(() => this.goBack(), 1500);
        } else {
          this.showToastMessage('移动邮件失败', 'error');
        }
      } catch (error) {
        console.error('移动邮件出错:', error);
        this.showToastMessage('移动邮件失败', 'error');
      }
    },
    
    // 从回收站还原
    async restoreMail() {
      if (!this.mail.mail_id || this.currentFolder !== 'TRASH') {
        this.showToastMessage('无法操作，邮件ID无效或不在回收站', 'error');
        return;
      }
      
      try {
        const endpoint = `/api/mail/${this.currentFolder}/mails/${this.mail.mail_id}/change/TRASH/-FLAG`;
        
        const response = await fetch(endpoint, {
          method: 'POST'
        });
        
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          this.showToastMessage('邮件已还原');
          // 延迟返回，让用户看到提示
          setTimeout(() => this.goBack(), 1500);
        } else {
          this.showToastMessage('还原邮件失败', 'error');
        }
      } catch (error) {
        console.error('还原邮件出错:', error);
        this.showToastMessage('还原邮件失败', 'error');
      }
    },
    
    // 显示删除确认
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
        const response = await fetch(`/api/mail/${this.currentFolder}/mails/${this.mail.mail_id}/delete`, {
          method: 'DELETE'
        });
        
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          this.showDeleteModal = false;
          this.showToastMessage('邮件已永久删除');
          // 延迟返回，让用户看到提示
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

/* 回复按钮样式 */
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

/* 附件样式 - 参考view-mail.html */
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

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
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
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
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