<template>
  <!-- 主邮件区域 -->
  <div class="mail-area">
        <!-- 邮件操作栏 -->
        <div class="mail-toolbar">
          <div class="toolbar-left">
            <input 
              type="checkbox" 
              v-model="allSelected" 
              @change="toggleSelectAll" 
              class="select-all-checkbox"
            />
            <button class="toolbar-btn" @click="permanentDelete" :disabled="selectedMails.length === 0">
              彻底删除
            </button>
            <button class="toolbar-btn" @click="showReportDropdown = !showReportDropdown">
              举报 ▼
            </button>
            <div v-show="showReportDropdown" class="dropdown-menu">
              <button @click="reportSpam">举报垃圾邮件</button>
              <button @click="reportPhishing">举报钓鱼邮件</button>
            </div>
            
            <div class="mark-dropdown">
              <button class="toolbar-btn" @click="showMarkDropdown = !showMarkDropdown">
                标记为 ▼
              </button>
              <div v-show="showMarkDropdown" class="dropdown-menu">
                <button @click="markAsRead">已读</button>
                <button @click="markAsUnread">未读</button>
                <button @click="markAsStarred">星标</button>
              </div>
            </div>

            <div class="move-dropdown">
              <button class="toolbar-btn" @click="showMoveDropdown = !showMoveDropdown">
                移动到 ▼
              </button>
              <div v-show="showMoveDropdown" class="dropdown-menu">
                <button @click="moveToInbox">收件箱</button>
                <button @click="moveToSpam">垃圾邮件</button>
                <button @click="moveToDraft">草稿箱</button>
              </div>
            </div>

            <button class="toolbar-btn" @click="showMoreActions = !showMoreActions">
              更多 ▼
            </button>
            <div v-show="showMoreActions" class="dropdown-menu">
              <button @click="selectAll">全选</button>
              <button @click="clearSelection">取消选择</button>
              <button @click="exportSelected">导出选中</button>
            </div>

            <button class="toolbar-btn restore-btn" @click="restoreSelected" :disabled="selectedMails.length === 0">
              读取恢复
            </button>
            <button class="toolbar-btn" @click="refreshList">刷新</button>
          </div>

          <div class="toolbar-right">
            <span class="mail-count">{{ filteredMails.length }} 封邮件</span>
            <span class="page-info">1/1页</span>
            <span class="pagination-controls">
              <button class="nav-btn" @click="prevPage" :disabled="currentPage === 1">上一页</button>
              <button class="nav-btn" @click="nextPage" :disabled="currentPage === totalPages">下一页</button>
            </span>
          </div>
        </div>

        <!-- 邮件列表头部 -->
        <div class="mail-list-header">
          <span class="header-checkbox"></span>
          <span class="header-sender">发件人</span>
          <span class="header-subject">主题</span>
          <span class="header-time">保存时间</span>
        </div>

        <!-- 已删除提示信息 -->
        <div class="delete-notice">
          <span class="notice-text">
            "已删除"自动删除14天以上的邮件
          </span>
          <button class="upgrade-btn">关闭</button>
        </div>

        <!-- 邮件列表 -->
        <div class="mail-list">
          <div class="list-content">
            <div 
              v-for="mail in paginatedMails" 
              :key="mail.id" 
              class="mail-item"
              :class="{ 'unread': !mail.isRead, 'selected': selectedMails.includes(mail.id) }"
              @click="selectMail(mail.id)"
            >
              <div class="mail-checkbox">
                <input 
                  type="checkbox" 
                  v-model="selectedMails" 
                  :value="mail.id"
                  @click.stop
                />
              </div>
              
              <div class="mail-sender">{{ mail.sender }}</div>
              <div class="mail-subject">
                <span class="subject-text">{{ mail.subject }}</span>
                <span v-if="mail.hasAttachment" class="attachment-icon">📎</span>
              </div>
              <div class="mail-time">{{ formatTime(mail.time) }}</div>
            </div>

            <!-- 空状态 -->
            <div v-if="filteredMails.length === 0" class="empty-state">
              <div class="empty-icon">🗑️</div>
              <div class="empty-text">已删除文件夹为空</div>
              <div class="empty-desc">删除的邮件会在这里保存7天</div>
            </div>
          </div>
        </div>
      </div>
</template>

