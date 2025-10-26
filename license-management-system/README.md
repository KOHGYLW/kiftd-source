# License Management System

## 项目概述

企业客户授权管理系统是一个基于Spring Boot 3.2的现代化企业级应用，用于管理企业客户的软件授权和许可证。

## 技术栈

- **后端框架**: Spring Boot 3.2.12
- **数据库**: PostgreSQL
- **缓存**: Redis
- **安全**: Spring Security + JWT
- **文档**: OpenAPI 3.0 (Swagger UI)
- **构建工具**: Maven
- **Java版本**: Java 17

## 项目结构

```
license-management-system/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── enterprise/
│   │   │           └── license/
│   │   │               ├── controller/     # 控制器层
│   │   │               ├── service/        # 业务逻辑层
│   │   │               ├── repository/     # 数据访问层
│   │   │               ├── entity/         # 实体类
│   │   │               ├── dto/            # 数据传输对象
│   │   │               ├── config/         # 配置类
│   │   │               ├── util/           # 工具类
│   │   │               ├── security/       # 安全相关
│   │   │               └── LicenseManagementSystemApplication.java
│   │   └── resources/
│   │       ├── application.yml             # 应用配置
│   │       ├── static/                     # 静态资源
│   │       └── templates/                  # 模板文件
│   └── test/
│       ├── java/                           # 测试代码
│       └── resources/                      # 测试资源
├── pom.xml                                 # Maven配置
└── README.md                               # 项目说明
```

## 主要功能

- 用户认证与授权
- 客户信息管理
- 许可证生成与管理
- 许可证状态跟踪
- API接口文档
- 系统监控与健康检查

## 快速开始

### 环境要求

- Java 17+
- Maven 3.6+
- PostgreSQL 12+
- Redis 6+

### 数据库配置

1. 创建PostgreSQL数据库：
```sql
CREATE DATABASE license_management;
CREATE USER license_user WITH PASSWORD 'license_password';
GRANT ALL PRIVILEGES ON DATABASE license_management TO license_user;
```

2. 修改 `application.yml` 中的数据库连接配置

### 运行应用

1. 克隆项目
2. 配置数据库连接
3. 启动Redis服务
4. 运行应用：
```bash
mvn spring-boot:run
```

### 访问应用

- 应用地址: http://localhost:8080/api
- API文档: http://localhost:8080/api/swagger-ui.html
- 健康检查: http://localhost:8080/api/actuator/health

## 配置说明

### 环境变量

| 变量名 | 描述 | 默认值 |
|--------|------|--------|
| DB_HOST | 数据库主机 | localhost |
| DB_PORT | 数据库端口 | 5432 |
| DB_NAME | 数据库名称 | license_management |
| DB_USERNAME | 数据库用户名 | license_user |
| DB_PASSWORD | 数据库密码 | license_password |
| REDIS_HOST | Redis主机 | localhost |
| REDIS_PORT | Redis端口 | 6379 |
| REDIS_PASSWORD | Redis密码 | - |
| JWT_SECRET | JWT密钥 | license-management-system-secret-key-2024 |
| JWT_EXPIRATION | JWT过期时间(ms) | 86400000 |

### 配置文件

项目支持多环境配置：
- `dev`: 开发环境
- `test`: 测试环境  
- `prod`: 生产环境

## 开发指南

### 代码规范

- 遵循Spring Boot最佳实践
- 使用Lombok减少样板代码
- 统一异常处理
- API响应格式标准化
- 完善的日志记录

### 安全考虑

- JWT token认证
- Spring Security权限控制
- SQL注入防护
- CORS跨域配置
- 敏感信息加密

## 部署说明

### Docker部署

项目支持Docker容器化部署，详见Dockerfile。

### 生产环境注意事项

1. 修改JWT密钥
2. 配置HTTPS
3. 设置合适的日志级别
4. 配置数据库连接池
5. 启用生产环境配置文件

## 许可证

本项目采用Apache 2.0许可证。

## 联系方式

- 项目团队: Enterprise Team
- 邮箱: support@enterprise.com