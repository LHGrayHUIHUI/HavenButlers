package com.haven.storage.model.enums;

import lombok.Getter;

@Getter
public enum FileCategory {
    IMAGE("image"),
    VIDEO("video"),
    AUDIO("audio"),
    DOCUMENT("document"),
    ARCHIVE("archive"),
    UNKNOWN("unknown"); // 未知类型

    private final String categoryName;

    FileCategory(String categoryName) {
        this.categoryName = categoryName;
    }

}
