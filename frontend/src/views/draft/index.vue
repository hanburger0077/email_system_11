<template>
  <div class="mail-list">
    <div class="list-header">
      <input 
        type="checkbox" 
        v-model="allSelected" 
        @change="toggleSelectAll" 
        class="header-checkbox" 
      />
      <button 
        class="list-btn" 
        @click="deleteSelected"
      >删除选中</button>
      <button 
        class="list-btn" 
        @click="deleteAll"
      >全部删除</button>
      <span class="page-info">{{ currentPage }}/{{ totalPages }}页</span>
      <button 
        class="list-btn" 
        @click="prevPage" 
        :disabled="currentPage === 1"
      >上一页</button>
      <button 
        class="list-btn" 
        @click="nextPage" 
        :disabled="currentPage === totalPages"
      >下一页</button>
    </div>

    <div class="mail-header">
      <span class="column checkbox-col"></span>
      <span class="column sender">发件人</span>
      <span class="column subject">主题</span>
      <span class="column time">保存时间</span>
    </div>

    <div class="list-content">
      <div 
        v-for="(mail, index) in paginatedDrafts" 
        :key="mail.globalIndex" 
        class="mail-item"
        :class="{ 'unread': !mail.isRead }"
        @click="editDraft(mail)"
      >
        <div class="checkbox-container">
          <input 
            type="checkbox" 
            v-model="selectedMails" 
            :value="mail.globalIndex" 
            class="item-checkbox"
            @click.stop 
          />
        </div>
        <div class="mail-content">
          <span class="column sender">{{ mail.sender }}</span>
          <span class="column subject">{{ mail.subject }}</span>
          <span class="column time">{{ mail.time }}</span>
        </div>
      </div>

      <div v-if="draftMails.length === 0" class="empty-message">
        草稿箱暂无邮件
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DraftPage',
  data() {
    return {
      currentPage: 1,
      itemsPerPage: 10,
      selectedMails: [],
      allSelected: false,
      draftMails: [
        { 
          sender: "self@scut.edu.cn", 
          subject: "会议记录草稿", 
          time: "2025-06-02 10:30",
          isRead: false,
          content: "今天会议的主要内容是..."
        },
        { 
          sender: "self@scut.edu.cn", 
          subject: "项目计划草稿", 
          time: "2025-06-01 14:15",
          isRead: true,
          content: "初步计划分为三个阶段..."
        },
        { 
          sender: "self@scut.edu.cn", 
          subject: "请假申请草稿", 
          time: "2025-05-31 09:00",
          isRead: false,
          content: "因事需请假一天..."
        }
      ]
    };
  },
  computed: {
    totalPages() {
      return Math.ceil(this.draftMails.length / this.itemsPerPage);
    },
    paginatedDrafts() {
      const start = (this.currentPage - 1) * this.itemsPerPage;
      const end = start + this.itemsPerPage;
      return this.draftMails.slice(start, end).map((mail, i) => ({
        ...mail,
        globalIndex: start + i
      }));
    }
  },
  methods: {
    toggleSelectAll() {
      const indexes = this.paginatedDrafts.map(mail => mail.globalIndex);
      this.selectedMails = this.allSelected ? indexes : [];
    },
    deleteSelected() {
      if (this.selectedMails.length === 0) return;
      if (confirm('确认删除选中的草稿？')) {
        this.draftMails = this.draftMails.filter((_, index) => 
          !this.selectedMails.includes(index)
        );
        this.selectedMails = [];
        this.allSelected = false;
      }
    },
    deleteAll() {
      if (confirm('确认删除所有草稿？')) {
        this.draftMails = [];
        this.selectedMails = [];
        this.allSelected = false;
        this.currentPage = 1;
      }
    },
    prevPage() {
      if (this.currentPage > 1) this.currentPage--;
    },
    nextPage() {
      if (this.currentPage < this.totalPages) this.currentPage++;
    },
    editDraft(mail) {
      console.log('编辑草稿:', mail);
      // 这里可以跳转到写邮件页面，并且加载草稿内容
      this.$router.push({
        path: '/edit',
        query: { 
          draft: true,
          id: mail.globalIndex 
        }
      });
    }
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

.list-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.list-btn {
  padding: 4px 10px;
  border: 1px solid #cce2fa;
  background: #f5f7fa;
  color: #1f74c0;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.list-btn:hover {
  background: #e6f2fb;
}

.page-info {
  color: #666;
  margin-left: auto;
  margin-right: 8px;
  font-size: 13px;
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

.list-content {
  flex: 1;
  overflow-y: auto;
}

.mail-item {
  border: 1px solid #e6f2fb;
  margin-bottom: 12px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.03);
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  transition: background-color 0.2s;
  cursor: pointer;
}

.mail-item:hover {
  background-color: #f5f7fa;
}

.mail-item:last-child {
  margin-bottom: 0;
}

.empty-message {
  text-align: center;
  padding: 40px;
  color: #999;
}

.mail-content {
  flex-grow: 1;
  display: flex;
  align-items: center;
  gap: 20px;
  width: 100%;
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
  min-width: 120px;
  text-align: right;
  color: #999;
  font-size: 0.85em;
}

.checkbox-container {
  min-width: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.header-checkbox,
.item-checkbox {
  margin: 0;
  width: 16px;
  height: 16px;
}

.unread {
  background-color: #f8f9fa;
  font-weight: 500;
}

.unread .subject {
  font-weight: bold;
}
</style>