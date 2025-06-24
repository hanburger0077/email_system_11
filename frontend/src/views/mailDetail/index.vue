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
      
      <!-- 邮件内容 - 使用处理后的格式化内容 -->
      <div class="mail-content" v-html="formattedContent"></div>
      
      <!-- 邮件附件 -->
      <div class="attachments-section" v-if="attachments && attachments.length > 0">
        <h4>附件:</h4>
        <div class="attachments-list">
          <div 
            v-for="attachment in attachments" 
            :key="attachment.id" 
            class="attachment-item"
          >
            <a 
              :href="`/attachments/download/${attachment.id}`" 
              class="attachment-link" 
              target="_blank"
            >
              {{ attachment.name }} <!-- 附件名称 -->
            </a>
            <button 
              class="attachment-download-btn" 
              @click="downloadAttachment(attachment.id, attachment.name)"
              :disabled="isDownloading"
            >
              {{ isDownloading ? '下载中...' : '下载' }}
            </button>
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
    
    <!-- 下载错误提示 -->
    <div 
      class="download-error" 
      v-if="downloadError"
    >
      {{ downloadErrorMessage }}
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
      toastTimeout: null,
      downloadError: false,
      downloadErrorMessage: '',
      isDownloading: false
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
    },
    mailId() {
      return this.$route.query.id;
    },
    mailbox() {
      return this.$route.query.mailbox || 'INBOX';
    },
    // 新增计算属性：处理后的格式化内容
    formattedContent() {
      if (!this.mail.content) return '';
      
      // 先检查是否已有HTML标签，避免重复处理
      if (/<[a-z][\s\S]*>/i.test(this.mail.content)) {
        return this.mail.content;
      }
      
      // 纯文本处理：转换换行符为<br>
      let content = this.mail.content;
      
      // 转义HTML特殊字符，防止XSS
      content = content
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
      
      // 转换各种换行符为<br>
      content = content
        .replace(/\r\n/g, '<br>')
        .replace(/\n\r/g, '<br>')
        .replace(/\r/g, '<br>')
        .replace(/\n/g, '<br>');
      
      // 处理连续换行（最多保留2个<br>）
      content = content.replace(/(<br\s*\/?>\s*){3,}/g, '<br><br>');
      
      // 将两个<br>转换为段落
      content = content.replace(/<br\s*\/?>\s*<br\s*\/?>/g, '</p><p>');
      content = `<p>${content}</p>`;
      
      // 转换URL为可点击链接
      content = this.convertUrlsToLinks(content);
      
      return content;
    }
  },
  created() {
    this.initMailData();
  },
  methods: {
    // 转换URL为可点击链接
    convertUrlsToLinks(text) {
      const urlRegex = /(https?:\/\/[^\s<]+|www\.[^\s<]+)/g;
      return text.replace(urlRegex, url => {
        const href = url.startsWith('http') ? url : `http://${url}`;
        return `<a href="${href}" target="_blank" rel="noopener noreferrer">${url}</a>`;
      });
    },
    
    initMailData() {
      this.currentFolder = this.mailbox;
      this.fetchMailData(this.currentFolder, this.mailId);
    },
    
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
          sessionStorage.setItem('currentMail', JSON.stringify(this.mail));
          
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
        
        const quotedContent = `



------------------ 原始邮件 ------------------
发件人: ${originalSender}
发送时间: ${formattedTime}
主题: ${originalSubject}
内容: ${originalContent}
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
    
    formatTime(dateStr) {
      if (!dateStr) return '未知时间';
      
      try {
        const date = new Date(dateStr);
        if (isNaN(date.getTime())) return '未知时间';
        
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
    
    showToastMessage(message, type = 'success') {
      if (this.toastTimeout) clearTimeout(this.toastTimeout);
      
      this.toastMessage = message;
      this.toastType = type;
      this.showToast = true;
      
      this.toastTimeout = setTimeout(() => {
        this.showToast = false;
      }, 3000);
    },
    
    // 优化后的附件获取方法
    // async fetchAttachmentsInfo() {
    //   try {
    //     if (!this.mail.attachmentIds || this.mail.attachmentIds.length === 0) {
    //       this.attachments = [];
    //       return;
    //     }
        
    //     // 并行获取所有附件信息
    //     const attachmentPromises = this.mail.attachmentIds.map(attachmentId => 
    //       this.getAttachmentInfo(attachmentId)
    //         .then(attachmentInfo => ({ 
    //           id: attachmentId, 
    //           // name: attachmentInfo?.fileName || `附件-${attachmentId}` 
    //           name: attachmentInfo.fileName 
    //         }))
    //         .catch(error => {
    //           console.error(`获取附件 ${attachmentId} 信息失败:`, error);
    //           // return { id: attachmentId, name: `附件-${attachmentId}` };
    //           return { id: attachmentId, name: attachmentInfo.fileName  };
    //         })
    //     );
        
    //     this.attachments = await Promise.all(attachmentPromises);
    //   } catch (error) {
    //     console.error('获取附件信息失败:', error);
    //     this.showToastMessage('获取附件信息失败', 'error');
    //   }
    // },
    async fetchAttachmentsInfo() {
      try {
        if (!this.mail.attachmentIds || this.mail.attachmentIds.length === 0) {
          this.attachments = [];
          return;
        }
        
        // 并行获取所有附件信息
        const attachmentPromises = this.mail.attachmentIds.map(async attachmentId => {
          try {
            const attachmentInfo = await this.getAttachmentInfo(attachmentId);
            console.log('附件信息:', attachmentInfo); // 查看完整的附件信息对象
            return { 
              id: attachmentId, 
              // name: attachmentInfo && attachmentInfo.fileName ? attachmentInfo.fileName : `附件-${attachmentId}` 
              name: attachmentInfo && attachmentInfo.fileName 
            };
          } catch (error) {
            console.error(`获取附件 ${attachmentId} 信息失败:`, error);
            return { id: attachmentId, name: `附件-${attachmentId}` };
          }
        });
        
        this.attachments = await Promise.all(attachmentPromises);
      } catch (error) {
        console.error('获取附件信息失败:', error);
        this.showToastMessage('获取附件信息失败', 'error');
      }
    },
    
    // 单独提取的附件信息获取方法，与参考代码风格一致
    async getAttachmentInfo(attachmentId) {
      try {
      console.log(`请求URL: /attachments/${attachmentId}`);
      
      const response = await fetch(`/attachments/${attachmentId}`);
      console.log('响应状态:', response.status, response.statusText);
      console.log('响应头:', [...response.headers.entries()]);
      
      // 尝试获取响应体文本以查看具体内容
      const responseClone = response.clone();
      const responseText = await responseClone.text();
      console.log('响应体:', responseText);
        
        // 检查响应类型
        const contentType = response.headers.get('content-type');
        if (!contentType || !contentType.includes('application/json')) {
          console.error('附件API返回了非JSON响应:', contentType);
          return null;
        }
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          return result.data;
        } else if (result.code === 'code.error') {
          this.showToastMessage(`获取附件信息失败: ${result.message}${result.reason ? ': ' + result.reason : ''}`, 'error');
          return null;
        } else {
          console.error('获取附件信息失败:', result.message);
          return null;
        }
      } catch (error) {
        console.error('获取附件信息出错:', error);
        return null;
      }
    },
    
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
          query: { folder: this.currentFolder || 'INBOX' }
        });
      }
    },
    
    async toggleStar() {
      if (!this.mail.mail_id) {
        this.showToastMessage('无法操作，邮件ID无效', 'error');
        return;
      }
      
      try {
        let starSign;
        if (this.currentFolder === 'INBOX') {
          starSign = 'R_STAR';
        } else if (this.currentFolder === 'SENT') {
          starSign = 'S_STAR';
        } else {
          this.showToastMessage('当前文件夹不支持星标操作', 'error');
          return;
        }
        
        const operation = this.isStarred ? '-FLAG' : '+FLAG';
        const isAdding = !this.isStarred;
        
        const response = await fetch(`/api/mail/${this.currentFolder}/mails/${this.mail.mail_id}/change/${starSign}/${operation}`, {
          method: 'POST'
        });
        
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          if (this.currentFolder === 'INBOX') {
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
    
    async toggleReadStatus() {
      if (!this.mail.mail_id || this.currentFolder !== 'INBOX') {
        this.showToastMessage('无法操作，邮件ID无效或不在收件箱', 'error');
        return;
      }
      
      try {
        const operation = this.mail.read === 1 ? '-FLAG' : '+FLAG';
        const endpoint = `/api/mail/${this.currentFolder}/mails/${this.mail.mail_id}/change/READ/${operation}`;
        
        const response = await fetch(endpoint, {
          method: 'POST'
        });
        
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
          setTimeout(() => this.goBack(), 1500);
        } else {
          this.showToastMessage('移动邮件失败', 'error');
        }
      } catch (error) {
        console.error('移动邮件出错:', error);
        this.showToastMessage('移动邮件失败', 'error');
      }
    },
    
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
          setTimeout(() => this.goBack(), 1500);
        } else {
          this.showToastMessage('还原邮件失败', 'error');
        }
      } catch (error) {
        console.error('还原邮件出错:', error);
        this.showToastMessage('还原邮件失败', 'error');
      }
    },
    
    confirmDelete() {
      this.showDeleteModal = true;
    },
    
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
    },
    
    async downloadAttachment(attachmentId, fileName) {
      this.downloadError = false;
      this.isDownloading = true;
      
      try {
        const response = await fetch(`/attachments/download/${attachmentId}`);
        
        if (!response.ok) {
          throw new Error(`下载失败: ${response.statusText}`);
        }
        
        let suggestedFileName = fileName;
        const contentDisposition = response.headers.get('Content-Disposition');
        // if (contentDisposition) {
        //   const fileNameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
        //   if (fileNameMatch && fileNameMatch[1]) {
        //     suggestedFileName = fileNameMatch[1].replace(/['"]/g, '');
        //   }
        // }
        
        // if (!suggestedFileName) {
        //   suggestedFileName = `附件-${attachmentId}`;
        // }
        
        const blob = await response.blob();
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = suggestedFileName;
        
        document.body.appendChild(link);
        link.click();
        
        setTimeout(() => {
          document.body.removeChild(link);
          window.URL.revokeObjectURL(url);
        }, 100);
        
        this.showToastMessage('下载已开始');
      } catch (error) {
        console.error('下载附件出错:', error);
        this.downloadError = true;
        this.downloadErrorMessage = `下载失败: ${error.message}`;
      } finally {
        this.isLoading = false;
        this.isDownloading = false;
      }
    }
  },
  beforeDestroy() {
    if (this.toastTimeout) clearTimeout(this.toastTimeout);
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
  line-height: 1.8;
  color: #303133;
  word-wrap: break-word;
  overflow-wrap: break-word;
  padding-bottom: 20px;
}

.mail-content p {
  margin-bottom: 16px;
}

.mail-content pre {
  white-space: pre-wrap;
  background-color: #f9f9f9;
  padding: 10px;
  border-radius: 4px;
  overflow: auto;
  margin-bottom: 16px;
}

.mail-content a {
  color: #409EFF;
  text-decoration: underline;
  transition: color 0.2s;
}

.mail-content a:hover {
  color: #66b1ff;
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

.attachments-section {
  margin-top: 20px;
  padding: 15px;
  background-color: #f0f8ff;
  border-radius: 5px;
}

.attachments-list {
  margin-top: 10px;
}

.attachment-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px dashed #ddd;
}

.attachment-link {
  flex-grow: 1;
  margin-right: 10px;
  color: #409EFF;
  text-decoration: underline;
  word-break: break-all;
}

.attachment-download-btn {
  padding: 4px 10px;
  background-color: #409EFF;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background-color 0.2s;
}

.attachment-download-btn:disabled {
  background-color: #90caf9;
  cursor: not-allowed;
}

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

.download-error {
  margin-top: 10px;
  padding: 8px 12px;
  background-color: #ffebee;
  color: #c62828;
  border-radius: 4px;
  font-size: 12px;
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