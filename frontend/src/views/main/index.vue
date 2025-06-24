<template>
  <div class="mail-list">
    <div class="mail-toolbar">
      <div class="toolbar-left">
        <el-checkbox 
          v-model="allSelected" 
          @change="toggleSelectAll" 
          class="select-all-checkbox" 
        />

        <el-tooltip content="删除" placement="bottom">
          <el-button  
            :disabled="selectedMails.length === 0"
            @click="deleteSelected"
            class="delete-button icon-button"
          >
            <img src="@/assets/delete-icon.svg" class="delete-icon" alt="删除" />
          </el-button>
        </el-tooltip>

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
                <span>加注星标</span>
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
        <!-- 修改：当没有数据时，显示"暂无分页"而不是页码信息 -->
        <span class="page-info" v-if="totalPages > 0">{{ currentPage }}/{{ totalPages }}页</span>
        <span class="page-info" v-else>暂无分页</span>
        <div class="pagination-controls">
          <el-button 
            size="small" 
            :disabled="currentPage <= 1"
            @click="prevPage"
          >
            上一页
          </el-button>
          <el-button 
            size="small" 
            :disabled="currentPage >= totalPages || totalPages === 0"
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

    <div class="list-content" v-if="!isLoading">
      <div v-for="(group, index) in groupedMails" :key="index" class="mail-group">
        <h3 class="group-title" @click="toggleExpand(index)">
          {{ group.title }}
          <span class="expand-icon">{{ group.isExpanded ? '−' : '+' }}</span>
        </h3>

        <div v-show="group.isExpanded" class="mail-items">
          <div 
            v-for="(mail, mailIndex) in group.mails" 
            :key="mail.mail_id || mail.globalIndex" 
            class="mail-item" 
            :class="{ 'unread': !mail.isRead }"
            :data-mail-id="mail.mail_id"
          >
            <div class="checkbox-container">
              <el-checkbox v-model="selectedMails" :value="mail.mail_id || mail.globalIndex" class="item-checkbox" />
            </div>
            <div class="mail-content" @click="openMail(mail)">
              <span class="column sender">{{ mail.sender_email || mail.sender }}</span>
              <span class="column subject">{{ mail.subject }}</span>
              <span class="column time">{{ formatTime(mail.create_at || mail.send_time || mail.time) }}</span>
              <span 
                class="star-icon" 
                :class="{ 'star-filled': isStarred(mail) }" 
                @click.stop="toggleStar(mail)"
              >{{ isStarred(mail) ? '★' : '☆' }}</span>
            </div>
          </div>

          <div v-if="group.mails.length === 0 && group.isExpanded" class="empty-message">
            该分类暂无邮件
          </div>
        </div>
      </div>

      <div v-if="mailList.length === 0" class="empty-message">
        当前邮箱内无邮件
      </div>
    </div>

    <div v-else class="loading-container">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <!-- 新邮件通知 -->
    <div v-show="showNewMailNotification" class="new-mail-notification">
      <div class="notification-content">
        <i class="notification-icon">📬</i>
        <div class="notification-text">{{ newMailMessage }}</div>
        <button class="notification-close" @click="hideNewMailNotification">×</button>
      </div>
    </div>
  </div>
</template>

<script>
import { ArrowDown, Loading } from '@element-plus/icons-vue'
import mark1Icon from '@/assets/read-icon.svg'
import mark2Icon from '@/assets/unread-icon.svg'
import mark3Icon from '@/assets/refresh-icon.svg'
import mark4Icon from './assets/mark4.png'

