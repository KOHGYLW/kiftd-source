package com.enterprise.license.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户详情服务实现
 * 负责加载用户信息和权限
 */
@Service
public class LicenseUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(LicenseUserDetailsService.class);
    private static final String REDIS_USER_PREFIX = "license:users:";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 用户信息类
     */
    public static class LicenseUser {
        private String username;
        private String password;
        private String email;
        private Set<String> roles;
        private boolean enabled;
        private boolean accountNonExpired;
        private boolean accountNonLocked;
        private boolean credentialsNonExpired;
        private LocalDateTime createdAt;
        private LocalDateTime lastLoginAt;
        private Map<String, Object> metadata;

        public LicenseUser() {
            this.roles = new HashSet<>();
            this.enabled = true;
            this.accountNonExpired = true;
            this.accountNonLocked = true;
            this.credentialsNonExpired = true;
            this.metadata = new HashMap<>();
        }

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public Set<String> getRoles() { return roles; }
        public void setRoles(Set<String> roles) { this.roles = roles; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public boolean isAccountNonExpired() { return accountNonExpired; }
        public void setAccountNonExpired(boolean accountNonExpired) { this.accountNonExpired = accountNonExpired; }

        public boolean isAccountNonLocked() { return accountNonLocked; }
        public void setAccountNonLocked(boolean accountNonLocked) { this.accountNonLocked = accountNonLocked; }

        public boolean isCredentialsNonExpired() { return credentialsNonExpired; }
        public void setCredentialsNonExpired(boolean credentialsNonExpired) { this.credentialsNonExpired = credentialsNonExpired; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getLastLoginAt() { return lastLoginAt; }
        public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("加载用户信息: {}", username);

        LicenseUser licenseUser = getUserFromStorage(username);
        if (licenseUser == null) {
            logger.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        // 转换权限
        Collection<GrantedAuthority> authorities = licenseUser.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        // 创建Spring Security用户对象
        return User.builder()
                .username(licenseUser.getUsername())
                .password(licenseUser.getPassword())
                .authorities(authorities)
                .accountExpired(!licenseUser.isAccountNonExpired())
                .accountLocked(!licenseUser.isAccountNonLocked())
                .credentialsExpired(!licenseUser.isCredentialsNonExpired())
                .disabled(!licenseUser.isEnabled())
                .build();
    }

    /**
     * 创建用户
     */
    public void createUser(String username, String rawPassword, String email, Set<String> roles) {
        if (userExists(username)) {
            throw new IllegalArgumentException("用户已存在: " + username);
        }

        LicenseUser user = new LicenseUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(email);
        user.setRoles(roles);
        user.setCreatedAt(LocalDateTime.now());

        saveUserToStorage(user);
        logger.info("用户创建成功: {}", username);
    }

    /**
     * 更新用户
     */
    public void updateUser(LicenseUser user) {
        if (!userExists(user.getUsername())) {
            throw new IllegalArgumentException("用户不存在: " + user.getUsername());
        }

        saveUserToStorage(user);
        logger.info("用户更新成功: {}", user.getUsername());
    }

    /**
     * 删除用户
     */
    public void deleteUser(String username) {
        String key = REDIS_USER_PREFIX + username;
        Boolean deleted = redisTemplate.delete(key);
        
        if (Boolean.TRUE.equals(deleted)) {
            logger.info("用户删除成功: {}", username);
        } else {
            logger.warn("删除用户失败，用户可能不存在: {}", username);
        }
    }

    /**
     * 更改密码
     */
    public void changePassword(String username, String oldPassword, String newPassword) {
        LicenseUser user = getUserFromStorage(username);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在: " + username);
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        saveUserToStorage(user);
        
        logger.info("用户密码更改成功: {}", username);
    }

    /**
     * 检查用户是否存在
     */
    public boolean userExists(String username) {
        String key = REDIS_USER_PREFIX + username;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 获取用户信息
     */
    public LicenseUser getUser(String username) {
        return getUserFromStorage(username);
    }

    /**
     * 更新最后登录时间
     */
    public void updateLastLoginTime(String username) {
        LicenseUser user = getUserFromStorage(username);
        if (user != null) {
            user.setLastLoginAt(LocalDateTime.now());
            saveUserToStorage(user);
        }
    }

    /**
     * 启用/禁用用户
     */
    public void setUserEnabled(String username, boolean enabled) {
        LicenseUser user = getUserFromStorage(username);
        if (user != null) {
            user.setEnabled(enabled);
            saveUserToStorage(user);
            logger.info("用户状态更新: {} - {}", username, enabled ? "启用" : "禁用");
        }
    }

    /**
     * 锁定/解锁用户
     */
    public void setUserLocked(String username, boolean locked) {
        LicenseUser user = getUserFromStorage(username);
        if (user != null) {
            user.setAccountNonLocked(!locked);
            saveUserToStorage(user);
            logger.info("用户锁定状态更新: {} - {}", username, locked ? "锁定" : "解锁");
        }
    }

    /**
     * 获取所有用户（分页）
     */
    public List<LicenseUser> getAllUsers() {
        Set<String> keys = redisTemplate.keys(REDIS_USER_PREFIX + "*");
        List<LicenseUser> users = new ArrayList<>();

        if (keys != null) {
            for (String key : keys) {
                LicenseUser user = (LicenseUser) redisTemplate.opsForValue().get(key);
                if (user != null) {
                    users.add(user);
                }
            }
        }

        return users;
    }

    /**
     * 从存储中获取用户
     */
    private LicenseUser getUserFromStorage(String username) {
        String key = REDIS_USER_PREFIX + username;
        return (LicenseUser) redisTemplate.opsForValue().get(key);
    }

    /**
     * 保存用户到存储
     */
    private void saveUserToStorage(LicenseUser user) {
        String key = REDIS_USER_PREFIX + user.getUsername();
        redisTemplate.opsForValue().set(key, user);
    }

    /**
     * 初始化默认管理员用户
     */
    public void initializeDefaultAdmin() {
        String adminUsername = "admin";
        if (!userExists(adminUsername)) {
            Set<String> adminRoles = new HashSet<>();
            adminRoles.add("ADMIN");
            adminRoles.add("LICENSE_MANAGER");
            
            createUser(adminUsername, "admin123", "admin@enterprise.com", adminRoles);
            logger.info("默认管理员用户已创建: {}", adminUsername);
        }
    }

    /**
     * 验证用户密码
     */
    public boolean validatePassword(String username, String rawPassword) {
        LicenseUser user = getUserFromStorage(username);
        if (user == null) {
            return false;
        }
        
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }
}