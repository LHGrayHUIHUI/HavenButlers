package com.haven.base.performance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 性能测试套件
 * 集成了 HavenButler 平台核心组件的性能测试
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class PerformanceTestSuite {

    @Autowired
    private PerformanceBenchmark performanceBenchmark;

    /**
     * 运行完整的性能测试套件
     *
     * @return 所有测试结果
     */
    public List<PerformanceTestResult> runFullTestSuite() {
        log.info("开始执行 HavenButler 性能测试套件");
        List<PerformanceTestResult> results = new ArrayList<>();

        try {
            // 1. TraceID生成性能测试
            results.add(runTraceIdGenerationTest());

            // 2. JSON序列化/反序列化性能测试
            results.add(runJsonSerializationTest());

            // 3. 加密/解密性能测试
            results.add(runEncryptionTest());

            // 4. 缓存操作性能测试
            results.add(runCacheOperationTest());

            // 5. 响应包装器性能测试
            results.add(runResponseWrapperTest());

            // 6. 国际化消息获取性能测试
            results.add(runI18nMessageTest());

            // 7. 并发性能测试
            results.add(runConcurrencyTest());

            // 8. 内存分配性能测试
            results.add(runMemoryAllocationTest());

        } catch (Exception e) {
            log.error("性能测试套件执行失败", e);
        }

        log.info("性能测试套件执行完成，共执行 {} 项测试", results.size());
        return results;
    }

    /**
     * TraceID生成性能测试
     */
    private PerformanceTestResult runTraceIdGenerationTest() {
        log.info("执行 TraceID 生成性能测试");

        PerformanceBenchmark.BenchmarkConfig config = new PerformanceBenchmark.BenchmarkConfig();
        config.threadCount = 20;
        config.requestsPerThread = 1000;
        config.testDescription = "TraceID生成性能测试";

        return performanceBenchmark.runBenchmark("TraceID生成测试", () -> {
            long startTime = System.nanoTime();

            // 模拟TraceID生成逻辑
            String traceId = generateTraceId();

            long endTime = System.nanoTime();
            return (endTime - startTime) / 1_000_000; // 转换为毫秒
        }, config);
    }

    /**
     * JSON序列化/反序列化性能测试
     */
    private PerformanceTestResult runJsonSerializationTest() {
        log.info("执行 JSON 序列化/反序列化性能测试");

        // 创建测试数据
        TestData testData = TestData.builder()
                .id(UUID.randomUUID().toString())
                .name("性能测试数据")
                .timestamp(System.currentTimeMillis())
                .value(Math.random() * 1000)
                .active(true)
                .tags(List.of("performance", "test", "benchmark"))
                .build();

        PerformanceBenchmark.BenchmarkConfig config = new PerformanceBenchmark.BenchmarkConfig();
        config.threadCount = 10;
        config.requestsPerThread = 500;
        config.testDescription = "JSON序列化/反序列化性能测试";

        return performanceBenchmark.runBenchmark("JSON序列化测试", () -> {
            long startTime = System.nanoTime();

            // 模拟JSON序列化
            String json = serializeToJson(testData);

            // 模拟JSON反序列化
            TestData deserialized = deserializeFromJson(json, TestData.class);

            long endTime = System.nanoTime();
            return (endTime - startTime) / 1_000_000;
        }, config);
    }

    /**
     * 加密/解密性能测试
     */
    private PerformanceTestResult runEncryptionTest() {
        log.info("执行加密/解密性能测试");

        String testData = "这是一段需要加密的性能测试数据，包含了一些敏感信息。";

        PerformanceBenchmark.BenchmarkConfig config = new PerformanceBenchmark.BenchmarkConfig();
        config.threadCount = 5;
        config.requestsPerThread = 200;
        config.testDescription = "AES加密/解密性能测试";

        return performanceBenchmark.runBenchmark("加密解密测试", () -> {
            long startTime = System.nanoTime();

            // 模拟加密操作
            String encrypted = encryptData(testData);

            // 模拟解密操作
            String decrypted = decryptData(encrypted);

            long endTime = System.nanoTime();
            return (endTime - startTime) / 1_000_000;
        }, config);
    }

    /**
     * 缓存操作性能测试
     */
    private PerformanceTestResult runCacheOperationTest() {
        log.info("执行缓存操作性能测试");

        PerformanceBenchmark.BenchmarkConfig config = new PerformanceBenchmark.BenchmarkConfig();
        config.threadCount = 15;
        config.requestsPerThread = 800;
        config.testDescription = "缓存读写性能测试";

        return performanceBenchmark.runBenchmark("缓存操作测试", () -> {
            long startTime = System.nanoTime();

            String key = "cache_key_" + UUID.randomUUID().toString();
            String value = "cache_value_" + System.currentTimeMillis();

            // 模拟缓存写入
            putToCache(key, value);

            // 模拟缓存读取
            String cachedValue = getFromCache(key);

            long endTime = System.nanoTime();
            return (endTime - startTime) / 1_000_000;
        }, config);
    }

    /**
     * 响应包装器性能测试
     */
    private PerformanceTestResult runResponseWrapperTest() {
        log.info("执行响应包装器性能测试");

        PerformanceBenchmark.BenchmarkConfig config = new PerformanceBenchmark.BenchmarkConfig();
        config.threadCount = 25;
        config.requestsPerThread = 1500;
        config.testDescription = "响应包装器创建性能测试";

        return performanceBenchmark.runBenchmark("响应包装器测试", () -> {
            long startTime = System.nanoTime();

            // 模拟创建响应包装器
            TestData data = TestData.builder()
                    .id(UUID.randomUUID().toString())
                    .name("测试数据")
                    .build();

            createSuccessResponse(data);

            long endTime = System.nanoTime();
            return (endTime - startTime) / 1_000_000;
        }, config);
    }

    /**
     * 国际化消息获取性能测试
     */
    private PerformanceTestResult runI18nMessageTest() {
        log.info("执行国际化消息获取性能测试");

        PerformanceBenchmark.BenchmarkConfig config = new PerformanceBenchmark.BenchmarkConfig();
        config.threadCount = 10;
        config.requestsPerThread = 1000;
        config.testDescription = "国际化消息获取性能测试";

        return performanceBenchmark.runBenchmark("国际化消息测试", () -> {
            long startTime = System.nanoTime();

            // 模拟国际化消息获取
            String message = getI18nMessage("success.operation", new Object[0]);
            String error = getI18nMessage("error.10000", new Object[0]);

            long endTime = System.nanoTime();
            return (endTime - startTime) / 1_000_000;
        }, config);
    }

    /**
     * 并发性能测试
     */
    private PerformanceTestResult runConcurrencyTest() {
        log.info("执行并发性能测试");

        PerformanceBenchmark.BenchmarkConfig config = new PerformanceBenchmark.BenchmarkConfig();
        config.threadCount = 50;
        config.requestsPerThread = 200;
        config.testDescription = "高并发性能测试";

        return performanceBenchmark.runBenchmark("并发性能测试", () -> {
            long startTime = System.nanoTime();

            // 模拟并发业务操作
            performConcurrentOperation();

            long endTime = System.nanoTime();
            return (endTime - startTime) / 1_000_000;
        }, config);
    }

    /**
     * 内存分配性能测试
     */
    private PerformanceTestResult runMemoryAllocationTest() {
        log.info("执行内存分配性能测试");

        PerformanceBenchmark.BenchmarkConfig config = new PerformanceBenchmark.BenchmarkConfig();
        config.threadCount = 8;
        config.requestsPerThread = 300;
        config.testDescription = "内存分配性能测试";

        return performanceBenchmark.runBenchmark("内存分配测试", () -> {
            long startTime = System.nanoTime();

            // 模拟内存密集型操作
            performMemoryIntensiveOperation();

            long endTime = System.nanoTime();
            return (endTime - startTime) / 1_000_000;
        }, config);
    }

    // ========== 模拟方法实现 ==========

    private String generateTraceId() {
        return "tr-" + System.currentTimeMillis() + "-" +
               UUID.randomUUID().toString().substring(0, 6);
    }

    private String serializeToJson(Object obj) {
        // 模拟JSON序列化
        return "{\"data\":\"" + obj.toString() + "\"}";
    }

    private <T> T deserializeFromJson(String json, Class<T> clazz) {
        // 模拟JSON反序列化
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }

    private String encryptData(String data) {
        // 模拟AES加密
        return "encrypted_" + Base64.getEncoder().encodeToString(data.getBytes());
    }

    private String decryptData(String encrypted) {
        // 模拟AES解密
        if (encrypted.startsWith("encrypted_")) {
            return new String(Base64.getDecoder().decode(encrypted.substring(10)));
        }
        return encrypted;
    }

    private void putToCache(String key, String value) {
        // 模拟缓存写入操作
        // 在实际实现中，这里会调用真实的缓存服务
    }

    private String getFromCache(String key) {
        // 模拟缓存读取操作
        return "cached_value_for_" + key;
    }

    private void createSuccessResponse(Object data) {
        // 模拟创建成功响应
    }

    private String getI18nMessage(String key, Object[] args) {
        // 模拟国际化消息获取
        switch (key) {
            case "success.operation":
                return "操作成功";
            case "error.10000":
                return "系统内部错误";
            default:
                return "未知消息";
        }
    }

    private void performConcurrentOperation() {
        // 模拟并发业务操作
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1); // 模拟1ms的处理时间
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).join();
    }

    private void performMemoryIntensiveOperation() {
        // 模拟内存密集型操作
        List<byte[]> memoryList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            memoryList.add(new byte[1024]); // 分配1KB内存
        }
        // 模拟使用后释放
        memoryList.clear();
    }

    // ========== 测试数据类 ==========

    public static class TestData {
        private String id;
        private String name;
        private long timestamp;
        private double value;
        private boolean active;
        private List<String> tags;

        public static TestDataBuilder builder() {
            return new TestDataBuilder();
        }

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }

        public static class TestDataBuilder {
            private TestData data = new TestData();

            public TestDataBuilder id(String id) { data.setId(id); return this; }
            public TestDataBuilder name(String name) { data.setName(name); return this; }
            public TestDataBuilder timestamp(long timestamp) { data.setTimestamp(timestamp); return this; }
            public TestDataBuilder value(double value) { data.setValue(value); return this; }
            public TestDataBuilder active(boolean active) { data.setActive(active); return this; }
            public TestDataBuilder tags(List<String> tags) { data.setTags(tags); return this; }

            public TestData build() { return data; }
        }
    }
}