<template>
  <div class="mail-toolbar">
    <div class="toolbar-left">
      <el-checkbox 
        :model-value="selectAllState" 
        :indeterminate="selectAllState === 'partial'"
        @change="handleSelectAllToggle"
        class="select-all-checkbox"
      />
      
      <el-button 
        type="danger" 
        :loading="isLoading"
        :disabled="selectedCount === 0"
        @click="handlePermanentDelete"
      >
        永久删除
      </el-button>

      <el-tooltip 
        :content="isMarked ? '标为未读' : '标为已读'"
        placement="bottom"
      >
        <el-button 
          @click="handleMarkToggle"
          :disabled="selectedCount === 0"
          class="mark-button"
        >
          <img 
            v-if="!imageError"
            :src="isMarked ? mark2Img : mark1Img" 
            class="mark-icon"
            alt="标记"
            @error="handleImageError"
          />
          <div v-else class="mark-icon mark-placeholder" :class="{ active: isMarked }">
          </div>
        </el-button>
      </el-tooltip>

      <!-- <el-dropdown @command="handleMoveCommand">
        <el-button>
          移动到 <el-icon><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="inbox">收件箱</el-dropdown-item>
            <el-dropdown-item command="spam">垃圾邮件</el-dropdown-item>
            <el-dropdown-item command="draft">草稿箱</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown> -->

      <el-tooltip 
        content="移回"
        placement="bottom"
      >
        <el-button 
          :loading="isLoading"
          :disabled="selectedCount === 0"
          @click="handleRestore"
          class="restore-button"
        >
          <img 
            v-if="!restoreImageError"
            :src="mark4Img" 
            class="restore-icon"
            alt="读取恢复"
            @error="handleRestoreImageError"
          />
          <div v-else class="restore-icon restore-placeholder">
            恢复
          </div>
        </el-button>
      </el-tooltip>
      
      <el-tooltip 
        content="刷新"
        placement="bottom"
      >
        <el-button 
          :loading="isLoading"
          @click="handleRefresh"
          class="refresh-button"
        >
          <img 
            v-if="!refreshImageError"
            :src="mark3Img" 
            class="refresh-icon"
            alt="刷新"
            @error="handleRefreshImageError"
          />
          <div v-else class="refresh-icon refresh-placeholder">
            刷新
          </div>
        </el-button>
      </el-tooltip>
    </div>

    <div class="toolbar-right">
      <span class="mail-count">{{ mailCount }} 封邮件</span>
      <span class="page-info">{{ currentPage }}/{{ totalPages }}页</span>
      <div class="pagination-controls">
        <el-button 
          size="small" 
          :disabled="currentPage === 1 || isLoading"
          @click="$emit('prev-page')"
        >
          上一页
        </el-button>
        <el-button 
          size="small" 
          :disabled="currentPage === totalPages || isLoading"
          @click="$emit('next-page')"
        >
          下一页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ArrowDown, Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import mark1Img from '../assets/mark1.png'
import mark2Img from '../assets/mark2.png'
import mark3Img from '../assets/mark3.png'
import mark4Img from '../assets/mark4.png'

// 标记状态
const isMarked = ref(false)
const imageError = ref(false)
const refreshImageError = ref(false)
const restoreImageError = ref(false)
// 全选状态：false=未选择, true=全选, 'partial'=部分选择
const selectAllState = ref(false)

function handleImageError() {
  imageError.value = true
}

function handleRefreshImageError() {
  refreshImageError.value = true
}

function handleRestoreImageError() {
  restoreImageError.value = true
}

const props = defineProps({
  allSelected: Boolean,
  selectedCount: Number,
  mailCount: Number,
  currentPage: Number,
  totalPages: Number,
  isLoading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits([
  'select-all',
  'permanent-delete', 
  'restore',
  'mark-as',
  'move-to',
  'refresh',
  'prev-page',
  'next-page'
])

function handleSelectAllToggle() {
  if (selectAllState.value === false || selectAllState.value === 'partial') {
    // 当前未选择或部分选择 -> 全选
    selectAllState.value = true
    emit('select-all', true)
  } else {
    // 当前全选 -> 取消全选
    selectAllState.value = false
    emit('select-all', false)
  }
}

async function handlePermanentDelete() {
  if (props.selectedCount === 0) return
  
  try {
    await ElMessageBox.confirm(
      `确定要彻底删除选中的 ${props.selectedCount} 封邮件吗？删除后无法恢复！`,
      '确认删除',
      { type: 'warning' }
    )
    emit('permanent-delete')
    ElMessage.success('邮件已彻底删除')
  } catch {
    // 用户取消
  }
}

function handleRestore() {
  if (props.selectedCount === 0) return
  
  emit('restore')
  ElMessage.success('邮件已恢复到收件箱')
}



function handleMarkToggle() {
  if (props.selectedCount === 0) return
  
  isMarked.value = !isMarked.value
  const markType = isMarked.value ? 'marked' : 'unmarked'
  emit('mark-as', markType)
  
  ElMessage.success(isMarked.value ? '已标记邮件' : '已取消标记')
}

function handleMoveCommand(command) {
  emit('move-to', command)
}



function handleRefresh() {
  emit('refresh')
  ElMessage.success('列表已刷新')
}

// 监听选择变化，更新复选框状态
function updateSelectAllState() {
  if (props.selectedCount === 0) {
    selectAllState.value = false
  } else if (props.allSelected) {
    selectAllState.value = true
  } else {
    selectAllState.value = 'partial'
  }
}

// 监听props变化
watch(() => [props.selectedCount, props.allSelected], updateSelectAllState, { immediate: true })
</script>

<style scoped>
.mail-toolbar {
  padding: 15px 20px;
  border-bottom: 1px solid #e6f2fb;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8faff;
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

.pagination-controls {
  display: flex;
  gap: 8px;
}

.mark-button {
  padding: 0;
  margin: 0;
  width: 32px;
  height: 32px;
  min-width: 32px;
  border-radius: 6px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mark-icon {
  width: 100%;
  height: 80%;
  object-fit: cover;
  display: block;
}

.mark-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.mark-button:disabled .mark-icon {
  filter: grayscale(100%);
}

.mark-placeholder {
  background-color: #e0e0e0;
  border: 2px solid #ccc;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.mark-placeholder.active {
  background-color: #4CAF50;
  border-color: #45a049;
}

.refresh-button {
  padding: 0;
  margin: 0;
  width: 32px;
  height: 32px;
  min-width: 32px;
  border-radius: 6px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.refresh-icon {
  width: 100%;
  height: 80%;
  object-fit: cover;
  display: block;
}

.refresh-placeholder {
  font-size: 12px;
  color: #666;
}

.restore-button {
  padding: 0;
  margin: 0;
  width: 32px;
  height: 32px;
  min-width: 32px;
  border-radius: 6px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.restore-icon {
  width: 100%;
  height: 80%;
  object-fit: cover;
  display: block;
}

.restore-placeholder {
  font-size: 12px;
  color: #666;
}

.restore-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.restore-button:disabled .restore-icon {
  filter: grayscale(100%);
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
}
</style> 