<script>
export default {
  name: 'DeletePage',
  data() {
    return {
      searchKeyword: '',
      currentPage: 1,
      itemsPerPage: 20,
      selectedMails: [],
      allSelected: false,
      showReportDropdown: false,
      showMarkDropdown: false,
      showMoveDropdown: false,
      showMoreActions: false,
      inboxCount: 99,
      
      // 模拟已删除的邮件数据
      deletedMails: [
        {
          id: 1,
          sender: '邮箱的朋友',
          subject: '担心错过重要邮件？别怕！邮箱大师随时提醒！',
          time: new Date('2024-01-20 22:00'),
          isRead: true,
          hasAttachment: false,
          deleteTime: new Date('2024-01-21 10:00')
        },
        {
          id: 2,
          sender: 'admin@scut.edu.cn',
          subject: '系统维护通知',
          time: new Date('2024-01-19 15:30'),
          isRead: false,
          hasAttachment: true,
          deleteTime: new Date('2024-01-20 09:00')
        },
        {
          id: 3,
          sender: 'newsletter@company.com',
          subject: '本周热门文章推荐',
          time: new Date('2024-01-18 08:45'),
          isRead: true,
          hasAttachment: false,
          deleteTime: new Date('2024-01-19 14:30')
        },
        {
          id: 4,
          sender: 'support@service.com',
          subject: '您的服务即将到期',
          time: new Date('2024-01-17 16:20'),
          isRead: false,
          hasAttachment: false,
          deleteTime: new Date('2024-01-18 11:15')
        },
        {
          id: 5,
          sender: 'teacher@scut.edu.cn',
          subject: '期末考试安排通知',
          time: new Date('2024-01-16 10:00'),
          isRead: true,
          hasAttachment: true,
          deleteTime: new Date('2024-01-17 08:30')
        }
      ]
    };
  },
  
  computed: {
    filteredMails() {
      if (!this.searchKeyword.trim()) {
        return this.deletedMails;
      }
      return this.deletedMails.filter(mail => 
        mail.subject.toLowerCase().includes(this.searchKeyword.toLowerCase()) ||
        mail.sender.toLowerCase().includes(this.searchKeyword.toLowerCase())
      );
    },
    
    totalPages() {
      return Math.ceil(this.filteredMails.length / this.itemsPerPage);
    },
    
    paginatedMails() {
      const start = (this.currentPage - 1) * this.itemsPerPage;
      const end = start + this.itemsPerPage;
      return this.filteredMails.slice(start, end);
    },
    
    visiblePages() {
      const pages = [];
      const maxVisible = 5;
      const start = Math.max(1, this.currentPage - Math.floor(maxVisible / 2));
      const end = Math.min(this.totalPages, start + maxVisible - 1);
      
      for (let i = start; i <= end; i++) {
        pages.push(i);
      }
      return pages;
    }
  },
  
  methods: {
    // 搜索功能
    handleSearch() {
      this.currentPage = 1;
    },
    
    // 分页功能
    goToPage(page) {
      this.currentPage = page;
    },
    
    prevPage() {
      if (this.currentPage > 1) {
        this.currentPage--;
      }
    },
    
    nextPage() {
      if (this.currentPage < this.totalPages) {
        this.currentPage++;
      }
    },
    
    // 选择功能
    toggleSelectAll() {
      if (this.allSelected) {
        this.selectedMails = this.paginatedMails.map(mail => mail.id);
      } else {
        this.selectedMails = [];
      }
    },
    
    selectMail(mailId) {
      const index = this.selectedMails.indexOf(mailId);
      if (index > -1) {
        this.selectedMails.splice(index, 1);
      } else {
        this.selectedMails.push(mailId);
      }
      this.updateAllSelected();
    },
    
    updateAllSelected() {
      this.allSelected = this.selectedMails.length === this.paginatedMails.length && this.paginatedMails.length > 0;
    },
    
    selectAll() {
      this.selectedMails = this.filteredMails.map(mail => mail.id);
      this.allSelected = true;
      this.showMoreActions = false;
    },
    
    clearSelection() {
      this.selectedMails = [];
      this.allSelected = false;
      this.showMoreActions = false;
    },
    
    // 邮件操作
    permanentDelete() {
      if (this.selectedMails.length === 0) return;
      
      if (confirm(`确定要彻底删除选中的 ${this.selectedMails.length} 封邮件吗？删除后无法恢复！`)) {
        this.deletedMails = this.deletedMails.filter(mail => !this.selectedMails.includes(mail.id));
        this.selectedMails = [];
        this.allSelected = false;
        this.currentPage = Math.min(this.currentPage, this.totalPages || 1);
        this.$message?.success('邮件已彻底删除');
      }
    },
    
    restoreSelected() {
      if (this.selectedMails.length === 0) return;
      
      if (confirm(`确定要恢复选中的 ${this.selectedMails.length} 封邮件到收件箱吗？`)) {
        // 这里应该调用API将邮件恢复到收件箱
        this.deletedMails = this.deletedMails.filter(mail => !this.selectedMails.includes(mail.id));
        this.selectedMails = [];
        this.allSelected = false;
        this.$message?.success('邮件已恢复到收件箱');
      }
    },
    
    // 标记操作
    markAsRead() {
      this.selectedMails.forEach(mailId => {
        const mail = this.deletedMails.find(m => m.id === mailId);
        if (mail) mail.isRead = true;
      });
      this.showMarkDropdown = false;
    },
    
    markAsUnread() {
      this.selectedMails.forEach(mailId => {
        const mail = this.deletedMails.find(m => m.id === mailId);
        if (mail) mail.isRead = false;
      });
      this.showMarkDropdown = false;
    },
    
    markAsStarred() {
      this.selectedMails.forEach(mailId => {
        const mail = this.deletedMails.find(m => m.id === mailId);
        if (mail) mail.isStarred = true;
      });
      this.showMarkDropdown = false;
    },
    
    // 移动操作
    moveToInbox() {
      this.restoreSelected();
      this.showMoveDropdown = false;
    },
    
    moveToSpam() {
      if (confirm('确定要将选中邮件移动到垃圾邮件文件夹吗？')) {
        // 实际应用中这里会调用API
        this.deletedMails = this.deletedMails.filter(mail => !this.selectedMails.includes(mail.id));
        this.selectedMails = [];
        this.allSelected = false;
        this.$message?.success('邮件已移动到垃圾邮件');
      }
      this.showMoveDropdown = false;
    },
    
    moveToDraft() {
      if (confirm('确定要将选中邮件移动到草稿箱吗？')) {
        this.deletedMails = this.deletedMails.filter(mail => !this.selectedMails.includes(mail.id));
        this.selectedMails = [];
        this.allSelected = false;
        this.$message?.success('邮件已移动到草稿箱');
      }
      this.showMoveDropdown = false;
    },
    
    // 举报操作
    reportSpam() {
      if (this.selectedMails.length === 0) return;
      this.$message?.success('已举报为垃圾邮件');
      this.showReportDropdown = false;
    },
    
    reportPhishing() {
      if (this.selectedMails.length === 0) return;
      this.$message?.success('已举报为钓鱼邮件');
      this.showReportDropdown = false;
    },
    
    // 其他操作
    exportSelected() {
      if (this.selectedMails.length === 0) return;
      this.$message?.success('邮件导出功能开发中...');
      this.showMoreActions = false;
    },
    
    refreshList() {
      // 模拟刷新
      this.$message?.success('列表已刷新');
    },
    
    // 导航功能
    goToCompose() {
      this.$router.push('/edit');
    },
    
    goToInbox() {
      this.$router.push('/main');
    },
    
    goToStarred() {
      this.$router.push('/star');
    },
    
    goToDraft() {
      this.$router.push('/draft');
    },
    
    // 时间格式化
    formatTime(time) {
      const now = new Date();
      const diff = now - time;
      const days = Math.floor(diff / (1000 * 60 * 60 * 24));
      
      if (days === 0) {
        return time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
      } else if (days === 1) {
        return '昨天';
      } else if (days < 7) {
        return `${days}天前`;
      } else {
        return time.toLocaleDateString('zh-CN');
      }
    }
  },
  
  watch: {
    selectedMails() {
      this.updateAllSelected();
    },
    
    searchKeyword() {
      this.currentPage = 1;
    }
  },
  
  mounted() {
    // 关闭下拉菜单的点击事件
    document.addEventListener('click', (e) => {
      if (!e.target.closest('.mark-dropdown')) {
        this.showMarkDropdown = false;
      }
      if (!e.target.closest('.move-dropdown')) {
        this.showMoveDropdown = false;
      }
      if (!e.target.closest('.toolbar-btn')) {
        this.showReportDropdown = false;
        this.showMoreActions = false;
      }
    });
  }
};
</script>

