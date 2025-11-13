package com.haven.storage.domain.model.enums;

import lombok.Getter;

@Getter
public enum FileStatus {
    NORMAL("正常", 0),       // 可用状态
    DELETED("已删除", 10),    // 放入回收站
    EXPIRED("已过期", 20),    // 超过有效期
    AUDITING("审核中", 30);   // 需审核后可用

    private final String desc;
    private final int code;

    FileStatus(String desc, int code) {
        this.desc = desc;
        this.code = code;
    }
}
