package com.haven.base.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HavenBase 统一配置属性
 * 提供快速配置和高级配置两种模式，大幅降低学习成本
 *
 * @author HavenButler
 */
@Data
@ConfigurationProperties(prefix = "haven.base")
public class HavenBaseProperties {

    /**
     * 快速配置模式 - 一键启用核心功能
     * 新用户推荐使用此模式，配置简单，开箱即用
     */
    private QuickStart quickStart = new QuickStart();

    /**
     * 高级配置模式 - 可选的详细配置
     * 有特殊需求的用户可以使用此模式进行细粒度控制
     */
    private Advanced advanced = new Advanced();

    /**
     * 快速配置模式
     */
    @Data
    public static class QuickStart {
        /**
         * 是否启用快速模式
         * 默认启用，为用户提供开箱即用的体验
         */
        private boolean enabled = true;

        /**
         * 运行环境模式
         * development: 开发环境，启用调试功能
         * testing: 测试环境，启用测试相关功能
         * production: 生产环境，优化性能和安全
         */
        private String profile = "development";

        /**
         * 自动配置缓存服务
         * true: 启用本地缓存 + Redis缓存
         * false: 禁用缓存服务
         */
        private boolean enableCache = true;

        /**
         * 自动配置监控服务
         * true: 启用异常监控和性能监控
         * false: 禁用监控服务
         */
        private boolean enableMonitoring = false;

        /**
         * 自动配置容错保护
         * true: 启用熔断、重试、超时控制
         * false: 禁用容错保护
         */
        private boolean enableResilience = true;

        /**
         * 自动配置安全功能
         * true: 启用密钥管理和加密功能
         * false: 禁用安全功能
         */
        private boolean enableSecurity = true;

        /**
         * 自动配置服务发现
         * true: 启用服务发现和客户端负载均衡
         * false: 禁用服务发现
         */
        private boolean enableServiceDiscovery = false;

        /**
         * 预设模板配置
         * microservice: 微服务模板，启用分布式相关功能
         * monolith: 单体应用模板，启用基础功能
         * high-concurrency: 高并发模板，优化缓存和容错
         * secure: 安全模板，强化安全配置
         */
        private String preset = "microservice";
    }

    /**
     * 高级配置模式
     */
    @Data
    public static class Advanced {
        /**
         * 核心功能配置
         */
        private Core core = new Core();

        /**
         * 缓存配置
         */
        private Cache cache = new Cache();

        /**
         * 容错配置
         */
        private Resilience resilience = new Resilience();

        /**
         * 监控配置
         */
        private Monitoring monitoring = new Monitoring();

        /**
         * 安全配置
         */
        private Security security = new Security();

        /**
         * 服务客户端配置
         */
        private ServiceClient serviceClient = new ServiceClient();

        /**
         * 分布式锁配置
         */
        private DistributedLock distributedLock = new DistributedLock();

        /**
         * 消息队列配置
         */
        private Messaging messaging = new Messaging();

        /**
         * 指标收集配置
         */
        private Metrics metrics = new Metrics();

        /**
         * 核心功能详细配置
         */
        @Data
        public static class Core {
            /**
             * 是否启用TraceID链路追踪
             */
            private boolean enableTraceId = true;

            /**
             * 是否启用全局异常处理
             */
            private boolean enableGlobalException = true;

            /**
             * 是否启用日志追踪切面
             */
            private boolean enableLogAspect = true;

            /**
             * TraceID格式配置
             */
            private String traceIdFormat = "tr-{yyyyMMdd}-{HHmmss}-{random}";
        }

        /**
         * 缓存详细配置
         */
        @Data
        public static class Cache {
            /**
             * 是否启用缓存
             */
            private boolean enabled = true;

            /**
             * 本地缓存配置
             */
            private Local local = new Local();

            /**
             * 分布式缓存配置
             */
            private Distributed distributed = new Distributed();

            @Data
            public static class Local {
                /**
                 * 是否启用本地缓存
                 */
                private boolean enabled = true;

                /**
                 * 本地缓存最大容量
                 */
                private long maximumSize = 10000;

                /**
                 * 写入后过期时间(秒)
                 */
                private long expireAfterWrite = 3600;

                /**
                 * 访问后过期时间(秒)
                 */
                private long expireAfterAccess = 1800;

                /**
                 * 初始容量
                 */
                private int initialCapacity = 100;
            }

            @Data
            public static class Distributed {
                /**
                 * 是否启用分布式缓存
                 */
                private boolean enabled = true;

                /**
                 * 默认TTL时间(秒)
                 */
                private int defaultTtl = 3600;

                /**
                 * 缓存键前缀
                 */
                private String keyPrefix = "haven:cache:";

                /**
                 * 是否启用缓存穿透保护
                 */
                private boolean enableCachePenetrationProtection = true;

                /**
                 * 空值缓存TTL时间(秒)
                 */
                private int nullValueTtl = 300;
            }
        }

        /**
         * 容错详细配置
         */
        @Data
        public static class Resilience {
            /**
             * 是否启用容错
             */
            private boolean enabled = true;

            /**
             * 超时控制配置
             */
            private TimeLimiter timeLimiter = new TimeLimiter();

