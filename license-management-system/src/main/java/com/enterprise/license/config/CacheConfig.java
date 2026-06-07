package com.enterprise.license.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 缓存配置优化
 */
@Configuration
@EnableCaching
@Profile("!test")
public class CacheConfig extends CachingConfigurerSupport {

    /**
     * 自定义缓存键生成器
     */
    @Bean
    @Override
    public KeyGenerator keyGenerator() {
        return (target, method, params) -> {
            StringBuilder key = new StringBuilder();
            key.append(target.getClass().getSimpleName()).append(":");
            key.append(method.getName()).append(":");
            
            for (Object param : params) {
                if (param != null) {
                    key.append(param.toString()).append(":");
                }
            }
            
            // 移除最后一个冒号
            if (key.length() > 0 && key.charAt(key.length() - 1) == ':') {
                key.deleteCharAt(key.length() - 1);
            }
            
            return key.toString();
        };
    }

    /**
     * Redis缓存管理器配置
     */
    @Bean
    @Override
    public CacheManager cacheManager() {
        return RedisCacheManager.builder(redisConnectionFactory())
                .cacheDefaults(defaultCacheConfiguration())
                .withInitialCacheConfigurations(cacheConfigurationMap())
                .transactionAware()
                .build();
    }

    /**
     * Redis连接工厂（由Spring自动配置提供）
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        // 这里使用Spring Boot自动配置的RedisConnectionFactory
        return null; // Spring会自动注入
    }

    /**
     * 默认缓存配置
     */
    private RedisCacheConfiguration defaultCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30)) // 默认30分钟过期
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues(); // 不缓存null值
    }

    /**
     * 不同缓存区域的特定配置
     */
    private Map<String, RedisCacheConfiguration> cacheConfigurationMap() {
        Map<String, RedisCacheConfiguration> configMap = new HashMap<>();
        
        // 客户缓存配置 - 1小时过期
        configMap.put("customers", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues());

        // 许可证缓存配置 - 30分钟过期
        configMap.put("licenses", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues());

        // 许可证验证缓存配置 - 10分钟过期
        configMap.put("license-validations", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues());

        // 统计数据缓存配置 - 5分钟过期
        configMap.put("statistics", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues());

        // 系统配置缓存 - 24小时过期
        configMap.put("system-config", RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(24))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues());

        return configMap;
    }

    /**
     * 缓存错误处理器
     */
    @Bean
    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, 
                                          org.springframework.cache.Cache cache, Object key) {
                // 记录缓存获取错误但不抛出异常，系统继续正常运行
                logger.warn("缓存获取失败 - Cache: {}, Key: {}, Error: {}", 
                          cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, 
                                          org.springframework.cache.Cache cache, Object key, Object value) {
                // 记录缓存写入错误但不抛出异常
                logger.warn("缓存写入失败 - Cache: {}, Key: {}, Error: {}", 
                          cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, 
                                            org.springframework.cache.Cache cache, Object key) {
                // 记录缓存清除错误但不抛出异常
                logger.warn("缓存清除失败 - Cache: {}, Key: {}, Error: {}", 
                          cache.getName(), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, 
                                            org.springframework.cache.Cache cache) {
                // 记录缓存清空错误但不抛出异常
                logger.warn("缓存清空失败 - Cache: {}, Error: {}", 
                          cache.getName(), exception.getMessage());
            }
        };
    }
}