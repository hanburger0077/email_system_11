# email_system_11

## 1. API接口规范协议

### 1.1 前端请求格式：

API URL：{protocol}://{domain}:{port}/api/{resource}

http://localhost:8080/api/user/login


HTTP 方法：

请求参数：
1. post请求：
参数放置在请求体，请求体格式为：Content-Type: application/json。

1. get请求：
参数放在query中。

### 1.2 状态码前后端约定
```
成功响应：
- code.ok OK: 请求成功

其余状态码为失败 
笼统失败可用 'code.error', 

场景细分：
客户端错误：
- 400 Bad Request: 请求参数错误
- 401 Unauthorized: 未认证
- 403 Forbidden: 无权限
- 404 Not Found: 资源不存在
- 422 Unprocessable Entity: 请求格式正确但语义错误

服务器错误：
- 500 Internal Server Error: 服务器内部错误
- 502 Bad Gateway: 网关错误
- 503 Service Unavailable: 服务不可用
```

### 1.3 后端相应格式：

#### 1.3.1 成功响应格式
```json
{
  "code": "code.ok",
  "message": "操作成功",
  "data": {
    // 具体数据内容
  },
}
```

#### 1.2.2 错误响应格式
```json
{
  "code": "code.error",
  "message": "操作错误",
  "reason": "错误具体原因XXXX"
}
```

#### 1.2.3 分页响应格式
```json
{
  "code": "code.ok",
  "message": "查询成功",
  "data": {
    "items": [
      // 数据列表
    ],
    // 分页信息：供参考
    "pagination": {
      "current_page": 1,
      "per_page": 20,
      "total": 100,
      "total_pages": 5,
      "has_next": true,
      "has_prev": false
    }
  },
}
```


## 2. 前后端联调说明：

### 后端技术栈
- **框架**: Spring Boot 3.5.0
- **数据库**: MySQL
- **ORM**: MyBatis Plus 3.5.5
- **安全**: Spring Security
- **邮件**: Jakarta Mail + Spring Boot Mail
- **网络**: Netty 4.2.1
- **端口**: 8080

### 前端技术栈
- **框架**: Vue 3.5.13
- **UI库**: Element Plus 2.9.11
- **状态管理**: Pinia 3.0.1
- **路由**: Vue Router 4.5.0
- **构建工具**: Vite 6.2.4
- **包管理**: pnpm

### 运行前环境要求
- 后端： java > 17.0+ 、mysql、  spring-boot、  maven

- 前端： node > 18、 pnpm

**联调流程**：

 启动数据库->数据库初始化->启动spring-boot服务器->启动前端

具体指令：
后端部分：

前端部分： 
1. cd ./frontend 
2. pnpm install
3. pnpm run dev

