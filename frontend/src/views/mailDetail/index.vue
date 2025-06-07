<template>
  <div class="mail-detail">
    <div class="mail-actions">
      <button class="action-btn" @click="goBack">
        <i class="action-icon">←</i> 返回
      </button>
      <button class="action-btn" @click="deleteMail">
        <i class="action-icon">🗑️</i> 删除
      </button>
      <button class="action-btn" @click="toggleStar">
        <i class="action-icon" :class="{ 'star-active': mail.isStarred }">
          {{ mail.isStarred ? '★' : '☆' }}
        </i>
        {{ mail.isStarred ? '取消星标' : '星标' }}
      </button>
      <button class="action-btn" @click="replyMail">
        <i class="action-icon">↩️</i> 回复
      </button>
    </div>

    <div class="mail-header-info">
      <h1 class="mail-subject">{{ mail.subject }}</h1>
      <div class="mail-meta">
        <div class="sender-info">
          <span class="label">发件人：</span>
          <span class="value">{{ mail.sender }}</span>
        </div>
        <div class="receiver-info">
          <span class="label">收件人：</span>
          <span class="value">{{ userEmail }}</span>
        </div>
        <div class="time-info">
          <span class="label">时间：</span>
          <span class="value">{{ mail.time }}</span>
        </div>
      </div>
    </div>

    <div class="mail-content">
      <pre>{{ mail.content }}</pre>
    </div>

    <div v-if="mail.attachments && mail.attachments.length > 0" class="attachments-section">
      <h3 class="attachments-title">附件</h3>
      <div class="attachments-list">
        <div v-for="(attachment, index) in mail.attachments" :key="index" class="attachment-item">
          <i class="attachment-icon">📎</i>
          <span class="attachment-name">{{ attachment.name }}</span>
          <span class="attachment-size">({{ formatFileSize(attachment.size) }})</span>
          <button class="attachment-download">下载</button>
        </div>
      </div>
    </div>

    <!-- 删除确认弹窗 -->
    <div v-if="showDeleteModal" class="modal-overlay" @click.self="showDeleteModal = false">
      <div class="modal-content">
        <h3 class="modal-title">确认删除</h3>
        <p class="modal-message">您确定要删除这封邮件吗？</p>
        <div class="modal-buttons">
          <button class="modal-cancel-btn" @click="showDeleteModal = false">取消</button>
          <button class="modal-confirm-btn" @click="confirmDelete">确认</button>
        </div>
      </div>
    </div>

    <!-- Toast提示 -->
    <div v-if="showToast" class="toast-message" :class="toastType">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      mailId: null,
      fromPage: '',
      userEmail: 'user@scut.edu.cn', // 用户邮箱，实际应该从用户信息或登录状态获取
      showToast: false,
      toastMessage: '',
      toastType: 'success',
      showDeleteModal: false,
      mail: {
        id: null,
        sender: '',
        subject: '',
        time: '',
        isRead: false,
        isStarred: false,
        content: '',
        attachments: []
      },
      // 添加一个存储原始邮件数据的数组
      allMails: []
    };
  },
  created() {
    // 从路由参数获取邮件ID
    this.mailId = this.$route.query.id;
    this.fromPage = this.$route.query.from || 'inbox';
    
    // 尝试从sessionStorage获取所有邮件数据
    const storedAllMails = sessionStorage.getItem('allMails');
    if (storedAllMails) {
      try {
        this.allMails = JSON.parse(storedAllMails);
      } catch (e) {
        console.error('解析所有邮件数据失败:', e);
      }
    }
    
    this.fetchMailData();
  },
  methods: {
    fetchMailData() {
      // 1. 首先尝试从 sessionStorage 获取当前选中的邮件数据
      const storedMail = sessionStorage.getItem('currentMail');
      if (storedMail) {
        try {
          this.mail = JSON.parse(storedMail);
          return; // 如果成功获取并解析，直接返回
        } catch (e) {
          console.error('解析存储的邮件数据失败:', e);
          // 解析失败会继续执行下面的逻辑
        }
      }
      
      // 2. 如果没有找到存储的邮件数据，使用完整的模拟数据
      const mockMailsData = [
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
        }
      ];

      // 如果sessionStorage中没有存储所有邮件，则使用模拟数据
      if (this.allMails.length === 0) {
        this.allMails = [...mockMailsData];
        // 存储到sessionStorage以便后续使用
        sessionStorage.setItem('allMails', JSON.stringify(this.allMails));
      }

      // 3. 查找指定ID的邮件
      const foundMail = this.allMails.find(mail => mail.id == this.mailId);
      if (foundMail) {
        this.mail = { ...foundMail };
      } else {
        // 4. 如果没有找到，显示错误提示
        this.showToastMessage('邮件不存在或已被删除', 'error');
        setTimeout(() => {
          this.goBack();
        }, 2000);
      }
    },
    goBack() {
      // 返回上一页
      if (this.fromPage === 'inbox') {
        this.$router.push('/main');
      } else {
        this.$router.push('/'); // 默认返回首页
      }
    },
    deleteMail() {
      this.showDeleteModal = true;
    },
    confirmDelete() {
      // 实际应用中，这里应该调用API删除邮件
      
      // 从所有邮件中删除当前邮件
      const mailIndex = this.allMails.findIndex(m => m.id === this.mail.id);
      if (mailIndex !== -1) {
        this.allMails.splice(mailIndex, 1);
        // 更新sessionStorage中的所有邮件数据
        sessionStorage.setItem('allMails', JSON.stringify(this.allMails));
      }
      
      this.showDeleteModal = false;
      this.showToastMessage('邮件已删除', 'success');
      setTimeout(() => {
        this.goBack();
      }, 1500);
    },
    toggleStar() {
      // 切换星标状态
      this.mail.isStarred = !this.mail.isStarred;
      
      // 同步更新所有邮件中该邮件的星标状态
      const mailToUpdate = this.allMails.find(m => m.id === this.mail.id);
      if (mailToUpdate) {
        mailToUpdate.isStarred = this.mail.isStarred;
        // 更新sessionStorage中的所有邮件数据
        sessionStorage.setItem('allMails', JSON.stringify(this.allMails));
      }
      
      // 更新当前邮件的会话存储，确保返回时显示正确的星标状态
      sessionStorage.setItem('currentMail', JSON.stringify(this.mail));
      
      // 实际应用中，这里应该调用API更新邮件星标状态
      this.showToastMessage(
        this.mail.isStarred ? '已添加星标' : '已取消星标', 
        'success'
      );
    },
    replyMail() {
      // 跳转到写邮件页面，并预填写回复信息
      this.$router.push({
        path: '/edit',  // 确保这个路径与menuList中"写信"菜单项的link一致
        query: {
          reply: true,
          to: this.mail.sender,
          // subject字段根据前面的需求已经移除，回复时主题栏保持为空
          from: 'reply' // 添加一个标记，表明是从回复功能跳转过来的
        }
      });
    },
    formatFileSize(bytes) {
      if (!bytes) return '未知';
      if (bytes < 1024) return bytes + ' B';
      else if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
      else return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
    },
    showToastMessage(message, type = 'success') {
      this.toastMessage = message;
      this.toastType = type;
      this.showToast = true;
      setTimeout(() => {
        this.showToast = false;
      }, 3000);
    }
  }
};
</script>

