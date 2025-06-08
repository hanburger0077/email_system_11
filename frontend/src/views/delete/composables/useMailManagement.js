import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 邮件管理 Composable
 * 提供邮件的增删改查、选择、分页等功能
 */
export function useMailManagement() {
  // 响应式状态
  const searchKeyword = ref('')
  const currentPage = ref(1)
  const itemsPerPage = ref(20)
  const selectedMails = ref([])
  const allSelected = ref(false)
  const isLoading = ref(false)

  // 模拟已删除的邮件数据
  const deletedMails = ref([
    {
      id: 1,
      sender: '邮箱的朋友',
      subject: '担心错过重要邮件？别怕！邮箱大师随时提醒！',
      time: new Date('2024-01-20 22:00'),
      isRead: true,
      hasAttachment: false,
      deleteTime: new Date('2024-01-21 10:00')
    },
    {
      id: 2,
      sender: 'admin@scut.edu.cn',
      subject: '系统维护通知',
      time: new Date('2024-01-19 15:30'),
      isRead: false,
      hasAttachment: true,
      deleteTime: new Date('2024-01-20 09:00')
    },
    {
      id: 3,
      sender: 'newsletter@company.com',
      subject: '本周热门文章推荐',
      time: new Date('2024-01-18 08:45'),
      isRead: true,
      hasAttachment: false,
      deleteTime: new Date('2024-01-19 14:30')
    },
    {
      id: 4,
      sender: 'support@service.com',
      subject: '您的服务即将到期',
      time: new Date('2024-01-17 16:20'),
      isRead: false,
      hasAttachment: false,
      deleteTime: new Date('2024-01-18 11:15')
    },
    {
      id: 5,
      sender: 'teacher@scut.edu.cn',
      subject: '期末考试安排通知',
      time: new Date('2024-01-16 10:00'),
      isRead: true,
      hasAttachment: true,
      deleteTime: new Date('2024-01-17 08:30')
    }
  ])

  // 计算属性
  const filteredMails = computed(() => {
    const keyword = searchKeyword.value.trim().toLowerCase()
    if (!keyword) return deletedMails.value
    
    return deletedMails.value.filter(mail => 
      mail.subject.toLowerCase().includes(keyword) ||
      mail.sender.toLowerCase().includes(keyword)
    )
  })

  const totalPages = computed(() => {
    return Math.ceil(filteredMails.value.length / itemsPerPage.value) || 1
  })

  const paginatedMails = computed(() => {
    const start = (currentPage.value - 1) * itemsPerPage.value
    const end = start + itemsPerPage.value
    return filteredMails.value.slice(start, end)
  })

  const selectedCount = computed(() => selectedMails.value.length)

  const hasSelection = computed(() => selectedCount.value > 0)

  // 选择相关方法
  function handleSelectAll(selected) {
    try {
      if (selected) {
        selectedMails.value = [...paginatedMails.value.map(mail => mail.id)]
      } else {
        selectedMails.value = []
      }
      updateAllSelected()
    } catch (error) {
      console.error('选择全部邮件失败:', error)
      ElMessage.error('操作失败')
    }
  }

  function selectMail(mailId) {
    try {
      const index = selectedMails.value.indexOf(mailId)
      if (index > -1) {
        selectedMails.value.splice(index, 1)
      } else {
        selectedMails.value.push(mailId)
      }
      updateAllSelected()
    } catch (error) {
      console.error('选择邮件失败:', error)
    }
  }

  function updateAllSelected() {
    allSelected.value = selectedMails.value.length === paginatedMails.value.length && 
                      paginatedMails.value.length > 0
  }

  function selectAll() {
    try {
      selectedMails.value = [...filteredMails.value.map(mail => mail.id)]
      allSelected.value = true
    } catch (error) {
      console.error('全选失败:', error)
      ElMessage.error('全选失败')
    }
  }

  function clearSelection() {
    selectedMails.value = []
    allSelected.value = false
  }

  // 邮件操作方法
  async function permanentDelete() {
    if (!hasSelection.value) return

    try {
      isLoading.value = true
      // 模拟 API 调用延迟
      await new Promise(resolve => setTimeout(resolve, 500))
      
      deletedMails.value = deletedMails.value.filter(
        mail => !selectedMails.value.includes(mail.id)
      )
      clearSelection()
      
      // 确保当前页有效
      if (currentPage.value > totalPages.value) {
        currentPage.value = Math.max(1, totalPages.value)
      }
      
      ElMessage.success('邮件已彻底删除')
    } catch (error) {
      console.error('删除邮件失败:', error)
      ElMessage.error('删除失败，请重试')
    } finally {
      isLoading.value = false
    }
  }

  async function restoreSelected() {
    if (!hasSelection.value) return

    try {
      isLoading.value = true
      await new Promise(resolve => setTimeout(resolve, 500))
      
      deletedMails.value = deletedMails.value.filter(
        mail => !selectedMails.value.includes(mail.id)
      )
      clearSelection()
      
      ElMessage.success('邮件已恢复到收件箱')
    } catch (error) {
      console.error('恢复邮件失败:', error)
      ElMessage.error('恢复失败，请重试')
    } finally {
      isLoading.value = false
    }
  }

  function markAs(type) {
    if (!hasSelection.value) return

    try {
      selectedMails.value.forEach(mailId => {
        const mail = deletedMails.value.find(m => m.id === mailId)
        if (mail) {
          switch (type) {
            case 'read':
              mail.isRead = true
              break
            case 'unread':
              mail.isRead = false
              break
            case 'starred':
              mail.isStarred = !mail.isStarred
              break
          }
        }
      })
      
      const actionMap = {
        read: '已标记为已读',
        unread: '已标记为未读',
        starred: '已标记星标'
      }
      ElMessage.success(actionMap[type] || '标记成功')
    } catch (error) {
      console.error('标记邮件失败:', error)
      ElMessage.error('标记失败')
    }
  }

  async function moveTo(destination) {
    if (!hasSelection.value) return

    try {
      switch (destination) {
        case 'inbox':
          await restoreSelected()
          break
        case 'spam':
        case 'draft':
          deletedMails.value = deletedMails.value.filter(
            mail => !selectedMails.value.includes(mail.id)
          )
          clearSelection()
          
          const destinationMap = {
            spam: '垃圾邮件',
            draft: '草稿箱'
          }
          ElMessage.success(`邮件已移动到${destinationMap[destination]}`)
          break
      }
    } catch (error) {
      console.error('移动邮件失败:', error)
      ElMessage.error('移动失败')
    }
  }

  function handleMoreAction(action) {
    try {
      switch (action) {
        case 'selectAll':
          selectAll()
          break
        case 'clearSelection':
          clearSelection()
          break
        case 'export':
          if (hasSelection.value) {
            ElMessage.info('导出功能开发中...')
          } else {
            ElMessage.warning('请先选择邮件')
          }
          break
      }
    } catch (error) {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    }
  }

  // 分页方法
  function goToPage(page) {
    if (page >= 1 && page <= totalPages.value) {
      currentPage.value = page
    }
  }

  function prevPage() {
    if (currentPage.value > 1) {
      currentPage.value--
    }
  }

  function nextPage() {
    if (currentPage.value < totalPages.value) {
      currentPage.value++
    }
  }

  // 搜索方法
  function handleSearch() {
    currentPage.value = 1
    clearSelection()
  }

  // 监听器
  watch(selectedMails, updateAllSelected, { deep: true })
  
  watch(searchKeyword, () => {
    handleSearch()
  })

  // 当分页数据变化时，清理无效的选择
  watch(paginatedMails, () => {
    const validIds = paginatedMails.value.map(mail => mail.id)
    selectedMails.value = selectedMails.value.filter(id => validIds.includes(id))
  })

  return {
    // 响应式数据
    searchKeyword,
    currentPage,
    itemsPerPage,
    selectedMails,
    allSelected,
    deletedMails,
    isLoading,
    
    // 计算属性
    filteredMails,
    totalPages,
    paginatedMails,
    selectedCount,
    hasSelection,
    
    // 方法
    handleSelectAll,
    selectMail,
    updateAllSelected,
    selectAll,
    clearSelection,
    permanentDelete,
    restoreSelected,
    markAs,
    moveTo,
    handleMoreAction,
    goToPage,
    prevPage,
    nextPage,
    handleSearch
  }
} 