<style scoped>
/* 邮件区域 */
.mail-area {
  flex: 1;
  background: #fff;
  display: flex;
  flex-direction: column;
  margin-top: 80px;
  border-radius: 0 0 20px 20px;
  margin-left: 0;
  height: calc(100vh - 80px);
  overflow-y: auto;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

/* 工具栏 */
.mail-toolbar {
  padding: 15px 20px;
  border-bottom: 1px solid #e6f2fb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8faff;
  border-radius: 0;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
}

.toolbar-btn {
  padding: 8px 16px;
  background: #fff;
  border: 1px solid #cce2fa;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #1f74c0;
  transition: all 0.2s ease;
}

.toolbar-btn:hover {
  background: #e6f2fb;
  border-color: #1f74c0;
}

.toolbar-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  background: #f5f7fa;
}

.restore-btn {
  background: #4CAF50;
  color: white;
  border-color: #4CAF50;
}

.restore-btn:hover:not(:disabled) {
  background: #45a049;
}

.select-all-checkbox {
  margin-right: 12px;
  width: 16px;
  height: 16px;
  accent-color: #1f74c0;
}

.dropdown-menu {
  position: absolute;
  top: 100%;
  left: 0;
  background: white;
  border: 1px solid #cce2fa;
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(31, 116, 192, 0.1);
  z-index: 100;
  min-width: 140px;
}

