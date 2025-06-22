<template>
  <div class="sent-mails-page">
    <!-- 顶部功能栏 -->
    <div class="mail-toolbar">
      <div class="toolbar-left">
        <!-- 全选 -->
        <el-checkbox 
          v-model="allSelected" 
          @change="toggleSelectAll"
          class="select-all-checkbox"
          :disabled="mailList.length === 0"
        />
        <!-- 删除 -->
        <el-button 
          class="delete-button" 
          :disabled="selectedMails.length === 0"
          @click="deleteSelected"
        >
          删除
        </el-button>
        <!-- 标记为已读（仅收件箱支持，此处仅做占位，可用于其他操作） -->
        <el-tooltip content="标为已读" placement="bottom">
          <el-button 
            class="mark-button"
            :disabled="selectedMails.length === 0"
            @click="markAsRead"
          >
            <img :src="mark1Icon" class="mark-icon" alt="标为已读" />
          </el-button>
        </el-tooltip>
        <!-- 标记为未读/取消星标 -->
        <el-tooltip content="取消星标" placement="bottom">
          <el-button 
            class="mark-button"
            :disabled="selectedMails.length === 0"
            @click="markSelectedAsUnstarred"
          >
            <img :src="mark2Icon" class="mark-icon" alt="取消星标" />
          </el-button>
        </el-tooltip>
        <!-- 更多操作下拉菜单 -->
        <el-dropdown @command="handleMarkCommand" :disabled="selectedMails.length === 0">
          <el-button class="mark-more-button">
            更多操作 <el-icon><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="star" class="dropdown-item">
                <i class="star-icon starred">★</i>
                <span>添加星标</span>
              </el-dropdown-item>
              <el-dropdown-item command="unstar" class="dropdown-item">
                <i class="star-icon">☆</i>
                <span>取消星标</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <!-- 刷新按钮 -->
        <el-tooltip content="刷新" placement="bottom">
          <el-button class="refresh-button" @click="handleReceive">
            <img :src="mark3Icon" class="refresh-icon" alt="刷新" />
          </el-button>
        </el-tooltip>
        <!-- 删除全部 -->
        <el-tooltip content="全部删除" placement="bottom">
          <el-button class="delete-all-button" @click="deleteAll">
            全部删除
          </el-button>
        </el-tooltip>
      </div>
      <div class="toolbar-right">
        <span class="mail-count">
          <span v-if="mailList.length === 0">暂无邮件</span>
          <span v-else>共 {{ totalMails }} 封</span>
        </span>
        <span class="page-info" v-if="totalPages > 0">{{ currentPage }}/{{ totalPages }}页</span>
        <span class="page-info" v-else>暂无分页</span>
        <div class="pagination-controls">
          <el-button size="small" :disabled="currentPage <= 1" @click="prevPage">上一页</el-button>
          <el-button size="small" :disabled="currentPage >= totalPages || totalPages === 0" @click="nextPage">下一页</el-button>
        </div>
      </div>
    </div>
    
    <!-- 邮件列表头部 -->
    <div class="mail-header" v-if="mailList.length > 0">
      <span class="column checkbox-col">
        <el-checkbox 
          v-model="allSelected" 
          @change="toggleSelectAll"
          class="select-all-checkbox"
          :disabled="mailList.length === 0"
        />
      </span>
      <span class="column receiver">收件人</span>
      <span class="column subject">主题</span>
      <span class="column time">时间</span>
      <span class="column star-col">星标</span>
    </div>
    
    <!-- 邮件列表内容 -->
    <div class="list-content" v-if="!isLoading">
      <div v-for="mail in mailList" :key="mail.mail_id" class="mail-item" :class="{ 'unread': false }">
        <div class="checkbox-container">
          <el-checkbox 
            v-model="selectedMails" 
            :value="mail.mail_id" 
            class="item-checkbox" 
            @change="updateAllSelectedState" 
          />
        </div>
        <div class="mail-content" @click="openMail(mail)">
          <span class="column receiver">{{ mail.receiver_email }}</span>
          <span class="column subject">{{ mail.subject }}</span>
          <span class="column time">{{ formatTime(mail.create_at) }}</span>
          <span class="star-icon" :class="{ 'star-filled': mail.sender_star === 1 }" @click.stop="toggleStar(mail)">
            {{ mail.sender_star === 1 ? '★' : '☆' }}
          </span>
        </div>
      </div>
      <div v-if="mailList.length === 0" class="empty-message">当前未找到已发送邮件</div>
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
import { ArrowDown, Loading } from '@element-plus/icons-vue'
import mark1Icon from '../main/assets/mark1.png'
import mark2Icon from '../main/assets/mark2.png'
import mark3Icon from '../main/assets/mark3.png'

