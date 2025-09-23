package com.haven.base.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 动态服务配置测试
 *
 * @author HavenButler
 */
@SpringBootTest
@TestPropertySource(properties = {
    "haven.services.storage.url=http://test-storage:9999",
    "haven.services.storage.timeout=8000",
    "haven.services.storage.enabled=false",
    "haven.services.account.url=http://test-account:9998"
})
class DynamicServiceConfigTest {

    @Test
    void testServiceConfigDefaultValues() {
        DynamicServiceConfig config = new DynamicServiceConfig();

        // 测试默认值
        assertEquals("http://storage-service:8081", config.getStorage().getUrl());
        assertEquals(5000, config.getStorage().getTimeout());
        assertEquals(3, config.getStorage().getRetryCount());
        assertTrue(config.getStorage().isEnabled());

        assertEquals("http://account-service:8082", config.getAccount().getUrl());
        assertEquals(5000, config.getAccount().getTimeout());
        assertEquals(3, config.getAccount().getRetryCount());
        assertTrue(config.getAccount().isEnabled());
    }

    @Test
    void testGetServiceUrl() {
        DynamicServiceConfig config = new DynamicServiceConfig();

        // 测试获取服务URL
        assertEquals("http://storage-service:8081", config.getServiceUrl("storage"));
        assertEquals("http://account-service:8082", config.getServiceUrl("account"));
        assertEquals("http://gateway:8080", config.getServiceUrl("gateway"));
        assertEquals("http://nlp-service:8085", config.getServiceUrl("nlp"));
        assertEquals("http://ai-service:8084", config.getServiceUrl("ai"));
        assertEquals("http://file-manager-service:8086", config.getServiceUrl("file-manager"));
        assertEquals("http://message-service:8083", config.getServiceUrl("message"));
    }

    @Test
    void testGetServiceUrlCaseInsensitive() {
        DynamicServiceConfig config = new DynamicServiceConfig();

        // 测试大小写不敏感
        assertEquals("http://storage-service:8081", config.getServiceUrl("Storage"));
        assertEquals("http://storage-service:8081", config.getServiceUrl("STORAGE"));
        assertEquals("http://account-service:8082", config.getServiceUrl("Account"));
        assertEquals("http://account-service:8082", config.getServiceUrl("ACCOUNT"));
    }

    @Test
    void testGetServiceUrlWithInvalidName() {
        DynamicServiceConfig config = new DynamicServiceConfig();

        // 测试无效服务名
        assertThrows(IllegalArgumentException.class, () -> {
            config.getServiceUrl("invalid-service");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            config.getServiceUrl("");
        });

        assertThrows(IllegalArgumentException.class, () -> {
            config.getServiceUrl(null);
        });
    }

    @Test
    void testIsServiceEnabled() {
        DynamicServiceConfig config = new DynamicServiceConfig();

        // 测试服务启用状态
        assertTrue(config.isServiceEnabled("storage"));
        assertTrue(config.isServiceEnabled("account"));
        assertTrue(config.isServiceEnabled("gateway"));
        assertTrue(config.isServiceEnabled("nlp"));
        assertTrue(config.isServiceEnabled("ai"));
        assertTrue(config.isServiceEnabled("file-manager"));
        assertTrue(config.isServiceEnabled("message"));

        // 测试无效服务名
        assertFalse(config.isServiceEnabled("invalid-service"));
        assertFalse(config.isServiceEnabled(""));
        assertFalse(config.isServiceEnabled(null));
    }

    @Test
    void testServiceConfigModification() {
        DynamicServiceConfig config = new DynamicServiceConfig();

        // 测试修改配置
        config.getStorage().setUrl("http://new-storage:8888");
        config.getStorage().setTimeout(10000);
        config.getStorage().setEnabled(false);

        assertEquals("http://new-storage:8888", config.getStorage().getUrl());
        assertEquals(10000, config.getStorage().getTimeout());
        assertFalse(config.getStorage().isEnabled());

        // 验证通过getServiceUrl方法获取的是新值
        assertEquals("http://new-storage:8888", config.getServiceUrl("storage"));
        assertFalse(config.isServiceEnabled("storage"));
    }

    @Test
    void testAllServiceUrls() {
        DynamicServiceConfig config = new DynamicServiceConfig();

        // 验证所有服务都有正确的默认URL
        String[] services = {"storage", "account", "gateway", "nlp", "ai", "file-manager", "message"};

        for (String service : services) {
            String url = config.getServiceUrl(service);
            assertNotNull(url, "服务 " + service + " 的URL不能为空");
            assertTrue(url.startsWith("http://"), "服务 " + service + " 的URL必须以http://开头");
            assertTrue(url.contains(":"), "服务 " + service + " 的URL必须包含端口号");
        }
    }

    @Test
    void testServiceConfigurationConsistency() {
        DynamicServiceConfig config = new DynamicServiceConfig();

        // 验证配置的一致性
        assertNotNull(config.getStorage());
        assertNotNull(config.getAccount());
        assertNotNull(config.getGateway());
        assertNotNull(config.getNlp());
        assertNotNull(config.getAi());
        assertNotNull(config.getFileManager());
        assertNotNull(config.getMessage());

        // 验证所有服务的基本配置都不为空
        assertTrue(config.getStorage().getTimeout() > 0);
        assertTrue(config.getStorage().getRetryCount() > 0);

        assertTrue(config.getAccount().getTimeout() > 0);
        assertTrue(config.getAccount().getRetryCount() > 0);
    }
}