export default {
  name: 'MainPage',
  components: {
    ArrowDown,
    Loading
  },
  data() {
    return {
      mark1Icon,
      mark2Icon,
      mark3Icon,
      mark4Icon,
      currentPage: 1,  // 修改：初始化为1而不是0
      totalPages: 0,
      itemsPerPage: 10, // 使用API的页大小
      selectedMails: [],
      allSelected: false,
      showMarkDropdown: false,
      mailList: [],
      groupedMails: [
        { title: "最近三天", isExpanded: true, mails: [] },
        { title: "最近一周", isExpanded: true, mails: [] },
        { title: "更早", isExpanded: true, mails: [] },
      ],
      currentFolder: 'INBOX', // 默认文件夹
      isLoading: false,
      eventSource: null, // SSE连接对象
      showNewMailNotification: false,
      newMailMessage: '',
      notificationTimer: null
    };
  },
  computed: {
    paginatedMails() {
      return this.mailList.map((mail, i) => ({
        ...mail,
        isRead: mail.read === 1,
        globalIndex: i
      }));
    }
  },
  methods: {
    // 格式化时间显示 - 始终显示完整年月日和时间
    formatTime(dateStr) {
      if (!dateStr) return '未知时间';
      
      try {
        // 解析日期
        let date;
        if (typeof dateStr === 'number') {
          date = new Date(dateStr);
        } else if (typeof dateStr === 'string') {
          // 尝试解析不同格式的字符串日期
          date = new Date(dateStr);
        } else {
          return '未知时间';
        }
        
        // 检查日期是否有效
        if (isNaN(date.getTime())) {
          console.warn('无法解析的时间:', dateStr);
          return '未知时间';
        }
        
        // 获取年月日时分秒
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const seconds = String(date.getSeconds()).padStart(2, '0');
        
        // 始终返回完整格式的日期时间
        return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
      } catch (error) {
        console.error('格式化时间错误:', error);
        return '未知时间';
      }
    },
    
    // 解析日期为Date对象，用于比较
    parseDate(dateStr) {
      if (!dateStr) return new Date(0); // 默认最早时间
      
      let date;
      try {
        if (typeof dateStr === 'number') {
          // 时间戳
          date = new Date(dateStr);
        } else if (typeof dateStr === 'string') {
          // 直接尝试解析字符串
          date = new Date(dateStr);
        } else {
          // 默认当前日期
          date = new Date();
        }
        
        if (isNaN(date.getTime())) {
          console.warn(`无法解析的日期: ${dateStr}`);
          date = new Date(0);
        }
      } catch (error) {
        console.error(`日期解析错误: ${error}`, dateStr);
        date = new Date(0);
      }
      
      return date;
    },
    
    // 检查邮件是否被星标
    isStarred(mail) {
      if (this.currentFolder === 'INBOX') {
        return mail.receiver_star === 1 || mail.isStarred === true;
      } else if (this.currentFolder === 'SENT') {
        return mail.sender_star === 1 || mail.isStarred === true;
      }
      return mail.isStarred === true;
    },
    
    // 初始化SSE连接
    initSSE() {
      try {
        // 关闭可能存在的旧连接
        this.closeSSE();
        
        // 仅在INBOX文件夹时建立SSE连接
        if (this.currentFolder === 'INBOX') {
          console.log('初始化SSE连接...');
          this.eventSource = new EventSource('/api/mail/sse/emails');
          
          this.eventSource.onmessage = (event) => {
            console.log('收到新邮件通知:', event.data);
            this.showNewMailNotification = true;
            this.newMailMessage = `收到新邮件: ${event.data}`;
            
            // 自动刷新邮件列表
            this.loadMails(this.currentPage);
            
            // 10秒后自动关闭通知
            if (this.notificationTimer) clearTimeout(this.notificationTimer);
            this.notificationTimer = setTimeout(() => {
              this.hideNewMailNotification();
            }, 10000);
          };
          
          this.eventSource.onerror = (error) => {
            console.error('SSE连接错误:', error);
            // 尝试重连
            setTimeout(() => this.initSSE(), 5000);
          };
          
          console.log('SSE连接已建立');
        }
      } catch (error) {
        console.error('初始化SSE连接失败:', error);
      }
    },
    
    // 关闭SSE连接
    closeSSE() {
      if (this.eventSource) {
        this.eventSource.close();
        this.eventSource = null;
        console.log('SSE连接已关闭');
      }
    },
    
    // 隐藏新邮件通知
    hideNewMailNotification() {
      this.showNewMailNotification = false;
    },
    
    // 分组邮件 - 根据邮件日期进行分组
    groupAndIndexMails() {
      const today = new Date().setHours(0, 0, 0, 0);
      const groups = {
        "最近三天": [],
        "最近一周": [],
        "更早": []
      };

      this.paginatedMails.forEach(mail => {
        // 获取邮件日期，优先使用create_at字段
        const mailDate = this.parseDate(mail.create_at || mail.send_time || mail.time);
        const mailTimestamp = mailDate.setHours(0, 0, 0, 0);
        
        // 根据日期差分组
        const daysDiff = Math.floor((today - mailTimestamp) / (24 * 60 * 60 * 1000));
        
        if (daysDiff <= 2) {
          groups["最近三天"].push(mail);
        } else if (daysDiff <= 6) {
          groups["最近一周"].push(mail);
        } else {
          groups["更早"].push(mail);
        }
      });

      // 更新分组
      this.groupedMails.forEach(group => {
        group.mails = groups[group.title];
      });
      
      // 打印日志信息，帮助调试
      console.log('邮件分组结果:', {
        '最近三天': groups["最近三天"].length,
        '最近一周': groups["最近一周"].length, 
        '更早': groups["更早"].length
      });
      
      // 打印加载的第一封邮件的时间信息，帮助调试
      if (this.mailList.length > 0) {
        const firstMail = this.mailList[0];
        console.log('第一封邮件时间信息:', {
          create_at: firstMail.create_at,
          send_time: firstMail.send_time,
          formatted: this.formatTime(firstMail.create_at || firstMail.send_time || firstMail.time)
        });
      }
    },
    
    // 加载邮件
    async loadMails(page) {
      if (page < 1) page = 1;
      if (this.totalPages > 0 && page > this.totalPages) page = this.totalPages;
      
      this.isLoading = true;
      this.currentPage = page;
      
      try {
        const response = await fetch(`/api/mail/${this.currentFolder}/pages/${page}`);
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          this.mailList = result.data || [];
          console.log('加载邮件数据样本:', this.mailList.length > 0 ? this.mailList[0] : 'No emails');
          // 更新总页数
          this.totalPages = parseInt(result.message, 10) || 0; // 修改：没有邮件时totalPages为0
          this.groupAndIndexMails();
          console.log('邮件加载成功，共', this.mailList.length, '封');
        } else {
          console.error('加载邮件失败:', result.message, result.reason);
          this.$message.error(`加载邮件失败: ${result.message}`);
          // 修改：失败时重置数据状态
          this.mailList = [];
          this.totalPages = 0;
          this.groupAndIndexMails();
        }
      } catch (error) {
        console.error('请求邮件出错:', error);
        this.$message.error('加载邮件失败，请检查网络连接');
        // 修改：错误时重置数据状态
        this.mailList = [];
        this.totalPages = 0;
        this.groupAndIndexMails();
      } finally {
        this.isLoading = false;
      }
    },
    
    // 搜索邮件
    async searchMails(from = '', to = '', subject = '', body = '', since = 0, unseen = null) {
      this.isLoading = true;
      
      try {
        // 构建URL
        const url = new URL(`/api/mail/${this.currentFolder}/pages/${this.currentPage}/search`, window.location.origin);
        url.searchParams.append('from', from);
        url.searchParams.append('to', to);
        url.searchParams.append('subject', subject);
        url.searchParams.append('body', body);
        url.searchParams.append('since', since);
        
        if (unseen === 'UNSEEN' || unseen === 'SEEN') {
          url.searchParams.append('unseen', unseen);
        }
        
        // 根据当前文件夹设置星标搜索条件
        if (this.currentFolder === 'INBOX') {
          url.searchParams.append('receiver_star', true);
        } else if (this.currentFolder === 'SENT') {
          url.searchParams.append('sender_star', true);
        }
        
        const response = await fetch(url);
        const result = await response.json();
        
        if (result.code === 'code.ok') {
          this.mailList = result.data || [];
          this.totalPages = parseInt(result.message, 10) || 0; // 修改：搜索无结果时totalPages为0
          this.groupAndIndexMails();
          this.$message.success('搜索完成');
        } else {
          console.error('搜索邮件失败:', result.message, result.reason);
          this.$message.error(`搜索失败: ${result.message}`);
          // 修改：搜索失败时重置数据状态
          this.mailList = [];
          this.totalPages = 0;
          this.groupAndIndexMails();
        }
      } catch (error) {
        console.error('搜索请求出错:', error);
        this.$message.error('搜索失败，请检查网络连接');
        // 修改：搜索错误时重置数据状态
        this.mailList = [];
        this.totalPages = 0;
        this.groupAndIndexMails();
      } finally {
        this.isLoading = false;
      }
    },
    
    // 检查mailId参数并高亮邮件
    checkMailIdParam() {
      const mailId = this.$route.query.mailId;
      if (mailId) {
        // 延迟执行，确保邮件列表已加载
        this.$nextTick(() => {
          setTimeout(() => {
            this.highlightMail(parseInt(mailId));
          }, 500);
        });
      }
    },
    
    // 高亮指定邮件
    highlightMail(mailId) {
      // 查找并高亮对应的邮件项
      const mailElement = document.querySelector(`[data-mail-id="${mailId}"]`);
      if (mailElement) {
        mailElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
        mailElement.style.backgroundColor = '#ecf5ff';
        mailElement.style.border = '2px solid #409eff';
        
        // 3秒后移除高亮效果
        setTimeout(() => {
          mailElement.style.backgroundColor = '';
          mailElement.style.border = '';
        }, 3000);
      }
    },
    
    // 翻页函数
    prevPage() {
      if (this.currentPage > 1) {
        this.loadMails(this.currentPage - 1);
      }
    },
    nextPage() {
      if (this.currentPage < this.totalPages) {
        this.loadMails(this.currentPage + 1);
      }
    },
    
    // 刷新邮件列表
    handleReceive() {
      this.loadMails(1);
      this.$message.success('刷新成功，已获取最新邮件');
    },
    
    // 展开/收起邮件组
    toggleExpand(index) {
      this.groupedMails[index].isExpanded = !this.groupedMails[index].isExpanded;
    },
    
    // 切换全选
    toggleSelectAll() {
      if (this.allSelected) {
        this.selectedMails = this.paginatedMails.map(mail => mail.mail_id || mail.globalIndex);
      } else {
        this.selectedMails = [];
      }
    },
    
     // 删除选中的邮件（放入回收站）
      async deleteSelected() {
        if (this.selectedMails.length === 0) return;
        try {
          this.$confirm('确认删除选中的邮件吗？', '提示', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }).then(async () => {
            this.isLoading = true;
            for (const mailId of this.selectedMails) {
              try {
                // 使用当前文件夹（如INBOX）作为发起源，然后将邮件移入回收站
                await fetch(`/api/mail/${this.currentFolder}/mails/${mailId}/change/TRASH/+FLAG`, {
                  method: 'POST'
                });
              } catch (error) {
                console.error(`删除邮件 ${mailId} 失败:`, error);
              }
            }
            this.$message.success('删除成功，邮件已移入回收站');
            this.selectedMails = [];
            this.allSelected = false;
            this.loadMails(this.currentPage);
          }).catch(() => {
            this.$message.info('已取消删除操作');
          });
        } catch (error) {
          console.error('删除邮件出错:', error);
          this.$message.error('删除邮件失败');
        } finally {
          this.isLoading = false;
        }
      },

      // 删除所有邮件（放入回收站）
      async deleteAll() {
        this.$confirm('确认删除所有邮件吗？', '警告', {
          confirmButtonText: '确定删除',
          cancelButtonText: '取消',
          type: 'warning',
          confirmButtonClass: 'el-button--danger'
        }).then(async () => {
          this.isLoading = true;
          try {
            const response = await fetch(`/api/mail/${this.currentFolder}/pages/1`);
            const result = await response.json();
            if (result.code === 'code.ok' && result.data) {
              for (const mail of result.data) {
                // 使用当前文件夹作为发起源，将邮件移入回收站
                await fetch(`/api/mail/${this.currentFolder}/mails/${mail.mail_id}/change/TRASH/+FLAG`, {
                  method: 'POST'
                });
              }
              this.mailList = [];
              this.selectedMails = [];
              this.allSelected = false;
              this.currentPage = 1;
              this.totalPages = 0;
              this.groupAndIndexMails();
              this.$message.success('已删除所有邮件，邮件已移入回收站');
            } else {
              this.$message.error('获取邮件列表失败，无法删除');
            }
          } catch (error) {
            console.error('删除所有邮件出错:', error);
            this.$message.error('删除邮件失败');
          } finally {
            this.isLoading = false;
          }
        }).catch(() => {
          // 取消删除操作
        });
      },
      
    // 打开邮件详情
    async openMail(mail) {
      const mailId = mail.mail_id || mail.globalIndex;
      
      // 如果是真实邮件ID，先将其标记为已读
      if (mail.mail_id && this.currentFolder === 'INBOX' && mail.read === 0) {
        try {
          // 使用新的API格式标记为已读
          await fetch(`/api/mail/${this.currentFolder}/mails/${mail.mail_id}/change/READ/+FLAG`, {
            method: 'POST'
          });
          // 更新本地邮件的已读状态
          mail.read = 1;
          mail.isRead = true;
        } catch (error) {
          console.error('标记邮件为已读失败:', error);
        }
      }
      
      // 存储当前所有邮件数据到sessionStorage，以便在详情页面可以访问和修改
      sessionStorage.setItem('allMails', JSON.stringify(this.mailList));
      
      // 使用sessionStorage存储当前邮件数据
      sessionStorage.setItem('currentMail', JSON.stringify(mail));
      
      // 跳转到邮件详情页面
      this.$router.push({
        path: '/mail-detail',
        query: { 
          id: mailId,
          mailbox: this.currentFolder,
          from: this.currentFolder.toLowerCase()
        }
      });
    },
    
    // 标记为已读
    async markAsRead() {
      if (this.selectedMails.length === 0) return;
      
      // 只有INBOX文件夹支持已读/未读标记
      if (this.currentFolder !== 'INBOX') {
        this.$message.info('只有收件箱支持已读/未读标记');
        return;
      }
      
      try {
        this.isLoading = true;
        
        for (const mailId of this.selectedMails) {
          if (typeof mailId === 'number') { // 只操作真实邮件，非模拟数据
            // 使用新的API格式标记为已读
            await fetch(`/api/mail/${this.currentFolder}/mails/${mailId}/change/READ/+FLAG`, {
              method: 'POST'
            });
            
            // 更新本地邮件的已读状态
            const mailIndex = this.mailList.findIndex(m => m.mail_id === mailId);
            if (mailIndex !== -1) {
              this.mailList[mailIndex].read = 1;
            }
          }
        }
        
        this.groupAndIndexMails();
        this.$message.success('已标记为已读');
      } catch (error) {
        console.error('标记为已读出错:', error);
        this.$message.error('标记为已读失败');
      } finally {
        this.isLoading = false;
      }
    },

    // 修改 toggleStar 方法，确保对“收件人星标”操作使用 R_STAR 与+FLAG/-FLAG
    async toggleStar(mail) {
        if (!mail.mail_id) return;
        try {
            const operation = mail.receiver_star === 1 ? '-FLAG' : '+FLAG';
            const response = await fetch(`/api/mail/${this.currentFolder}/mails/${mail.mail_id}/change/R_STAR/${operation}`, {
                method: 'POST'
            });
            const result = await response.json();
            if (result.code === 'code.ok') {
                mail.receiver_star = mail.receiver_star === 1 ? 0 : 1;
                this.$message.success(`已成功${mail.receiver_star === 1 ? '添加' : '取消'}星标`);
            } else {
                this.$message.error(`修改星标状态失败: ${result.reason || result.message}`);
            }
        } catch (error) {
            console.error('修改星标状态出错:', error);
            this.$message.error('修改星标状态失败');
        }
    },

    // 标记/取消标记星标
    async markSelectedAsStarred(starred) {
      if (this.selectedMails.length === 0) return;
      
      // 确定星标字段和操作
      let starSign;
      let operation = starred ? '+FLAG' : '-FLAG';
      
      if (this.currentFolder === 'INBOX') {
        starSign = 'R_STAR'; // 收件人星标
      } else if (this.currentFolder === 'SENT') {
        starSign = 'S_STAR'; // 发件人星标
      } else {
        this.$message.info('当前文件夹不支持星标操作');
        return;
      }
      
      try {
        this.isLoading = true;
        let successCount = 0;
        let failureCount = 0;
        
        for (const mailId of this.selectedMails) {
          // 确保只处理有效的邮件ID（忽略模拟数据）
          if (typeof mailId === 'number' && mailId > 0) {
            try {
              // 使用新的API格式修改星标状态
              const response = await fetch(`/api/mail/${this.currentFolder}/mails/${mailId}/change/${starSign}/${operation}`, {
                method: 'POST'
              });
              
              const result = await response.json();
              
              if (result.code === 'code.ok') {
                // 更新本地邮件的星标状态
                const mailIndex = this.mailList.findIndex(m => m.mail_id === mailId);
                if (mailIndex !== -1) {
                  if (this.currentFolder === 'INBOX') {
                    this.mailList[mailIndex].receiver_star = starred ? 1 : 0;
                  } else {
                    this.mailList[mailIndex].sender_star = starred ? 1 : 0;
                  }
                }
                successCount++;
              } else {
                failureCount++;
                console.error(`邮件 ${mailId} 星标操作失败:`, result);
              }
            } catch (err) {
              failureCount++;
              console.error(`邮件 ${mailId} 星标操作异常:`, err);
            }
          }
        }
        
        // 重新组织邮件分组
        this.groupAndIndexMails();
        
        // 显示操作结果
        if (successCount > 0) {
          if (failureCount === 0) {
            this.$message({
              message: starred ? `已成功为 ${successCount} 封邮件添加星标` : `已成功为 ${successCount} 封邮件取消星标`,
              type: 'success',
              duration: 3000
            });
          } else {
            this.$message({
              message: `已为 ${successCount} 封邮件更新星标状态，${failureCount} 封邮件操作失败`,
              type: 'warning',
              duration: 3000
            });
          }
        } else if (failureCount > 0) {
          this.$message.error(`星标操作失败，请稍后重试`);
        }
      } catch (error) {
        console.error('批量修改星标状态出错:', error);
        this.$message.error('修改星标状态失败，请检查网络连接');
      } finally {
        this.isLoading = false;
      }
    },
    
    // 标记为未读
    async markAsUnread() {
      if (this.selectedMails.length === 0) return;
      
      // 只有INBOX文件夹支持已读/未读标记
      if (this.currentFolder !== 'INBOX') {
        this.$message.info('只有收件箱支持已读/未读标记');
        return;
      }
      
      try {
        this.isLoading = true;
        
        for (const mailId of this.selectedMails) {
          if (typeof mailId === 'number') { // 只操作真实邮件，非模拟数据
            // 使用新的API格式标记为未读
            await fetch(`/api/mail/${this.currentFolder}/mails/${mailId}/change/READ/-FLAG`, {
              method: 'POST'
            });
            
            // 更新本地邮件的未读状态
            const mailIndex = this.mailList.findIndex(m => m.mail_id === mailId);
            if (mailIndex !== -1) {
              this.mailList[mailIndex].read = 0;
            }
          }
        }
        
        this.groupAndIndexMails();
        this.$message.success('已标记为未读');
      } catch (error) {
        console.error('标记为未读出错:', error);
        this.$message.error('标记为未读失败');
      } finally {
        this.isLoading = false;
      }
    },
    
    // 处理标记命令
    async handleMarkCommand(command) {
      if (this.selectedMails.length === 0) return;
      
      if (command === 'star') {
        await this.markSelectedAsStarred(true);
      } else if (command === 'unstar') {
        await this.markSelectedAsStarred(false);
      }
    },
  },
  watch: {
    // 监听路由参数变化
    '$route'(to, from) {
      const folder = to.query.folder || 'INBOX';
      if (folder !== this.currentFolder) {
        this.currentFolder = folder;
        this.currentPage = 1;
        this.selectedMails = [];
        this.allSelected = false;
        this.loadMails(1);
        
        // 如果切换到INBOX，初始化SSE连接
        this.initSSE();
      }
    }
  },
  created() {
    // 从路由参数获取当前文件夹
    this.currentFolder = this.$route.query.folder || 'INBOX';
  },
  mounted() {
    // 初始化加载邮件
    this.loadMails(1);
    
    // 初始化SSE连接
    this.initSSE();
    
    // 检查是否有mailId参数，如果有则高亮显示对应邮件
    this.checkMailIdParam();
  },
  beforeDestroy() {
    // 组件销毁前关闭SSE连接
    this.closeSSE();
    
    // 清除通知定时器
    if (this.notificationTimer) {
      clearTimeout(this.notificationTimer);
    }
  }
}
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

.delete-button.icon-button {
  width: 32px;
  height: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.delete-icon {
  width: 18px;
  height: 18px;
}

.delete-button:hover {
  background: #f5f7fa;        
  border-color: #c6e2ff;      
  color: #409eff;            
}

.delete-button:active {
  background: #f5f7fa;
  border-color: #3a8ee6;
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
  font-size: 1.2em;
  margin-left: 8px;
  cursor: pointer;
  color: #999;
  transition: color 0.2s;
}

.star-filled {
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

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #1f74c0;
  gap: 10px;
  font-size: 16px;
}

.new-mail-notification {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 1000;
  animation: slide-in 0.3s ease-out;
}

.notification-content {
  display: flex;
  align-items: center;
  background-color: #4CAF50;
  color: white;
  padding: 16px;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.2);
  min-width: 280px;
}

.notification-icon {
  font-size: 24px;
  margin-right: 12px;
}

.notification-text {
  flex-grow: 1;
}

.notification-close {
  background: none;
  border: none;
  color: white;
  font-size: 20px;
  cursor: pointer;
  padding: 0;
  margin-left: 10px;
}

@keyframes slide-in {
  from {
    transform: translateY(100px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
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