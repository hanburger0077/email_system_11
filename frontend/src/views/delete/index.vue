<template>
  <div class="mail-list">
    <div class="mail-toolbar">
      <div class="toolbar-left">
        <!-- 全选按钮 -->
        <el-checkbox 
          v-model="allSelected" 
          @change="toggleSelectAll" 
          class="select-all-checkbox" 
        />
        <el-tooltip content="恢复邮件" placement="bottom">
          <el-button 
            class="restore-button" 
            :disabled="selectedMails.length === 0" 
            @click="restoreSelected">
            恢复邮件
          </el-button>
        </el-tooltip>
        <el-tooltip content="永久删除" placement="bottom">
          <el-button 
            class="delete-button" 
            :disabled="selectedMails.length === 0" 
            @click="permanentDeleteAll">
            永久删除
          </el-button>
        </el-tooltip>
        <el-tooltip content="刷新" placement="bottom">
          <el-button 
            class="refresh-button" 
            @click="handleReceive">
            <img :src="refreshIcon" class="refresh-icon" alt="刷新" />
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
            @click="prevPage">
            上一页
          </el-button>
          <el-button 
            size="small" 
            :disabled="currentPage >= totalPages || totalPages === 0"
            @click="nextPage">
            下一页
          </el-button>
        </div>
      </div>
    </div>
    
    <!-- 表头 -->
    <div class="mail-header">
      <span class="column checkbox-col">
        <el-checkbox v-model="allSelected" @change="toggleSelectAll" class="header-checkbox" />
      </span>
      <span class="column sender">发件人</span>
      <span class="column subject">主题</span>
      <span class="column time">时间</span>
    </div>

    <div class="list-content" v-if="!isLoading">
      <div v-if="mailList.length > 0">
        <div 
          v-for="(mail, index) in mailList" 
          :key="mail.mail_id || index" 
          class="mail-item">
          <div class="checkbox-container">
            <el-checkbox 
              v-model="selectedMails" 
              :value="mail.mail_id || index" 
              class="item-checkbox" />
          </div>
          <!-- 点击邮件内容跳转详情 -->
          <div class="mail-content" @click="openMail(mail)">
            <span class="column sender">{{ mail.sender_email || mail.sender }}</span>
            <span class="column subject">{{ mail.subject }}</span>
            <span class="column time">
              {{ formatTime(mail.create_at || mail.send_time || mail.time) }}
            </span>
          </div>
        </div>
      </div>
      <div v-else class="empty-message">
        当前回收站无邮件
      </div>
    </div>

    <div v-else class="loading-container">
      <el-icon class="is-loading">
        <Loading />
      </el-icon>
      <span>加载中...</span>
    </div>
  </div>
</template>

