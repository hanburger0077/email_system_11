<template>
  <div class="mail-list">
    <div class="mail-toolbar">
      <div class="toolbar-left">
        <el-tooltip content="删除" placement="bottom">
          <button
            :disabled="selectedMails.length === 0"
            @click="deleteSelected"
            class="toolbar-button"
          >
            <img src="../main/assets/mark5.png" alt="删除" />
          </button>
        </el-tooltip>
        <el-tooltip content="刷新" placement="bottom">
          <button
            class="toolbar-button"
            @click="handleReceive"
          >
            <img src="../main/assets/mark3.png" alt="刷新" />
          </button>
        </el-tooltip>
        <el-tooltip content="全部删除" placement="bottom">
          <el-button 
            class="delete-all-button" 
            @click="deleteAll"
          >
            全部删除
          </el-button>
        </el-tooltip>
      </div>

      <div class="toolbar-right">
        <span class="mail-count">{{ mailList.length }} 封邮件</span>
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
            :disabled="currentPage >= totalPages || totalPages === 0"
            @click="nextPage"
          >
            下一页
          </el-button>
        </div>
      </div>
    </div>

    <div class="mail-header">
      <span class="column checkbox-col"></span>
      <span class="column sender">发件人</span>
      <span class="column subject">主题</span>
      <span class="column time">时间</span>
    </div>

    <div class="list-content" v-if="!isLoading">
      <div class="mail-items-container">
        <div 
          v-for="mail in paginatedMails" 
          :key="mail.mail_id || mail.globalIndex" 
          class="mail-item" 
          :class="{ 'unread': !mail.isRead }"
          :data-mail-id="mail.mail_id"
        >
          <div class="checkbox-container">
            <el-checkbox v-model="selectedMails" :value="mail.mail_id || mail.globalIndex" class="item-checkbox" />
          </div>
          <div class="mail-content" @click="openMail(mail)">
            <span class="column sender">{{ mail.sender_email || mail.sender }}</span>
            <span class="column subject">{{ mail.subject }}</span>
            <span class="column time">{{ formatTime(mail.create_at || mail.send_time || mail.time) }}</span>
          </div>
        </div>

        <div v-if="mailList.length === 0" class="empty-message">
          垃圾邮件箱内无邮件
        </div>
      </div>
    </div>

    <div v-else class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
  </div>
</template>

<script>
import { Loading } from '@element-plus/icons-vue'

