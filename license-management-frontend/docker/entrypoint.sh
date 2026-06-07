#!/bin/sh

# 前端启动脚本

set -e

echo "Starting License Management System Frontend..."

# 替换环境变量（如果需要）
if [ -n "$API_BASE_URL" ]; then
    echo "Setting API base URL to: $API_BASE_URL"
    find /usr/share/nginx/html -name "*.js" -exec sed -i "s|__API_BASE_URL__|$API_BASE_URL|g" {} \;
fi

echo "Frontend is ready!"

# 执行传入的命令
exec "$@"