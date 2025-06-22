<template>
  <div class="starred-mails-page">
    <!-- 顶部功能栏（参照 main 页面样式） -->
    <div class="mail-toolbar">
      <div class="toolbar-left">
        <el-checkbox 
          v-model="allSelected" 
          @change="toggleSelectAll" 
          class="select-all-checkbox">
        </el-checkbox>
        <button class="toolbar-button delete-button" @click="deleteSelected">删除</button>
        <button class="toolbar-button delete-all-button" @click="deleteAll">全部删除</button>
        <button class="toolbar-button cancel-star-button" @click="cancelSelectedStars">取消星标</button>
        <button class="toolbar-button refresh-button" @click="refreshMails">
          <i class="refresh-icon">⟳</i> 刷新
        </button>
      </div>
      <div class="toolbar-right">
        <div class="mail-info">
          邮件总数：{{ totalEmails }} &nbsp;&nbsp; 页数：{{ currentPage }} / {{ totalPages }}
        </div>
      </div>
    </div>
    
    <!-- 加载状态指示器 -->
    <div v-if="isLoading" class="loading-overlay">
      <div class="spinner"></div>
      <p>正在加载星标邮件...</p>
    </div>

    <!-- 接收星标邮件区域 -->
    <div class="mail-section">
      <h2>接收星标邮件</h2>
      <div v-for="group in groupedReceivedMails" :key="group.title" class="mail-group">
        <h3 class="group-title" @click="toggleExpand('received', group.title)">
          {{ group.title }} <span class="expand-icon">{{ group.isExpanded ? '−' : '+' }}</span>
        </h3>
        <div v-show="group.isExpanded" class="mail-items">
          <div v-if="group.mails.length > 0" v-for="mail in group.mails" :key="mail.mail_id" class="mail-item" :class="{ 'unread': mail.read === 0 }">
            <div class="checkbox-container">
              <el-checkbox v-model="selectedReceived" :value="mail.mail_id" class="item-checkbox" />
            </div>
            <div class="mail-content" @click="openMail(mail, 'INBOX')">
              <span class="column sender">{{ formatSender(mail.sender_email) }}</span>
              <span class="column subject">{{ mail.subject }}</span>
              <span class="column time">{{ formatTime(mail.create_at) }}</span>
              <!-- 固定显示实心星，点击取消星标 -->
              <span class="star-icon star-filled" @click.stop="toggleStarReceived(mail)">★</span>
            </div>
          </div>
          <div v-else class="empty-message">
            当前无星标邮件
          </div>
        </div>
      </div>
    </div>
    <!-- 发送星标邮件区域 -->
    <div class="mail-section" style="margin-top: 30px;">
      <h2>发送星标邮件</h2>
      <div v-for="group in groupedSentMails" :key="group.title" class="mail-group">
        <h3 class="group-title" @click="toggleExpand('sent', group.title)">
          {{ group.title }} <span class="expand-icon">{{ group.isExpanded ? '−' : '+' }}</span>
        </h3>
        <div v-show="group.isExpanded" class="mail-items">
          <div v-if="group.mails.length > 0" v-for="mail in group.mails" :key="mail.mail_id" class="mail-item">
            <div class="checkbox-container">
              <el-checkbox v-model="selectedSent" :value="mail.mail_id" class="item-checkbox" />
            </div>
            <div class="mail-content" @click="openMail(mail, 'SENT')">
              <span class="column receiver">{{ mail.receiver_email }}</span>
              <span class="column subject">{{ mail.subject }}</span>
              <span class="column time">{{ formatTime(mail.create_at) }}</span>
              <!-- 固定显示实心星，点击取消星标 -->
              <span class="star-icon star-filled" @click.stop="toggleStarSent(mail)">★</span>
            </div>
          </div>
          <div v-else class="empty-message">
            当前无星标邮件
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'StarredMailsPage',
  data() {
    return {
      receivedStarred: [],
      sentStarred: [],
      groupedReceivedMails: [],
      groupedSentMails: [],
      selectedReceived: [],
      selectedSent: [],
      allSelected: false,
      isLoading: false,
      loadError: false,
      loadErrorMessage: '',
      retryCount: 0,
      maxRetries: 3,
      pollingTimer: null,
      pollingCount: 0,
      maxPolls: 3,
      // 模拟分页信息，可根据实际情况从接口中获取
      currentPage: 1,
      totalPages: 1
    };
  },
  computed: {
    totalEmails() {
      return this.receivedStarred.length + this.sentStarred.length;
    }
  },
  methods: {
    formatSender(sender) {
      if (typeof sender === 'string') {
        return sender.replace(/^\d+[\.\s]+/, '');
      }
      return sender;
    },
    formatTime(dateStr) {
      if (!dateStr) return '未知时间';
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) return '未知时间';
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}`;
    },
    
    // 新增函数 - 统一加载所有星标邮件
    async loadAllStarredMails() {
      this.isLoading = true;
      this.loadError = false;
      
      try {
        // 并行请求接收和发送的星标邮件，提高加载效率
        await Promise.all([
          this.fetchReceivedStarred(),
          this.fetchSentStarred()
        ]);
        
        // 如果加载成功，重置重试计数和轮询计数
        this.retryCount = 0;
        this.pollingCount = 0;
        
        // 如果数据为空，设置轮询
        if (this.receivedStarred.length === 0 && this.sentStarred.length === 0) {
          this.setupPolling();
        }
      } catch (error) {
        console.error('加载星标邮件出错:', error);
        this.handleLoadError('加载星标邮件失败，请检查网络连接');
      } finally {
        this.isLoading = false;
      }
    },
    
    // 拆分请求和处理逻辑
    async fetchReceivedStarred() {
      const response = await fetch(`/api/mail/INBOX/pages/1/search?receiver_star=1`);
      if (!response.ok) {
        throw new Error(`HTTP错误 ${response.status}`);
      }
      const result = await response.json();
      
      if (result.code === 'code.ok') {
        this.receivedStarred = result.data || [];
        this.groupedReceivedMails = [{ 
          title: '接收星标邮件', 
          isExpanded: true, 
          mails: this.receivedStarred 
        }];
        
        console.log('成功加载接收星标邮件:', this.receivedStarred.length);
      } else {
        throw new Error(result.reason || result.message);
      }
    },
    
    // 同样拆分发送星标邮件的请求处理
    async fetchSentStarred() {
      const response = await fetch(`/api/mail/SENT/pages/1/search?sender_star=1`);
      if (!response.ok) {
        throw new Error(`HTTP错误 ${response.status}`);
      }
      const result = await response.json();
      
      if (result.code === 'code.ok') {
        this.sentStarred = result.data || [];
        this.groupedSentMails = [{ 
          title: '发送星标邮件', 
          isExpanded: true, 
          mails: this.sentStarred 
        }];
        
        console.log('成功加载发送星标邮件:', this.sentStarred.length);
      } else {
        throw new Error(result.reason || result.message);
      }
    },
    
    // 添加轮询机制
    setupPolling() {
      // 清除可能存在的旧定时器
      if (this.pollingTimer) {
        clearTimeout(this.pollingTimer);
      }
      
      // 如果没有加载到数据并且还未达到最大轮询次数
      if ((this.receivedStarred.length === 0 || this.sentStarred.length === 0) && 
          this.pollingCount < this.maxPolls) {
        console.log(`设置轮询 #${this.pollingCount + 1}...`);
        this.pollingTimer = setTimeout(() => {
          console.log(`自动轮询 #${this.pollingCount + 1}：尝试重新加载星标邮件`);
          this.pollingCount++;
          this.loadAllStarredMails();
        }, 2000); // 2秒后尝试再次加载
      }
    },
    
    // 处理加载错误
    handleLoadError(message) {
      this.loadError = true;
      this.loadErrorMessage = message;
      console.error(message);
      
      // 自动重试机制（最多重试3次）
      if (this.retryCount < this.maxRetries) {
        console.log(`加载失败，将在2秒后自动重试 (${this.retryCount + 1}/${this.maxRetries})...`);
        this.retryCount++;
        setTimeout(() => {
          this.loadAllStarredMails();
        }, 2000); // 2秒后自动重试
      }
    },
    
    // 手动刷新按钮的处理函数
    refreshMails() {
      // 重置计数器
      this.retryCount = 0;
      this.pollingCount = 0;
      
      // 清除定时器
      if (this.pollingTimer) {
        clearTimeout(this.pollingTimer);
        this.pollingTimer = null;
      }
      
      // 重新加载数据
      this.loadAllStarredMails();
      this.$message.success('正在刷新邮件...');
    },
    
    toggleExpand(type, groupTitle) {
      if (type === 'received') {
        const group = this.groupedReceivedMails.find(g => g.title === groupTitle);
        if (group) group.isExpanded = !group.isExpanded;
      } else if (type === 'sent') {
        const group = this.groupedSentMails.find(g => g.title === groupTitle);
        if (group) group.isExpanded = !group.isExpanded;
      }
    },
    
    // 取消接收邮件的星标：直接发送取消请求
    async toggleStarReceived(mail) {
      try {
        const response = await fetch(`/api/mail/INBOX/mails/${mail.mail_id}/change/R_STAR/-FLAG`, { method: 'POST' });
        const result = await response.json();
        if (result.code === 'code.ok') {
          // 更新星标状态，或者直接重新加载数据
          this.$message.success('已取消星标');
          
          // 直接从当前列表中移除该邮件，无需重新请求
          this.receivedStarred = this.receivedStarred.filter(m => m.mail_id !== mail.mail_id);
          this.groupedReceivedMails = [{ 
            title: '接收星标邮件', 
            isExpanded: true, 
            mails: this.receivedStarred 
          }];
          
          // 同时从选中列表中移除
          this.selectedReceived = this.selectedReceived.filter(id => id !== mail.mail_id);
        } else {
          this.$message.error(`操作失败: ${result.reason || result.message}`);
        }
      } catch (error) {
        console.error('取消接收星标失败:', error);
        this.$message.error('取消接收星标失败');
      }
    },
    
    // 取消发送邮件的星标：直接发送取消请求
    async toggleStarSent(mail) {
      try {
        const response = await fetch(`/api/mail/SENT/mails/${mail.mail_id}/change/S_STAR/-FLAG`, { method: 'POST' });
        const result = await response.json();
        if (result.code === 'code.ok') {
          this.$message.success('已取消星标');
          
          // 直接从当前列表中移除该邮件，无需重新请求
          this.sentStarred = this.sentStarred.filter(m => m.mail_id !== mail.mail_id);
          this.groupedSentMails = [{ 
            title: '发送星标邮件', 
            isExpanded: true, 
            mails: this.sentStarred 
          }];
          
          // 同时从选中列表中移除
          this.selectedSent = this.selectedSent.filter(id => id !== mail.mail_id);
        } else {
          this.$message.error(`操作失败: ${result.reason || result.message}`);
        }
      } catch (error) {
        console.error('取消发送星标失败:', error);
        this.$message.error('取消发送星标失败');
      }
    },
    
    async deleteSelected() {
      for (const mailId of this.selectedReceived) {
        try {
          const response = await fetch(`/api/mail/INBOX/mails/${mailId}/delete`, { method: 'DELETE' });
          const result = await response.json();
          if (result.code !== 'code.ok') {
            this.$message.error(`删除邮件 ${mailId} 失败：${result.reason || result.message}`);
          }
        } catch (error) {
          console.error(`删除邮件 ${mailId} 出错:`, error);
          this.$message.error(`删除邮件 ${mailId} 出错`);
        }
      }
      
      for (const mailId of this.selectedSent) {
        try {
          const response = await fetch(`/api/mail/SENT/mails/${mailId}/delete`, { method: 'DELETE' });
          const result = await response.json();
          if (result.code !== 'code.ok') {
            this.$message.error(`删除邮件 ${mailId} 失败：${result.reason || result.message}`);
          }
        } catch (error) {
          console.error(`删除邮件 ${mailId} 出错:`, error);
          this.$message.error(`删除邮件 ${mailId} 出错`);
        }
      }
      
      this.$message.success('选中邮件删除成功');
      
      // 从本地列表中移除已删除的邮件
      this.receivedStarred = this.receivedStarred.filter(mail => !this.selectedReceived.includes(mail.mail_id));
      this.sentStarred = this.sentStarred.filter(mail => !this.selectedSent.includes(mail.mail_id));
      
      // 更新分组数据
      this.groupedReceivedMails = [{ title: '接收星标邮件', isExpanded: true, mails: this.receivedStarred }];
      this.groupedSentMails = [{ title: '发送星标邮件', isExpanded: true, mails: this.sentStarred }];
      
      this.selectedReceived = [];
      this.selectedSent = [];
      this.allSelected = false;
    },
    
    async deleteAll() {
      if (!confirm('确定要删除所有星标邮件吗？')) return;
      
      for (const mail of this.receivedStarred) {
        try {
          const response = await fetch(`/api/mail/INBOX/mails/${mail.mail_id}/delete`, { method: 'DELETE' });
          const result = await response.json();
          if (result.code !== 'code.ok') {
            this.$message.error(`删除邮件 ${mail.mail_id} 失败：${result.reason || result.message}`);
          }
        } catch (error) {
          console.error(`删除邮件 ${mail.mail_id} 出错:`, error);
          this.$message.error(`删除邮件 ${mail.mail_id} 出错`);
        }
      }
      
      for (const mail of this.sentStarred) {
        try {
          const response = await fetch(`/api/mail/SENT/mails/${mail.mail_id}/delete`, { method: 'DELETE' });
          const result = await response.json();
          if (result.code !== 'code.ok') {
            this.$message.error(`删除邮件 ${mail.mail_id} 失败：${result.reason || result.message}`);
          }
        } catch (error) {
          console.error(`删除邮件 ${mail.mail_id} 出错:`, error);
          this.$message.error(`删除邮件 ${mail.mail_id} 出错`);
        }
      }
      
      this.$message.success('全部邮件删除成功');
      this.selectedReceived = [];
      this.selectedSent = [];
      this.allSelected = false;
      
      // 清空数据
      this.receivedStarred = [];
      this.sentStarred = [];
      this.groupedReceivedMails = [{ title: '接收星标邮件', isExpanded: true, mails: [] }];
      this.groupedSentMails = [{ title: '发送星标邮件', isExpanded: true, mails: [] }];
    },
    
    async cancelSelectedStars() {
      for (const mailId of this.selectedReceived) {
        const mail = this.receivedStarred.find(m => m.mail_id === mailId);
        if (mail && mail.receiver_star === 1) {
          try {
            const response = await fetch(`/api/mail/INBOX/mails/${mail.mail_id}/change/R_STAR/-FLAG`, { method: 'POST' });
            const result = await response.json();
            if (result.code !== 'code.ok') {
              this.$message.error(`取消邮件 ${mail.mail_id} 星标失败：${result.reason || result.message}`);
            }
          } catch (error) {
            console.error(`取消邮件 ${mail.mail_id} 星标出错:`, error);
            this.$message.error(`取消邮件 ${mail.mail_id} 星标出错`);
          }
        }
      }
      
      for (const mailId of this.selectedSent) {
        const mail = this.sentStarred.find(m => m.mail_id === mailId);
        if (mail && mail.sender_star === 1) {
          try {
            const response = await fetch(`/api/mail/SENT/mails/${mail.mail_id}/change/S_STAR/-FLAG`, { method: 'POST' });
            const result = await response.json();
            if (result.code !== 'code.ok') {
              this.$message.error(`取消邮件 ${mail.mail_id} 星标失败：${result.reason || result.message}`);
            }
          } catch (error) {
            console.error(`取消邮件 ${mail.mail_id} 星标出错:`, error);
            this.$message.error(`取消邮件 ${mail.mail_id} 星标出错`);
          }
        }
      }
      
      this.$message.success('选中邮件取消星标成功');
      
      // 从当前列表中移除已取消星标的邮件
      this.receivedStarred = this.receivedStarred.filter(mail => !this.selectedReceived.includes(mail.mail_id));
      this.sentStarred = this.sentStarred.filter(mail => !this.selectedSent.includes(mail.mail_id));
      
      // 更新分组数据
      this.groupedReceivedMails = [{ title: '接收星标邮件', isExpanded: true, mails: this.receivedStarred }];
      this.groupedSentMails = [{ title: '发送星标邮件', isExpanded: true, mails: this.sentStarred }];
      
      this.selectedReceived = [];
      this.selectedSent = [];
      this.allSelected = false;
    },
    
    toggleSelectAll() {
      if (this.allSelected) {
        // 全选：将所有当前邮件id加入
        this.selectedReceived = this.receivedStarred.map(mail => mail.mail_id);
        this.selectedSent = this.sentStarred.map(mail => mail.mail_id);
      } else {
        // 清空选中
        this.selectedReceived = [];
        this.selectedSent = [];
      }
    },
    
    openMail(mail, mailbox) {
      sessionStorage.setItem('currentMail', JSON.stringify(mail));
      this.$router.push({
        path: '/mail-detail',
        query: { id: mail.mail_id, mailbox: mailbox, from: 'star' }
      });
    }
  },
  
  // 组件生命周期钩子
  created() {
    // 在 created 生命周期就开始加载，而不是等到 mounted
    this.loadAllStarredMails();
  },
  
  // 保留 mounted 以确保兼容性
  mounted() {
    // 如果 created 中的请求还未完成，不要重复请求
    if (!this.isLoading && this.receivedStarred.length === 0 && this.sentStarred.length === 0) {
      this.loadAllStarredMails();
    }
  },
  
  // 组件销毁前清理定时器
  beforeDestroy() {
    if (this.pollingTimer) {
      clearTimeout(this.pollingTimer);
    }
  }
};
</script>

