package com.haven.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ID生成器
 * 支持多种ID生成策略
 */
@Slf4j
public class IdGenerator {

    /**
     * 雪花算法起始时间戳 (2024-01-01 00:00:00)
     */
    private static final long START_TIMESTAMP = 1704067200000L;

    /**
     * 机器ID所占位数
     */
    private static final long WORKER_ID_BITS = 5L;

    /**
     * 数据中心ID所占位数
     */
    private static final long DATACENTER_ID_BITS = 5L;

    /**
     * 序列号所占位数
     */
    private static final long SEQUENCE_BITS = 12L;

    /**
     * 机器ID最大值
     */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);

    /**
     * 数据中心ID最大值
     */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    /**
     * 序列号最大值
     */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /**
     * 机器ID左移位数
     */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;

    /**
     * 数据中心ID左移位数
     */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;

    /**
     * 时间戳左移位数
     */
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    /**
     * 数据中心ID
     */
    private final long datacenterId;

    /**
     * 机器ID
     */
    private final long workerId;

    /**
     * 序列号
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间戳
     */
    private long lastTimestamp = -1L;

    /**
     * 单例实例
     */
    private static volatile IdGenerator instance;

    /**
     * 构造函数
     */
    private IdGenerator(long datacenterId, long workerId) {
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("数据中心ID不能大于" + MAX_DATACENTER_ID + "或小于0");
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("机器ID不能大于" + MAX_WORKER_ID + "或小于0");
        }
        this.datacenterId = datacenterId;
        this.workerId = workerId;
    }

    /**
     * 获取单例实例
     */
    public static IdGenerator getInstance() {
        if (instance == null) {
            synchronized (IdGenerator.class) {
                if (instance == null) {
                    // 根据机器IP生成workerId
                    long workerId = getWorkerId();
                    long datacenterId = getDatacenterId();
                    instance = new IdGenerator(datacenterId, workerId);
                    log.info("ID生成器初始化: datacenterId={}, workerId={}", datacenterId, workerId);
                }
            }
        }
        return instance;
    }

    /**
     * 生成雪花算法ID
     */
    public synchronized long nextId() {
        long timestamp = currentTimeMillis();

        // 如果时钟回拨
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成ID");
        }

        // 如果是同一毫秒内
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            // 序列号溢出
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 不同毫秒内，序列号重置
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        // 移位并通过或运算拼接成64位ID
        return ((timestamp - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    /**
     * 生成字符串格式的雪花ID
     */
    public String nextIdStr() {
        return String.valueOf(nextId());
    }

    /**
     * 生成带前缀的ID
     */
    public String nextIdStr(String prefix) {
        return prefix + nextId();
    }

    /**
     * 生成订单号
     * 格式: yyyyMMddHHmmssSSS + 4位随机数
     */
    public static String generateOrderNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        String timestamp = LocalDateTime.now().format(formatter);
        String random = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return timestamp + random;
    }

    /**
     * 生成设备ID
     * 格式: DEV + yyyyMMdd + 6位随机字母数字
     */
    public static String generateDeviceId() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String date = LocalDateTime.now().format(formatter);
        String random = RandomStringUtils.randomAlphanumeric(6).toUpperCase();
        return "DEV" + date + random;
    }

    /**
     * 生成会话ID
     * 格式: SID + 时间戳 + 8位随机字符串
     */
    public static String generateSessionId() {
        long timestamp = System.currentTimeMillis();
        String random = RandomStringUtils.randomAlphanumeric(8);
        return "SID" + timestamp + random;
    }

    /**
     * 生成短链接ID
     * 格式: 8位随机字母数字
     */
    public static String generateShortId() {
        return RandomStringUtils.randomAlphanumeric(8);
    }

    /**
     * 生成UUID（去掉横线）
     */
    public static String generateUuid() {
        return java.util.UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 阻塞到下一毫秒
     */
    private long tilNextMillis(long lastTimestamp) {
        long timestamp = currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = currentTimeMillis();
        }
        return timestamp;
    }

    /**
     * 获取当前时间戳
     */
    private long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * 根据IP地址生成workerId
     */
    private static long getWorkerId() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            byte[] ipBytes = address.getAddress();
            // 使用IP地址最后两个字节生成workerId
            return ((ipBytes[ipBytes.length - 2] & 0xFF) << 8 | (ipBytes[ipBytes.length - 1] & 0xFF)) % (MAX_WORKER_ID + 1);
        } catch (Exception e) {
            log.warn("获取IP地址失败，使用随机workerId", e);
            return ThreadLocalRandom.current().nextLong(0, MAX_WORKER_ID + 1);
        }
    }

    /**
     * 获取数据中心ID
     */
    private static long getDatacenterId() {
        // 可以根据配置或环境变量设置
        String dcId = System.getProperty("datacenter.id", "1");
        try {
            return Long.parseLong(dcId);
        } catch (NumberFormatException e) {
            log.warn("数据中心ID配置错误，使用默认值1");
            return 1L;
        }
    }

    /**
     * 简单自增ID生成器
     */
    public static class SimpleIdGenerator {
        private static final AtomicLong counter = new AtomicLong(0);

        public static long nextId() {
            return counter.incrementAndGet();
        }

        public static String nextIdStr(String prefix) {
            return prefix + counter.incrementAndGet();
        }

        public static void reset() {
            counter.set(0);
        }
    }
}