#!/bin/bash

echo "🚀 MySQL数据库设置脚本"
echo "========================"

# 检查MySQL是否安装
if ! command -v mysql &> /dev/null; then
    echo "❌ MySQL未安装，请先安装MySQL"
    exit 1
fi

echo "✅ MySQL已安装"

# 尝试启动MySQL服务
echo "📡 尝试启动MySQL服务..."

# 检查不同的MySQL启动方式
if command -v brew &> /dev/null; then
    echo "🍺 使用Homebrew启动MySQL..."
    brew services start mysql
elif command -v systemctl &> /dev/null; then
    echo "🐧 使用systemctl启动MySQL..."
    sudo systemctl start mysql
else
    echo "🔧 手动启动MySQL..."
    sudo mysqld_safe --user=mysql &
fi

sleep 3

echo ""
echo "🗄️  现在需要执行数据库初始化脚本"
echo "请按照以下步骤操作："
echo ""
echo "1️⃣  以root用户连接MySQL："
echo "   mysql -u root -p"
echo ""
echo "2️⃣  在MySQL中执行以下命令："
echo "   source $(pwd)/init_database.sql"
echo ""
echo "3️⃣  或者直接执行："
echo "   mysql -u root -p < init_database.sql"
echo ""
echo "4️⃣  如果忘记root密码，可以尝试重置："
echo "   sudo mysql_secure_installation"
echo ""
echo "📁 数据库脚本位置: $(pwd)/init_database.sql"
echo ""
echo "🔗 完成后，重启Spring Boot应用即可测试API" 