<style scoped>
.starred-mails-page {
  padding: 20px 28px;
  font-family: Arial, sans-serif;
  height: calc(100vh - 48px);
  overflow-y: auto;
}
/* 顶部功能栏，参照 main 页面 */
.mail-toolbar {
  padding: 15px 20px;
  border-bottom: 1px solid #e6f2fb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8faff;
  margin-bottom: 14px;
  border-radius: 6px;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.toolbar-right {
  display: flex;
  align-items: center;
}
.toolbar-button {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: #f0f0f0;
  cursor: pointer;
  height: 32px;
  line-height: 16px;
  display: flex;
  align-items: center;
}
.toolbar-button:hover {
  background-color: #e0e0e0;
}
/* 删除和取消星标按钮背景采用底色，即默认背景 */
.delete-button {
  background-color: #f0f0f0;
  color: #444;
  border-color: #ddd;
}
.delete-button:hover {
  background-color: #e0e0e0;
}
.delete-all-button {
  background-color: #f56c6c;
  color: #fff;
  border-color: #f56c6c;
}
.delete-all-button:hover {
  background-color: #e64242;
}
.cancel-star-button {
  background-color: #f0f0f0;
  color: #444;
  border-color: #ddd;
}
.cancel-star-button:hover {
  background-color: #e0e0e0;
}
.refresh-button {
  background-color: #f0f0f0;
  color: #444;
  border-color: #ddd;
}
.refresh-button:hover {
  background-color: #e0e0e0;
}
.refresh-icon {
  font-style: normal;
  margin-right: 4px;
  font-size: 14px;
  display: inline-block;
}
.mail-info {
  font-size: 14px;
  color: #666;
}
.mail-group {
  margin-bottom: 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.03);
  padding: 16px;
}
.group-title {
  font-weight: bold;
  color: #1f74c0;
  margin: 0 0 16px 0;
  font-size: 17px;
  border-bottom: 2px solid #cce2fa;
  padding-bottom: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}