<script>
import { Loading } from '@element-plus/icons-vue'
import refreshIcon from './assets/mark3.png'
export default {
  name: 'DeletedPage',
  components: {
    Loading
  },
  data() {
    return {
      refreshIcon,
      currentPage: 1,
      totalPages: 0,
      mailList: [],
      isLoading: false,
      selectedMails: [],
      allSelected: false
    }
  },
  methods: {
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
        if (isNaN(date.getTime())) return '未知时间';
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
      } catch (error) {
        console.error('时间格式化错误:', error);
        return '未知时间';
      }
    },
    async loadMails(page) {
      if (page < 1) page = 1;
      if (this.totalPages > 0 && page > this.totalPages) page = this.totalPages;
      this.isLoading = true;
      this.currentPage = page;
      try {
        // 调用回收站邮件接口
        const response = await fetch(`/api/mail/TRASH/pages/${page}`);
        const result = await response.json();
        if (result.code === 'code.ok') {
          this.mailList = result.data || [];
          this.totalPages = parseInt(result.message, 10) || 0;
        } else {
          if (!result.data || result.data.length === 0) {
            this.mailList = [];
            this.totalPages = 0;
          } else {
            this.$message.error(`加载邮件失败: ${result.message}`);
          }
        }
      } catch (error) {
        console.error('加载邮件出错:', error);
        if (this.mailList.length === 0) {
          this.mailList = [];
          this.totalPages = 0;
        } else {
          this.$message.error('加载邮件失败，请检查网络连接');
        }
      } finally {
        this.isLoading = false;
      }
    },
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
    handleReceive() {
      this.loadMails(1);
      this.$message.success('刷新成功，已获取最新邮件');
    },
    async restoreSelected() {
      if (this.selectedMails.length === 0) return;
      this.$confirm('确认恢复选中的邮件吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        this.isLoading = true;
        try {
          for (const mailId of this.selectedMails) {
            // 使用TRASH作为邮箱参数，移除TRASH标记以恢复邮件
            await fetch(`/api/mail/TRASH/mails/${mailId}/change/TRASH/-FLAG`, {
              method: 'POST'
            });
          }
          this.$message.success('选中邮件已恢复');
          this.selectedMails = [];
          this.allSelected = false;
          this.loadMails(this.currentPage);
        } catch (error) {
          console.error('恢复邮件出错:', error);
          this.$message.error('恢复邮件失败，请检查网络连接');
        } finally {
          this.isLoading = false;
        }
      }).catch(() => {
        this.$message.info('已取消恢复操作');
      });
    },
    // 永久删除选中邮件
    async permanentDeleteAll() {
      if (this.selectedMails.length === 0) return;
      this.$confirm('确认永久删除选中的邮件吗？', '警告', {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        this.isLoading = true;
        try {
          for (const mailId of this.selectedMails) {
            // 使用TRASH作为邮箱参数调用删除接口
            await fetch(`/api/mail/TRASH/mails/${mailId}/delete`, {
              method: 'DELETE'
            });
          }
          this.$message.success('选中邮件已永久删除');
          this.selectedMails = [];
          this.allSelected = false;
          this.loadMails(this.currentPage);
        } catch (error) {
          console.error('永久删除出错:', error);
          this.$message.error('永久删除失败，请检查网络连接');
        } finally {
          this.isLoading = false;
        }
      }).catch(() => {
        this.$message.info('已取消永久删除操作');
      });
    },
    toggleSelectAll() {
      if (this.allSelected) {
        this.selectedMails = this.mailList.map(mail => mail.mail_id || this.mailList.indexOf(mail));
      } else {
        this.selectedMails = [];
      }
    },
    // 添加：点击邮件跳转到详情页面的方法
    openMail(mail) {
      const mailId = mail.mail_id || mail.globalIndex;
      // 将当前回收站邮件数据存入sessionStorage，方便详情页读取
      sessionStorage.setItem('allMails', JSON.stringify(this.mailList));
      sessionStorage.setItem('currentMail', JSON.stringify(mail));
      // 跳转到邮件详情页面，并传递mailbox参数为TRASH
      this.$router.push({
        path: '/mail-detail',
        query: { 
          id: mailId,
          mailbox: 'TRASH',
          from: 'trash'
        }
      });
    }
  },
  mounted() {
    this.loadMails(1);
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
  gap: 20px;
  font-size: 14px;
  color: #666;
}
.select-all-checkbox {
  margin-right: 12px;
}
.delete-button {
  font-size: 14px;
  border: 1px solid #f56c6c;
  background: #f56c6c;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  height: 32px;
  padding: 0 12px;
  line-height: 30px;
}
.delete-button:hover {
  background: #e64242;
  border-color: #e64242;
}
.refresh-button {
  padding: 0;
  margin: 0;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dcdfe6;
  background: #fff;
}
.refresh-icon {
  width: 20px;
  height: 20px;
  object-fit: cover;
}
.restore-button {
  font-size: 14px;
  border: 1px solid #409eff;
  background: #409eff;
  color: white;
  border-radius: 4px;
  cursor: pointer;
  height: 32px;
  padding: 0 12px;
  line-height: 30px;
}
.restore-button:hover {
  background: #66b1ff;
  border-color: #66b1ff;
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
.column {
  font-size: 14px;
  color: #333;
}
.checkbox-col {
  width: 40px;
}
.sender {
  flex: 1;
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
.header-checkbox,
.item-checkbox {
  margin: 0;
  width: 16px;
  height: 16px;
}
.list-content {
  flex: 1;
  overflow-y: auto;
}
.mail-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e6f2fb;
  transition: background-color 0.2s;
}
.mail-item:hover {
  background-color: #f5f7fa;
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
.empty-message {
  text-align: center;
  padding: 30px;
  color: #999;
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
</style>