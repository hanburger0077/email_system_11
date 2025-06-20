<template>
  <div class="mail-list">
    <div class="mail-toolbar">
      <div class="toolbar-left">
        <el-checkbox 
          v-model="allSelected" 
          @change="toggleSelectAll" 
          class="select-all-checkbox" 
        />

        <el-button  
          :disabled="selectedMails.length === 0"
          @click="deleteSelected"
          class="delete-button"
        >
          删除
        </el-button>

        <el-tooltip 
          content="标为已读"
          placement="bottom"
        >
          <el-button 
            @click="markAsRead"
            :disabled="selectedMails.length === 0"
            class="mark-button"
          >
            <img 
              :src="mark1Icon" 
              class="mark-icon"
              alt="标为已读"
            />
          </el-button>
        </el-tooltip>

        <el-tooltip 
          content="标为未读"
          placement="bottom"
        >
          <el-button 
            @click="markAsUnread"
            :disabled="selectedMails.length === 0"
            class="mark-button"
          >
            <img 
              :src="mark2Icon" 
              class="mark-icon"
              alt="标为未读"
            />
          </el-button>
        </el-tooltip>

        <el-dropdown @command="handleMarkCommand" :disabled="selectedMails.length === 0">
          <el-button 
            class="mark-more-button" 
            :disabled="selectedMails.length === 0"
          >
            更多标记 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="star" class="dropdown-item">
                <i class="star-icon starred">★</i>
                <span>星标邮件</span>
              </el-dropdown-item>
              <el-dropdown-item command="unstar" class="dropdown-item">
                <i class="star-icon">☆</i>
                <span>取消星标</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <el-tooltip content="刷新" placement="bottom">
          <el-button 
            class="refresh-button" 
            @click="handleReceive"
          >
            <img :src="mark3Icon" class="refresh-icon" alt="刷新" />
          </el-button>
        </el-tooltip>
        
        <el-tooltip content="全部删除" placement="bottom">
          <el-button 
            class="delete-all-button" 
            @click="deleteAll"
          >
            全部删除
          </el-button>
        </el-tooltip>
      </div>

      <div class="toolbar-right">
        <span class="mail-count">{{ mailList.length }} 封邮件</span>
        <span class="page-info">{{ currentPage }}/{{ totalPages }}页</span>
        <div class="pagination-controls">
          <el-button 
            size="small" 
            :disabled="currentPage === 1"
            @click="prevPage"
          >
            上一页
          </el-button>
          <el-button 
            size="small" 
            :disabled="currentPage === totalPages"
            @click="nextPage"
          >
            下一页
          </el-button>
        </div>
      </div>
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
              <el-checkbox v-model="selectedMails" :value="mail.globalIndex" class="item-checkbox" />
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
import { ArrowDown } from '@element-plus/icons-vue'
import mark1Icon from './assets/mark1.png'
import mark2Icon from './assets/mark2.png'
import mark3Icon from './assets/mark3.png'
import mark4Icon from './assets/mark4.png'

