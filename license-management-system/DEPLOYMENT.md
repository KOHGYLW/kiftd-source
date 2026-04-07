# 企业授权管理系统部署指南

## 系统概述

企业授权管理系统是一个基于Spring Boot和Vue.js的现代化企业级应用，提供完整的软件授权管理解决方案。

### 技术栈
- **后端**: Spring Boot 3.2.12 + PostgreSQL + Redis
- **前端**: Vue 3 + Element Plus + TypeScript
- **部署**: Docker + Docker Compose
- **监控**: Prometheus + Grafana
- **日志**: ELK Stack（可选）

## 环境要求

### 系统要求
- **操作系统**: Linux (推荐 Ubuntu 20.04+ / CentOS 8+)
- **CPU**: 4核心以上
- **内存**: 8GB以上（推荐16GB）
- **存储**: 100GB以上SSD
- **网络**: 公网IP（如需外部访问）

### 软件依赖
- Docker 20.10+
- Docker Compose 2.0+
- Git

## 快速部署

### 1. 克隆项目
```bash
# 克隆后端项目
git clone <backend-repository-url>
cd license-management-system

# 克隆前端项目（如果分离部署）
git clone <frontend-repository-url> ../license-management-frontend
```

### 2. 环境配置
创建环境变量文件：
```bash
cp .env.example .env
```

编辑 `.env` 文件：
```bash
# 数据库配置
DB_PASSWORD=your_secure_db_password_2024!

# Redis配置
REDIS_PASSWORD=your_secure_redis_password_2024!

# JWT配置
JWT_SECRET=your_jwt_secret_key_here_very_long_and_secure_2024

# Grafana配置（如果启用监控）
GRAFANA_PASSWORD=your_grafana_admin_password

# 加密配置
RSA_KEY_LENGTH=4096
AES_KEY_LENGTH=256

# 密钥轮换配置
KEY_ROTATION_INTERVAL=24
MAX_KEY_AGE=168

# 硬件指纹配置
HARDWARE_ONLINE_THRESHOLD=0.85
HARDWARE_OFFLINE_THRESHOLD=0.70
```

### 3. 启动服务
```bash
# 启动基础服务（数据库、缓存、应用）
docker-compose up -d

# 启动包含监控的完整服务
docker-compose --profile monitoring up -d

# 启动包含日志收集的完整服务
docker-compose --profile logging up -d

# 启动所有服务
docker-compose --profile monitoring --profile logging up -d
```

### 4. 验证部署
```bash
# 检查服务状态
docker-compose ps

# 查看应用日志
docker-compose logs -f license-backend

# 健康检查
curl http://localhost:8080/api/actuator/health
```

## 详细部署指南

### 数据库配置

#### PostgreSQL设置
```bash
# 进入数据库容器
docker-compose exec postgres psql -U license_user -d license_management

# 查看数据库状态
\l
\dt

# 检查初始数据
SELECT * FROM users;
SELECT * FROM system_configs;
```

#### 数据库备份与恢复
```bash
# 备份数据库
docker-compose exec postgres pg_dump -U license_user license_management > backup.sql

# 恢复数据库
docker-compose exec -T postgres psql -U license_user license_management < backup.sql
```

### Redis配置

#### 连接测试
```bash
# 进入Redis容器
docker-compose exec redis redis-cli -a your_redis_password

# 测试连接
ping
info
```

#### 缓存监控
```bash
# 查看缓存统计
docker-compose exec redis redis-cli -a your_redis_password info stats

# 查看内存使用
docker-compose exec redis redis-cli -a your_redis_password info memory
```

### 应用配置

#### 环境变量
| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `SPRING_PROFILES_ACTIVE` | Spring激活配置 | `prod` |
| `DB_HOST` | 数据库主机 | `postgres` |
| `DB_PORT` | 数据库端口 | `5432` |
| `REDIS_HOST` | Redis主机 | `redis` |
| `JWT_SECRET` | JWT密钥 | - |
| `RSA_KEY_LENGTH` | RSA密钥长度 | `4096` |

#### 日志配置
日志文件位置：
- 应用日志：`/var/log/license-management-system/application.log`
- 错误日志：`/var/log/license-management-system/error.log`
- 访问日志：`/var/log/license-management-system/access.log`
- 审计日志：`/var/log/license-management-system/audit.log`

### 前端配置

#### Nginx配置
```nginx
# /etc/nginx/conf.d/default.conf
server {
    listen 80;
    server_name your-domain.com;
    
    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    
    # API代理
    location /api/ {
        proxy_pass http://license-backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## 监控配置

### Prometheus配置
```yaml
# monitoring/prometheus.yml
global:
  scrape_interval: 30s

scrape_configs:
  - job_name: 'license-backend'
    static_configs:
      - targets: ['license-backend:8080']
    metrics_path: '/api/actuator/prometheus'
```

### Grafana仪表板
访问 `http://localhost:3000`
- 用户名：`admin`
- 密码：环境变量中设置的 `GRAFANA_PASSWORD`

预配置仪表板：
- 应用性能监控
- 数据库监控
- Redis监控
- 系统资源监控

## 安全配置

### SSL/TLS配置
```bash
# 生成自签名证书（仅用于测试）
openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout ./ssl/private.key \
  -out ./ssl/certificate.crt

# 配置Nginx支持HTTPS
# 编辑 config/default.conf 添加SSL配置
```

