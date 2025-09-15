# Admin 管理服务 开发指南

## 模块概述
Admin管理服务是HavenButler平台的运维管理中心，提供监控、配置、日志、链路追踪等管理功能。

## 开发规范

### 1. 项目结构
```
admin/
├── admin-server/             # 后端服务
│   ├── src/main/java/
│   │   ├── config/           # 配置类
│   │   ├── controller/       # 控制器
│   │   ├── service/          # 业务服务
│   │   ├── monitor/          # 监控模块
│   │   ├── alert/            # 告警模块
│   │   ├── trace/            # 链路追踪
│   │   └── security/         # 安全模块
│   └── src/main/resources/
└── admin-ui/                  # 前端界面
    ├── src/
    │   ├── views/             # 页面组件
    │   ├── components/        # 通用组件
    │   ├── api/               # API接口
    │   └── utils/             # 工具类
    └── public/
```

### 2. Spring Boot Admin配置

```java
package com.havenbutler.admin.config;

import de.codecentric.boot.admin.server.config.EnableAdminServer;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableAdminServer
public class AdminServerConfig extends WebSecurityConfigurerAdapter {
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        SavedRequestAwareAuthenticationSuccessHandler successHandler = 
            new SavedRequestAwareAuthenticationSuccessHandler();
        successHandler.setTargetUrlParameter("redirectTo");
        successHandler.setDefaultTargetUrl("/");
        
        http.authorizeRequests()
            .antMatchers("/assets/**").permitAll()
            .antMatchers("/login").permitAll()
            .antMatchers("/actuator/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .formLogin()
                .loginPage("/login")
                .successHandler(successHandler)
            .and()
            .logout()
                .logoutUrl("/logout")
            .and()
            .httpBasic()
            .and()
            .csrf()
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringAntMatchers(
                    "/instances",
                    "/actuator/**"
                );
    }
}
```

### 3. 监控模块实现

```java
package com.havenbutler.admin.monitor;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class ServiceMonitor {
    private final MeterRegistry meterRegistry;
    private final DiscoveryClient discoveryClient;
    
    // 服务健康检查
    @Scheduled(fixedDelay = 30000)
    public void checkServiceHealth() {
        List<String> services = discoveryClient.getServices();
        
        for (String service : services) {
            List<ServiceInstance> instances = 
                discoveryClient.getInstances(service);
            
            for (ServiceInstance instance : instances) {
                checkInstanceHealth(instance);
            }
        }
    }
    
    private void checkInstanceHealth(ServiceInstance instance) {
        String healthUrl = instance.getUri() + "/actuator/health";
        
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                healthUrl, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> health = response.getBody();
                String status = (String) health.get("status");
                
                // 记录指标
                meterRegistry.gauge(
                    "service.health",
                    Tags.of("service", instance.getServiceId(),
                           "instance", instance.getInstanceId()),
                    "UP".equals(status) ? 1 : 0
                );
                
                // 如果不健康，触发告警
                if (!"UP".equals(status)) {
                    alertService.sendAlert(
                        AlertLevel.WARNING,
                        String.format("Service %s instance %s is %s",
                            instance.getServiceId(),
                            instance.getInstanceId(),
                            status)
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed to check health for {}", 
                     instance.getServiceId(), e);
            
            // 服务不可达告警
            alertService.sendAlert(
                AlertLevel.CRITICAL,
                String.format("Service %s instance %s is unreachable",
                    instance.getServiceId(),
                    instance.getInstanceId())
            );
        }
    }
}

// 自定义健康指示器
@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    @Autowired
    private DataSource dataSource;
    
    @Override
    public Health health() {
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(1)) {
                return Health.up()
                    .withDetail("database", "MySQL")
                    .withDetail("version", getDatabaseVersion(conn))
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
        return Health.down().build();
    }
}
```

### 4. 告警系统