            /**
             * 重试配置
             */
            private Retry retry = new Retry();

            /**
             * 熔断器配置
             */
            private CircuitBreaker circuitBreaker = new CircuitBreaker();

            @Data
            public static class TimeLimiter {
                /**
                 * 是否启用超时控制
                 */
                private boolean enabled = true;

                /**
                 * 超时时间(毫秒)
                 */
                private long timeoutDuration = 5000;
            }

            @Data
            public static class Retry {
                /**
                 * 是否启用重试
                 */
                private boolean enabled = true;

                /**
                 * 最大重试次数
                 */
                private int maxAttempts = 3;

                /**
                 * 重试间隔(毫秒)
                 */
                private long waitDuration = 1000;
            }

            @Data
            public static class CircuitBreaker {
                /**
                 * 是否启用熔断器
                 */
                private boolean enabled = true;

                /**
                 * 失败率阈值(%)
                 */
                private float failureRateThreshold = 50.0f;

                /**
                 * 熔断器打开状态等待时间(毫秒)
                 */
                private long waitDurationInOpenState = 30000;

                /**
                 * 最小调用次数
                 */
                private int minimumNumberOfCalls = 10;
            }
        }

        /**
         * 监控详细配置
         */
        @Data
        public static class Monitoring {
            /**
             * 是否启用监控
             */
            private boolean enabled = false;

            /**
             * Sentry配置
             */
            private Sentry sentry = new Sentry();

            @Data
            public static class Sentry {
                /**
                 * Sentry DSN
                 */
                private String dsn = "";

                /**
                 * 环境名称
                 */
                private String environment = "development";

                /**
                 * 采样率
                 */
                private double tracesSampleRate = 0.1;

                /**
                 * 性能监控配置
                 */
                private Performance performance = new Performance();

                @Data
                public static class Performance {
                    /**
                     * 是否启用性能监控
                     */
                    private boolean enabled = true;

                    /**
                     * 慢请求阈值(毫秒)
                     */
                    private long slowRequestThreshold = 2000;

                    /**
                     * 慢查询阈值(毫秒)
                     */
                    private long slowQueryThreshold = 1000;
                }
            }
        }

        /**
         * 安全详细配置
         */
        @Data
        public static class Security {
            /**
             * 是否启用安全功能
             */
            private boolean enabled = true;

            /**
             * 密钥管理配置
             */
            private KeyManager keyManager = new KeyManager();

            @Data
            public static class KeyManager {
                /**
                 * 是否启用密钥管理器
                 */
                private boolean enabled = true;

                /**
                 * 密钥缓存配置
                 */
                private Cache cache = new Cache();

                /**
                 * 密钥轮换配置
                 */
                private Rotation rotation = new Rotation();

                /**
                 * 密钥强度验证
                 */
                private StrengthValidation strengthValidation = new StrengthValidation();

                @Data
                public static class Cache {
                    /**
                     * 是否启用密钥缓存
                     */
                    private boolean enabled = true;

                    /**
                     * 缓存TTL时间(秒)
                     */
                    private int ttl = 300;
                }

                @Data
                public static class Rotation {
                    /**
                     * 是否启用密钥轮换
                     */
                    private boolean enabled = false;

                    /**
                     * 检查间隔(秒)
                     */
                    private long checkInterval = 86400;

                    /**
                     * 密钥有效期(天)
                     */
                    private int keyValidityDays = 90;
                }

                @Data
                public static class StrengthValidation {
                    /**
                     * 是否启用强度验证
                     */
                    private boolean enabled = true;

                    /**
                     * 最小密钥长度
                     */
                    private int minKeyLength = 32;

                    /**
                     * 是否包含数字
                     */
                    private boolean requireNumbers = true;

                    /**
                     * 是否包含特殊字符
                     */
                    private boolean requireSpecialChars = false;
                }
            }
        }

        /**
         * 服务客户端配置
         */
        @Data
        public static class ServiceClient {
            /**
             * 连接超时时间(毫秒)
             */
            private int connectTimeout = 5000;

            /**
             * 读取超时时间(毫秒)
             */
            private int readTimeout = 10000;

            /**
             * 连接池最大连接数
             */
            private int maxTotal = 100;

            /**
             * 每个路由最大连接数
             */
            private int defaultMaxPerRoute = 20;
        }

        /**
         * 分布式锁配置
         */
        @Data
        public static class DistributedLock {
            /**
             * 锁等待时间(毫秒)
             */
            private long waitTime = 10000;

            /**
             * 锁自动续期时间(毫秒)
             */
            private long leaseTime = 30000;

            /**
             * 锁重试间隔(毫秒)
             */
            private long retryInterval = 100;
        }

        /**
         * 消息队列配置
         */
        @Data
        public static class Messaging {
            /**
             * 是否启用消息队列
             */
            private boolean enabled = false;

            /**
             * 重试次数
             */
            private int maxRetries = 3;

            /**
             * 重试间隔(毫秒)
             */
            private long retryDelay = 1000;
        }

        /**
         * 指标收集配置
         */
        @Data
        public static class Metrics {
            /**
             * 是否启用指标收集
             */
            private boolean enabled = false;

            /**
             * 指标收集间隔(秒)
             */
            private int interval = 60;
        }
    }
}