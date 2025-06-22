<template>
  <div class="starred-mails-page">
    <div class="mail-toolbar">
      <div class="toolbar-left">
        <!-- 全选框 -->
        <el-checkbox 
          v-model="allSelected" 
          @change="toggleSelectAll" 
          class="select-all-checkbox"
          :disabled="mailList.length === 0"
        />

        <!-- 删除按钮 - 标红修改 -->
        <el-button  
          :disabled="selectedMails.length === 0"
          @click="deleteSelected"
          class="action-button delete-button"
        >
          删除
        </el-button>

        <!-- 取消星标按钮 -->
        <el-tooltip content="为选中邮件取消星标" placement="bottom">
          <el-button 
            @click="unstarSelected"
            :disabled="selectedMails.length === 0"
            class="action-button unstar-button"
          >
            <i class="star-icon-empty">☆</i>
            <span>取消星标</span>
          </el-button>
        </el-tooltip>

        <!-- 刷新按钮 -->
        <el-tooltip content="刷新" placement="bottom">
          <el-button 
            class="refresh-button" 
            @click="handleReceive"
          >
            <span style="font-size: 16px;">🔄</span>
          </el-button>
        </el-tooltip>
      </div>

      <div class="toolbar-right">
        <!-- 修改：优化邮件计数显示逻辑 -->
        <span class="mail-count">
          <span v-if="mailList.length === 0">暂无邮件</span>
          <span v-else>共 {{ mailList.length }} 封</span>
        </span>
        
        <!-- 修改：当没有数据时，不显示页码信息或显示合理的页码 -->
        <span class="page-info" v-if="totalPages > 0">{{ currentPage }}/{{ totalPages }}页</span>
        <span class="page-info" v-else>暂无分页</span>
        
        <div class="pagination-controls">
          <el-button 
            size="small" 
            :disabled="currentPage <= 1"
            @click="prevPage"
          >
            上一页
          </el-button>
          <el-button 
            size="small" 
            :disabled="currentPage >= totalPages"
            @click="nextPage"
          >
            下一页
          </el-button>
        </div>
      </div>
    </div>

    <!-- 邮件列表头部 -->
    <div class="mail-header" v-if="!isLoading && mailList.length > 0">
      <span class="column checkbox-col"></span>
      <span class="column sender">发件人</span>
      <span class="column subject">主题</span>
      <span class="column time">时间</span>
      <span class="column star-col"></span>
    </div>

    <!-- 邮件列表内容 -->
    <div class="list-content" v-if="!isLoading">
      <div v-if="mailList.length > 0" class="mail-items">
        <div 
          v-for="mail in mailList"  
          :key="mail.mail_id" 
          class="mail-item" 
          :class="{ 'unread': mail.read === 0 }"
        >
          <div class="checkbox-container">
            <el-checkbox 
              v-model="selectedMails" 
              class="item-checkbox" 
              @change="updateAllSelectedState" 
            />
          </div>
          <div class="mail-content" @click="openMail(mail)">
            <span class="column sender">{{ formatSender(mail.sender_email) }}</span>
            <span class="column subject">{{ mail.subject }}</span>
            <span class="column time">{{ formatTime(mail.create_at) }}</span>
            <span 
              class="star-icon star-filled"
              @click.stop="toggleStar(mail)"
            >★</span>
          </div>
        </div>
      </div>
      <!-- 空状态 -->
      <div v-else class="empty-message">
        暂无星标邮件
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-else class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>正在加载星标邮件...</span>
    </div>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus';
import { Loading } from '@element-plus/icons-vue';

