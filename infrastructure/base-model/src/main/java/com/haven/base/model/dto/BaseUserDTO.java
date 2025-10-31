package com.haven.base.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户数据传输对象
 * 在服务间传递用户信息的通用模型
 *
 * @author HavenButler
 */
@Data
public abstract class BaseUserDTO implements Serializable {

    protected static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    protected String userId;

    /**
     * 用户名
     */
    protected String username;

    /**
     * 邮箱
     */
    protected String email;

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