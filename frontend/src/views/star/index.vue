<template>
  <div class="starred-mails-page">
    <!-- 顶部功能栏 -->
    <div class="mail-toolbar">
      <div class="toolbar-left">
        <el-checkbox 
          v-model="allSelected" 
          @change="toggleSelectAll" 
          class="select-all-checkbox"
          :disabled="isLoading"
        >
        </el-checkbox>

        <el-tooltip content="删除" placement="bottom">
          <button 
            class="toolbar-button delete-button" 
            @click="deleteSelected" 
            :disabled="(selectedReceived.length === 0 && selectedSent.length === 0) || isLoading"
          >
            <img src="../main/assets/mark5.png" alt="删除" />
          </button>
        </el-tooltip>

        <el-tooltip content="取消星标" placement="bottom">
          <button 
            class="toolbar-button cancel-star-button" 
            @click="cancelSelectedStars" 
            :disabled="(selectedReceived.length === 0 && selectedSent.length === 0) || isLoading"
          >
            取消星标
          </button>
        </el-tooltip>

        <el-tooltip content="刷新" placement="bottom">
          <button 
            class="toolbar-button" 
            @click="refreshMails" 
            :disabled="isLoading"
          >
            <img src="../main/assets/mark3.png" alt="刷新" />
          </button>
        </el-tooltip>
        
        <el-tooltip content="全部删除" placement="bottom">
          <el-button 
            class="delete-all-button" 
            @click="deleteAll" 
            :disabled="(receivedStarred.length === 0 && sentStarred.length === 0) || isLoading"
          >
            全部删除
          </el-button>
        </el-tooltip>
      </div>
      <div class="toolbar-right">
        <span class="mail-count">{{ isLoading ? '0' : totalEmails }} 封邮件</span>
        <span class="page-info">{{ isLoading ? '暂无分页' : `${currentPage}/${totalPages}页` }}</span>
        <div class="pagination-controls">
          <el-button 
            size="small" 
            :disabled="currentPage <= 1 || isLoading"
            @click="prevPage"
          >
            上一页
          </el-button>
          <el-button 
            size="small" 
            :disabled="currentPage >= totalPages || totalPages === 0 || isLoading"
            @click="nextPage"
          >
            下一页
          </el-button>
        </div>
      </div>
    </div>
    
    <!-- 表头 -->
    <div class="mail-header">
      <span class="column checkbox-col"></span>
      <span class="column sender">发件人/收件人</span>
      <span class="column subject">主题</span>
      <span class="column time">时间</span>
      <span class="column star-col">星标</span>
    </div>
    
    <!-- 接收星标邮件区域 -->
    <div class="mail-section" v-if="!isLoading">
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
            </div>
            <!-- 固定显示实心星，点击取消星标 -->
            <span class="star-icon star-filled" @click.stop="toggleStarReceived(mail)">★</span>
          </div>
          <div v-else class="empty-message">
            当前无接收星标邮件
          </div>
        </div>
      </div>
    </div>
    
    <!-- 发送星标邮件区域 -->
    <div class="mail-section" v-if="!isLoading" style="margin-top: 30px;">
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
            </div>
            <!-- 固定显示实心星，点击取消星标 -->
            <span class="star-icon star-filled" @click.stop="toggleStarSent(mail)">★</span>
          </div>
          <div v-else class="empty-message">
            当前无发送星标邮件
          </div>
        </div>
      </div>
    </div>
    
    <!-- 加载状态指示器 -->
    <div v-if="isLoading" class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
    
    <!-- 无数据显示 -->
    <div v-if="!isLoading && receivedStarred.length === 0 && sentStarred.length === 0" class="empty-message global-empty">
      当前未找到任何星标邮件
    </div>
  </div>
</template>

<script>
import { Loading } from '@element-plus/icons-vue'

