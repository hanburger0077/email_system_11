<template>
  <div class="mail-list">
    <div class="mail-toolbar">
      <div class="toolbar-left">
        <el-checkbox 
          v-model="allSelected" 
          @change="toggleSelectAll" 
          class="select-all-checkbox" 
        />

        <el-tooltip content="删除" placement="bottom">
          <button  
            :disabled="selectedMails.length === 0"
            @click="deleteSelected"
            class="toolbar-button delete-button"
          >
            <img src="../main/assets/mark5.png" alt="删除" />
          </button>
        </el-tooltip>
        
        <el-tooltip content="刷新" placement="bottom">
          <button 
            class="toolbar-button" 
            @click="loadDrafts"
          >
            <img src="../main/assets/mark3.png" alt="刷新" />
          </button>
        </el-tooltip>
        
        <el-tooltip content="全部删除" placement="bottom">
          <el-button 
            class="delete-all-button" 
            @click="deleteAll"
            :disabled="draftMails.length === 0"
          >
            全部删除
          </el-button>
        </el-tooltip>
      </div>

      <div class="toolbar-right">
        <span class="mail-count">{{ draftMails.length }} 封草稿</span>
        <!-- 当没有草稿时，显示"暂无分页"而不是页码信息 -->
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
      <span class="column sender">收件人</span>
      <span class="column subject">主题</span>
      <span class="column time">时间</span>
    </div>

    <div class="list-content" v-if="!isLoading">
      <div class="mail-items" v-if="draftMails.length > 0">
        <div 
          v-for="mail in draftMails" 
          :key="mail.mail_id" 
          class="mail-item"
        >
          <div class="checkbox-container">
            <el-checkbox v-model="selectedMails" :value="mail.mail_id" class="item-checkbox" @click.stop />
          </div>
          <div class="mail-content" @click="editDraft(mail)">
            <span class="column sender">{{ mail.receiver_email || '(无收件人)' }}</span>
            <span class="column subject">{{ mail.subject || '(无主题)' }}</span>
            <span class="column time">{{ formatTime(mail.create_at) }}</span>
          </div>
        </div>
      </div>

      <div v-if="draftMails.length === 0" class="empty-message">
        草稿箱暂无邮件
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
  name: 'DraftPage',
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
      draftMails: [],
      isLoading: false
    };
  },
  computed: {
    paginatedDrafts() {
      return this.draftMails.map((mail, i) => ({
        ...mail,
        globalIndex: i
      }));
    }
  },
  methods: {
    // 加载草稿邮件列表
    async loadDrafts() {
      this.isLoading = true;
      
      try {
        // 调用后端API获取草稿列表
        const response = await fetch(`/api/mail/DRAFT/pages/${this.currentPage}`);
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          // 成功获取数据
          this.draftMails = result.data || [];
          // 更新总页数
          this.totalPages = parseInt(result.message, 10) || 0;
          
          // 如果当前页超出范围，重新加载正确的页
          if (this.currentPage > this.totalPages && this.totalPages > 0) {
            this.currentPage = this.totalPages;
            this.loadDrafts();
            return;
          }
        } else if (result.reason && (
            result.reason.includes('empty') || 
            result.reason.includes('为空') || 
            result.reason.includes('不存在') ||
            result.message === '操作失败'
          )) {
          // 草稿箱为空的情况，不显示错误，只清空数据
          console.log('草稿箱为空:', result.reason || result.message);
          this.draftMails = [];
          this.totalPages = 0;
        } else {
          // 真正的错误情况
          console.error('加载草稿失败:', result);
          this.$message.error(`加载草稿失败: ${result.message}`);
          this.draftMails = [];
          this.totalPages = 0;
        }
      } catch (error) {
        console.error('加载草稿箱出错:', error);
        this.$message.error('加载草稿失败，请检查网络连接');
        this.draftMails = [];
        this.totalPages = 0;
      } finally {
        this.isLoading = false;
      }
    },
    
    // 解析日期为Date对象
    parseDate(dateStr) {
      if (!dateStr) return new Date(0);
      
      let date;
      try {
        if (typeof dateStr === 'number') {
          date = new Date(dateStr);
        } else if (typeof dateStr === 'string') {
          date = new Date(dateStr);
        } else {
          date = new Date();
        }
        
        if (isNaN(date.getTime())) {
          console.warn(`无法解析的日期: ${dateStr}`);
          date = new Date(0);
        }
      } catch (error) {
        console.error(`日期解析错误: ${error}`, dateStr);
        date = new Date(0);
      }
      
      return date;
    },
    
    // 格式化时间显示
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
        
        // 返回完整格式的日期时间
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
      } catch (error) {
        console.error('格式化时间错误:', error);
        return '未知时间';
      }
    },
    
    // 全选/取消全选
    toggleSelectAll() {
      if (this.allSelected) {
        this.selectedMails = this.draftMails.map(mail => mail.mail_id);
      } else {
        this.selectedMails = [];
      }
    },
    
    // 删除选中的草稿
    async deleteSelected() {
      if (this.selectedMails.length === 0) return;
      
      try {
        this.$confirm(`确认删除选中的 ${this.selectedMails.length} 封草稿？`, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(async () => {
          this.isLoading = true;
          
          let successCount = 0;
          let failCount = 0;
          
          // 逐个删除选中的草稿，调用回收站相关接口
          for (const mailId of this.selectedMails) {
            try {
              const response = await fetch(`/api/mail/TRASH/mails/${mailId}/delete`, {
                method: 'DELETE'
              });
              
              const result = await response.json();
              
              if (result.code === 'code.ok') {
                successCount++;
              } else {
                failCount++;
                console.error(`删除草稿 ${mailId} 失败:`, result.message);
              }
            } catch (error) {
              failCount++;
              console.error(`删除草稿 ${mailId} 失败:`, error);
            }
          }
          
          // 显示结果消息
          if (successCount > 0) {
            if (failCount === 0) {
              this.$message.success(`成功删除 ${successCount} 封草稿`);
            } else {
              this.$message.warning(`成功删除 ${successCount} 封草稿，${failCount} 封删除失败`);
            }
          } else {
            this.$message.error('删除草稿失败');
          }
          
          // 重置选择状态
          this.selectedMails = [];
          this.allSelected = false;
          
          // 重新加载草稿列表
          this.loadDrafts();
        }).catch(() => {
          // 取消删除
          this.$message.info('已取消删除操作');
        });
      } catch (error) {
        console.error('删除草稿出错:', error);
        this.$message.error('删除草稿失败');
      } finally {
        this.isLoading = false;
      }
    },
    
    // 删除所有草稿
    async deleteAll() {
      if (this.draftMails.length === 0) return;
      
      try {
        this.$confirm('确认删除所有草稿？此操作无法撤销。', '警告', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        }).then(async () => {
          this.isLoading = true;
          
          const mailIds = this.draftMails.map(mail => mail.mail_id);
          let successCount = 0;
          let failCount = 0;
          
          // 逐个删除所有草稿，调用回收站相关接口
          for (const mailId of mailIds) {
            try {
              const response = await fetch(`/api/mail/TRASH/mails/${mailId}/delete`, {
                method: 'DELETE'
              });
              
              const result = await response.json();
              
              if (result.code === 'code.ok') {
                successCount++;
              } else {
                failCount++;
              }
            } catch (error) {
              failCount++;
              console.error(`删除草稿 ${mailId} 失败:`, error);
            }
          }
          
          // 显示结果消息
          if (successCount > 0) {
            if (failCount === 0) {
              this.$message.success(`已删除全部 ${successCount} 封草稿`);
            } else {
              this.$message.warning(`成功删除 ${successCount} 封草稿，${failCount} 封删除失败`);
            }
          } else {
            this.$message.error('删除草稿失败');
          }
          
          // 重置选择和分页
          this.selectedMails = [];
          this.allSelected = false;
          this.currentPage = 1;
          
          // 重新加载草稿列表
          this.loadDrafts();
        }).catch(() => {
          // 取消删除
        });
      } catch (error) {
        console.error('删除所有草稿出错:', error);
        this.$message.error('删除草稿失败');
      } finally {
        this.isLoading = false;
      }
    },
    
    // 上一页
    prevPage() {
      if (this.currentPage > 1) {
        this.currentPage--;
        this.loadDrafts();
      }
    },
    
    // 下一页
    nextPage() {
      if (this.currentPage < this.totalPages) {
        this.currentPage++;
        this.loadDrafts();
      }
    },
    
    // 编辑草稿
    editDraft(mail) {
      // 跳转到写信页面，并传递草稿ID
      this.$router.push({
        path: '/edit',
        query: { 
          draft: 'true',
          draftId: mail.mail_id
        }
      });
    }
  },
  created() {
    // 组件创建时加载草稿
    this.loadDrafts();
  },
  mounted() {
    // 可以添加事件监听，例如监听草稿保存事件
    window.addEventListener('storage', (event) => {
      if (event.key === 'draftSaved') {
        this.loadDrafts();
      }
    });
  },
  beforeUnmount() {
    // 移除事件监听器
    window.removeEventListener('storage', this.handleStorageEvent);
  }
};
</script>

