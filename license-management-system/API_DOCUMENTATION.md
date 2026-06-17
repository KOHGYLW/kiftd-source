# 企业授权管理系统API文档

## 系统概述

企业授权管理系统提供完整的RESTful API接口，支持客户管理、许可证管理、许可证验证等核心功能。所有API遵循REST设计原则，使用JSON格式进行数据交换。

## 基础信息

- **Base URL**: `http://your-domain.com/api`
- **API版本**: v1.0
- **认证方式**: Bearer Token (JWT)
- **内容类型**: `application/json`
- **字符编码**: UTF-8

## 通用响应格式

### 成功响应
```json
{
  "success": true,
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": "2024-07-30T10:30:00Z"
}
```

### 错误响应
```json
{
  "success": false,
  "code": 400,
  "message": "请求参数错误",
  "errors": [
    {
      "field": "customerCode",
      "message": "客户编码不能为空"
    }
  ],
  "timestamp": "2024-07-30T10:30:00Z"
}
```

### 分页响应
```json
{
  "success": true,
  "code": 200,
  "message": "查询成功",
  "data": {
    "content": [],
    "totalElements": 100,
    "totalPages": 10,
    "size": 10,
    "number": 0,
    "first": true,
    "last": false
  },
  "timestamp": "2024-07-30T10:30:00Z"
}
```

## 状态码说明

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 429 | 请求过于频繁 |
| 500 | 服务器内部错误 |

## 认证接口

### 用户登录
```http
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

**响应示例**:
```json
{
  "success": true,
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 86400,
    "username": "admin",
    "roles": ["ADMIN"]
  }
}
```

### 刷新令牌
```http
POST /auth/refresh
Content-Type: application/json
Authorization: Bearer {refreshToken}

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

### 用户登出
```http
POST /auth/logout
Authorization: Bearer {accessToken}
```

### 修改密码
```http
POST /auth/change-password
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "oldPassword": "oldPassword123",
  "newPassword": "newPassword456",
  "confirmPassword": "newPassword456"
}
```

### 获取用户信息
```http
GET /auth/user
Authorization: Bearer {accessToken}
```

## 客户管理接口

### 创建客户
```http
POST /customers
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "customerCode": "CUST001",
  "customerName": "测试客户",
  "contactPerson": "张三",
  "contactPhone": "13800138001",
  "contactEmail": "zhangsan@test.com",
  "address": "北京市朝阳区",
  "description": "客户描述信息"
}
```

### 查询客户列表
```http
GET /customers?page=0&size=10&keyword=测试&status=ACTIVE
Authorization: Bearer {accessToken}
```

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | int | 否 | 页码，从0开始 |
| size | int | 否 | 每页大小，默认10 |
| keyword | string | 否 | 搜索关键词 |
| status | string | 否 | 客户状态：ACTIVE/INACTIVE |
| sort | string | 否 | 排序字段，如：createdAt,desc |

### 获取客户详情
```http
GET /customers/{id}
Authorization: Bearer {accessToken}
```

### 更新客户信息
```http
PUT /customers/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "customerName": "更新后的客户名",
  "contactPerson": "李四",
  "status": "ACTIVE"
}
```

### 删除客户
```http
DELETE /customers/{id}
Authorization: Bearer {accessToken}
```

### 批量导入客户
```http
POST /customers/batch-import
Authorization: Bearer {accessToken}
Content-Type: application/json

[
  {
    "customerCode": "BATCH001",
    "customerName": "批量导入客户1",
    "contactPerson": "王五"
  },
  {
    "customerCode": "BATCH002",
    "customerName": "批量导入客户2",
    "contactPerson": "赵六"
  }
]
```

### 客户统计信息
```http
GET /customers/statistics
Authorization: Bearer {accessToken}
```

### 导出客户数据
```http
GET /customers/export?format=excel&status=ACTIVE
Authorization: Bearer {accessToken}
```

## 许可证管理接口

### 创建许可证
```http
POST /licenses
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "licenseCode": "LIC001",
  "customerId": 1,
  "licenseType": "STANDARD",
  "productName": "测试产品",
  "productVersion": "1.0.0",
  "maxUsers": 100,
  "expiresAt": "2025-12-31T23:59:59",
  "description": "许可证描述"
}
```

**许可证类型**:
- `TRIAL`: 试用版
- `STANDARD`: 标准版
- `ENTERPRISE`: 企业版
- `UNLIMITED`: 无限制版

### 查询许可证列表
```http
GET /licenses?page=0&size=10&customerId=1&status=ACTIVE&licenseType=STANDARD
Authorization: Bearer {accessToken}
```

### 获取许可证详情
```http
GET /licenses/{id}
Authorization: Bearer {accessToken}
```

