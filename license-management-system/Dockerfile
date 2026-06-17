# 多阶段构建 - 基础镜像
FROM openjdk:17-jdk-slim as base

# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone

# 安装必要的工具
RUN apt-get update && \
    apt-get install -y curl wget && \
    rm -rf /var/lib/apt/lists/*

# 构建阶段
FROM base as builder

# 设置工作目录
WORKDIR /build

# 复制Maven配置文件
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# 下载依赖（利用Docker缓存层）
RUN ./mvnw dependency:go-offline -B

# 复制源代码
COPY src src

# 编译应用
RUN ./mvnw clean package -DskipTests -B

# 生产运行时镜像
FROM base as production

# 创建应用用户
RUN groupadd -r appuser && useradd -r -g appuser appuser

# 设置工作目录
WORKDIR /app

# 创建日志目录
RUN mkdir -p /var/log/license-management-system && \
    chown -R appuser:appuser /var/log/license-management-system

# 创建数据目录
RUN mkdir -p /app/data && \
    chown -R appuser:appuser /app/data

# 从构建阶段复制jar文件
COPY --from=builder /build/target/*.jar app.jar

# 修改文件权限
RUN chown appuser:appuser app.jar

# 切换到非root用户
USER appuser

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/actuator/health || exit 1

# 暴露端口
EXPOSE 8080

# JVM优化参数
ENV JAVA_OPTS="-Xms1g -Xmx2g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseJVMCICompiler \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai \
    -Djava.security.egd=file:/dev/./urandom"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# 开发阶段镜像（用于开发和测试）
FROM base as development

# 设置工作目录
WORKDIR /app

# 复制Maven配置文件
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# 下载依赖
RUN ./mvnw dependency:go-offline -B

# 复制源代码
COPY src src

# 暴露端口
EXPOSE 8080 5005

# 开发环境JVM参数（包含调试端口）
ENV JAVA_OPTS="-Xms512m -Xmx1g \
    -XX:+UseG1GC \
    -Dfile.encoding=UTF-8 \
    -Duser.timezone=Asia/Shanghai \
    -Djava.security.egd=file:/dev/./urandom \
    -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# 开发环境启动命令
ENTRYPOINT ["sh", "-c", "./mvnw spring-boot:run -Dspring-boot.run.jvmArguments=\"$JAVA_OPTS\""]