export default {
  name: 'MainPage',
  components: {
    ArrowDown
  },
  data() {
    return {
      mark1Icon,
      mark2Icon,
      mark3Icon,
      mark4Icon,
      currentPage: 1,
      itemsPerPage: 32,
      selectedMails: [],
      allSelected: false,
      showMarkDropdown: false,
      mailList: [
        { 
          id: 1,
          sender: "admin@scut.edu.cn", 
          subject: "校园通知", 
          time: "2025-6-7 9:30", 
          isRead: true, 
          isStarred: false,
          content: "亲爱的同学们：\n\n请注意，下周一（6月10日）起，图书馆将调整开放时间为早上8点至晚上10点。同时，请大家记得在离开图书馆时归还所借书籍，保持阅读环境的整洁。\n\n此外，学校将于6月15日举行校园开放日活动，欢迎邀请家人朋友参观我们美丽的校园。\n\n祝学习愉快！\n\n校园管理处"
        },
        { 
          id: 2,
          sender: "teacher@scut.edu.cn", 
          subject: "作业提醒", 
          time: "2025-6-6 09:00", 
          isRead: false, 
          isStarred: false,
          content: "各位同学：\n\n请注意，软件工程课程的期末项目报告将于下周五（6月14日）截止提交。请确保你们的项目符合之前发布的要求清单，并按时提交到教学系统。\n\n如有任何疑问，请在办公时间前来咨询或发送邮件。\n\n祝学习进步！\n\n李教授"
        },
        { 
          id: 3,
          sender: "friend@example.com", 
          subject: "周末聚会", 
          time: "2025-6-5 18:45", 
          isRead: true, 
          isStarred: false,
          content: "嘿！\n\n这周末我们打算组织一次聚会，地点定在学校附近的「青橙咖啡」，时间是周六下午3点。已经有好几个同学确认参加了，你有空一起来吗？\n\n如果有什么特别想吃的，可以提前告诉我，我们可以预订。\n\n期待你的回复！\n\n小明"
        },
        { 
          id: 4,
          sender: "new-student@scut.edu.cn", 
          subject: "新生指南", 
          time: "2025-6-4 09:00", 
          isRead: false, 
          isStarred: false,
          content: "亲爱的新同学：\n\n欢迎加入华南理工大学大家庭！为了帮助你更好地适应大学生活，我们准备了一份详细的新生指南。请通过以下链接访问：campus.scut.edu.cn/guide\n\n如果你有任何问题，可以随时联系你的班主任或学院辅导员。\n\n期待在校园里见到你！\n\n招生办公室"
        },
        { 
          id: 5,
          sender: "dean@scut.edu.cn", 
          subject: "教学安排", 
          time: "2025-6-3 15:30", 
          isRead: true, 
          isStarred: false,
          content: "全体师生：\n\n关于下学期的教学安排已经确定，详细课表将于下周一发布在教务系统。请各位老师提前做好教学准备工作，学生可以在系统开放后进行选课。\n\n另外，本学期的教学评估将于6月20日开始，请大家积极参与，提供宝贵意见。\n\n教务处"
        },
        { 
          id: 6,
          sender: "hr@company.com", 
          subject: "面试邀请", 
          time: "2025-6-2 11:15", 
          isRead: false, 
          isStarred: false,
          content: "尊敬的申请人：\n\n感谢您申请我公司的软件工程师职位。我们对您的简历和作品集印象深刻，希望邀请您参加面试。\n\n面试定于6月5日下午2点，地点在我公司总部（广州市天河区科技园B栋12楼）。请带上您的身份证件和作品集。\n\n如果您对时间有任何问题，请回复此邮件调整。\n\n期待与您的会面！\n\n人力资源部 张经理"
        },
        { 
          id: 7,
          sender: "system@mail.com", 
          subject: "系统升级", 
          time: "2025-6-1 08:00", 
          isRead: true, 
          isStarred: false,
          content: "尊敬的用户：\n\n我们将于本周六凌晨2点至6点进行系统升级维护，在此期间系统服务将暂时不可用。升级后，您将体验到更快的响应速度和更多的新功能。\n\n感谢您的理解与支持！\n\n系统管理员"
        },
        { 
          id: 8,
          sender: "alumni@scut.edu.cn", 
          subject: "校友活动", 
          time: "2025-5-25 15:30", 
          isRead: true, 
          isStarred: false,
          content: "亲爱的校友：\n\n华南理工大学将于7月15日举办2025年校友返校日活动。活动包括校园参观、学术讲座、联谊午宴等丰富内容。\n\n如果您计划参加，请在7月5日前通过校友网站登记：alumni.scut.edu.cn\n\n期待与您在母校重逢！\n\n校友会"
        },
        { 
          id: 9,
          sender: "library@scut.edu.cn", 
          subject: "图书到期", 
          time: "2025-5-25 09:45", 
          isRead: false, 
          isStarred: false,
          content: "尊敬的读者：\n\n您借阅的《数据结构与算法》将于3天后(6月3日)到期，请及时归还或在线续借。如已归还，请忽略此提醒。\n\n图书馆开放时间：周一至周五 8:00-22:00，周末 9:00-21:00\n\n感谢您的配合！\n\n图书馆管理员"
        },
        { 
          id: 10,
          sender: "tech-support@scut.edu.cn", 
          subject: "网络维护", 
          time: "2025-5-24 14:00", 
          isRead: true, 
          isStarred: false,
          content: "全校师生：\n\n为提升校园网络服务质量，信息中心将于本周日(6月2日)上午9:00-12:00对校园网进行维护升级。期间可能出现网络不稳定或短暂中断的情况。\n\n如有重要工作需依赖网络，请提前做好准备。\n\n感谢您的理解与支持！\n\n信息技术中心"
        },
        { 
          id: 11,
          sender: "shopping@mall.com", 
          subject: "促销信息", 
          time: "2023-10-10 16:20", 
          isRead: true, 
          isStarred: false,
          content: "尊敬的会员：\n\n感谢您一直以来对我们的支持！我们商城将于本周末进行年中大促，全场商品低至5折，还有额外的会员专属优惠券可以领取。\n\n活动时间：6月8日-6月10日\n活动网址：www.shoppingmall.com/sale\n\n先到先得，欢迎选购！\n\n购物商城团队"
        },
        { 
          id: 12,
          sender: "news@scut.edu.cn", 
          subject: "学术讲座", 
          time: "2023-10-10 14:00", 
          isRead: false, 
          isStarred: false,
          content: "各位师生：\n\n本周四(6月6日)下午3点，著名计算机科学家李明教授将在我校大礼堂举办题为《人工智能的未来发展》的学术讲座。李教授是人工智能领域的国际知名学者，此次讲座将分享他对AI领域最新研究成果和未来趋势的见解。\n\n欢迎全校师生参加，座位有限，请提前到场。\n\n学术委员会"
        },
        
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
      this.$message.success('刷新成功，已获取最新邮件');
    },
    deleteSelected() {
      if (this.selectedMails.length === 0) return;
      
      this.$confirm('确认删除选中的邮件吗？', '提示', {
        type: 'warning'
      }).then(() => {
        this.mailList = this.mailList.filter((_, index) => !this.selectedMails.includes(index));
        this.selectedMails = [];
        this.allSelected = false;
        this.groupAndIndexMails();
        this.$message.success('删除成功');
      }).catch(() => {
        // 取消删除操作
      });
    },
    deleteAll() {
      this.$confirm('确认删除所有邮件吗？', '警告', {
        type: 'warning',
        confirmButtonText: '确定删除',
        confirmButtonClass: 'el-button--danger'
      }).then(() => {
        this.mailList = [];
        this.selectedMails = [];
        this.allSelected = false;
        this.currentPage = 1;
        this.groupAndIndexMails();
        this.$message.success('已删除所有邮件');
      }).catch(() => {
        // 取消删除操作
      });
    },
    loadSavedMailsData() {
      // 从sessionStorage加载可能被修改过的邮件数据
      const storedMails = sessionStorage.getItem('allMails');
      if (storedMails) {
        try {
          const parsedMails = JSON.parse(storedMails);
          // 更新本地邮件列表的星标状态
          parsedMails.forEach(storedMail => {
            const localMail = this.mailList.find(m => m.id === storedMail.id);
            if (localMail) {
              localMail.isStarred = storedMail.isStarred;
            }
          });
          this.groupAndIndexMails();
        } catch (e) {
          console.error('解析存储的邮件数据失败:', e);
        }
      }
    },
    openMail(mail) {
      // 标记邮件为已读
      if (!mail.isRead) {
        const realMail = this.mailList.find((m, idx) => idx === mail.globalIndex);
        if (realMail) realMail.isRead = true;
      }
      
      // 存储当前所有邮件数据到sessionStorage，以便在详情页面可以访问和修改
      sessionStorage.setItem('allMails', JSON.stringify(this.mailList));
      
      // 使用sessionStorage存储当前邮件数据
      sessionStorage.setItem('currentMail', JSON.stringify(mail));
      
      // 跳转到邮件详情页面
      this.$router.push({
        path: '/mail-detail',
        query: { 
          id: mail.id || mail.globalIndex,
          from: 'inbox'
        }
      });
    },
    toggleSelectAll() {
      const indexes = this.paginatedMails.map(mail => mail.globalIndex);
      this.selectedMails = this.allSelected ? indexes : [];
    },
    toggleStar(mail) {
      const realMail = this.mailList.find((m, idx) => idx === mail.globalIndex);
      if (realMail) {
        realMail.isStarred = !realMail.isStarred;
        
        // 更新sessionStorage中的所有邮件数据
        sessionStorage.setItem('allMails', JSON.stringify(this.mailList));
      }
      this.groupAndIndexMails();
    },
    markAsRead() {
      if (this.selectedMails.length === 0) return;
      
      // 检查是否所有选中邮件都已经是已读状态
      const allAlreadyRead = this.selectedMails.every(index => {
        return this.mailList[index] && this.mailList[index].isRead;
      });
      
      if (allAlreadyRead) {
        this.$message({
          message: '所选邮件已是已读状态',
          type: 'info'
        });
        return;
      }
      
      this.selectedMails.forEach(index => {
        if (this.mailList[index]) {
          this.mailList[index].isRead = true;
        }
      });
      
      sessionStorage.setItem('allMails', JSON.stringify(this.mailList));
      this.groupAndIndexMails();
      this.$message.success('已标记为已读');
    },
    markAsUnread() {
      if (this.selectedMails.length === 0) return;
      
      // 检查是否所有选中邮件都已经是未读状态
      const allAlreadyUnread = this.selectedMails.every(index => {
        return this.mailList[index] && !this.mailList[index].isRead;
      });
      
      if (allAlreadyUnread) {
        this.$message({
          message: '所选邮件已是未读状态',
          type: 'info'
        });
        return;
      }
      
      this.selectedMails.forEach(index => {
        if (this.mailList[index]) {
          this.mailList[index].isRead = false;
        }
      });
      
      sessionStorage.setItem('allMails', JSON.stringify(this.mailList));
      this.groupAndIndexMails();
      this.$message.success('已标记为未读');
    },
    handleMarkCommand(command) {
      if (this.selectedMails.length === 0) return;
      
      if (command === 'star') {
        this.markSelectedAsStarred();
      } else if (command === 'unstar') {
        this.unmarkSelectedAsStarred();
      }
    },
    markSelectedAsStarred() {
      if (this.selectedMails.length === 0) return;
      
      this.selectedMails.forEach(index => {
        if (this.mailList[index]) this.mailList[index].isStarred = true;
      });
      
      // 更新sessionStorage中的所有邮件数据
      sessionStorage.setItem('allMails', JSON.stringify(this.mailList));
      
      this.groupAndIndexMails();
      this.$message.success('已添加星标');
    },
    unmarkSelectedAsStarred() {
      if (this.selectedMails.length === 0) return;
      
      this.selectedMails.forEach(index => {
        if (this.mailList[index]) this.mailList[index].isStarred = false;
      });
      
      // 更新sessionStorage中的所有邮件数据
      sessionStorage.setItem('allMails', JSON.stringify(this.mailList));
      
      this.groupAndIndexMails();
      this.$message.success('已取消星标');
    }
  },
  watch: {
    currentPage() {
      this.groupAndIndexMails();
    }
  },
  mounted() {
    this.loadSavedMailsData(); // 加载可能在详情页面更新过的邮件数据
    this.groupAndIndexMails();
  },
  activated() {
    // 每次页面被重新激活时，重新加载可能更新的邮件数据
    this.loadSavedMailsData();
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
  background: #f8faff;
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

/* 删除按钮样式 */
/* 删除按钮样式 */
.delete-button {
  font-size: 14px;
  border: 1px solid #dcdfe6;  
  background: #fff;         
  color: #606266;         
  border-radius: 4px;
  cursor: pointer;
  height: 32px;
  padding: 0 12px;
  line-height: 30px;
}

.delete-button:hover {
  background: #f5f7fa;        
  border-color: #c6e2ff;      
  color: #409eff;            
}

.delete-button:active {
  background: #f5f7fa;        /* 改为普通点击背景 */
  border-color: #3a8ee6;      /* 改为普通点击边框 */
}

.delete-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #f5f7fa;
}

/* 更多标记按钮样式 */
.mark-more-button {
  font-size: 14px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  border-radius: 4px;
  cursor: pointer;
  height: 32px;
  padding: 0 12px;
  line-height: 30px;
  display: flex;
  align-items: center;
}

.mark-more-button:hover {
  background: #f5f7fa;
  color: #409eff;
}

.mark-more-button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  background: #f5f7fa;
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

.mark-button {
  padding: 0;
  margin: 0;
  width: 32px;
  height: 32px;
  min-width: 32px;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dcdfe6;
  background: #fff;
}

.mark-button:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.mark-button:disabled .mark-icon {
  filter: grayscale(100%);
}

.mark-icon {
  width: 20px;
  height: 20px;
  object-fit: cover;
}

.dropdown-item {
  display: flex;
  align-items: center;
  width: 100%;
  text-align: left;
  cursor: pointer;
  transition: background-color 0.2s;
  font-size: 14px;
  padding: 8px 12px;
}

.dropdown-item span {
  margin-left: 8px;
  flex-grow: 1;
}

.refresh-button {
  padding: 0;
  margin: 0;
  width: 32px;
  height: 32px;
  min-width: 32px;
  border-radius: 4px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #dcdfe6;
  background: #fff;
}

.refresh-icon {
  width: 20px;
  height: 20px;
  object-fit: cover;
  display: block;
}

.star-icon {
  margin-right: 8px;
  color: #ccc;
}

.star-icon.starred {
  color: #ffc107;
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

.mail-header .star-col {
  min-width: 40px;
  text-align: center;
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

  .sender {
    min-width: 140px;
  }

  .time {
    min-width: 100px;
  }
}
</style>