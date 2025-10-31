package com.haven.base.model.dto;

import com.haven.base.model.entity.BaseEntity;
import lombok.*;

import java.io.Serializable;

/**
 * 房间DTO
 */
@EqualsAndHashCode(callSuper = true)
@Data

public abstract class BaseRoomDTO extends BaseEntity implements Serializable {

    protected static final long serialVersionUID = 1L;

    /**
     * 房间ID
     */
    protected String roomId;

    /**
     * 房间名称
     */
    protected String roomName;

    /**
     * 房间类型（BEDROOM/LIVING_ROOM/KITCHEN/BATHROOM/BALCONY/OTHER）
     */
    protected String roomType;

    /**
     * 楼层
     */
    protected Integer floor;

    /**
     * 设备数量
     */
    protected Integer deviceCount;

    /**
     * 房间图标
     */
    protected String icon;

    /**
     * 排序号
     */
    protected Integer sortOrder;
}
