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

0 开始联调启动流程： 
1.git拉取新代码， 
2.配置后端运行环境、
3.前端运行环境、
4.后端开启数据库
5.后端启动服务
6.前端开启服务，浏览器就可以访问； 
7. 前端的接口通过http请求访问后端已经开启的服务，后端返回相应的数据或处理。

具体指令：
后端部分：
1.配置数据库：
下载MySQL
打开MySQL configuration进行数据库基本信息配置
下载MySQL workbench
新建数据库
将三个给定表项导入数据库

2.在后端写入数据库配置：application.properties
spring.datasource.url=jdbc:mysql://localhost:3306/mail?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
在3306/后面的数据库名改为自己建立的数据库名

spring.datasource.username=
数据库用户名，可以是root

spring.datasource.password=
数据库用户密码，自己定的

spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
基本不用改

一般数据库在第一次开启后自己开机的时候会打开，如果没打开就打开MySQL configuration开启

3.服务器开启
打开test.java就能动了

4.为了不影响每个人的application.properties配置，请修改后不要上传这个文件



前端部分： 
1. cd ./frontend 
2. pnpm install
3. pnpm run dev