<style scoped>
.mail-detail {
  padding: 24px 32px;
  height: 100%;
  overflow-y: auto;
}

.mail-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eee;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #cce2fa;
  background: #f5f7fa;
  color: #1f74c0;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.action-btn:hover {
  background: #e6f2fb;
}

.action-icon {
  font-style: normal;
  font-size: 16px;
}

.star-active {
  color: #ffc107;
}

.mail-header-info {
  padding: 20px 0;
  border-bottom: 1px solid #eee;
  margin-bottom: 24px;
}

.mail-subject {
  font-size: 22px;
  margin-bottom: 16px;
  color: #333;
  font-weight: bold;
}

.mail-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sender-info,
.receiver-info,
.time-info {
  display: flex;
  align-items: flex-start;
}

.label {
  min-width: 60px;
  color: #666;
  font-weight: bold;
}

.value {
  color: #333;
}

.mail-content {
  padding: 24px 0;
  line-height: 1.6;
  color: #333;
  font-size: 16px;
}

.mail-content pre {
  font-family: Arial, sans-serif;
  white-space: pre-wrap;
  margin: 0;
}

.attachments-section {
  margin-top: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
}

.attachments-title {
  font-size: 16px;
  margin-bottom: 12px;
  color: #333;
}

.attachments-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  background: white;
  border-radius: 4px;
  border: 1px solid #eee;
}

.attachment-icon {
  font-style: normal;
  color: #666;
}

.attachment-name {
  flex-grow: 1;
}

.attachment-size {
  color: #999;
  font-size: 12px;
}

.attachment-download {
  padding: 4px 10px;
  background: #1f74c0;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.attachment-download:hover {
  background: #1a5f9e;
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
}

.modal-content {
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  padding: 24px;
  width: 400px;
  max-width: 90%;
}

.modal-title {
  color: #1f74c0;
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 12px;
}

.modal-message {
  color: #333;
  font-size: 16px;
  margin-bottom: 24px;
}

.modal-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.modal-cancel-btn {
  padding: 8px 16px;
  background: #f5f7fa;
  color: #1f74c0;
  border: 1px solid #cce2fa;
  border-radius: 4px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
}

.modal-cancel-btn:hover {
  background: #e6f2fb;
}

.modal-confirm-btn {
  padding: 8px 16px;
  background: #1f74c0;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
}

.modal-confirm-btn:hover {
  background: #1a5f9e;
}

/* Toast提示样式 */
.toast-message {
  position: fixed;
  top: 100px;
  right: 30px;
  padding: 12px 24px;
  border-radius: 4px;
  color: white;
  font-size: 16px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  z-index: 100;
  animation: fadeIn 0.3s ease;
}

.toast-message.success {
  background: #4CAF50;
}

.toast-message.error {
  background: #F44336;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>