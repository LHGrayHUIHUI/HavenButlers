package com.haven.base.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础实体类
 * 所有实体类的父类，提供通用字段
 *
 * @author HavenButler
 */
@Data
@MappedSuperclass
public abstract class BaseEntity implements BaseModel {

    private static final long serialVersionUID = 1L;
    /**
     * id
     */
    private Long id;

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

    /**
     * 创建人ID
     */
    private String createBy;

    /**
     * 更新人ID
     */
    private String updateBy;

    /**
     * 逻辑删除标记（0：未删除，1：已删除）
     */
    private Integer deleted;

    /**
     * 版本号（用于乐观锁）
     */
    private Integer version;

    /**
     * 备注
     */
    private String remark;

    /**
     * 初始化创建信息
     */
    public void initCreateInfo(String userId) {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
        this.createBy = userId;
        this.updateBy = userId;
        this.deleted = 0;
        this.version = 1;
    }

    /**
     * 更新修改信息
     */
    public void updateModifyInfo(String userId) {
        this.updateTime = LocalDateTime.now();
        this.updateBy = userId;
    }

    /**
     * 是否已删除
     */
    public boolean isDeleted() {
        return this.deleted != null && this.deleted == 1;
    }

    /**
     * 标记为删除
     */
    public void markAsDeleted(String userId) {
        this.deleted = 1;
        this.updateModifyInfo(userId);
    }
}