### 更新许可证
```http
PUT /licenses/{id}
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "maxUsers": 200,
  "expiresAt": "2026-12-31T23:59:59",
  "description": "更新后的描述"
}
```

### 激活许可证
```http
POST /licenses/{id}/activate
Authorization: Bearer {accessToken}
```

### 停用许可证
```http
POST /licenses/{id}/deactivate
Authorization: Bearer {accessToken}
```

### 延期许可证
```http
POST /licenses/{id}/extend
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "expiresAt": "2026-12-31T23:59:59",
  "reason": "客户续费"
}
```

### 生成许可证密钥
```http
POST /licenses/{id}/generate-key
Authorization: Bearer {accessToken}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "licenseKey": "encrypted_license_key_data",
    "signature": "digital_signature",
    "publicKey": "public_key_for_verification",
    "keyVersion": 1,
    "generatedAt": "2024-07-30T10:30:00Z"
  }
}
```

### 许可证统计信息
```http
GET /licenses/statistics
Authorization: Bearer {accessToken}
```

### 即将过期的许可证
```http
GET /licenses/expiring?days=30
Authorization: Bearer {accessToken}
```

## 许可证验证接口

### 在线验证许可证
```http
POST /licenses/validate
Content-Type: application/json

{
  "licenseCode": "LIC001",
  "hardwareFingerprint": "MAC:AA:BB:CC:DD:EE:FF|CPU:Intel|DISK:12345",
  "productName": "测试产品",
  "productVersion": "1.0.0",
  "clientInfo": {
    "osName": "Windows 10",
    "osVersion": "10.0.19042",
    "javaVersion": "17.0.2"
  }
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "valid": true,
    "licenseCode": "LIC001",
    "customerName": "测试客户",
    "productName": "测试产品",
    "licenseType": "STANDARD",
    "maxUsers": 100,
    "currentUsers": 25,
    "issuedAt": "2024-01-01T00:00:00Z",
    "expiresAt": "2024-12-31T23:59:59Z",
    "remainingDays": 150,
    "features": [
      "BASIC_FEATURES",
      "ADVANCED_REPORTS"
    ],
    "message": "许可证验证成功"
  }
}
```

### 离线验证许可证
```http
POST /licenses/validate-offline
Content-Type: application/json

{
  "licenseKey": "encrypted_license_key_data",
  "signature": "digital_signature",
  "hardwareFingerprint": "MAC:AA:BB:CC:DD:EE:FF|CPU:Intel|DISK:12345"
}
```

### 获取硬件指纹
```http
GET /licenses/hardware-fingerprint
```

### 验证历史记录
```http
GET /licenses/validation-logs?page=0&size=10&licenseCode=LIC001&startDate=2024-01-01&endDate=2024-12-31
Authorization: Bearer {accessToken}
```

## 仪表板接口

### 系统概览
```http
GET /dashboard/overview
Authorization: Bearer {accessToken}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "customerCount": 150,
    "licenseCount": 320,
    "activeLicenseCount": 280,
    "expiredLicenseCount": 25,
    "expiringLicenseCount": 15,
    "validationCount": 12500,
    "successValidationRate": 98.5,
    "revenue": 1250000.00,
    "trends": {
      "customerGrowth": 15.2,
      "licenseGrowth": 22.8,
      "validationGrowth": 8.9
    }
  }
}
```

### 许可证使用趋势
```http
GET /dashboard/license-trends?period=30
Authorization: Bearer {accessToken}
```

### 客户统计数据
```http
GET /dashboard/customer-stats
Authorization: Bearer {accessToken}
```

### 验证统计数据
```http
GET /dashboard/validation-stats?period=7
Authorization: Bearer {accessToken}
```

### 系统告警
```http
GET /dashboard/alerts
Authorization: Bearer {accessToken}
```

### 活动记录
```http
GET /dashboard/activities?page=0&size=20
Authorization: Bearer {accessToken}
```

## 系统管理接口

### 获取系统配置
```http
GET /system/configs
Authorization: Bearer {accessToken}
```

### 更新系统配置
```http
PUT /system/configs
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "license.default_expiry_days": "365",
  "security.password_min_length": "8",
  "notification.email_enabled": "true"
}
```

### 系统健康检查
```http
GET /actuator/health
```

### 系统信息
```http
GET /actuator/info
Authorization: Bearer {accessToken}
```

### 应用指标
```http
GET /actuator/metrics
Authorization: Bearer {accessToken}
```

## 错误处理

### 业务错误码

