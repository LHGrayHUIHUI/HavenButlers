package com.haven.base.model.dto;

import com.haven.base.model.entity.BaseEntity;
import jakarta.persistence.MappedSuperclass;
import lombok.*;

import java.io.Serializable;

/**
 * 房间DTO
 */
@EqualsAndHashCode(callSuper = true)
@Data
@MappedSuperclass
public abstract class BaseRoomDTO extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 房间ID
     */
    private String roomId;

    /**
     * 房间名称
     */
    private String roomName;

    /**
     * 房间类型（BEDROOM/LIVING_ROOM/KITCHEN/BATHROOM/BALCONY/OTHER）
     */
    private String roomType;

    /**
     * 楼层
     */
    private Integer floor;

    /**
     * 设备数量
     */
    private Integer deviceCount;

    /**
     * 房间图标
     */
    private String icon;

    /**
     * 排序号
     */
    private Integer sortOrder;
}