export default {
  name: 'StarredMailsPage',
  components: {
    Loading,
  },
  data() {
    return {
      currentPage: 0,  
      totalPages: 0,
      totalMails: 0, 
      selectedMails: [],
      allSelected: false,
      mailList: [],
      isLoading: false,
      loadAttempts: 0, // 调试用：记录加载尝试次数
    };
  },
  methods: {
    // 格式化发件人，移除前导数字和符号
    formatSender(sender) {
      if (typeof sender === 'string') {
        return sender.replace(/^\d+[\.\s]+/, '');
      }
      return sender;
    },

    // 检查是否为"无搜索结果"的情况
    isNoResultsError(result) {
      const message = result.message || '';
      const reason = result.reason || '';
      
      return message.includes('No mail searched') || 
             reason.includes('No mail searched') ||
             message.includes('no mail found') ||
             reason.includes('no mail found') ||
             (result.code !== 'code.ok' && (!result.data || result.data.length === 0));
    },

    // 加载星标邮件 - 增强版本，添加超时处理
    async loadMails(page = 1) {
      console.log(`开始加载第 ${page} 页星标邮件，尝试次数: ${++this.loadAttempts}`);
      
      // 防止重复加载
      if (this.isLoading) {
        console.log('已在加载中，跳过此次请求');
        return;
      }

      this.isLoading = true;
      this.selectedMails = [];
      this.allSelected = false;
      
      // 设置请求超时
      const timeoutId = setTimeout(() => {
        console.error('请求超时，强制结束加载状态');
        this.isLoading = false;
        this.$message.error('请求超时，请检查网络连接');
      }, 10000); // 10秒超时
      
      try {
        console.log('发送请求到:', `/api/mail/INBOX/pages/${page}/search?receiver_star=true`);
        
        const response = await fetch(`/api/mail/INBOX/pages/${page}/search?receiver_star=true`, {
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
          },
          // 添加请求超时
          signal: AbortSignal.timeout(8000) // 8秒超时
        });

        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        console.log('接收到响应:', result);

        clearTimeout(timeoutId); // 清除超时定时器

        if (result.code === 'code.ok') {
          // 成功获取到数据
          this.mailList = result.data || [];
          this.totalPages = parseInt(result.message, 10) || 0;
          this.currentPage = page;
          this.totalMails = this.mailList.length;
          console.log('数据加载成功:', this.mailList.length, '封邮件');
        } else if (this.isNoResultsError(result)) {
          // 关键修改：搜索无结果时，将页码也设置为0，保持一致性
          this.mailList = [];
          this.totalPages = 0;
          this.currentPage = 0; // 修改：设置为0而不是1
          this.totalMails = 0;
          console.log('无星标邮件，正常显示空状态');
        } else {
          // 其他错误
          console.error('加载星标邮件失败:', result);
          this.$message.error(`加载星标邮件失败: ${result.reason || result.message || '未知错误'}`);
          this.mailList = [];
          this.totalPages = 0;
          this.currentPage = 0; // 修改：错误时也设置为0
          this.totalMails = 0;
        }
      } catch (error) {
        clearTimeout(timeoutId); // 清除超时定时器
        console.error('加载星标邮件网络错误:', error);
        
        if (error.name === 'AbortError') {
          this.$message.error('请求超时，请检查网络连接');
        } else {
          this.$message.error(`加载星标邮件失败: ${error.message}`);
        }
        
        // 网络错误时重置数据
        this.mailList = [];
        this.totalPages = 0;
        this.currentPage = 0; // 修改：错误时也设置为0
        this.totalMails = 0;
      } finally {
        // 确保 isLoading 始终被设置为 false
        this.isLoading = false;
        console.log('加载完成，isLoading 设置为 false');
      }
    },

    // 格式化时间
    formatTime(dateStr) {
      if (!dateStr) return '未知时间';
      try {
        const date = new Date(dateStr);
        if (isNaN(date.getTime())) return '无效时间';
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `${year}-${month}-${day} ${hours}:${minutes}`;
      } catch (error) {
        return '未知时间';
      }
    },

    // 切换全选
    toggleSelectAll() {
      if (this.allSelected) {
        this.selectedMails = this.mailList.map(mail => mail.mail_id);
      } else {
        this.selectedMails = [];
      }
    },
    
    // 更新全选框状态
    updateAllSelectedState() {
      this.allSelected = this.mailList.length > 0 && this.selectedMails.length === this.mailList.length;
    },

    // 删除选中
    async deleteSelected() {
      if (this.selectedMails.length === 0) return;
      try {
        await ElMessageBox.confirm(`确认删除选中的 ${this.selectedMails.length} 封邮件吗？`, '提示', { type: 'warning' });
        
        let successCount = 0;
        for (const mailId of this.selectedMails) {
          try {
            const response = await fetch(`/api/mail/INBOX/mails/${mailId}/delete`, { method: 'DELETE' });
            const result = await response.json();
            if (result.code === 'code.ok') {
              successCount++;
            }
          } catch (err) {
            console.error(`删除邮件 ${mailId} 失败:`, err);
          }
        }
        
        if (successCount > 0) {
          this.$message.success(`成功删除 ${successCount} 封邮件`);
          this.loadMails(this.currentPage || 1); // 修改：确保不传入0作为页码
        } else {
          this.$message.error('删除邮件失败');
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除邮件失败:', error);
          this.$message.error('删除邮件失败');
        } else {
          this.$message.info('已取消删除');
        }
      }
    },

    // 批量取消星标
    async unstarSelected() {
      if (this.selectedMails.length === 0) return;
      
      try {
        let successCount = 0;
        for (const mailId of this.selectedMails) {
          try {
            const response = await fetch(`/api/mail/INBOX/mails/${mailId}/change/R_STAR/-FLAG`, { method: 'POST' });
            const result = await response.json();
            if (result.code === 'code.ok') {
              successCount++;
            }
          } catch (err) {
            console.error(`取消星标邮件 ${mailId} 失败:`, err);
          }
        }
        
        if (successCount > 0) {
          this.$message.success(`已为 ${successCount} 封邮件取消星标`);
          this.loadMails(this.currentPage || 1); // 修改：确保不传入0作为页码
        } else {
          this.$message.error('取消星标失败');
        }
      } catch (error) {
        console.error('取消星标失败:', error);
        this.$message.error('取消星标失败');
      }
    },
    
    // 切换单个邮件的星标状态
    async toggleStar(mail) {
      try {
        const response = await fetch(`/api/mail/INBOX/mails/${mail.mail_id}/change/R_STAR/-FLAG`, { method: 'POST' });
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          this.$message.success(`已取消邮件 "${mail.subject}" 的星标`);
          this.loadMails(this.currentPage || 1); // 修改：确保不传入0作为页码
        } else {
          this.$message.error('取消星标失败');
        }
      } catch (error) {
        console.error('取消星标失败:', error);
        this.$message.error('取消星标失败');
      }
    },

    // 刷新
    handleReceive() {
      this.loadMails(this.currentPage || 1); // 修改：确保不传入0作为页码
    },

    // 翻页
    prevPage() {
      if (this.currentPage > 1) {
        this.loadMails(this.currentPage - 1);
      }
    },
    nextPage() {
      if (this.currentPage < this.totalPages) {
        this.loadMails(this.currentPage + 1);
      }
    },
    
    // 打开邮件详情
    openMail(mail) {
      sessionStorage.setItem('currentMail', JSON.stringify(mail));
      this.$router.push({ 
        path: '/mail-detail', 
        query: { id: mail.mail_id, mailbox: 'INBOX', from: 'star' } 
      });
    }
  },
  mounted() {
    console.log('组件已挂载，开始加载星标邮件');
    this.loadMails(1);
  }
}
</script>

