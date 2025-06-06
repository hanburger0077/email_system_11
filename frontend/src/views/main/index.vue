<template>
  <div class="mail-list">
    <div class="list-header">
      <input type="checkbox" v-model="allSelected" @change="toggleSelectAll" class="header-checkbox" />
      <button class="list-btn" @click="deleteSelected">删除选中</button>
      <div class="mark-dropdown">
        <button class="list-btn" @click="showMarkDropdown = !showMarkDropdown">标记为...</button>
        <div v-show="showMarkDropdown" class="dropdown-content" @click.self="showMarkDropdown = false">
          <button @click="markSelectedAsStarred">星标邮件</button>
          <button @click="unmarkSelectedAsStarred">取消星标</button>
        </div>
      </div>
      <button class="list-btn" @click="deleteAll">全部删除</button>
      <span class="page-info">{{ currentPage }}/{{ totalPages }}页</span>
      <button class="list-btn" @click="prevPage" :disabled="currentPage === 1">上一页</button>
      <button class="list-btn" @click="nextPage" :disabled="currentPage === totalPages">下一页</button>
    </div>

    <div class="mail-header">
      <span class="column checkbox-col"></span>
      <span class="column sender">发件人</span>
      <span class="column subject">主题</span>
      <span class="column time">时间</span>
      <span class="column star-col">星标</span>
    </div>

    <div class="list-content">
      <div v-for="(group, index) in groupedMails" :key="index" class="mail-group">
        <h3 class="group-title" @click="toggleExpand(index)">
          {{ group.title }}
          <span class="expand-icon">{{ group.isExpanded ? '−' : '+' }}</span>
        </h3>

        <div v-show="group.isExpanded" class="mail-items">
          <div 
            v-for="(mail, mailIndex) in group.mails" 
            :key="mail.globalIndex" 
            class="mail-item" 
            :class="{ 'unread': !mail.isRead }"
          >
            <div class="checkbox-container">
              <input type="checkbox" v-model="selectedMails" :value="mail.globalIndex" class="item-checkbox" />
            </div>
            <div class="mail-content" @click="openMail(mail)">
              <span class="column sender">{{ mail.sender }}</span>
              <span class="column subject">{{ mail.subject }}</span>
              <span class="column time">{{ mail.time }}</span>
              <span 
                class="star-icon" 
                :class="{ 'star-filled': mail.isStarred }" 
                @click.stop="toggleStar(mail)"
              >&#9734;</span>
            </div>
          </div>

          <div v-if="group.mails.length === 0 && group.isExpanded" class="empty-message">
            该分类暂无邮件
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'MainPage',
  data() {
    return {
      currentPage: 1,
      itemsPerPage: 32,
      selectedMails: [],
      allSelected: false,
      showMarkDropdown: false,
      mailList: [
        { sender: "admin@scut.edu.cn", subject: "校园通知", time: "2025-5-30 14:30", isRead: true, isStarred: false },
        { sender: "teacher@scut.edu.cn", subject: "作业提醒", time: "2025-5-30 09:00", isRead: false, isStarred: false },
        { sender: "friend@example.com", subject: "周末聚会", time: "2025-5-29 18:45", isRead: true, isStarred: false },
        { sender: "new-student@scut.edu.cn", subject: "新生指南", time: "2025-5-29 09:00", isRead: false, isStarred: false },
        { sender: "dean@scut.edu.cn", subject: "教学安排", time: "2025-5-28 15:30", isRead: true, isStarred: false },
        { sender: "hr@company.com", subject: "面试邀请", time: "2025-5-26 11:15", isRead: false, isStarred: false },
        { sender: "system@mail.com", subject: "系统升级", time: "2025-5-26 08:00", isRead: true, isStarred: false },
        { sender: "alumni@scut.edu.cn", subject: "校友活动", time: "2025-5-25 15:30", isRead: true, isStarred: false },
        { sender: "library@scut.edu.cn", subject: "图书到期", time: "2025-5-25 09:45", isRead: false, isStarred: false },
        { sender: "tech-support@scut.edu.cn", subject: "网络维护", time: "2025-5-24 14:00", isRead: true, isStarred: false },
        { sender: "shopping@mall.com", subject: "促销信息", time: "2023-10-10 16:20", isRead: true, isStarred: false },
        { sender: "news@scut.edu.cn", subject: "学术讲座", time: "2023-10-10 14:00", isRead: false, isStarred: false },
        { sender: "bank@example.com", subject: "账户通知", time: "2023-10-05 08:30", isRead: true, isStarred: false },
      ],
      groupedMails: [
        { title: "最近三天", isExpanded: true, mails: [] },
        { title: "最近一周", isExpanded: true, mails: [] },
        { title: "更早", isExpanded: true, mails: [] },
      ]
    };
  },
  computed: {
    totalPages() {
      return Math.ceil(this.mailList.length / this.itemsPerPage);
    },
    paginatedMails() {
      const start = (this.currentPage - 1) * this.itemsPerPage;
      const end = start + this.itemsPerPage;
      return this.mailList.slice(start, end).map((mail, i) => ({
        ...mail,
        globalIndex: start + i
      }));
    }
  },
  methods: {
    groupAndIndexMails() {
      const today = new Date().setHours(0, 0, 0, 0);
      const groups = {
        "最近三天": [],
        "最近一周": [],
        "更早": []
      };

      this.paginatedMails.forEach(mail => {
        const mailTime = new Date(mail.time).setHours(0, 0, 0, 0);
        if (mailTime >= today - 2 * 86400000) {
          groups["最近三天"].push(mail);
        } else if (mailTime >= today - 6 * 86400000) {
          groups["最近一周"].push(mail);
        } else {
          groups["更早"].push(mail);
        }
      });

      this.groupedMails.forEach(group => {
        group.mails = groups[group.title];
      });
    },
    toggleExpand(index) {
      this.groupedMails[index].isExpanded = !this.groupedMails[index].isExpanded;
    },
    prevPage() {
      if (this.currentPage > 1) this.currentPage--;
    },
    nextPage() {
      if (this.currentPage < this.totalPages) this.currentPage++;
    },
    handleReceive() {
      this.currentPage = 1;
      alert('刷新成功，已获取最新邮件');
    },
    deleteSelected() {
      if (this.selectedMails.length === 0) return;
      if (confirm('确认删除选中的邮件吗？')) {
        this.mailList = this.mailList.filter((_, index) => !this.selectedMails.includes(index));
        this.selectedMails = [];
        this.allSelected = false;
        this.groupAndIndexMails();
      }
    },
    deleteAll() {
      if (confirm('确认删除所有邮件吗？')) {
        this.mailList = [];
        this.selectedMails = [];
        this.allSelected = false;
        this.currentPage = 1;
        this.groupAndIndexMails();
      }
    },
    openMail(mail) {
      // 标记邮件为已读
      if (!mail.isRead) {
        const realMail = this.mailList.find((m, idx) => idx === mail.globalIndex);
        if (realMail) realMail.isRead = true;
      }
      // 这里可以添加邮件详情页导航
      console.log("打开邮件:", mail);
    },
    toggleSelectAll() {
      const indexes = this.paginatedMails.map(mail => mail.globalIndex);
      this.selectedMails = this.allSelected ? indexes : [];
    },
    toggleStar(mail) {
      const realMail = this.mailList.find((m, idx) => idx === mail.globalIndex);
      if (realMail) realMail.isStarred = !realMail.isStarred;
      this.groupAndIndexMails();
    },
    markSelectedAsStarred() {
      this.selectedMails.forEach(index => {
        if (this.mailList[index]) this.mailList[index].isStarred = true;
      });
      this.groupAndIndexMails();
      this.showMarkDropdown = false;
    },
    unmarkSelectedAsStarred() {
      this.selectedMails.forEach(index => {
        if (this.mailList[index]) this.mailList[index].isStarred = false;
      });
      this.groupAndIndexMails();
      this.showMarkDropdown = false;
    }
  },
  watch: {
    currentPage() {
      this.groupAndIndexMails();
    }
  },
  mounted() {
    this.groupAndIndexMails();
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

.mark-dropdown {
  position: relative;
  display: inline-block;
  margin-right: 10px;
}

.dropdown-content {
  position: absolute;
  background-color: #fff;
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
  border-radius: 4px;
  z-index: 10;
  min-width: 120px;
  right: 0;
  top: 100%;
  margin-top: 4px;
}

.dropdown-content button {
  display: block;
  width: 100%;
  padding: 8px 12px;
  text-align: left;
  border: none;
  background: none;
  cursor: pointer;
  transition: background-color 0.2s;
}

.dropdown-content button:hover {
  background-color: #f5f7fa;
}

.page-info {
  color: #666;
  margin-left: auto;
  margin-right: 8px;
  font-size: 13px;
}

.mail-group {
  margin-bottom: 24px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.03);
  padding: 16px;
}

.group-title {
  font-weight: bold;
  color: #1f74c0;
  margin: 0 0 16px 0;
  font-size: 17px;
  border-bottom: 2px solid #cce2fa;
  padding-bottom: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}

.expand-icon {
  font-size: 1.2em;
  color: #666;
}

.mail-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.mail-item:hover {
  background-color: #f5f7fa;
}

.unread {
  background-color: #f8f9fa;
  font-weight: 500;
}

.unread .subject {
  font-weight: bold;
}

.checkbox-container {
  min-width: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.mail-content {
  flex-grow: 1;
  display: flex;
  align-items: center;
  gap: 20px;
  width: 100%;
  cursor: pointer;
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

.star-icon {
  font-size: 1.2em;
  margin-left: 8px;
  cursor: pointer;
  color: #999;
}

.star-filled {
  content: "\9733";
  color: #ffc107;
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

.mail-header .star-col {
  min-width: 40px;
  text-align: center;
}

.empty-message {
  text-align: center;
  padding: 30px;
  color: #999;
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
</style>