# kiftd Windows 部署文档

## 概述

kiftd 是一款便捷、开源、功能完善的个人&团队&小型团队网盘服务器系统。本文档详细介绍了在 Windows 环境下部署 kiftd 的完整流程。

## 系统要求

### 硬件要求
- **CPU**: 双核 2.0GHz 或更高
- **内存**: 最低 2GB RAM，推荐 4GB 或更高
- **存储**: 至少 1GB 可用磁盘空间（不包括文件存储空间）
- **网络**: 稳定的网络连接

### 软件要求
- **操作系统**: Windows 7/8/10/11 (64位推荐)
- **Java**: JDK 1.8.0 或更高版本
- **Maven**: 3.6.0 或更高版本（仅开发环境需要）

## 部署方式

### 方式一：使用预编译版本（推荐）

#### 1. 下载预编译版本
- 访问 [kiftd官方主页](https://kohgylw.github.io/)
- 下载最新的 kiftd 发行版
- 解压到目标目录（如 `C:\kiftd`）

#### 2. 启动服务
```cmd
# 进入 kiftd 目录
cd C:\kiftd

# 启动 UI 模式
java -jar kiftd-1.2.2-RELEASE.jar

# 或启动控制台模式
java -jar kiftd-1.2.2-RELEASE.jar -console
```

### 方式二：从源码编译部署

#### 1. 环境准备

##### 安装 JDK
1. 下载并安装 JDK 1.8.0 或更高版本
2. 配置环境变量：
   ```cmd
   # 设置 JAVA_HOME
   set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_XXX
   
   # 添加到 PATH
   set PATH=%JAVA_HOME%\bin;%PATH%
   ```
3. 验证安装：
   ```cmd
   java -version
   javac -version
   ```

##### 安装 Maven（可选）
1. 下载并解压 Maven
2. 配置环境变量：
   ```cmd
   set MAVEN_HOME=C:\apache-maven-3.x.x
   set PATH=%MAVEN_HOME%\bin;%PATH%
   ```
3. 验证安装：
   ```cmd
   mvn -version
   ```

#### 2. 获取源码
```cmd
# 克隆仓库
git clone https://github.com/KOHGYLW/kiftd-source.git
cd kiftd-source
```

#### 3. 编译项目
```cmd
# 清理并编译
mvn clean compile

# 打包生成 JAR 文件
mvn package -DskipTests
```

#### 4. 部署运行
```cmd
# 复制生成的 JAR 文件到项目根目录
copy target\kiftd-1.2.2-RELEASE.jar .

# 启动服务
java -jar kiftd-1.2.2-RELEASE.jar
```

## 配置说明

### 基本配置

#### 1. 首次启动配置
- 首次启动时，系统会自动创建 `conf` 目录
- 主要配置文件：`conf\server.properties`

#### 2. 端口配置
```properties
# HTTP 端口（默认 8080）
server.port=8080

# HTTPS 端口（可选）
server.https.port=8443
```

#### 3. 文件存储配置
```properties
# 文件存储根目录
file.storage.path=D:\kiftd-files

# 最大上传文件大小（MB）
file.upload.max.size=1024
```

### 高级配置

#### 1. 数据库配置
kiftd 使用 H2 内嵌数据库，配置文件位于 `conf\h2.properties`

#### 2. 日志配置
- 日志文件位置：`logs\` 目录
- 日志级别可在 `conf\logback.xml` 中配置

#### 3. SSL/HTTPS 配置
```properties
# 启用 HTTPS
server.ssl.enabled=true

# 证书文件路径
server.ssl.key-store=conf\keystore.p12

# 证书密码
server.ssl.key-store-password=your_password
```

## 服务管理

### 启动模式

#### 1. 图形界面模式（默认）
```cmd
java -jar kiftd-1.2.2-RELEASE.jar
```

#### 2. 控制台模式
```cmd
java -jar kiftd-1.2.2-RELEASE.jar -console
```

#### 3. 后台服务模式
```cmd
# 使用 nohup 或 Windows 服务包装器
java -jar kiftd-1.2.2-RELEASE.jar -start
```

### Windows 服务安装

#### 使用 NSSM（推荐）
1. 下载 NSSM (Non-Sucking Service Manager)
2. 安装服务：
   ```cmd
   nssm install kiftd
   ```
3. 配置服务参数：
   - Application path: `C:\Program Files\Java\jdk1.8.0_XXX\bin\java.exe`
   - Startup directory: `C:\kiftd`
   - Arguments: `-jar kiftd-1.2.2-RELEASE.jar -start`

#### 使用 Windows SC 命令
```cmd
sc create kiftd binPath= "java -jar C:\kiftd\kiftd-1.2.2-RELEASE.jar -start" start= auto
```

## 故障排除

### 常见问题及解决方案

#### 1. 端口占用问题
**问题**: `Address already in use: bind`
**解决方案**:
```cmd
# 查找占用端口的进程
netstat -ano | findstr :8080

# 终止占用进程
taskkill /PID <进程ID> /F

# 或修改配置文件中的端口号
```

#### 2. Java 版本不兼容
**问题**: `UnsupportedClassVersionError`
**解决方案**:
- 确保使用 JDK 1.8.0 或更高版本
- 检查 JAVA_HOME 环境变量设置

#### 3. 内存不足
**问题**: `OutOfMemoryError`
**解决方案**:
```cmd
# 增加 JVM 内存参数
java -Xms512m -Xmx2048m -jar kiftd-1.2.2-RELEASE.jar
```

#### 4. 文件权限问题
**问题**: 无法创建文件或目录
**解决方案**:
- 确保运行用户有足够的文件系统权限
- 以管理员身份运行

#### 5. Maven 编译失败
**问题**: `BUILD FAILURE` 或依赖下载失败
**解决方案**:
```cmd
# 清理 Maven 缓存
mvn dependency:purge-local-repository

# 强制更新依赖
mvn clean compile -U

# 跳过测试编译
mvn package -DskipTests
```

#### 6. 进程锁定 JAR 文件
**问题**: Maven clean 阶段无法删除 target 目录中的 JAR 文件
**解决方案**:
```cmd
# 查找并终止相关 Java 进程
Get-Process | Where-Object {$_.ProcessName -eq "java" -or $_.ProcessName -eq "javaw"}

# 终止特定进程
Stop-Process -Id <进程ID> -Force

# 重新编译
mvn clean compile
```

### 性能优化

#### 1. JVM 参数优化
```cmd
java -server -Xms1024m -Xmx2048m -XX:+UseG1GC -jar kiftd-1.2.2-RELEASE.jar
```

#### 2. 文件系统优化
- 使用 SSD 存储提高 I/O 性能
- 定期清理临时文件和日志

#### 3. 网络优化
- 配置防火墙规则允许相应端口
- 使用反向代理（如 Nginx）提高并发处理能力

## 安全建议

### 1. 访问控制
- 设置强密码策略
- 启用用户权限管理
- 定期审查用户账户

### 2. 网络安全
- 启用 HTTPS 加密传输
- 配置防火墙限制访问来源
- 使用 VPN 进行远程访问

### 3. 数据安全
- 定期备份数据库和配置文件
- 启用文件版本控制
- 监控磁盘空间使用情况

## 监控和维护

### 1. 日志监控
- 定期检查 `logs` 目录下的日志文件
- 设置日志轮转避免磁盘空间耗尽

### 2. 性能监控
```cmd
# 监控 Java 进程
jps -l

# 查看 JVM 内存使用情况
jstat -gc <进程ID>
```

### 3. 定期维护
- 定期重启服务释放内存
- 清理过期的临时文件
- 更新到最新版本

## 备份和恢复

### 1. 数据备份
```cmd
# 备份配置文件
xcopy /E /I conf backup\conf

# 备份数据库文件
xcopy /E /I data backup\data

# 备份用户文件
xcopy /E /I files backup\files
```

### 2. 系统恢复
```cmd
# 恢复配置文件
xcopy /E /I backup\conf conf

# 恢复数据库
xcopy /E /I backup\data data

# 恢复用户文件
xcopy /E /I backup\files files
```

## 版本升级

### 1. 升级前准备
- 备份当前版本的所有数据
- 记录当前配置参数
- 停止 kiftd 服务

### 2. 升级步骤
```cmd
# 下载新版本
# 替换 JAR 文件
copy kiftd-new-version.jar kiftd-1.2.2-RELEASE.jar

# 启动新版本
java -jar kiftd-1.2.2-RELEASE.jar
```

### 3. 升级验证
- 检查服务是否正常启动
- 验证用户数据完整性
- 测试主要功能

## 联系支持

如遇到部署问题，可通过以下方式获取帮助：

- **官方主页**: https://kohgylw.github.io/
- **源码仓库**: https://github.com/KOHGYLW/kiftd-source
- **邮件支持**: kohgylw@163.com
- **问题反馈**: 在 GitHub 仓库中提交 Issue

---

**文档版本**: v1.0  
**最后更新**: 2024年12月  
**适用版本**: kiftd v1.2.2-RELEASE  
**作者**: kiftd 开发团队