| 错误码 | 说明 |
|--------|------|
| 1001 | 客户编码已存在 |
| 1002 | 客户不存在 |
| 1003 | 客户状态无效 |
| 2001 | 许可证编码已存在 |
| 2002 | 许可证不存在 |
| 2003 | 许可证已过期 |
| 2004 | 许可证已达到最大用户数 |
| 3001 | 许可证验证失败 |
| 3002 | 硬件指纹不匹配 |
| 3003 | 产品信息不匹配 |
| 4001 | 用户名或密码错误 |
| 4002 | 令牌已过期 |
| 4003 | 权限不足 |

### 错误响应示例
```json
{
  "success": false,
  "code": 1001,
  "message": "客户编码已存在",
  "data": null,
  "errors": [
    {
      "field": "customerCode",
      "message": "客户编码 'CUST001' 已存在，请使用其他编码"
    }
  ],
  "timestamp": "2024-07-30T10:30:00Z"
}
```

## SDK示例

### Java SDK
```java
// Maven依赖
<dependency>
    <groupId>com.enterprise.license</groupId>
    <artifactId>license-client-java</artifactId>
    <version>1.0.0</version>
</dependency>

// 使用示例
LicenseClient client = new LicenseClient("http://your-domain.com/api");

// 在线验证
ValidationRequest request = ValidationRequest.builder()
    .licenseCode("LIC001")
    .hardwareFingerprint(HardwareUtils.generateFingerprint())
    .productName("Your Product")
    .productVersion("1.0.0")
    .build();

ValidationResponse response = client.validateLicense(request);
if (response.isValid()) {
    // 许可证有效，继续执行
    System.out.println("License is valid, remaining days: " + response.getRemainingDays());
} else {
    // 许可证无效，处理错误
    System.err.println("License validation failed: " + response.getMessage());
}
```

### C# SDK
```csharp
// NuGet包
Install-Package Enterprise.License.Client

// 使用示例
var client = new LicenseClient("http://your-domain.com/api");

var request = new ValidationRequest
{
    LicenseCode = "LIC001",
    HardwareFingerprint = HardwareUtils.GenerateFingerprint(),
    ProductName = "Your Product",
    ProductVersion = "1.0.0"
};

var response = await client.ValidateLicenseAsync(request);
if (response.Valid)
{
    Console.WriteLine($"License is valid, remaining days: {response.RemainingDays}");
}
else
{
    Console.WriteLine($"License validation failed: {response.Message}");
}
```

### Python SDK
```python
# pip install enterprise-license-client

from enterprise_license import LicenseClient, ValidationRequest
import platform

client = LicenseClient("http://your-domain.com/api")

request = ValidationRequest(
    license_code="LIC001",
    hardware_fingerprint=get_hardware_fingerprint(),
    product_name="Your Product",
    product_version="1.0.0"
)

response = client.validate_license(request)
if response.valid:
    print(f"License is valid, remaining days: {response.remaining_days}")
else:
    print(f"License validation failed: {response.message}")
```

## 测试工具

### Postman集合
导入提供的Postman集合文件进行API测试：
```bash
# 下载Postman集合
curl -O http://your-domain.com/docs/license-management-api.postman_collection.json

# 导入环境变量
curl -O http://your-domain.com/docs/license-management-api.postman_environment.json
```

### cURL示例
```bash
# 登录获取token
TOKEN=$(curl -X POST http://your-domain.com/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' \
  | jq -r '.data.accessToken')

# 创建客户
curl -X POST http://your-domain.com/api/customers \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "customerCode": "API001",
    "customerName": "API测试客户",
    "contactPerson": "测试人员",
    "contactPhone": "13800138000",
    "contactEmail": "test@example.com"
  }'

# 验证许可证
curl -X POST http://your-domain.com/api/licenses/validate \
  -H "Content-Type: application/json" \
  -d '{
    "licenseCode": "LIC001",
    "hardwareFingerprint": "MAC:AA:BB:CC:DD:EE:FF|CPU:Intel|DISK:12345",
    "productName": "测试产品",
    "productVersion": "1.0.0"
  }'
```

## 限流和配额

### API限流策略
- 登录接口：每IP每分钟最多10次请求
- 许可证验证：每许可证每分钟最多100次请求
- 其他接口：每用户每秒最多20次请求

### 响应头
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1627747200
```

## 版本控制

### API版本
- 当前版本：v1.0
- 版本策略：向后兼容
- 废弃通知：至少提前6个月

### 更新日志
- v1.0.0 (2024-07-30): 初始版本发布
- 详细更新日志请查看：[CHANGELOG.md]

---

**注意事项**：
1. 所有API调用都应使用HTTPS
2. 敏感信息不应在URL中传递
3. 建议实现客户端重试机制
4. 定期更新访问令牌
5. 遵循API调用频率限制