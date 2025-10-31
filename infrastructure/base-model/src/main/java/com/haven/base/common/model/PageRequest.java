package com.haven.base.common.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 通用分页请求模型
 * 统一分页查询参数
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {

    /**
     * 页码（从0开始）
     */
    @Builder.Default
    private int page = 0;

    /**
     * 每页大小
     */
    @Builder.Default
    private int size = 20;

    /**
     * 排序字段
     */
    private String sort;

    /**
     * 排序方向（ASC/DESC）
     */
    @Builder.Default
    private String direction = "DESC";

    /**
     * 默认分页请求
     */
    public static PageRequest of(int page, int size) {
        return PageRequest.builder()
                .page(page)
                .size(size)
                .build();
    }

    /**
     * 带排序的分页请求
     */
    public static PageRequest of(int page, int size, String sort, String direction) {
        return PageRequest.builder()
                .page(page)
                .size(size)
                .sort(sort)
                .direction(direction)
                .build();
    }

    /**
     * 转换为Spring PageRequest
     */
    public org.springframework.data.domain.PageRequest toSpringPageRequest() {
        if (sort != null && !sort.trim().isEmpty()) {
            org.springframework.data.domain.Sort.Direction dir =
                "ASC".equalsIgnoreCase(direction) ?
                org.springframework.data.domain.Sort.Direction.ASC :
                org.springframework.data.domain.Sort.Direction.DESC;

            return org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by(dir, sort));
        }

        return org.springframework.data.domain.PageRequest.of(page, size);
    }

    /**
     * 验证分页参数
     */
    public void validate() {
        if (page < 0) {
            throw new IllegalArgumentException("页码不能小于0");
        }
        if (size <= 0 || size > 1000) {
            throw new IllegalArgumentException("每页大小必须在1-1000之间");
        }
        if (sort != null && sort.trim().isEmpty()) {
            sort = null;
        }
    }
}