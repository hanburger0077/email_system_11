<template>
  <div class="mail-list-container">
    <!-- 邮件列表头部 -->
    <div class="mail-list-header">
      <span class="header-checkbox"></span>
      <span class="header-sender">发件人</span>
      <span class="header-subject">主题</span>
      <span class="header-time">保存时间</span>
    </div>

    <!-- 邮件列表 -->
    <div class="mail-list" v-if="mails.length > 0">
      <MailItem
        v-for="mail in mails" 
        :key="mail.id"
        :mail="mail"
        :is-selected="selectedMails.includes(mail.id)"
        @select="handleSelectMail"
        @toggle-select="handleToggleSelect"
      />
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <el-empty 
        description="已删除文件夹为空"
        :image-size="120"
      >
        <template #image>
          <el-icon size="80" color="#c0c4cc">
            <Delete />
          </el-icon>
        </template>
        <template #description>
          <p class="empty-text">已删除文件夹为空</p>
          <p class="empty-desc">删除的邮件会在这里保存7天</p>
        </template>
      </el-empty>
    </div>
  </div>
</template>

<script setup>
import { Delete } from '@element-plus/icons-vue'
import MailItem from './MailItem.vue'

const props = defineProps({
  mails: {
    type: Array,
    default: () => []
  },
  selectedMails: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['select-mail', 'toggle-select'])

function handleSelectMail(mailId) {
  emit('select-mail', mailId)
}

function handleToggleSelect(mailId, checked) {
  emit('toggle-select', mailId, checked)
}
</script>

<style scoped>
.mail-list-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
}

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

.mail-list {
  flex: 1;
  overflow-y: auto;
}

.empty-state {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
}

.empty-text {
  font-size: 18px;
  color: #666;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: #999;
}

@media (max-width: 768px) {
  .header-sender {
    width: 150px;
  }
  
  .header-time {
    width: 100px;
  }
}
</style> 