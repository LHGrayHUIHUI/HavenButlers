package com.haven.admin.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * 生产环境安全启动校验器
 *
 * 功能：
 * - 检查生产环境是否使用默认凭证（admin123）
 * - 检查密码复杂度
 * - 如果校验失败，拒绝启动应用
 *
 * 注意：Admin 服务使用 Spring Security 基础认证（账户名密码），不使用 JWT
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
@Configuration
public class SecurityStartupValidator {

    @Value("${spring.security.user.password:}")
    private String userPassword;

    @Value("${ENVIRONMENT:local}")
    private String environment;

    private final Environment env;

    public SecurityStartupValidator(Environment env) {
        this.env = env;
    }

    /**
     * 应用启动时执行安全校验
     *
     * @throws IllegalStateException 如果安全校验失败
     */
    @PostConstruct
    public void validateSecurityConfig() {
        log.info("=== 开始安全配置校验 ===");
        log.info("当前环境: {}", environment);
        log.info("激活的 Profiles: {}", Arrays.toString(env.getActiveProfiles()));

        boolean isProduction = isProductionEnvironment();

        if (isProduction) {
            log.warn("⚠️ 检测到生产环境，执行严格的安全校验...");

            // 校验用户密码
            validateUserPassword();

            log.info("✅ 生产环境安全校验通过");
        } else {
            log.info("ℹ️ 开发环境，跳过严格安全校验");

            // 开发环境也给出警告
            if (isDefaultPassword(userPassword)) {
                log.warn("⚠️ 开发环境使用默认密码，生产环境请务必修改！");
            }
        }

        log.info("=== 安全配置校验完成 ===");
    }

    /**
     * 判断是否为生产环境
     *
     * 判断依据：
     * - ENVIRONMENT 环境变量包含 prod
     * - 或 Spring Profiles 包含 prod
     *
     * @return true=生产环境，false=非生产环境
     */
    private boolean isProductionEnvironment() {
        // 检查 ENVIRONMENT 环境变量
        if (environment != null && environment.toLowerCase().contains("prod")) {
            return true;
        }

        // 检查 Spring Profiles
        String[] activeProfiles = env.getActiveProfiles();
        for (String profile : activeProfiles) {
            if (profile.toLowerCase().contains("prod")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 校验用户密码强度
     *
     * @throws IllegalStateException 如果密码不符合要求
     */
    private void validateUserPassword() {
        // 1. 检查是否为默认密码
        if (isDefaultPassword(userPassword)) {
            throw new IllegalStateException(
                    "❌ 生产环境禁止使用默认密码 'admin123'！\n" +
                    "请通过环境变量 SPRING_SECURITY_USER_PASSWORD 设置强密码。"
            );
        }

        // 2. 检查密码长度
        if (userPassword == null || userPassword.length() < 12) {
            throw new IllegalStateException(
                    "❌ 生产环境密码长度必须 ≥ 12 位！\n" +
                    "当前密码长度: " + (userPassword == null ? 0 : userPassword.length()) + "\n" +
                    "请通过环境变量 SPRING_SECURITY_USER_PASSWORD 设置强密码。"
            );
        }

        // 3. 检查密码复杂度（必须包含大小写字母、数字、特殊字符）
        if (!isStrongPassword(userPassword)) {
            throw new IllegalStateException(
                    "❌ 生产环境密码强度不足！\n" +
                    "密码必须包含：\n" +
                    "  - 至少1个大写字母\n" +
                    "  - 至少1个小写字母\n" +
                    "  - 至少1个数字\n" +
                    "  - 至少1个特殊字符 (!@#$%^&*)\n" +
                    "请通过环境变量 SPRING_SECURITY_USER_PASSWORD 设置强密码。"
            );
        }

        log.info("✅ 用户密码强度校验通过");
    }

    /**
     * 检查是否为默认密码
     *
     * @param password 待检查的密码
     * @return true=默认密码，false=非默认密码
     */
    private boolean isDefaultPassword(String password) {
        if (password == null) {
            return false;
        }

        String[] defaultPasswords = {
                "admin123",
                "admin",
                "password",
                "123456",
                "Admin123"
        };

        for (String defaultPassword : defaultPasswords) {
            if (defaultPassword.equalsIgnoreCase(password)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检查密码强度
     *
     * 要求：
     * - 包含至少1个大写字母
     * - 包含至少1个小写字母
     * - 包含至少1个数字
     * - 包含至少1个特殊字符
     *
     * @param password 待检查的密码
     * @return true=强密码，false=弱密码
     */
    private boolean isStrongPassword(String password) {
        if (password == null || password.length() < 12) {
            return false;
        }

        // 正则表达式：至少包含大写字母、小写字母、数字、特殊字符各一个
        Pattern pattern = Pattern.compile(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]).+$"
        );

        return pattern.matcher(password).matches();
    }
}