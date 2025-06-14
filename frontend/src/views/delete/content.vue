<template>
  <!-- 主邮件区域 -->
  <div class="mail-area">
    <!-- 邮件操作栏 -->
    <MailToolbar
      :all-selected="allSelected"
      :selected-count="selectedCount"
      :mail-count="filteredMails.length"
      :current-page="currentPage"
      :total-pages="totalPages"
      :is-loading="isLoading"
      @select-all="handleSelectAll"
      @permanent-delete="permanentDelete"
      @restore="restoreSelected"
      @mark-as="markAs"
      @move-to="moveTo"
      @report="handleReport"
      @more-action="handleMoreAction"
      @refresh="handleRefresh"
      @prev-page="prevPage"
      @next-page="nextPage"
    />

    <!-- 邮件列表 -->
    <MailList
      :mails="paginatedMails"
      :selected-mails="selectedMails"
      @select-mail="selectMail"
      @toggle-select="handleToggleSelect"
    />
  </div>
</template>

<script setup>
import { useMailManagement } from './composables/useMailManagement.js'
import MailToolbar from './components/MailToolbar.vue'
import MailList from './components/MailList.vue'

// 使用 composable
const {
  // 响应式数据
  searchKeyword,
  currentPage,
  itemsPerPage,
  selectedMails,
  allSelected,
  deletedMails,
  isLoading,
  
  // 计算属性
  filteredMails,
  totalPages,
  paginatedMails,
  selectedCount,
  hasSelection,
  
  // 方法
  handleSelectAll,
  selectMail,
  updateAllSelected,
  selectAll,
  clearSelection,
  permanentDelete,
  restoreSelected,
  markAs,
  moveTo,
  handleMoreAction,
  goToPage,
  prevPage,
  nextPage,
  handleSearch
} = useMailManagement()

// 其他处理方法
function handleReport(type) {
  // 举报逻辑已在组件内处理
}

function handleRefresh() {
  // 刷新逻辑已在组件内处理
}

function handleToggleSelect(mailId, checked) {
  if (checked) {
    if (!selectedMails.value.includes(mailId)) {
      selectedMails.value.push(mailId)
    }
  } else {
    const index = selectedMails.value.indexOf(mailId)
    if (index > -1) {
      selectedMails.value.splice(index, 1)
    }
  }
  updateAllSelected()
}
</script>

<style scoped>
/* 邮件区域 */
.mail-area {
  flex: 1;
  background: #fff;
  display: flex;
  flex-direction: column;
  border-radius: 0 0 20px 20px;
  margin-left: 0;
  height: calc(100vh - 80px);
  overflow-y: auto;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
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

.mark-icon {
  width: 24px;
  height: 24px;
  object-fit: cover;
  margin-left: 4px;
  vertical-align: middle;
}
</style>
