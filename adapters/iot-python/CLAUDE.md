# IoT Python适配器 开发指南

## 模块概述
IoT Python适配器是HavenButler平台的设备集成层，负责各种智能设备的发现、连接、控制和状态同步。

## 开发规范

### 1. 代码结构
```
iot-python/
├── src/
│   ├── adapters/          # 设备适配器
│   │   ├── mqtt/          # MQTT设备
│   │   ├── coap/          # CoAP设备
│   │   ├── zigbee/        # Zigbee设备
│   │   └── matter/        # Matter设备
│   ├── discovery/         # 设备发现
│   ├── protocols/         # 协议实现
│   ├── models/            # 数据模型
│   ├── services/          # 业务服务
│   └── main.py            # 入口文件
├── tests/                 # 测试文件
├── proto/                 # gRPC定义
└── docker/                # Docker配置
```

### 2. 设备适配器开发

#### 基础适配器接口
```python
from abc import ABC, abstractmethod
from typing import Dict, List, Optional

class DeviceAdapter(ABC):
    """设备适配器基类"""
    
    @abstractmethod
    async def discover(self) -> List[DeviceInfo]:
        """发现设备"""
        pass
    
    @abstractmethod
    async def connect(self, device_id: str) -> bool:
        """连接设备"""
        pass
    
    @abstractmethod
    async def get_status(self, device_id: str) -> DeviceStatus:
        """获取设备状态"""
        pass
    
    @abstractmethod
    async def control(self, device_id: str, command: Dict) -> bool:
        """控制设备"""
        pass
    
    @abstractmethod
    async def disconnect(self, device_id: str) -> bool:
        """断开连接"""
        pass
```

#### 具体适配器实现
```python
class XiaomiAdapter(DeviceAdapter):
    """小米设备适配器"""
    
    def __init__(self):
        self.devices = {}
        self.client = MiioClient()
    
    async def discover(self) -> List[DeviceInfo]:
        """通过mDNS发现小米设备"""
        devices = await self.client.discover()
        return [self._convert_to_standard(d) for d in devices]
    
    async def control(self, device_id: str, command: Dict) -> bool:
        """发送控制指令"""
        device = self.devices.get(device_id)
        if not device:
            return False
        
        # 转换标准指令为小米协议
        mi_command = self._convert_command(command)
        return await device.send(mi_command)
```

### 3. 协议实现

#### MQTT协议集成
```python
import asyncio
from paho.mqtt import client as mqtt_client

class MQTTProtocol:
    def __init__(self, broker: str, port: int = 1883):
        self.broker = broker
        self.port = port
        self.client = mqtt_client.Client()
        self.client.on_connect = self._on_connect
        self.client.on_message = self._on_message
    
    async def connect(self):
        """连接MQTT代理"""
        self.client.connect(self.broker, self.port)
        self.client.loop_start()
    
    async def publish(self, topic: str, payload: dict):
        """发布消息"""
        self.client.publish(topic, json.dumps(payload))
    
    async def subscribe(self, topic: str, callback):
        """订阅主题"""
        self.client.subscribe(topic)
        self.callbacks[topic] = callback
```

### 4. 设备模型定义

```python
from dataclasses import dataclass
from enum import Enum
from typing import Optional, Dict, Any

class DeviceType(Enum):
    """设备类型枚举"""
    LIGHT = "light"
    SWITCH = "switch"
    SENSOR = "sensor"
    CAMERA = "camera"
    THERMOSTAT = "thermostat"
    LOCK = "lock"

@dataclass
class DeviceInfo:
    """设备信息"""
    device_id: str
    name: str
    type: DeviceType
    manufacturer: str
    model: str
    firmware_version: str
    ip_address: Optional[str]
    mac_address: Optional[str]
    capabilities: Dict[str, Any]

@dataclass
class DeviceStatus:
    """设备状态"""
    device_id: str
    online: bool
    state: Dict[str, Any]
    last_seen: datetime
    battery_level: Optional[int]
```

### 5. 服务与存储交互

```python
class DeviceService:
    def __init__(self):
        self.storage_client = StorageServiceClient()
        self.message_client = MessageServiceClient()
    
    async def save_device(self, device: DeviceInfo):
        """保存设备信息到storage-service"""
        # 所有数据必须通过storage-service
        await self.storage_client.save(
            collection="devices",
            document=device.dict(),
            family_id=self.get_family_id()
        )
    
    async def notify_device_update(self, device_id: str, status: DeviceStatus):
        """通知设备状态更新"""
        await self.message_client.publish(
            topic=f"device/{device_id}/status",
            message=status.dict()
        )
```

### 6. 异步任务管理

