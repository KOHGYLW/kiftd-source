package com.enterprise.license.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 测试容器配置
 * 用于集成测试时启动真实的PostgreSQL和Redis容器
 */
@TestConfiguration
@Profile("integration-test")
public class TestContainersConfig {

    /**
     * PostgreSQL测试容器
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
                .withDatabaseName("license_test")
                .withUsername("test_user")
                .withPassword("test_password")
                .withReuse(true);
    }

    /**
     * Redis测试容器
     */
    @Bean
    @ServiceConnection
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
                .withReuse(true);
    }
}