.expand-icon {
  font-size: 1.2em;
  color: #666;
}
.mail-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid #eee;
  transition: background-color 0.2s;
}
.mail-item:hover {
  background-color: #f5f7fa;
}
.mail-content {
  flex-grow: 1;
  display: flex;
  align-items: center;
  gap: 20px;
  cursor: pointer;
}
.column {
  display: flex;
  align-items: center;
}
.sender, .receiver {
  min-width: 180px;
  color: #666;
  font-size: 0.9em;
  text-align: left;
}
.subject {
  flex-grow: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: left;
}
.time {
  min-width: 120px;
  text-align: right;
  color: #999;
  font-size: 0.85em;
}
.star-icon {
  font-size: 1.2em;
  margin-left: 8px;
  cursor: pointer;
  color: #999;
  transition: color 0.2s;
}
.star-filled {
  color: #ffc107;
}
.star-icon:hover {
  color: #ffc107;
}
.empty-message {
  text-align: center;
  padding: 20px;
  color: #999;
}
.checkbox-container {
  min-width: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}
/* 小正方形全选复选框 */
.select-all-checkbox {
  width: 16px;
  height: 16px;
}

/* 加载状态样式 */
.loading-overlay {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: rgba(255, 255, 255, 0.9);
  padding: 20px 30px;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.spinner {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #3498db;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  animation: spin 1s linear infinite;
  margin-bottom: 12px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>