<template>
  <div class="header-container">
    <div class="brand-section">
      <img src="@/assets/logo.jpg" alt="华南理工大学" class="logo" />
      <div class="brand-name">
        <span class="brand-flow">Flow</span>
        <span class="brand-mail">mail</span>
      </div>
    </div>
    <div class="header-content">
      <div class="search-container">
        <el-input 
          v-model="searchKeyword"
          placeholder="邮箱搜索" 
          class="search-input"
          @input="handleSearchInput"
          @focus="showSearchPanel = true"
        >
          <template #suffix>
            <el-button 
              :icon="Search" 
              @click="handleSearch"
              type="text"
              class="search-button"
            />
          </template>
        </el-input>
        
        <!-- 搜索面板 -->
        <div v-show="showSearchPanel" class="search-panel" @click.stop>
          <!-- 搜索类型选项卡 -->
          <div class="search-tabs">
            <div 
              v-for="tab in searchTabs" 
              :key="tab.key"
              :class="['search-tab', { active: activeTab === tab.key }]"
              @click="activeTab = tab.key; handleSearchInput()"
            >
              {{ tab.label }}
            </div>
          </div>
          
          <!-- 搜索结果列表 -->
          <div v-if="searchResults.length > 0" class="search-results">
            <div 
              v-for="mail in searchResults" 
              :key="`${mail.source}-${mail.mail_id}`"
              class="search-result-item"
              @click="openMail(mail)"
            >
              <div class="result-header">
                <span class="result-source">{{ mail.source === 'INBOX' ? '收件箱' : '已发送' }}</span>
                <span class="result-time">{{ formatTime(mail.create_at) }}</span>
              </div>
              <div class="result-subject">{{ mail.subject || '(无主题)' }}</div>
              <div class="result-content">
                <span class="result-from">{{ mail.source === 'INBOX' ? mail.sender_email : mail.receiver_email }}</span>
              </div>
            </div>
          </div>
          
          <!-- 无结果提示 -->
          <div v-else-if="searchKeyword && !isSearching" class="no-results">
            暂无搜索结果
          </div>
          
          <!-- 搜索中提示 -->
          <div v-if="isSearching" class="searching">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>搜索中...</span>
          </div>
        </div>
      </div>
      <div class="header-right" @mouseleave="handleAccountMouseLeave">
        <div class="account-circle" @click="handleAccountClick" @mouseenter="handleAccountMouseEnter">
          <span>账号</span>
          <DropDown v-show="isMenuVisible" class="account-dropdown" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Loading } from '@element-plus/icons-vue'
import DropDown from './drop-down.vue'

const router = useRouter()

// 搜索相关数据
const searchKeyword = ref('')
const showSearchPanel = ref(false)
const activeTab = ref('all')
const searchResults = ref([])
const isSearching = ref(false)
const allMails = ref([]) // 缓存所有邮件数据

// 移除自动搜索监听，只在用户主动输入时搜索

// 搜索选项卡
const searchTabs = reactive([
  { key: 'all', label: '全部' },
  { key: 'subject', label: '主题' },
  { key: 'body', label: '正文' },
  { key: 'from', label: '发件人' },
  { key: 'to', label: '收件人' }
])

// 原有的下拉菜单逻辑
const isMenuVisible = ref(false)
const timer = ref(null)

const handleAccountClick = () => {
  console.log('账号按钮点击');
};

const handleAccountMouseEnter = () => {
  // console.log('账号按钮鼠标进入');
  isMenuVisible.value = true
  clearTimeout(timer.value)
};

const handleAccountMouseLeave = () => {
  // console.log('账号按钮鼠标离开');
  clearTimeout(timer.value)
  timer.value = setTimeout(() => {
    isMenuVisible.value = false
  }, 300)
};

// 搜索功能
let searchTimer = null
const handleSearchInput = () => {
  // 清除之前的定时器
  clearTimeout(searchTimer)
  
  // 添加100ms防抖，避免输入过程中频繁搜索
  searchTimer = setTimeout(() => {
    performSearch()
  }, 100)
}

const handleSearch = () => {
  if (searchKeyword.value?.trim()) {
    performSearch()
  }
}

const handleClearSearch = () => {
  searchKeyword.value = ''
  searchResults.value = []
  showSearchPanel.value = false
}

// 执行搜索
const performSearch = () => {
  const keyword = searchKeyword.value?.trim()
  if (!keyword) {
    searchResults.value = []
    return
  }
  
  // 使用本地搜索，瞬时返回结果
  const results = searchLocally(keyword)
  searchResults.value = results.slice(0, 10) // 限制显示前10条结果
}

// 本地搜索函数
const searchLocally = (keyword) => {
  if (!allMails.value.length) {
    // 如果没有缓存数据，异步加载
    loadAllMails()
    return []
  }
  
  const lowerKeyword = keyword.toLowerCase()
  
  const results = allMails.value.filter(mail => {
    let match = false
    switch (activeTab.value) {
      case 'subject':
        match = mail.subject?.toLowerCase().includes(lowerKeyword)
        break
      case 'body':
        match = mail.content?.toLowerCase().includes(lowerKeyword)
        break
      case 'from':
        // 发件人搜索：统一搜索sender_email字段
        match = mail.sender_email?.toLowerCase().includes(lowerKeyword)
        break
      case 'to':
        // 收件人搜索：统一搜索receiver_email字段
        match = mail.receiver_email?.toLowerCase().includes(lowerKeyword)
        break
      case 'all':
      default:
        match = (
          mail.subject?.toLowerCase().includes(lowerKeyword) ||
          mail.content?.toLowerCase().includes(lowerKeyword) ||
          mail.sender_email?.toLowerCase().includes(lowerKeyword) ||
          mail.receiver_email?.toLowerCase().includes(lowerKeyword)
        )
        break
    }
    return match
  }).sort((a, b) => new Date(b.create_at) - new Date(a.create_at))
  
  return results
}

