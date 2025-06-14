# 邮件系统API接口设计示例

## 概述
基于前面的协议，这里提供邮件系统的具体API接口设计示例，供前后端开发参考。

## 1. 认证相关接口

### 1.1 用户登录
```
POST /api/v1/auth/login
Content-Type: application/json

请求体：
{
  "email": "user@example.com",
  "password": "password123"
}

成功响应 (200)：
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "token_type": "Bearer",
    "expires_in": 86400,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "display_name": "张三",
      "avatar_url": "https://example.com/avatars/1.jpg"
    }
  },
  "timestamp": "2024-01-15T10:30:00Z"
}

错误响应 (401)：
{
  "code": 401,
  "message": "邮箱或密码错误",
  "error": {
    "type": "AUTHENTICATION_ERROR"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 1.2 用户登出
```
POST /api/v1/auth/logout
Authorization: Bearer {access_token}

成功响应 (200)：
{
  "code": 200,
  "message": "登出成功",
  "data": null,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 1.3 刷新Token
```
POST /api/v1/auth/refresh
Content-Type: application/json

请求体：
{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

成功响应 (200)：
{
  "code": 200,
  "message": "Token刷新成功",
  "data": {
    "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expires_in": 86400
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## 2. 邮件相关接口

### 2.1 获取邮件列表
```
GET /api/v1/emails?folder=inbox&page=1&per_page=20&sort_by=created_at&sort_order=desc
Authorization: Bearer {access_token}

查询参数：
- folder: 邮件文件夹 (inbox|sent|drafts|trash|spam|archive)
- page: 页码，默认1
- per_page: 每页数量，默认20，最大100
- sort_by: 排序字段 (created_at|subject|from)
- sort_order: 排序方向 (asc|desc)
- search: 搜索关键词（可选）

成功响应 (200)：
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "items": [
      {
        "id": 1,
        "from": "sender@example.com",
        "from_name": "发送者姓名",
        "to": ["user@example.com"],
        "cc": [],
        "bcc": [],
        "subject": "邮件主题",
        "content_preview": "邮件内容预览...",
        "has_attachments": true,
        "attachment_count": 2,
        "status": "received",
        "priority": "normal",
        "is_read": false,
        "is_starred": false,
        "created_at": "2024-01-15T10:30:00Z",
        "updated_at": "2024-01-15T10:30:00Z"
      }
    ],
    "pagination": {
      "current_page": 1,
      "per_page": 20,
      "total": 100,
      "total_pages": 5,
      "has_next": true,
      "has_prev": false
    }
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 2.2 获取单个邮件详情
```
GET /api/v1/emails/{id}
Authorization: Bearer {access_token}

成功响应 (200)：
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "id": 1,
    "from": "sender@example.com",
    "from_name": "发送者姓名",
    "to": ["user@example.com"],
    "to_names": ["接收者姓名"],
    "cc": ["cc@example.com"],
    "cc_names": ["抄送者姓名"],
    "bcc": [],
    "bcc_names": [],
    "subject": "邮件主题",
    "content": "邮件完整内容...",
    "content_type": "html",
    "attachments": [
      {
        "id": 1,
        "filename": "document.pdf",
        "size": 1024000,
        "content_type": "application/pdf",
        "download_url": "/api/v1/attachments/1/download"
      }
    ],
    "status": "received",
    "priority": "normal",
    "is_read": true,
    "is_starred": false,
    "folder": "inbox",
    "created_at": "2024-01-15T10:30:00Z",
    "updated_at": "2024-01-15T10:30:00Z",
    "read_at": "2024-01-15T11:00:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 2.3 发送邮件
```
POST /api/v1/emails
Authorization: Bearer {access_token}
Content-Type: application/json

请求体：
{
  "to": ["recipient@example.com"],
  "cc": ["cc@example.com"],
  "bcc": [],
  "subject": "邮件主题",
  "content": "邮件内容",
  "content_type": "html",
  "priority": "normal",
  "attachment_ids": [1, 2],
  "send_at": null  // 定时发送时间，null表示立即发送
}

成功响应 (201)：
{
  "code": 201,
  "message": "邮件发送成功",
  "data": {
    "id": 123,
    "status": "sent",
    "sent_at": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}

错误响应 (422)：
{
  "code": 422,
  "message": "邮件发送失败",
  "error": {
    "type": "VALIDATION_ERROR",
    "details": [
      {
        "field": "to",
        "message": "收件人邮箱格式不正确"
      }
    ]
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 2.4 保存草稿
```
POST /api/v1/emails/drafts
Authorization: Bearer {access_token}
Content-Type: application/json

请求体：
{
  "to": ["recipient@example.com"],
  "cc": [],
  "bcc": [],
  "subject": "草稿主题",
  "content": "草稿内容",
  "content_type": "html",
  "attachment_ids": []
}

成功响应 (201)：
{
  "code": 201,
  "message": "草稿保存成功",
  "data": {
    "id": 124,
    "status": "draft",
    "created_at": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 2.5 更新邮件状态
```
PATCH /api/v1/emails/{id}/status
Authorization: Bearer {access_token}
Content-Type: application/json

请求体：
{
  "status": "read",  // read|unread|starred|unstarred|archived|deleted
  "folder": "inbox"  // 可选，移动到指定文件夹
}

成功响应 (200)：
{
  "code": 200,
  "message": "状态更新成功",
  "data": {
    "id": 1,
    "status": "read",
    "folder": "inbox",
    "updated_at": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 2.6 批量操作邮件
```
POST /api/v1/emails/batch
Authorization: Bearer {access_token}
Content-Type: application/json

请求体：
{
  "email_ids": [1, 2, 3, 4, 5],
  "action": "mark_read",  // mark_read|mark_unread|star|unstar|archive|delete|move
  "folder": "archive"     // 当action为move时必需
}

成功响应 (200)：
{
  "code": 200,
  "message": "批量操作成功",
  "data": {
    "success_count": 5,
    "failed_count": 0,
    "failed_ids": []
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## 3. 附件相关接口

### 3.1 上传附件
```
POST /api/v1/attachments/upload
Authorization: Bearer {access_token}
Content-Type: multipart/form-data

请求体：
- file: 文件数据
- filename: 文件名（可选，默认使用原文件名）

成功响应 (201)：
{
  "code": 201,
  "message": "文件上传成功",
  "data": {
    "id": 1,
    "filename": "document.pdf",
    "original_filename": "原始文件名.pdf",
    "size": 1024000,
    "content_type": "application/pdf",
    "upload_at": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}

错误响应 (413)：
{
  "code": 413,
  "message": "文件大小超出限制",
  "error": {
    "type": "VALIDATION_ERROR",
    "details": [
      {
        "field": "file",
        "message": "文件大小不能超过10MB"
      }
    ]
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 3.2 下载附件
```
GET /api/v1/attachments/{id}/download
Authorization: Bearer {access_token}

成功响应 (200)：
Content-Type: application/octet-stream
Content-Disposition: attachment; filename="document.pdf"

[文件二进制数据]
```

### 3.3 删除附件
```
DELETE /api/v1/attachments/{id}
Authorization: Bearer {access_token}

成功响应 (204)：
无响应体
```

## 4. 用户相关接口

### 4.1 获取用户信息
```
GET /api/v1/users/profile
Authorization: Bearer {access_token}

成功响应 (200)：
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "display_name": "张三",
    "avatar_url": "https://example.com/avatars/1.jpg",
    "signature": "邮件签名",
    "timezone": "Asia/Shanghai",
    "language": "zh-CN",
    "created_at": "2024-01-01T00:00:00Z",
    "last_login_at": "2024-01-15T10:00:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 4.2 更新用户信息
```
PUT /api/v1/users/profile
Authorization: Bearer {access_token}
Content-Type: application/json

请求体：
{
  "display_name": "新的显示名称",
  "signature": "新的邮件签名",
  "timezone": "Asia/Shanghai",
  "language": "zh-CN"
}

成功响应 (200)：
{
  "code": 200,
  "message": "更新成功",
  "data": {
    "id": 1,
    "display_name": "新的显示名称",
    "signature": "新的邮件签名",
    "updated_at": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 4.3 修改密码
```
POST /api/v1/users/change-password
Authorization: Bearer {access_token}
Content-Type: application/json

请求体：
{
  "current_password": "当前密码",
  "new_password": "新密码",
  "confirm_password": "确认新密码"
}

成功响应 (200)：
{
  "code": 200,
  "message": "密码修改成功",
  "data": null,
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## 5. 搜索相关接口

### 5.1 搜索邮件
```
GET /api/v1/emails/search?q=关键词&folder=inbox&page=1&per_page=20
Authorization: Bearer {access_token}

查询参数：
- q: 搜索关键词
- folder: 搜索范围文件夹（可选）
- from: 发件人筛选（可选）
- date_from: 开始日期（可选）
- date_to: 结束日期（可选）
- has_attachments: 是否有附件（可选）
- page: 页码
- per_page: 每页数量

成功响应 (200)：
{
  "code": 200,
  "message": "搜索成功",
  "data": {
    "items": [
      // 邮件列表，格式同获取邮件列表
    ],
    "pagination": {
      // 分页信息
    },
    "search_info": {
      "query": "关键词",
      "total_results": 25,
      "search_time": 0.15
    }
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## 6. 统计相关接口

### 6.1 获取邮件统计
```
GET /api/v1/emails/stats
Authorization: Bearer {access_token}

成功响应 (200)：
{
  "code": 200,
  "message": "查询成功",
  "data": {
    "total_emails": 1000,
    "unread_count": 25,
    "folder_counts": {
      "inbox": 100,
      "sent": 200,
      "drafts": 5,
      "trash": 50,
      "spam": 10,
      "archive": 635
    },
    "today_received": 15,
    "today_sent": 8
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## 7. 系统相关接口

### 7.1 健康检查
```
GET /api/v1/health

成功响应 (200)：
{
  "code": 200,
  "message": "系统正常",
  "data": {
    "status": "healthy",
    "version": "1.0.0",
    "timestamp": "2024-01-15T10:30:00Z"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

## 8. 错误响应示例

### 8.1 参数验证错误 (400)
```json
{
  "code": 400,
  "message": "请求参数错误",
  "error": {
    "type": "VALIDATION_ERROR",
    "details": [
      {
        "field": "email",
        "message": "邮箱格式不正确"
      },
      {
        "field": "subject",
        "message": "邮件主题不能为空"
      }
    ]
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 8.2 认证错误 (401)
```json
{
  "code": 401,
  "message": "认证失败",
  "error": {
    "type": "AUTHENTICATION_ERROR",
    "details": "Token已过期或无效"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 8.3 权限错误 (403)
```json
{
  "code": 403,
  "message": "权限不足",
  "error": {
    "type": "AUTHORIZATION_ERROR",
    "details": "您没有权限访问此邮件"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 8.4 资源不存在 (404)
```json
{
  "code": 404,
  "message": "资源不存在",
  "error": {
    "type": "NOT_FOUND_ERROR",
    "details": "邮件不存在或已被删除"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

### 8.5 服务器错误 (500)
```json
{
  "code": 500,
  "message": "服务器内部错误",
  "error": {
    "type": "SYSTEM_ERROR",
    "details": "系统繁忙，请稍后重试"
  },
  "timestamp": "2024-01-15T10:30:00Z"
}
```

---

## 使用说明

1. **所有需要认证的接口都必须在请求头中包含有效的Bearer Token**
2. **时间格式统一使用ISO 8601标准格式**
3. **分页参数page从1开始计数**
4. **文件上传大小限制为10MB**
5. **所有响应都包含统一的code、message、data、timestamp字段**
6. **错误响应包含详细的错误类型和描述信息**

这个API设计可以作为前后端开发的具体参考，确保双方对接口的理解完全一致。 