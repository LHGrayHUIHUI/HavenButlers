package com.haven.base.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备数据传输对象
 * 在服务间传递设备信息的通用模型
 *
 * @author HavenButler
 */
@Data
public abstract class BaseDeviceDTO implements Serializable {

    protected static final long serialVersionUID = 1L;

    /**
     * 设备ID
     */
    protected String deviceId;

    /**
     * 所属家庭ID
     */
    protected String familyId;

    /**
     * 所属房间ID
     */
    protected String roomId;

    /**
     * 设备名称
     */
    protected String deviceName;

    /**
     * 设备类型
     */
    protected String deviceType;

    /**
     * 设备状态
     */
    protected String status;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    protected LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    protected LocalDateTime updateTime;
}