// 加载所有邮件数据到缓存
const loadAllMails = async () => {
  try {
    const [inboxResponse, sentResponse] = await Promise.all([
      fetch('/api/mail/INBOX/pages/1'),
      fetch('/api/mail/SENT/pages/1')
    ])
    
    const [inboxResult, sentResult] = await Promise.all([
      inboxResponse.json(),
      sentResponse.json()
    ])
    
    const inboxMails = inboxResult.code === 'code.ok' ? (inboxResult.data || []) : []
    const sentMails = sentResult.code === 'code.ok' ? (sentResult.data || []) : []
    
    allMails.value = [
      ...inboxMails.map(mail => ({ ...mail, source: 'INBOX' })),
      ...sentMails.map(mail => ({ ...mail, source: 'SENT' }))
    ]
    
    // 重新执行搜索
    if (searchKeyword.value?.trim()) {
      performSearch()
    }
  } catch (error) {
    console.error('加载邮件数据失败:', error)
  }
}



// 打开邮件详情
const openMail = (mail) => {
  showSearchPanel.value = false
  
  // 跳转到对应的邮件列表页面并定位到具体邮件
  if (mail.source === 'INBOX') {
    router.push({
      path: '/main',
      query: { folder: 'INBOX', mailId: mail.mail_id }
    })
  } else {
    router.push({
      path: '/sent',
      query: { mailId: mail.mail_id }
    })
  }
}

// 格式化时间
const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  
  if (diff < 24 * 60 * 60 * 1000) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else {
    return date.toLocaleDateString('zh-CN', { month: '2-digit', day: '2-digit' })
  }
}

// 点击外部关闭搜索面板
const handleClickOutside = (event) => {
  if (!event.target.closest('.search-container')) {
    showSearchPanel.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  // 立即加载所有邮件数据到缓存
  loadAllMails()
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  clearTimeout(searchTimer)
})
</script>

<style lang="scss" scoped>
.header-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 68px;
  background: #fff;
  border-bottom: 2px solid #cce2fa;
  display: flex;
  align-items: center;
  z-index: 100;
  padding: 0 24px;
}

.brand-section {
  display: flex;
  align-items: center;
  margin-right: 16px;
  
  .logo {
    height: 40px;
    margin-right: 12px;
  }
  
  .brand-name {
    display: flex;
    align-items: baseline;
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
    
    .brand-flow {
      font-size: 24px;
      font-weight: 300;
      color: #2c3e50;
      letter-spacing: -0.5px;
    }
    
    .brand-mail {
      font-size: 24px;
      font-weight: 600;
      color: #409eff;
      letter-spacing: -0.5px;
      margin-left: 2px;
    }
  }
}

.header-content {
  flex: 1;
  display: flex;
  align-items: center;
  max-width: calc(100vw - 200px); /* 为侧边栏留出空间 */
  margin-left: 10px; /* 与侧边栏宽度对齐 */
}

.search-container {
  position: relative;
  width: 320px;
  max-width: 600px;
  margin-right: auto; /* 推向左边 */
}

.search-input {
  height: 36px;
}

.search-button {
  padding: 0;
  margin: 0;
  min-height: auto;
  color: #909399;
  
  &:hover {
    color: #409eff;
  }
}

.search-panel {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 1000;
  max-height: 400px;
  overflow-y: auto;
}

.search-tabs {
  display: flex;
  border-bottom: 1px solid #e4e7ed;
  background: #f5f7fa;
}

.search-tab {
  flex: 1;
  padding: 8px 12px;
  text-align: center;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
  
  &:hover {
    background: #ecf5ff;
    color: #409eff;
  }
  
  &.active {
    background: #409eff;
    color: #fff;
  }
}

.search-results {
  max-height: 300px;
  overflow-y: auto;
}

.search-result-item {
  padding: 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background-color 0.2s;
  
  &:hover {
    background: #f5f7fa;
  }
  
  &:last-child {
    border-bottom: none;
  }
}

.result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.result-source {
  font-size: 11px;
  color: #909399;
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 2px;
}

.result-time {
  font-size: 11px;
  color: #c0c4cc;
}

.result-subject {
  font-size: 13px;
  color: #303133;
  font-weight: 500;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-content {
  font-size: 12px;
  color: #606266;
}

.result-from {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.no-results, .searching {
  padding: 20px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

.searching {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.header-right {
  margin-left: auto;
  margin-right: 50px; /* 与右边保持24px距离 */
}

.account-circle {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #1f74c0;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s, transform 0.1s;

  user-select: none;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative; /* 确保定位正确 */
  
  &:hover {
    background-color: #1a5f9e;
  }

  // &:active {
  //   transform: scale(0.95);
  // }
  
  span {
    display: block; /* 确保文本显示 */
    line-height: 1; /* 设置行高 */
  }
  .account-dropdown{
    position: absolute;
    top: 36px;
    right: 0px;
    cursor: auto;
  }
}

@media (max-width: 768px) {
  .brand-section {
    .logo {
      height: 36px;
      margin-right: 8px;
    }
    
    .brand-name {
      .brand-flow,
      .brand-mail {
        font-size: 20px;
      }
    }
  }
  
  .search-container {
    width: 250px;
  }
}
</style>