export default {
  name: 'StarredMailsPage',
  components: {
    Loading
  },
  data() {
    return {
      receivedStarred: [],
      sentStarred: [],
      groupedReceivedMails: [
        { title: "接收星标邮件", isExpanded: true, mails: [] }
      ],
      groupedSentMails: [
        { title: "发送星标邮件", isExpanded: true, mails: [] }
      ],
      selectedReceived: [],
      selectedSent: [],
      allSelected: false,
      isLoading: false,
      currentPage: 1,
      totalPages: 1,
      errorMessage: '',
      lastRoute: null
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
      return sender || '';
    },
    
    formatTime(dateStr) {
      if (!dateStr) return '未知时间';
      
      try {
        let date;
        if (typeof dateStr === 'number') {
          date = new Date(dateStr);
        } else if (typeof dateStr === 'string') {
          date = new Date(dateStr);
        } else {
          return '未知时间';
        }
        
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
    
    async prevPage() {
      if (this.currentPage > 1) {
        this.currentPage--;
        await this.loadAllStarredMails();
      }
    },
    
    async nextPage() {
      if (this.currentPage < this.totalPages) {
        this.currentPage++;
        await this.loadAllStarredMails();
      }
    },
    
    async loadAllStarredMails() {
      this.isLoading = true;
      console.log('正在加载星标邮件，页码:', this.currentPage);
      
      try {
        const results = await Promise.allSettled([
          this.loadReceivedStarred(),
          this.loadSentStarred()
        ]);
        
        const allFailed = results.every(result => result.status === 'rejected');
        if (allFailed) {
          throw new Error('所有星标邮件加载失败');
        }
        
        console.log('成功加载星标邮件:',
          '接收:', this.receivedStarred.length,
          '发送:', this.sentStarred.length
        );
      } catch (error) {
        console.error('加载星标邮件失败:', error);
        this.errorMessage = '加载星标邮件失败，请检查网络连接';
        this.$message.error(this.errorMessage);
      } finally {
        this.isLoading = false;
      }
    },
    
    async loadReceivedStarred() {
      try {
        console.log('开始加载接收星标邮件...');
        const response = await fetch(`/api/mail/INBOX/pages/${this.currentPage}/search?receiver_star=1`);
        
        if (!response.ok) {
          throw new Error(`HTTP错误 ${response.status}`);
        }
        
        const result = await response.json();
        console.log('接收星标邮件响应:', result);
        
        if (result.code === 'code.ok') {
          this.receivedStarred = result.data || [];
          if (result.message && !isNaN(parseInt(result.message, 10))) {
            this.totalPages = Math.max(parseInt(result.message, 10) || 1, 1);
          }
          
          this.groupedReceivedMails = [
            { title: "接收星标邮件", isExpanded: true, mails: this.receivedStarred }
          ];
          return true;
        } else if (result.code === 'code.error') {
          console.warn('接收星标邮件加载警告:', result.message, result.reason);
          if (!this.receivedStarred.length) {
            this.receivedStarred = [];
            this.groupedReceivedMails = [
              { title: "接收星标邮件", isExpanded: true, mails: [] }
            ];
          }
          return false;
        } else {
          throw new Error(result.message || '未知错误');
        }
      } catch (error) {
        console.error('加载接收星标邮件错误:', error);
        if (!this.receivedStarred.length) {
          this.receivedStarred = [];
          this.groupedReceivedMails = [
            { title: "接收星标邮件", isExpanded: true, mails: [] }
          ];
        }
        throw error;
      }
    },
    
    async loadSentStarred() {
      try {
        console.log('开始加载发送星标邮件...');
        const response = await fetch(`/api/mail/SENT/pages/${this.currentPage}/search?sender_star=1`);
        
        if (!response.ok) {
          throw new Error(`HTTP错误 ${response.status}`);
        }
        
        const result = await response.json();
        console.log('发送星标邮件响应:', result);
        
        if (result.code === 'code.ok') {
          this.sentStarred = result.data || [];
          if (result.message && !isNaN(parseInt(result.message, 10))) {
            this.totalPages = Math.max(parseInt(result.message, 10) || 1, this.totalPages);
          }
          
          this.groupedSentMails = [
            { title: "发送星标邮件", isExpanded: true, mails: this.sentStarred }
          ];
          return true;
        } else if (result.code === 'code.error') {
          console.warn('发送星标邮件加载警告:', result.message, result.reason);
          if (!this.sentStarred.length) {
            this.sentStarred = [];
            this.groupedSentMails = [
              { title: "发送星标邮件", isExpanded: true, mails: [] }
            ];
          }
          return false;
        } else {
          throw new Error(result.message || '未知错误');
        }
      } catch (error) {
        console.error('加载发送星标邮件错误:', error);
        if (!this.sentStarred.length) {
          this.sentStarred = [];
          this.groupedSentMails = [
            { title: "发送星标邮件", isExpanded: true, mails: [] }
          ];
        }
        throw error;
      }
    },
    
    refreshMails() {
      this.currentPage = 1;
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
    
    async toggleStarReceived(mail) {
      try {
        const response = await fetch(`/api/mail/INBOX/mails/${mail.mail_id}/change/R_STAR/-FLAG`, { 
          method: 'POST'
        });
        
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          this.$message.success('已取消星标');
          
          this.receivedStarred = this.receivedStarred.filter(m => m.mail_id !== mail.mail_id);
          this.groupedReceivedMails = [
            { title: "接收星标邮件", isExpanded: true, mails: this.receivedStarred }
          ];
          
          this.selectedReceived = this.selectedReceived.filter(id => id !== mail.mail_id);
        } else {
          this.$message.error(`操作失败: ${result.reason || result.message}`);
        }
      } catch (error) {
        console.error('取消接收星标失败:', error);
        this.$message.error('取消接收星标失败');
      }
    },
    
    async toggleStarSent(mail) {
      try {
        const response = await fetch(`/api/mail/SENT/mails/${mail.mail_id}/change/S_STAR/-FLAG`, { 
          method: 'POST'
        });
        
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          this.$message.success('已取消星标');
          
          this.sentStarred = this.sentStarred.filter(m => m.mail_id !== mail.mail_id);
          this.groupedSentMails = [
            { title: "发送星标邮件", isExpanded: true, mails: this.sentStarred }
          ];
          
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
      if (this.selectedReceived.length === 0 && this.selectedSent.length === 0) return;
      
      this.$confirm('确认删除选中的邮件吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        this.isLoading = true;
        
        try {
          for (const mailId of this.selectedReceived) {
            try {
              await fetch(`/api/mail/INBOX/mails/${mailId}/change/TRASH/+FLAG`, { 
                method: 'POST'
              });
            } catch (error) {
              console.error(`删除接收邮件 ${mailId} 失败:`, error);
            }
          }
          
          for (const mailId of this.selectedSent) {
            try {
              await fetch(`/api/mail/SENT/mails/${mailId}/change/TRASH/+FLAG`, { 
                method: 'POST'
              });
            } catch (error) {
              console.error(`删除发送邮件 ${mailId} 失败:`, error);
            }
          }
          
          this.$message.success('删除成功，邮件已移入回收站');
          
          this.receivedStarred = this.receivedStarred.filter(mail => !this.selectedReceived.includes(mail.mail_id));
          this.sentStarred = this.sentStarred.filter(mail => !this.selectedSent.includes(mail.mail_id));
          
          this.groupedReceivedMails = [
            { title: "接收星标邮件", isExpanded: true, mails: this.receivedStarred }
          ];
          this.groupedSentMails = [
            { title: "发送星标邮件", isExpanded: true, mails: this.sentStarred }
          ];
          
          this.selectedReceived = [];
          this.selectedSent = [];
          this.allSelected = false;
        } finally {
          this.isLoading = false;
        }
      }).catch(() => {
        this.$message.info('已取消删除操作');
      });
    },
    
    async deleteAll() {
      if (this.receivedStarred.length === 0 && this.sentStarred.length === 0) {
        this.$message.info('没有可删除的星标邮件');
        return;
      }
      
      this.$confirm('确认删除所有星标邮件吗？', '警告', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }).then(async () => {
        this.isLoading = true;
        
        try {
          for (const mail of this.receivedStarred) {
            try {
              await fetch(`/api/mail/INBOX/mails/${mail.mail_id}/change/TRASH/+FLAG`, { 
                method: 'POST'
              });
            } catch (error) {
              console.error(`删除接收邮件 ${mail.mail_id} 失败:`, error);
            }
          }
          
          for (const mail of this.sentStarred) {
            try {
              await fetch(`/api/mail/SENT/mails/${mail.mail_id}/change/TRASH/+FLAG`, { 
                method: 'POST'
              });
            } catch (error) {
              console.error(`删除发送邮件 ${mail.mail_id} 失败:`, error);
            }
          }
          
          this.$message.success('全部星标邮件已移至回收站');
          
          this.receivedStarred = [];
          this.sentStarred = [];
          this.selectedReceived = [];
          this.selectedSent = [];
          this.allSelected = false;
          
          this.groupedReceivedMails = [
            { title: "接收星标邮件", isExpanded: true, mails: [] }
          ];
          this.groupedSentMails = [
            { title: "发送星标邮件", isExpanded: true, mails: [] }
          ];
        } finally {
          this.isLoading = false;
        }
      }).catch(() => {
        // 取消删除操作
      });
    },
    
    async cancelSelectedStars() {
      if (this.selectedReceived.length === 0 && this.selectedSent.length === 0) return;
      
      this.isLoading = true;
      
      try {
        let successCount = 0;
        
        for (const mailId of this.selectedReceived) {
          try {
            const response = await fetch(`/api/mail/INBOX/mails/${mailId}/change/R_STAR/-FLAG`, { 
              method: 'POST'
            });
            
            const result = await response.json();
            
            if (result.code === 'code.ok') {
              successCount++;
            } else {
              console.error(`取消邮件 ${mailId} 星标失败:`, result);
            }
          } catch (error) {
            console.error(`取消邮件 ${mailId} 星标出错:`, error);
          }
        }
        
        for (const mailId of this.selectedSent) {
          try {
            const response = await fetch(`/api/mail/SENT/mails/${mailId}/change/S_STAR/-FLAG`, { 
              method: 'POST'
            });
            
            const result = await response.json();
            
            if (result.code === 'code.ok') {
              successCount++;
            } else {
              console.error(`取消邮件 ${mailId} 星标失败:`, result);
            }
          } catch (error) {
            console.error(`取消邮件 ${mailId} 星标出错:`, error);
          }
        }
        
        this.receivedStarred = this.receivedStarred.filter(mail => !this.selectedReceived.includes(mail.mail_id));
        this.sentStarred = this.sentStarred.filter(mail => !this.selectedSent.includes(mail.mail_id));
        
        this.groupedReceivedMails = [
          { title: "接收星标邮件", isExpanded: true, mails: this.receivedStarred }
        ];
        this.groupedSentMails = [
          { title: "发送星标邮件", isExpanded: true, mails: this.sentStarred }
        ];
        
        this.selectedReceived = [];
        this.selectedSent = [];
        this.allSelected = false;
        
        if (successCount > 0) {
          this.$message.success(`成功取消 ${successCount} 封邮件的星标`);
        } else {
          this.$message.warning('没有邮件的星标被取消');
        }
      } finally {
        this.isLoading = false;
      }
    },
    
    toggleSelectAll() {
      if (this.allSelected) {
        this.selectedReceived = this.receivedStarred.map(mail => mail.mail_id);
        this.selectedSent = this.sentStarred.map(mail => mail.mail_id);
      } else {
        this.selectedReceived = [];
        this.selectedSent = [];
      }
    },
    
    openMail(mail, mailbox) {
      sessionStorage.setItem('currentMail', JSON.stringify(mail));
      
      this.$router.push({
        path: '/mail-detail',
        query: { 
          id: mail.mail_id, 
          mailbox: mailbox, 
          from: 'star'
        }
      });
    }
  },
  created() {
    console.log('星标邮件组件创建');
    this.loadAllStarredMails();
  },
  mounted() {
    console.log('星标邮件组件挂载');
  },
  activated() {
    console.log('星标邮件组件被激活');
    this.loadAllStarredMails();
  },
  beforeDestroy() {
    console.log('星标邮件组件即将销毁');
  },
  beforeRouteEnter(to, from, next) {
    next(vm => {
      console.log('进入星标邮件页面，来源:', from.path);
      vm.lastRoute = from.path;
      vm.loadAllStarredMails();
    });
  },
  beforeRouteUpdate(to, from, next) {
    console.log('星标邮件路由更新');
    this.loadAllStarredMails();
    next();
  },
  watch: {
    '$route'(to, from) {
      console.log('路由变化 - 从:', from.path, '到:', to.path);
      if (to.path === '/star') {
        this.loadAllStarredMails();
      }
    }
  }
};
</script>

<style scoped>
.starred-mails-page {
  padding: 20px 28px 28px 28px;
  height: calc(100vh - 48px);
  overflow-y: auto;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
}

  .mail-toolbar {
    padding: 15px 20px;
    border-bottom: 1px solid #e6f2fb;
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 14px;
    border-radius: 6px;
  }
  
  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  
  .select-all-checkbox {
    margin-right: 12px;
  }

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 14px;
  color: #666;
}

