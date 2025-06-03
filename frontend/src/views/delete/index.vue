<template>
  <div class="main-page">
    <div class="top-bar">
      <img src="@/assets/logo.jpg" alt="华南理工大学" class="logo" />
      <input type="text" placeholder="邮箱搜索" class="search-input" />
      <button class="account-btn">账号中心</button>
    </div>

    <div class="left-nav">
      <button class="nav-btn" @click="goToWrite">写信</button>
      <button class="nav-btn" @click="handleReceive">收信</button>
      <div class="nav-group">
        <button class="nav-sub-btn" @click="goToMain">收件箱</button>
        <button class="nav-sub-btn">草稿箱</button>
        <button class="nav-sub-btn" @click="goToStarred">星标邮件</button>
        <button 
          class="nav-sub-btn" 
          :class="{ active: $route.name === 'draft' }" 
          @click="goToDraft"
        >已发送</button>
        <button class="nav-sub-btn active">已删除</button>
      </div>
    </div>
    <Content />
  </div>
</template>

<script>
import Content from './content.vue';
export default {
  name: 'DeletePage',
  components: {
    Content
  },
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
    goToWrite() {
      this.$router.push('/write');
    },
    goToMain() {
      this.$router.push('/main');
    },
    goToDraft() {
      // 保持当前页面
    },
    goToStarred() {
      this.$router.push('/star');
    }
  }
};
</script>

<style scoped>
.main-page {
  display: flex;
  flex-direction: row;
  height: 99vh;
  width: 97vw;
  background: #e6f2fb;
  overflow: hidden;
}

.top-bar {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 80px;
  background: #fff;
  border-bottom: 2px solid #cce2fa;
  display: flex;
  align-items: center;
  z-index: 10;
  padding: 0 48px;
}

.logo {
  height: 60px;
  margin-right: 32px;
}

.search-input {
  flex-grow: 1;
  padding: 14px;
  border: 1px solid #cce2fa;
  border-radius: 4px;
  margin-right: 32px;
  background: #f8faff;
  font-size: 18px;
}

.account-btn {
  padding: 14px 28px;
  background: #f5f7fa;
  color: #1f74c0;
  border: 1px solid #cce2fa;
  border-radius: 4px;
  font-weight: bold;
  font-size: 18px;
}

.left-nav {
  margin-top: 80px;
  width: 165px;
  background: #e6f2fb;
  border-right: 2px solid #cce2fa;
  height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
  padding: 20px 0;
}

.nav-btn {
  width: 100%;
  padding: 10px 0;
  text-align: center;
  border: none;
  background: #fff;
  font-size: 15px;
  cursor: pointer;
  margin-bottom: 8px;
  border-radius: 8px;
  font-weight: bold;
  color: #1f74c0;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}

.nav-group {
  margin-top: 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
  padding: 8px 0;
}

.nav-sub-btn {
  width: 100%;
  padding: 8px 0;
  text-align: center;
  border: none;
  background: none;
  color: #1f74c0;
  cursor: pointer;
  margin-bottom: 3px;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-sub-btn.active {
  font-weight: bold;
  background: #e6f2fb;
  border-left: 4px solid #1f74c0;
}

.mail-list {
  margin-top: 80px;
  flex: 1;
  background: #fff;
  border-radius: 0 0 20px 20px;
  margin-left: 0;
  padding: 20px 28px 28px 28px;
  height: calc(100vh - 80px);
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
</style>