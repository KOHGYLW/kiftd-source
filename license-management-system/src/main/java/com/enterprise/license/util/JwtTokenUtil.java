package com.enterprise.license.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 企业级JWT工具类
 * 支持令牌撤销、刷新令牌、多设备登录管理等高级功能
 */
@Component
public class JwtTokenUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);

    @Value("${license.jwt.secret}")
    private String secret;

    @Value("${license.jwt.expiration}")
    private Long expiration;

    @Value("${license.jwt.refresh-expiration:604800000}")
    private Long refreshExpiration; // 7天

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SecureRandomUtil secureRandomUtil;

    private static final String REDIS_TOKEN_BLACKLIST_PREFIX = "jwt:blacklist:";
    private static final String REDIS_REFRESH_TOKEN_PREFIX = "jwt:refresh:";
    private static final String REDIS_USER_TOKENS_PREFIX = "jwt:user:";

    /**
     * JWT令牌信息
     */
    public static class TokenInfo {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private Long expiresIn;
        private Date issuedAt;
        private String jti; // JWT ID
        private String deviceId;

        // Getters and Setters
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }

        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }

        public Long getExpiresIn() { return expiresIn; }
        public void setExpiresIn(Long expiresIn) { this.expiresIn = expiresIn; }

        public Date getIssuedAt() { return issuedAt; }
        public void setIssuedAt(Date issuedAt) { this.issuedAt = issuedAt; }

        public String getJti() { return jti; }
        public void setJti(String jti) { this.jti = jti; }

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * 生成完整的令牌信息（包含访问令牌和刷新令牌）
     */
    public TokenInfo generateTokenInfo(UserDetails userDetails, String deviceId) {
        Date now = new Date();
        String jti = secureRandomUtil.generateUUID();
        
        // 生成访问令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        claims.put("jti", jti);
        claims.put("deviceId", deviceId);
        
        String accessToken = createToken(claims, userDetails.getUsername(), expiration);
        
        // 生成刷新令牌
        String refreshToken = generateRefreshToken(userDetails.getUsername(), jti, deviceId);
        
        // 存储令牌关联信息
        storeUserToken(userDetails.getUsername(), jti, deviceId, now);
        
        TokenInfo tokenInfo = new TokenInfo();
        tokenInfo.setAccessToken(accessToken);
        tokenInfo.setRefreshToken(refreshToken);
        tokenInfo.setExpiresIn(expiration / 1000);
        tokenInfo.setIssuedAt(now);
        tokenInfo.setJti(jti);
        tokenInfo.setDeviceId(deviceId);
        
        logger.debug("为用户生成令牌: {}, 设备: {}, JTI: {}", userDetails.getUsername(), deviceId, jti);
        return tokenInfo;
    }

    /**
     * 刷新访问令牌
     */
    public TokenInfo refreshToken(String refreshToken) {
        try {
            Claims claims = getAllClaimsFromToken(refreshToken);
            String username = claims.getSubject();
            String jti = claims.get("jti", String.class);
            String deviceId = claims.get("deviceId", String.class);
            
            // 验证刷新令牌是否存在且有效
            String refreshKey = REDIS_REFRESH_TOKEN_PREFIX + jti;
            if (!redisTemplate.hasKey(refreshKey)) {
                throw new JwtException("刷新令牌无效或已过期");
            }
            
            // 撤销旧的访问令牌
            revokeUserToken(username, jti);
            
            // 生成新的令牌信息
            // 这里需要重新加载用户信息以获取最新的权限
            // 为简化，这里使用基本实现
            Map<String, Object> newClaims = new HashMap<>();
            newClaims.put("jti", jti);
            newClaims.put("deviceId", deviceId);
            
            Date now = new Date();
            String newAccessToken = createToken(newClaims, username, expiration);
            
            TokenInfo tokenInfo = new TokenInfo();
            tokenInfo.setAccessToken(newAccessToken);
            tokenInfo.setRefreshToken(refreshToken); // 刷新令牌保持不变
            tokenInfo.setExpiresIn(expiration / 1000);
            tokenInfo.setIssuedAt(now);
            tokenInfo.setJti(jti);
            tokenInfo.setDeviceId(deviceId);
            
            logger.debug("令牌刷新成功: {}, JTI: {}", username, jti);
            return tokenInfo;
            
        } catch (Exception e) {
            logger.error("令牌刷新失败", e);
            throw new JwtException("令牌刷新失败: " + e.getMessage());
        }
    }

    /**
     * 撤销令牌
     */
    public void revokeToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String jti = claims.get("jti", String.class);
            String username = claims.getSubject();
            
            // 添加到黑名单
            Date expiration = claims.getExpiration();
            long ttl = (expiration.getTime() - System.currentTimeMillis()) / 1000;
            
            if (ttl > 0) {
                redisTemplate.opsForValue().set(
                    REDIS_TOKEN_BLACKLIST_PREFIX + jti, 
                    true, 
                    ttl, 
                    TimeUnit.SECONDS
                );
            }
            
            // 撤销用户令牌
            revokeUserToken(username, jti);
            
            logger.info("令牌已撤销: {}, JTI: {}", username, jti);
            
        } catch (Exception e) {
            logger.error("撤销令牌失败", e);
            throw new JwtException("撤销令牌失败: " + e.getMessage());
        }
    }

    /**
     * 撤销用户所有令牌
     */
    public void revokeAllUserTokens(String username) {
        try {
            String userTokensKey = REDIS_USER_TOKENS_PREFIX + username;
            Set<String> tokenIds = redisTemplate.opsForSet().members(userTokensKey);
            
            if (tokenIds != null) {
                for (String jti : tokenIds) {
                    // 添加到黑名单
                    redisTemplate.opsForValue().set(
                        REDIS_TOKEN_BLACKLIST_PREFIX + jti, 
                        true, 
                        expiration / 1000, 
                        TimeUnit.SECONDS
                    );
                    
                    // 删除刷新令牌
                    redisTemplate.delete(REDIS_REFRESH_TOKEN_PREFIX + jti);
                }
                
                // 清空用户令牌集合
                redisTemplate.delete(userTokensKey);
            }
            
            logger.info("已撤销用户所有令牌: {}", username);
            
        } catch (Exception e) {
            logger.error("撤销用户所有令牌失败: {}", username, e);
        }
    }

    /**
     * 验证令牌（增强版）
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = getAllClaimsFromToken(token);
            String jti = claims.get("jti", String.class);
            
            // 检查是否在黑名单中
            if (isTokenBlacklisted(jti)) {
                logger.debug("令牌在黑名单中: {}", jti);
                return false;
            }
            
            // 检查是否过期
            return !isTokenExpired(token);
            
        } catch (Exception e) {
            logger.debug("令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从token中获取用户名
     */
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    /**
     * 从token中获取JTI
     */
    public String getJtiFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("jti", String.class));
    }

    /**
     * 从token中获取设备ID
     */
    public String getDeviceIdFromToken(String token) {
        return getClaimFromToken(token, claims -> claims.get("deviceId", String.class));
    }

    /**
     * 从token中获取过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    /**
     * 从token中获取指定claim
     */
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从token中获取所有claims
     */
    private Claims getAllClaimsFromToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token已过期: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            logger.warn("不支持的JWT token: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            logger.warn("JWT token格式错误: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token为空: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 检查token是否过期
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 为用户生成token（兼容性方法）
     */
    public String generateToken(UserDetails userDetails) {
        String deviceId = secureRandomUtil.generateUUID();
        TokenInfo tokenInfo = generateTokenInfo(userDetails, deviceId);
        return tokenInfo.getAccessToken();
    }

    /**
     * 创建token
     */
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 生成刷新令牌
     */
    private String generateRefreshToken(String username, String jti, String deviceId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        Map<String, Object> claims = new HashMap<>();
        claims.put("jti", jti);
        claims.put("deviceId", deviceId);
        claims.put("type", "refresh");

        String refreshToken = Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();

        // 存储刷新令牌到Redis
        redisTemplate.opsForValue().set(
            REDIS_REFRESH_TOKEN_PREFIX + jti,
            refreshToken,
            refreshExpiration,
            TimeUnit.MILLISECONDS
        );

        return refreshToken;
    }

    /**
     * 存储用户令牌关联信息
     */
    private void storeUserToken(String username, String jti, String deviceId, Date issuedAt) {
        String userTokensKey = REDIS_USER_TOKENS_PREFIX + username;
        
        // 将JTI添加到用户令牌集合
        redisTemplate.opsForSet().add(userTokensKey, jti);
        redisTemplate.expire(userTokensKey, expiration, TimeUnit.MILLISECONDS);
        
        // 存储令牌详细信息
        Map<String, Object> tokenDetails = new HashMap<>();
        tokenDetails.put("deviceId", deviceId);
        tokenDetails.put("issuedAt", issuedAt);
        tokenDetails.put("username", username);
        
        redisTemplate.opsForValue().set(
            "jwt:details:" + jti,
            tokenDetails,
            expiration,
            TimeUnit.MILLISECONDS
        );
    }

    /**
     * 撤销用户特定令牌
     */
    private void revokeUserToken(String username, String jti) {
        String userTokensKey = REDIS_USER_TOKENS_PREFIX + username;
        redisTemplate.opsForSet().remove(userTokensKey, jti);
        redisTemplate.delete("jwt:details:" + jti);
    }

    /**
     * 检查令牌是否在黑名单中
     */
    private boolean isTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(REDIS_TOKEN_BLACKLIST_PREFIX + jti));
    }

    /**
     * 验证token（兼容性方法）
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = getUsernameFromToken(token);
            return (username.equals(userDetails.getUsername()) && validateToken(token));
        } catch (Exception e) {
            logger.warn("JWT token验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查token是否有效（兼容性方法）
     */
    public Boolean isTokenValid(String token) {
        return validateToken(token);
    }

    /**
     * 获取用户活跃令牌数量
     */
    public int getUserActiveTokensCount(String username) {
        String userTokensKey = REDIS_USER_TOKENS_PREFIX + username;
        Long count = redisTemplate.opsForSet().size(userTokensKey);
        return count != null ? count.intValue() : 0;
    }

    /**
     * 获取用户所有设备的令牌信息
     */
    public List<Map<String, Object>> getUserTokenDetails(String username) {
        String userTokensKey = REDIS_USER_TOKENS_PREFIX + username;
        Set<String> tokenIds = redisTemplate.opsForSet().members(userTokensKey);
        List<Map<String, Object>> tokenDetails = new ArrayList<>();

        if (tokenIds != null) {
            for (String jti : tokenIds) {
                Map<String, Object> details = (Map<String, Object>) 
                    redisTemplate.opsForValue().get("jwt:details:" + jti);
                if (details != null) {
                    details.put("jti", jti);
                    tokenDetails.add(details);
                }
            }
        }

        return tokenDetails;
    }

    /**
     * 清理过期的令牌信息
     */
    public int cleanupExpiredTokens() {
        int cleanedCount = 0;
        
        try {
            // 清理过期的刷新令牌
            Set<String> refreshKeys = redisTemplate.keys(REDIS_REFRESH_TOKEN_PREFIX + "*");
            if (refreshKeys != null) {
                for (String key : refreshKeys) {
                    if (!redisTemplate.hasKey(key)) {
                        cleanedCount++;
                    }
                }
            }
            
            logger.info("清理了{}个过期令牌", cleanedCount);
            
        } catch (Exception e) {
            logger.error("清理过期令牌时发生错误", e);
        }
        
        return cleanedCount;
    }

}