### 防火墙配置
```bash
# 开放必要端口
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 22/tcp
sudo ufw enable

# 限制数据库和Redis访问（仅容器内部）
# 默认配置已经通过Docker网络隔离
```

### 密钥管理
系统自动管理RSA密钥轮换：
- 默认24小时轮换一次
- 旧密钥保留7天用于验证
- 密钥存储在数据库中并加密

## 性能优化

### JVM调优
```bash
# 编辑docker-compose.yml中的JAVA_OPTS
JAVA_OPTS: >-
  -Xms2g -Xmx4g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+UnlockExperimentalVMOptions
  -XX:+UseJVMCICompiler
```

### 数据库优化
```sql
-- 调整PostgreSQL配置
ALTER SYSTEM SET shared_buffers = '256MB';
ALTER SYSTEM SET effective_cache_size = '1024MB';
ALTER SYSTEM SET maintenance_work_mem = '64MB';
ALTER SYSTEM SET checkpoint_completion_target = 0.9;
SELECT pg_reload_conf();
```

### Redis优化
```bash
# 编辑Redis配置
maxmemory 512mb
maxmemory-policy allkeys-lru
save 900 1
save 300 10
save 60 10000
```

## 运维指南

### 日常维护

#### 日志轮转
```bash
# 查看日志大小
docker-compose exec license-backend du -sh /var/log/license-management-system/

# 清理旧日志（已配置自动轮转）
docker-compose exec license-backend find /var/log/license-management-system/ -name "*.gz" -mtime +30 -delete
```

#### 数据库维护
```bash
# 定期清理过期数据
docker-compose exec postgres psql -U license_user -d license_management -c "SELECT cleanup_expired_data();"

# 分析表统计信息
docker-compose exec postgres psql -U license_user -d license_management -c "ANALYZE;"

# 重建索引
docker-compose exec postgres psql -U license_user -d license_management -c "REINDEX DATABASE license_management;"
```

#### 备份策略
```bash
#!/bin/bash
# backup.sh - 自动备份脚本
DATE=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/backup"

# 数据库备份
docker-compose exec postgres pg_dump -U license_user license_management | gzip > "$BACKUP_DIR/db_backup_$DATE.sql.gz"

# 应用数据备份
docker cp license-backend:/app/data "$BACKUP_DIR/app_data_$DATE"

# 清理30天前的备份
find "$BACKUP_DIR" -name "*.gz" -mtime +30 -delete
find "$BACKUP_DIR" -name "app_data_*" -mtime +30 -exec rm -rf {} \;
```

### 故障排除

#### 常见问题

1. **应用启动失败**
```bash
# 查看详细日志
docker-compose logs license-backend

# 检查数据库连接
docker-compose exec license-backend curl -f http://localhost:8080/api/actuator/health
```

2. **数据库连接问题**
```bash
# 检查数据库状态
docker-compose exec postgres pg_isready -U license_user

# 查看连接数
docker-compose exec postgres psql -U license_user -c "SELECT count(*) FROM pg_stat_activity;"
```

3. **Redis连接问题**
```bash
# 测试Redis连接
docker-compose exec redis redis-cli -a your_password ping

# 查看Redis状态
docker-compose exec redis redis-cli -a your_password info server
```

4. **前端无法访问**
```bash
# 检查Nginx状态
docker-compose exec license-frontend nginx -t

# 重启Nginx
docker-compose restart license-frontend
```

#### 性能问题诊断
```bash
# 查看容器资源使用
docker stats

# 查看应用JVM状态
docker-compose exec license-backend jstat -gc 1

# 查看数据库性能
docker-compose exec postgres psql -U license_user -c "SELECT * FROM pg_stat_activity WHERE state = 'active';"
```

### 更新升级

#### 应用更新
```bash
# 拉取最新代码
git pull origin main

# 重新构建镜像
docker-compose build license-backend

# 滚动更新（零停机）
docker-compose up -d --no-deps license-backend
```

#### 数据库升级
```bash
# 备份数据库
docker-compose exec postgres pg_dump -U license_user license_management > backup_before_upgrade.sql

# 应用数据库迁移
docker-compose exec license-backend java -jar app.jar --spring.profiles.active=prod --flyway.migrate
```

## 监控告警

### 关键指标监控
- 应用响应时间 < 500ms
- 数据库连接池使用率 < 80%
- Redis内存使用率 < 80%
- 磁盘使用率 < 85%
- CPU使用率 < 80%

### 告警规则
```yaml
# alerts.yml
groups:
  - name: license-system
    rules:
      - alert: ApplicationDown
        expr: up{job="license-backend"} == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "授权管理系统不可用"
          
      - alert: DatabaseConnectionHigh
        expr: pg_stat_activity_count > 50
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "数据库连接数过高"
```

## 技术支持

### 联系信息
- 技术文档：[系统文档链接]
- 问题反馈：[GitHub Issues链接]
- 技术支持：support@enterprise.com

### 开发调试
```bash
# 开发环境启动
docker-compose -f docker-compose.dev.yml up -d

# 查看实时日志
docker-compose logs -f license-backend

# 进入容器调试
docker-compose exec license-backend bash
```

---

**注意事项**：
1. 首次部署后请立即修改默认密码
2. 定期更新系统和依赖组件
3. 监控系统资源使用情况
4. 定期备份重要数据
5. 遵循安全最佳实践