.toolbar-button {
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
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
  background: #fff;
}

.toolbar-button img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  transition: all 0.2s;
}

.cancel-star-button {
  width: auto;
  font-size: 14px;
  font-weight: bold;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
  height: 32px;
  padding: 0 12px;
  line-height: 30px;
  display: flex;
  align-items: center;
}

.cancel-star-button:hover:not(:disabled) {
  background: #f5f7fa;
  color: #409eff;
  border-color: #c0c4cc;
}

.cancel-star-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #f5f7fa;
}

.delete-all-button {
  background-color: #f56c6c;
  color: #fff;
  border-color: #f56c6c;
}

.delete-all-button:hover:not(:disabled) {
  background-color: #e64242;
}

.delete-all-button:disabled {
  background-color: #fab6b6;
  border-color: #fab6b6;
}

.mail-header {
  padding: 12px 16px;
  border-radius: 4px;
  margin: 12px 0;
  background-color: #f5f7fa;
  font-weight: bold;
  color: #666;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.mail-header .checkbox-col {
  width: 40px;
}

.mail-header .sender {
  flex: 1;
}

.mail-header .subject {
  flex: 2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: left;
}

.mail-header .time {
  flex: 1;
  white-space: nowrap;
  text-align: right;
  color: #999;
  font-size: 0.85em;
}

