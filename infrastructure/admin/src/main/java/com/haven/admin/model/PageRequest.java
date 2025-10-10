package com.haven.admin.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页请求DTO
 * 替代base-model的PageRequest
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
    private int page = 1; // 从1开始
    private int size = 20;
    private String sort;

    public PageRequest(int page, int size) {
        this.page = page;
        this.size = size;
    }
}