export default {
  name: 'JunkPage',
  components: {
    Loading
  },
  data() {
    return {
      currentPage: 1,
      totalPages: 0,
      itemsPerPage: 10,
      selectedMails: [],
      allSelected: false,
      mailList: [],
      currentFolder: 'JUNK', // 固定为垃圾邮件文件夹
      isLoading: false
    };
  },
  computed: {
    paginatedMails() {
      return this.mailList.map((mail, i) => ({
        ...mail,
        isRead: mail.read === 1,
        globalIndex: i
      }));
    }
  },
  methods: {
    // 格式化时间显示 - 始终显示完整年月日和时间
    formatTime(dateStr) {
      if (!dateStr) return '未知时间';
      
      try {
        // 解析日期
        let date;
        if (typeof dateStr === 'number') {
          date = new Date(dateStr);
        } else if (typeof dateStr === 'string') {
          date = new Date(dateStr);
        } else {
          return '未知时间';
        }
        
        // 检查日期是否有效
        if (isNaN(date.getTime())) {
          console.warn('无法解析的时间:', dateStr);
          return '未知时间';
        }
        
        // 获取年月日时分秒
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        
        // 始终返回完整格式的日期时间
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
      } catch (error) {
        console.error('格式化时间错误:', error);
        return '未知时间';
      }
    },
    
    // 加载邮件
    async loadMails(page) {
      // 确保页码至少为1
      if (page < 1) page = 1;
      // 只有当totalPages已知且大于0时才限制最大页码
      if (this.totalPages > 0 && page > this.totalPages) page = this.totalPages;
      
      this.isLoading = true;
      this.currentPage = page;
      
      try {
        // 使用与mailbox.html相同的API端点
        const response = await fetch(`/api/mail/${this.currentFolder}/pages/${page}`);
        if (!response.ok) { 
          throw new Error(`请求失败，状态码: ${response.status}`); 
        }
        
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          // 设置邮件列表数据
          this.mailList = result.data || [];
          
          // 更新分页信息
          this.totalPages = parseInt(result.message, 10) || 0;
          
          // 如果当前页没有数据，但总页数大于0且当前页大于1，则返回第一页重试
          if (this.mailList.length === 0 && this.totalPages > 0 && this.currentPage > 1) {
            return this.loadMails(1);
          }
          
          // 处理邮件内容
          this.mailList.forEach(mail => {
            // 确保content字段不为null
            if (mail.content === null) {
              mail.content = '';
            }
          });
          
          console.log(`垃圾邮件加载成功，第${this.currentPage}页，共${this.totalPages}页，${this.mailList.length}封邮件`);
        } else {
          console.error('加载垃圾邮件失败:', result.message, result.reason);
          this.$message.error(`加载邮件失败: ${result.message}`);
          
          // 保留旧数据，避免不必要的清空
          if (!this.mailList.length) {
            this.totalPages = 0;
          }
        }
      } catch (error) {
        console.error('请求垃圾邮件出错:', error);
        this.$message.error('加载邮件失败，请检查网络连接');
        
        // 保留旧数据，避免不必要的清空
        if (!this.mailList.length) {
          this.totalPages = 0;
        }
      } finally {
        this.isLoading = false;
      }
    },
    
    // 搜索邮件
    async searchMails(from = '', to = '', subject = '', body = '', since = 0) {
      this.isLoading = true;
      
      try {
        // 构建URL，参考mailbox.html中的搜索功能
        const url = new URL(`/api/mail/${this.currentFolder}/pages/${this.currentPage}/search`, window.location.origin);
        url.searchParams.append('from', from);
        url.searchParams.append('to', to);
        url.searchParams.append('subject', subject);
        url.searchParams.append('body', body);
        url.searchParams.append('since', since);
        
        const response = await fetch(url);
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          this.mailList = result.data || [];
          this.totalPages = parseInt(result.message, 10) || 0;
          
          // 处理邮件内容
          this.mailList.forEach(mail => {
            if (mail.content === null) {
              mail.content = '';
            }
          });
          
          this.$message.success('搜索完成');
        } else {
          console.error('搜索垃圾邮件失败:', result.message, result.reason);
          this.$message.error(`搜索失败: ${result.message}`);
          
          if (!this.mailList.length) {
            this.totalPages = 0;
          }
        }
      } catch (error) {
        console.error('搜索请求出错:', error);
        this.$message.error('搜索失败，请检查网络连接');
        
        if (!this.mailList.length) {
          this.totalPages = 0;
        }
      } finally {
        this.isLoading = false;
      }
    },
    
    // 高亮指定邮件
    highlightMail(mailId) {
      const mailElement = document.querySelector(`[data-mail-id="${mailId}"]`);
      if (mailElement) {
        mailElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
        mailElement.style.backgroundColor = '#ecf5ff';
        mailElement.style.border = '2px solid #409eff';
        
        setTimeout(() => {
          mailElement.style.backgroundColor = '';
          mailElement.style.border = '';
        }, 3000);
      }
    },
    
    // 翻页函数
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
    
    // 刷新邮件列表
    handleReceive() {
      this.loadMails(1);
      this.$message.success('刷新成功，已获取最新垃圾邮件');
    },
    
    // 切换全选
    toggleSelectAll() {
      if (this.allSelected) {
        this.selectedMails = this.paginatedMails.map(mail => mail.mail_id || mail.globalIndex);
      } else {
        this.selectedMails = [];
      }
    },
    
    // 删除选中的邮件（移到回收站）
    async deleteSelected() {
      if (this.selectedMails.length === 0) return;
      
      try {
        this.$confirm('确认删除选中的垃圾邮件吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(async () => {
          this.isLoading = true;
          for (const mailId of this.selectedMails) {
            try {
              // 将垃圾邮件移到回收站
              await fetch(`/api/mail/${this.currentFolder}/mails/${mailId}/change/TRASH/+FLAG`, {
                method: 'POST'
              });
            } catch (error) {
              console.error(`删除垃圾邮件 ${mailId} 失败:`, error);
            }
          }
          this.$message.success('删除成功，邮件已移入回收站');
          this.selectedMails = [];
          this.allSelected = false;
          this.loadMails(this.currentPage);
        }).catch(() => {
          this.$message.info('已取消删除操作');
        });
      } catch (error) {
        console.error('删除垃圾邮件出错:', error);
        this.$message.error('删除邮件失败');
      } finally {
        this.isLoading = false;
      }
    },

    // 删除所有邮件（移到回收站）
    async deleteAll() {
      this.$confirm('确认删除所有垃圾邮件吗？', '警告', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }).then(async () => {
        this.isLoading = true;
        try {
          const response = await fetch(`/api/mail/${this.currentFolder}/pages/1`);
          const result = await response.json();
          if (result.code === 'code.ok' && result.data) {
            for (const mail of result.data) {
              await fetch(`/api/mail/${this.currentFolder}/mails/${mail.mail_id}/change/TRASH/+FLAG`, {
                method: 'POST'
              });
            }
            this.mailList = [];
            this.selectedMails = [];
            this.allSelected = false;
            this.currentPage = 1;
            this.totalPages = 0;
            this.$message.success('已删除所有垃圾邮件');
          } else {
            this.$message.error('获取邮件列表失败，无法删除');
          }
        } catch (error) {
          console.error('删除所有垃圾邮件出错:', error);
          this.$message.error('删除邮件失败');
        } finally {
          this.isLoading = false;
        }
      }).catch(() => {
        // 取消删除操作
      });
    },
    
    // 打开邮件详情
    async openMail(mail) {
      const mailId = mail.mail_id || mail.globalIndex;
      
      // 将垃圾邮件标记为已读
      if (mail.mail_id && mail.read === 0) {
        try {
          await fetch(`/api/mail/${this.currentFolder}/mails/${mail.mail_id}/change/READ/+FLAG`, {
            method: 'POST'
          });
          
          mail.read = 1;
          mail.isRead = true;
        } catch (error) {
          console.error('标记垃圾邮件为已读失败:', error);
        }
      }
      
      // 存储邮件数据
      sessionStorage.setItem('allMails', JSON.stringify(this.mailList));
      sessionStorage.setItem('currentMail', JSON.stringify(mail));
      
      // 跳转到邮件详情页面
      this.$router.push({
        path: '/mail-detail',
        query: { 
          id: mailId,
          mailbox: this.currentFolder,
          from: 'junk'
        }
      });
    },
    
    // 标记为已读
    async markAsRead() {
      if (this.selectedMails.length === 0) return;
      
      try {
        this.isLoading = true;
        
        for (const mailId of this.selectedMails) {
          if (typeof mailId === 'number') {
            await fetch(`/api/mail/${this.currentFolder}/mails/${mailId}/change/READ/+FLAG`, {
              method: 'POST'
            });
            
            const mailIndex = this.mailList.findIndex(m => m.mail_id === mailId);
            if (mailIndex !== -1) {
              this.mailList[mailIndex].read = 1;
            }
          }
        }
        
        this.$message.success('已标记为已读');
      } catch (error) {
        console.error('标记垃圾邮件为已读出错:', error);
        this.$message.error('标记为已读失败');
      } finally {
        this.isLoading = false;
      }
    },
    
    // 标记为未读
    async markAsUnread() {
      if (this.selectedMails.length === 0) return;
      
      try {
        this.isLoading = true;
        
        for (const mailId of this.selectedMails) {
          if (typeof mailId === 'number') {
            await fetch(`/api/mail/${this.currentFolder}/mails/${mailId}/change/READ/-FLAG`, {
              method: 'POST'
            });
            
            const mailIndex = this.mailList.findIndex(m => m.mail_id === mailId);
            if (mailIndex !== -1) {
              this.mailList[mailIndex].read = 0;
            }
          }
        }
        
        this.$message.success('已标记为未读');
      } catch (error) {
        console.error('标记垃圾邮件为未读出错:', error);
        this.$message.error('标记为未读失败');
      } finally {
        this.isLoading = false;
      }
    }
  },
  mounted() {
    // 初始化加载垃圾邮件
    this.loadMails(1);
    
    // 检查是否有mailId参数，如果有则高亮显示对应邮件
    const mailId = this.$route.query.mailId;
    if (mailId) {
      this.$nextTick(() => {
        setTimeout(() => {
          this.highlightMail(parseInt(mailId));
        }, 500);
      });
    }
  }
}
</script>

