#!/bin/sh

# 启动脚本 - License Management System Backend

set -e

echo "Starting License Management System Backend..."

# 等待数据库启动
echo "Waiting for database..."
while ! nc -z ${DB_HOST:-postgres} ${DB_PORT:-5432}; do
  echo "Waiting for PostgreSQL to be ready..."
  sleep 2
done
echo "Database is ready!"

# 等待Redis启动
echo "Waiting for Redis..."
while ! nc -z ${REDIS_HOST:-redis} ${REDIS_PORT:-6379}; do
  echo "Waiting for Redis to be ready..."
  sleep 2
done
echo "Redis is ready!"

# 启动应用
echo "Starting application with Java options: $JAVA_OPTS"
exec java $JAVA_OPTS -jar app.jar