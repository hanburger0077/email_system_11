# 邮件系统项目启动指南

## 🔧 环境要求
- Node.js (推荐 18+)
- pnpm
- Java JDK 17+
- Maven 3.6+

## 📂 项目结构
```
email_system_11/
├── frontend/          # Vue.js 前端
├── backend/           # Spring Boot 后端
└── data/             # 数据库文件存储
```

## 🚀 启动步骤

### 1. 安装依赖

#### 前端依赖
```bash
cd frontend
pnpm install
```

#### 后端依赖
```bash
cd backend
mvn clean compile
```

### 2. 启动服务

#### 方式一：分别启动（推荐）

**终端1 - 启动后端：**
```bash
cd backend
mvn spring-boot:run
```
等待看到：`Started Test in X.XXX seconds`

**终端2 - 启动前端：**
```bash
cd frontend
pnpm run dev
```
等待看到：`Local: http://localhost:5175/`

#### 方式二：后台启动
```bash
# 后台启动后端
cd backend && mvn spring-boot:run &

# 启动前端
cd frontend && pnpm run dev
```

### 3. 访问应用
- 前端页面：http://localhost:5175
- 后端API：http://localhost:8080
- H2数据库控制台：http://localhost:8080/h2-console

## ✅ 验证联调成功
1. 前端页面能正常打开
2. 可以注册新用户
3. 可以登录系统
4. 浏览器Network面板能看到API请求成功

## ❌ 常见问题
1. **端口被占用**：确保8080和5175端口未被占用
2. **依赖安装失败**：检查网络连接，尝试切换npm源
3. **Java版本问题**：确保使用JDK 17+
4. **Maven编译失败**：检查是否有缺失的类文件

## 🔧 故障排查
```bash
# 检查端口占用
lsof -i :8080
lsof -i :5175

# 强制停止进程
pkill -f "spring-boot:run"
pkill -f "vite"
``` 