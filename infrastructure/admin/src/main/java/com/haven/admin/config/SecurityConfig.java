package com.haven.admin.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Boot Admin安全配置
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

    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler =
            new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl(this.adminServerProperties.path("/"));

        return http
            .authorizeHttpRequests(authz -> authz
                .requestMatchers(new AntPathRequestMatcher(this.adminServerProperties.path("/assets/**"))).permitAll()
                .requestMatchers(new AntPathRequestMatcher(this.adminServerProperties.path("/actuator/info"))).permitAll()
                .requestMatchers(new AntPathRequestMatcher(this.adminServerProperties.path("/actuator/health"))).permitAll()
                .requestMatchers(new AntPathRequestMatcher(this.adminServerProperties.path("/login"))).permitAll()
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

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails user = User.withDefaultPasswordEncoder()
            .username(security.getUser().getName())
            .password(security.getUser().getPassword())
            .roles("ADMIN")
            .build();
        return new InMemoryUserDetailsManager(user);
    }
}