.mail-header .star-col {
  min-width: 40px;
  text-align: center;
}

.header-checkbox {
  margin: 0;
  width: 16px;
  height: 16px;
}

.mail-section {
  margin-bottom: 20px;
}

.mail-section h2 {
  font-size: 18px;
  color: #1f74c0;
  margin-bottom: 15px;
  font-weight: bold;
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
  padding: 12px 16px;
  border-bottom: 1px solid #e6f2fb;
  transition: background-color 0.2s;
}

.mail-item:last-child {
  border-bottom: none;
}

.mail-item:hover {
  background-color: #f5f7fa;
}

.unread {
  background-color: #f8f9fa;
  font-weight: 500;
}

.unread .subject {
  font-weight: bold;
}

.checkbox-container {
  width: 40px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.mail-content {
  display: flex;
  justify-content: space-between;
  width: 100%;
  cursor: pointer;
}

.column {
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.sender, .receiver {
  flex: 1;
  color: #666;
  font-size: 0.9em;
  text-align: left;
}

.subject {
  flex: 2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: left;
}

.time {
  flex: 1;
  white-space: nowrap;
  text-align: right;
  color: #999;
  font-size: 0.85em;
}

.star-icon {
  font-size: 1.2em;
  min-width: 40px;
  text-align: center;
  cursor: pointer;
  color: #999;
  transition: color 0.2s;
}

.star-filled {
  color: #ffc107;
}

.empty-message {
  text-align: center;
  padding: 30px;
  color: #999;
}

.global-empty {
  margin-top: 100px;
  font-size: 16px;
}

.item-checkbox {
  margin: 0;
  width: 16px;
  height: 16px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #1f74c0;
  gap: 10px;
  font-size: 16px;
}

@media (max-width: 768px) {
  .mail-toolbar {
    padding: 12px 16px;
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .toolbar-left {
    flex-wrap: wrap;
    gap: 6px;
  }
  
  .toolbar-right {
    width: 100%;
    justify-content: space-between;
    margin-top: 8px;
  }

  .sender, .receiver {
    min-width: 140px;
  }

  .time {
    min-width: 100px;
  }
}
</style>