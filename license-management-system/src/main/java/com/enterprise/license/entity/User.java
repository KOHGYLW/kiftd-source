package com.enterprise.license.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "users")
@Schema(description = "用户实体")
public class User extends BaseEntity implements UserDetails {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Column(nullable = false, unique = true, length = 50)
    @Schema(description = "用户名", example = "admin")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    @Column(nullable = false)
    @Schema(description = "密码")
    private String password;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Column(nullable = false, unique = true)
    @Schema(description = "邮箱", example = "admin@example.com")
    private String email;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 100, message = "真实姓名长度不能超过100个字符")
    @Column(name = "real_name", nullable = false, length = 100)
    @Schema(description = "真实姓名", example = "张三")
    private String realName;

    @Size(max = 20, message = "手机号长度不能超过20个字符")
    @Column(name = "phone_number", length = 20)
    @Schema(description = "手机号", example = "13800138000")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Schema(description = "用户角色", example = "ADMIN")
    private UserRole role = UserRole.USER;

    @Column(name = "is_enabled", nullable = false, columnDefinition = "boolean default true")
    @Schema(description = "是否启用", example = "true")
    private Boolean enabled = true;

    @Column(name = "is_account_non_expired", nullable = false, columnDefinition = "boolean default true")
    @Schema(description = "账户是否未过期", example = "true")
    private Boolean accountNonExpired = true;

    @Column(name = "is_account_non_locked", nullable = false, columnDefinition = "boolean default true")
    @Schema(description = "账户是否未锁定", example = "true")
    private Boolean accountNonLocked = true;

    @Column(name = "is_credentials_non_expired", nullable = false, columnDefinition = "boolean default true")
    @Schema(description = "凭证是否未过期", example = "true")
    private Boolean credentialsNonExpired = true;

    @Column(name = "last_login_time")
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Column(name = "last_login_ip", length = 45)
    @Schema(description = "最后登录IP", example = "192.168.1.100")
    private String lastLoginIp;

    @Column(name = "login_count", columnDefinition = "bigint default 0")
    @Schema(description = "登录次数", example = "10")
    private Long loginCount = 0L;

    @Column(name = "password_reset_token")
    @Schema(description = "密码重置令牌")
    private String passwordResetToken;

    @Column(name = "password_reset_token_expiry")
    @Schema(description = "密码重置令牌过期时间")
    private LocalDateTime passwordResetTokenExpiry;

    // 用户角色枚举
    public enum UserRole {
        ADMIN("管理员"),
        USER("普通用户");

        private final String description;

        UserRole(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // UserDetails接口实现
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public boolean isAccountNonExpired() {
        return accountNonExpired != null ? accountNonExpired : true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return accountNonLocked != null ? accountNonLocked : true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired != null ? credentialsNonExpired : true;
    }

    @Override
    public boolean isEnabled() {
        return enabled != null ? enabled : true;
    }

    /**
     * 增加登录次数
     */
    public void incrementLoginCount() {
        if (this.loginCount == null) {
            this.loginCount = 0L;
        }
        this.loginCount++;
    }

    /**
     * 更新最后登录信息
     */
    public void updateLastLogin(String ip) {
        this.lastLoginTime = LocalDateTime.now();
        this.lastLoginIp = ip;
        incrementLoginCount();
    }

    /**
     * 设置密码重置令牌
     */
    public void setPasswordResetToken(String token, int expiryHours) {
        this.passwordResetToken = token;
        this.passwordResetTokenExpiry = LocalDateTime.now().plusHours(expiryHours);
    }

    /**
     * 清除密码重置令牌
     */
    public void clearPasswordResetToken() {
        this.passwordResetToken = null;
        this.passwordResetTokenExpiry = null;
    }

    /**
     * 检查密码重置令牌是否有效
     */
    public boolean isPasswordResetTokenValid(String token) {
        return this.passwordResetToken != null 
            && this.passwordResetToken.equals(token)
            && this.passwordResetTokenExpiry != null
            && this.passwordResetTokenExpiry.isAfter(LocalDateTime.now());
    }
}