```python
class TaskManager:
    def __init__(self):
        self.tasks = {}
    
    async def start_discovery_task(self, interval: int = 30):
        """启动设备发现任务"""
        async def discovery_loop():
            while True:
                try:
                    devices = await self.discover_all_devices()
                    await self.process_discovered_devices(devices)
                except Exception as e:
                    logger.error(f"Discovery error: {e}")
                await asyncio.sleep(interval)
        
        task = asyncio.create_task(discovery_loop())
        self.tasks['discovery'] = task
    
    async def start_status_sync_task(self, interval: int = 5):
        """启动状态同步任务"""
        async def sync_loop():
            while True:
                try:
                    await self.sync_all_device_status()
                except Exception as e:
                    logger.error(f"Sync error: {e}")
                await asyncio.sleep(interval)
        
        task = asyncio.create_task(sync_loop())
        self.tasks['status_sync'] = task
```

### 7. gRPC服务实现

```python
import grpc
from concurrent import futures
import iot_adapter_pb2
import iot_adapter_pb2_grpc

class IoTAdapterService(iot_adapter_pb2_grpc.IoTAdapterServicer):
    def __init__(self):
        self.device_manager = DeviceManager()
    
    async def DiscoverDevices(self, request, context):
        """发现设备RPC方法"""
        devices = await self.device_manager.discover()
        return iot_adapter_pb2.DeviceList(
            devices=[self._to_proto(d) for d in devices]
        )
    
    async def ControlDevice(self, request, context):
        """控制设备RPC方法"""
        result = await self.device_manager.control(
            device_id=request.device_id,
            command=MessageToDict(request.command)
        )
        return iot_adapter_pb2.CommandResult(success=result)

def serve():
    server = grpc.aio.server()
    iot_adapter_pb2_grpc.add_IoTAdapterServicer_to_server(
        IoTAdapterService(), server
    )
    server.add_insecure_port('[::]:8090')
    return server
```

### 8. 性能优化

```python
# 使用连接池
from asyncio import Queue

class ConnectionPool:
    def __init__(self, max_connections: int = 100):
        self.pool = Queue(maxsize=max_connections)
        self.connections = {}
    
    async def get_connection(self, device_id: str):
        if device_id in self.connections:
            return self.connections[device_id]
        
        conn = await self.create_connection(device_id)
        self.connections[device_id] = conn
        return conn

# 使用缓存
from functools import lru_cache
from aiocache import cached

@cached(ttl=60)
async def get_device_info(device_id: str):
    """缓存设备信息60秒"""
    return await storage_client.get_device(device_id)
```

### 9. 错误处理

```python
class DeviceError(Exception):
    """设备错误基类"""
    pass

class DeviceNotFoundError(DeviceError):
    """设备未找到"""
    pass

class DeviceOfflineError(DeviceError):
    """设备离线"""
    pass

class DeviceControlError(DeviceError):
    """设备控制失败"""
    pass

# 错误处理装饰器
def handle_device_errors(func):
    async def wrapper(*args, **kwargs):
        try:
            return await func(*args, **kwargs)
        except DeviceNotFoundError as e:
            logger.error(f"Device not found: {e}")
            return {"error": "device_not_found", "message": str(e)}
        except DeviceOfflineError as e:
            logger.warning(f"Device offline: {e}")
            return {"error": "device_offline", "message": str(e)}
        except Exception as e:
            logger.exception(f"Unexpected error: {e}")
            return {"error": "internal_error", "message": "Internal server error"}
    return wrapper
```

### 10. 测试策略

```python
# tests/test_adapter.py
import pytest
from unittest.mock import Mock, AsyncMock

@pytest.mark.asyncio
async def test_device_discovery():
    adapter = XiaomiAdapter()
    adapter.client = AsyncMock()
    adapter.client.discover.return_value = [
        {"id": "device1", "name": "Light 1"}
    ]
    
    devices = await adapter.discover()
    assert len(devices) == 1
    assert devices[0].device_id == "device1"

@pytest.mark.asyncio
async def test_device_control():
    adapter = XiaomiAdapter()
    adapter.devices = {"device1": AsyncMock()}
    
    result = await adapter.control(
        "device1", 
        {"action": "turn_on"}
    )
    assert result is True
```

## 开发注意事项

1. **异步编程**：全部使用async/await，避免阻塞操作
2. **错误处理**：所有设备操作都要捕获异常
3. **日志记录**：详细记录设备操作日志
4. **性能优化**：使用连接池和缓存
5. **数据存储**：严禁直接访问数据库，必须通过storage-service

## 常用命令

```bash
# 安装依赖
pip install -r requirements.txt

# 运行服务
python src/main.py

# 运行测试
pytest tests/ -v

# 生成覆盖率报告
pytest --cov=src --cov-report=html

# Docker构建
docker build -t smart-home/iot-python:v1.0.0 .

# Docker运行
docker run -d --name iot-python --network smart-home-network smart-home/iot-python:v1.0.0
```