export default {
  name: 'SentMails',
  components: { ArrowDown, Loading },
  data() {
    return {
      mailList: [],
      selectedMails: [],
      allSelected: false,
      currentPage: 1,
      totalPages: 0,
      totalMails: 0,
      to: '',
      subject: '',
      body: '',
      senderStar: false,
      isLoading: false,
      refreshInterval: null,
      mark1Icon,
      mark2Icon,
      mark3Icon,
    };
  },
  methods: {
    // 格式化时间
    formatTime(dateStr) {
      if (!dateStr) return '未知时间';
      const date = new Date(dateStr);
      if (isNaN(date.getTime())) return '未知时间';
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      const hours = String(date.getHours()).padStart(2, '0');
      const minutes = String(date.getMinutes()).padStart(2, '0');
      const seconds = String(date.getSeconds()).padStart(2, '0');
      return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
    },
    // 加载邮件数据（调用 /api/mail/SENT/pages/{page}）
    async loadMails(page) {
      if (page < 1) page = 1;
      this.isLoading = true;
      try {
        const response = await fetch(`/api/mail/SENT/pages/${page}`);
        const result = await response.json();
        if (result.code === 'code.ok') {
          this.mailList = result.data || [];
          this.totalPages = parseInt(result.message, 10) || 0;
          this.currentPage = page;
          this.totalMails = this.mailList.length;
        } else {
          this.mailList = [];
          this.totalPages = 0;
          this.totalMails = 0;
          this.$message.error(`${result.message} ${result.reason || ''}`);
        }
      } catch (error) {
        console.error('加载已发送邮件出错:', error);
        this.$message.error('加载邮件失败');
        this.mailList = [];
        this.totalPages = 0;
        this.totalMails = 0;
      } finally {
        this.isLoading = false;
      }
    },
    // 搜索邮件（调用 /api/mail/SENT/pages/{page}/search）
    async searchMails(page = 1) {
      if (page < 1) page = 1;
      this.isLoading = true;
      try {
        const url = new URL(`/api/mail/SENT/pages/${page}/search`, window.location.origin);
        url.searchParams.append('to', this.to);
        url.searchParams.append('subject', this.subject);
        url.searchParams.append('body', this.body);
        url.searchParams.append('sender_star', this.senderStar);
        const response = await fetch(url);
        const result = await response.json();
        if (result.code === 'code.ok') {
          this.mailList = result.data || [];
          this.totalPages = parseInt(result.message, 10) || 0;
          this.currentPage = page;
          this.totalMails = this.mailList.length;
        } else {
          this.$message.error(`${result.message} ${result.reason || ''}`);
        }
      } catch (error) {
        console.error('搜索已发送邮件出错:', error);
        this.$message.error('搜索邮件失败');
      } finally {
        this.isLoading = false;
      }
    },
    // 上一页
    prevPage() {
      if (this.currentPage > 1) {
        this.loadMails(this.currentPage - 1);
      }
    },
    // 下一页
    nextPage() {
      if (this.currentPage < this.totalPages) {
        this.loadMails(this.currentPage + 1);
      }
    },
    // 修改 toggleStar 方法，确保对“发件人星标”操作使用 S_STAR 与+FLAG/-FLAG
    async toggleStar(mail) {
        if (!mail.mail_id) return;
        try {
            const operation = mail.sender_star === 1 ? '-FLAG' : '+FLAG';
            const response = await fetch(`/api/mail/${this.currentFolder}/mails/${mail.mail_id}/change/S_STAR/${operation}`, {
                method: 'POST'
            });
            const result = await response.json();
            if (result.code === 'code.ok') {
                mail.sender_star = mail.sender_star === 1 ? 0 : 1;
                this.$message.success(`已成功${mail.sender_star === 1 ? '添加' : '取消'}星标`);
            } else {
                this.$message.error(`修改星标状态失败: ${result.reason || result.message}`);
            }
        } catch (error) {
            console.error('修改星标状态出错:', error);
            this.$message.error('修改星标状态失败');
        }
    },
    // 更新全选状态
    updateAllSelectedState() {
      this.allSelected = this.mailList.length > 0 && this.selectedMails.length === this.mailList.length;
    },
    // 切换全选
    toggleSelectAll() {
      if (this.allSelected) {
        this.selectedMails = this.mailList.map(mail => mail.mail_id);
      } else {
        this.selectedMails = [];
      }
    },
    // 删除选中的邮件（永久删除，调用回收站接口）
    async deleteSelected() {
      if (this.selectedMails.length === 0) return;
      try {
        await this.$confirm(`确认删除选中的 ${this.selectedMails.length} 封邮件吗？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        });
        for (const mailId of this.selectedMails) {
          try {
            await fetch(`/api/mail/TRASH/mails/${mailId}/delete`, { method: 'DELETE' });
          } catch (err) {
            console.error(`删除邮件 ${mailId} 失败:`, err);
          }
        }
        this.$message.success('删除成功');
        this.selectedMails = [];
        this.allSelected = false;
        this.loadMails(this.currentPage);
      } catch (error) {
        this.$message.info('已取消删除操作');
      }
    },
    // 删除所有邮件（永久删除，调用回收站接口）
    async deleteAll() {
      try {
        await this.$confirm('确认删除所有邮件吗？', '警告', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning'
        });
        this.isLoading = true;
        // 获取第一页邮件列表
        const response = await fetch(`/api/mail/SENT/pages/1`);
        const result = await response.json();
        if (result.code === 'code.ok' && result.data) {
          for (const mail of result.data) {
            await fetch(`/api/mail/TRASH/mails/${mail.mail_id}/delete`, { method: 'DELETE' });
          }
          this.mailList = [];
          this.selectedMails = [];
          this.allSelected = false;
          this.currentPage = 1;
          this.totalPages = 0;
          this.totalMails = 0;
          this.$message.success('已删除所有邮件');
        } else {
          this.$message.error('获取邮件列表失败，无法删除');
        }
      } catch (error) {
        console.error('删除所有邮件出错:', error);
        this.$message.error('删除邮件失败');
      } finally {
        this.isLoading = false;
      }
    },
    // 处理下拉菜单命令
    async handleMarkCommand(command) {
      if (this.selectedMails.length === 0) return;
      if (command === 'star') {
        await this.markSelectedAsStarred(true);
      } else if (command === 'unstar') {
        await this.markSelectedAsStarred(false);
      }
    },
    // 批量标记（添加或取消星标）
    async markSelectedAsStarred(starred) {
      let starSign = 'S_STAR';
      const operation = starred ? '+FLAG' : '-FLAG';
      try {
        this.isLoading = true;
        let successCount = 0;
        for (const mailId of this.selectedMails) {
          if (typeof mailId === 'number' && mailId > 0) {
            try {
              const response = await fetch(`/api/mail/SENT/mails/${mailId}/change/${starSign}/${operation}`, {
                method: 'POST'
              });
              const result = await response.json();
              if (result.code === 'code.ok') {
                const index = this.mailList.findIndex(m => m.mail_id === mailId);
                if (index !== -1) {
                  this.mailList[index].sender_star = starred ? 1 : 0;
                }
                successCount++;
              }
            } catch (err) {
              console.error(`邮件 ${mailId} 操作异常:`, err);
            }
          }
        }
        if (successCount > 0) {
          this.$message.success(starred ? `已为 ${successCount} 封邮件添加星标` : `已为 ${successCount} 封邮件取消星标`);
        } else {
          this.$message.error('星标操作失败');
        }
      } catch (error) {
        console.error('批量修改星标状态出错:', error);
        this.$message.error('修改星标状态失败');
      } finally {
        this.isLoading = false;
      }
    },
    // 取消批量星标操作
    async markSelectedAsUnstarred() {
      await this.markSelectedAsStarred(false);
    },
    // 刷新邮件列表
    handleReceive() {
      this.loadMails(1);
      this.$message.success('刷新成功，已获取最新邮件');
    },
    // 点击邮件跳转详情
    openMail(mail) {
      sessionStorage.setItem('currentMail', JSON.stringify(mail));
      this.$router.push({
        path: '/mail-detail',
        query: { id: mail.mail_id, mailbox: 'SENT', from: 'sent' }
      });
    },
    // 启动自动刷新（每60秒刷新一次）
    startAutoRefresh() {
      this.refreshInterval = setInterval(() => {
        this.loadMails(this.currentPage);
      }, 60000);
    }
  },
  mounted() {
    this.loadMails(1);
    this.startAutoRefresh();
  },
  beforeDestroy() {
    if (this.refreshInterval) {
      clearInterval(this.refreshInterval);
    }
  }
};
</script>

<style scoped>
.sent-mails-page {
  padding: 20px 28px;
  font-family: Arial, sans-serif;
  max-width: 1200px;
  margin: 0 auto;
}

/* 顶部工具栏，参考主页面布局，缩小左右间距 */
.mail-toolbar {
  padding: 12px 16px;
  border-bottom: 1px solid #e6f2fb;
  background: #f8faff;
  border-radius: 6px;
  display: flex;
  flex-wrap: nowrap;
  justify-content: space-between;
  align-items: center;
}

/* 左侧工具栏，按钮间距调整为6px */
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 右侧工具栏，间距统一调整 */
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #666;
}

/* 调整全选复选框尺寸 */
.select-all-checkbox {
  margin: 0;
  width: 16px;
  height: 16px;
}

/* 按钮统一调整为高度32px，提升易点击性 */
.delete-button,
.mark-button,
.mark-more-button,
.refresh-button,
.delete-all-button {
  font-size: 14px;
  border-radius: 4px;
  height: 32px;
  padding: 0 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

/* 删除按钮 */
.delete-button {
  border: 1px solid #f56c6c;
  background: #f56c6c;
  color: #fff;
}
.delete-button:hover {
  background: #f78989;
}
.delete-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* 标记按钮 */
.mark-button {
  border: 1px solid #dcdfe6;
  background: #fff;
}
.mark-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.mark-icon {
  width: 22px;
  height: 22px;
  object-fit: cover;
}

/* 更多操作下拉按钮 */
.mark-more-button {
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
}
.mark-more-button:hover {
  background: #f5f7fa;
  color: #409eff;
}

/* 刷新按钮尺寸保持32px，但图标稍大 */
.refresh-button {
  width: 32px;
  height: 32px;
  border: 1px solid #dcdfe6;
  background: #fff;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.refresh-icon {
  width: 22px;
  height: 22px;
  object-fit: cover;
}

/* 删除全部按钮 */
.delete-all-button {
  border: 1px solid #f56c6c;
  background: #f56c6c;
  color: #fff;
  min-width: 70px;
}

/* 右侧信息 */
.mail-count,
.page-info {
  font-size: 14px;
  color: #666;
}

/* 分页容器 */
.pagination-controls {
  display: flex;
  gap: 6px;
}

/* 邮件列表头部 */
.mail-header {
  padding: 12px 16px;
  background: #f5f7fa;
  border-radius: 4px;
  margin: 12px 0;
  display: flex;
  align-items: center;
  gap: 10px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}
.column {
  display: flex;
  align-items: center;
}
.checkbox-col {
  min-width: 24px;
}
.receiver {
  min-width: 180px;
  color: #666;
  font-size: 0.9em;
}
.subject {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.time {
  min-width: 120px;
  text-align: right;
  color: #999;
  font-size: 0.85em;
}
.star-col {
  min-width: 40px;
  text-align: center;
}
.star-icon {
  font-size: 1.2em;
  color: #999;
  margin-left: 8px;
  cursor: pointer;
  transition: color 0.2s;
}
.star-filled {
  color: #ffc107;
}

/* 邮件项 */
.mail-item {
  padding: 15px;
  border-bottom: 1px solid #eee;
  display: flex;
  align-items: center;
}
.checkbox-container {
  min-width: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mail-content {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 20px;
  cursor: pointer;
}

/* 邮件列表滚动区域 */
.list-content {
  max-height: calc(100vh - 220px);
  overflow-y: auto;
}

/* 空状态 */
.empty-message {
  padding: 20px;
  text-align: center;
  color: #777;
}

/* 加载状态 */
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  font-size: 16px;
  color: #1f74c0;
}
</style>