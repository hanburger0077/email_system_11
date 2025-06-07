<template>
  <div 
    class="mail-item"
    :class="{ 
      'unread': !mail.isRead, 
      'selected': isSelected 
    }"
    @click="handleSelectMail"
  >
    <div class="mail-checkbox">
      <el-checkbox 
        :model-value="isSelected"
        @change="handleCheckboxChange"
        @click.stop
      />
    </div>
    
    <div class="mail-sender">{{ mail.sender }}</div>
    <div class="mail-subject">
      <span class="subject-text">{{ mail.subject }}</span>
      <el-icon v-if="mail.hasAttachment" class="attachment-icon">
        <Paperclip />
      </el-icon>
    </div>
    <div class="mail-time">{{ formattedTime }}</div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { Paperclip } from '@element-plus/icons-vue'
import { formatTime } from '../utils/dateUtils.js'

const props = defineProps({
  mail: {
    type: Object,
    required: true
  },
  isSelected: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['select', 'toggle-select'])

const formattedTime = computed(() => {
  return formatTime(props.mail.time)
})

function handleSelectMail() {
  emit('select', props.mail.id)
}

function handleCheckboxChange(checked) {
  emit('toggle-select', props.mail.id, checked)
}
</script>

<style scoped>
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
  border-left: 4px solid #409eff;
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
  color: #666;
  flex-shrink: 0;
}

.mail-time {
  width: 140px;
  text-align: right;
  font-size: 13px;
  color: #999;
}

@media (max-width: 768px) {
  .mail-item {
    padding: 12px 16px;
  }
  
  .mail-sender {
    width: 150px;
  }
  
  .mail-time {
    width: 100px;
  }
}
</style> 