<style scoped>
.starred-mails-page {
  padding: 20px 28px;
  font-family: sans-serif;
  height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
}

.mail-toolbar {
  padding: 15px 20px;
  border-bottom: 1px solid #e6f2fb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8faff;
  margin-bottom: 14px;
  border-radius: 6px;
  flex-shrink: 0;
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

/* 基础按钮样式 */
.action-button {
  font-size: 14px;
  border-radius: 4px;
  cursor: pointer;
  height: 32px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

/* 普通按钮样式 */
.action-button:not(.delete-button) {
  border: 1px solid #dcdfe6;  
  background: #fff;         
  color: #606266;         
}

.action-button:not(.delete-button):hover {
  background: #f5f7fa;        
  border-color: #c6e2ff;      
  color: #409eff;            
}

.action-button:not(.delete-button):disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #f5f7fa;
}

/* 删除按钮样式 - 标红修改 */
.delete-button {
  border: 1px solid #f56c6c;  
  background: #f56c6c;         
  color: #fff;         
}

.delete-button:hover {
  background: #f78989;        
  border-color: #f78989;      
  color: #fff;            
}

.delete-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #f56c6c;
  border-color: #f56c6c;
}

.unstar-button .star-icon-empty {
  color: #999;
  font-style: normal;
  font-size: 16px;
}

.refresh-button {
  padding: 0;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.pagination-controls {
  display: flex;
  gap: 8px;
}

.mail-header {
  padding: 12px 16px;
  border-radius: 4px;
  background-color: #f5f7fa;
  font-weight: bold;
  color: #666;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.list-content {
  flex: 1;
  overflow-y: auto;
}

.mail-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s;
}

.mail-item:hover {
  background-color: #f5f7fa;
}

.unread {
  font-weight: bold;
}

.mail-content {
  flex-grow: 1;
  display: flex;
  align-items: center;
  gap: 20px;
  cursor: pointer;
  overflow: hidden;
}

.column {
  display: flex;
  align-items: center;
}

.checkbox-col { min-width: 24px; }
.sender { min-width: 220px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.subject { flex-grow: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.time { min-width: 150px; text-align: right; color: #999; font-size: 0.9em; }
.star-col { min-width: 40px; text-align: center; }

.star-icon {
  font-size: 1.2em;
  cursor: pointer;
}

.star-filled {
  color: #ffc107;
}

.loading-container, .empty-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
  font-size: 16px;
  gap: 10px;
}

.loading-container {
  color: #1f74c0;
}
</style>