<style scoped>
.mail-list {
  padding: 20px 28px 28px 28px;
  height: calc(100vh - 48px);
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.mail-toolbar {
  padding: 15px 20px;
  border-bottom: 1px solid #e6f2fb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 6px;
  margin-bottom: 14px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
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

.delete-all-button {
  height: 32px;
  padding: 0 15px;
  font-size: 14px;
  background-color: #f56c6c;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.delete-all-button:hover {
  background-color: #f78989;
  border-color: #f78989;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 14px;
  color: #666;
}

.mail-count {
  font-size: 14px;
  color: #666;
  margin-right: 20px;
}

.page-info {
  color: #666;
  margin-right: 8px;
  font-size: 14px;
}

.pagination-controls {
  display: flex;
  gap: 8px;
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

.mail-items-container {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.03);
  padding: 16px;
}

.mail-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 4px;
  transition: background-color 0.2s;
  border-bottom: 1px solid #f0f0f0;
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
  min-width: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mail-content {
  flex-grow: 1;
  display: flex;
  align-items: center;
  gap: 20px;
  width: 100%;
  cursor: pointer;
}

.column {
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.sender {
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
  min-width: 150px;
  text-align: right;
  color: #999;
  font-size: 0.85em;
}

.empty-message {
  text-align: center;
  padding: 30px;
  color: #999;
}

.item-checkbox {
  margin: 0;
  width: 16px;
  height: 16px;
}

.list-content {
  flex: 1;
  overflow-y: auto;
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

  .sender {
    min-width: 140px;
  }

  .time {
    min-width: 110px;
  }
}
</style>