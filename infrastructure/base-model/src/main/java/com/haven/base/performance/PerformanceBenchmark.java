package com.haven.base.performance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 性能基准测试框架
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class PerformanceBenchmark {

    /**
     * 性能基准配置
     */
    public static class BenchmarkConfig {
        public int threadCount = 10;
        public int requestsPerThread = 100;
        public int warmupRequests = 50;
        public long timeoutMs = 5000;
        public boolean enableDetailedMetrics = true;
        public String testDescription = "";
    }

    /**
     * 性能测试接口
     */
    @FunctionalInterface
    public interface PerformanceTest {
        /**
         * 执行测试操作
         * @return 操作耗时（毫秒）
         * @throws Exception 测试异常
         */
        long execute() throws Exception;
    }

    /**
     * 执行性能基准测试
     *
     * @param testName 测试名称
     * @param test 测试操作
     * @param config 测试配置
     * @return 测试结果
     */
    public PerformanceTestResult runBenchmark(String testName, PerformanceTest test, BenchmarkConfig config) {
        log.info("开始性能基准测试: {}", testName);

        PerformanceTestResult.PerformanceTestResultBuilder resultBuilder = PerformanceTestResult.builder()
                .testName(testName)
                .startTime(LocalDateTime.now())
                .threadCount(config.threadCount);

        List<Long> responseTimes = new CopyOnWriteArrayList<>();
        AtomicLong successCount = new AtomicLong(0);
        AtomicLong failureCount = new AtomicLong(0);
        AtomicInteger activeThreads = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(config.threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(config.threadCount);

        // 预热阶段
        log.info("执行预热测试...");
        runWarmup(test, config.warmupRequests);

        // 正式测试阶段
        log.info("开始正式性能测试...");
        long testStartTime = System.currentTimeMillis();

        for (int i = 0; i < config.threadCount; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    activeThreads.incrementAndGet();
                    startLatch.await(); // 等待统一开始信号

                    for (int j = 0; j < config.requestsPerThread; j++) {
                        try {
                            long startTime = System.nanoTime();
                            test.execute();
                            long responseTime = (System.nanoTime() - startTime) / 1_000_000; // 转换为毫秒

                            responseTimes.add(responseTime);
                            successCount.incrementAndGet();

                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            log.debug("测试请求失败 (线程{}-请求{}): {}", threadIndex, j, e.getMessage());
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    activeThreads.decrementAndGet();
                    finishLatch.countDown();
                }
            });
        }

        // 开始测试
        startLatch.countDown();

        try {
            // 等待所有线程完成或超时
            boolean completed = finishLatch.await(config.timeoutMs * config.requestsPerThread, TimeUnit.MILLISECONDS);

            if (!completed) {
                log.warn("测试未在预期时间内完成，强制结束");
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("性能测试被中断", e);
        } finally {
            executor.shutdown();
        }

        long testEndTime = System.currentTimeMillis();
        long totalExecutionTime = testEndTime - testStartTime;

        // 收集系统资源使用情况
        PerformanceTestResult.SystemResourceUsage resourceUsage = collectSystemResourceUsage();

        // 计算统计指标
        Collections.sort(responseTimes);
        long totalRequests = successCount.get() + failureCount.get();
        double averageResponseTime = responseTimes.isEmpty() ? 0 :
            responseTimes.stream().mapToLong(Long::longValue).average().orElse(0);

        PerformanceTestResult result = resultBuilder
                .endTime(LocalDateTime.now())
                .totalExecutionTimeMs(totalExecutionTime)
                .totalRequests(totalRequests)
                .successfulRequests(successCount.get())
                .failedRequests(failureCount.get())
                .averageResponseTimeMs(averageResponseTime)
                .minResponseTimeMs(responseTimes.isEmpty() ? 0 : responseTimes.get(0))
                .maxResponseTimeMs(responseTimes.isEmpty() ? 0 : responseTimes.get(responseTimes.size() - 1))
                .p50ResponseTimeMs(calculatePercentile(responseTimes, 50))
                .p95ResponseTimeMs(calculatePercentile(responseTimes, 95))
                .p99ResponseTimeMs(calculatePercentile(responseTimes, 99))
                .throughput(totalRequests > 0 ? (double) totalRequests / (totalExecutionTime / 1000.0) : 0)
                .errorRate(totalRequests > 0 ? (double) failureCount.get() / totalRequests * 100 : 0)
                .concurrentConnections(config.threadCount)
                .resourceUsage(resourceUsage)
                .grade(calculatePerformanceGrade(resultBuilder.build()))
                .passedBenchmark(checkPerformanceBenchmark(resultBuilder.build()))
                .build();

        // 生成建议
        generateRecommendations(result);

        log.info("性能基准测试完成: {}", result.getSummary());
        return result;
    }

    /**
     * 预热测试
     */
    private void runWarmup(PerformanceTest test, int warmupRequests) {
        try {
            for (int i = 0; i < warmupRequests; i++) {
                try {
                    test.execute();
                } catch (Exception e) {
                    // 预热阶段忽略异常
                }
            }
            log.info("预热测试完成，执行了 {} 次操作", warmupRequests);
        } catch (Exception e) {
            log.warn("预热测试失败", e);
        }
    }

    /**
     * 收集系统资源使用情况
     */
    private PerformanceTestResult.SystemResourceUsage collectSystemResourceUsage() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        long totalGcTime = 0;
        long totalGcCount = 0;
        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            totalGcTime += gcBean.getCollectionTime();
            totalGcCount += gcBean.getCollectionCount();
        }

        return PerformanceTestResult.SystemResourceUsage.builder()
                .memoryUsageMB(memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024)
                .heapMemoryUsageMB(memoryBean.getHeapMemoryUsage().getUsed() / 1024 / 1024)
                .nonHeapMemoryUsageMB(memoryBean.getNonHeapMemoryUsage().getUsed() / 1024 / 1024)
                .threadCount(threadBean.getThreadCount())
                .gcCount(totalGcCount)
                .gcTimeMs(totalGcTime)
                .build();
    }

    /**
     * 计算百分位数
     */
    private double calculatePercentile(List<Long> sortedList, int percentile) {
        if (sortedList.isEmpty()) return 0;

        int index = (int) Math.ceil(percentile / 100.0 * sortedList.size()) - 1;
        index = Math.max(0, Math.min(index, sortedList.size() - 1));
        return sortedList.get(index);
    }

    /**
     * 计算性能等级
     */
    private PerformanceTestResult.PerformanceGrade calculatePerformanceGrade(PerformanceTestResult result) {
        double avgResponseTime = result.getAverageResponseTimeMs();
        double p95ResponseTime = result.getP95ResponseTimeMs();
        double errorRate = result.getErrorRate();
        double throughput = result.getThroughput();

        // 优秀：平均响应时间 < 100ms，P95 < 200ms，错误率 < 0.1%，吞吐量 > 1000 req/s
        if (avgResponseTime < 100 && p95ResponseTime < 200 && errorRate < 0.1 && throughput > 1000) {
            return PerformanceTestResult.PerformanceGrade.EXCELLENT;
        }

        // 良好：平均响应时间 < 200ms，P95 < 500ms，错误率 < 0.5%，吞吐量 > 500 req/s
        if (avgResponseTime < 200 && p95ResponseTime < 500 && errorRate < 0.5 && throughput > 500) {
            return PerformanceTestResult.PerformanceGrade.GOOD;
        }

        // 可接受：平均响应时间 < 500ms，P95 < 1000ms，错误率 < 1%，吞吐量 > 100 req/s
        if (avgResponseTime < 500 && p95ResponseTime < 1000 && errorRate < 1 && throughput > 100) {
            return PerformanceTestResult.PerformanceGrade.ACCEPTABLE;
        }

        // 差：平均响应时间 < 1000ms，P95 < 2000ms，错误率 < 5%，吞吐量 > 10 req/s
        if (avgResponseTime < 1000 && p95ResponseTime < 2000 && errorRate < 5 && throughput > 10) {
            return PerformanceTestResult.PerformanceGrade.POOR;
        }

        // 严重：其他情况
        return PerformanceTestResult.PerformanceGrade.CRITICAL;
    }

    /**
     * 检查是否通过性能基准
     */
    private boolean checkPerformanceBenchmark(PerformanceTestResult result) {
        // 基准要求：平均响应时间 < 2秒，错误率 < 1%，吞吐量 > 10 req/s
        return result.getAverageResponseTimeMs() < 2000
            && result.getErrorRate() < 1.0
            && result.getThroughput() > 10;
    }

    /**
     * 生成性能优化建议
     */
    private void generateRecommendations(PerformanceTestResult result) {
        if (result.getAverageResponseTimeMs() > 1000) {
            result.addRecommendation("平均响应时间过高，建议优化算法或增加缓存");
        }

        if (result.getP95ResponseTimeMs() > 2000) {
            result.addRecommendation("P95响应时间过高，存在性能抖动，建议检查慢查询");
        }

        if (result.getErrorRate() > 1.0) {
            result.addRecommendation("错误率过高，建议检查异常处理和系统稳定性");
        }

        if (result.getThroughput() < 100) {
            result.addRecommendation("吞吐量较低，建议优化并发处理能力");
        }

        PerformanceTestResult.SystemResourceUsage usage = result.getResourceUsage();
        if (usage != null && usage.getMemoryUsageMB() > 1024) {
            result.addRecommendation("内存使用量过高，建议优化内存使用或增加堆内存");
        }

        if (usage != null && usage.getGcTimeMs() > result.getTotalExecutionTimeMs() * 0.1) {
            result.addRecommendation("GC时间占比过高，建议优化对象创建和内存分配");
        }
    }

    /**
     * 简化的性能测试方法
     */
    public PerformanceTestResult runSimpleBenchmark(String testName, PerformanceTest test) {
        BenchmarkConfig config = new BenchmarkConfig();
        config.testDescription = "简单性能测试";
        return runBenchmark(testName, test, config);
    }
}