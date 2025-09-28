package com.haven.admin.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.core.annotation.Order;

/**
 * Spring Boot Admin安全配置
 *
 * 认证方案（内网运维面板）:
 * - 🔌 API调用: HTTP Basic认证 (Stateless) - curl -u admin:admin123
 * - 🖥️ 运维管理: 浏览器访问 http://localhost:8888 (Form登录+会话)
 * - 📊 监控采集: 无认证端点 /actuator/health|info|prometheus|metrics
 * - 🔧 客户端注册: Basic认证 POST /instances
 *
 * 双链设计避免base-model JWT冲突:
 * - API链(@Order(1)): /api/** 使用 Basic + Stateless
 * - UI链(默认): Admin UI 使用 Form + Basic + 会话
 *
 * @author HavenButler
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AdminServerProperties adminServerProperties;
    private final SecurityProperties security;

    public SecurityConfig(AdminServerProperties adminServerProperties, SecurityProperties security) {
        this.adminServerProperties = adminServerProperties;
        this.security = security;
    }

    /**
     * API专用安全过滤链 - 最高优先级
     *
     * 处理 /api/** 路径，使用 HTTP Basic 认证 + Stateless
     * 避免与 base-model JWT 拦截器冲突
     */
    @Bean
    @Order(0)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
            .securityMatcher(new AntPathRequestMatcher("/api/**"))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .build();
    }

    /**
     * UI专用安全过滤链 - 默认优先级
     *
     * 处理 Admin UI 界面，支持 Form 登录 + HTTP Basic + 会话管理
     */
    @Bean
    protected SecurityFilterChain uiSecurityFilterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler =
            new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(this.adminServerProperties.path("/"));

        return http
            .authorizeHttpRequests(authz -> authz
                // 静态资源无需认证
                .requestMatchers(new AntPathRequestMatcher(this.adminServerProperties.path("/assets/**"))).permitAll()
                .requestMatchers(new AntPathRequestMatcher(this.adminServerProperties.path("/login"))).permitAll()

                // 监控端点无需认证 - 支持Prometheus采集
                .requestMatchers(new AntPathRequestMatcher("/actuator/health")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/actuator/info")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/actuator/prometheus")).permitAll()
                .requestMatchers(new AntPathRequestMatcher("/actuator/metrics")).permitAll()

                // Spring Boot Admin客户端注册 - 需要Basic认证
                .requestMatchers(new AntPathRequestMatcher("/instances", "POST")).authenticated()
                .requestMatchers(new AntPathRequestMatcher("/instances/*", "DELETE")).authenticated()

                // UI界面需要认证 - 支持Form+Basic登录
                .requestMatchers(new AntPathRequestMatcher("/ui/**")).authenticated()
                .requestMatchers(new AntPathRequestMatcher(this.adminServerProperties.path("/**"))).authenticated()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage(this.adminServerProperties.path("/login"))
                .successHandler(successHandler)
            )
            .logout(logout -> logout
                .logoutUrl(this.adminServerProperties.path("/logout"))
            )
            .httpBasic(Customizer.withDefaults())
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(
                    new AntPathRequestMatcher(this.adminServerProperties.path("/instances"), "POST"),
                    new AntPathRequestMatcher(this.adminServerProperties.path("/instances/*"), "DELETE"),
                    new AntPathRequestMatcher(this.adminServerProperties.path("/actuator/**"))
                )
            )
            .rememberMe(remember -> remember
                .key("haven-admin")
                .tokenValiditySeconds(1209600)
            )
            .build();
    }

    /**
     * UI专用安全过滤链说明
     *
     * 处理Spring Boot Admin界面和客户端注册
     * - Form登录：浏览器访问Admin控制台
     * - HTTP Basic：客户端注册和API调用备用
     * - 会话管理：支持"记住我"功能
     */

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails user = User.builder()
            .username(security.getUser().getName())
            .password(passwordEncoder.encode(security.getUser().getPassword()))
            .roles("ADMIN")
            .build();
        return new InMemoryUserDetailsManager(user);
    }
}