```java
package com.havenbutler.admin.alert;

@Service
@Slf4j
public class AlertService {
    @Autowired
    private AlertRuleRepository ruleRepository;
    
    @Autowired
    private NotificationService notificationService;
    
    // 告警规则引擎
    @Scheduled(fixedDelay = 60000)
    public void evaluateAlertRules() {
        List<AlertRule> rules = ruleRepository.findAllActive();
        
        for (AlertRule rule : rules) {
            evaluateRule(rule);
        }
    }
    
    private void evaluateRule(AlertRule rule) {
        // 查询Prometheus指标
        String query = rule.getPromQL();
        PrometheusResponse response = prometheusClient.query(query);
        
        if (response.getData().getResult().size() > 0) {
            double value = response.getData().getResult().get(0).getValue();
            
            boolean shouldAlert = false;
            switch (rule.getOperator()) {
                case GREATER_THAN:
                    shouldAlert = value > rule.getThreshold();
                    break;
                case LESS_THAN:
                    shouldAlert = value < rule.getThreshold();
                    break;
                case EQUALS:
                    shouldAlert = Math.abs(value - rule.getThreshold()) < 0.001;
                    break;
            }
            
            if (shouldAlert) {
                triggerAlert(rule, value);
            }
        }
    }
    
    private void triggerAlert(AlertRule rule, double value) {
        Alert alert = Alert.builder()
            .ruleId(rule.getId())
            .ruleName(rule.getName())
            .level(rule.getLevel())
            .message(String.format(rule.getMessageTemplate(), value))
            .value(value)
            .timestamp(Instant.now())
            .build();
        
        // 保存告警记录
        alertRepository.save(alert);
        
        // 发送通知
        notificationService.sendNotification(alert);
    }
}

// 通知服务
@Service
public class NotificationService {
    @Autowired
    private EmailService emailService;
    
    @Autowired
    private SmsService smsService;
    
    @Autowired
    private WebhookService webhookService;
    
    public void sendNotification(Alert alert) {
        // 根据告警级别选择通知方式
        switch (alert.getLevel()) {
            case CRITICAL:
                // 严重告警：邮件 + 短信 + Webhook
                emailService.sendAlertEmail(alert);
                smsService.sendAlertSms(alert);
                webhookService.sendWebhook(alert);
                break;
            case WARNING:
                // 警告：邮件 + Webhook
                emailService.sendAlertEmail(alert);
                webhookService.sendWebhook(alert);
                break;
            case INFO:
                // 信息：仅Webhook
                webhookService.sendWebhook(alert);
                break;
        }
    }
}
```

### 5. 日志管理

```java
package com.havenbutler.admin.log;

@RestController
@RequestMapping("/api/logs")
public class LogController {
    @Autowired
    private ElasticsearchClient elasticsearchClient;
    
    @GetMapping("/search")
    public PageResult<LogEntry> searchLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        // 构建查询
        BoolQueryBuilder query = QueryBuilders.boolQuery();
        
        if (StringUtils.hasText(keyword)) {
            query.must(QueryBuilders.matchQuery("message", keyword));
        }
        
        if (StringUtils.hasText(service)) {
            query.filter(QueryBuilders.termQuery("service", service));
        }
        
        if (StringUtils.hasText(level)) {
            query.filter(QueryBuilders.termQuery("level", level));
        }
        
        if (StringUtils.hasText(traceId)) {
            query.filter(QueryBuilders.termQuery("traceId", traceId));
        }
        
        if (startTime != null || endTime != null) {
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("@timestamp");
            if (startTime != null) {
                rangeQuery.gte(startTime);
            }
            if (endTime != null) {
                rangeQuery.lte(endTime);
            }
            query.filter(rangeQuery);
        }
        
        // 执行查询
        SearchRequest searchRequest = new SearchRequest("logs-*");
        searchRequest.source()
            .query(query)
            .from(page * size)
            .size(size)
            .sort("@timestamp", SortOrder.DESC);
        
        SearchResponse response = elasticsearchClient.search(searchRequest);
        
        // 解析结果
        List<LogEntry> logs = Arrays.stream(response.getHits().getHits())
            .map(hit -> parseLogEntry(hit.getSourceAsMap()))
            .collect(Collectors.toList());
        
        return PageResult.<LogEntry>builder()
            .content(logs)
            .totalElements(response.getHits().getTotalHits().value)
            .totalPages((int) Math.ceil(response.getHits().getTotalHits().value / (double) size))
            .page(page)
            .size(size)
            .build();
    }
    
    @GetMapping("/aggregate")
    public Map<String, Object> aggregateLogs(
            @RequestParam String field,
            @RequestParam(defaultValue = "1h") String interval) {
        
        // 时间聚合
        DateHistogramAggregationBuilder dateAgg = 
            AggregationBuilders.dateHistogram("time_buckets")
                .field("@timestamp")
                .fixedInterval(new DateHistogramInterval(interval));
        
        // 字段聚合
        TermsAggregationBuilder termsAgg = 
            AggregationBuilders.terms("field_buckets")
                .field(field)
                .size(10);
        
        dateAgg.subAggregation(termsAgg);
        
        SearchRequest searchRequest = new SearchRequest("logs-*");
        searchRequest.source()
            .size(0)
            .aggregation(dateAgg);
        
        SearchResponse response = elasticsearchClient.search(searchRequest);
        
        // 解析聚合结果
        return parseAggregation(response.getAggregations());
    }
}
```

### 6. 链路追踪

