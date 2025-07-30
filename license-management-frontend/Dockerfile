# 多阶段构建 - 构建阶段
FROM node:18-alpine AS builder

# 设置工作目录
WORKDIR /app

# 设置npm镜像源（加速构建）
RUN npm config set registry https://registry.npmmirror.com/

# 复制package文件
COPY package*.json ./

# 安装依赖
RUN npm ci --only=production --silent

# 复制源代码
COPY . .

# 构建应用
RUN npm run build

# 生产运行阶段 - 使用Nginx
FROM nginx:1.25-alpine

# 设置维护者信息
LABEL maintainer="Enterprise DevOps Team <devops@enterprise.com>"
LABEL description="License Management System Frontend"
LABEL version="1.0.0"

# 安装必要工具
RUN apk add --no-cache \
    curl \
    tzdata \
    && rm -rf /var/cache/apk/*

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 删除默认nginx配置
RUN rm /etc/nginx/conf.d/default.conf

# 复制自定义nginx配置
COPY docker/nginx.conf /etc/nginx/nginx.conf
COPY docker/default.conf /etc/nginx/conf.d/default.conf

# 从构建阶段复制构建结果
COPY --from=builder /app/dist /usr/share/nginx/html

# 创建nginx用户目录
RUN mkdir -p /var/cache/nginx/client_temp \
    && mkdir -p /var/cache/nginx/proxy_temp \
    && mkdir -p /var/cache/nginx/fastcgi_temp \
    && mkdir -p /var/cache/nginx/uwsgi_temp \
    && mkdir -p /var/cache/nginx/scgi_temp \
    && chown -R nginx:nginx /var/cache/nginx \
    && chown -R nginx:nginx /usr/share/nginx/html \
    && chmod -R 755 /usr/share/nginx/html

# 复制启动脚本
COPY docker/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:80/ || exit 1

# 暴露端口
EXPOSE 80

# 启动nginx
ENTRYPOINT ["/entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]