.dropdown-menu button {
  width: 100%;
  padding: 10px 16px;
  background: none;
  border: none;
  text-align: left;
  cursor: pointer;
  font-size: 14px;
  color: #333;
  transition: background-color 0.2s ease;
}

.dropdown-menu button:hover {
  background: #e6f2fb;
  color: #1f74c0;
}

.mark-dropdown, .move-dropdown {
  position: relative;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 14px;
  color: #666;
}

.pagination-controls {
  display: flex;
  gap: 8px;
}

.nav-btn {
  padding: 6px 12px;
  background: #fff;
  border: 1px solid #cce2fa;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  color: #1f74c0;
}

.nav-btn:hover:not(:disabled) {
  background: #e6f2fb;
}

.nav-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 邮件列表头部 */
.mail-list-header {
  padding: 14px 20px;
  border-bottom: 1px solid #e6f2fb;
  display: flex;
  align-items: center;
  background: #f5f7fa;
  font-weight: bold;
  color: #333;
  font-size: 14px;
}

.header-checkbox {
  width: 50px;
}

.header-sender {
  width: 220px;
}

.header-subject {
  flex: 1;
}

.header-time {
  width: 140px;
  text-align: right;
}

/* 删除提示 */
.delete-notice {
  padding: 12px 20px;
  background: #fff3cd;
  border-bottom: 1px solid #ffeaa7;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
}

.notice-text {
  color: #856404;
}

.upgrade-btn {
  padding: 6px 16px;
  background: #1f74c0;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 13px;
  transition: background-color 0.2s ease;
}

.upgrade-btn:hover {
  background: #1a5a8c;
}

/* 邮件列表 */
.mail-list {
  flex: 1;
  overflow-y: auto;
  background: #fff;
}

.mail-item {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f4f8;
  display: flex;
  align-items: center;
  cursor: pointer;
  transition: background-color 0.2s ease;
  background: #fff;
}

.mail-item:hover {
  background: #f8faff;
}

.mail-item.selected {
  background: #e6f2fb;
  border-left: 4px solid #1f74c0;
}

.mail-item.unread {
  font-weight: bold;
  background: #fafbfc;
}

.mail-item.unread:hover {
  background: #f0f4f8;
}

.mail-checkbox {
  width: 50px;
  display: flex;
  align-items: center;
  justify-content: flex-start;
}

.mail-checkbox input[type="checkbox"] {
  width: 16px;
  height: 16px;
  accent-color: #1f74c0;
}

.mail-sender {
  width: 220px;
  font-size: 14px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mail-subject {
  flex: 1;
  font-size: 14px;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
  padding-right: 10px;
}

.subject-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.attachment-icon {
  font-size: 14px;
  color: #666;
  flex-shrink: 0;
}

.mail-time {
  width: 140px;
  text-align: right;
  font-size: 13px;
  color: #999;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: #999;
}

.empty-icon {
  font-size: 56px;
  margin-bottom: 20px;
  opacity: 0.6;
}

.empty-text {
  font-size: 20px;
  margin-bottom: 12px;
  color: #666;
  font-weight: 500;
}

.empty-desc {
  font-size: 14px;
  color: #999;
}

/* 响应式设计 */
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
  
  .mail-item {
    padding: 12px 16px;
  }
  
  .mail-sender {
    width: 150px;
  }
  
  .header-sender {
    width: 150px;
  }
  
  .mail-time, .header-time {
    width: 100px;
  }
}
</style>
