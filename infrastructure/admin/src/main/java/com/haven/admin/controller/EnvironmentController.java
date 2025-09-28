package com.haven.admin.controller;

import com.haven.admin.common.AdminResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.refresh.ContextRefresher;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 环境切换控制器
 * 提供动态环境切换功能
 */
@Slf4j
@RestController
@RequestMapping("/api/environment")
@RequiredArgsConstructor
public class EnvironmentController {

    private final ContextRefresher contextRefresher;

    @Value("${environment:dev}")
    private String currentEnvironment;

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * 获取当前环境信息
     */
    @GetMapping("/current")
    public AdminResponse<Map<String, Object>> getCurrentEnvironment() {
        Map<String, Object> envInfo = new HashMap<>();
        envInfo.put("environment", currentEnvironment);
        envInfo.put("service", applicationName);
        envInfo.put("timestamp", System.currentTimeMillis());

        return AdminResponse.success(envInfo);
    }

    /**
     * 获取所有可用环境
     */
    @GetMapping("/available")
    public AdminResponse<Map<String, Object>> getAvailableEnvironments() {
        Map<String, Object> environments = new HashMap<>();

        // 开发环境
        Map<String, Object> dev = new HashMap<>();
        dev.put("name", "开发环境");
        dev.put("description", "用于开发和调试");
        dev.put("database", "postgres-dev");
        dev.put("logLevel", "DEBUG");
        dev.put("alertEnabled", false);
        environments.put("dev", dev);

        // 测试环境
        Map<String, Object> test = new HashMap<>();
        test.put("name", "测试环境");
        test.put("description", "用于功能测试和集成测试");
        test.put("database", "postgres-test");
        test.put("logLevel", "INFO");
        test.put("alertEnabled", true);
        environments.put("test", test);

        // 生产环境
        Map<String, Object> prod = new HashMap<>();
        prod.put("name", "生产环境");
        prod.put("description", "正式生产环境");
        prod.put("database", "postgres-prod");
        prod.put("logLevel", "WARN");
        prod.put("alertEnabled", true);
        environments.put("prod", prod);

        Map<String, Object> result = new HashMap<>();
        result.put("current", currentEnvironment);
        result.put("environments", environments);

        return AdminResponse.success(result);
    }

    /**
     * 切换环境（通过环境变量）
     * 注意：这个功能需要重启容器才能生效
     */
    @PostMapping("/switch/{environment}")
    public AdminResponse<Map<String, String>> switchEnvironment(@PathVariable String environment) {
        log.info("请求切换环境: {} -> {}", currentEnvironment, environment);

        Map<String, String> result = new HashMap<>();
        result.put("message", "环境切换需要重启服务");
        result.put("instruction", "请设置环境变量 ENVIRONMENT=" + environment + " 并重启容器");
        result.put("dockerCommand", "docker run -e ENVIRONMENT=" + environment + " " + applicationName);

        return AdminResponse.success(result);
    }

    /**
     * 刷新配置（从Nacos重新加载配置）
     */
    @PostMapping("/refresh")
    public AdminResponse<Map<String, String>> refreshConfig() {
        log.info("刷新配置，当前环境: {}", currentEnvironment);

        try {
            // 刷新Spring Context，重新从Nacos加载配置
            contextRefresher.refresh();

            Map<String, String> result = new HashMap<>();
            result.put("message", "配置刷新成功");
            result.put("environment", currentEnvironment);
            result.put("timestamp", String.valueOf(System.currentTimeMillis()));

            log.info("配置刷新成功");
            return AdminResponse.success(result);

        } catch (Exception e) {
            log.error("配置刷新失败", e);

            Map<String, String> result = new HashMap<>();
            result.put("message", "配置刷新失败");
            result.put("error", e.getMessage());

            return AdminResponse.error(500, "配置刷新失败: " + e.getMessage(), result);
        }
    }

    /**
     * 获取当前配置信息
     */
    @GetMapping("/config")
    public AdminResponse<Map<String, Object>> getCurrentConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("environment", currentEnvironment);
        config.put("service", applicationName);

        // 可以添加更多配置信息展示
        // 注意：不要暴露敏感信息如密码等

        return AdminResponse.success(config);
    }
}