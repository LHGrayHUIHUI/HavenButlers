package com.haven.account.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Account Service 核心配置类
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Configuration
public class AccountServiceConfiguration {

    /**
     * 密码编码器 - 使用BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}