```java
package com.havenbutler.admin.trace;

@Service
public class TraceService {
    @Autowired
    private JaegerClient jaegerClient;
    
    public TraceDetails getTrace(String traceId) {
        // 获取完整链路
        Trace trace = jaegerClient.getTrace(traceId);
        
        // 构建链路树
        TraceTree tree = buildTraceTree(trace.getSpans());
        
        // 计算性能指标
        TraceMetrics metrics = calculateMetrics(trace.getSpans());
        
        return TraceDetails.builder()
            .traceId(traceId)
            .tree(tree)
            .metrics(metrics)
            .spans(trace.getSpans())
            .build();
    }
    
    private TraceTree buildTraceTree(List<Span> spans) {
        Map<String, TraceNode> nodeMap = new HashMap<>();
        TraceNode root = null;
        
        // 创建节点
        for (Span span : spans) {
            TraceNode node = TraceNode.builder()
                .spanId(span.getSpanId())
                .operationName(span.getOperationName())
                .serviceName(span.getProcess().getServiceName())
                .startTime(span.getStartTime())
                .duration(span.getDuration())
                .tags(span.getTags())
                .logs(span.getLogs())
                .build();
            
            nodeMap.put(span.getSpanId(), node);
            
            if (span.getReferences().isEmpty()) {
                root = node;
            }
        }
        
        // 构建树结构
        for (Span span : spans) {
            if (!span.getReferences().isEmpty()) {
                String parentId = span.getReferences().get(0).getSpanId();
                TraceNode parent = nodeMap.get(parentId);
                TraceNode child = nodeMap.get(span.getSpanId());
                
                if (parent != null && child != null) {
                    parent.addChild(child);
                }
            }
        }
        
        return new TraceTree(root);
    }
    
    private TraceMetrics calculateMetrics(List<Span> spans) {
        long totalDuration = spans.stream()
            .mapToLong(Span::getDuration)
            .sum();
        
        Map<String, Long> serviceDurations = spans.stream()
            .collect(Collectors.groupingBy(
                span -> span.getProcess().getServiceName(),
                Collectors.summingLong(Span::getDuration)
            ));
        
        long criticalPathDuration = calculateCriticalPath(spans);
        
        return TraceMetrics.builder()
            .totalDuration(totalDuration)
            .spanCount(spans.size())
            .serviceCount(serviceDurations.size())
            .serviceDurations(serviceDurations)
            .criticalPathDuration(criticalPathDuration)
            .build();
    }
}
```

### 7. 前端界面 (Vue3)

```vue
<!-- Dashboard.vue -->
<template>
  <div class="dashboard">
    <el-row :gutter="20">
      <!-- 服务状态卡片 -->
      <el-col :span="6" v-for="service in services" :key="service.name">
        <el-card>
          <div class="service-card">
            <div class="service-name">{{ service.name }}</div>
            <div class="service-status" :class="service.status">
              <i :class="getStatusIcon(service.status)"></i>
              {{ service.status }}
            </div>
            <div class="service-metrics">
              <div>CPU: {{ service.cpu }}%</div>
              <div>Memory: {{ service.memory }}MB</div>
              <div>Uptime: {{ service.uptime }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 指标图表 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>API调用量</span>
          </template>
          <div ref="apiChart" style="height: 300px"></div>
        </el-card>
      </el-col>
      
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>响应时间</span>
          </template>
          <div ref="responseChart" style="height: 300px"></div>
        </el-card>
      </el-col>
    </el-row>
    
    <!-- 告警列表 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <span>最近告警</span>
      </template>
      <el-table :data="alerts" style="width: 100%">
        <el-table-column prop="timestamp" label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.timestamp) }}
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)">
              {{ row.level }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="service" label="服务" width="150" />
        <el-table-column prop="message" label="消息" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="handleAlert(row)">
              处理
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { getServices, getAlerts, getMetrics } from '@/api/admin'

const services = ref([])
const alerts = ref([])
const apiChart = ref(null)
const responseChart = ref(null)

let apiChartInstance = null
let responseChartInstance = null
let refreshTimer = null

onMounted(async () => {
  await loadData()
  initCharts()
  
  // 定时刷新
  refreshTimer = setInterval(loadData, 30000)
})

onUnmounted(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  
  if (apiChartInstance) {
    apiChartInstance.dispose()
  }
  
  if (responseChartInstance) {
    responseChartInstance.dispose()
  }
})

const loadData = async () => {
  services.value = await getServices()
  alerts.value = await getAlerts({ limit: 10 })
  
  const metrics = await getMetrics({
    metrics: ['api_calls', 'response_time'],
    interval: '5m',
    duration: '1h'
  })
  
  updateCharts(metrics)
}

const initCharts = () => {
  apiChartInstance = echarts.init(apiChart.value)
  responseChartInstance = echarts.init(responseChart.value)
  
  // 配置图表选项
  const option = {
    xAxis: { type: 'time' },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true }]
  }
  
  apiChartInstance.setOption(option)
  responseChartInstance.setOption(option)
}
</script>
```

## 开发注意事项

1. **安全性**：Admin服务不应该对外暴露
2. **权限控制**：严格的RBAC权限管理
3. **审计日志**：所有操作记录日志
4. **数据保留**：定期清理历史数据
5. **性能优化**：避免频繁查询大量数据

## 常用命令

```bash
# 后端服务
mvn clean package
java -jar target/admin-server.jar

# 前端界面
cd admin-ui
npm install
npm run dev
npm run build

# Docker构建
docker build -t smart-home/admin-service:v1.0.0 .

# Docker运行
docker run -d --name admin-service --network smart-home-network smart-home/admin-service:v1.0.0
```