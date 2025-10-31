package com.haven.base.model.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

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

    protected static final long serialVersionUID = 1L;
    /**
     * 主键ID
     * 数据库自增长主键，用于唯一标识实体记录
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

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

    /**
     * 创建人ID
     */
    protected String createBy;

    /**
     * 更新人ID
     */
    protected String updateBy;

  
    /**
     * 版本号（用于乐观锁）
     */
    protected Integer version;

    /**
     * 备注
     */
    protected String remark;

    /**
     * 初始化创建信息
     */
    public void initCreateInfo(String userId) {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
        this.createBy = userId;
        this.updateBy = userId;
        this.version = 1;
    }

    /**
     * 更新修改信息
     */
    public void updateModifyInfo(String userId) {
        this.updateTime = LocalDateTime.now();
        this.updateBy = userId;
    }


}