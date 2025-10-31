package com.haven.base.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家庭数据传输对象
 * 在服务间传递家庭信息的通用模型
 *
 * @author HavenButler
 */
@Data
public abstract class BaseFamilyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 家庭ID
     */
    private String familyId;

    /**
     * 家庭名称
     */
    private String familyName;

    /**
     * 家庭创建者/管理员ID
     */
    private String ownerId;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}