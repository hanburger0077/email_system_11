<template>
  <div class="mail-detail">
    <div v-if="isLoading" class="loading-indicator">
      <div class="loader"></div>
      <p>加载中，请稍候...</p>
    </div>

    <template v-else>
      <!-- 邮件操作栏 -->
      <div class="mail-actions">
        <el-tooltip content="返回" placement="bottom">
          <button class="toolbar-button" @click="goBack">
            <img src="./assets/back.png" alt="返回" />
          </button>
        </el-tooltip>
        
        <el-tooltip content="回复" placement="bottom">
          <button class="toolbar-button" @click="replyMail">
            <img src="./assets/reply.png" alt="回复" />
          </button>
        </el-tooltip>
        
        <!-- 根据文件夹类型显示不同操作按钮 -->
        <template v-if="currentFolder === 'INBOX' || currentFolder === 'JUNK'">
          <el-tooltip content="移至回收站" placement="bottom">
            <button class="toolbar-button" @click="moveToTrash">
              <img src="./assets/delete.png" alt="移至回收站" />
            </button>
          </el-tooltip>
        </template>
        
        <template v-if="currentFolder === 'TRASH'">
          <el-tooltip content="还原邮件" placement="bottom">
            <button class="toolbar-button" @click="restoreMail">
              <img src="./assets/reply.png" alt="还原邮件" style="transform: scaleX(-1);" />
            </button>
          </el-tooltip>
        </template>
        
        <!-- 永久删除按钮 -->
        <el-tooltip content="永久删除" placement="bottom">
          <button class="toolbar-button delete-button" @click="confirmDelete">
            <span class="delete-icon-text">❌</span>
          </button>
        </el-tooltip>
        
        <!-- 星标按钮 -->
        <template v-if="!isStarred">
          <el-tooltip content="加注星标" placement="bottom">
            <button class="toolbar-button" @click="toggleStar">
              <img src="./assets/star.png" alt="加注星标" />
            </button>
          </el-tooltip>
        </template>
        <template v-else>
          <el-tooltip content="取消星标" placement="bottom">
            <button class="toolbar-button" @click="toggleStar">
              <img src="./assets/unstar.png" alt="取消星标" />
            </button>
          </el-tooltip>
        </template>
        
        <!-- 收件箱才显示已读/未读切换 -->
        <template v-if="currentFolder === 'INBOX'">
          <!-- '标为已读' 按钮 -->
          <el-tooltip v-if="mail.read !== 1" content="标为已读" placement="bottom">
            <button class="toolbar-button" @click="toggleReadStatus">
              <img src="./assets/read.png" alt="标为已读" />
            </button>
          </el-tooltip>
          
          <!-- '标为未读' 按钮 -->
          <el-tooltip v-if="mail.read === 1" content="标为未读" placement="bottom">
            <button class="toolbar-button" @click="toggleReadStatus">
              <img src="./assets/unread.png" alt="标为未读" />
            </button>
          </el-tooltip>
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
import { ElTooltip, ElMessageBox } from 'element-plus';

export default {
  components: {
    ElTooltip,
  },
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
      ElMessageBox.confirm(
        '确认永久删除该邮件吗?',
        '警告',
        {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
        }
      )
      .then(() => {
        this.deleteMail();
      })
      .catch(() => {
        // 用户点击了取消
      });
    },
    
    async deleteMail() {
      try {
        const response = await fetch(`/api/mail/delete/${this.mailId}`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          }
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
          this.showToastMessage('邮件已永久删除', 'success');
          setTimeout(() => {
            this.goBack(); // 返回上一页
          }, 1500);
        } else {
          throw new Error(result.message || '删除失败');
        }
      } catch (error) {
        this.showToastMessage(`删除失败: ${error.message}`, 'error');
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
  padding: 25px;
  background-color: #ffffff;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  max-width: 1000px;
  margin: 20px auto;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  color: #333;
  line-height: 1.6;
  position: relative;
  overflow: hidden;
}

.loading-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  font-size: 1.2em;
  color: #555;
}

.loader {
  border: 5px solid #f3f3f3;
  border-top: 5px solid #3498db;
  border-radius: 50%;
  width: 50px;
  height: 50px;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.mail-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 25px;
  padding-bottom: 20px;
  border-bottom: 2px solid #f0f0f0;
  align-items: center;
}

.toolbar-button {
  background-color: #fff;
  border: 1px solid #dcdfe6;
  width: 32px;
  height: 32px;
  padding: 0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
  box-sizing: border-box;
  overflow: hidden;
}

.toolbar-button:hover:not(:disabled) {
  background-color: #f5f7fa;
  border-color: #c0c4cc;
}

.toolbar-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.toolbar-button img {
  width: 28px;
  height: 28px;
  object-fit: contain;
}

.delete-button {
  border-color: #fde2e2;
  background-color: #fef0f0;
}

.delete-button:hover:not(:disabled) {
  border-color: #f56c6c;
  background-color: #f56c6c;
}

.delete-button:hover:not(:disabled) .delete-icon-text {
  color: white;
}

.delete-icon-text {
  color: #f56c6c;
  font-size: 16px;
  line-height: 1;
  transition: color 0.2s;
}

.mail-header-info {
  margin-bottom: 20px;
}

.mail-subject {
  font-size: 2em;
  font-weight: bold;
  margin-bottom: 15px;
  color: #2c3e50;
}

.mail-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 15px;
  background-color: #f9f9f9;
  border-radius: 6px;
  font-size: 0.95em;
}

.mail-meta > div {
  display: flex;
  align-items: center;
}

.mail-meta .label {
  font-weight: bold;
  color: #555;
  width: 70px;
}

.mail-meta .value {
  color: #333;
}

.mail-content {
  padding: 20px 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-size: 1.05em;
  color: #333;
  line-height: 1.8;
}

.attachments-section {
  margin-top: 25px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.attachments-section h4 {
  margin-bottom: 15px;
  font-size: 1.2em;
  color: #444;
}

.attachments-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.attachment-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 5px;
  transition: background-color 0.2s;
}

.attachment-item:hover {
  background-color: #f0f0f0;
}

.attachment-link {
  font-weight: 500;
  color: #007bff;
  text-decoration: none;
}

.attachment-link:hover {
  text-decoration: underline;
}

.attachment-download-btn {
  padding: 5px 12px;
  border: 1px solid #ccc;
  border-radius: 4px;
  background-color: #fff;
  cursor: pointer;
}

.attachment-download-btn:hover {
  background-color: #f0f0f0;
}

.attachment-download-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.toast-message {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 12px 20px;
  border-radius: 6px;
  color: #fff;
  z-index: 1001;
  font-size: 1em;
}

.toast-message.success {
  background-color: #67c23a;
}

.toast-message.error {
  background-color: #f56c6c;
}

.download-error {
  position: fixed;
  bottom: 20px;
  left: 50%;
  transform: translateX(-50%);
  background-color: #f56c6c;
  color: white;
  padding: 12px 25px;
  border-radius: 6px;
  z-index: 1002;
  box-shadow: 0 4px 10px rgba(0,0,0,0.2);
}
</style>