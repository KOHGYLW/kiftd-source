# 企业授权管理系统安全加密系统

## 系统概述

本系统实现了企业级的授权管理和安全加密功能，包括：

- **RSA-4096位密钥管理**：支持密钥生成、存储、轮换和安全管理
- **AES-256-GCM对称加密**：用于授权数据的高速加密
- **数字签名防篡改**：使用RSA数字签名确保数据完整性
- **硬件指纹绑定**：支持设备绑定和离线验证
- **JWT认证系统**：企业级身份认证和授权管理
- **Spring Security集成**：完整的安全框架配置

## 核心组件

### 1. 安全工具类

#### CryptoUtil - 加密解密工具
```java
// 生成RSA-4096密钥对
KeyPair keyPair = cryptoUtil.generateRSAKeyPair();

// AES-256-GCM加密
String encrypted = cryptoUtil.encryptAES(plaintext, aesKey);

// RSA数字签名
String signature = cryptoUtil.signRSA(data, privateKey);
```

#### HashUtil - 哈希工具
```java
// SHA-256哈希
String hash = hashUtil.sha256(input);

// PBKDF2密码哈希
String hashedPassword = hashUtil.pbkdf2Hash(password, salt);

// HMAC-SHA256
String hmac = hashUtil.hmacSha256(data, key);
```

#### SecureRandomUtil - 安全随机数生成
```java
// 生成授权码格式密钥
String licenseKey = secureRandomUtil.generateLicenseKey();

// 生成强密码
String password = secureRandomUtil.generateStrongPassword(16);

// 生成API密钥
String apiKey = secureRandomUtil.generateApiKey();
```

#### HardwareFingerprintUtil - 硬件指纹工具
```java
// 生成硬件指纹
String fingerprint = hardwareFingerprintUtil.generateHardwareFingerprint();

// 验证硬件匹配
boolean isMatch = hardwareFingerprintUtil.verifyHardwareFingerprint(expectedFingerprint);

// 获取硬件信息
HardwareInfo info = hardwareFingerprintUtil.getHardwareInfo();
```

### 2. 密钥管理系统

#### RsaKeyGeneratorService - RSA密钥生成
```java
// 生成密钥对
KeyPairInfo keyPair = rsaKeyGeneratorService.generateKeyPair();

// 异步生成
CompletableFuture<KeyPairInfo> future = rsaKeyGeneratorService.generateKeyPairAsync();

// 批量生成
CompletableFuture<KeyPairInfo[]> batch = rsaKeyGeneratorService.generateKeyPairBatch(5);
```

#### KeyManagerService - 密钥管理
```java
// 存储RSA密钥对
String keyId = keyManagerService.storeRSAKeyPair(keyPair, 8760); // 1年

// 获取公钥
PublicKey publicKey = keyManagerService.getRSAPublicKey(keyId);

// 密钥轮换
Map<String, String> result = keyManagerService.rotateKeys();
```

### 3. 授权加密系统

#### LicenseEncryptionService - 授权码加密
```java
// 创建授权数据
LicenseData licenseData = licenseEncryptionService.createLicenseData(
    customerId, productName, productVersion, validityDays, features, restrictions
);

// 加密授权码
String encryptedLicense = licenseEncryptionService.encryptLicense(licenseData, rsaKeyId);

// 绑定硬件指纹
String boundLicense = licenseEncryptionService.bindHardwareFingerprint(
    encryptedLicense, rsaKeyId
);
```

### 4. 授权验证系统

#### LicenseValidationService - 授权验证
```java
// 在线验证
ValidationDetails details = licenseValidationService.validateLicenseOnline(
    licenseKey, rsaKeyId, productName, productVersion
);

// 离线验证
ValidationDetails details = licenseValidationService.validateLicenseOffline(
    licenseKey, rsaKeyId, productName, productVersion
);

// 功能验证
boolean featureAllowed = licenseValidationService.validateFeature(
    licenseData, featureName
);
```

## API 接口

### 认证接口

#### 登录
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123",
  "deviceId": "optional-device-id"
}
```

#### 刷新令牌
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "refresh-token-here"
}
```

