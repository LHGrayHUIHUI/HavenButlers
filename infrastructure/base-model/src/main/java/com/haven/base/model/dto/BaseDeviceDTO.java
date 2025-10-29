package com.haven.base.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.haven.base.model.entity.BaseEntity;
import jakarta.persistence.MappedSuperclass;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 设备数据传输对象
 * 在服务间传递设备信息的通用模型
 *
 * @author HavenButler
 */
@EqualsAndHashCode(callSuper = true)
@Data
@MappedSuperclass
public abstract class BaseDeviceDTO extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * 所属家庭ID
     */
    private String familyId;

    /**
     * 所属房间ID
     */
    private String roomId;

    /**
     * 设备名称
     */
    private String deviceName;

    /**
     * 设备类型（LIGHT/SWITCH/SENSOR/CAMERA/LOCK/CURTAIN/AIR_CONDITIONER等）
     */
    private String deviceType;

    /**
     * 设备分类（照明/安防/环境/家电/其他）
     */
    private String category;

    /**
     * 品牌
     */
    private String brand;

    /**
     * 型号
     */
    private String model;

    /**
     * 通信协议（MATTER/ZIGBEE/WIFI/BLUETOOTH/ZWAVE）
     */
    private String protocol;

    /**
     * MAC地址
     */
    private String macAddress;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 固件版本
     */
    private String firmwareVersion;

    /**
     * 硬件版本
     */
    private String hardwareVersion;

    /**
     * 设备状态（ONLINE/OFFLINE/ERROR/UPDATING）
     */
    private String status;

    /**
     * 连接状态（CONNECTED/DISCONNECTED/CONNECTING）
     */
    private String connectionStatus;

    /**
     * 是否可用
     */
    private Boolean available;

    /**
     * 是否支持OTA升级
     */
    private Boolean supportOta;

    /**
     * 设备能力描述（JSON）
     * 例如：调光、调色、定时、场景等
     */
    private Map<String, Object> capabilities;

    /**
     * 设备属性（JSON）
     * 当前状态值：开关状态、亮度、颜色、温度等
     */
    private Map<String, Object> properties;

    /**
     * 设备配置（JSON）
     * 用户自定义配置
     */
    private Map<String, Object> settings;

    /**
     * 设备图标
     */
    private String icon;

    /**
     * 设备图片URL
     */
    private String imageUrl;

    /**
     * 信号强度（RSSI）
     */
    private Integer signalStrength;

    /**
     * 电池电量（百分比）
     */
    private Integer batteryLevel;

    /**
     * 是否为电池供电
     */
    private Boolean batteryPowered;

    /**
     * 最后上线时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastOnlineTime;

    /**
     * 最后离线时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastOfflineTime;

    /**
     * 最后心跳时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastHeartbeatTime;

    /**
     * 添加时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    /**
     * 是否在线
     */
    public boolean isOnline() {
        return "ONLINE".equals(status);
    }

    /**
     * 是否离线
     */
    public boolean isOffline() {
        return "OFFLINE".equals(status);
    }

    /**
     * 是否有错误
     */
    public boolean hasError() {
        return "ERROR".equals(status);
    }

    /**
     * 是否正在更新
     */
    public boolean isUpdating() {
        return "UPDATING".equals(status);
    }

    /**
     * 是否已连接
     */
    public boolean isConnected() {
        return "CONNECTED".equals(connectionStatus);
    }

    /**
     * 是否低电量（低于20%）
     */
    public boolean isLowBattery() {
        return batteryPowered != null && batteryPowered &&
                batteryLevel != null && batteryLevel < 20;
    }

    /**
     * 获取设备属性值
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> clazz) {
        if (properties == null || !properties.containsKey(key)) {
            return null;
        }
        Object value = properties.get(key);
        if (clazz.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 设置设备属性值
     */
    public void setProperty(String key, Object value) {
        if (properties == null) {
            properties = new java.util.HashMap<>();
        }
        properties.put(key, value);
    }

    /**
     * 获取设备能力
     */
    public boolean hasCapability(String capability) {
        if (capabilities == null) {
            return false;
        }
        Object value = capabilities.get(capability);
        return Boolean.TRUE.equals(value);
    }

    /**
     * 获取设备设置值
     */
    @SuppressWarnings("unchecked")
    public <T> T getSetting(String key, Class<T> clazz) {
        if (settings == null || !settings.containsKey(key)) {
            return null;
        }
        Object value = settings.get(key);
        if (clazz.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * 获取设备显示名称
     * 优先返回用户自定义名称，否则返回型号
     */
    public String getDisplayName() {
        if (deviceName != null && !deviceName.trim().isEmpty()) {
            return deviceName;
        }
        if (model != null && !model.trim().isEmpty()) {
            return model;
        }
        return deviceType;
    }

    /**
     * 获取设备完整标识
     * 格式：品牌-型号-MAC地址后4位
     */
    public String getFullIdentifier() {
        StringBuilder sb = new StringBuilder();
        if (brand != null) {
            sb.append(brand).append("-");
        }
        if (model != null) {
            sb.append(model).append("-");
        }
        if (macAddress != null && macAddress.length() >= 4) {
            sb.append(macAddress.substring(macAddress.length() - 4));
        }
        return sb.toString();
    }
}