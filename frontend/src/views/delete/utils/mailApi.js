// 邮件相关API接口封装

// 基础配置
const BASE_URL = '/api/mail' // 根据实际情况调整

// HTTP请求封装
async function request(url, options = {}) {
  try {
    const response = await fetch(`${BASE_URL}${url}`, {
      headers: {
        'Content-Type': 'application/json',
        // 可以在这里添加认证header
        ...options.headers
      },
      ...options
    })
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    
    // 如果响应为空，返回空对象
    const text = await response.text()
    return text ? JSON.parse(text) : {}
  } catch (error) {
    console.error('API请求失败:', error)
    throw error
  }
}

/**
 * 插入邮件
 * @param {Object} mail - 邮件对象
 */
export async function insertMail(mail) {
  return await request('/insert', {
    method: 'POST',
    body: JSON.stringify({ mail })
  })
}

/**
 * 根据接收者ID和标签查询邮件
 * @param {Object} params - 查询参数
 * @param {number} params.userId - 当前操作用户ID
 * @param {number} params.senderId - 发件人ID (不需要时设为0)
 * @param {string} params.subject - 主题 (不需要时传null)
 * @param {string} params.body - 正文 (不需要时传null)
 * @param {string} params.since - 时间 (不需要时传null)
 * @param {number} params.receiver_sign - 接收人标签 (0-收件箱, 1-回收站, 2-垃圾箱)
 * @param {string} params.read - 读取标签 ("SEEN"/"UNSEEN"/null)
 * @param {string} params.receiver_star - 星标 (不需要时传null)
 * @returns {Promise<Array<number>>} 邮件ID列表
 */
export async function selectByReceiverIdWithSign(params) {
  const queryParams = new URLSearchParams()
  
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined) {
      queryParams.append(key, value)
    }
  })
  
  return await request(`/selectByReceiver?${queryParams}`, {
    method: 'GET'
  })
}

/**
 * 根据发送者ID和标签查询邮件
 * @param {Object} params - 查询参数
 * @param {number} params.userId - 当前操作用户ID
 * @param {number} params.receiverId - 收件人ID (不需要时设为0)
 * @param {string} params.subject - 主题 (不需要时传null)
 * @param {string} params.body - 正文 (不需要时传null)
 * @param {string} params.since - 时间 (不需要时传null)
 * @param {number} params.sender_sign - 发送人标签 (0-发件箱, 1-草稿箱)
 * @param {string} params.sender_star - 星标 (不需要时传null)
 * @returns {Promise<Array<number>>} 邮件ID列表
 */
export async function selectBySenderIdWithSign(params) {
  const queryParams = new URLSearchParams()
  
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined) {
      queryParams.append(key, value)
    }
  })
  
  return await request(`/selectBySender?${queryParams}`, {
    method: 'GET'
  })
}

/**
 * 统计发送方邮件数量
 * @param {number} userId - 用户ID
 * @param {number} sender_sign - 发送方标签
 * @returns {Promise<number>} 邮件数量
 */
export async function countBySenderIdWithSign(userId, sender_sign) {
  return await request(`/countBySender?userId=${userId}&sender_sign=${sender_sign}`, {
    method: 'GET'
  })
}

/**
 * 统计接收方邮件数量
 * @param {number} userId - 用户ID
 * @param {number} receiver_sign - 接收方标签
 * @returns {Promise<number>} 邮件数量
 */
export async function countByReceiverIdWithSign(userId, receiver_sign) {
  return await request(`/countByReceiver?userId=${userId}&receiver_sign=${receiver_sign}`, {
    method: 'GET'
  })
}

/**
 * 根据邮件ID查询邮件详情
 * @param {number} mailId - 邮件ID
 * @returns {Promise<Object>} 邮件对象
 */
export async function selectByMailId(mailId) {
  return await request(`/selectById?mailId=${mailId}`, {
    method: 'GET'
  })
}

/**
 * 改变邮件状态
 * @param {number} mailId - 邮件ID
 * @param {string} sign - 操作标签 ("READ"/"S_STAR"/"R_STAR"/"TRASH")
 * @param {string} op - 操作类型 ("+FLAG"/"-FLAG")
 */
export async function changeState(mailId, sign, op) {
  return await request('/changeState', {
    method: 'POST',
    body: JSON.stringify({
      mailId,
      sign,
      op
    })
  })
}

// 批量操作封装
/**
 * 批量删除邮件（移入回收站）
 * @param {Array<number>} mailIds - 邮件ID数组
 */
export async function batchDeleteToTrash(mailIds) {
  const promises = mailIds.map(mailId => 
    changeState(mailId, 'TRASH', '+FLAG')
  )
  return await Promise.all(promises)
}

/**
 * 批量恢复邮件（从回收站移出）
 * @param {Array<number>} mailIds - 邮件ID数组
 */
export async function batchRestoreFromTrash(mailIds) {
  const promises = mailIds.map(mailId => 
    changeState(mailId, 'TRASH', '-FLAG')
  )
  return await Promise.all(promises)
}

/**
 * 批量标记为已读
 * @param {Array<number>} mailIds - 邮件ID数组
 */
export async function batchMarkAsRead(mailIds) {
  const promises = mailIds.map(mailId => 
    changeState(mailId, 'READ', '+FLAG')
  )
  return await Promise.all(promises)
}

/**
 * 批量标记为未读
 * @param {Array<number>} mailIds - 邮件ID数组
 */
export async function batchMarkAsUnread(mailIds) {
  const promises = mailIds.map(mailId => 
    changeState(mailId, 'READ', '-FLAG')
  )
  return await Promise.all(promises)
}

/**
 * 批量设置星标
 * @param {Array<number>} mailIds - 邮件ID数组
 * @param {string} starType - 星标类型 ("S_STAR"/"R_STAR")
 */
export async function batchSetStar(mailIds, starType = 'R_STAR') {
  const promises = mailIds.map(mailId => 
    changeState(mailId, starType, '+FLAG')
  )
  return await Promise.all(promises)
}

/**
 * 批量取消星标
 * @param {Array<number>} mailIds - 邮件ID数组
 * @param {string} starType - 星标类型 ("S_STAR"/"R_STAR")
 */
export async function batchUnsetStar(mailIds, starType = 'R_STAR') {
  const promises = mailIds.map(mailId => 
    changeState(mailId, starType, '-FLAG')
  )
  return await Promise.all(promises)
}

// 获取当前用户ID的辅助函数（需要根据实际认证方式实现）
export function getCurrentUserId() {
  // TODO: 根据实际的认证方式获取用户ID
  // 可能从 localStorage、vuex/pinia store、JWT token 等获取
  return localStorage.getItem('userId') || 1 // 临时返回默认值
} 