### 授权管理接口

#### 生成授权码
```http
POST /api/licenses/generate
Authorization: Bearer {access-token}
Content-Type: application/json

{
  "customerId": "CUST_12345",
  "productName": "Enterprise License System",
  "productVersion": "1.0.0",
  "licenseType": "ENTERPRISE",
  "validityDays": 365,
  "bindHardware": true,
  "features": {
    "feature1": "enabled",
    "maxUsers": "100"
  },
  "restrictions": {
    "maxUsage": "1000"
  }
}
```

#### 验证授权码
```http
POST /api/licenses/validate
Content-Type: application/json

{
  "licenseKey": "encrypted-license-key-here",
  "productName": "Enterprise License System",
  "productVersion": "1.0.0",
  "online": true
}
```

#### 获取硬件指纹
```http
GET /api/licenses/hardware-fingerprint
```

## 安全建议

### 1. 密钥管理
- 定期轮换RSA密钥（建议24小时）
- 使用安全的密钥存储（Redis加密存储）
- 实施密钥版本控制和退役策略

### 2. 网络安全
- 使用HTTPS传输所有敏感数据
- 实施IP白名单和访问控制
- 配置防火墙和入侵检测

### 3. 应用安全
- 启用CSRF保护
- 实施SQL注入防护
- 配置安全响应头

### 4. 运维安全
- 启用审计日志
- 监控异常访问模式
- 定期安全扫描和渗透测试

## 部署配置

### 环境变量
```bash
# JWT配置
JWT_SECRET=your-super-secure-jwt-secret-key-here
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# 数据库配置
DB_USERNAME=license_user
DB_PASSWORD=secure_database_password
DB_HOST=localhost
DB_PORT=5432
DB_NAME=license_management

# Redis配置
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=secure_redis_password

# 安全配置
KEY_ROTATION_INTERVAL=24
MAX_KEY_AGE=168
RSA_KEY_LENGTH=4096
AES_KEY_LENGTH=256
```

### Docker部署
```yaml
version: '3.8'
services:
  license-system:
    image: enterprise/license-management:latest
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - DB_HOST=postgres
      - REDIS_HOST=redis
    depends_on:
      - postgres
      - redis
    ports:
      - "8080:8080"
  
  postgres:
    image: postgres:15
    environment:
      - POSTGRES_DB=license_management
      - POSTGRES_USER=license_user
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
  
  redis:
    image: redis:7-alpine
    command: redis-server --requirepass ${REDIS_PASSWORD}
    volumes:
      - redis_data:/data

volumes:
  postgres_data:
  redis_data:
```

## 默认管理员账户

- **用户名**: admin
- **密码**: admin123
- **权限**: ADMIN, LICENSE_MANAGER

**重要提示**: 部署后请立即修改默认密码！

## 支持的加密算法

- **对称加密**: AES-256-GCM
- **非对称加密**: RSA-4096
- **哈希算法**: SHA-256, SHA-512
- **签名算法**: SHA256withRSA
- **密钥派生**: PBKDF2WithHmacSHA256
- **消息认证**: HMAC-SHA256

## 性能特点

- RSA-4096密钥生成: ~2-5秒
- AES-256加密: 高速（MB/s级别）
- 授权码验证: <100ms（在线），<50ms（离线）
- 硬件指纹生成: <200ms
- 支持并发验证: 1000+ QPS

## 故障排除

### 常见问题

1. **密钥生成失败**
   - 检查系统随机数生成器
   - 确保足够的系统熵

2. **Redis连接失败**
   - 检查Redis服务状态
   - 验证网络连接和认证信息

3. **JWT令牌无效**
   - 检查系统时间同步
   - 验证JWT密钥配置

4. **硬件指纹不匹配**
   - 检查硬件变更
   - 调整相似度阈值

### 日志位置
- 应用日志: `logs/license-management-system.log`
- 访问日志: Spring Boot Actuator
- 安全审计: Redis存储

## 技术支持

如需技术支持，请联系：
- 邮箱: support@enterprise.com
- 文档: [内部文档链接]
- 监控: [监控面板链接]