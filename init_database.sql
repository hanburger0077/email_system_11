-- 创建数据库初始化脚本

-- 1. 创建数据库
CREATE DATABASE IF NOT EXISTS mail CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. 使用数据库
USE mail;

-- 3. 创建用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 4. 创建用户 MengZhi（如果不存在）
CREATE USER IF NOT EXISTS 'MengZhi'@'localhost' IDENTIFIED BY '20040707';

-- 5. 授权用户访问 mail 数据库
GRANT ALL PRIVILEGES ON mail.* TO 'MengZhi'@'localhost';

-- 6. 刷新权限
FLUSH PRIVILEGES;

-- 7. 插入测试数据（可选）
INSERT IGNORE INTO users (username, password, email, phone) VALUES 
('testuser', '123456', '1234567@hh.com', '13800138000'),
('admin', 'admin123', '12345678@hh.com', '13900139000');

-- 显示创建的表
SHOW TABLES;

-- 显示用户表结构
DESCRIBE users;

-- 显示插入的数据
SELECT * FROM users; 