<style scoped>
.mail-list {
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

.toolbar-left > * {
  margin: 0;
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

/* 工具栏按钮统一样式 */
.toolbar-button {
  background-color: #fff;
  border: 1px solid #dcdfe6;
  width: 32px;
  height: 32px;
  padding: 0; /* 移除内边距，让图标填充整个按钮 */
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
  box-sizing: border-box;
  overflow: hidden; /* 确保图标不会超出按钮边界 */
}

.toolbar-button:hover:not(:disabled) {
  background-color: #f5f7fa; /* 背景稍微变灰 */
  border-color: #c0c4cc;
}

.toolbar-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
  background: #fff;
}

.toolbar-button img {
  width: 100%; /* 图标填充整个按钮宽度 */
  height: 100%; /* 图标填充整个按钮高度 */
  object-fit: contain; /* 保持图标比例，完全显示在按钮内 */
  transition: all 0.2s;
}

.delete-button img {
  width: 100%; /* 删除按钮图标也填充整个按钮 */
  height: 100%;
  object-fit: contain;
}

.delete-all-button {
  padding: 0;
  margin: 0;
  width: 70px;
  height: 32px;
  min-width: 32px;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #f56c6c;
  background: #f56c6c;
  color: white;
  font-size: 14px;
}

.delete-all-button:hover {
  background: #f78989;
  border-color: #f78989;
}

.delete-all-button:active {
  background: #e64242;
  border-color: #e64242;
}

.delete-all-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #f78989;
}

.pagination-controls {
  display: flex;
  gap: 8px;
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