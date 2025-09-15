# IoT Python适配器

## 服务定位
- **架构层级**：多语言适配层
- **核心职责**：提供Python环境下的IoT设备SDK集成、协议适配和设备发现
- **业务范围**：设备发现、协议转换、MQTT/CoAP通信、Matter设备集成

## 技术栈
- **主开发语言**：Python 3.10+
- **核心框架**：FastAPI、asyncio
- **IoT协议**：MQTT (paho-mqtt)、CoAP (aiocoap)、Zigbee (zigpy)
- **设备SDK**：Tuya SDK、Xiaomi IoT SDK、HomeKit SDK
- **通信协议**：gRPC（与Java服务通信）

## 部署信息
- **Docker镜像**：`smart-home/iot-python:v1.0.0`
- **内部端口**：8090 (Docker网络内)
- **健康检查**：`/health`
- **环境变量**：
  ```bash
  GATEWAY_URL=http://gateway:8080
  STORAGE_SERVICE_URL=http://storage-service:8080
  MQTT_BROKER=mqtt://mqtt-broker:1883
  DEVICE_DISCOVERY_INTERVAL=30
  ```

## 接口信息

### 内部服务调用
- **通信协议**：gRPC + TLS
- **Proto定义**：`proto/iot_adapter.proto`
- **主要接口**：
  ```protobuf
  service IoTAdapter {
    rpc DiscoverDevices(Empty) returns (DeviceList);
    rpc ControlDevice(DeviceCommand) returns (CommandResult);
    rpc GetDeviceStatus(DeviceId) returns (DeviceStatus);
    rpc RegisterDevice(DeviceInfo) returns (RegisterResult);
  }
  ```

### 设备协议适配
- **MQTT设备**：自动订阅和发布消息
- **CoAP设备**：REST风格资源访问
- **Zigbee设备**：网关模式集成
- **Matter设备**：标准化设备模型

## 支持的设备类型

### 智能照明
- Philips Hue
- Yeelight
- LIFX
- TP-Link Kasa

### 智能插座
- 小米智能插座
- TP-Link智能插座
- 涂鸦智能插座

### 传感器
- 温湿度传感器
- 门窗传感器
- 人体传感器
- 烟雾传感器

### 智能家电
- 空调（红外控制）
- 电视（红外/WiFi）
- 净化器
- 扫地机器人

## 设备发现机制

```python
# 设备发现流程
class DeviceDiscovery:
    async def discover_devices():
        devices = []
        
        # 1. mDNS/Bonjour发现
        devices.extend(await discover_mdns())
        
        # 2. UPnP发现
        devices.extend(await discover_upnp())
        
        # 3. 广播发现
        devices.extend(await discover_broadcast())
        
        # 4. 云端API发现
        devices.extend(await discover_cloud_api())
        
        return devices
```

## 依赖关系
- **直接依赖**：
  - `storage-service:v1.0.0` - 设备信息存储
  - `message-service:v1.0.0` - 设备事件推送
- **被依赖方**：
  - `gateway-service` - API路由
  - `ai-service` - 设备控制指令

## 数据访问规范
⚠️ **严禁直接连接数据库** - 所有数据操作必须通过 `storage-service` 接口

## 性能指标
- 设备发现时间：< 5秒
- 指令响应时间：< 500ms
- 并发设备数：1000+
- 消息吞吐量：10000 msg/s

## 安全要求
- 设备通信加密：TLS 1.3
- 设备认证：OAuth 2.0 / API Key
- 数据隔离：基于家庭 ID
- 敏感信息加密存储

## 独立测试部署
```bash
# 启动最小依赖环境
docker-compose -f docker/test-compose.yml up -d

# 启动服务
docker run -d \
  --name iot-python \
  --network smart-home-network \
  -e GATEWAY_URL=http://gateway:8080 \
  smart-home/iot-python:v1.0.0

# 验证服务状态
curl http://localhost:8090/health
```

## Docker网络配置
- **网络名称**：smart-home-network
- **容器名称**：iot-python
- **端口映射**：仅测试环境临时开放 localhost:8090->8090
- **内部通信**：其他服务通过 `iot-python:8090` 访问

## 测试要求
### 单元测试
```bash
pytest tests/ --cov=src --cov-report=html
# 覆盖率要求：≥75%
```

### Docker集成测试
```bash
# 必须在Docker环境中测试
docker-compose -f docker/integration-test.yml up --abort-on-container-exit
```

## 监控和日志
- **关键指标**：设备在线率、指令成功率、响应延迟
- **日志级别**：INFO (设备操作), ERROR (通信异常)
- **TraceID**：所有请求必须携带并传递

## 故障排查
1. **设备发现失败**：检查网络配置和防火墙
2. **指令执行超时**：查看设备网络状态
3. **协议不兼容**：更新设备SDK版本

## 更新历史
- v1.0.0 (2025-01-15): 初始版本，支持基本设备类型