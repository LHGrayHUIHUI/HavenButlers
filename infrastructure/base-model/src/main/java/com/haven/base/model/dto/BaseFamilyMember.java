package com.haven.base.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家庭成员DTO
 */
@Data
public abstract class BaseFamilyMember implements Serializable {

    protected static final long serialVersionUID = 1L;

    /**
     * 成员用户ID
     */
    protected String userId;

    /**
     * 成员角色
     */
    protected String role;

    /**
     * 加入时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    protected LocalDateTime joinTime;
}
